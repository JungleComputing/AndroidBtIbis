package ibis.ipl.impl.androidbt.util;

import android.bluetooth.BluetoothAdapter;
import android.os.HandlerThread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdaptorFinder extends HandlerThread {
    
    public AdaptorFinder() {
	super("Adaptor Handler thread");
    }

    private static final Logger log = LoggerFactory.getLogger(AdaptorFinder.class);
    
    private static BluetoothAdapter bt = null;
    
    private static int lock = 0;
    
    private static boolean bt_done = false;
    
    public static BluetoothAdapter getBluetoothAdaptor() {
	synchronized(AdaptorFinder.class)  {
	    if (bt == null) {
		if (lock == 0) {
		    lock = 1;
		    startFinder();
		}
	    }
	}
	waitForFinder();
	return bt;

    }
    
    private static void startFinder() {
	AdaptorFinder f = new AdaptorFinder();
	f.start();
    }
    
    private static synchronized void waitForFinder() {
	while (! bt_done) {
	    try {
		AdaptorFinder.class.wait();
	    } catch (InterruptedException e) {
		// ignored
	    }
	}
	if (log.isDebugEnabled()) {
	    log.debug("After wait: bt = " + bt == null ? "null" : bt.getAddress());
	}
    }
    
    protected void onLooperPrepared() {
	// Funny stuff, this. Apparently, getDefaultAdapter() must be called from a
	// thread that has a looper. So, this is a handler thread.
	BluetoothAdapter x = BluetoothAdapter.getDefaultAdapter();
	if (log.isDebugEnabled()) {
	    log.debug("BT adres = " + x.getAddress());
	}
	synchronized(AdaptorFinder.class) {
	    bt = x;
	    bt_done = true;
	    AdaptorFinder.class.notifyAll();
	}
    }
}
