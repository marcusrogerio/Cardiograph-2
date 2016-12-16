package com.bukhmastov.cardiograph;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";
    public final static String APP_PREFERENCES = "appsettings";
    private BluetoothAdapter btAdapter;
    private ArrayList<BtDevice> btDevices = new ArrayList<>();
    private DeviceListView foundedDevicesAdapter;
    private Menu menu;
    private RelativeLayout btOffLayout;
    private LinearLayout btSelectConnectionLayout;
    private ListView devices_view;
    private Button bt_enable_btn, quick_connect_btn;
    private EditText quick_connect_input;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.lobby_toolbar));
        getSupportActionBar().setLogo(R.mipmap.ic_launcher_simple);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter == null){
            Toast.makeText(getBaseContext(), R.string.bt_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        if(btAdapter.isDiscovering()) btAdapter.cancelDiscovery();
        btOffLayout = (RelativeLayout) findViewById(R.id.btOffLayout);
        btSelectConnectionLayout = (LinearLayout) findViewById(R.id.btSelectConnectionLayout);
        devices_view = (ListView) findViewById(R.id.devices_view);
        bt_enable_btn = (Button) findViewById(R.id.bt_enable_btn);
        quick_connect_btn = (Button) findViewById(R.id.quick_connect_btn);
        quick_connect_input = (EditText) findViewById(R.id.quick_connect_input);
        bt_enable_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0); }
        });
        foundedDevicesAdapter = new DeviceListView(this, btDevices);
        devices_view.setAdapter(foundedDevicesAdapter);
        devices_view.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) { connect(btDevices.get(position).mac); }
        });
        quick_connect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { connect(quick_connect_input.getText().toString()); }
        });
        if((this.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE)).getBoolean("storage_disclaimer", true)) {
            Intent intent = new Intent(this, DisclaimerActivity.class);
            startActivity(intent);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(bluetoothState, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        updateUI();
    }
    @Override
    protected void onPause() {
        super.onPause();
        if(btAdapter.isDiscovering()) btAdapter.cancelDiscovery();
        try{ unregisterReceiver(bluetoothState); } catch (Exception e){}
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.lobby_toolbar, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_about:
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_toggle_search:
                if (btAdapter.isEnabled()) {
                    if (btAdapter.isDiscovering()) {
                        btAdapter.cancelDiscovery();
                    } else {
                        findDevices();
                    }
                } else {
                    startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);
                }
                return true;
            default: return super.onOptionsItemSelected(item);
        }
    }
    private void updateUI(){
        if(btAdapter.isEnabled()){
            btOffLayout.setVisibility(View.GONE);
            btSelectConnectionLayout.setVisibility(View.VISIBLE);
            resetDeviceList();
            displayPairedDevices();
        } else {
            btOffLayout.setVisibility(View.VISIBLE);
            btSelectConnectionLayout.setVisibility(View.GONE);
        }
    }
    private void findDevices(){
        if (btAdapter == null) return;
        if (!btAdapter.isEnabled()) return;
        if (btAdapter.isDiscovering()) return;
        resetDeviceList();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION") + this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if(permissionCheck != 0) this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
        }
        try { unregisterReceiver(mReceiver); } catch (Exception e){}
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
        btAdapter.startDiscovery();
    }
    private void resetDeviceList(){
        btDevices.clear();
        foundedDevicesAdapter.notifyDataSetChanged();
    }
    private void displayPairedDevices(){
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if(pairedDevices.size() > 0) {
            for (BluetoothDevice pairedDevice : pairedDevices) addToDeviceList(pairedDevice.getName(), pairedDevice.getAddress(), true);
        }
    }
    private void addToDeviceList(String name, String address, Boolean isPaired){
        addToDeviceList(new BtDevice(name, address, isPaired));
    }
    private void addToDeviceList(BtDevice btDevice){
        btDevices.add(btDevice);
        foundedDevicesAdapter.notifyDataSetChanged();
    }
    private void connect(String mac){
        if(mac.isEmpty()) return;
        if(btAdapter.isDiscovering()) btAdapter.cancelDiscovery();
        Intent intent = new Intent(this, ConnectionActivity.class);
        intent.putExtra("mac", mac);
        startActivity(intent);
    }
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                menu.findItem(R.id.action_toggle_search).setIcon(R.drawable.ic_bluetooth_connected);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                menu.findItem(R.id.action_toggle_search).setIcon(R.drawable.ic_bluetooth_search);
                try { unregisterReceiver(mReceiver); } catch (Exception e){}
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BtDevice btDevice = new BtDevice(device.getName() == null ? "" : device.getName(), device.getAddress(), true);
                if(!btDevices.contains(btDevice)){
                    btDevice.isPaired = false;
                    addToDeviceList(btDevice);
                }
            }
        }
    };
    private BroadcastReceiver bluetoothState = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                case (BluetoothAdapter.STATE_ON) :
                case (BluetoothAdapter.STATE_OFF) : {
                    updateUI();
                    break;
                }
            }
        }
    };
}