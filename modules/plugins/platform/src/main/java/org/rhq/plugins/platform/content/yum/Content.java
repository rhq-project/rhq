 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.platform.content.yum;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The content class is the base class for yum file content. This includes the medatadata as well as the packages
 * (rpms).
 *
 * @author jortel
 */
public abstract class Content {
    /**
     * The current yum request.
     */
    protected Request request;

    /**
     * A stream copy buffer.
     */
    protected byte[] bfr = new byte[10240];

    /**
     * Logger
     */
    protected final Log log = LogFactory.getLog(Content.class);

    /**
     * Construct a new content object.
     *
     * @param request The request that is constructing the object.
     */
    protected Content(Request request) {
        this.request = request;
    }

    public abstract long length() throws Exception;

    public abstract void writeContent(OutputStream ostr) throws Exception;

    public abstract void writeHeader(OutputStream ostr) throws Exception;

    public abstract InputStream openStream() throws Exception;

    /**
     * Delete the local content artifact.
     */
    public void delete() {
    }

    /**
     * Transfer bytes from the input stream to the output stream.
     *
     * @param  istr An input stream.
     * @param  ostr An output stream.
     *
     * @throws IOException On all errors.
     */
    protected void transfer(InputStream istr, OutputStream ostr) throws IOException {
        while (true) {
            int bytesread = istr.read(bfr);
            if (bytesread != -1) {
                ostr.write(bfr, 0, bytesread);
            } else {
                break;
            }
        }

        ostr.flush();
    }

    protected YumContext context() {
        return request.context();
    }
}