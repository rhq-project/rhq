/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.core.util;

/**
 * Utilities for working with bytes and byte arrays.
 *
 * @author Lukas Krejci
 */
public class ByteUtil {

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private ByteUtil() {

    }

    /**
     * Converts the byte array into a string of hexadecimal numbers.
     * Each byte in the array is represented by exactly two characters
     * in the resulting string, which form a hexadecimal number with the
     * value that represents the bits in the byte.
     * 
     * @param bytes
     * @return a string of hexadecimal numbers representing the bytes
     */
    public static String toHexString(byte[] bytes) {
        char[] str = new char[bytes.length * 2];

        for (int i = 0; i < bytes.length; ++i) {
            byte b = bytes[i];
            
            int lower = b & 0x0f;
            int upper = (b & 0xf0) >>> 4;

            str[2 * i] = HEX_DIGITS[upper];
            str[2 * i + 1] = HEX_DIGITS[lower];
        }

        return new String(str);
    }
}
