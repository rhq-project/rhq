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
package org.rhq.plugins.platform.content.yum;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import org.rhq.core.domain.content.PackageDetailsKey;

/**
 * Represents an rpm package.
 *
 * @author jortel
 */
public class Package extends Content {
    /**
     * The package key.
     */
    private PackageDetailsKey packageKey;

    /**
     * Construct a package with an active yum request object.
     *
     * @param request An active yum request.
     */
    public Package(Request request) {
        super(request);
        this.packageKey = Primary.toKey(request.args);
    }

    /**
     * Get the length in bytes of this package.
     *
     * @return The lengh of this package.
     *
     * @throws Exception on all errors.
     */
    @Override
    public long length() throws Exception {
        long result = 0;
        long[] range = byteRange();
        if (range != null) {
            result = (range[1] - range[0]) + 1;
        } else {
            result = context().getPackageBitsLength(packageKey);
        }

        return result;
    }

    /**
     * Open an input stream for the package bits.
     *
     * @throws Exception On error.
     */
    @Override
    public InputStream openStream() throws Exception {
        return null;
    }

    /**
     * Write the http header for this package to the specified stream.
     *
     * @param  ostr An open output stream.
     *
     * @throws Exception On all errors.
     */
    @Override
    public void writeHeader(OutputStream ostr) throws Exception {
        PrintWriter writer = new PrintWriter(ostr);
        writer.printf("HTTP/1.1 200\n");
        writer.println("Server: Ackbar (Red Hat)");
        writer.printf("Content-Length: %d\n\n", length());
        writer.flush();
        ostr.flush();
    }

    /**
     * Write the content (bits) for this package to the specified stream.
     *
     * @param  ostr An open output stream.
     *
     * @throws Exception On all errors.
     */
    @Override
    public void writeContent(OutputStream ostr) throws Exception {
        long[] range = byteRange();
        if (range != null) {
            context().writePackageBits(packageKey, range, ostr);
        } else {
            context().writePackageBits(packageKey, ostr);
        }
    }

    /**
     * Get the byte range specified in the request.
     *
     * @return Returns the byte range if specified, else null.
     */
    private long[] byteRange() {
        long[] result = null;
        String range = request.fields.get("Range");
        if (range != null) {
            String[] bytes = range.split("=")[1].split("-");
            result = new long[2];
            result[0] = Long.valueOf(bytes[0]);
            result[1] = Long.valueOf(bytes[1]);
        }

        return result;
    }
}