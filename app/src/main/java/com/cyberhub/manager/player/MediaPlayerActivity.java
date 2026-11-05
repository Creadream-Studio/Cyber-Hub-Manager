package com.cyberhub.manager.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import com.cyberhub.manager.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MediaPlayerActivity extends Activity implements
        PlayerManager.PlaybackCallback,
        PlayerManager.VideoSizeChangedListener,
        PlayerManager.SubtitleListener,
        SurfaceHolder.Callback {

    private PlayerManager playerManager;
    private SurfaceView surfaceView;
    private FrameLayout videoContainer;
    private LinearLayout artworkContainer, abRepeatBar, controlsContainer;
    private View toolbar;
    private ImageView artworkView;
    private TextView trackTitle, trackArtist, trackAlbum, currentTime, totalTime, speedLabel, errorMessage, bufferingIndicator, subtitleView;
    private ImageButton btnPlayPause, btnNext, btnPrevious, btnRewind, btnFastForward, btnRepeat, btnShuffle, btnMute, btnQueue, btnLock, btnSettings, btnAspect, btnUnlock;
    private Button btnSetA, btnSetB;
    private ImageButton btnClearAB;
    private SeekBar seekBar, speedSeekBar, volumeSeekBar;
    private ListView queueList;
    private PlayerQueueAdapter queueAdapter;
    private boolean controlsLocked;
    private boolean controlsVisible = true;
    private boolean userIsSeeking;
    private boolean pausedForBackground;
    private int videoW;
    private int videoH;
    private int aspectMode;
    private int fitRetryCount;
    private SharedPreferences prefs;
    private GestureDetector gestureDetector;
    private View seekOverlay;
    private TextView seekDeltaText, seekTargetText;
    private boolean isSwipeSeeking;
    private float touchStartX, touchStartY;
    private int seekStartPosition, seekTargetPosition;
    private VelocityTracker velocityTracker;
    private final Handler autoHideHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoHideRunnable = this::hideControls;
    private final Runnable autoHideUnlockRunnable = this::hideUnlock;

    public static void open(Activity activity) {
        Intent intent = new Intent(activity, MediaPlayerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    public static void openAndPlay(Activity activity, String filePath) {
        Intent intent = new Intent(activity, MediaPlayerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("play_path", filePath);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);

        playerManager = PlayerManager.getInstance(this);
        prefs = getSharedPreferences("player", MODE_PRIVATE);
        playerManager.registerCallback(this);
        playerManager.setVideoSizeChangedListener(this);
        playerManager.registerSubtitleListener(this);

        initViews();
        setupControls();
        setupSeekBar();
        setupVideoSurface();
        setupQueueList();

        String playPath = getIntent().getStringExtra("play_path");
        if (playPath != null) {
            playPathWithSiblings(playPath);
        } else if (playerManager.getState() == PlayerManager.PlayState.IDLE && playerManager.getQueue().isEmpty()) {
            finish();
        }

        updateUIForCurrentItem();
        updateControlsState();
        updatePlayPauseIcon();
    }

    private void initViews() {
        surfaceView = findViewById(R.id.surfaceView);
        videoContainer = findViewById(R.id.videoContainer);
        artworkContainer = findViewById(R.id.artworkContainer);
        abRepeatBar = findViewById(R.id.abRepeatBar);
        controlsContainer = findViewById(R.id.controlsContainer);
        artworkView = findViewById(R.id.artworkView);
        trackTitle = findViewById(R.id.trackTitle);
        trackArtist = findViewById(R.id.trackArtist);
        trackAlbum = findViewById(R.id.trackAlbum);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);
        speedLabel = findViewById(R.id.speedLabel);
        errorMessage = findViewById(R.id.errorMessage);
        bufferingIndicator = findViewById(R.id.bufferingIndicator);
        subtitleView = findViewById(R.id.subtitleView);
        applySubtitleStyle();
        seekOverlay = findViewById(R.id.seekOverlay);
        seekDeltaText = findViewById(R.id.seekDeltaText);
        seekTargetText = findViewById(R.id.seekTargetText);
        toolbar = findViewById(R.id.toolbar);

        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnRewind = findViewById(R.id.btnRewind);
        btnFastForward = findViewById(R.id.btnFastForward);
        btnRepeat = findViewById(R.id.btnRepeat);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnMute = findViewById(R.id.btnMute);
        btnQueue = findViewById(R.id.btnQueue);
        btnLock = findViewById(R.id.btnLock);
        btnSettings = findViewById(R.id.btnSettings);
        btnAspect = findViewById(R.id.btnAspect);
        btnUnlock = findViewById(R.id.btnUnlock);
        btnSetA = findViewById(R.id.btnSetA);
        btnSetB = findViewById(R.id.btnSetB);
        btnClearAB = findViewById(R.id.btnClearAB);

        seekBar = findViewById(R.id.seekBar);
        speedSeekBar = findViewById(R.id.speedSeekBar);
        volumeSeekBar = findViewById(R.id.volumeSeekBar);
        queueList = findViewById(R.id.queueList);
        videoW = playerManager.getLastVideoWidth();
        videoH = playerManager.getLastVideoHeight();
        aspectMode = prefs.getInt("video_aspect_mode", 0);
        videoContainer.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, orr, ob) -> {
            if (r - l != orr - ol || b - t != ob - ot) fitVideo();
        });
        videoContainer.post(this::fitVideo);
    }

    private void setupControls() {
        btnPlayPause.setOnClickListener(v -> playerManager.playPause());
        btnNext.setOnClickListener(v -> { playerManager.next(); showControlsTemporarily(); });
        btnPrevious.setOnClickListener(v -> { playerManager.previous(); showControlsTemporarily(); });
        btnRewind.setOnClickListener(v -> { playerManager.skipBackward(); showControlsTemporarily(); });
        btnFastForward.setOnClickListener(v -> { playerManager.skipForward(); showControlsTemporarily(); });

        btnRepeat.setOnClickListener(v -> {
            PlayerManager.RepeatMode[] modes = PlayerManager.RepeatMode.values();
            int next = (playerManager.getRepeatMode().ordinal() + 1) % modes.length;
            playerManager.setRepeatMode(modes[next]);
            updateRepeatIcon();
        });

        btnShuffle.setOnClickListener(v -> {
            playerManager.setShuffle(!playerManager.isShuffle());
            btnShuffle.setAlpha(playerManager.isShuffle() ? 1.0f : 0.4f);
        });

        btnMute.setOnClickListener(v -> {
            playerManager.setMuted(!playerManager.isMuted());
            btnMute.setImageResource(playerManager.isMuted()
                    ? android.R.drawable.ic_lock_silent_mode
                    : android.R.drawable.ic_lock_silent_mode_off);
        });

        btnLock.setOnClickListener(v -> setLocked(!controlsLocked));

        btnUnlock.setOnClickListener(v -> setLocked(false));

        btnAspect.setOnClickListener(v -> cycleAspectMode());

        btnSettings.setOnClickListener(v -> showPlayerSettings());

        btnQueue.setOnClickListener(v -> toggleQueue());
    }

    private void setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean u) {
                if (u) {
                    int dur = playerManager.getDuration();
                    if (dur > 0) currentTime.setText(formatTime((long) p * dur / 1000));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { userIsSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                int dur = playerManager.getDuration();
                if (dur > 0) playerManager.seekTo(sb.getProgress() * dur / 1000);
                userIsSeeking = false;
                showControlsTemporarily();
            }
        });

        btnSetA.setOnClickListener(v -> {
            int pos = playerManager.getCurrentPosition();
            playerManager.setABRepeat(pos, playerManager.getABRepeatB() >= 0 ? playerManager.getABRepeatB() : pos);
            btnSetA.setText("A:" + formatTime(pos));
            abRepeatBar.setVisibility(View.VISIBLE);
        });
        btnSetB.setOnClickListener(v -> {
            int pos = playerManager.getCurrentPosition();
            int a = playerManager.getABRepeatA() >= 0 ? playerManager.getABRepeatA() : pos;
            playerManager.setABRepeat(a, pos);
            btnSetB.setText("B:" + formatTime(pos));
            abRepeatBar.setVisibility(View.VISIBLE);
        });
        btnClearAB.setOnClickListener(v -> {
            playerManager.clearABRepeat();
            abRepeatBar.setVisibility(View.GONE);
        });

        speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean u) {
                if (u) {
                    float speed = Math.max(0.25f, p / 100f);
                    playerManager.setPlaybackSpeed(speed);
                    speedLabel.setText(String.format("%.1fx", speed));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean u) {
                if (u) playerManager.setVolume(p / 100f, p / 100f);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        setupGestureControls();
    }

    private void setupGestureControls() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (controlsLocked) return false;
                float screenWidth = getWindow().getDecorView().getWidth();
                if (e.getX() < screenWidth / 3f) {
                    playerManager.skipBackward();
                } else if (e.getX() > screenWidth * 2f / 3f) {
                    playerManager.skipForward();
                } else {
                    playerManager.playPause();
                }
                showControlsTemporarily();
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (playerManager.isPlayingVideo()) {
                    if (controlsLocked) {
                        showUnlockOnly();
                        return true;
                    }
                    if (controlsVisible) hideControls();
                    else showControlsTemporarily();
                }
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                if (controlsLocked) return false;
                float dx = e2.getX() - e1.getX();
                if (Math.abs(dx) > Math.abs(vY * 3) && Math.abs(dx) > 100) {
                    if (dx > 0) playerManager.skipBackward();
                    else playerManager.skipForward();
                    showControlsTemporarily();
                    return true;
                }
                return false;
            }
        });

        videoContainer.setOnTouchListener((v, event) -> {
            if (controlsLocked) {
                gestureDetector.onTouchEvent(event);
                return true;
            }
            if (velocityTracker == null) {
                velocityTracker = VelocityTracker.obtain();
            }
            velocityTracker.addMovement(event);

            int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    touchStartX = event.getX();
                    touchStartY = event.getY();
                    seekStartPosition = playerManager.getCurrentPosition();
                    seekTargetPosition = seekStartPosition;
                    isSwipeSeeking = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getX() - touchStartX;
                    float dy = event.getY() - touchStartY;
                    if (!isSwipeSeeking) {
                        float touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
                        if (Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                            isSwipeSeeking = true;
                            if (controlsVisible) hideControls();
                            if (seekOverlay != null) seekOverlay.setVisibility(View.VISIBLE);
                        }
                    }
                    if (isSwipeSeeking) {
                        velocityTracker.computeCurrentVelocity(1000);
                        float vx = velocityTracker.getXVelocity();
                        float speedFactor = 1.0f + Math.min(Math.abs(vx) / 600f, 4.0f);
                        int duration = playerManager.getDuration();
                        float screenWidth = Math.max(1, v.getWidth());
                        float progressFraction = (dx / screenWidth) * speedFactor;
                        long deltaMs = (long) (progressFraction * Math.min(duration > 0 ? duration : 300000, 180000));
                        int target = (int) Math.max(0, Math.min(duration > 0 ? duration : Integer.MAX_VALUE, seekStartPosition + deltaMs));
                        seekTargetPosition = target;

                        // Seek MediaPlayer in real-time so the frame at the target time renders on screen
                        playerManager.seekTo(target);

                        if (seekOverlay != null) {
                            seekOverlay.setVisibility(View.VISIBLE);
                            long diffMs = target - seekStartPosition;
                            String sign = diffMs >= 0 ? "+" : "-";
                            long absDiff = Math.abs(diffMs);
                            seekDeltaText.setText(sign + formatTime(absDiff));
                            seekTargetText.setText(formatTime(target) + " / " + formatTime(duration));
                        }
                        return true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isSwipeSeeking) {
                        isSwipeSeeking = false;
                        if (seekOverlay != null) seekOverlay.setVisibility(View.GONE);
                        playerManager.seekTo(seekTargetPosition);
                        if (action == MotionEvent.ACTION_UP && playerManager.getState() != PlayerManager.PlayState.PLAYING) {
                            playerManager.play();
                        }
                        showControlsTemporarily();
                        if (velocityTracker != null) {
                            velocityTracker.recycle();
                            velocityTracker = null;
                        }
                        return true;
                    }
                    if (velocityTracker != null) {
                        velocityTracker.recycle();
                        velocityTracker = null;
                    }
                    break;
            }

            gestureDetector.onTouchEvent(event);
            return true;
        });
        videoContainer.setClickable(true);
        videoContainer.setFocusable(true);
    }

    private void showControls() {
        if (controlsLocked) return;
        controlsVisible = true;
        toolbar.setVisibility(View.VISIBLE);
        controlsContainer.setVisibility(View.VISIBLE);
        if (playerManager.isPlayingVideo()) {
            autoHideHandler.removeCallbacks(autoHideRunnable);
            autoHideHandler.postDelayed(autoHideRunnable, 5000);
        }
    }

    private void hideControls() {
        controlsVisible = false;
        toolbar.setVisibility(View.GONE);
        controlsContainer.setVisibility(View.GONE);
        queueList.setVisibility(View.GONE);
        autoHideHandler.removeCallbacks(autoHideRunnable);
        hideUnlock();
    }

    private void showUnlockOnly() {
        controlsVisible = false;
        toolbar.setVisibility(View.GONE);
        controlsContainer.setVisibility(View.GONE);
        queueList.setVisibility(View.GONE);
        btnUnlock.setVisibility(View.VISIBLE);
        autoHideHandler.removeCallbacks(autoHideRunnable);
        autoHideHandler.removeCallbacks(autoHideUnlockRunnable);
        autoHideHandler.postDelayed(autoHideUnlockRunnable, 3000);
    }

    private void hideUnlock() {
        autoHideHandler.removeCallbacks(autoHideUnlockRunnable);
        if (btnUnlock != null) btnUnlock.setVisibility(View.GONE);
    }

    private void setLocked(boolean locked) {
        controlsLocked = locked;
        hideUnlock();
        btnLock.setImageResource(locked
                ? android.R.drawable.ic_secure
                : android.R.drawable.ic_partial_secure);
        btnLock.setColorFilter(locked ? 0xFFFF4444 : 0xFFFFFFFF);
        if (locked) hideControls();
        else showControlsTemporarily();
    }

    private void cycleAspectMode() {
        aspectMode = (aspectMode + 1) % 4;
        prefs.edit().putInt("video_aspect_mode", aspectMode).apply();
        fitVideo();
        Toast.makeText(this, "画面比例: " + aspectName(), Toast.LENGTH_SHORT).show();
        showControlsTemporarily();
    }

    private String aspectName() {
        switch (aspectMode) {
            case 1: return "填充";
            case 2: return "16:9";
            case 3: return "4:3";
            default: return "适应";
        }
    }

    private void fitVideo() {
        if (videoW <= 0 || videoH <= 0 || surfaceView == null || videoContainer == null) return;
        if (videoContainer.getVisibility() != View.VISIBLE || !playerManager.isPlayingVideo()) return;
        int cw = videoContainer.getWidth();
        int ch = videoContainer.getHeight();
        if (cw <= 0 || ch <= 0) {
            if (fitRetryCount++ < 20) videoContainer.postDelayed(this::fitVideo, 100);
            return;
        }
        fitRetryCount = 0;
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) surfaceView.getLayoutParams();
        if (aspectMode == 1) {
            params.width = FrameLayout.LayoutParams.MATCH_PARENT;
            params.height = FrameLayout.LayoutParams.MATCH_PARENT;
        } else {
            float target = aspectMode == 2 ? 16f / 9f : aspectMode == 3 ? 4f / 3f : (float) videoW / videoH;
            float containerRatio = (float) cw / ch;
            if (target > containerRatio) {
                params.width = FrameLayout.LayoutParams.MATCH_PARENT;
                params.height = (int) (cw / target);
            } else {
                params.height = FrameLayout.LayoutParams.MATCH_PARENT;
                params.width = (int) (ch * target);
            }
        }
        surfaceView.setLayoutParams(params);
    }

    private void showControlsTemporarily() {
        showControls();
    }

    private void toggleQueue() {
        if (queueList.getVisibility() == View.VISIBLE) {
            queueList.setVisibility(View.GONE);
            showControlsTemporarily();
        } else {
            queueList.setVisibility(View.VISIBLE);
            if (!controlsVisible) showControlsTemporarily();
        }
    }

    private void updateRepeatIcon() {
        switch (playerManager.getRepeatMode()) {
            case OFF:
                btnRepeat.setAlpha(0.4f);
                btnRepeat.setImageResource(android.R.drawable.ic_menu_rotate);
                break;
            case ONE:
                btnRepeat.setAlpha(1.0f);
                btnRepeat.setImageResource(android.R.drawable.ic_menu_mylocation);
                break;
            case ALL:
                btnRepeat.setAlpha(1.0f);
                btnRepeat.setImageResource(android.R.drawable.ic_menu_rotate);
                break;
        }
    }

    private void setupVideoSurface() {
        surfaceView.getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        playerManager.setSurface(holder.getSurface());
        if (playerManager.getState() == PlayerManager.PlayState.ERROR || playerManager.getState() == PlayerManager.PlayState.STOPPED) {
            playerManager.play();
        }
        surfaceView.post(this::fitVideo);
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        fitVideo();
    }
    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        playerManager.setSurface(null);
    }

    private void setupQueueList() {
        updateQueueAdapter();
        queueList.setVisibility(View.GONE);
    }

    private void updateQueueAdapter() {
        queueAdapter = new PlayerQueueAdapter(playerManager.getQueue(), playerManager.getCurrentIndex(),
                new PlayerQueueAdapter.OnItemClickListener() {
                    @Override public void onItemClick(int index) {
                        playerManager.addAndPlay(index);
                        queueList.setVisibility(View.GONE);
                        showControlsTemporarily();
                    }
                    @Override public void onRemoveClick(int index) {
                        playerManager.removeFromQueue(index);
                        updateQueueAdapter();
                    }
                });
        queueList.setAdapter(queueAdapter);
    }

    @Override
    public void onStateChanged(PlayerManager.PlayState newState) {
        runOnUiThread(() -> {
            updatePlayPauseIcon();
            switch (newState) {
                case PLAYING:
                    errorMessage.setVisibility(View.GONE);
                    bufferingIndicator.setVisibility(View.GONE);
                    showControlsTemporarily();
                    break;
                case PREPARING:
                    bufferingIndicator.setVisibility(View.VISIBLE);
                    break;
                case ERROR:
                    errorMessage.setVisibility(View.VISIBLE);
                    errorMessage.setText("播放出错");
                    showControls();
                    break;
                case ENDED:
                    break;
                default: break;
            }
            updateQueueAdapter();
        });
    }

    private void updatePlayPauseIcon() {
        PlayerManager.PlayState s = playerManager.getState();
        btnPlayPause.setImageResource(s == PlayerManager.PlayState.PLAYING ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    @Override
    public void onProgress(int position, int duration) {
        runOnUiThread(() -> {
            currentTime.setText(formatTime(position));
            totalTime.setText(formatTime(duration));
            if (!userIsSeeking && duration > 0) {
                seekBar.setProgress((int) ((long) position * 1000 / duration));
            }
            if (playerManager.hasABRepeat()) {
                int ab = (int) ((long) position * 1000 / Math.max(duration, 1));
                ((SeekBar) findViewById(R.id.abSeekBar)).setProgress(ab);
            }
        });
    }

    @Override
    public void onMediaItemChanged(MediaItem item) {
        runOnUiThread(() -> {
            updateUIForCurrentItem();
            updateQueueAdapter();
        });
    }

    private void updateUIForCurrentItem() {
        MediaItem item = playerManager.getCurrentItem();
        if (item == null) return;

        boolean isVideo = item.isVideo;
        videoContainer.setVisibility(isVideo ? View.VISIBLE : View.GONE);
        artworkContainer.setVisibility(isVideo ? View.GONE : View.VISIBLE);

        if (isVideo) {
            showControlsTemporarily();
        } else {
            showControls();
        }

        if (!isVideo) {
            trackTitle.setText(item.title != null ? item.title : "未知媒体");
            trackArtist.setText(item.artist != null ? item.artist : "未知艺术家");
            trackAlbum.setText(item.album != null ? item.album : "");

            if (item.path != null) try (MediaMetadataRetriever mmr = new MediaMetadataRetriever()) {
                mmr.setDataSource(this, item.uri);
                byte[] art = mmr.getEmbeddedPicture();
                if (art != null) artworkView.setImageBitmap(BitmapFactory.decodeByteArray(art, 0, art.length));
                else artworkView.setImageResource(android.R.drawable.ic_menu_gallery);
                mmr.release();
            } catch (Exception e) {
                artworkView.setImageResource(android.R.drawable.ic_menu_gallery);
            }
        }

        updateKeepScreenOn();
        updateRepeatIcon();
        updatePlayPauseIcon();
        btnShuffle.setAlpha(playerManager.isShuffle() ? 1.0f : 0.4f);

        int vol = Math.round(playerManager.isMuted() ? 0 : 100);
        volumeSeekBar.setProgress(vol);

        float speed = playerManager.getPlaybackSpeed();
        speedSeekBar.setProgress((int) (speed * 100));
        speedLabel.setText(String.format("%.1fx", speed));
    }

    private void updateControlsState() {
        PlayerManager.PlayState state = playerManager.getState();
        boolean enabled = state != PlayerManager.PlayState.IDLE && state != PlayerManager.PlayState.STOPPED;
        controlsContainer.setEnabled(enabled);
    }

    private void updateKeepScreenOn() {
        if (playerManager.isPlayingVideo() || playerManager.isKeepScreenOn()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> {
            errorMessage.setVisibility(View.VISIBLE);
            errorMessage.setText(message);
            showControls();
        });
    }

    @Override
    public void onBufferUpdate(int percent) {
        runOnUiThread(() -> {
            if (percent < 100) {
                bufferingIndicator.setVisibility(View.VISIBLE);
                bufferingIndicator.setText("缓冲中 " + percent + "%");
            } else {
                bufferingIndicator.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onVideoSizeChanged(int width, int height) {
        runOnUiThread(() -> {
            if (width > 0 && height > 0) {
                videoW = width;
                videoH = height;
                fitVideo();
            }
        });
    }

    @Override
    public void onSubtitleCue(String text) {
        runOnUiThread(() -> {
            if (subtitleView == null) return;
            if (text == null || text.isEmpty()) {
                subtitleView.setVisibility(View.GONE);
            } else {
                // Multi-line cues: TextView handles \n natively.
                subtitleView.setText(text);
                subtitleView.setVisibility(View.VISIBLE);
            }
        });
    }

    /** Applies persisted subtitle style (size/color/bold/background). */
    private void applySubtitleStyle() {
        if (subtitleView == null) return;
        int sizeSp = prefs.getInt("subtitle_text_size", 18);
        int color = prefs.getInt("subtitle_text_color", Color.WHITE);
        boolean bold = prefs.getBoolean("subtitle_bold", true);
        int bgMode = prefs.getInt("subtitle_bg", 0);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, Math.max(10, Math.min(40, sizeSp)));
        subtitleView.setTextColor(color);
        subtitleView.setTypeface(null, bold ? Typeface.BOLD : Typeface.NORMAL);
        subtitleView.setShadowLayer(3f, 1f, 1f, Color.BLACK);
        switch (bgMode) {
            case 1:
                subtitleView.setBackgroundColor(0x99000000);
                break;
            case 2:
                subtitleView.setBackgroundColor(0xFF000000);
                break;
            default:
                subtitleView.setBackgroundColor(Color.TRANSPARENT);
                break;
        }
        subtitleView.setPadding(16, 4, 16, 4);
    }

    @Override
    public void onSubtitleTracksChanged() {
        // Track list is read when the dialog opens; nothing to refresh here.
    }

    private void showSubtitlesDialog() {
        MediaItem item = playerManager.getCurrentItem();
        if (item == null || !item.isVideo) return;
        List<PlayerManager.SubtitleTrack> tracks = playerManager.getSubtitleTracks();
        List<String> labels = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        String active = playerManager.getActiveSubtitleLabel();
        labels.add("关闭字幕"
                + (!playerManager.isSubtitlesEnabled() || active == null ? " ●" : ""));
        indices.add(-1);
        for (PlayerManager.SubtitleTrack t : tracks) {
            String mark = playerManager.isSubtitlesEnabled() && t.label != null
                    && t.label.equals(active) ? " ●" : "";
            String detail = t.external && t.playerTrackIndex == PlayerManager.EXTERNAL_TRACK_INDEX
                    ? " (" + playerManager.getExternalCueCount() + ")" : "";
            labels.add((t.external ? "外挂 " : "")
                    + t.label + detail + mark);
            indices.add(t.playerTrackIndex);
        }
        labels.add("加载外挂字幕");
        indices.add(-2);
        labels.add("字幕设置");
        indices.add(-3);
        new AlertDialog.Builder(this)
                .setTitle("字幕")
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    int idx = indices.get(which);
                    if (idx == -1) {
                        playerManager.setSubtitlesEnabled(false);
                    } else if (idx == -2) {
                        pickExternalSubtitle(item);
                    } else if (idx == -3) {
                        showSubtitleSettings();
                    } else {
                        playerManager.setSubtitlesEnabled(true);
                        playerManager.selectSubtitleTrack(idx);
                    }
                }).show();
    }

    private void showSubtitleSettings() {
        final String[] delayLabel = {String.format(
                java.util.Locale.ROOT, "偏移 %dms", playerManager.getSubtitleDelayMs())};
        final String[] sizeLabel = {String.format(
                java.util.Locale.ROOT, "%d", prefs.getInt("subtitle_text_size", 18))};
        final String[] colorLabel = {colorName(prefs.getInt("subtitle_text_color", Color.WHITE))};
        final String[] bgLabel = {bgName(prefs.getInt("subtitle_bg", 0))};
        final List<String> labels = new ArrayList<>();
        final List<Integer> actions = new ArrayList<>();
        rebuildSubtitleSettings(labels, actions, delayLabel, sizeLabel, colorLabel, bgLabel);
        final AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("字幕设置")
                .setItems(labels.toArray(new String[0]), null).create();
        dlg.show();
        dlg.getListView().setOnItemClickListener((parent, view, position, id) -> {
            onSubtitleSettingClicked(actions.get(position), dlg,
                    delayLabel, sizeLabel, colorLabel, bgLabel, labels, actions);
        });
    }

    private void rebuildSubtitleSettings(List<String> labels, List<Integer> actions,
            String[] delayLabel, String[] sizeLabel, String[] colorLabel, String[] bgLabel) {
        labels.clear();
        actions.clear();
        labels.add((playerManager.isSubtitlesEnabled() ? "✓ " : "")
                + "启用字幕");
        actions.add(0);
        labels.add((playerManager.isSubtitleAutoLoad() ? "✓ " : "")
                + "自动加载同名字幕"
                + " — 播放时自动查找同目录同名外挂字幕");
        actions.add(1);
        labels.add("字幕延迟: " + delayLabel[0]);
        actions.add(2);
        labels.add("字号: " + sizeLabel[0]);
        actions.add(5);
        labels.add("颜色: " + colorLabel[0]);
        actions.add(6);
        labels.add((prefs.getBoolean("subtitle_bold", true) ? "✓ " : "")
                + "加粗");
        actions.add(7);
        labels.add("背景: " + bgLabel[0]);
        actions.add(8);
    }

    private void onSubtitleSettingClicked(int action,
            AlertDialog dlg, String[] delayLabel,
            String[] sizeLabel, String[] colorLabel, String[] bgLabel,
            List<String> labels, List<Integer> actions) {
        if (action == 0) {
            playerManager.setSubtitlesEnabled(!playerManager.isSubtitlesEnabled());
        } else if (action == 1) {
            playerManager.setSubtitleAutoLoad(!playerManager.isSubtitleAutoLoad());
        } else if (action == 2) {
            showDelayPicker(() -> {
                delayLabel[0] = String.format(java.util.Locale.ROOT,
                        "偏移 %dms", playerManager.getSubtitleDelayMs());
                try {
                    if (dlg.isShowing()) dlg.dismiss();
                } catch (Exception ignored) {
                }
                showSubtitleSettings();
            });
            return;
        } else if (action == 5) {
            int[] sizes = {14, 16, 18, 22, 26, 32};
            int cur = prefs.getInt("subtitle_text_size", 18);
            int next = sizes[0];
            for (int i = 0; i < sizes.length; i++) {
                if (sizes[i] == cur) {
                    next = sizes[(i + 1) % sizes.length];
                    break;
                }
            }
            prefs.edit().putInt("subtitle_text_size", next).apply();
            sizeLabel[0] = String.format(java.util.Locale.ROOT, "%d", next);
            applySubtitleStyle();
        } else if (action == 6) {
            int cur = prefs.getInt("subtitle_text_color", Color.WHITE);
            int next = cur == Color.WHITE ? Color.YELLOW
                    : cur == Color.YELLOW ? Color.CYAN : Color.WHITE;
            prefs.edit().putInt("subtitle_text_color", next).apply();
            colorLabel[0] = colorName(next);
            applySubtitleStyle();
        } else if (action == 7) {
            boolean b = !prefs.getBoolean("subtitle_bold", true);
            prefs.edit().putBoolean("subtitle_bold", b).apply();
            applySubtitleStyle();
        } else if (action == 8) {
            int next = (prefs.getInt("subtitle_bg", 0) + 1) % 3;
            prefs.edit().putInt("subtitle_bg", next).apply();
            bgLabel[0] = bgName(next);
            applySubtitleStyle();
        }
        rebuildSubtitleSettings(labels, actions, delayLabel, sizeLabel, colorLabel, bgLabel);
        delayLabel[0] = String.format(java.util.Locale.ROOT,
                "偏移 %dms", playerManager.getSubtitleDelayMs());
        dlg.getListView().setAdapter(new ArrayAdapter<>(this,
                android.R.layout.select_dialog_item, labels.toArray(new String[0])));
    }

    private String colorName(int color) {
        if (color == Color.YELLOW) return "黄色";
        if (color == Color.CYAN) return "青色";
        return "白色";
    }

    private String bgName(int mode) {
        if (mode == 1) return "半透明";
        if (mode == 2) return "纯黑";
        return "无";
    }

    /** Delay stepper dialog (-/+100ms, reset). */
    private void showDelayPicker(Runnable onChanged) {
        String[] items = {"减少 100ms",
                "增加 100ms",
                "重置"};
        new AlertDialog.Builder(this)
                .setTitle("字幕延迟: "
                        + String.format(java.util.Locale.ROOT,
                                "偏移 %dms", playerManager.getSubtitleDelayMs()))
                .setItems(items, (d, which) -> {
                    int cur = playerManager.getSubtitleDelayMs();
                    if (which == 0) playerManager.setSubtitleDelayMs(cur - 100);
                    else if (which == 1) playerManager.setSubtitleDelayMs(cur + 100);
                    else playerManager.setSubtitleDelayMs(0);
                    onChanged.run();
                }).show();
    }

    private void pickExternalSubtitle(MediaItem item) {
        // 本项目没有原仓库的文件选择器依赖，改为列出视频同目录
        // 可用的外挂字幕文件（SubtitleUtil.findSidecars），点击即加载。
        try {
            List<File> sidecars = item.path != null
                    ? SubtitleUtil.findSidecars(item.path) : new ArrayList<>();
            if (sidecars.isEmpty()) {
                Toast.makeText(this, "未找到可用的外挂字幕文件", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] names = new String[sidecars.size()];
            for (int i = 0; i < sidecars.size(); i++) names[i] = sidecars.get(i).getName();
            new AlertDialog.Builder(this)
                    .setTitle("加载外挂字幕")
                    .setItems(names, (d, which) -> {
                        String picked = sidecars.get(which).getAbsolutePath();
                        if (playerManager.setExternalSubtitle(picked)) {
                            Toast.makeText(this, "已加载字幕: " + names[which], Toast.LENGTH_SHORT).show();
                            showControlsTemporarily();
                        } else {
                            Toast.makeText(this, "不支持的字幕格式", Toast.LENGTH_SHORT).show();
                        }
                    }).show();
        } catch (Exception e) {
            Toast.makeText(this, "不支持的字幕格式", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPlayerSettings() {
        String subState = playerManager.isSubtitlesEnabled()
                && playerManager.getActiveSubtitleLabel() != null
                ? playerManager.getActiveSubtitleLabel() : "关闭";
        String[] items = {"保持屏幕常亮: " + (playerManager.isKeepScreenOn() ? "开" : "关"),
                "快进/快退: " + (playerManager.getSkipDuration() / 1000) + " 秒",
                "字幕: " + subState,
                "关闭播放器"};
        new AlertDialog.Builder(this)
                .setTitle("播放器设置")
                .setItems(items, (d, which) -> {
                    switch (which) {
                        case 0:
                            playerManager.setKeepScreenOn(!playerManager.isKeepScreenOn());
                            updateKeepScreenOn();
                            break;
                        case 1:
                            int[] durations = {5000, 10000, 15000, 30000};
                            String[] labels = {"5s", "10s", "15s", "30s"};
                            new AlertDialog.Builder(this)
                                    .setTitle("跳转时长")
                                    .setSingleChoiceItems(labels, -1, (d2, w) -> {
                                        if (w >= 0 && w < durations.length) {
                                            playerManager.setSkipDuration(durations[w]);
                                            d2.dismiss();
                                        }
                                    }).show();
                            break;
                        case 2: showSubtitlesDialog(); break;
                        case 3: finish(); break;
                    }
                }).show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String playPath = intent.getStringExtra("play_path");
        if (playPath != null) {
            playPathWithSiblings(playPath);
        }
    }

    private void playPathWithSiblings(String playPath) {
        try {
            File cur = new File(playPath);
            File dir = cur.getParentFile();
            if (dir != null && dir.isDirectory()) {
                File[] all = dir.listFiles();
                if (all != null) {
                    List<File> media = new ArrayList<>();
                    for (File f : all) {
                        if (f == null || !f.isFile()) continue;
                        String n = f.getName().toLowerCase(Locale.ROOT);
                        if (n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".flac") || n.endsWith(".ogg")
                                || n.endsWith(".m4a") || n.endsWith(".aac") || n.endsWith(".wma") || n.endsWith(".opus")
                                || n.endsWith(".mp4") || n.endsWith(".mkv") || n.endsWith(".avi") || n.endsWith(".mov")
                                || n.endsWith(".webm") || n.endsWith(".3gp") || n.endsWith(".ts") || n.endsWith(".flv")
                                || n.endsWith(".wmv") || n.endsWith(".m4v")) media.add(f);
                    }
                    // 简单按文件名排序，替代原仓库的列表排序偏好。
                    Collections.sort(media, (a, b) -> a.getName().compareTo(b.getName()));
                    File[] arr = media.toArray(new File[0]);
                    if (arr.length > 0) {
                        List<MediaItem> q = new ArrayList<>();
                        int start = 0;
                        String curAbs = cur.getAbsolutePath();
                        String curName = cur.getName();
                        for (int i = 0; i < arr.length; i++) {
                            String path = arr[i].getAbsolutePath();
                            if (path.equals(curAbs) || arr[i].getName().equalsIgnoreCase(curName)) {
                                start = i;
                                q.add(PlayerManager.buildMediaItem(this, path));
                            } else {
                                q.add(PlayerManager.buildMediaItemFast(path));
                            }
                        }
                        playerManager.play(q, start);
                        return;
                    }
                }
            }
        } catch (Exception ignored) {}
        playerManager.play(PlayerManager.buildMediaItem(this, playPath));
    }

    @Override
    protected void onResume() {
        super.onResume();
        playerManager.incrementUIVisible();
        playerManager.registerCallback(this);
        updateUIForCurrentItem();
        updateControlsState();
        updatePlayPauseIcon();
        if (pausedForBackground) {
            playerManager.play();
            pausedForBackground = false;
        }
        videoContainer.post(this::fitVideo);
    }

    @Override
    protected void onPause() {
        super.onPause();
        playerManager.decrementUIVisible();
        autoHideHandler.removeCallbacks(autoHideRunnable);
        if (playerManager.isPlayingVideo() && !playerManager.isVideoBackgroundPlay() && playerManager.getState() == PlayerManager.PlayState.PLAYING) {
            playerManager.pause();
            pausedForBackground = true;
        }
        // Don't (re)start the service here: it is already running whenever
        // something plays, and starting it on every pause kept the app alive
        // in the background after the user left the player.
    }

    @Override
    protected void onDestroy() {
        playerManager.unregisterCallback(this);
        playerManager.unregisterSubtitleListener(this);
        playerManager.setVideoSizeChangedListener(null);
        autoHideHandler.removeCallbacks(autoHideRunnable);
        playerManager.setSurface(null);
        if (isFinishing() && playerManager.getState() != PlayerManager.PlayState.PLAYING) {
            // Backed out while paused/idle: release the engine and don't leave
            // the foreground service lingering with nothing happening. Actively
            // playing audio is left alone so background playback keeps working.
            try {
                playerManager.stop();
            } catch (Exception ignored) {
            }
        }
        super.onDestroy();
    }

    private static String formatTime(long ms) {
        long totalSec = ms / 1000;
        return String.format("%d:%02d", totalSec / 60, totalSec % 60);
    }
}
