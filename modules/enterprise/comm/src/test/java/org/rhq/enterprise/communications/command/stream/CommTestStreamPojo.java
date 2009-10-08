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
package org.rhq.enterprise.communications.command.stream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.communications.command.client.RemoteInputStream;
import org.rhq.enterprise.communications.command.client.RemoteOutputStream;

/**
 * POJO to test remote streaming.
 */
public class CommTestStreamPojo implements ICommTestStreamPojo {
    /**
     * This is the string that will be available in the {@link #returnInputStream()}.
     */
    public static final String INPUT_STREAM_STRING = "CommTestStreamPojo INPUT STREAM";

    /**
     * This is the output stream that was returned in the last call to {@link #returnOutputStream()}. It is public so
     * tests can access it directly.
     */
    public ByteArrayOutputStream byteArrayOutputStream;

    private CommStreamTest test;

    public CommTestStreamPojo(CommStreamTest test) {
        this.test = test;
    }

    public boolean ping() {
        return true;
    }

    public InputStream returnInputStream() throws Exception {
        InputStream in;
        in = test.prepareRemoteStreamInServer2(new ByteArrayInputStream(INPUT_STREAM_STRING.getBytes()));

        if (!(in instanceof RemoteInputStream)) {
            throw new RuntimeException("Stream should have been a remote input stream");
        }

        return in;
    }

    public OutputStream returnOutputStream() throws Exception {
        byteArrayOutputStream = new ByteArrayOutputStream();
        OutputStream out = test.prepareRemoteStreamInServer2(byteArrayOutputStream);

        if (!(out instanceof RemoteOutputStream)) {
            throw new RuntimeException("Stream should have been a remote output stream");
        }

        return out;
    }

    public boolean slurpInputStream(InputStream stream, String expectedStreamContents) {
        if (!(stream instanceof RemoteInputStream)) {
            throw new RuntimeException("Stream should have been a remote input stream");
        }

        // we slurp the contents of the stream and compare it to what the
        // caller says we should expect to be in there. If they don't
        // match, we'll throw an exception.
        String streamContents = new String(StreamUtil.slurp(stream));
        if (!streamContents.equals(expectedStreamContents)) {
            throw new RuntimeException("Stream contents [" + streamContents + "] was not the expected value of ["
                + expectedStreamContents + "]");
        }

        return true;
    }

    public boolean slurpOutputStream(OutputStream stream, String contentsToWrite) throws Exception {
        if (!(stream instanceof RemoteOutputStream)) {
            throw new RuntimeException("Stream should have been a remote output stream");
        }

        // caller is telling us what contents to write to the stream
        stream.write(contentsToWrite.getBytes());
        stream.close();

        return true;
    }

    public long slurpOutputStreamRange(OutputStream stream, String contents, long startByte, long endByte)
        throws Exception {
        // for this test, send in endByte that is not negative
        if (endByte < 0) {
            throw new IllegalArgumentException("Do not send in endByte<0 for this test method: " + endByte);
        }

        InputStream in = new ByteArrayInputStream(contents.getBytes());
        long length = (endByte - startByte) + 1;
        return StreamUtil.copy(in, stream, startByte, length);
    }
}