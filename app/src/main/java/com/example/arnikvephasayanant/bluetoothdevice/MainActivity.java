package com.example.arnikvephasayanant.bluetoothdevice;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.nfc.Tag;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "bluetooth";
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder sb = new StringBuilder();
    private ConnectedThread mConnectedThread;
    private TextView txtAndroid;
    private TextView txtStatus;
    private TextView txtSpo2;

    final int RECIEVE_MESSAGE = 1;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static String MACaddr = "98:D3:32:20:BA:A7";
    private StringBuilder recDataString = new StringBuilder();

    Handler h;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {


        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        txtAndroid = findViewById(R.id.t1);
        txtStatus = findViewById(R.id.txtStatus);
        txtSpo2 = findViewById(R.id.txtSpo2);

        h = new Handler() {

        @SuppressLint("SetTextI18n")
        public void handleMessage(android.os.Message msg) {
            Log.d(TAG, "handle handle");

            switch (msg.what) {
            case RECIEVE_MESSAGE:                                                   // if receive massage
                String readMessage = (String) msg.obj;
                recDataString.append(readMessage);

                             // create string from bytes array
                                                 // append string
                int endOfLineIndex = recDataString.indexOf("\n");                            // determine the end-of-line
                if (endOfLineIndex > 0) {                                            // if end-of-line,
                    String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
                    // extract string
                    String arr[] = dataInPrint.split(",");
                    //Log.d(TAG, arr[0].trim());
//                    Log.d(TAG, arr[1].trim());
//                    Log.d(TAG, arr[2].trim());


                    Log.i(TAG, "value of input "+dataInPrint);
                    Log.i(TAG, "info value "+arr.length+" , "+arr+" dataInPrint");

                    for(String temp : arr){
                        Log.i(TAG, "arr "+temp);

                    }

                    if(arr.length > 2) {
                        String measure = arr[0].trim();
                        String hr = arr[1].trim();
                        String spo2 = arr[2].trim();

                        String status = "";
//                        measure = measure;
//                        hr = hr;
//                        spo2 = spo2;
                        int x = Integer.parseInt(measure);
                        int y = Integer.parseInt(hr);
                        int z = Integer.parseInt(spo2);

                        if (x == 0) {
                            txtAndroid.setText("--");
                            txtSpo2.setText("--");
                            txtStatus.setText("");
                        } else {
                            txtAndroid.setText(hr);
                            txtSpo2.setText(spo2);
                            if (z < 90) {
                                txtStatus.setText("รุนแรง");
                            } else
                            {
                                txtStatus.setText("ฉุกเฉิน");
                            }
                            // update TextView
                            if (z >= 90 && z <= 95) {
                                txtStatus.setText("ปานกลาง");
                            }
                            else if (z > 95 && z <= 100) {
                                    txtStatus.setText("ปกติ");
                            }
                        }
                    }



                             // update TextView
                    Log.d(TAG, "...String:"+recDataString.toString() );

                    dataInPrint = " ";
                    recDataString.delete(0, recDataString.length()); 					//clear all string data


                }// end if

                Log.d(TAG,"test : "+recDataString.toString());
               // txtAndroid.setText(readMessage);
                break;
            }
        };
    };
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();


    }
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if(Build.VERSION.SDK_INT >= 10){
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection",e);
            }
        }
        return  device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "...onResume - try connect...");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(MACaddr);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try {
            btSocket.connect();
            Log.d(TAG, "....Connection ok...");
            Log.d(TAG, "...Create Socket...");

            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();
        } catch (IOException e) {
            try {
                btSocket.close();
                btSocket = null;
            } catch (IOException e2) {
                btSocket =null;

                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.

        //
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        try     {
            btSocket.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }
    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }
    private class ConnectedThread extends Thread{
        private InputStream mmInStream ;

        InputStream tmpIn = null;

        public ConnectedThread(BluetoothSocket socket) {


            try {
                tmpIn = socket.getInputStream();


            } catch (IOException e) {



            }
            mmInStream = tmpIn;
        }

            public void run(){
                byte[] buffer = new byte[256];
                int bytes;

                while(true){
                    try{
                        bytes=mmInStream.read(buffer);
                        String readMessage = new String(buffer, 0, bytes);

                        Log.d(TAG,"message message = = =" + readMessage);

                        h.obtainMessage(RECIEVE_MESSAGE,bytes,-1,readMessage).sendToTarget();
                    }catch (IOException e){
                        break;
                    }

                }
            }


        }
}


