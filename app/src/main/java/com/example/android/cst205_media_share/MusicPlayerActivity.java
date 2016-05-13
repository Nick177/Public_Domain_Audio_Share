package com.example.android.cst205_media_share;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * Created by Nick89 on 5/11/2016.
 */
public class MusicPlayerActivity extends AppCompatActivity {
    private String filePath;

    protected static MediaPlayer Sound;
    public static SeekBar seek_bar;
    private static TextView text_view;

    int pause;

    //


    public void timeline(){
        // seek_bar = (SeekBar) findViewById(R.id.seekBar); //Not used at the moment
        // text_view = (TextView) findViewById(R.id.textView);
        // text_view.setText("Duration: " + seek_bar.getProgress() + );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_player);
        Intent intent = getIntent();
        filePath = intent.getExtras().getString("File");
        Toast.makeText(MusicPlayerActivity.this, filePath, Toast.LENGTH_SHORT).show();
        Toast.makeText(MusicPlayerActivity.this, intent.getExtras().getString("File"), Toast.LENGTH_SHORT).show();
        if(filePath != null) {
            Toast.makeText(MusicPlayerActivity.this, filePath, Toast.LENGTH_SHORT).show();
            Sound = MediaPlayer.create(MusicPlayerActivity.this, Uri.parse(filePath));
        }
        else
            Toast.makeText(MusicPlayerActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
    }

    public void onToggleClicked(View view)
    {
        Log.d("here", "before"); //Debugging message
        boolean checked = ((ToggleButton)findViewById(R.id.playPauseButton)).isChecked();
        Log.d("here", "after"); //Debugging message

        Log.d("here", Boolean.toString(checked)); //Debugging message

        if (checked && !Sound.isPlaying() && Sound!=null)
        {
            Log.d("here", "checked"); //Debugging message
            Sound.start();
            //Play the song
        }
        else if (Sound.isPlaying())
        {
            Log.d("here", "checked2"); //Debugging message
            Sound.pause();
            pause = Sound.getCurrentPosition();
            //Pause on the current position
        }
        else
        {
            Log.d("here", "checked3"); //Debugging message
            Toast.makeText(MusicPlayerActivity.this, "Have the play button ready", Toast.LENGTH_SHORT).show();
            //Shows a small message that describes the error when user attempts to play the song again
        }

    }


    public void stop(View view)
    {
        Sound.release();
        Sound = MediaPlayer.create(MusicPlayerActivity.this, Uri.parse(filePath));
        //Stops the song that is playing and recreates the same file into the app
        if(Sound == null) //Checks if the song is existing or not
            Log.d("here", "isNnull");  //Debugging message
    }
}
