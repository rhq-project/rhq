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
package org.rhq.core.pluginapi.inventory;

/**
 * This exception is thrown by plugin resource components when an attempt to connect to a managed resource fails due to
 * an invalid plugin configuration.
 *
 * @author John Mazzitelli
 */
public class InvalidPluginConfigurationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidPluginConfigurationException(String message) {
        super(message);
    }

    public InvalidPluginConfigurationException(Throwable cause) {
        super(cause);
    }

    public InvalidPluginConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}