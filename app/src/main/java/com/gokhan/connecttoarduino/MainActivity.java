package com.gokhan.connecttoarduino;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import android.content.ActivityNotFoundException;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import java.util.ArrayList;
import java.util.Locale;


    public class MainActivity extends AppCompatActivity implements OLEDBroadcastReceiver.AppReceiver,
            CustomHandler.AppHandlerReceiver, AdapterView.OnItemClickListener, BluetoothProfile.ServiceListener {


            private final int REQUEST_CODE_SPEECH_INPUT = 100;

            Button konusButon;
            TextView yaziyiGoster;


            @Override
            protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_main);
                initViews();
                initBluetooth();

                konusButon = (Button) findViewById(R.id.konusButon);
                yaziyiGoster = (TextView) findViewById(R.id.yaziyiGoster);

                konusButon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        konusDialog();
                    }
                });
            }

            private void konusDialog() {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                // getDefault metodu telefonun seçili olan dilini döndürür.
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Konuş bakalım.");

                try {
                    startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
                } catch (ActivityNotFoundException a) {
                    Toast.makeText(getApplicationContext(), "Desteklenmiyor", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            protected void onActivityResult(int requestCode, int resultCode, Intent data) {
                super.onActivityResult(requestCode, resultCode, data);

                switch (requestCode) {
                    case REQUEST_CODE_SPEECH_INPUT: {
                        if (resultCode == RESULT_OK && data != null) {
                            ArrayList<String> donus = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                            // intentden dönen veriyi arrayliste atıyoruz

                            yaziyiGoster.setText(donus.get(0));
                        }

                        break;
                    }
                }
                // Check which request we're responding to
                if (requestCode == REQUEST_ENABLE_BT) {
                    // Make sure the request was successful
                    if (resultCode == RESULT_OK) {
                        // The user picked a contact.
                        // The Intent's data Uri identifies which contact was selected.
                        mBluetoothStatus.setText("Enabled");
                    } else
                        mBluetoothStatus.setText("Disabled");
                }
            }

        private Set pairedDevices;

        // GUI Components
        private TextView mBluetoothStatus;
        private TextView mReadBuffer;
        private Button mScanBtn;
        private Button mOffBtn;
        private Button mListPairedDevicesBtn;
        private Button mDiscoverBtn;
        private BluetoothAdapter mBTAdapter;
        private Set<BluetoothDevice> mPairedDevices;
        private ArrayAdapter<String> mBTArrayAdapter;
        private ListView mDevicesListView;
        private CheckBox mLED1;
        BluetoothHeadset bluetoothHeadset;
        private CustomHandler mHandler; // Our main handler that will receive callback notifications
        private ConnectedThread mConnectedThread; // bluetooth background worker thread to send and receive data
        private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path

        private OLEDBroadcastReceiver oledBroadcastReceiver;

        private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier


        // #defines for identifying shared types between calling functions
        private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
        private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
        private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status


        /*@Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);
            initViews();
            initBluetooth();
        }*/

        private void initViews() {
            mBluetoothStatus = (TextView) findViewById(R.id.bluetoothStatus);
            mReadBuffer = (TextView) findViewById(R.id.readBuffer);
            mScanBtn = (Button) findViewById(R.id.scan);
            mOffBtn = (Button) findViewById(R.id.off);
            mDiscoverBtn = (Button) findViewById(R.id.discover);
            mListPairedDevicesBtn = (Button) findViewById(R.id.PairedBtn);
            mLED1 = (CheckBox) findViewById(R.id.checkboxLED1);

            mBTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
            mBTAdapter = BluetoothAdapter.getDefaultAdapter(); // get a handle on the bluetooth radio

            mDevicesListView = (ListView) findViewById(R.id.devicesListView);
            mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view

            mDevicesListView.setOnItemClickListener(this);
            mHandler = new CustomHandler(this);
            oledBroadcastReceiver = new OLEDBroadcastReceiver(this);
        }


        private void initBluetooth() {
            if (mBTArrayAdapter == null) {
                // Device does not support Bluetooth
                mBluetoothStatus.setText("Status: Bluetooth not found");
                Toast.makeText(getApplicationContext(), "Bluetooth device not found!", Toast.LENGTH_SHORT).show();
            } else {

                mLED1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mConnectedThread != null) //First check to make sure thread created
                            mConnectedThread.write("1");
                    }
                });


                mScanBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bluetoothOn();
                    }
                });

                mOffBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bluetoothOff();
                    }
                });

                mListPairedDevicesBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listPairedDevices();
                    }
                });

                mDiscoverBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        discover();
                    }
                });
            }
        }

        private void bluetoothOn() {
            if (!mBTAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                mBluetoothStatus.setText("Bluetooth enabled");
                Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth is already on", Toast.LENGTH_SHORT).show();
            }
        }

        private void bluetoothOff() {
            mBTAdapter.disable(); // turn off
            mBluetoothStatus.setText("Bluetooth disabled");
            Toast.makeText(getApplicationContext(), "Bluetooth turned Off", Toast.LENGTH_SHORT).show();
        }

        private void discover() {
            // Check if the device is already discovering
            if (mBTAdapter.isDiscovering()) {
                mBTAdapter.cancelDiscovery();
                Toast.makeText(getApplicationContext(), "Discovery stopped", Toast.LENGTH_SHORT).show();
            } else {
                if (mBTAdapter.isEnabled()) {
                    mBTArrayAdapter.clear(); // clear items
                    mBTAdapter.startDiscovery();
                    Toast.makeText(getApplicationContext(), "Discovery started", Toast.LENGTH_SHORT).show();
                    registerReceiver(oledBroadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
                } else {
                    Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                }
            }
        }

        // Enter here after user selects "yes" or "no" to enabling radio
        /*@Override
        protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
            // Check which request we're responding to
            if (requestCode == REQUEST_ENABLE_BT) {
                // Make sure the request was successful
                if (resultCode == RESULT_OK) {
                    // The user picked a contact.
                    // The Intent's data Uri identifies which contact was selected.
                    mBluetoothStatus.setText("Enabled");
                } else
                    mBluetoothStatus.setText("Disabled");
            }
        }*/

        private void listPairedDevices() {
            mPairedDevices = mBTAdapter.getBondedDevices();
            if (mBTAdapter.isEnabled()) {
                // put it's one to the adapter
                for (BluetoothDevice device : mPairedDevices)
                    mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

                Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(getApplicationContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
        }


        private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
            return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
            //creates secure outgoing connection with BT device using UUID
        }


        @Override
        protected void onStop() {
            super.onStop();
            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            // unregister the ACTION_FOUND receiver.
            unregisterReceiver(oledBroadcastReceiver);
        }

        /**
         * Interface method of OLEDBroadcastReceiver
         *
         * @param bluetoothDevice from the receiver
         */
        @Override
        public void onReceiveResult(BluetoothDevice bluetoothDevice, Intent intent) {
            String deviceName = bluetoothDevice.getName();
            String deviceHardwareAddress = bluetoothDevice.getAddress();
            mBTArrayAdapter.add(deviceName + "\n" + deviceHardwareAddress);
            mBTArrayAdapter.notifyDataSetChanged();
        }

        /**
         * Handler class method
         *
         * @param message from the handler
         */
        @Override
        public void onMessageResult(Message message) {
            if (message.what == MESSAGE_READ) {
                String readMessage = null;
                readMessage = new String((byte[]) message.obj, StandardCharsets.UTF_8);
                mReadBuffer.setText(readMessage);
            }

            if (message.what == CONNECTING_STATUS) {
                if (message.arg1 == 1)
                    //String obj = (String)(message.obj);
                    mBluetoothStatus.setText("Connected to Device: " + (String) (message.obj));
                else
                    mBluetoothStatus.setText("Connection Failed");
            }
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (!mBTAdapter.isEnabled()) {
                Toast.makeText(getBaseContext(), "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            mBluetoothStatus.setText("Connecting...");
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) view).getText().toString();
            final String address = info.substring(info.length() - 17);
            final String name = info.substring(0, info.length() - 17);

            // Spawn a new thread to avoid blocking the GUI one
            new Thread() {
                public void run() {
                    boolean fail = false;

                    BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                    try {
                        mBTSocket = createBluetoothSocket(device);
                    } catch (IOException e) {
                        fail = true;
                        Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                    }
                    // Establish the Bluetooth socket connection.
                    try {
                        mBTSocket.connect();
                    } catch (IOException e) {
                        try {
                            fail = true;
                            mBTSocket.close();
                            mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                    .sendToTarget();
                        } catch (IOException e2) {
                            //insert code to deal with this
                            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if (!fail) {
                        mConnectedThread = new ConnectedThread(mBTSocket, mHandler);
                        mConnectedThread.start();

                        mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                .sendToTarget();
                    }
                }
            }.start();
        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.HEADSET) {
                bluetoothHeadset = (BluetoothHeadset) proxy;
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothHeadset.HEADSET) {
                bluetoothHeadset = null;
            }
        }
    }
