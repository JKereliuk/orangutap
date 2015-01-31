package com.digutsoft.metronome;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import java.util.Set;


public class MainActivity extends Activity implements
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final int REQUEST_ENABLE_BT = 1;
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
    private Time mStartTime;

    private static final String BPM_KEY = "com.example.key.BPM";
    private static final String TIME_KEY = "com.example.key.TIME";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mIsHosting = false;

        // Populate lists with data
        String[] clients = {"Jimmy", "Peter"};
        ArrayAdapter<String> clientsAdapter = new ArrayAdapter<String>(this, R.layout.list_item, clients);

        String[] hosts = {"Scottie", "Martin"};
        ArrayAdapter<String> hostsAdapter = new ArrayAdapter<String>(this, R.layout.list_item, hosts);

        mClientListView = (ListView) findViewById(R.id.client_listview);
        mHostListView = (ListView) findViewById(R.id.host_listview);

        mClientListView.setAdapter(clientsAdapter);
        mHostListView.setAdapter(hostsAdapter);

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

                // Create a DataMap object and send it to the data layer
                DataMap dataMap = new DataMap();
                dataMap.putLong(TIME_KEY, new Date().getTime());
                dataMap.putInt(BPM_KEY, 140);
                //Requires a new thread to avoid blocking the UI
                new SendToDataLayerThread("/bpm", dataMap).start();
            }
        });
        // take an instance of BluetoothAdapter - Bluetooth radio
//        myBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if(myBluetoothAdapter == null) {
//            onBtn.setEnabled(false);
//            offBtn.setEnabled(false);
//            listBtn.setEnabled(false);
//            findBtn.setEnabled(false);
//            sendMsgBtn.setEnabled(false);
//            text.setText("Status: not supported");
//
//            Toast.makeText(getApplicationContext(),"Your device does not support Bluetooth",
//                    Toast.LENGTH_LONG).show();
//        } else {
//            text = (TextView) findViewById(R.id.text);
//            onBtn = (Button)findViewById(R.id.turnOn);
//            onBtn.setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    // TODO Auto-generated method stub
//                    on(v);
//                }
//            });
//
//            offBtn = (Button)findViewById(R.id.turnOff);
//            offBtn.setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    // TODO Auto-generated method stub
//                    off(v);
//                }
//            });
//
//            listBtn = (Button)findViewById(R.id.paired);
//            listBtn.setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    // TODO Auto-generated method stub
//                    list(v);
//                }
//            });
//
//            findBtn = (Button)findViewById(R.id.search);
//            findBtn.setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    // TODO Auto-generated method stub
//                    find(v);
//                }
//            });
//
//            sendMsgBtn = (Button)findViewById(R.id.send_message);
//            sendMsgBtn.setOnClickListener(new View.OnClickListener() {
//
//                @Override
//                public void onClick(View v) {
//                    // TODO Auto-generated method stub
//                    sendMessage(v);
//                }
//            });
//
//            myListView = (ListView)findViewById(R.id.listView1);
//
//            // create the arrayAdapter that contains the BTDevices, and set it to the ListView
//            BTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
//            myListView.setAdapter(BTArrayAdapter);
//        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    public void on(View view){
//        if (!myBluetoothAdapter.isEnabled()) {
//            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);
//
//            Toast.makeText(getApplicationContext(),"Bluetooth turned on" ,
//                    Toast.LENGTH_LONG).show();
//        }
//        else{
//            Toast.makeText(getApplicationContext(),"Bluetooth is already on",
//                    Toast.LENGTH_LONG).show();
//        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        // TODO Auto-generated method stub
//        if(requestCode == REQUEST_ENABLE_BT){
//            if(myBluetoothAdapter.isEnabled()) {
//                text.setText("Status: Enabled");
//            } else {
//                text.setText("Status: Disabled");
//            }
//        }
    }

    public void list(View view){
//        // get paired devices
//        pairedDevices = myBluetoothAdapter.getBondedDevices();
//
//        // put it's one to the adapter
//        for(BluetoothDevice device : pairedDevices)
//            BTArrayAdapter.add(device.getName()+ "\n" + device.getAddress());
//
//        Toast.makeText(getApplicationContext(),"Show Paired Devices",
//                Toast.LENGTH_SHORT).show();

    }

    final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            // When discovery finds a device
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                // Get the BluetoothDevice object from the Intent
//                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                // add the name and the MAC address of the object to the arrayAdapter
//                BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
//                BTArrayAdapter.notifyDataSetChanged();
//            }
        }
    };

    public void find(View view) {
//        if (myBluetoothAdapter.isDiscovering()) {
//            // the button is pressed when it discovers, so cancel the discovery
//            myBluetoothAdapter.cancelDiscovery();
//        }
//        else {
//            BTArrayAdapter.clear();
//            myBluetoothAdapter.startDiscovery();
//
//            registerReceiver(bReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
//        }
    }

    public void off(View view){
//        myBluetoothAdapter.disable();
//        text.setText("Status: Disconnected");
//
//        Toast.makeText(getApplicationContext(), "Bluetooth turned off",
//                Toast.LENGTH_LONG).show();
    }

    public void sendMessage(View view){
//        // get paired devices
//        pairedDevices = myBluetoothAdapter.getBondedDevices();
//
//        // put it's one to the adapter
//        for(BluetoothDevice device : pairedDevices) {
//            //send message
//        }
//
//        Toast.makeText(getApplicationContext(),"Message Sent",
//                Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
//        unregisterReceiver(bReceiver);
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
        mStartTime = new Time(startTime);
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
}