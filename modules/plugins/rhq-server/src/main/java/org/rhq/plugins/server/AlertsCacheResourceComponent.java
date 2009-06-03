/*
 * Jopr Management Platform
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
package org.rhq.plugins.server;

import java.util.Map;

import org.mc4j.ems.connection.bean.EmsBean;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * The resource component that represents the AlertConditionCache with the Alerts subsystem.
 * 
 * @author Joseph Marques
 */
public class AlertsCacheResourceComponent extends MBeanResourceComponent {

    @SuppressWarnings("unchecked")
    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if ("reloadCaches".equals(name)) {
            EmsBean emsBean = getEmsBean();
            Map<String, Integer> before = (Map<String, Integer>) emsBean.getAttribute("CacheCounts").refresh();
            emsBean.getAttribute("ReloadCaches"); // void return
            Map<String, Integer> after = (Map<String, Integer>) emsBean.getAttribute("CacheCounts").refresh();

            OperationResult result = new OperationResult();
            PropertyList statistics = new PropertyList("reloadStatistics");
            result.getComplexResults().put(statistics);
            for (String cacheName : before.keySet()) {
                    PropertyMap stat = new PropertyMap("stat");
                    stat.put(new PropertySimple("cacheName", cacheName));
                    stat.put(new PropertySimple("beforeReloading", before.get(cacheName)));
                    stat.put(new PropertySimple("afterReloading", after.get(cacheName)));
                    statistics.add(stat);

            }
            return result;
        }

        // isn't an operation we know about, must be an MBean operation that EMS can handle
        return super.invokeOperation(name, parameters);
    }

}
