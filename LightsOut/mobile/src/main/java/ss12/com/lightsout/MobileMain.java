package ss12.com.lightsout;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.Locale;

public class MobileMain extends Activity implements GoogleApiClient.ConnectionCallbacks,MessageApi.MessageListener,
        View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {


    private boolean mSignInClicked=false;
    private  ConnectionResult mConnectionResult;
    private boolean mIntentInProgress;
    private int RC_SIGN_IN=0;
    private GoogleApiClient mGoogleApiClient;
    private final String TAG="mobile main";
    private String nodeId="";
    private int size;
    private TextView tv;
    private TextToSpeech textToSpeech;
    private MediaPlayer sfx;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        textToSpeech = new TextToSpeech(this,
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status != TextToSpeech.ERROR) {
                            textToSpeech.setLanguage(Locale.UK);
                        }
                    }
                });
        createGoogleApiClient();
        mGoogleApiClient.connect();
        Wearable.MessageApi.addListener(mGoogleApiClient,this);
        retrieveDeviceNode();
    }

    @Override
    protected void onStart() {
        super.onStart();


        findViewById(R.id.sign_in).setOnClickListener(this);

        //set up button for going into game modes
        findViewById(R.id.fight).setOnClickListener(this);

        //set up button for going into leaderboards
        findViewById(R.id.leaderboards).setOnClickListener(this);

    }


    @Override
    protected void onStop() {
        super.onStop();
    }


    //this gets called on start, builds the GoogleAPIClient with proper api's and connects to it
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
                //.addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addApi(Wearable.API)
              //  .addApi(Games.API).addScope(Games.SCOPE_GAMES)
               // .addScope(Plus.SCOPE_PLUS_LOGIN)
                .build();
        mGoogleApiClient.connect();
        Toast.makeText(this,"Google Api Client Built",Toast.LENGTH_LONG).show();

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mobile_main, menu);
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

    //on connected callback for GoogleAPIClient
    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected: " + bundle);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        //removes Google+ sign in if the user is already signed in
        findViewById(R.id.sign_in).setVisibility(View.GONE);
        Toast.makeText(this, "Google Api Client Connected", Toast.LENGTH_LONG).show();


    }

    //on suspended callback for GoogleAPIClient
    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
        Toast.makeText(this,"Google Api Client Suspended",Toast.LENGTH_LONG).show();

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
                    Log.d(TAG,nodeId);
                }

                Log.d(TAG,nodes.size()+"");
            }
        }).start();
        Log.d(TAG,size+" = node size");
    }
    //Google+ sign in
    private void signInWithGplus() {
        if (!mGoogleApiClient.isConnecting()) {
            mSignInClicked = true;
            resolveSignInError();
        }
    }


    //resolve Google+ sign in errors
    private void resolveSignInError() {
        if (mConnectionResult.hasResolution()) {
            try {
                mIntentInProgress = true;
                mConnectionResult.startResolutionForResult(this, RC_SIGN_IN);
            } catch (IntentSender.SendIntentException e) {
                mIntentInProgress = false;
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int responseCode,
                                    Intent intent) {
        if (requestCode == RC_SIGN_IN) {
            if (responseCode != RESULT_OK) {
                mSignInClicked = false;
            }

            mIntentInProgress = false;

            if (!mGoogleApiClient.isConnecting()) {
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Toast.makeText(this,"Google Api Client connection failed",Toast.LENGTH_LONG).show();
        if (!result.hasResolution()) {
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this, 0).show();
            return;
        }

        if (!mIntentInProgress) {
            // Store the ConnectionResult for later usage
            mConnectionResult = result;

            if (mSignInClicked) {
                // The user has already clicked 'sign-in' so we attempt to
                // resolve all
                // errors until the user is signed in, or they cancel.
                resolveSignInError();
            }
        }
    }

    //on click listeners
    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.sign_in){
            signInWithGplus();
        }
        else if(v.getId() == R.id.leaderboards){
            startActivityForResult(Games.Leaderboards.getLeaderboardIntent(mGoogleApiClient,
                    getResources().getString(R.string.leaderboard_high_score)), 0);
        }
        else if(v.getId() == R.id.fight){
            Intent intent=new Intent(getApplicationContext(),GameModes.class);
            startActivity(intent);
        }
        else{
            //no button defined in code is pressed
        }
    }
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        final String message = messageEvent.getPath();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "wear message received " + message);
                tv = (TextView) findViewById(R.id.results);
                if (Integer.parseInt(message) == 4) {
                    textToSpeech.speak("start game",TextToSpeech.QUEUE_FLUSH,null);
                    setContentView(R.layout.activity_in_game);
                    Intent intent = new Intent(getApplicationContext(),SinglePlayerGame.class);
                    startActivity(intent);

                } else {
                    Toast.makeText(getApplicationContext(), "Invalid commpand APP", Toast.LENGTH_SHORT).show();

                }
            }
        });

    }

}
