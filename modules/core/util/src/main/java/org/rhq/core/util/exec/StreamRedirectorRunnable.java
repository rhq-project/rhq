 /*
 * RHQ Management Platform
 * Copyright (C) 2005-2015 Red Hat, Inc.
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
package org.rhq.core.util.exec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Redirects data coming in from one (input) stream into another (output) stream.
 *
 * @author John Mazzitelli
 */
public class StreamRedirectorRunnable implements Runnable {
    /**
     * the stream where we read data from
     */
    private final InputStream m_input;

    /**
     * the stream where we write data to
     */
    private final OutputStream m_output;

    private final String m_name;

    private static final Log LOG = LogFactory.getLog(StreamRedirectorRunnable.class);

    /**
     * Constructor for {@link StreamRedirectorRunnable} that takes an input stream where we read data in and an output stream
     * where we write the data read from the input stream. If the output stream is <code>null</code>, the incoming data
     * is simply consumed and ignored without being redirected anywhere.
     *
     * @param  name the name of the thread
     * @param  is   the input stream that we read data from (must not be <code>null</code>)
     * @param  os   the output stream that we write data to (may be <code>null</code>)
     *
     * @throws IllegalArgumentException if input stream is <code>null</code>
     */
    public StreamRedirectorRunnable(String name, InputStream is, OutputStream os) throws IllegalArgumentException {

        if (is == null) {
            throw new IllegalArgumentException("is=null");
        }

        m_name = name;
        m_input = is;
        m_output = os;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        Thread thread = Thread.currentThread();
        thread.setName(m_name);


        final int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];

        try {
            while (Thread.interrupted() != true) {
                int read = m_input.read(buffer, 0, bufferSize);
                // check for EOF
                if (read < 0) {
                    LOG.debug("Reached EOF on input stream"); //$NON-NLS-1$
                    break;
                }
                // do redirection
                if (read > 0 && m_output != null) {
                    m_output.write(buffer, 0, read);
                }
            }
        } catch (Throwable t) {
            LOG.warn("An unexpected error occurred while redirecting stream output: " + t.getMessage(), t); //$NON-NLS-1$
        } finally {
            // finished reading the input, close the streams and exit the thread
            try {
                m_input.close();
            } catch (IOException e) {
                // we do not care if close fails as there is nothing we can do
            }
            try {
                if (m_output != null) {
                    m_output.close();
                }
            } catch (IOException e) {
                // we do not care if close fails as there is nothing we can do
            }
        }

        return;
    }
}