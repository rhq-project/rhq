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

package org.rhq.services;

import java.util.Arrays;
import java.util.List;

import org.fedoraproject.Config.Services.ServiceHerder;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;

public class ServiceManager {
    private static final ServiceManager INSTANCE = new ServiceManager();

    private ServiceManager() {
    }

    public static final ServiceManager instance() {
        return INSTANCE;
    }

    public List<String> listSysVServices(DBusConnection conn) throws DBusException {
        return listServices(conn, "/org/fedoraproject/Config/Services/ServiceHerders/SysVServiceHerder");
    }

    public List<String> listXinetdServices(DBusConnection conn) throws DBusException {
        return listServices(conn, "/org/fedoraproject/Config/Services/ServiceHerders/XinetdServiceHerder");
    }

    private List<String> listServices(DBusConnection conn, String herderPath) throws DBusException {
        ServiceHerder herder = (ServiceHerder) conn.getRemoteObject(Service.BUS_NAME, herderPath, ServiceHerder.class);
        return Arrays.asList(herder.list_services());
    }
}
