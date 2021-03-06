/*
 * MainActivity.java -- Simple demo application for the Thingsee cloud server agent
 *
 * Request 20 latest position measurements and displays them on the
 * listview wigdet.
 *
 * Note: you need to insert the following line before application -tag in
 * the AndroidManifest.xml file
 *  <uses-permission android:name="android.permission.INTERNET" />
 *
 * Author(s): Jarkko Vuori
 * Modification(s):
 *   First version created on 04.02.2017
 *   Clears the positions array before button pressed 15.02.2017
 *   Stores username and password to SharedPreferences 17.02.2017
 */
package daniel_vahlametropolia.thingsee;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.CursorIndexOutOfBoundsException;
import android.location.Location;
import android.os.AsyncTask;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.support.v4.app.FragmentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import daniel_vahlametropolia.thingsee.R;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;



public class MainActivity extends FragmentActivity implements View.OnClickListener {
    final int EXECUTE_ORDER = 6;
    private static final int    MAXPOSITIONS = 1;
    private static final String PREFERENCEID = "Credentials";

    private String               username, password;
    private String[]             positions = new String[MAXPOSITIONS];
    private ArrayAdapter<String> myAdapter;

    private Handler handler;
    private int interval = 5000;

    private Location FirstLoc = null;
    private Location LastLoc = null;
    private Location CurrentLoc = null;

    public Double TotalDistance = null;
    private Double CurrentSpeed = null;
    public Double AverageSpeed = null;
    private Double DeltaDist = null;

    public Double TotalTime = null;
    private Double CurrentTime = null;
    private Double LastTime = null;
    private Double FirstTime = null;
    private Double DeltaTime = null;

    private TextView SpeedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startButton = (Button)findViewById(R.id.button_start);
        Button stopButton = (Button)findViewById(R.id.button_stop);
        Button statisticButton = (Button)findViewById(R.id.button_statistics);

        startButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
        statisticButton.setOnClickListener(this);

        SpeedText = (TextView)findViewById(R.id.SpeedValue);


        // initialize the array so that every position has an object (even it is empty string)
        for (int i = 0; i < positions.length; i++)
            positions[i] = "";

        // check that we know username and password for the Thingsee cloud
        SharedPreferences prefGet = getSharedPreferences(PREFERENCEID, Activity.MODE_PRIVATE);
        username = prefGet.getString("username", "");
        password = prefGet.getString("password", "");
        if (username.length() == 0 || password.length() == 0)
            // no, ask them from the user
            queryDialog(this, getResources().getString(R.string.prompt));

