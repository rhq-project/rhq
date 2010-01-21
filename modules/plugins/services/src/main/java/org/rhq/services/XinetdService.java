/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.services;

import org.freedesktop.dbus.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;

/**
 * @author paji
 *
 */
public class XinetdService extends Service implements org.fedoraproject.Config.Services.XinetdService {
    private String serviceName;
    private org.fedoraproject.Config.Services.XinetdService service;
    private static final String SERVICE_PATH = "/org/fedoraproject/Config/Services/ServiceHerders/XinetdServiceHerder/Services/";

    protected XinetdService(String name) {
        serviceName = name;
    }

    public static XinetdService load(String serviceName, DBusConnection conn) throws DBusException {
        XinetdService service = new XinetdService(serviceName);
        service.setup(conn);
        return service;
    }

    @Override
    protected void setup(DBusConnection con) throws DBusException {
        super.setup(con);
        service = con.getRemoteObject(BUS_NAME, getObjectPath(), org.fedoraproject.Config.Services.XinetdService.class);
    }

    /* (non-Javadoc)
     * @see org.rhq.services.Service#getObjectPath()
     */
    @Override
    protected String getObjectPath() {
        return SERVICE_PATH + serviceName.replace("-", "_");
    }

    /* (non-Javadoc)
     * @see org.fedoraproject.Config.Services.XinetdService#get_description()
     */
    @Override
    public String get_description() {
        return service.get_description();
    }

    /* (non-Javadoc)
     * @see org.fedoraproject.Config.Services.XinetdService#save(boolean)
     */
    @Override
    public void save(boolean enabled) {
        service.save(enabled);
    }

}
