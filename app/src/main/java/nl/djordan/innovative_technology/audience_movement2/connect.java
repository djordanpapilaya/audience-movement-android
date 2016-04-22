package nl.djordan.innovative_technology.audience_movement2;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class connect extends AppCompatActivity {

    private Button mBleConnect;
    private Button mBleScan;
    private Button mConnectBtn;
    private Button mDisconnectBtn;

    private ArrayList<BluetoothDevice> mDeviceList = new ArrayList<BluetoothDevice>();

    private DeviceListAdapter mAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private ProgressDialog mProgressDlg;
    private ProgressDialog mProgressDlgPair;
    private ListView mListView;

    private ArrayList<BluetoothDevice> mPairedDevices;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mBleConnect = (Button) findViewById(R.id.btn_prg1);
        mBleScan = (Button) findViewById(R.id.btn_prg2);
        mConnectBtn = (Button) findViewById(R.id.btn_connect);
        mDisconnectBtn = (Button) findViewById(R.id.disconnect);
        mListView = (ListView) findViewById(R.id.lv_paired);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mAdapter = new DeviceListAdapter(this);

        mProgressDlg = new ProgressDialog(this);
        mProgressDlgPair = new ProgressDialog(this);

        mProgressDlg.setMessage("Scanning...");
        mProgressDlgPair.setMessage("Pairing...");
        mProgressDlg.setCancelable(false);
        mProgressDlgPair.setCancelable(false);
        mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                mBluetoothAdapter.cancelDiscovery();
            }
        });

        mDisconnectBtn.setEnabled(false);

        if (mBluetoothAdapter == null) {
            mBleConnect.setEnabled(false);
            mBleScan.setEnabled(false);
            Snackbar.make(findViewById(android.R.id.content), "Sorry, your device does not support bluetooth", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        } else {
            mBleScan.setOnClickListener(new View.OnClickListener() {
                public void onClick(View arg0) {
                    mBluetoothAdapter.startDiscovery();
                }
            });

            mBleConnect.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.disable();
                    } else {
                        Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

                        startActivityForResult(i, 1000);
                    }
                }
            });

            if (mBluetoothAdapter.isEnabled()) {
                mBleConnect.setText("Bluetooth is on");
                mBleConnect.setEnabled(false);
            } else {
                mBleConnect.setText("Turn on bluetooth");
            }

            CheckPairedDevices();
        }

        mConnectBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                onCreateDialog(savedInstanceState).show();
            }
        });

        mDisconnectBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                mDisconnectBtn.setEnabled(false);
                mConnectBtn.setEnabled(true);
                if (outStream != null) {
                    try {
                        outStream.flush();
                    } catch (IOException e) {
                        Log.d("BLE", "AN ERROR");
                    }
                }

                try {
                    btSocket.close();
                } catch (IOException e2) {
                    Log.d("BLE", "AN ERROR");
                }
            }
        });

        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mReceiver, filter);
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        final CharSequence[] items;

        ArrayList<String> PairedNames = new ArrayList<>();

        for (BluetoothDevice device : mPairedDevices) {
            PairedNames.add(device.getName());
        }

        items = PairedNames.toArray(new CharSequence[PairedNames.size()]);

        builder.setTitle("Connect with device")
                .setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        BluetoothDevice ClickedDevice = mPairedDevices.get(which);

                        BluetoothDevice Device = mBluetoothAdapter.getRemoteDevice(ClickedDevice.getAddress());
                        try {
                            btSocket = createBluetoothSocket(Device);
                        } catch (IOException e1) {
                            Log.d("BLE", "AN ERROR");
                        }

                        try {
                            btSocket.connect();
                            Log.d("BLE", "...Connection ok...");
                            mDisconnectBtn.setEnabled(true);
                            mConnectBtn.setEnabled(false);
                        } catch (IOException e) {
                            try {
                                btSocket.close();
                            } catch (IOException e2) {
                                Log.d("BLE", "AN ERROR");
                            }
                        }

                        try {
                            outStream = btSocket.getOutputStream();
                        } catch (IOException e) {
                            Log.d("BLE", "AN ERROR");
                        }
                    }
                });

        return builder.create();
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class});
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e("BLE", "Could not create Insecure RFComm Connection", e);
            }
        }
        return device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    @Override
    public void onPause() {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
        }

        super.onPause();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);

        super.onDestroy();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                if (state == BluetoothAdapter.STATE_ON) {
                    Snackbar.make(findViewById(android.R.id.content), "Your bluetooth is enabled", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();

                    mBleConnect.setText("Bluetooth is on");
                    mBleConnect.setEnabled(false);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                mDeviceList = new ArrayList<BluetoothDevice>();

                mProgressDlg.show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mProgressDlg.dismiss();
                mAdapter.setData(mDeviceList);

                mAdapter.setListener(new DeviceListAdapter.OnPairButtonClickListener() {
                    @Override
                    public void onPairButtonClick(int position) {
                        BluetoothDevice device = mDeviceList.get(position);

                        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                            unpairDevice(device);
                        } else {
                            Snackbar.make(findViewById(android.R.id.content), "Pairing...", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            mProgressDlgPair.show();

                            pairDevice(device);
                        }
                    }
                });

                mListView.setAdapter(mAdapter);
                registerReceiver(mPairReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));

                CheckPairedDevices();
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                mDeviceList.add(device);

                Snackbar.make(findViewById(android.R.id.content), "Found device " + device.getName(), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        }
    };

    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {
                    Snackbar.make(findViewById(android.R.id.content), "Device paired.", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    mProgressDlgPair.dismiss();
                } else if (state == BluetoothDevice.BOND_NONE && prevState == BluetoothDevice.BOND_BONDED) {
                    Snackbar.make(findViewById(android.R.id.content), "Device unpaired.", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }

                mAdapter.notifyDataSetChanged();
            }
        }
    };

    private void pairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("createBond", (Class[]) null);
            method.invoke(device, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unpairDevice(BluetoothDevice device) {
        try {
            Method method = device.getClass().getMethod("removeBond", (Class[]) null);
            method.invoke(device, (Object[]) null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void CheckPairedDevices() {
        Set<BluetoothDevice> mPairedDevicesList;

        mPairedDevicesList = mBluetoothAdapter.getBondedDevices();
        mPairedDevices = new ArrayList<>();
        if (mPairedDevicesList != null) {
            for (BluetoothDevice bt : mPairedDevicesList) {
                mPairedDevices.add(bt);
                break;
            }
        } else {
            mConnectBtn.setEnabled(false);
            Log.d("BLE", "NO BOUND DEVICES");
        }
    }
}