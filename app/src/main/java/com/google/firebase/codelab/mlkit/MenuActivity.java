package com.google.firebase.codelab.mlkit;

import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MenuActivity extends AppCompatActivity {
    private static final String LOG_TAG = "MenuActivity";
    // Unique tag required for the intent extra
    public static final String EXTRA_MESSAGE
            = "com.google.firebase.codelab.mlkit.extra.MESSAGE";
    private String selection;
    private TextToSpeech tts;
    private boolean isTTSinitialized;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        setTitle("I love Numbers");
        isTTSinitialized = false;

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                isTTSinitialized = true;
            }
        });
        /*if (isTTSinitialized) {
            tts.speak("What would you like to count? ",
                    TextToSpeech.QUEUE_FLUSH, null);
        }*/
    }
    public void carButtonOnClick(View view) {
        selection = "car";
        if (isTTSinitialized) {
            tts.speak("You selected cars, nice ",
                    TextToSpeech.QUEUE_FLUSH, null);
        }
        Toast toast = Toast.makeText(this, "You selected Cars!",
                Toast.LENGTH_SHORT);
        toast.show();
        launchSecondActivity(view, selection);
    }
    public void birdButtonOnClick(View view) {
        selection = "bird";
        if (isTTSinitialized) {
            tts.speak("You selected birds, nice ",
                    TextToSpeech.QUEUE_FLUSH, null);
        }
        Toast toast = Toast.makeText(this,"You selected Birds!",
                Toast.LENGTH_SHORT);
        toast.show();
        launchSecondActivity(view, selection);
    }
    public void launchSecondActivity(View view, String selection) {
        Log.d(LOG_TAG, "Button clicked!");
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_MESSAGE, selection);
        startActivity(intent);
    }

}
