package ibis.ipl.impl.androidbt.util;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;

public class BlueToothEnabler extends Activity {
    
    private static final int ENABLE = 1;
    
    private final BluetoothAdapter bt;
    
    private boolean haveResult = false;
    private boolean enabled = false;
    
    public BlueToothEnabler(BluetoothAdapter bt) {
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
}
