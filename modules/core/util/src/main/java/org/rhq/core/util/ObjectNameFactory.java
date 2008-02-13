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
package org.rhq.core.util;

import java.util.Hashtable;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * Should only be used for static instantiantion of object names.
 */
public class ObjectNameFactory {
    public static ObjectName create(String name) {
        try {
            return new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            throw new Error("Invalid ObjectName: " + name + "; " + e);
        }
    }

    public static ObjectName create(String domain, String key, String value) {
        try {
            return new ObjectName(domain, key, value);
        } catch (MalformedObjectNameException e) {
            throw new Error("Invalid ObjectName: " + domain + "," + key + "," + value + "; " + e);
        }
    }

    public static ObjectName create(String domain, Hashtable table) {
        try {
            return new ObjectName(domain, table);
        } catch (MalformedObjectNameException e) {
            throw new Error("Invalid ObjectName: " + domain + "," + table + "; " + e);
        }
    }
}