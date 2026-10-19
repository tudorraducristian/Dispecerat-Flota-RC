package ro.flota.proxy.connect;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Un InputStream care ține minte tot ce s-a citit prin el, ca pachetul să plece mai departe întocmai. */
final class CopieStream extends FilterInputStream {

    private final ByteArrayOutputStream copie = new ByteArrayOutputStream();

    CopieStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b >= 0) {
            copie.write(b);
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = super.read(b, off, len);
        if (n > 0) {
            copie.write(b, off, n);
        }
        return n;
    }

    byte[] octeti() {
        return copie.toByteArray();
    }
}
