package com.example.android.cst205_media_share;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.os.Handler;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Nick89 on 5/11/2016.
 */
public class ServerActivity extends AppCompatActivity {

    public static final UUID MY_UUID = UUID.fromString("34f04c1a-17ed-11e6-b6ba-3e1d05defe78");
    public static final String NAME = "BluetoothDemo";

    boolean readyToSend = false;
    TextView screenText;
    Button btn_start, btn_send;
    String filePath = "";
    File root;
    File curFolder;
    ListView dialog_ListView;
    String KEY_TEXTPSS = "TEXTPSS";
    static final int CUSTOM_DIALOG_ID = 0;
    TextView textFolder;
    Button buttonOpenDialog, buttonUp;

    private List<String> fileList = new ArrayList<String>();

    BluetoothAdapter mBluetoothAdapter = null;

    private final int MESSAGE_READ = 1;
    private ConnectedThread mConnectedThread;
    private final int SUCCESS_CONNECT = 0;
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case SUCCESS_CONNECT:
                    //do something
                    if (mConnectedThread != null)
                        mConnectedThread = new ConnectedThread((BluetoothSocket) msg.obj);
                    String send = "Successfully Connected";
                    mConnectedThread.write(send.getBytes());
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String s = readBuf.toString();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.server_activity);

        screenText = (TextView) findViewById(R.id.output);

        btn_start = (Button) findViewById(R.id.start_server);

        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                screenText.append("Starting server\n");
                startServer();
            }
        });

        buttonOpenDialog = (Button) findViewById(R.id.open_dialog);
        buttonOpenDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(CUSTOM_DIALOG_ID);
            }
        });
        root = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        curFolder = root;
        //text field for output info.


        btn_send = (Button) findViewById(R.id.send);

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(readyToSend) {

                    try {
                        Toast.makeText(ServerActivity.this, filePath, Toast.LENGTH_SHORT).show();
                        File file = new File(filePath);
                        FileInputStream fis = new FileInputStream(file);

                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        byte[] buf = new byte[1024];

                        try {
                            for (int readNum; (readNum = fis.read(buf)) != -1; ) {
                                bos.write(buf, 0, readNum);
                            } // end for
                        } // end try
                        catch (IOException ex) {
                            Log.d("File: ", "cannot read from file");
                        } // end catch
                        byte[] bytes = bos.toByteArray();

                        write(bytes);


                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Log.d("File: ", "File not found");
                    }

//////////////////////////////////////////////////////////////
                }
                else {
                    Toast.makeText(ServerActivity.this, "Not Connected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //setup the bluetooth adapter.
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            screenText.append("No bluetooth device.\n");
            btn_start.setEnabled(false);
        }

    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        switch (id) {
            case CUSTOM_DIALOG_ID:
                dialog = new Dialog(this);
                dialog.setContentView(R.layout.dialoglayout);
                dialog.setTitle("Custom Dialog");
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                textFolder = (TextView) dialog.findViewById(R.id.folder);
                buttonUp = (Button) dialog.findViewById(R.id.up);
                buttonUp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ListDir(curFolder.getParentFile());
                    }
                }); //4:20
                dialog_ListView = (ListView) dialog.findViewById(R.id.dialoglist); //4.49
                dialog_ListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        File selected = new File(fileList.get(position)); //4.49
                        if(selected != null)
                            filePath = filePath + selected.getAbsolutePath();
                        if (selected.isDirectory()) {
                            ListDir(selected);
                        } else {
                            filePath = filePath.substring(20, filePath.length());
                            Toast.makeText(view.getContext(), filePath + " selected", Toast.LENGTH_LONG).show();
                            dismissDialog(CUSTOM_DIALOG_ID);
                        }
                    }
                });
                break;
        }
        return dialog;
    }
    //@Override ///5.21
    protected void onPrepare(int id, Dialog dialog){
        super.onPrepareDialog(id, dialog);
        switch(id) {
            case CUSTOM_DIALOG_ID:
                ListDir(curFolder);
                break;
        }
    }
    void ListDir(File f){
        if(f.equals(root)){
            buttonUp.setEnabled(false);
        } else {
            buttonUp.setEnabled(true);
        }
        curFolder = f;
        //String name = f.getName();
        textFolder.setText(f.getPath());
        File[] files = f.listFiles();
        fileList.clear();
        for(File file: files){
            if(file.isDirectory())
                fileList.add(file.getPath());
            else
                fileList.add(file.getName());
        }
        ArrayAdapter<String> directoryList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fileList);
        dialog_ListView.setAdapter(directoryList);
    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            screenText.append(msg.getData().getString("msg"));
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

    public void write(byte[] out) {
        // Create temporary object
        /*
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
        */
        mConnectedThread.write(out);
    }

    public void startServer() {
        new Thread(new AcceptThread()).start();
    }

    private void connected(BluetoothSocket socket, BluetoothDevice remoteDevice) {
        // Cancel the thread that completed the connection
        //if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        // Cancel the accept thread because we only want to connect to one device
        //if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        mkmsg("Connected\n");
        readyToSend = true;
    }


    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */

    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            // Create a new listening server socket
            try {
                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
               Log.d("Server:", "Failed to start server");
            }
            mmServerSocket = tmp;
        }

        public void run() {
            Log.d("Searching: ", "waiting on accept");
            BluetoothSocket socket = null;
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                Log.d("Server: ", "Failed to accept");
            }

            // If a connection was accepted
            if (socket != null) {
                mkmsg("Connection made\n");
                mkmsg("Remote device address: " + socket.getRemoteDevice().getAddress() + "\n");
                connected(socket, socket.getRemoteDevice());
                try {

                } catch (Exception e) {
                    Log.d("Sending: ", "Error sending");
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.d("Socket: ", "Failed to close");
            }
        }
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
            byte[] buffer; // buffer store for the stream
            int bytes; // bytes returned from read()

            //listen to input stream until exception
            while(true) {
                try {
                    buffer = new byte[1024];
                    //read from inputstream
                    bytes = mmInStream.read(buffer);
                    // send obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
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
