package com.example.android.cst205_media_share;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.os.Handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * Created by Nick89 on 5/11/2016.
 */
public class ClientActivity extends AppCompatActivity {
    String TAG = "client";
    TextView output;
    Button btn_start, btn_device;
    BluetoothAdapter mBluetoothAdapter = null;
    BluetoothDevice device;
    BluetoothDevice remoteDevice;
    Button end, toMusicPlayer;

    String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();

    File newFile = new File(filePath + "/newFile.mp3");

    FileOutputStream fos = null;

    long size = 0;


    private final int MESSAGE_READ = 1;
    private ConnectedThread mConnectedThread;
    private final int SUCCESS_CONNECT = 0;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    try {
                        if(fos != null) {

                            fos.write(readBuf);
                        }
                        else
                            Log.d("File: ", "File is still null");
                    } catch(IOException e) {Log.d("File: ", "File not writing/saving"); }

                    break;
            }
        }
    };

    public void close() {
        try {
            fos.flush();
            fos.close();
        } catch(IOException e) { Log.d("File: ", "Closed");}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.client_layout);
        output = (TextView) findViewById(R.id.ct_output);

        btn_device = (Button) findViewById(R.id.which_device);
        btn_device.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                querypaired();
            }
        });
        btn_start = (Button) findViewById(R.id.start_client);
        btn_start.setEnabled(false);
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                output.append("Starting client\n");
                startClient();
            }
        });

        toMusicPlayer = (Button) findViewById(R.id.toPreview);
        toMusicPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent nextScreen = new Intent(v.getContext(), MusicPlayerActivity.class);
                String pathToSend = newFile.getAbsolutePath();
                nextScreen.putExtra("File", pathToSend);
                startActivity(nextScreen);
            }
        });
        end = (Button) findViewById(R.id.end);

        end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                close();
            }
        });
        //setup the bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null) {
            //Device does not support Bluetooth
            output.append("No bluetooth device.]n");
            btn_start.setEnabled(false);
            btn_device.setEnabled(false);
        }
        Log.v(TAG, "bluetooth");
    }

    public void querypaired() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        //if there are paired devices
        if(pairedDevices.size() > 0) {
            //loop thru paired devices
            output.append("at least 1 paired device\n");
            final BluetoothDevice blueDev[] = new BluetoothDevice[pairedDevices.size()];
            String[] items = new String[blueDev.length];
            int i = 0;
            for(BluetoothDevice device1 :pairedDevices) {
                blueDev[i] = device1;
                items[i] = blueDev[i].getName() + ": " + blueDev[i].getAddress();
                output.append("Device: " + items[i] + "\n");
                //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                i++;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Choose Bluetooth:");
            builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    dialog.dismiss();
                    if (item >= 0 && item < blueDev.length) {
                        device = blueDev[item];
                        btn_device.setText("device: " + blueDev[item].getName());
                        btn_start.setEnabled(true);
                    }
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    public void startClient() {
        if(device != null) {
            output.append("Connecting................\n");
            new Thread(new ConnectThread(device)).start();
        }
        else {
            Log.d("Device: ", "Failed to connect");
        }
    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            output.append(msg.getData().getString("msg"));
            return true;
        }

    });

    public void mkmsg(String str) {
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("msg", str);
        msg.setData(b);
        handler.sendMessage(msg);
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */

    private class ConnectThread extends Thread {
        private BluetoothSocket socket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            //Get a bluetoothSocket fro a connection with given device

            try {
                tmp = device.createRfcommSocketToServiceRecord(ServerActivity.MY_UUID);
            } catch(IOException e) {
                Log.d("Client: ","Client connection failed: " + e.getMessage() + "\n");
            }
            socket = tmp;
        }

        public void run() {
            //Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            //Make a connection to the BluetoothSocket
            try {
                //This is a blocking call and will only return on
                //successful connection or an exception
                socket.connect();
            } catch (IOException e) {
                Log.d("Socket: ","Connect failed\n");
                try {
                    socket.close();
                    socket = null;
                } catch (IOException e2) {
                    Log.d("Socket: ", "unable to close() socket during connection failure: " + e2.getMessage() + "\n");
                    socket = null;
                }
                //Start the service over to restart listening mode
            }
            // if a connection was accepted
            if(socket != null) {
                mkmsg("Connection made\n");
                mkmsg("Remote device address: " + socket.getRemoteDevice().getAddress() + "\n");
                connected(socket, socket.getRemoteDevice());
                try {

                } catch(Exception e) {
                    Log.d("Receiving: ", "Error happened sending/receiving\n");
                }
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.d("Socket:", "close() of connect socket failed: " + e.getMessage() + "\n");
            }
        }
    }

    private void connected(BluetoothSocket socket, BluetoothDevice remoteDevice) {

        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}

        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        try {
            fos = new FileOutputStream(newFile);
        } catch(FileNotFoundException e) {Log.d("File: ", "File not found"); }
    }


    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch(IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[3000];// = new byte[337292]; // buffer store for the stream
            int bytes; // bytes returned from read()
            byte[] toSend;

            //listen to input stream until exception
            while(true) {
                try {

                    bytes = mmInStream.read(buffer);
                    toSend = new byte[bytes];

                    for(int ix = 0; ix < bytes; ix++)
                        toSend[ix] = buffer[ix];

                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, toSend).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        // call this from main activity to send data to the remote device

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        //Call this from the main activity to shutdown connection
        public void cancel() {
            try {
                mmSocket.close();
            } catch(IOException e) { }
        }
    }


}
