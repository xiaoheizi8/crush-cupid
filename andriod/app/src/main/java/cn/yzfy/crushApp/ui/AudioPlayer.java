package cn.yzfy.crushApp.ui;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileOutputStream;

import cn.yzfy.crushApp.api.Rest;
import cn.yzfy.crushApp.api.VoiceApi;

/**
 * 语音播放器：TTS base64 mp3 落缓存文件 → MediaPlayer 播放。
 * <p>
 * ChatFragment（气泡 ▶/■）与 VoiceConfigFragment（音色试听）共用，消除重复实现。
 * 「正在播放」状态以 (text, voice) 标识——点哪条播哪条，修复旧实现里
 * 播放状态总是错位到最后一条消息的 bug。
 */
public final class AudioPlayer {

    /** 播放状态变化（开始/停止）回调，主线程 */
    public interface Listener {
        void onPlayingChanged();
    }

    /** 合成/播放失败提示，主线程 */
    public interface ErrorSink {
        void onError(String message);
    }

    private static final Handler UI = new Handler(Looper.getMainLooper());

    private final android.content.Context appContext;
    private final Listener listener;
    private final ErrorSink errors;

    private MediaPlayer player;
    private String playingText;
    private String playingVoice;

    public AudioPlayer(android.content.Context context, Listener listener, ErrorSink errors) {
        this.appContext = context.getApplicationContext();
        this.listener = listener;
        this.errors = errors;
    }

    /** 当前是否在播放 */
    public boolean isPlaying() {
        return player != null;
    }

    /** 正在播放的文本（用于气泡 ▶/■ 状态匹配，可能为 null） */
    public String playingText() {
        return playingText;
    }

    /** 正在播放的音色（试听页用于按钮状态匹配，可能为 null） */
    public String playingVoice() {
        return playingVoice;
    }

    /** 播放/停止切换：正在播 → 停止；否则合成并播放 */
    public void toggle(String text, String voice) {
        if (isPlaying()) {
            stop();
            return;
        }
        play(text, voice);
    }

    /** 合成并播放（自动停掉上一次播放） */
    public void play(String text, String voice) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }
        stop();
        VoiceApi.synthesize(text, voice, new Rest.Callback<String>() {
            @Override
            public void ok(String base64) {
                start(base64, text, voice);
            }

            @Override
            public void fail(String message) {
                if (errors != null) {
                    errors.onError("语音合成失败：" + message);
                }
            }
        });
    }

    /** 停止播放并释放资源（幂等） */
    public void stop() {
        boolean wasPlaying = isPlaying();
        releasePlayer();
        if (wasPlaying && listener != null) {
            listener.onPlayingChanged();
        }
    }

    /** 彻底释放（页面销毁时调用） */
    public void release() {
        releasePlayer();
    }

    private void releasePlayer() {
        try {
            if (player != null) {
                if (player.isPlaying()) player.stop();
                player.release();
            }
        } catch (Exception ignored) {
        }
        player = null;
        playingText = null;
        playingVoice = null;
    }

    private void start(final String base64, final String text, final String voice) {
        UI.post(() -> {
            try {
                releasePlayer();
                File f = new File(appContext.getCacheDir(), "voice_" + System.currentTimeMillis() + ".mp3");
                FileOutputStream fos = new FileOutputStream(f);
                fos.write(android.util.Base64.decode(base64, android.util.Base64.DEFAULT));
                fos.close();
                player = new MediaPlayer();
                player.setDataSource(f.getAbsolutePath());
                player.setOnCompletionListener(mp -> stop());
                player.setOnErrorListener((mp, w, e) -> {
                    stop();
                    if (errors != null) {
                        errors.onError("播放失败，请稍后再试");
                    }
                    return true;
                });
                player.prepare();
                player.start();
                playingText = text;
                playingVoice = voice;
                if (listener != null) {
                    listener.onPlayingChanged();
                }
            } catch (Exception e) {
                releasePlayer();
                if (errors != null) {
                    errors.onError("播放失败，请稍后再试");
                }
            }
        });
    }
}
