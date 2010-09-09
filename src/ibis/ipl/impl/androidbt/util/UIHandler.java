package ibis.ipl.impl.androidbt.util;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UIHandler extends Activity {
    
    private static final int ENABLE = 1;
    
    private boolean haveResult = false;
    private boolean enabled = false;
    private boolean discoveryDone = false;
    
    private final BluetoothAdapter bt;
    
    public UIHandler(BluetoothAdapter bt) {
        this.bt = bt;
    }
    
    public void enableBT() {
        if (bt == null) {
            synchronized(this) {
                haveResult = true;
                enabled = false;
                notifyAll();
                return;
            }
        }
        if (bt.isEnabled()) {
            synchronized(this) {
                haveResult = true;
                enabled = true;
                notifyAll();
                return;
            }
        }
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, ENABLE);
    }
    
    public synchronized boolean waitForBT() {
        while (! haveResult) {
            try {
                wait();
            } catch(Throwable e) {
                // ignore
            }
        }
        return enabled;
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case ENABLE:
            // When the request to enable Bluetooth returns
            synchronized(this) {
                haveResult = true;
                enabled = resultCode == Activity.RESULT_OK;
                notifyAll();
            }
            break;
        }
    }
    
    /**
     * Start device discover with the BluetoothAdapter.
     */
    public void doDiscovery() {
        // If we're already discovering, stop it
        if (bt.isDiscovering()) {
            bt.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        bt.startDiscovery();
    }
    
    // The BroadcastReceiver that listens for discovered devices.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if ("BT-registry".equals(device.getName())) {
                    // Found registry.
                    bt.cancelDiscovery();
                    discoveryDone = true;
                    
                }
            // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                discoveryDone = true;
            }
        }
    };


}
