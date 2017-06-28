package com.stellarscript.vlcvideo;

import android.content.res.Configuration;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceView;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.text.MessageFormat;
import java.util.ArrayList;

public final class VLCVideoView extends SurfaceView {

    private static final String TAG = VLCVideoView.class.getSimpleName();
    private static final String HARDWARE_ACCELERATION_ERROR_MESSAGE = "VLC encountered an error with hardware acceleration.";
    private static final String MEDIA_ERROR_MESSAGE = "VLC encountered an error with this media.";
    private static final double MIN_TIME_INTERVAL_TO_EMIT_TIME_EVENT = 100;

    private double mPrevTime;
    private final ThemedReactContext mThemedReactContext;
    private final RCTEventEmitter mEventEmitter;
    private final LibVLC mLibVLC;
    private final MediaPlayer mMediaPlayer;
    private final LifecycleEventListener mLifecycleEventListener = new LifecycleEventListener() {

        @Override
        public void onHostResume() {
            VLCVideoView.this.attachVLCVoutViews();
        }

        @Override
        public void onHostPause() {
            mMediaPlayer.pause();
            VLCVideoView.this.detachVLCVoutViews();
        }

        @Override
        public void onHostDestroy() {
        }

    };
    private final MediaPlayer.EventListener mMediaPlayerEventListener = new MediaPlayer.EventListener() {

        @Override
        public void onEvent(@NonNull final MediaPlayer.Event mediaEvent) {
            final WritableMap event = Arguments.createMap();
            String eventName = VLCVideoEvents.UNHANDLED_EVENT;

            switch (mediaEvent.type) {
                case MediaPlayer.Event.EndReached:
                    eventName = VLCVideoEvents.ON_END_REACHED_EVENT;
                    break;
                case MediaPlayer.Event.EncounteredError:
                    eventName = VLCVideoEvents.ON_ERROR_EVENT;
                    event.putString(VLCVideoEvents.ON_ERROR_MESSAGE_PROP, MEDIA_ERROR_MESSAGE);
                    break;
                case MediaPlayer.Event.Buffering:
                    if (!mMediaPlayer.isPlaying()) {
                        eventName = VLCVideoEvents.ON_BUFFERING_EVENT;
                    }
                    break;
                case MediaPlayer.Event.Paused:
                    eventName = VLCVideoEvents.ON_PAUSED_EVENT;
                    break;
                case MediaPlayer.Event.Stopped:
                    eventName = VLCVideoEvents.ON_STOPPED_EVENT;
                    break;
                case MediaPlayer.Event.TimeChanged:
                    final double currentTime = mMediaPlayer.getTime();
                    if (Math.abs(currentTime - mPrevTime) >= MIN_TIME_INTERVAL_TO_EMIT_TIME_EVENT || currentTime == 0) {
                        mPrevTime = currentTime;
                        eventName = VLCVideoEvents.ON_TIME_CHANGED_EVENT;
                        event.putDouble(VLCVideoEvents.ON_TIME_CHANGED_TIME_PROP, currentTime);
                    }
                    break;
                case MediaPlayer.Event.Playing:
                    final double duration = mMediaPlayer.getLength();
                    eventName = VLCVideoEvents.ON_PLAYING_EVENT;
                    event.putDouble(VLCVideoEvents.ON_PLAYING_DURATION_PROP, duration);
                    break;
                case MediaPlayer.Event.SeekableChanged:
                    Log.d(TAG, "SeekableChanged");
                    break;
            }

            if (!eventName.equals(VLCVideoEvents.UNHANDLED_EVENT)) {
                mEventEmitter.receiveEvent(VLCVideoView.this.getId(), eventName, event);
            }
        }

    };
    private final IVLCVout.Callback mVoutCallback = new IVLCVout.Callback() {

        @Override
        public void onNewLayout(@NonNull final IVLCVout vout, final int width, final int height, final int visibleWidth, final int visibleHeight, final int sarNum, final int sarDen) {
        }

        @Override
        public void onSurfacesCreated(@NonNull final IVLCVout vout) {
        }

        @Override
        public void onSurfacesDestroyed(@NonNull final IVLCVout vout) {
        }

        @Override
        public void onHardwareAccelerationError(@NonNull final IVLCVout vout) {
            final WritableMap event = Arguments.createMap();
            event.putString(VLCVideoEvents.ON_ERROR_MESSAGE_PROP, HARDWARE_ACCELERATION_ERROR_MESSAGE);
            mEventEmitter.receiveEvent(VLCVideoView.this.getId(), VLCVideoEvents.ON_ERROR_EVENT, event);
        }

    };

    public VLCVideoView(@NonNull final ThemedReactContext themedReactContext) {
        super(themedReactContext);

        mThemedReactContext = themedReactContext;
        mThemedReactContext.addLifecycleEventListener(mLifecycleEventListener);

        mEventEmitter = mThemedReactContext.getJSModule(RCTEventEmitter.class);

        final ArrayList<String> libVLCOptions = new ArrayList<>();
        libVLCOptions.add("-vvv");
        libVLCOptions.add("--http-reconnect");
        // TODO get more options from config
        mLibVLC = new LibVLC(mThemedReactContext, libVLCOptions);

        mMediaPlayer = new MediaPlayer(mLibVLC);
        mMediaPlayer.setEventListener(mMediaPlayerEventListener);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attachVLCVoutViews();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        detachVLCVoutViews();
        mThemedReactContext.removeLifecycleEventListener(mLifecycleEventListener);
        mMediaPlayer.release();
        mLibVLC.release();
    }

    @Override
    protected void onConfigurationChanged(@NonNull final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void loadMedia(@NonNull final String sourceUrl, final double startTime, final boolean autoplay) {
        if (sourceUrl.isEmpty()) {
            return;
        }

        final Media media = new Media(mLibVLC, Uri.parse(sourceUrl));
        media.setHWDecoderEnabled(true, false);
        if (startTime > 0) {
            final int startTimeInSeconds = (int) (startTime / 1000);
            final String startTimeOption = MessageFormat.format(":start-time={0}", startTimeInSeconds);
            media.addOption(startTimeOption);
        }

        mMediaPlayer.stop();
        mMediaPlayer.setMedia(media);

        if (autoplay) {
            mMediaPlayer.play();
        }
    }

    public void play() {
        mMediaPlayer.play();
    }

    public void pause() {
        mMediaPlayer.pause();
    }

    public void seek(final int time) {
        mMediaPlayer.setTime(time);
        mMediaPlayer.play();
    }

    private void attachVLCVoutViews() {
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.addCallback(mVoutCallback);
        if (!vout.areViewsAttached()) {
            vout.setVideoView(VLCVideoView.this);
            vout.attachViews();
        }
    }

    private void detachVLCVoutViews() {
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(mVoutCallback);
        if (vout.areViewsAttached()) {
            vout.detachViews();
        }
    }

}