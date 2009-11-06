/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.communications;

/**
 * @author Ian Springer
 */
public class CommTestConstants {
    private static final String CONNECTOR_BIND_PORT_SYSPROP = "rhq.comm.test.connectorBindPort";
    private static final String CONNECTOR2_BIND_PORT_SYSPROP = "rhq.comm.test.connector2BindPort";
    private static final String DEFAULT_CONNECTOR2_BIND_PORT = "62621";
    private static final String DEFAULT_CONNECTOR_BIND_PORT = "33333";
    public static final String CONNECTOR_BIND_PORT = System.getProperty(CONNECTOR_BIND_PORT_SYSPROP,
            DEFAULT_CONNECTOR_BIND_PORT);
    public static final String CONNECTOR2_BIND_PORT = System.getProperty(CONNECTOR2_BIND_PORT_SYSPROP,
            DEFAULT_CONNECTOR2_BIND_PORT);
}
