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
package org.rhq.core.util.exec;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Redirects data coming in from one (input) stream into another (output) stream.
 *
 * @author John Mazzitelli
 */
public class StreamRedirector extends Thread {
    /**
     * the stream where we read data from
     */
    private final InputStream m_input;

    /**
     * the stream where we write data to
     */
    private final OutputStream m_output;

    /**
     * Constructor for {@link StreamRedirector} that takes an input stream where we read data in and an output stream
     * where we write the data read from the input stream. If the output stream is <code>null</code>, the incoming data
     * is simply consumed and ignored without being redirected anywhere.
     *
     * @param  name the name of the thread
     * @param  is   the input stream that we read data from (must not be <code>null</code>)
     * @param  os   the output stream that we write data to (may be <code>null</code>)
     *
     * @throws IllegalArgumentException if input stream is <code>null</code>
     */
    public StreamRedirector(String name, InputStream is, OutputStream os) throws IllegalArgumentException {
        super(name);

        if (is == null) {
            throw new IllegalArgumentException("is=null");
        }

        setDaemon(true);

        m_input = is;
        m_output = os;

        return;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        final int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];
        boolean keepGoing = true;

        try {
            while (keepGoing) {
                int read = m_input.read(buffer, 0, bufferSize);
                if (read > 0) {
                    if (m_output != null) {
                        m_output.write(buffer, 0, read);
                    }
                } else {
                    keepGoing = false;
                }
            }
        } catch (Exception e) {
            // just abort the while loop and close the streams
        }

        // finished reading the input, so we can now close the streams and exit the thread
        try {
            m_input.close();
        } catch (IOException e) {
        }

        try {
            if (m_output != null) {
                m_output.close();
            }
        } catch (IOException e) {
        }

        return;
    }
}