package com.gokhan.connecttoarduino;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class OLEDBroadcastReceiver extends BroadcastReceiver {
    private AppReceiver appReceiver;

    public OLEDBroadcastReceiver() { }
    public OLEDBroadcastReceiver(AppReceiver receiver) {
        appReceiver = receiver;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(BluetoothDevice.ACTION_FOUND.equals(action)){
            Log.d("DEVICE-LIST", "Bluetooth device found\n");
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            appReceiver.onReceiveResult(device,intent);
        }
    }

    public interface AppReceiver {
        void onReceiveResult(BluetoothDevice bluetoothDevice, Intent intent);
    }
}