        //create a handler for repeating tasks
        handler = new Handler();
    }
    private Runnable UpdateData = new Runnable() {
        @Override
        public void run() {
            // we make the request to the Thingsee cloud server in backgroud
            // (AsyncTask) so that we don't block the UI (to prevent ANR state, Android Not Responding)
            new TalkToThingsee().execute("QueryState");

            //add delay for executing this task again
            handler.postDelayed(this,interval);
        }
    };

    // Call this method to start repeatedly requesting updates from Cloud
    private void startRepeatingTask(){
        UpdateData.run();
    }

    // Call this method to stop requesting updates from Cloud
    private void stopRepeatingTask(){
        handler.removeCallbacks(UpdateData);
    }

    private void queryDialog(Context context, String msg) {
        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.credentials_dialog, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final TextView dialogMsg      = (TextView) promptsView.findViewById(R.id.textViewDialogMsg);
        final EditText dialogUsername = (EditText) promptsView.findViewById(R.id.editTextDialogUsername);
        final EditText dialogPassword = (EditText) promptsView.findViewById(R.id.editTextDialogPassword);

        dialogMsg.setText(msg);
        dialogUsername.setText(username);
        dialogPassword.setText(password);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                // get user input and set it to result
                                username = dialogUsername.getText().toString();
                                password = dialogPassword.getText().toString();

                                SharedPreferences prefPut = getSharedPreferences(PREFERENCEID, Activity.MODE_PRIVATE);
                                SharedPreferences.Editor prefEditor = prefPut.edit();
                                prefEditor.putString("username", username);
                                prefEditor.putString("password", password);
                                prefEditor.commit();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    public void onClick(View v) {
        Log.d("USR", "Button pressed");

        if(v.getId() == R.id.button_start){
            startRepeatingTask();
        }

        if(v.getId() == R.id.button_stop){
            stopRepeatingTask();
        }

        if(v.getId() == R.id.button_statistics){
            Intent myIntent = new Intent(this, StatisticsActivity.class);
            myIntent.putExtra("totalDistance",TotalDistance);
            myIntent.putExtra("totalTime",TotalTime);
            myIntent.putExtra("aveSpeed",AverageSpeed);
            startActivityForResult(myIntent,EXECUTE_ORDER);
        }
    }

    public void onActivityResult(){};

    /* This class communicates with the ThingSee client on a separate thread (background processing)
     * so that it does not slow down the user interface (UI)
     */
    private class TalkToThingsee extends AsyncTask<String, Integer, String> {
        ThingSee       thingsee;
        List<Location> coordinates = new ArrayList<Location>();

        @Override
        protected String doInBackground(String... params) {
            String result = "NOT OK";

            // here we make the request to the cloud server for MAXPOSITION number of coordinates
            try {
                thingsee = new ThingSee(username, password);

                JSONArray events = thingsee.Events(thingsee.Devices(), MAXPOSITIONS);
                //System.out.println(events);
                coordinates = thingsee.getPath(events);

//                for (Location coordinate: coordinates)
//                    System.out.println(coordinate);
                result = "OK";
            } catch(Exception e) {
                Log.d("NET", "Communication error: " + e.getMessage());
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            // check that the background communication with the client was succesfull
            if (result.equals("OK")) {
                // now the coordinates variable has those coordinates
                // elements of these coordinates is the Location object who has
                // fields for longitude, latitude and time when the position was fixed
                for (int i = 0; i < coordinates.size(); i++) {
                    Location loc = coordinates.get(i);

                    CalculationRoutine(loc);

                    /* Old line for showing stuff
                    positions[i] = (new Date(loc.getTime())) +
                                   " (" + loc.getLatitude() + "," +
                                   loc.getLongitude() + ")"; //coordinates.get(i).toString();
                                   */
                }
            } else {
                // no, tell that to the user and ask a new username/password pair
                // *** now it will try again ***
                new TalkToThingsee().execute("QueryState");
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Integer... values) {}
    }

    private void CalculationRoutine(Location loc){
        CurrentLoc = loc;
        CurrentTime = (double)loc.getTime() / 1000;

        if(FirstLoc == null){
            FirstLoc = loc;
            LastLoc = loc;
            FirstTime = (double)loc.getTime() / 1000;
            LastTime = (double)loc.getTime() / 1000;
            CurrentSpeed = 0d;
            AverageSpeed = 0d;
            TotalDistance = 0d;
            TotalTime = 0d;
            DeltaTime = 0d;
        }

        if(CurrentLoc.getTime()!=LastLoc.getTime()){
            TotalTime = CurrentTime - FirstTime;
            DeltaTime = CurrentTime - LastTime;

            DeltaDist = HaversineDistance(CurrentLoc,LastLoc);
            TotalDistance += DeltaDist;

            CurrentSpeed = DeltaDist / DeltaTime;
            AverageSpeed = TotalDistance / TotalTime;

            LastLoc = CurrentLoc;
            LastTime = CurrentTime;
        }

        SpeedText.setText(CurrentSpeed.toString()+" km/s");

    }
    private double HaversineDistance(Location current, Location last){
        double EARTH_RADIUS = 6371d;

        double dLat = Math.toRadians(( current.getLatitude() - last.getLatitude() ));
        double dLong = Math.toRadians((current.getLongitude() - last.getLongitude() ));

        double startLat = Math.toRadians(last.getLatitude());
        double endLat = Math.toRadians(current.getLatitude());

        double a = Math.pow(Math.sin(dLat /2),2) +  Math.pow(Math.sin(dLong /2),2) * Math.cos(endLat) * Math.cos(startLat);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        double d = EARTH_RADIUS * c;

        return d;
    }
}