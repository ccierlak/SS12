package ss12.com.lightsout;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.wearable.Wearable;

public class MobileMain extends Activity implements GoogleApiClient.ConnectionCallbacks,
        View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {


    private boolean mSignInClicked=false;
    private  ConnectionResult mConnectionResult;
    private boolean mIntentInProgress;
    private int RC_SIGN_IN=0;
    private GoogleApiClient mGoogleApiClient;
    private final String TAG="mobile main";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
        createGoogleApiClient();
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
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API, Plus.PlusOptions.builder().build())
                .addApi(Wearable.API)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .addScope(Plus.SCOPE_PLUS_LOGIN)
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

}
