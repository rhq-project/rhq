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

import java.io.ByteArrayOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * in-memory output stream with a limited buffer size. Once limit is reached it starts to ignore
 * {@link #write(int)} and {@link #write(byte[], int, int)} calls. Default limit is 2MB - this default can be
 * set using  rhq.process-execution.captured-output.limit system property (in Bytes) or via {@link #ProcessExecutionOutputStream(int, boolean)}.
 * Optionally written output can be forwarded to logging subsystem.
 * @author lzoubek@redhat.com
 *
 */
public class ProcessExecutionOutputStream extends ByteArrayOutputStream {

    private static final Log LOG = LogFactory.getLog(ProcessExecutionOutputStream.class);

    private int limit = Integer.getInteger("rhq.process-execution.captured-output.limit", 2 * 1024 * 1024); // default 2MB

    private boolean writeToLog;

    /**
     * Creates output stream with specified limit size
     * @param limit (in Bytes) maximum size of stream buffer, once reached, stream silently ignores any writes
     * @param writeToLog true to forward all messages to logger
     */
    public ProcessExecutionOutputStream(int limit, boolean writeToLog) {
        super();
        this.limit = limit;
        this.writeToLog = writeToLog;
    }

    /**
     * Creates output stream with default stream buffer limit (2MB)
     * @param writeToLog true to forward all messages to logger
     */
    public ProcessExecutionOutputStream(boolean writeToLog) {
        super();
        this.writeToLog = writeToLog;
    }

    @Override
    public synchronized void write(int b) {
        if (this.count < this.limit) {
            super.write(b);
        }

        if (this.writeToLog) {
            LOG.info(String.valueOf(b));
        }
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) {
        if (this.count < this.limit) {
            if (this.count + len > this.limit) {
                len = this.limit - this.count;
            }
            super.write(b, off, len);
        }

        if (this.writeToLog && len > 0) {
            LOG.info(new String(this.buf, this.count - len, len));
        }
    }
}
