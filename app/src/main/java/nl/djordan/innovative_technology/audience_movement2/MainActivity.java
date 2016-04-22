package nl.djordan.innovative_technology.audience_movement2;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.support.design.widget.TabLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.HeaderViewListAdapter;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.Toast;
import android.view.Menu;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import me.priyesh.chroma.ChromaDialog;
import me.priyesh.chroma.ColorMode;
import me.priyesh.chroma.ColorSelectListener;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter mBluetoothAdapter;

    private ArrayList<BluetoothDevice> mPairedDevices;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private int progressValue;
    private SeekBar mSeekBar;
    private TextView mTextBrightness;
    private Spinner mSpinner;
    private Button mSendButton;
    private Button mColorPickerButton;
    private SurfaceView mColorSurface;
    private ArrayList<String> spinnerArray;
    private String mSpinnerSelected;
    private int mIntColor;
    private String mHexColor;

    private boolean Connected;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        initializeVariables();

        if (mBluetoothAdapter == null) {
            Intent i = new Intent(this, noSupport.class);
            startActivity(i);
        }

        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);

        checkConnection(savedInstanceState);

        this.registerReceiver(mReceiver, filter1);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.connect_page);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(view.getContext(), connect.class);
                startActivity(i);
            }
        });

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Connected) {
                    getDataAndSend();
                } else {
                    Snackbar snackbar = Snackbar
                            .make(findViewById(android.R.id.content), "Please first connect!", Snackbar.LENGTH_LONG)
                            .setAction("CONNECT", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    onCreateDialog(savedInstanceState).show();
                                }
                            });

                    snackbar.setActionTextColor(Color.RED);
                    snackbar.show();
                }
            }
        });

        mColorPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPickerDialog();
            }
        });

        seekbarFunctionality();
        spinnerFunctionality();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    private void seekbarFunctionality() {
        mTextBrightness.setText((String.valueOf(mSeekBar.getProgress())) + " %");

        mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressValue = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mTextBrightness.setText((String.valueOf(progressValue)) + " %");
            }
        });
    }

    private void spinnerFunctionality() {
        for (int i = 0; i <= 5; i++) {
            spinnerArray.add("program" + (i + 1));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, spinnerArray);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                mSpinnerSelected = mSpinner.getSelectedItem().toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {

            }

        });
    }

    private void initializeVariables() {
        mColorPickerButton = (Button) findViewById(R.id.btn_select_color);
        mSendButton = (Button) findViewById(R.id.btn_send);
        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mTextBrightness = (TextView) findViewById(R.id.text_brightness);
        spinnerArray = new ArrayList<>();
        mSpinner = (Spinner) findViewById(R.id.programSpinner);
        mIntColor = Color.BLACK;
        mColorSurface = (SurfaceView) findViewById(R.id.color_surface);
        mColorSurface.setBackgroundColor(mIntColor);
        Connected = false;
    }

    private void showColorPickerDialog() {
        new ChromaDialog.Builder()
                .initialColor(mIntColor)
                .colorMode(ColorMode.RGB)
                .onColorSelected(new ColorSelectListener() {
                    @Override
                    public void onColorSelected(int color) {
                        mHexColor = String.format("#%06X", (0xFFFFFF & color));
                        mIntColor = color;
                        Log.d("BLE", "color: " + mHexColor);
                        mColorSurface.setBackgroundColor(color);
                    }
                })
                .create()
                .show(getSupportFragmentManager(), "dialog");
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

    private void checkConnection(Bundle savedInstanceState) {
        if (!Connected) {
            CheckPairedDevices();
            onCreateDialog(savedInstanceState).show();
        }
    }

    private void getDataAndSend() {
        String brightness = String.valueOf(progressValue);
        String sendString = brightness + "," + mSpinnerSelected + "," + (String.valueOf(mHexColor));
//        String sendString = mHexColor;

        Log.d("BLE",sendString);

        sendData(sendString);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Connected = true;
            } else {
                Connected = false;
            }
        }
    };

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
            Log.d("BLE", "NO BOUND DEVICES");
        }
    }

    private void sendData(String message) {
        byte[] msgBuffer = message.getBytes();

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            Log.d("BLE", "AN BLE ERROR ACOURD");
        }
    }
}
