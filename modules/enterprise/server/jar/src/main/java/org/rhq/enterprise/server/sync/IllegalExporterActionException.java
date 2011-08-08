/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.sync;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 * This exception gets thrown from {@link ExportWriter} when some exporter performs
 * an illegal action on the writer.
 *
 * @author Lukas Krejci
 */
public class IllegalExporterActionException extends XMLStreamException {

    private static final long serialVersionUID = 1L;

    public IllegalExporterActionException() {
    }

    public IllegalExporterActionException(String msg, Location location, Throwable th) {
        super(msg, location, th);
    }

    public IllegalExporterActionException(String msg, Location location) {
        super(msg, location);
    }

    public IllegalExporterActionException(String msg, Throwable th) {
        super(msg, th);
    }

    public IllegalExporterActionException(String msg) {
        super(msg);
    }

    public IllegalExporterActionException(Throwable th) {
        super(th);
    }    
}
