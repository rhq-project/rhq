/*
  * RHQ Management Platform
  * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.core.util.stream;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Provides some utilities to work on streams and some (de)serialization methods..
 *
 * @author John Mazzitelli
 */
public class StreamUtil {

    /**
     * Logger
     */
    private static final Log LOG = LogFactory.getLog(StreamUtil.class);

    /**
     * Private to prevent instantiation.
     */
    private StreamUtil() {
    }

    /**
     * Replace characters having special meaning <em>inside</em> HTML tags with
     * their escaped equivalents, using character entities such as
     * <tt>'&amp;'</tt>.
     * <P>
     * The escaped characters are :
     * <ul>
     * <li><
     * <li>>
     * <li>"
     * <li>'
     * <li>\
     * <li>&
     * </ul>
     * <P>
     * This method ensures that arbitrary text appearing inside a tag does not
     * "confuse" the tag. For example, <tt>HREF='Blah.do?Page=1&Sort=ASC'</tt>
     * does not comply with strict HTML because of the ampersand, and should be
     * changed to <tt>HREF='Blah.do?Page=1&amp;Sort=ASC'</tt>. This is
     * commonly seen in building query strings. (In JSTL, the c:url tag performs
     * this task automatically.)
     * 
     * forHTMLTag is copy-n-pasted from: http://www.javapractices.com/Topic96.cjp 
     * used to be in our util.StringUtil, we should really use jakarta's <code>StringEscapeUtils.escapeHTML()</code> 
     * method, however, at this time we do not want to pull in the entire 
     * Commons Lang API dependency for just one method.
     * 
     * @param aTagFragment
     *           some HTML to be escaped
     * @return escaped HTML
     */
    private static String forHTMLTag(String aTagFragment) {
        final StringBuffer result = new StringBuffer();

        final StringCharacterIterator iterator = new StringCharacterIterator(aTagFragment);

        for (char character = iterator.current(); character != CharacterIterator.DONE; character = iterator.next()) {
            switch (character) {
            case '<':
                result.append("&lt;");
                break;
            case '>':
                result.append("&gt;");
                break;
            case '\"':
                result.append("&quot;");
                break;
            case '\'':
                result.append("&#039;");
                break;
            case '\\':
                result.append("&#092;");
                break;
            case '&':
                result.append("&amp;");
                break;
            case '|':
                result.append("&#124;");
                break;
            case ',':
                result.append("&#44;");
                break;
            default:
                // the char is not a special one add it to the result as is
                result.append(character);
                break;
            }
        }
        return result.toString();
    }

