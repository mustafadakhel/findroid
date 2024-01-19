package dev.jdtech.jellyfin

import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Rational
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Space
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.DefaultTimeBar
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TrackSelectionDialogBuilder
import androidx.navigation.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.jdtech.jellyfin.databinding.ActivityPlayerBinding
import dev.jdtech.jellyfin.dialogs.SpeedSelectionDialogFragment
import dev.jdtech.jellyfin.dialogs.TrackSelectionDialogFragment
import dev.jdtech.jellyfin.mpv.MPVPlayer
import dev.jdtech.jellyfin.mpv.TrackType
import dev.jdtech.jellyfin.utils.PlayerGestureViewControl
import dev.jdtech.jellyfin.utils.PreviewScrubListener
import dev.jdtech.jellyfin.utils.brightness.createBrightnessControl
import dev.jdtech.jellyfin.utils.gestureDSL.actions.handlers.DoubleTapActionHandler.DoubleTapArea
import dev.jdtech.jellyfin.utils.gestureDSL.actions.handlers.PlayerGesturePrefs
import dev.jdtech.jellyfin.utils.installPlayerGestureHandler
import dev.jdtech.jellyfin.utils.system.DefaultSystemControl
import dev.jdtech.jellyfin.utils.system.volume.createVolumeControl
import dev.jdtech.jellyfin.viewmodels.PlayerActivityViewModel
import dev.jdtech.jellyfin.viewmodels.PlayerEvents
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import dev.jdtech.jellyfin.player.video.R as PlayerVideoR

var isControlsLocked: Boolean = false

