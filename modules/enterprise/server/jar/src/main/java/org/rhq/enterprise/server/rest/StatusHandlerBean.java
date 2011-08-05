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
package org.rhq.enterprise.server.rest;

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;

import org.rhq.enterprise.server.rest.domain.Status;

/**
 * Return system status
 * @author Heiko W. Rupp
 */
@Stateless
@Interceptors(SetCallerInterceptor.class)
public class StatusHandlerBean extends AbstractRestBean implements StatusHandlerLocal {

    @Override
    public Status getStatus() {

        Status status = new Status();

        // TODO deliver real implementation

        status.setAlerts(123);
        status.setAlertTemplates(45);
        status.setPlatforms(1);
        status.setServers(2);
        status.setServices(3);
        status.setSchedules(4);
        status.setMetricsMin(99);

        return status;
    }
}
