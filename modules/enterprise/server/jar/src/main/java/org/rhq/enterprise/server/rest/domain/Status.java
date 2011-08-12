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
package org.rhq.enterprise.server.rest.domain;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * System status
 * @author Heiko W. Rupp
 */
@XmlRootElement
public class Status {

    int platforms;
    int servers;
    int services;
    int alerts;
    int alertDefinitions;
    int metricsMin;
    int schedules;

    public Status() {
    }

    public int getPlatforms() {
        return platforms;
    }

    public void setPlatforms(int platforms) {
        this.platforms = platforms;
    }

    public int getServers() {
        return servers;
    }

    public void setServers(int servers) {
        this.servers = servers;
    }

    public int getServices() {
        return services;
    }

    public void setServices(int services) {
        this.services = services;
    }

    public int getAlerts() {
        return alerts;
    }

    public void setAlerts(int alerts) {
        this.alerts = alerts;
    }

    public int getAlertDefinitions() {
        return alertDefinitions;
    }

    public void setAlertDefinitions(int alertDefinitions) {
        this.alertDefinitions = alertDefinitions;
    }

    public int getMetricsMin() {
        return metricsMin;
    }

    public void setMetricsMin(int metricsMin) {
        this.metricsMin = metricsMin;
    }

    public int getSchedules() {
        return schedules;
    }

    public void setSchedules(int schedules) {
        this.schedules = schedules;
    }
}
