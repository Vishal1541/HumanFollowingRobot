package com.example.vishalanand.humanfollowingrobot;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;

public class SecondActivity extends AppCompatActivity implements SensorEventListener {

    private Button b_Start;
    private TextView tv_Lat_Long;
    private LocationManager locationManager;
    private LocationListener listener;

    double degree=0.0;
    private TextView tv_Degree;
    private SensorManager sensorManager;

    private Button b_Dist;
    private EditText et_Dist;

    int dist_inacc;
    int command=0;

    int count=0;

    double lati, longi;
    double lati1=0, lati2=0, longi1=0, longi2;
    double A, C, D;
    final double Radius=637100000.0;  //cm
    final double PI = Math.PI;
    double Angle1, Angle2, delta_Angle;
    double phi1, phi2, lamda1, lamda2;          //phi - latitiude, lamba - longitude in radians
    double delta_phi, delta_lamda;


    String address = null;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        Intent newint = getIntent();
        address = newint.getStringExtra(MainActivity.EXTRA_ADDRESS);
        new ConnectBT().execute();

        tv_Degree = (TextView)findViewById(R.id.tv_degree);
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        b_Start = (Button)findViewById(R.id.b_start);
        tv_Lat_Long = (TextView)findViewById(R.id.tv_lat_long);

        b_Dist = (Button)findViewById(R.id.b_dist);
        et_Dist = (EditText)findViewById(R.id.et_dist);

        b_Dist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dist_inacc = Integer.parseInt(et_Dist.getText().toString());
                et_Dist.setText("");
            }
        });

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                longi = location.getLongitude();
                lati = location.getLatitude();
                longi = Math.toRadians(longi);
                lati = Math.toRadians(lati);

                calcDistanceAngle();

                calcCommand();
                if(count==0)
                    tv_Lat_Long.append("\nChecking Inaccuracy...\n");
                if(count>6) {
                    if (count == 7)
                        tv_Lat_Long.append("\nSending data...\n");
                    sendCommand();
                }
                tv_Lat_Long.append("\n " + D + "cm" + "\t\t\t\t" + command + "\t\t\t\t" + delta_Angle);
                count++;
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };

        configure_button();
    }


    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 10:
                configure_button();
                break;
            default:
                break;
        }
    }

    void calcCommand(){
        if(D<dist_inacc)
            command = 0;
        else if(delta_Angle>=0 && delta_Angle<22.5)
            command = 1;
        else if(delta_Angle>=22.5 && delta_Angle<67.5)
            command = 2;
        else if(delta_Angle>=67.5 && delta_Angle<112.5)
            command = 3;
        else if(delta_Angle>=112.5 && delta_Angle<157.5)
            command = 4;
        else if(delta_Angle>=157.5 && delta_Angle<180)
            command = 5;
        else if(delta_Angle>=-180 && delta_Angle<-157.5)
            command = 5;
        else if(delta_Angle>=-157.5 && delta_Angle<-112.5)
            command = 6;
        else if(delta_Angle>=-112.5 && delta_Angle<-67.5)
            command = 7;
        else if(delta_Angle>=-67.5 && delta_Angle<-22.5)
            command = 8;
        else if(delta_Angle>=-22.5 && delta_Angle<0)
            command = 1;
    }

    void sendCommand(){
            if (btSocket!=null)
            {
                try
                {
                    btSocket.getOutputStream().write(Integer.toString(command).getBytes());
                }
                catch (IOException e)
                {
                    msg("Error");
                }
            }
    }

    void calcDistanceAngle(){
        lati1 = lati2;
        lati2 = lati;

        longi1 = longi2;
        longi2 = longi;

        //phi - latitiude, lamba - longitude in radians

        phi1 = lati1;
        phi2 = lati2;

        lamda1 = longi1;
        lamda2 = longi2;

        delta_phi = phi2-phi1;
        delta_lamda = lamda2-lamda1;

        A = Math.pow(Math.sin(delta_lamda/2.0),2) + Math.cos(phi1)*Math.cos(phi2)* Math.pow(Math.sin(delta_lamda/2.0),2);
        C = 2.0*Math.atan2(Math.sqrt(A),Math.sqrt(1-A));
        D = Radius*C;
        D = Math.round(D*100.0)/100.0;

        calcDegree();
    }

    void calcDegree(){
        Angle1 = Angle2;
        Angle2 = degree;
        delta_Angle = Angle2 - Angle1;
        if(delta_Angle >=-360 && delta_Angle <-180)
            delta_Angle += 360;
        else  if(delta_Angle >=180 && delta_Angle < 360)
            delta_Angle -= 360;
    }

    /*void calcAngle(){
        //theta = Math.atan2(Math.sin(delta_lamda)*Math.cos(phi2),Math.cos(phi1)*Math.sin(phi2)-Math.sin(phi1)*Math.cos(phi2)*Math.cos(delta_lamda));
        //theta = theta*180.0/PI;
        double dLon = (longi2 - longi1);

        double y = Math.sin(dLon) * Math.cos(lati2);
        double x = Math.cos(lati1) * Math.sin(lati2) - Math.sin(lati1)
                * Math.cos(lati2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;
        brng = 360 - brng;
        Angle = brng;
        Angle = Math.round(Angle);

        if(Angle>180)
            Angle = Angle - 360;
    }
    */
    void configure_button(){
        // first check for permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET}
                        ,10);
            }
            return;
        }
        // this code won't execute IF permissions are not allowed, because in the line above there is return statement.
        b_Start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //noinspection MissingPermission
                tv_Lat_Long.append("Started!\n");
                locationManager.requestLocationUpdates("gps", 500, 0, listener);
            }
        });
    }

    private void msg(String s){
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        degree = Math.round(event.values[0]);
        tv_Degree.setText("Degrees : " + degree);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>{
        private boolean ConnectSuccess = true;
        @Override
        protected void onPreExecute(){
            progress = ProgressDialog.show(SecondActivity.this,"Connecting...","Please wait!!!");
        }
        @Override
        protected Void doInBackground(Void... devices){
            try{
                if(btSocket == null || !isBtConnected){
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result){
            super.onPostExecute(result);
            if(!ConnectSuccess){
                msg("Connection failed.");
                finish();
            }
            else{
                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }
}
