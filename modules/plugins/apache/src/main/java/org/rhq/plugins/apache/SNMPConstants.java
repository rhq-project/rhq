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
package org.rhq.plugins.apache;

public abstract class SNMPConstants {
    public static final String COLUMN_VHOST_NAME = "wwwServiceName";
    public static final String COLUMN_VHOST_PORT = "wwwServiceProtocol";
    public static final String COLUMN_VHOST_DESC = "wwwServiceDescription";
    public static final String COLUMN_VHOST_ADM = "wwwServiceContact";

    public static final String TCP_PROTO_ID = "1.3.6.1.2.1.6";
}