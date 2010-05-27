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

package org.rhq.core.util.stream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.rhq.core.util.MessageDigestGenerator;

/**
 * This copies stream data to another stream, while calculating the stream's message
 * digest on the fly as the copy is performed.
 *
 * @author John Mazzitelli
 */
public class StreamCopyDigest {
    private final MessageDigestGenerator generator;

    public StreamCopyDigest() {
        this.generator = new MessageDigestGenerator();
    }

    public StreamCopyDigest(MessageDigestGenerator generator) {
        this.generator = generator;
    }

    /**
     * @return the generator used to calculate digests/hashcodes
     */
    public MessageDigestGenerator getMessageDigestGenerator() {
        return generator;
    }

    /**
     * Copies the input stream data to the output stream and returns the
     * copied data's digest string.
     * 
     * Note: the streams are never closed - the caller is responsible for that.
     * 
     * @param in input content
     * @param out where to write the input content
     * @return the copied content's digest string (aka hashcode) 
     */
    public String copyAndCalculateHashcode(InputStream in, OutputStream out) {
        try {
            byte[] buffer = new byte[32768];
            BufferedInputStream bufferedStream = new BufferedInputStream(in, buffer.length);
            for (int bytesRead = bufferedStream.read(buffer); bytesRead != -1; bytesRead = bufferedStream.read(buffer)) {
                out.write(buffer, 0, bytesRead);
                this.generator.add(buffer, 0, bytesRead);
            }
            out.flush();
        } catch (IOException ioe) {
            throw new RuntimeException("Stream data cannot be copied", ioe);
        }

        return this.generator.getDigestString();
    }
}
