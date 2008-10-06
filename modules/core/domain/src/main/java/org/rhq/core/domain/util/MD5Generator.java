 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.domain.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * An object that can be used to generate an MD5 hashcode (called a "digest") for files, streams or strings. 128 bit
 * fingerprints are computed as described in <i>R. Rivest, The MD5 Message-Digest Algorithm, RFC1321</i>.
 */
public class MD5Generator {
    private final MessageDigest messageDigest;

    /**
     * Creates a new {@link MD5Generator} object.
     *
     * @throws IllegalStateException if the MD5 algorithm cannot be computed by the VM
     */
    public MD5Generator() {
        try {
            this.messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not find MD5 Algorithm");
        }
    }

    /**
     * Returns the <code>MessageDigest</code> object that is used to compute the MD5.
     *
     * @return object that will perform the calculations
     */
    public MessageDigest getMessageDigest() {
        return this.messageDigest;
    }

    /**
     * Use this to add more data to the set of data used to calculate the MD5. Once all data has been added, call
     * {@link #getDigest()} to get the final MD5 value.
     *
     * @param  is the stream whose data is to be part of the set of data from which the MD5 is to be calculated
     *
     * @throws IOException if there was a problem reading from the stream
     */
    public void add(InputStream is) throws IOException {
        byte[] bytes = new byte[1024];
        int len;

        while ((len = is.read(bytes, 0, bytes.length)) != -1) {
            messageDigest.update(bytes, 0, len);
        }

        return;
    }

    /**
     * Use this to add more data to the set of data used to calculate the MD5. Once all data has been added, call
     * {@link #getDigest()} to get the final MD5 value.
     *
     * <p>If <code>bytes</code> is <code>null</code>, this method is a no-op and simply returns.</p>
     *
     * @param bytes data to be part of the set of data from which the MD5 is to be calculated
     */
    public void add(byte[] bytes) {
        if (bytes != null) {
            messageDigest.update(bytes);
        }
    }

    /**
     * After all the data has been added to the message digest via {@link #add(InputStream)}, this method is used to
     * finalize the MD5 calcualation and return the MD5 hashcode. You can get the String form of this MD5 hashcode if
     * you call {@link #getDigestString()} instead.
     *
     * @return the bytes of the MD5 hashcode
     */
    public byte[] getDigest() {
        return this.messageDigest.digest();
    }

    /**
     * After all the data has been added to the message digest via {@link #add(InputStream)} or {@link #add(byte[])}
     * this method is used to finalize the MD5 calcualation and return the MD5 hashcode as a String. You can get the
     * actual bytes of the MD5 hashcode if you call {@link #getDigest()} instead.
     *
     * @return the MD5 hashcode as a string
     */
    public String getDigestString() {
        return calculateDigestStringFromBytes(getDigest());
    }

    /**
     * Returns the MD5 hashcode for the data found in the given stream. The MD5 is returned as a byte array; if you want
     * the MD5 as a String, call {@link #getDigestString(InputStream)} instead.
     *
     * @param  is the stream whose data is to be used to calculate the MD5
     *
     * @return the stream data's MD5
     *
     * @throws IOException if failed to read the stream for some reason
     */
    public static byte[] getDigest(InputStream is) throws IOException {
        MD5Generator md5 = new MD5Generator();
        md5.add(is);
        return md5.getDigest();
    }

    /**
     * Similar to {@link #getDigest(InputStream)}, only this returns the MD5 as a String.
     *
     * @param  is the stream whose data is to be used to calculate the MD5
     *
     * @return the stream data's MD5 as a String
     *
     * @throws IOException if failed to read the stream for some reason
     */
    public static String getDigestString(InputStream is) throws IOException {
        MD5Generator md5 = new MD5Generator();
        md5.add(is);
        return md5.getDigestString();
    }

    /**
     * Calculates an MD5 for a given string.
     *
     * @param  source_str the string whose contents will be used as the data to calculate the MD5 hashcode
     *
     * @return the string's MD5
     *
     * @throws RuntimeException if a system error occurred - should never really happen
     */
    public static byte[] getDigest(String source_str) {
        try {
            ByteArrayInputStream bs = new ByteArrayInputStream(source_str.getBytes());
            return getDigest(bs);
        } catch (IOException e) {
            throw new RuntimeException("IOException reading a byte array input stream, this should never happen", e);
        }
    }

    /**
     * Calculates an MD5 for a given string and returns the MD5's String representation.
     *
     * @param  source_str the string whose contents will be used as the data to calculate the MD5 hashcode
     *
     * @return the string's MD5 as a String
     */
    public static String getDigestString(String source_str) {
        return calculateDigestStringFromBytes(getDigest(source_str));
    }

    /**
     * Calculates the MD5 for a given file. The file's contents will be used as the source data for the MD5 calculation.
     *
     * @param  file the file whose contents are to be used to calculate the MD5.
     *
     * @return the file content's MD5
     *
     * @throws IOException if the file could not be read or accessed
     */
    public static byte[] getDigest(File file) throws IOException {
        FileInputStream is = null;

        try {
            is = new FileInputStream(file);
            return getDigest(new BufferedInputStream(is, 1024 * 32));
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
     * Calculates the MD5 for a given file. The file's contents will be used as the source data for the MD5 calculation.
     *
     * @param  file the file whose contents are to be used to calculate the MD5.
     *
     * @return the file content's MD5 as a String
     *
     * @throws IOException if the file could not be read or accessed
     */
    public static String getDigestString(File file) throws IOException {
        return calculateDigestStringFromBytes(getDigest(file));
    }

    /**
     * Given an MD5 hashcode byte array, this will return its String representation.
     *
     * @param  bytes the MD5 digest whose String representation is to be returned
     *
     * @return the MD5 string
     */
    private static String calculateDigestStringFromBytes(byte[] bytes) {
        StringBuffer sb = new StringBuffer(bytes.length * 2);

        for (int i = 0; i < bytes.length; i++) {
            int hi = (bytes[i] >> 4) & 0xf;
            int lo = bytes[i] & 0xf;
            sb.append(Character.forDigit(hi, 16));
            sb.append(Character.forDigit(lo, 16));
        }

        return sb.toString();
    }

    /**
     * This can be used to generate the MD5 hashcode from the command line.
     *
     * @param  args one and only one filename - may or may not be a .jar file.
     *
     * @throws Exception if failed to compute the MD5 for some reason
     */
    public static void main(String[] args) throws Exception {
        String file = args[0];
        String digest = MD5Generator.getDigestString(new File(file));
        System.out.println("MD5=" + digest);
    }
}