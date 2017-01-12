package fr.bmartel.webradio;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.URL;

public class RadioSingleton {

    private final static String TAG = RadioSingleton.class.getSimpleName();

    private static RadioSingleton mInstance;

    private MediaPlayer mediaPlayer;

    private Context mContext;

    private boolean initPlayer;

    private String mCurrentStream = "";

    private MediaSession mMediaSession;

    private String mTitle;

    private String mRadioIcon;

    /**
     * Media session tag used for now playing card.
     */
    private final static String MEDIA_SESSION_TAG = "fr.bmartel.webradio.MediaSession";


    public static RadioSingleton getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new RadioSingleton(context);
        }
        return mInstance;
    }

    private void initMediaSession() {

        mMediaSession = new MediaSession(mContext, MEDIA_SESSION_TAG);
        mMediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                // Consume the media button event here. Should not send it to other apps.
                return true;
            }
        });

        mMediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        if (!mMediaSession.isActive()) {
            mMediaSession.setActive(true);
        }
    }

    private RadioSingleton(Context context) {

        mContext = context;

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    public void pause() {

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            updateMediaSession(PlaybackState.STATE_PAUSED);
        }
    }

    public boolean load(String streamUrl, String radioIcon, String title, Context context) {
        mContext = context;
        if (!mCurrentStream.equals(streamUrl)) {
            mediaPlayer.reset();
            mCurrentStream = streamUrl;
            mRadioIcon = radioIcon;
            mTitle = title;
            new Player().execute(streamUrl);
            return true;
        }
        return false;
    }

    public void playPause(String streamUrl) {

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            updateMediaSession(PlaybackState.STATE_PAUSED);
        } else {
            if (!initPlayer) {
                new Player().execute(streamUrl);
            } else {
                if (!mediaPlayer.isPlaying()) {
                    mediaPlayer.start();
                    updateMediaSession(PlaybackState.STATE_PLAYING);
                }
            }
        }
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public String getStreamUrl() {
        return mCurrentStream;
    }

    public void closePlayer() {
        if (mMediaSession != null) {
            mMediaSession.setActive(false);
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    /**
     * preparing mediaplayer will take sometime to buffer the content so prepare it inside the background thread and starting it on UI thread.
     *
     * @author piyush
     */

    class Player extends AsyncTask<String, Void, Boolean> {

        private ProgressDialog progress;

        @Override
        protected Boolean doInBackground(String... params) {
            Boolean prepared;
            try {

                mediaPlayer.setDataSource(params[0]);

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        initPlayer = false;
                        mediaPlayer.stop();
                        mediaPlayer.reset();
                    }
                });
                mediaPlayer.prepare();
                prepared = true;
            } catch (IllegalArgumentException e) {
                prepared = false;
                e.printStackTrace();
            } catch (SecurityException e) {
                prepared = false;
                e.printStackTrace();
            } catch (IllegalStateException e) {
                prepared = false;
                e.printStackTrace();
            } catch (IOException e) {
                prepared = false;
                e.printStackTrace();
            }
            return prepared;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (progress.isShowing()) {
                progress.cancel();
            }
            mediaPlayer.start();

            if (mMediaSession == null) {
                initMediaSession();
            }
            updateMediaSession(PlaybackState.STATE_PLAYING);

            initPlayer = true;
        }

        public Player() {
            progress = new ProgressDialog(mContext);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.progress.setMessage(mContext.getString(R.string.buffering));
            this.progress.show();

        }
    }

    private void updateMediaSession(int state) {

        MediaMetadata.Builder mediaBuilder = null;

        Bitmap bitmap = null;
        try {
            URL url = new URL(mRadioIcon);
            bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
        } catch (IOException e) {
            Log.e(TAG, "BitmapFactory.decodeStream", e);
        }

        mediaBuilder = new MediaMetadata.Builder();
        mediaBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, mTitle);
        if (bitmap != null) {
            mediaBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap);
        }

        mMediaSession.setMetadata(mediaBuilder.build());

        PlaybackState.Builder stateBuilder = new PlaybackState.Builder();
        stateBuilder.setState(state, 0, 1.0f);

        mMediaSession.setPlaybackState(stateBuilder.build());
    }

    public MediaPlayer.TrackInfo[] getTrackInfo() {
        return mediaPlayer.getTrackInfo();
    }
}
