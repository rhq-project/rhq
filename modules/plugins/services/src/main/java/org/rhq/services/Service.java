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

import org.fedoraproject.Config.Services.ChkconfigService;
import org.fedoraproject.Config.Services.Introspectable;
import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;

public abstract class Service implements ChkconfigService, org.fedoraproject.Config.Services.Service, Introspectable {

    public static final int SVC_STATUS_REFRESHING = 0;
    public static final int SVC_STATUS_UNKNOWN = 1;
    public static final int SVC_STATUS_STOPPED = 2;
    public static final int SVC_STATUS_RUNNING = 3;
    public static final int SVC_STATUS_DEAD = 4;

    public static final int SVC_ENABLED_REFRESHING = 0;
    public static final int SVC_ENABLED_ERROR = 1;
    public static final int SVC_ENABLED_YES = 2;
    public static final int SVC_ENABLED_NO = 3;
    public static final int SVC_ENABLED_CUSTOM = 4;

    private ChkconfigService chkconfig;
    private org.fedoraproject.Config.Services.Service service;
    private Introspectable introspectable;
    protected DBusConnection conn;
    public static final String BUS_NAME = "org.fedoraproject.Config.Services";

    protected abstract String getObjectPath();

    protected Service() {
    }

    protected void setup(DBusConnection con) throws DBusException {

        /**"/org/fedoraproject/Config/Services/ServiceHerders/SysVServiceHerder/Services/"+sysVservice.replace("-", "_"),*/
        conn = con;
        chkconfig = conn.getRemoteObject(BUS_NAME, getObjectPath(), ChkconfigService.class);
        service = conn.getRemoteObject(BUS_NAME, getObjectPath(), org.fedoraproject.Config.Services.Service.class);
        introspectable = conn.getRemoteObject(BUS_NAME, getObjectPath(), Introspectable.class);
    }

    @Override
    public void disable() {
        chkconfig.disable();
    }

    @Override
    public void enable() {
        chkconfig.enable();
    }

    @Override
    public int get_enabled() {
        return chkconfig.get_enabled();
    }

    public boolean isEnabled() {
        return SVC_ENABLED_YES == chkconfig.get_enabled();
    }

    @Override
    public boolean isRemote() {
        return true;
    }

    @Override
    public void save() {
        service.save();
    }

    @Override
    public String Introspect() {
        return introspectable.Introspect();
    }

    public boolean isSysVService() {
        return false;
    }
}
