package ibis.ipl.impl.androidbt.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * OutputStream that limits the maximum size of writes to a specified limit.
 * Apparently, writes with a large len don't work properly on bluetooth ...
 */
public class LimitedOutputStream extends OutputStream {
    
    private final OutputStream out;
    private final int limit;

    public LimitedOutputStream(OutputStream out, int limit) {
	this.out = out;
	this.limit = limit;
    }

    public void close() throws IOException {
	out.close();
    }

    public void flush() throws IOException {
	out.flush();
    }

    public void write(byte[] buffer, int offset, int count) throws IOException {
	while (count > limit) {
	    out.write(buffer, offset, limit);
	    count -= limit;
	    offset += limit;
	}
	out.write(buffer, offset, count);
    }

    public void write(byte[] buffer) throws IOException {
	out.write(buffer);
    }

    public void write(int oneByte) throws IOException {
	out.write(oneByte);
    }
}
