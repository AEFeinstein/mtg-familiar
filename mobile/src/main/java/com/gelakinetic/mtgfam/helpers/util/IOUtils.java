package com.gelakinetic.mtgfam.helpers.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {
    /**
     * Represents the end-of-file (or stream).
     *
     * @since 2.5 (made public)
     */
    public static final int EOF = -1;

    /**
     * The default buffer size ({@value}) to use in copy methods.
     */
    public static final int DEFAULT_BUFFER_SIZE = 8192;


    public static int copy(final InputStream input, final OutputStream output) throws IOException {
        final long count = copyLarge(input, output);
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }

    public static long copyLarge(final InputStream input, final OutputStream output)
            throws IOException {
        return copy(input, output, DEFAULT_BUFFER_SIZE);
    }

    public static long copy(final InputStream input, final OutputStream output, final int bufferSize)
            throws IOException {
        return copyLarge(input, output, new byte[bufferSize]);
    }

    public static long copyLarge(final InputStream input, final OutputStream output, final byte[] buffer)
            throws IOException {
        long count = 0;
        if (input != null) {
            int n;
            while (EOF != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
            }
        }
        return count;
    }

    public static long copyFile(File src, File dst) throws IOException {
        try (FileInputStream fio = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dst)) {
            return copy(fio, fos);
        } catch (IOException e) {
            throw e;
        }
    }
}
