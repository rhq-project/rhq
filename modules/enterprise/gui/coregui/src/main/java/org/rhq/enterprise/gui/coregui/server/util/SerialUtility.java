/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.server.util;

import org.rhq.enterprise.server.safeinvoker.HibernateDetachUtility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Greg Hinkle
 */
public class SerialUtility {

    private static Log log = LogFactory.getLog(SerialUtility.class);

    public static <T> T prepare(T value, String message) {

        long start = System.currentTimeMillis();
        try {
            HibernateDetachUtility.nullOutUninitializedFields(value, HibernateDetachUtility.SerializationType.SERIALIZATION);
            if (log.isDebugEnabled())
               log.debug("SerialUtility.prepare [" + message + "] Detached in: " + (System.currentTimeMillis() - start) +
                     "ms, Size is: " + serialSize(value));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }


    public static int serialSize(Object value) {

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(50000);
            ObjectOutputStream o = new ObjectOutputStream(baos);
            o.writeObject(value);
            o.flush();
            return baos.size();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

}
