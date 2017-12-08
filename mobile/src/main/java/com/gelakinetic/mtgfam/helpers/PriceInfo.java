/*
 * Copyright 2017 Adam Feinstein
 *
 * This file is part of MTG Familiar.
 *
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

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