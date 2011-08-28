/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.core.pc.drift;

import java.io.IOException;
import java.io.InputStream;

/**
 * DriftInputStream is a simple wrapper the delegates to another input stream and executes
 * a task after the underlying stream is closed. {@link DriftManager} uses this class to perform
 * clean up after streaming change set reports and change set content to the server.
 */
// TODO come up with a better, more intuitive class name
public class DriftInputStream extends InputStream {

    private InputStream stream;

    private Runnable cleanupTask;

    /**
     * Createa a new DriftInputStream.
     *
     * @param stream The stream to which this object delegates all calls
     * @param cleanupTask A task to execute immediately after the stream is closed
     */
    public DriftInputStream(InputStream stream, Runnable cleanupTask) {
        this.stream = stream;
        this.cleanupTask = cleanupTask;
    }

    @Override
    public int read() throws IOException {
        return stream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return stream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return stream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return stream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return stream.available();
    }

    @Override
    public void close() throws IOException {
        stream.close();
        cleanupTask.run();
    }

    @Override
    public void mark(int readlimit) {
        stream.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        stream.reset();
    }

    @Override
    public boolean markSupported() {
        return stream.markSupported();
    }
}
