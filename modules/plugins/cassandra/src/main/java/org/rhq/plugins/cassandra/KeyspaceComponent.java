/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.plugins.cassandra;

import java.util.Map;

import me.prettyprint.hector.api.ddl.KeyspaceDefinition;

import org.mc4j.ems.connection.EmsConnection;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.jmx.JMXComponent;

/**
 * @author John Sanda
 */
public class KeyspaceComponent<T extends ResourceComponent<?>> implements ResourceComponent<T>, ConfigurationFacet,
    JMXComponent<T> {

    private ResourceContext<T> context;

    @Override
    public void start(ResourceContext<T> context) throws Exception {
        this.context = context;
    }

    @Override
    public void stop() {
    }

    @Override
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    @Override
    public EmsConnection getEmsConnection() {
        JMXComponent<?> parent = (JMXComponent<?>) context.getParentResourceComponent();
        return parent.getEmsConnection();
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        KeyspaceDefinition keyspaceDef = getKeyspaceDefinition();

        Configuration config = new Configuration();
        config.put(new PropertySimple("name", keyspaceDef.getName()));
        config.put(new PropertySimple("replicationFactor", keyspaceDef.getReplicationFactor()));
        config.put(new PropertySimple("strategyClass", keyspaceDef.getStrategyClass()));
        config.put(new PropertySimple("durableWrites", keyspaceDef.isDurableWrites()));

        PropertyList list = new PropertyList("strategyOptions");
        Map<String, String> strategyOptions = keyspaceDef.getStrategyOptions();
        for (String optionName : strategyOptions.keySet()) {
            PropertyMap map = new PropertyMap("strategyOptionsMap");
            map.put(new PropertySimple("strategyOptionName", optionName));
            map.put(new PropertySimple("strategyOptionValue", strategyOptions.get(optionName)));
            list.add(map);
        }
        config.put(list);

        return config;
    }

    public KeyspaceDefinition getKeyspaceDefinition() {
        return CassandraUtil.getKeyspaceDefinition(context.getResourceKey());
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
    }

}
