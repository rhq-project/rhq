/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.communications.util;

import java.io.File;
import java.io.FileInputStream;

/**
 * Dumps hexadecimal, decimal, octal and binary representations of any given File, String or byte array. For example,
 * the different representations for the string "helloworld" will show:
 *
 * <p>Hexadecimal:</p>
 *
 * <PRE>
 * 68 65 6c 6c 6f 77 6f 72 6c 64   helloworld
 * </PRE>
 *
 * <p>Decimal:</p>
 *
 * <PRE>
 * 104 101 108 108 111 119 111 114   hellowor
 * 108 100                           ld
 * </PRE>
 *
 * <p>Octal:</p>
 *
 * <PRE>
 * 150   145   154   154   157   167   hellow
 * 157   162   154   144               orld
 * </PRE>
 *
 * <p>Binary:</p>
 *
 * <PRE>
 * 1101000  1100101  1101100  1101100  1101111   hello
 * 1110111  1101111  1110010  1101100  1100100   world
 * </PRE>
 *
 * @author John Mazzitelli
 */
public class DumpBytes {
    /**
     * Hexadecimal base (16).
     */
    public static final int BASE_HEX = 16;

    /**
     * Decimal base (10).
     */
    public static final int BASE_DEC = 10;

    /**
     * Octal base (8).
     */
    public static final int BASE_OCT = 8;

    /**
     * Binary base (2).
     */
    public static final int BASE_BIN = 2;

    /**
     * Converts the given byte array to a numerical format representation.
     *
     * @param  bytes  data to be converted to a particular numerical format
     * @param  cols   number of columns the output data will have
     * @param  format numerical base the output will be shown as - e.g. {@link #BASE_HEX} is hex
     *
     * @return formatted representation of the given byte array.
     *
     * @throws IllegalArgumentException if format is invalid
     */
    public static String dumpData(byte[] bytes, int cols, int format) {
        int num_col_width;
        int char_col_width;
        StringBuffer result = new StringBuffer(1024);
        String nums = "";
        String chars = "";
        String byte_str;

        if ((format < 2) || (format > 16)) {
            throw new IllegalArgumentException("format=" + format);
        } else if (format < 3) // leave enough room for 8 binary digits and a space
        {
            num_col_width = 9;
            char_col_width = 1;
        } else if (format < 8) {
            num_col_width = 6;
            char_col_width = 1;
        } else if (format < 10) // leave enough room for 4 octal digits and a space
        {
            num_col_width = 5;
            char_col_width = 1;
        } else if (format < 16) // leave enough room for 3 decimal digits and a space
        {
            num_col_width = 4;
            char_col_width = 1;
        } else { // leave enough room for 2 hexadecimal digits and a space
            num_col_width = 3;
            char_col_width = 1;
        }

        for (int i = 0; i < bytes.length; i++) {
            if (((i % cols) == 0) && (i != 0)) {
                result.append(nums);
                result.append("   ");
                result.append(chars);
                result.append('\n');
                nums = "";
                chars = "";
            }

            byte_str = Integer.toString(bytes[i] & 0x000000FF, format);
            nums += padFront(byte_str, num_col_width);
            chars += padFront(toCharString(bytes[i]), char_col_width);
        }

        result.append(padBack(nums, (cols * num_col_width)));
        result.append("   ");
        result.append(padBack(chars, (cols * char_col_width)));
        result.append('\n');

        return result.toString();
    }

    /**
     * Converts the given String to a numerical format representation.
     *
     * @param  data   String to be converted to a particular numerical format
     * @param  cols   number of columns the output data will have
     * @param  format numerical base the output will be shown as (e.g. 16 is hex)
     *
     * @return Formatted representation of the given String.
     */
    public static String dumpData(String data, int cols, int format) {
        return dumpData(data.getBytes(), cols, format);
    }

