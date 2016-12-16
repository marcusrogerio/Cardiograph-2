package com.bukhmastov.cardiograph;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Objects;

public class ConnectionActivity extends AppCompatActivity {
    private final static String TAG = "ConnectionActivity";
    private BluetoothAdapter btAdapter;
    private String remoteDeviceMacAddress;
    private String remoteDeviceName;
    public ConnectionThread connectionThread;

    public LinearLayout container;
    public static ArrayList<GraphView> graphViews = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        setSupportActionBar((Toolbar) findViewById(R.id.connection_toolbar));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.app_name);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null || !btAdapter.isEnabled()) finish();
        remoteDeviceMacAddress = getIntent().getStringExtra("mac");
        if (remoteDeviceMacAddress.isEmpty() || !isMacAddr(remoteDeviceMacAddress)) finish();
        container = (LinearLayout) findViewById(R.id.container);
    }
    @Override
    protected void onResume() {
        super.onResume();
        container.removeAllViews();
        ((ViewGroup) findViewById(R.id.container)).addView((((LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.connection_state, null)), 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        BluetoothDevice btDevice = btAdapter.getRemoteDevice(remoteDeviceMacAddress);
        remoteDeviceName = btDevice.getName();
        connectionThread = new ConnectionThread(btDevice, handler);
        connectionThread.start();
    }
    @Override
    protected void onPause() {
        super.onPause();
        connectionThread.cancel();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    private boolean isMacAddr(String mac){
        char[] charArr = mac.toCharArray();
        String[] parts = mac.split(":");
        if (parts.length != 6) return false;
        for (String part : parts) if(part.length() != 2) return false;
        for (char c : charArr) if ("0123456789ABCDEFabcdef:".indexOf(c) == -1) return false;
        return true;
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if (msg.getData().getInt("what") == ConnectionThread.MESSAGE_CONNECTION) {
                String state = msg.getData().getString("state");
                assert state != null;
                //Log.d(TAG, "MESSAGE_CONNECTION | " + state);
                container.removeAllViews();
                if(Objects.equals(state, "connected_and_ready")){
                    LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View v = vi.inflate(R.layout.connected_interface, null);
                    ((ViewGroup) findViewById(R.id.container)).addView(v, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                } else {
                    LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View v = vi.inflate(R.layout.connection_state, null);
                    TextView textView = (TextView) v.findViewById(R.id.connection_state);
                    switch (state) {
                        case "starting":
                            state = "Подключение к устройству\n" + remoteDeviceName + " (" + remoteDeviceMacAddress + ")";
                            break;
                        case "connected":
                            state = "Соединен с устройством\n" + remoteDeviceName + " (" + remoteDeviceMacAddress + ")";
                            break;
                        case "connected_handshaking":
                            state = "Синхронизация с устройством";
                            break;
                        case "connected_and_ready":
                            state = "Соединение установлено";
                            break;
                        case "failed":
                            LinearLayout connection_image = (LinearLayout) v.findViewById(R.id.connection_image);
                            connection_image.removeAllViews();
                            ImageView gg = new ImageView(getBaseContext());
                            gg.setImageDrawable(getDrawable(R.drawable.ic_warning));
                            connection_image.addView(gg);
                            state = "Не удалось подключиться к устройству\n" + remoteDeviceName + " (" + remoteDeviceMacAddress + ")";
                            break;
                    }
                    textView.setText(state);
                    ((ViewGroup) findViewById(R.id.container)).addView(v, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                }
            }
            else if (msg.getData().getInt("what") == ConnectionThread.MESSAGE_DISCONNECTION){
                String state = msg.getData().getString("state");
                assert state != null;
                //Log.d(TAG, "MESSAGE_DISCONNECTION | " + state);
                container.removeAllViews();
                LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View v = vi.inflate(R.layout.connection_state, null);
                TextView textView = (TextView) v.findViewById(R.id.connection_state);
                LinearLayout connection_image = (LinearLayout) v.findViewById(R.id.connection_image);
                connection_image.removeAllViews();
                ImageView gg = new ImageView(getBaseContext());
                gg.setImageDrawable(getDrawable(R.drawable.ic_disconnected));
                connection_image.addView(gg);
                switch(state){
                    case "disconnected":
                        state = "Соединение разорвано";
                        break;
                    case "failed":
                        state = "Произошла ошибка во время разрыва соединения";
                        break;
                }
                textView.setText(state);
                ((ViewGroup) findViewById(R.id.container)).addView(v, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
            else if(msg.getData().getInt("what") == ConnectionThread.MESSAGE){
                Byte type = msg.getData().getByte("type");
                int value = msg.getData().getInt("value");
                switch (type){
                    case 0x00:  // PULSE

                        break;
                    case 0x01: ConnectionActivity.graphViews.get(R.id.graphView1); break; // 1 graph
                    case 0x02:  break; // 2 graph
                    case 0x03:  break; // 3 graph
                    case 0x04:  break; // 4 graph
                    case 0x05:  break; // 5 graph
                }
            }
        }
    };
}