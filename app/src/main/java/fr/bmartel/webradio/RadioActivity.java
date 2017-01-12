package fr.bmartel.webradio;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class RadioActivity extends Activity {

    private RadioSingleton mSingleton;

    private ImageButton playPause;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String streamUrl = getIntent().getStringExtra("EXTRA_STREAM");
        String iconUrl = getIntent().getStringExtra("EXTRA_ICON");
        String title = getIntent().getStringExtra("EXTRA_TITLE");

        setContentView(R.layout.activity_radio);

        ImageView radioIcon = (ImageView) findViewById(R.id.radio_icon);
        Picasso.with(this).load(iconUrl).fit().into(radioIcon);

        TextView radioTitle = (TextView) findViewById(R.id.radio_title);
        radioTitle.setText(title);

        mSingleton = RadioSingleton.getInstance(this);

        playPause = (ImageButton) findViewById(R.id.play_pause);

        checkPlayerState(false);

        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkPlayerState(true);
                mSingleton.playPause(streamUrl);
            }
        });

        boolean firstLoad = play(streamUrl, iconUrl, title);

        if (firstLoad) {
            playPause.setImageResource(R.drawable.pause);
        }

        /*
        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {

            public void run() {
                String title = "";
                IcyStreamMeta meta = new IcyStreamMeta();
                try {
                    meta.setStreamUrl(new URL(streamUrl));
                    title = meta.getStreamTitle();
                    Log.i("test", "title : " + title + " => " + meta.getArtist());

                    for (Map.Entry<String, String> entry : meta.getMetadata().entrySet())
                    {
                        System.out.println(entry.getKey() + "/" + entry.getValue());
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }, 1000, 1000);
        */
    }

    private void checkPlayerState(boolean invert) {
        if (mSingleton.isPlaying()) {
            if (invert) {
                playPause.setImageResource(R.drawable.play);
            } else {
                playPause.setImageResource(R.drawable.pause);
            }
        } else {
            if (invert) {
                playPause.setImageResource(R.drawable.pause);
            } else {
                playPause.setImageResource(R.drawable.play);
            }
        }
    }

    private boolean play(String streamUrl, String radioIcon, String title) {
        return mSingleton.load(streamUrl, radioIcon, title, this);
    }


}
