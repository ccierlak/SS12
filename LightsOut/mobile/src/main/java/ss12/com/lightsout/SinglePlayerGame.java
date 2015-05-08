package ss12.com.lightsout;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;

import com.google.android.gms.games.Games;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.Locale;
import java.util.Random;

//this class is the single player game activity and controls all
// single player game logic on the ui thread
public class SinglePlayerGame extends Activity implements MessageApi.MessageListener,
        GoogleApiClient.ConnectionCallbacks  {
    private final String TAG = "Single Player Mobile";
    private GoogleApiClient mGoogleApiClient;
    private String nodeId="";
    private int size;
    private TextView tv;
    private Random random = new Random();
    private Vibrator vibrator;
    private TextToSpeech textToSpeech;
    private MediaPlayer sfx;
    private int strikes = 0;
    private int points = 0;

    /*static private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //this triggers after a certain amount of time has passed defined by timeLimit
            Log.d("mobile message handler","delay message received");
            ((SinglePlayerGame) msg.obj).startRound();
        }
    };*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_in_game);
        createGoogleApiClient();
        textToSpeech = new TextToSpeech(this,
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status != TextToSpeech.ERROR) {
                            textToSpeech.setLanguage(Locale.UK);
                        }
                    }
                });
        vibrator= (Vibrator) this.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        tv = (TextView) findViewById(R.id.text2);
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String action = startRound();
                Log.d(TAG, "push: " + action);
            }
        });
    }
    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected: " + bundle);
        Wearable.MessageApi.addListener(mGoogleApiClient,this);
        // now we can use the Message API
        //assigns nodeId
        retrieveDeviceNode();

    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        final String message = messageEvent.getPath();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "wear message received "+message);
                tv = (TextView) findViewById(R.id.results);
                if (Integer.parseInt(message) != 9) {
                    tv.setText("Fail");
                    textToSpeech.speak("Wrong Move", TextToSpeech.QUEUE_FLUSH, null);
                    sfxPlayer(R.raw.fail);
                    //check to see if the game ends
                    endgame();
                }
                else
                {
                    tv.setText("Success");
                    textToSpeech.speak("Point Scored",TextToSpeech.QUEUE_FLUSH,null);
                    sfxPlayer(R.raw.cheering);
                    points++;
                    startRound();
                }
            }
        });
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onStop() {
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_in_game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /*generates a random number from 0 - 2 to send to the wearable
    each number represents a different action for the wearable to expect from
    the accelerometer data (ie 0 represents a punch expecting action on the x axis)
     */
    private String startRound(){

        int actionMotion = random.nextInt(3);
        String action = actionMotion+"";
        respond(actionMotion);
        Wearable.MessageApi.sendMessage(mGoogleApiClient,nodeId,action,null);
        return action;
    }

    //update ui elements
    private void updateUI(){
        //general updating of UI should occur here
        //probably will want to pass in what and how to update
        //OR
        //looping image or gif while waiting for the service to return with success or fail
    }

    //auditory and haptic feedback on success or fail
    private void respond(int action){
        //general playing of sounds should occur here
        //probably will want to pass in sound name or path
        switch (action)
        {
            case 0://punch
                vibrator.vibrate(new long[] { 0, 500, 0 }, -1);
                textToSpeech.speak("Punch", TextToSpeech.QUEUE_FLUSH, null);
                Toast.makeText(getApplicationContext(), "Punch", Toast.LENGTH_SHORT).show();
                break;
            case 1://counter
                vibrator.vibrate(new long[]{0, 400, 0, 0, 0, 0, 400, 0 , 0, 0, 0, 500}, -1);
                textToSpeech.speak("Block", TextToSpeech.QUEUE_FLUSH, null);
                Toast.makeText(getApplicationContext(), "Block", Toast.LENGTH_SHORT).show();
                break;
            case 2: //push
                vibrator.vibrate(new long[]{0, 400, 0, 0, 0, 0, 400,0 , 0, 0, 0, 400, 0 , 0 , 0, 500}, -1);
                textToSpeech.speak("Rush",TextToSpeech.QUEUE_FLUSH,null);
                Toast.makeText(getApplicationContext(), "Rush", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }

    }

    //method called when service returns success
    private void onSuccessAction(){
        //add as callback method for listener service?
        //increase score count and enter back into loop for gameplay
    }

    //method called when service returns fail
    private void onFailAction(){
        //add as callback method for listener service?
        //increase strike count and enter back into loop IFF game should not end
        //possibly end game here
    }


    private void createGoogleApiClient(){
        //Basic Google Api Client build
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

    }

    private void retrieveDeviceNode() {
        //we are using a worker thread to get the nodeId since we do not want to
        // clog up the main thread
        Log.d(TAG,"nodes retrieved");

        new Thread(new Runnable() {
            @Override
            public void run() {

                NodeApi.GetConnectedNodesResult nodesResult =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                final List<Node> nodes = nodesResult.getNodes();
                //we are assuming here that there is only one wearable attached to the phone
                //currently Android only supports having one wearable connected at a time
                if (nodes.size() > 0) {
                    nodeId=nodes.get(0).getId();
                    size=nodes.size();
                    Log.d(TAG,"node id: "+nodeId);
                }

                Log.d(TAG,nodes.size()+"");
            }
        }).start();
     Log.d(TAG,size+" = node size");
    }

    private void endgame(){
        strikes++;
        if(strikes == 3){
            strikes = 0;
            //end the game
            //submit score to leaderboard
            Games.Leaderboards.submitScore(mGoogleApiClient,
                    getResources().getString(R.string.leaderboard_high_score), 1337);
            Wearable.MessageApi.sendMessage(mGoogleApiClient,nodeId,"9",null);
        }
        else
        {
            startRound();
            /*
            Message msg = mHandler.obtainMessage(1337,this);
            mHandler.sendMessageDelayed(msg, 1000);
            */
        }
    }

    protected void sfxPlayer(int theText) {
        if (sfx != null) {
            sfx.reset();
            sfx.release();
        }
        sfx = MediaPlayer.create(this, theText);
        sfx.start();
    }

}
