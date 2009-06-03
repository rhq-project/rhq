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

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.MBeanResourceComponent;

/**
 * The resource component that represents the GroupDefinition / DynaGroup subsystem.
 * 
 * @author Joseph Marques
 */
public class GroupDefinitionResourceComponent extends MBeanResourceComponent {

    @Override
    @SuppressWarnings("unchecked")
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if ("retrieveStatistics".equals(name)) {

            Map<String, Map<String, Object>> allData;
            allData = (Map<String, Map<String, Object>>) getEmsBean().getAttribute("Statistics").refresh();
            
            OperationResult result = new OperationResult();
            PropertyList statistics = new PropertyList("statistics");
            result.getComplexResults().put(statistics);

            for (String groupDefinitionName : allData.keySet()) {

                PropertyMap stat = new PropertyMap("stat");
                stat.put(new PropertySimple("groupDefinitionName", groupDefinitionName));
                
                for (Map.Entry<String, Object> groupDefinitionStats : allData.get(groupDefinitionName).entrySet()) {
                    stat.put(new PropertySimple(groupDefinitionStats.getKey(), groupDefinitionStats.getValue()));
                }

                statistics.add(stat);
            }
            
            return result;
        }

        // isn't an operation we know about, must be an MBean operation that EMS can handle
        return super.invokeOperation(name, parameters);
    }

}