    /**
     * Reads the given File and converts its contents to a numerical format representation.
     *
     * @param  file   File whose contents are to be converted to a particular numerical format
     * @param  cols   number of columns the output data will have
     * @param  format numerical base the output will be shown as (e.g. 16 is hex)
     *
     * @return formatted representation of the given String or null if the file was not readable.
     */
    public static String dumpData(File file, int cols, int format) {
        FileInputStream fis;
        byte[] file_contents;
        int num_bytes;

        try {
            file_contents = new byte[(int) file.length()];
            fis = new FileInputStream(file);
            try {
                num_bytes = fis.read(file_contents);
            } finally {
                fis.close();
            }

            if (num_bytes != file_contents.length) {
                throw new IllegalStateException(num_bytes + "!=" + file_contents.length);
            }

            return dumpData(file_contents, cols, format);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Converts the contents of the given File to binary format.
     *
     * @param  f file whose contents is to be converted
     *
     * @return Formatted representation of the file contents
     */
    public static String dumpBinData(File f) {
        return dumpData(f, 7, BASE_BIN);
    }

    /**
     * Converts the given byte array to binary format.
     *
     * @param  data byte array to be converted
     *
     * @return Formatted representation of data
     */
    public static String dumpBinData(byte[] data) {
        return dumpData(data, 7, BASE_BIN);
    }

    /**
     * Converts the given String to binary format.
     *
     * @param  data String to be converted
     *
     * @return Formatted representation of data
     */
    public static String dumpBinData(String data) {
        return dumpData(data, 7, BASE_BIN);
    }

    /**
     * Converts the contents of the given File to octal format.
     *
     * @param  f file whose contents is to be converted
     *
     * @return Formatted representation of the file contents
     */
    public static String dumpOctData(File f) {
        return dumpData(f, 9, BASE_OCT);
    }

    /**
     * Converts the given byte array to octal format.
     *
     * @param  data byte array to be converted
     *
     * @return Formatted representation of data
     */
    public static String dumpOctData(byte[] data) {
        return dumpData(data, 9, BASE_OCT);
    }

    /**
     * Converts the given String to octal format.
     *
     * @param  data String to be converted
     *
     * @return Formatted representation of data
     */
    public static String dumpOctData(String data) {
        return dumpData(data, 9, BASE_OCT);
    }

    /**
     * Converts the contents of the given File to decimal format.
     *
     * @param  f file whose contents is to be converted
     *
     * @return Formatted representation of the file contents
     */
    public static String dumpDecData(File f) {
        return dumpData(f, 12, BASE_DEC);
    }

    /**
     * Converts the given byte array to decimal format.
     *
     * @param  data byte array to be converted
     *
     * @return Formatted representation of data
     */
    public static String dumpDecData(byte[] data) {
        return dumpData(data, 12, BASE_DEC);
    }

    /**
     * Converts the given String to decimal format.
     *
     * @param  data String to be converted
     *
     * @return Formatted representation of data
     */
    public static String dumpDecData(String data) {
        return dumpData(data, 12, BASE_DEC);
    }

    /**
     * Converts the contents of the given File to hexadecimal format.
     *
     * @param  f file whose contents is to be converted
     *
     * @return Formatted representation of the file contents
     */
    public static String dumpHexData(File f) {
        return dumpData(f, 15, BASE_HEX);
    }

    /**
     * Converts the given byte array to hexadecimal format.
     *
     * @param  data byte array to be converted
     *
     * @return Formatted representation of data
     */
    public static String dumpHexData(byte[] data) {
        return dumpData(data, 15, BASE_HEX);
    }

    /**
     * Converts the given String to hexadecimal format.
     *
     * @param  data String to be converted
     *
     * @return Formatted representation of data
     */
    public static String dumpHexData(String data) {
        return dumpData(data, 15, BASE_HEX);
    }

    /**
     * Appends whitespace to the end of the given string.
     *
     * @param  s   string to append with whitespace
     * @param  len number of spaces to append
     *
     * @return appended string
     */
    private static String padBack(String s, int len) {
        String spaces = "";

        for (int i = 0; i < (len - s.length()); i++) {
            spaces += " ";
        }

        return (s + spaces);
    }

    /**
     * Prepends whitespace to the given string.
     *
     * @param  s   string to prepend whitespace
     * @param  len number of spaces to prepend
     *
     * @return string with prepended whitespace
     */
    private static String padFront(String s, int len) {
        String spaces = "";

        for (int i = 0; i < (len - s.length()); i++) {
            spaces += " ";
        }

        return (spaces + s);
    }

    /**
     * Returns a string representation of the given byte. Whitespace will be represented with a " " and control
     * characters will be represented with a "." character.
     *
     * @param  b byte to convert to a string
     *
     * @return string version of the given byte
     */
    private static String toCharString(byte b) {
        if (Character.isWhitespace((char) b)) {
            return " ";
        } else if (Character.isISOControl((char) b)) {
            return ".";
        }

        return "" + (char) b;
    }
}