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
package org.rhq.enterprise.agent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.rhq.core.util.stream.StreamUtil;

/**
 * A test POJO that processes data from an input stream and sends that data back in its return value to confirm the
 * stream data was received.
 *
 * @author John Mazzitelli
 */
public class SimpleTestStreamPojo implements ITestStreamPojo {
    /**
     * @see ITestStreamPojo#streamData(InputStream)
     */
    public String streamData(InputStream stream) {
        byte[] b = StreamUtil.slurp(stream);
        return new String(b);
    }

    /**
     * @see ITestStreamPojo#streamData(InputStream, InputStream)
     */
    public String[] streamData(InputStream stream1, InputStream stream2) {
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();

        try {
            // do 2 first, then 1 but don't close either stream to force the remote stream services to stay up
            StreamUtil.copy(stream2, out2, false);
            StreamUtil.copy(stream1, out1, false);
        } finally {
            try {
                // now we've slurped both streams, we can close them
                stream1.close();
                stream2.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return new String[] { new String(out1.toByteArray()), new String(out2.toByteArray()) };
    }

    /**
     * @see ITestStreamPojo#streamData(String, int, InputStream, String)
     */
    public String streamData(String string1, int integer2, InputStream stream, String string3) {
        String stream_string = streamData(stream);
        return stream_string + string1 + integer2 + string3;
    }
}