    /**
     * Reads in the entire contents of the given input stream and returns the data in a byte array. Be careful - if the
     * stream has a lot of data, you run the risk of an <code>OutOfMemoryError</code>.
     *
     * @param  stream the stream to read
     *
     * @return the stream's data
     *
     * @throws RuntimeException if an IO exception occurred while reading the stream
     */
    public static byte[] slurp(InputStream stream) throws RuntimeException {
        if (stream == null) {
            throw new IllegalArgumentException("Input stream is null.");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(stream, out, true);
        return out.toByteArray();
    }

    /**
     * Equivalent of {@link #slurp(InputStream)} but using a reader instead of input stream.
     * 
     * @param reader
     * @return
     * @throws RuntimeException
     */
    public static String slurp(Reader reader) throws RuntimeException {
        if (reader == null) {
            throw new IllegalArgumentException("Reader is null.");
        }

        StringWriter wrt = new StringWriter();
        copy(reader, wrt);
        return wrt.toString();
    }

    /**
     * Copies data from the input stream to the output stream. Upon completion or on an exception, the streams will be
     * closed.
     *
     * @param  input  the originating stream that contains the data to be copied
     * @param  output the destination stream where the data should be copied to
     *
     * @return the number of bytes copied from the input to the output stream
     *
     * @throws RuntimeException if failed to read or write the data
     */
    public static long copy(InputStream input, OutputStream output) throws RuntimeException {
        return copy(input, output, true);
    }

    /**
     * Equivalent of {@link #copy(InputStream, OutputStream)} but using reader and writer instead
     * of streams.
     * 
     * @param rdr
     * @param wrt
     * @return
     * @throws RuntimeException
     */
    public static long copy(Reader rdr, Writer wrt) throws RuntimeException {
        return copy(rdr, wrt, true);
    }

    /**
     * Copies data from the input stream to the output stream. Upon completion or on an exception, the streams will be
     * closed but only if <code>closeStreams</code> is <code>true</code>. If <code>closeStreams</code> is <code>
     * false</code>, the streams are left open; the caller has the responsibility to close them.
     *
     * @param  input        the originating stream that contains the data to be copied
     * @param  output       the destination stream where the data should be copied to
     * @param  closeStreams if <code>true</code>, the streams will be closed before the method returns
     *
     * @return the number of bytes copied from the input to the output stream
     *
     * @throws RuntimeException if failed to read or write the data
     */
    public static long copy(InputStream input, OutputStream output, boolean closeStreams) throws RuntimeException {
        return copy(input, output, closeStreams, false);
    }

    /**
     * Copies data from the input stream to the output stream. Upon completion or on an exception, the streams will be
     * closed but only if <code>closeStreams</code> is <code>true</code>. If <code>closeStreams</code> is <code>
     * false</code>, the streams are left open; the caller has the reponsibility to close them.
     * <p>
     * If htmlEscape is <code>true</code> the input stream is read into a <code>String</code> and all HTML entities 
     * are escaped using {@link #forHTMLTag(String)} prior to being copied to output stream.
     *
     * @param  input        the originating stream that contains the data to be copied
     * @param  output       the destination stream where the data should be copied to
     * @param  closeStreams if <code>true</code>, the streams will be closed before the method returns
     * @param  htmlEscape   if <code>true</code>, the input stream will be HTML escaped before being written to output stream
     *
     * @return the number of bytes copied from the input to the output stream or the number of characters stored in output stream if htmlEscape is <code>true</code>
     *
     * @throws RuntimeException if failed to read or write the data
     * 
     * @since 4.4
     */
    public static long copy(InputStream input, OutputStream output, boolean closeStreams, boolean htmlEscape)
        throws RuntimeException {
        if (input == null) {
            throw new IllegalArgumentException("Input stream is null.");
        }
        if (output == null) {
            throw new IllegalArgumentException("Output stream is null.");
        }

        BufferedInputStream bufInput = null;
        long numBytesCopied = 0;
        int bufferSize = 32768;

        try {
            // make sure we buffer the input
            bufInput = new BufferedInputStream(input, bufferSize);

            byte[] buffer = new byte[bufferSize];

            for (int bytesRead = bufInput.read(buffer); bytesRead != -1; bytesRead = bufInput.read(buffer)) {
                if (htmlEscape) {
                    String htmlEncodedStr = forHTMLTag(new String(buffer, 0, bytesRead));
                    bytesRead = htmlEncodedStr.length();
                    output.write(htmlEncodedStr.getBytes(), 0, bytesRead);
                } else {
                    output.write(buffer, 0, bytesRead);
                }
                numBytesCopied += bytesRead;
            }

            output.flush();
        } catch (IOException ioe) {
            throw new RuntimeException("Stream data cannot be copied", ioe);
        } finally {
            if (closeStreams) {
                try {
                    if (null != bufInput) {
                        bufInput.close();
                    }
                } catch (IOException ioe2) {
                    LOG.warn("Buffered Input Stream could not be closed", ioe2);
                }

                try {
                    input.close();
                } catch (IOException ioe2) {
                    LOG.warn("Input Stream could not be closed", ioe2);
                }

                try {
                    output.close();
                } catch (IOException ioe2) {
                    LOG.warn("Output Stream could not be closed", ioe2);
                }
            }
        }

        return numBytesCopied;
    }

    /**
     * Equivalent of {@link #copy(InputStream, OutputStream, boolean)} only using reader and writer
     * instead of input stream and output stream.
     * 
     * @param rdr
     * @param wrt
     * @param closeStreams
     * @return
     * @throws RuntimeException
     */
    public static long copy(Reader rdr, Writer wrt, boolean closeStreams) throws RuntimeException {
        if (rdr == null) {
            throw new IllegalArgumentException("Reader is null.");
        }
        if (wrt == null) {
            throw new IllegalArgumentException("Writer is null.");
        }

        try {
            long numCharsCopied = 0;
            char[] buffer = new char[32768];

            int cnt;
            while ((cnt = rdr.read(buffer)) != -1) {
                numCharsCopied += cnt;
                wrt.write(buffer, 0, cnt);
            }

            return numCharsCopied;
        } catch (IOException e) {
            throw new RuntimeException("Reader could not have been copied to the writer.", e);
        } finally {
            if (closeStreams) {
                try {
                    rdr.close();
                } catch (IOException ioe) {
                    LOG.warn("Reader could not be closed.", ioe);
                }

                try {
                    wrt.close();
                } catch (IOException ioe) {
                    LOG.warn("Writer could not be closed.", ioe);
                }
            }
        }
    }

    /**
     * Copies data from the input stream to the output stream. The caller has the responsibility to close them. This
     * method allows you to copy a byte range from the input stream. The start byte is the index (where the first byte
     * of the stream is index #0) that starts to be copied. <code>length</code> indicates how many bytes to copy, a
     * negative length indicates copy everything up to the EOF of the input stream.
     *
     * <p>Because this method must leave the given input stream intact in case the caller wants to continue reading from
     * the input stream (that is, in case the caller wants to read the next byte after the final byte read by this
     * method), this method will not wrap the input stream with a buffered input stream. Because of this, this method is
     * less efficient than {@link #copy(InputStream, OutputStream, boolean)}. If you do not care to continue reading
     * from the input stream after this method completes, it is recommended you wrap your input stream in a
     * {@link BufferedInputStream} and pass that buffered stream to this method.</p>
     *
     * @param  input     the originating stream that contains the data to be copied
     * @param  output    the destination stream where the data should be copied to
     * @param  startByte the first byte to copy from the input stream (byte indexes start at #0)
     * @param  length    the number of bytes to copy - if -1, then copy all until EOF
     *
     * @return the number of bytes copied from the input to the output stream (usually length, but if length was larger
     *         than the number of bytes in <code>input</code> after the start byte, this return value will be less than
     *         <code>length</code>.
     *
     * @throws RuntimeException if failed to read or write the data
     */
    public static long copy(InputStream input, OutputStream output, long startByte, long length)
        throws RuntimeException {
        if (input == null) {
            throw new IllegalArgumentException("Input stream is null.");
        }
        if (output == null) {
            throw new IllegalArgumentException("Output stream is null.");
        }

        if (length == 0) {
            return 0;
        }

        if (startByte < 0) {
            throw new IllegalArgumentException("startByte=" + startByte);
        }

        long numBytesCopied = 0;
        int bufferSize = 32768;

        try {
            byte[] buffer = new byte[bufferSize];

            if (startByte > 0) {
                input.skip(startByte); // skips so the next read will read byte #startByte
            }

            // ok to cast to int, if length is less then bufferSize it must be able to fit into int
            int bytesRead = input.read(buffer, 0, ((length < 0) || (length >= bufferSize)) ? bufferSize : (int) length);

            while (bytesRead > 0) {
                output.write(buffer, 0, bytesRead);
                numBytesCopied += bytesRead;
                length -= bytesRead;
                bytesRead = input.read(buffer, 0, ((length < 0) || (length >= bufferSize)) ? bufferSize : (int) length);
            }

            output.flush();
        } catch (IOException ioe) {
            throw new RuntimeException("Stream data cannot be copied", ioe);
        }

        return numBytesCopied;
    }

    /**
     * Given a serializable object, this will return the object's serialized byte array representation.
     *
     * @param  object the object to serialize
     *
     * @return the serialized bytes
     *
     * @throws RuntimeException if failed to serialize the object
     */
    public static byte[] serialize(Serializable object) throws RuntimeException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream oos;

        try {
            oos = new ObjectOutputStream(byteStream);
            oos.writeObject(object);
            oos.close();
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to serialize object", ioe);
        }

        return byteStream.toByteArray();
    }

    /**
     * Deserializes the given serialization data and returns the object.
     *
     * @param  serializedData the serialized data as a byte array
     *
     * @return the deserialized object
     *
     * @throws RuntimeException if failed to deserialize the object
     */
    public static Object deserialize(byte[] serializedData) throws RuntimeException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(serializedData);
        ObjectInputStream ois;
        Object retObject;

        try {
            ois = new ObjectInputStream(byteStream);
            retObject = ois.readObject();
            ois.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize object", e);
        }

        return retObject;
    }

    /**
     * Can be used to safely close a stream. No-op if the stream is null.
     * 
     * @param stream the stream to close or null
     */
    public static void safeClose(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                LOG.error("Failed to close a stream.", e);
            }
        }
    }

}