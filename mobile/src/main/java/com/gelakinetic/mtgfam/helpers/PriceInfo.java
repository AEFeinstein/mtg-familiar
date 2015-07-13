package com.gelakinetic.mtgfam.helpers;

import java.nio.ByteBuffer;

public class PriceInfo {
    public double mLow = 0;
    public double mAverage = 0;
    public double mHigh = 0;
    public double mFoilAverage = 0;
    public String mUrl;

    public PriceInfo() {

    }

    public PriceInfo(byte[] bytes) {
        this.fromBytes(bytes);
    }

    /**
     * Pack all the fields into a byte buffer and return it.
     *
     * @return The byte representation of a PriceInfo
     */
    public byte[] toBytes() {

		/* Longs to bytes */
        byte[] bytes = new byte[8 * 4 + mUrl.length()];
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        buf.putDouble(mLow);
        buf.putDouble(mAverage);
        buf.putDouble(mHigh);
        buf.putDouble(mFoilAverage);
        buf.put(mUrl.getBytes());

        return bytes;
    }

    /**
     * Fill in the fields for this object from the byte buffer. The form should be 8 bytes for the double representation
     * of the low, average, high, and foil prices, and then the bytes for the URL
     *
     * @param bytes The byte representation of a PriceInfo
     */
    private void fromBytes(byte[] bytes) {

		/* Bytes to longs */
        ByteBuffer buf2 = ByteBuffer.wrap(bytes);
        mLow = buf2.getDouble();
        mAverage = buf2.getDouble();
        mHigh = buf2.getDouble();
        mFoilAverage = buf2.getDouble();

        byte stringBuf[] = new byte[bytes.length - 8 * 4];
        buf2.get(stringBuf);
        mUrl = new String(stringBuf);
    }

}