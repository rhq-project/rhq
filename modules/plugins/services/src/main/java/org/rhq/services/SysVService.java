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

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * @author paji
 *
 */
public class SysVService extends Service implements org.fedoraproject.Config.Services.SysVService {

    private String serviceName;
    private org.fedoraproject.Config.Services.SysVService service;
    private static final String SERVICE_PATH = "/org/fedoraproject/Config/Services/ServiceHerders/SysVServiceHerder/Services/";

    protected SysVService(String name) {
        serviceName = name;
    }

    public static SysVService load(String serviceName, DBusConnection conn) throws DBusException {
        SysVService service = new SysVService(serviceName);
        service.setup(conn);
        return service;
    }

    @Override
    protected void setup(DBusConnection con) throws DBusException {
        super.setup(con);
        service = con.getRemoteObject(BUS_NAME, getObjectPath(), org.fedoraproject.Config.Services.SysVService.class);
    }

    /* (non-Javadoc)
     * @see org.fedoraproject.Config.Services.SysVService#get_description()
     */
    @Override
    public String get_description() {
        return service.get_description();
    }

    /* (non-Javadoc)
     * @see org.fedoraproject.Config.Services.SysVService#get_runlevels()
     */
    @Override
    public int[] get_runlevels() {
        return service.get_runlevels();
    }

    /* (non-Javadoc)
     * @see org.fedoraproject.Config.Services.SysVService#get_shortdescription()
     */
    @Override
    public String get_shortdescription() {
        return service.get_shortdescription();
    }

    /* (non-Javadoc)
     * @see org.fedoraproject.Config.Services.SysVService#get_status()
     */
    @Override
    public int get_status() {
        return service.get_status();
    }

    public boolean isRunning() {
        return get_status() == SVC_STATUS_RUNNING;
    }

    /* (non-Javadoc)
     * @see org.fedoraproject.Config.Services.SysVService#get_status_updates_running()
     */
    @Override
    public int get_status_updates_running() {
        return service.get_status_updates_running();
    }

    /* (non-Javadoc)
     * @see org.fedoraproject.Config.Services.SysVService#reload()
     */
    @Override
    public void reload() {
        service.reload();
    }

    /* (non-Javadoc)
     * @see org.fedoraproject.Config.Services.SysVService#restart()
     */
    @Override
    public void restart() {
        service.restart();
    }

    /* (non-Javadoc)
     * @see org.fedoraproject.Config.Services.SysVService#set_runlevels(int[])
     */
    @Override
    public void set_runlevels(int[] levels) {
        service.set_runlevels(levels);
    }

    /* (non-Javadoc)
     * @see org.fedoraproject.Config.Services.SysVService#start()
     */
    @Override
    public void start() {
        service.start();
    }

    /* (non-Javadoc)
     * @see org.fedoraproject.Config.Services.SysVService#stop()
     */
    @Override
    public void stop() {
        service.stop();
    }

    @Override
    protected String getObjectPath() {
        return SERVICE_PATH + serviceName.replace("-", "_");
    }

    @Override
    public boolean isSysVService() {
        return true;
    }

}
