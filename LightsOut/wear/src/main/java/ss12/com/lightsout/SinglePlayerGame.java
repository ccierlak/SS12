package ss12.com.lightsout;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.Random;

public class SinglePlayerGame extends Activity implements MessageApi.MessageListener,
        GoogleApiClient.ConnectionCallbacks, SensorEventListener {

    private GoogleApiClient mGoogleApiClient;
    private final String TAG = "Single Player Wear"; //tag for logging
    private Vibrator vibrator;
    TextView mTextView;
    Button button;
    Button swap;

    private int sensorType=0;

    //timer variables
    //begin time limit at 5 seconds
    private int timeLimit = 5000;
    private Random rand;

    //node for mobile device
    private String nodeId = "";

    //sensor variables
    private SensorManager sensorManager;
    private Sensor accel,gyro,magnet;
    private float[] dataArray = new float[3];
    private double[] gravity =  {0,0,0};
    private double[] acceleration = {0,0,0};
    //max readings for the accelerometer per round
    private double xMaxAccel=0,yMaxAccel=0,zMaxAccel=0;
    //max readings for the gyroscope per round
    private double xMaxGyro=0,yMaxGyro=0,zMaxGyro=0;

    private double xMaxM=0,yMaxM=0,zMaxM=0;

    //Time limit implemented through Handler
    static private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
                //this triggers after a certain amount of time has passed defined by timeLimit
                ((SinglePlayerGame) msg.obj).endRound(msg.what);

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_player_game);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                button = (Button) stub.findViewById(R.id.refresh);
                swap = (Button) stub.findViewById(R.id.swap);

                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        xMaxGyro=0;
                        yMaxGyro=0;
                        zMaxGyro=0;
                        xMaxAccel=0;
                        yMaxAccel=0;
                        zMaxAccel=0;
                        xMaxM=0;
                        yMaxM=0;
                        zMaxM=0;
                    }
                });
                swap.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sensorType++;
                        sensorType%=3;
                    }
                });
            }
        });
        //keep screen active on wearable
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //register sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnet=sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //build apiClient
        createGoogleApiClient();

        //create random number generator
        rand = new Random();

        vibrator =  (Vibrator) this.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);



    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
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
                        // adding only the wearable API
                .addApi(Wearable.API)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

    }

    //this method sets the global variable nodeId to the id of the phone for communication
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
                    Log.d(TAG,nodeId);
                }

                Log.d(TAG,nodes.size()+"");
            }
        }).start();
    }

    //game logic for each round
    private void startRound(int action){
        sensorManager.registerListener(this,accel,sensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,gyro,SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this,magnet,SensorManager.SENSOR_DELAY_NORMAL);

        //increase time limit randomly up to half a second only if it will result in less than
        //five seconds for the user to react
        if(timeLimit<5000) {
            timeLimit += rand.nextInt(750);
        }
        //decrease time limit up to one second only if it will result in over .8 seconds for the
        //user to react
        if(timeLimit>1800) {
            timeLimit -= rand.nextInt(1000);
        }
        Message msg = mHandler.obtainMessage(action,this);
        mHandler.sendMessageDelayed(msg, timeLimit);


    }

    private void endRound(int expectedAction){
        //unregister Listener at the end of every round
        sensorManager.unregisterListener(this,accel);
        sensorManager.unregisterListener(this,gyro);

        int actualAction = 5;
        if(actualAction==expectedAction){
            //send message back to phone, 1 signifies win
            Wearable.MessageApi.sendMessage(mGoogleApiClient,nodeId,"1",null);
        }else{
            //send message back to phone, 0 signifies loss
            Wearable.MessageApi.sendMessage(mGoogleApiClient,nodeId,"0",null);        }

    }

    //game logic for a successful attempt at a round
    private void roundSuccess(){
        /*//remove the pending message so that the message will not trigger a loss
        mHandler.removeMessages(1337, this);*/



    }

    //game logic for an unsuccessful attempt at a round, called when the Handler receives the
    //delayed message
    private void roundLoss(){



    }

    //returns an int representing which of the 3 axes recorded the most activity
    //this will be compared to what axis activity is expected on to determine if the round is won
    //or lost
    /*private int compareAxes(){
        double maxAxis = Math.max(Math.max(xMaxAccel,yMaxAccel),zMaxAccel);
        if(maxAxis==xMaxAccel){//x
            return 0;
        }
        else if(maxAxis==yMaxAccel){//y
                return 1;
            }
        else{//z
            return 2;
        }

    }*/
    private int compareAxes(double x,double y,double z){
        double maxAxis = Math.max(Math.max(x,y),z);
        if(maxAxis==x){//x
            return 0;
        }
        else if(maxAxis==y){//y
            return 1;
        }
        else{//z
            return 2;
        }

    }

    //auditory and haptic feedback on success or fail
    private void respond(int action){
        //general playing of sounds should occur here
        //probably will want to pass in sound name or path
        switch (action)
        {
            case 0://punch
                vibrator.vibrate(new long[]{0, 400,0, 0, 0, 0}, -1);
                break;
            case 1: //counter
                vibrator.vibrate(new long[]{0, 400, 0, 0, 0, 0, 400, 0 , 0, 0, 0, 500}, -1);
                break;
            case 2: //push
                vibrator.vibrate(new long[]{0, 400, 0, 0, 0, 0, 400,0 , 0, 0, 0, 400, 0 , 0 , 0, 500}, -1);
                break;
            default:
                break;
        }

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
        final int actionVibration = Integer.parseInt(message);
        respond(actionVibration);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("Wearable", "message received");
                actionGiven(actionVibration);
           //   Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show();
                startRound(Integer.parseInt(message));
            }
        });
    }
     public  void actionGiven(int action)
     {
         switch (action)
         {
             case 0://punch
                 Toast.makeText(getApplicationContext(),"punch",Toast.LENGTH_SHORT).show();
                 break;
             case 1: //counter
                 Toast.makeText(getApplicationContext(),"counter",Toast.LENGTH_SHORT).show();
                 break;
             case 2: //push
                 Toast.makeText(getApplicationContext(),"push",Toast.LENGTH_SHORT).show();
                 break;
             default:
                 break;
         }
     }
    @Override
    protected void onStop() {
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        super.onStop();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;

            dataArray[0] = event.values[0];
            dataArray[1] = event.values[1];
            dataArray[2] = event.values[2];

        // filter to account for gravity in accelerometer readings
        // alpha is calculated as t / (t + dT)
        // with t, the low-pass filter's time-constant
        // and dT, the event delivery rate

       /* final double alpha = 0.8;

        gravity[0] = alpha * gravity[0] + (1 - alpha) * dataArray[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * dataArray[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * dataArray[2];

        acceleration[0] = dataArray[0] - gravity[0];
        acceleration[1] = dataArray[1] - gravity[1];
        acceleration[2] = dataArray[2] - gravity[2];
        */
        if (mySensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

            if (dataArray[0] > xMaxAccel)
                xMaxAccel = dataArray[0];
            if (dataArray[1] > yMaxAccel)
                yMaxAccel = dataArray[1];
            if (dataArray[2] > zMaxAccel)
                zMaxAccel = dataArray[2];
        }
        else if(mySensor.getType() == Sensor.TYPE_GYROSCOPE){
            if (dataArray[0] > xMaxGyro)
                xMaxGyro = dataArray[0];
            if (dataArray[1] > yMaxGyro)
                yMaxGyro = dataArray[1];
            if (dataArray[2] > zMaxGyro)
                zMaxGyro = dataArray[2];
        }
        else if(mySensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            if (dataArray[0] > xMaxM)
                xMaxM = dataArray[0];
            if (dataArray[1] > yMaxM)
                yMaxM = dataArray[1];
            if (dataArray[2] > zMaxM)
                zMaxM = dataArray[2];
        }

        //TextView display of accelerometer data
        TextView textview = (TextView) findViewById(R.id.text);
        if(sensorType==0)
        textview.setText("accel"+"\nx: "+dataArray[0]+"\n"+xMaxAccel+"\ny: "+dataArray[1]+"\n"+yMaxAccel+"\nz: "+dataArray[2]+"\n"+zMaxAccel);
        if(sensorType==1)
            textview.setText("gyro"+"\nx: "+dataArray[0]+"\n"+xMaxGyro+"\ny: "+dataArray[1]+"\n"+yMaxGyro+"\nz: "+dataArray[2]+"\n"+zMaxGyro);
        if(sensorType==2)
            textview.setText("magnet"+"\nx: "+dataArray[0]+"\n"+xMaxM+"\ny: "+dataArray[1]+"\n"+yMaxM+"\nz: "+dataArray[2]+"\n"+zMaxM);



    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}