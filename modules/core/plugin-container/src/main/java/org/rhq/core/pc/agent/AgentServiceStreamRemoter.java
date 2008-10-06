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
package org.rhq.core.pc.agent;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * An interface to any object that can prepare a stream for remote access.
 *
 * @author John Mazzitelli
 */
public interface AgentServiceStreamRemoter {
    /**
     * Given an input stream, this will prepare it to be accessed by remote clients. You should be able to send the
     * returned input stream remote clients.
     *
     * @param  stream the stream to remote
     *
     * @return the remoted input stream, possibly wrapped in a proxy to support remote access
     */
    InputStream prepareInputStream(InputStream stream);

    /**
     * Given an output stream, this will prepare it to be accessed by remote clients. You should be able to send the
     * returned output stream remote clients.
     *
     * @param  stream the stream to remote
     *
     * @return the remoted output stream, possibly wrapped in a proxy to support remote access
     */
    OutputStream prepareOutputStream(OutputStream stream);
}