@AndroidEntryPoint
class PlayerActivity :
    BasePlayerActivity(), PlayerGestureViewControl {
    @Inject
    lateinit var appPreferences: AppPreferences

    override val useController: Boolean
        get() = binding.playerView.useController

    private val systemControl by lazy {
        if (appPreferences.playerBrightnessRemember) {
            window.attributes.screenBrightness = appPreferences.playerBrightness
        }
        DefaultSystemControl(
            createBrightnessControl(),
            createVolumeControl(),
        )
    }

    private val gesturePrefs by lazy {
        PlayerGesturePrefs(
            enableHorizontalSwipeGesture = appPreferences.playerGesturesSeek && isControlsLocked.not(),
            enableRightVerticalSwipeGesture = appPreferences.playerGesturesVB && isControlsLocked.not(),
            enableLeftVerticalSwipeGesture = appPreferences.playerGesturesVB && isControlsLocked.not(),
            enableZoomGesture = appPreferences.playerGesturesZoom && isControlsLocked.not(),
            enableDoubleTapAction = isControlsLocked.not(),
        )
    }

    lateinit var binding: ActivityPlayerBinding

    override val viewModel: PlayerActivityViewModel by viewModels()

    private var previewScrubListener: PreviewScrubListener? = null

    private val isPipSupported by lazy {
        // Check if device has PiP feature
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            return@lazy false
        }

        // Check if PiP is enabled for the app
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps?.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                Process.myUid(),
                packageName,
            ) == AppOpsManager.MODE_ALLOWED
        } else {
            @Suppress("DEPRECATION")
            appOps?.checkOpNoThrow(
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                Process.myUid(),
                packageName,
            ) == AppOpsManager.MODE_ALLOWED
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args: PlayerActivityArgs by navArgs()

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding.playerView.player = viewModel.player
        binding.playerView.setControllerVisibilityListener(
            PlayerView.ControllerVisibilityListener { visibility ->
                if (visibility == View.GONE) {
                    hideSystemUI()
                }
            },
        )

        val playerControls = binding.playerView.findViewById<View>(R.id.player_controls)
        val lockedControls = binding.playerView.findViewById<View>(R.id.locked_player_view)

        isControlsLocked = false

        configureInsets(playerControls)
        configureInsets(lockedControls)

        if (appPreferences.playerGestures) {
            binding.playerView.installPlayerGestureHandler(
                playerGestureViewControl = this,
                playerPlaybackControl = viewModel.playerPlaybackControl,
                systemControl = systemControl,
                prefs = gesturePrefs,
            )
        }

        binding.playerView.findViewById<View>(R.id.back_button).setOnClickListener {
            finish()
        }

        binding.playerView.findViewById<View>(R.id.back_button_alt).setOnClickListener {
            finish()
        }

        val videoNameTextView = binding.playerView.findViewById<TextView>(R.id.video_name)

        val audioButton = binding.playerView.findViewById<ImageButton>(R.id.btn_audio_track)
        val subtitleButton = binding.playerView.findViewById<ImageButton>(R.id.btn_subtitle)
        val speedButton = binding.playerView.findViewById<ImageButton>(R.id.btn_speed)
        val skipIntroButton = binding.playerView.findViewById<Button>(R.id.btn_skip_intro)
        val pipButton = binding.playerView.findViewById<ImageButton>(R.id.btn_pip)
        val lockButton = binding.playerView.findViewById<ImageButton>(R.id.btn_lockview)
        val unlockButton = binding.playerView.findViewById<ImageButton>(R.id.btn_unlock)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { uiState ->
                        Timber.d("$uiState")
                        uiState.apply {
                            // Title
                            videoNameTextView.text = currentItemTitle

                            // Skip Intro button
                            skipIntroButton.isVisible =
                                !isInPictureInPictureMode && currentIntro != null
                            skipIntroButton.setOnClickListener {
                                currentIntro?.let {
                                    viewModel.seeker.seekTo(seconds = it.introEnd)
                                }
                            }

                            // Trick Play
                            previewScrubListener?.let {
                                it.currentTrickPlay = currentTrickPlay
                            }

                            // File Loaded
                            if (fileLoaded) {
                                audioButton.isEnabled = true
                                audioButton.imageAlpha = 255
                                lockButton.isEnabled = true
                                lockButton.imageAlpha = 255
                                subtitleButton.isEnabled = true
                                subtitleButton.imageAlpha = 255
                                speedButton.isEnabled = true
                                speedButton.imageAlpha = 255
                                pipButton.isEnabled = true
                                pipButton.imageAlpha = 255
                            }
                        }
                    }
                }

                launch {
                    viewModel.eventsChannelFlow.collect { event ->
                        when (event) {
                            is PlayerEvents.NavigateBack -> finish()
                        }
                    }
                }
            }
        }

        audioButton.isEnabled = false
        audioButton.imageAlpha = 75

        lockButton.isEnabled = false
        lockButton.imageAlpha = 75

        subtitleButton.isEnabled = false
        subtitleButton.imageAlpha = 75

        speedButton.isEnabled = false
        speedButton.imageAlpha = 75

        if (isPipSupported) {
            pipButton.isEnabled = false
            pipButton.imageAlpha = 75
        } else {
            val pipSpace = binding.playerView.findViewById<Space>(R.id.space_pip)
            pipButton.isVisible = false
            pipSpace.isVisible = false
        }

        audioButton.setOnClickListener {
            when (viewModel.player) {
                is MPVPlayer -> {
                    TrackSelectionDialogFragment(TrackType.AUDIO, viewModel).show(
                        supportFragmentManager,
                        "trackselectiondialog",
                    )
                }

                is ExoPlayer -> {
                    val mappedTrackInfo =
                        viewModel.trackSelector.currentMappedTrackInfo ?: return@setOnClickListener

                    var audioRenderer: Int? = null
                    for (i in 0 until mappedTrackInfo.rendererCount) {
                        if (isRendererType(mappedTrackInfo, i, C.TRACK_TYPE_AUDIO)) {
                            audioRenderer = i
                        }
                    }

                    if (audioRenderer == null) return@setOnClickListener

                    val trackSelectionDialogBuilder =
                        TrackSelectionDialogBuilder(
                            this,
                            resources.getString(PlayerVideoR.string.select_audio_track),
                            viewModel.player,
                            C.TRACK_TYPE_AUDIO,
                        )
                    val trackSelectionDialog = trackSelectionDialogBuilder.build()
                    trackSelectionDialog.show()
                }
            }
        }

        val exoPlayerControlView = findViewById<FrameLayout>(R.id.player_controls)
        val lockedLayout = findViewById<FrameLayout>(R.id.locked_player_view)

        lockButton.setOnClickListener {
            exoPlayerControlView.visibility = View.GONE
            lockedLayout.visibility = View.VISIBLE
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
            isControlsLocked = true
        }

        unlockButton.setOnClickListener {
            exoPlayerControlView.visibility = View.VISIBLE
            lockedLayout.visibility = View.GONE
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            isControlsLocked = false
        }

        subtitleButton.setOnClickListener {
            when (viewModel.player) {
                is MPVPlayer -> {
                    TrackSelectionDialogFragment(TrackType.SUBTITLE, viewModel).show(
                        supportFragmentManager,
                        "trackselectiondialog",
                    )
                }

                is ExoPlayer -> {
                    val mappedTrackInfo =
                        viewModel.trackSelector.currentMappedTrackInfo ?: return@setOnClickListener

                    var subtitleRenderer: Int? = null
                    for (i in 0 until mappedTrackInfo.rendererCount) {
                        if (isRendererType(mappedTrackInfo, i, C.TRACK_TYPE_TEXT)) {
                            subtitleRenderer = i
                        }
                    }

                    if (subtitleRenderer == null) return@setOnClickListener

                    val trackSelectionDialogBuilder =
                        TrackSelectionDialogBuilder(
                            this,
                            resources.getString(PlayerVideoR.string.select_subtile_track),
                            viewModel.player,
                            C.TRACK_TYPE_TEXT,
                        )
                    trackSelectionDialogBuilder.setShowDisableOption(true)

                    val trackSelectionDialog = trackSelectionDialogBuilder.build()
                    trackSelectionDialog.show()
                }
            }
        }

        speedButton.setOnClickListener {
            SpeedSelectionDialogFragment(viewModel).show(
                supportFragmentManager,
                "speedselectiondialog",
            )
        }

        pipButton.setOnClickListener {
            pictureInPicture()
        }

        if (appPreferences.playerTrickPlay) {
            val imagePreview = binding.playerView.findViewById<ImageView>(R.id.image_preview)
            val timeBar = binding.playerView.findViewById<DefaultTimeBar>(R.id.exo_progress)
            previewScrubListener =
                PreviewScrubListener(
                    imagePreview,
                    timeBar,
                    viewModel.player,
                )

            timeBar.addListener(previewScrubListener!!)
        }

        viewModel.initializePlayer(args.items)
        hideSystemUI()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)

        val args: PlayerActivityArgs by navArgs()
        viewModel.initializePlayer(args.items)
    }

    override fun onUserLeaveHint() {
        if (appPreferences.playerPipGesture && viewModel.player.isPlaying && !isControlsLocked) {
            pictureInPicture()
        }
    }

    private fun pipParams(): PictureInPictureParams {
        val displayAspectRatio = Rational(binding.playerView.width, binding.playerView.height)

        val aspectRatio =
            binding.playerView.player?.videoSize?.let {
                Rational(
                    it.width.coerceAtMost((it.height * 2.39f).toInt()),
                    it.height.coerceAtMost((it.width * 2.39f).toInt()),
                )
            }

        val sourceRectHint =
            if (displayAspectRatio < aspectRatio!!) {
                val space =
                    ((binding.playerView.height - (binding.playerView.width.toFloat() / aspectRatio.toFloat())) / 2).toInt()
                Rect(
                    0,
                    space,
                    binding.playerView.width,
                    (binding.playerView.width.toFloat() / aspectRatio.toFloat()).toInt() + space,
                )
            } else {
                val space =
                    ((binding.playerView.width - (binding.playerView.height.toFloat() * aspectRatio.toFloat())) / 2).toInt()
                Rect(
                    space,
                    0,
                    (binding.playerView.height.toFloat() * aspectRatio.toFloat()).toInt() + space,
                    binding.playerView.height,
                )
            }

        return PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .setSourceRectHint(sourceRectHint)
            .build()
    }

    private fun pictureInPicture() {
        if (!isPipSupported) {
            return
        }
        binding.playerView.useController = false
        binding.playerView.findViewById<Button>(R.id.btn_skip_intro).isVisible = false

        if (binding.playerView.player is MPVPlayer) {
            (binding.playerView.player as MPVPlayer).updateZoomMode(false)
        } else {
            binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        }

        try {
            enterPictureInPictureMode(pipParams())
        } catch (_: IllegalArgumentException) {
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isInPictureInPictureMode) {
            binding.playerView.useController = true
        }
    }

    private val hideGestureVolumeIndicatorOverlayAction =
        Runnable {
            binding.gestureVolumeLayout.visibility = View.GONE
        }

    override fun hideVolumeLayout() {
        binding.gestureVolumeLayout.apply {
            if (visibility == View.VISIBLE) {
                removeCallbacks(hideGestureVolumeIndicatorOverlayAction)
                postDelayed(hideGestureVolumeIndicatorOverlayAction, 1000)
            }
        }
    }

    override fun showVolumeLayout() {
        binding.gestureVolumeLayout.visibility = View.VISIBLE
    }

    override fun setVolumeProgress(
        maxVolume: Int,
        currentVolume: Int,
    ) {
        binding.gestureVolumeProgressBar.max = maxVolume
        binding.gestureVolumeProgressBar.progress = currentVolume
    }

    override fun setVolumeText(volumeText: String) {
        binding.gestureVolumeText.text = volumeText
    }

    override fun setVolumeImageLevel(level: Int) {
        binding.gestureVolumeImage.setImageLevel(level)
    }

    private val hideGestureProgressOverlayAction =
        Runnable {
            binding.progressScrubberLayout.visibility = View.GONE
        }

    override fun hideSwipeSeekLayout() {
        binding.progressScrubberLayout.apply {
            if (visibility == View.VISIBLE) {
                removeCallbacks(hideGestureProgressOverlayAction)
                postDelayed(hideGestureProgressOverlayAction, 1000)
            }
        }
    }

    override fun showSwipeSeekLayout() {
        binding.progressScrubberLayout.visibility = View.VISIBLE
    }

    override fun setSwipeSeekText(swipeSeekText: String) {
        binding.progressScrubberText.text = swipeSeekText
    }

    private val hideGestureBrightnessIndicatorOverlayAction =
        Runnable {
            binding.gestureBrightnessLayout.visibility = View.GONE
            if (appPreferences.playerBrightnessRemember) {
                appPreferences.playerBrightness = window.attributes.screenBrightness
            }
        }

    override fun hideBrightnessLayout() {
        binding.gestureBrightnessLayout.apply {
            if (visibility == View.VISIBLE) {
                removeCallbacks(hideGestureBrightnessIndicatorOverlayAction)
                postDelayed(hideGestureBrightnessIndicatorOverlayAction, 1000)
            }
        }
    }

    override fun showBrightnessLayout() {
        binding.gestureBrightnessLayout.visibility = View.VISIBLE
    }

    override fun setBrightnessProgress(
        maxBrightness: Float,
        currentBrightness: Float,
    ) {
        binding.gestureBrightnessProgressBar.max = maxBrightness.toInt()
        binding.gestureBrightnessProgressBar.progress = currentBrightness.toInt()
    }

    override fun setBrightnessText(brightnessText: String) {
        binding.gestureBrightnessText.text = brightnessText
    }

    override fun setBrightnessImageLevel(level: Int) {
        binding.gestureBrightnessImage.setImageLevel(level)
    }

    override fun showDoubleTapReaction(doubleTapArea: DoubleTapArea) {
        val reactionView =
            when (doubleTapArea) {
                DoubleTapArea.RightmostArea -> binding.imageFfwdAnimationRipple

                DoubleTapArea.LeftmostArea -> binding.imageRewindAnimationRipple

                DoubleTapArea.MiddleArea -> binding.imagePlaybackAnimationRipple
            }
        animateRipple(reactionView)
    }

    private fun animateRipple(image: ImageView) {
        image
            .animateSeekingRippleStart()
            .withEndAction {
                resetRippleImage(image)
            }
            .start()
    }

    private fun ImageView.animateSeekingRippleStart(): ViewPropertyAnimator {
        val rippleImageHeight = this.height
        val playerViewHeight = rootView.height.toFloat()
        val playerViewWidth = rootView.width.toFloat()
        val scaleDifference = playerViewHeight / rippleImageHeight
        val playerViewAspectRatio = playerViewWidth / playerViewHeight
        val scaleValue = scaleDifference * playerViewAspectRatio
        return animate()
            .alpha(1f)
            .scaleX(scaleValue)
            .scaleY(scaleValue)
            .setDuration(180)
            .setInterpolator(DecelerateInterpolator())
    }

    private fun resetRippleImage(image: ImageView) {
        image
            .animateSeekingRippleEnd()
            .withEndAction {
                image.scaleX = 1f
                image.scaleY = 1f
            }
            .start()
    }

    private fun ImageView.animateSeekingRippleEnd() =
        animate()
            .alpha(0f)
            .setDuration(150)
            .setInterpolator(AccelerateInterpolator())

    override fun showHideController() =
        with(binding.playerView) {
            if (!isControllerFullyVisible) showController() else hideController()
        }

    override fun updateZoomMode(zoom: Boolean) {
        if (viewModel.player is MPVPlayer) {
            (viewModel.player as MPVPlayer).updateZoomMode(zoom)
        } else {
            binding.playerView.resizeMode =
                if (zoom) {
                    AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                } else {
                    AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
        }
    }
}
