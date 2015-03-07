package ss12.com.lightsout;

import android.content.Intent;
import android.speech.RecognizerIntent;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Alex on 3/2/2015.
 */
public class VoiceCommand extends MobileMain {
    private TextView mTextView;
    private static final int SPEECH_REQUEST_CODE = 0;

    // Create an intent that can start the Speech Recognizer activity
    private void displaySpeechRecognizer() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
// Start the activity, the intent will be populated with the speech text
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    // This callback is invoked when the Speech Recognizer returns.
// This is where you process the intent and extract the speech text from the intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            List<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            String spokenText = results.get(0);
            // Do something with spokenText
            performAction(spokenText);

        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    protected void performAction(String voiceCommand)
    {
        switch (voiceCommand)
        {
            case "Start Fight":
                setContentView(R.layout.activity_in_game);
            break;
        }
    }
}

