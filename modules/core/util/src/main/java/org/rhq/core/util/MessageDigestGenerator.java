/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * An object that generates a message digest or hash for algorithms such as MD5 or SHA. This class is basically a
 * wrapper around {@link java.security.MessageDigest} and provides convenience methods making it easier to generate
 * hashes.
 *
 * There are static methods in here that use MD5 as the default algorithm. If you want to use another
 * algorithm, instantiate an instance of this object and use its instance methods.
 */
public class MessageDigestGenerator {
    /** you can pass this to the constructor to indicate you want to generate MD5 message digests */
    public static final String MD5 = "MD5";

    /** you can pass this to the constructor to indicate you want to generate SHA 256 message digests */
    public static final String SHA_256 = "SHA-256";

    private final MessageDigest messageDigest;

    /**
     * Creates a new {@link MessageDigestGenerator} object using MD5 as the default algorithm.
     * <p/>
     * MD5 is used as the default algorithm for backward compatibility. It originally only supported MD5 and has since
     * been refactored to support algortithms that are supported by your version of Java.
     *
     * @throws IllegalStateException if the MD5 algorithm cannot be computed by the VM
     */
    public MessageDigestGenerator() {
        this(MD5);
    }

    /**
     * Creates a new MessageDigestGenerator using the specified algorithm.
     *
     * @param algorithm The algorithm to use (e.g., MD5, SHA-256)
     *
     * @throws IllegalStateException if the algorithm is not supported by the VM
     */
    public MessageDigestGenerator(String algorithm) {
        try {
            messageDigest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithm + " is not a supported algorithm");
        }
    }

    /**
     * Returns the <code>MessageDigest</code> object that is used to compute the digest.
     *
     * @return object that will perform the calculations
     */
    public MessageDigest getMessageDigest() {
        return this.messageDigest;
    }

    /**
     * Use this to add more data to the set of data used to calculate the digest. Once all data has been added, call
     * {@link #getDigest()} to get the final value.
     *
     * @param  is the stream whose data is to be part of the set of data from which the digest is to be calculated
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
     * Use this to add more data to the set of data used to calculate the hash. Once all data has been added, call
     * {@link #getDigest()} to get the final digest value.
     *
     * <p>If <code>bytes</code> is <code>null</code>, this method is a no-op and simply returns.</p>
     *
     * @param bytes data to be part of the set of data from which the digest is to be calculated
     */
    public void add(byte[] bytes) {
        if (bytes != null) {
            messageDigest.update(bytes);
        }
    }

    /**
     * Use this to add more data to the set of data used to calculate the hash. Once all data has been added, call
     * {@link #getDigest()} to get the final digest value.
     *
     * <p>If <code>bytes</code> is <code>null</code>, this method is a no-op and simply returns.</p>
     *
     * @param bytes data to be part of the set of data from which the digest is to be calculated
     * @param offset the offset to start from in the array of bytes
     * @param length the number of bytes to use, starting at offset
     */
    public void add(byte[] bytes, int offset, int length) {
        if (bytes != null) {
            messageDigest.update(bytes, offset, length);
        }
    }

    /**
     * After all the data has been added to the message digest via add methods, this method is used to
     * finalize the digest calcualation and return the digest. You can get the String form of this digest if
     * you call {@link #getDigestString()} instead.
     *
     * @return the bytes of the digest
     */
    public byte[] getDigest() {
        return this.messageDigest.digest();
    }

    /**
     * After all the data has been added to the message digest via add methods,
     * this method is used to finalize the digest calcualation and return the digest as a String. You can get the
     * actual bytes of the digest if you call {@link #getDigest()} instead.
     *
     * @return the digest as a string
     */
    public String getDigestString() {
        return calculateDigestStringFromBytes(getDigest());
    }

    /**
     * Returns the digest for the data found in the given stream. The digest is returned as a byte array; if you want
     * the digest as a String, call getDigestString methods instead.
     *
     * @param  is the stream whose data is to be used to calculate the digest
     *
     * @return the stream data's hash
     *
     * @throws IOException if failed to read the stream for some reason
     */
    public static byte[] getDigest(InputStream is) throws IOException {
        MessageDigestGenerator md5 = new MessageDigestGenerator(MD5);
        return md5.calcDigest(is);
    }

    public byte[] calcDigest(InputStream is) throws IOException {
        add(is);
        return getDigest();
    }

    /**
     * Similar to {@link #getDigest(InputStream)}, only this returns the digest as a String.
     *
     * @param  is the stream whose data is to be used to calculate the digest
     *
     * @return the stream data's digest as a String
     *
     * @throws IOException if failed to read the stream for some reason
     */
    public static String getDigestString(InputStream is) throws IOException {
        MessageDigestGenerator md5 = new MessageDigestGenerator(MD5);
        return md5.calcDigestString(is);
    }

    public String calcDigestString(InputStream is) throws IOException {
        add(is);
        return getDigestString();
    }

    /**
     * Calculates a digest for a given string.
     *
     * @param  source_str the string whose contents will be used as the data to calculate the digest
     *
     * @return the string's digest
     *
     * @throws RuntimeException if a system error occurred - should never really happen
     */
    public static byte[] getDigest(String source_str) {
        MessageDigestGenerator md5 = new MessageDigestGenerator(MD5);
        return md5.calcDigest(source_str);
    }

    public byte[] calcDigest(String source_str) {
        try {
            ByteArrayInputStream bs = new ByteArrayInputStream(source_str.getBytes());
            return calcDigest(bs);
        } catch (IOException e) {
            throw new RuntimeException("IOException reading a byte array input stream, this should never happen", e);
        }
    }

    /**
     * Calculates a digest for a given string and returns the digest's String representation.
     *
     * @param  source_str the string whose contents will be used as the data to calculate the digest
     *
     * @return the string's digest or hash as a String
     */
    public static String getDigestString(String source_str) {
        MessageDigestGenerator md5 = new MessageDigestGenerator(MD5);
        return md5.calcDigestString(source_str);
    }

    public String calcDigestString(String source_str) {
        return calculateDigestStringFromBytes(calcDigest(source_str));
    }

    /**
     * Calculates the digest for a given file. The file's contents will be used as the source data for the digest calculation.
     *
     * @param  file the file whose contents are to be used to calculate the digest.
     *
     * @return the file content's digest
     *
     * @throws IOException if the file could not be read or accessed
     */
    public static byte[] getDigest(File file) throws IOException {
        MessageDigestGenerator md5 = new MessageDigestGenerator(MD5);
        return md5.calcDigest(file);
    }

    public byte[] calcDigest(File file) throws IOException {
        FileInputStream is = null;

        try {
            is = new FileInputStream(file);
            return calcDigest(new BufferedInputStream(is, 1024 * 32));
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public byte[] calcDigest(URL url) throws IOException {
        InputStream is = null;
        try {
            is = url.openStream();
            return calcDigest(new BufferedInputStream(is, 1024 * 32));
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
     * Calculates the digest for a given file. The file's contents will be used as the source data for the digest calculation.
     *
     * @param  file the file whose contents are to be used to calculate the digest.
     *
     * @return the file content's digest as a String
     *
     * @throws IOException if the file could not be read or accessed
     */
    public static String getDigestString(File file) throws IOException {
        MessageDigestGenerator md5 = new MessageDigestGenerator(MD5);
        return md5.calcDigestString(file);
    }

    public String calcDigestString(File file) throws IOException {
        return calculateDigestStringFromBytes(calcDigest(file));
    }

    public String calcDigestString(byte[] bytes) throws IOException {
        return calculateDigestStringFromBytes(bytes);
    }

    /**
     * Calculates the digest for a given file. The file's contents will be used as the source data for the digest calculation.
     *
     * @param  url the URL whose contents are to be used to calculate the digest.
     *
     * @return the URL content's digest as a String
     *
     * @throws IOException if the URL could not be read or accessed
     */
    public static String getDigestString(URL url) throws IOException {
        MessageDigestGenerator md5 = new MessageDigestGenerator(MD5);
        return md5.calcDigestString(url);
    }

    public String calcDigestString(URL url) throws IOException {
        return calculateDigestStringFromBytes(calcDigest(url));
    }

    /**
     * Given a digest byte array, this will return its String representation.
     *
     * @param  bytes the digest whose String representation is to be returned
     *
     * @return the digest string
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
     * This can be used to generate the digest hash from the command line.
     *
     * @param  args one and only one filename - may or may not be a .jar file.
     *
     * @throws Exception if failed to compute the digest for some reason
     */
    public static void main(String[] args) throws Exception {
        String file = args[0];
        String digest = MessageDigestGenerator.getDigestString(new File(file));
        System.out.println("MD5=" + digest);
        digest = new MessageDigestGenerator(SHA_256).calcDigestString(new File(file));
        System.out.println("SHA-256=" + digest);
    }
}