package com.digutsoft.metronome;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.sql.Time;
import java.util.Date;
import java.util.Random;
import java.util.Set;


public class MainActivity extends Activity implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {



    private static final String TAG = "BluetoothChatFragment";
    private Button onBtn;
    private Button offBtn;
    private Button listBtn;
    private Button findBtn;
    private Button sendMsgBtn;
    private Button mScanBtn;
    private CheckBox mHostCheckbox;
    private TextView text;
    private BluetoothAdapter myBluetoothAdapter;
    private Set<BluetoothDevice> pairedDevices;
    private ListView myListView;
    private ListView mHostListView;
    private ListView mClientListView;
    private ArrayAdapter<String> BTArrayAdapter;
    private boolean mIsHosting;
    private GoogleApiClient mGoogleApiClient;
    private int mBpm = 0;
    private Long mStartTime;

    private static final String BPM_KEY = "com.example.key.BPM";
    private static final String TIME_KEY = "com.example.key.TIME";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
//                case Constants.MESSAGE_WRITE:
//                    byte[] writeBuf = (byte[]) msg.obj;
//                    // construct a string from the buffer
//                    String writeMessage = new String(writeBuf);
//                    mConversationArrayAdapter.add("Me:  " + writeMessage);
//                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Toast.makeText(MainActivity.this, readMessage, Toast.LENGTH_SHORT).show();
                    updateBPM(decodeMessageStartTime(readMessage), decodeMessageBpm(readMessage));
                    updateWatchData();
                    break;
//                case Constants.MESSAGE_DEVICE_NAME:
//                    // save the connected device's name
//                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
//                    if (null != activity) {
//                        Toast.makeText(activity, "Connected to "
//                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
//                    }
////                    break;
//                case Constants.MESSAGE_TOAST:
//                    if (null != activity) {
//                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
//                                Toast.LENGTH_SHORT).show();
//                    }
//                    break;
            }
        }
    };

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName;
    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService;
    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(MainActivity.this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            MainActivity.this.finish();
        }

        setContentView(R.layout.activity_main);

        mIsHosting = false;

        // Populate lists with data
        String[] clients = {"Jimmy", "Peter"};
        ArrayAdapter<String> clientsAdapter = new ArrayAdapter<String>(this, R.layout.list_item, clients);

        mClientListView = (ListView) findViewById(R.id.client_listview);

        mClientListView.setAdapter(clientsAdapter);

        mHostCheckbox = (CheckBox) findViewById(R.id.host_checkbox);
        mScanBtn = (Button) findViewById(R.id.scan_btn);

        mHostCheckbox.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mScanBtn.setEnabled(!isChecked);
                mIsHosting = isChecked;
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mScanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            }
        });


        Button sendBtn = (Button) findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage(String.valueOf(System.currentTimeMillis()) + ":223");
            }
        });

        Button watch = (Button) findViewById(R.id.watch);
        watch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateBPM(System.currentTimeMillis(), new Random().nextInt(40) + 100);
                updateWatchData();
            }
        });
    }

    private void updateWatchData() {
        // Create a DataMap object and send it to the data layer
        DataMap dataMap = new DataMap();
        dataMap.putLong(TIME_KEY, mStartTime);
        dataMap.putInt(BPM_KEY, mBpm);
        //Requires a new thread to avoid blocking the UI
        new SendToDataLayerThread("/bpm", dataMap).start();
        Toast.makeText(MainActivity.this, "Sent message to phone", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {

            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        }

        if (mChatService == null) {
            // Initialize the BluetoothChatService to perform bluetooth connections
            mChatService = new BluetoothChatService(this, mHandler);

            // Initialize the buffer for outgoing messages
            mOutStringBuffer = new StringBuffer("");
        }
    }


    @Override
    protected void onResume() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Toast.makeText(MainActivity.this, "Connected to Watch", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        super.onPause();
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/bpm") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    updateBPM(dataMap.getLong(TIME_KEY), dataMap.getInt(BPM_KEY));
                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(MainActivity.this, "Connection to watch failed", Toast.LENGTH_SHORT).show();
    }

    public void updateBPM(long startTime, int bpm) {
        mStartTime = startTime;
        mBpm = bpm;
    }

    class SendToDataLayerThread extends Thread {
        String path;
        DataMap dataMap;

        // Constructor for sending data objects to the data layer
        SendToDataLayerThread(String p, DataMap data) {
            path = p;
            dataMap = data;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            for (Node node : nodes.getNodes()) {

                // Construct a DataRequest and send over the data layer
                PutDataMapRequest putDMR = PutDataMapRequest.create(path);
                putDMR.getDataMap().putAll(dataMap);
                PutDataRequest request = putDMR.asPutDataRequest();
                DataApi.DataItemResult result = Wearable.DataApi.putDataItem(mGoogleApiClient,request).await();
                if (result.getStatus().isSuccess()) {
                    Log.v("myTag", "DataMap: " + dataMap + " sent to: " + node.getDisplayName());
                } else {
                    // Log an error
                    Log.v("myTag", "ERROR: failed to send DataMap");
                }
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    // Initialize the BluetoothChatService to perform bluetooth connections
                    mChatService = new BluetoothChatService(MainActivity.this, mHandler);

                    // Initialize the buffer for outgoing messages
                    mOutStringBuffer = new StringBuffer("");
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(MainActivity.this, "BT not enabled",
                            Toast.LENGTH_SHORT).show();
                    MainActivity.this.finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    private String encodeMessage(Long startTime, int bpm) {
        return String.format("%d:%d", startTime, bpm);
    }

    private long decodeMessageStartTime(String msg) {
        return Long.parseLong(msg.substring(0, msg.indexOf(":")));
    }

    private int decodeMessageBpm(String msg) {
        return Integer.parseInt(msg.substring(msg.indexOf(":") + 1));
    }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(MainActivity.this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }



}