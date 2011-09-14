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

package org.rhq.core.pc.upgrade.plugins.multi.base;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 *
 * @author Lukas Krejci
 */
public class BaseResourceComponent<T extends ResourceComponent<?>> implements ResourceComponent<T>,
    BaseResourceComponentInterface {

    private ResourceContext<T> context;
    private final Log log = LogFactory.getLog(getClass());

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    public void start(ResourceContext<T> context) throws InvalidPluginConfigurationException, Exception {
        log.info("Starting multi resource child component with resource key '" + context.getResourceKey() + "'.");
        this.context = context;
    }

    public void stop() {
        log.info("Stopping multi resource child component with resource key '" + context.getResourceKey() + "'.");
    }

    public int getOrdinal() {
        return context.getPluginConfiguration().getSimple("ordinal").getIntegerValue();
    }

    public Map<String, Set<Integer>> getChildrenToFailUpgrade() {

        Map<String, Set<Integer>> ret = new HashMap<String, Set<Integer>>();

        Configuration pluginConfig = context.getPluginConfiguration();
        PropertyList childrenToFail = pluginConfig.getList("childrenToFail");

        if (childrenToFail != null) {
            for (Property p : childrenToFail.getList()) {
                PropertyMap entry = (PropertyMap) p;

                String type = entry.getSimpleValue("type", null);

                PropertyList typeList = entry.getList("children");

                if (type != null && typeList != null) {
                    Set<Integer> ordinals = new HashSet<Integer>();

                    for (Property pp : typeList.getList()) {
                        ordinals.add(((PropertySimple) pp).getIntegerValue());
                    }

                    ret.put(type, ordinals);
                }
            }
        }

        return ret;
    }

    public Configuration createPluginConfigurationWithMarkedFailures(Map<String, Set<Integer>> childrenToFailUpgrade) {
        Configuration ret = context.getPluginConfiguration().clone();
        PropertyList list = new PropertyList("childrenToFail");

        for (Map.Entry<String, Set<Integer>> entry : childrenToFailUpgrade.entrySet()) {
            PropertyMap configEntry = new PropertyMap("childrenInType");
            configEntry.put(new PropertySimple("type", entry.getKey()));

            PropertyList typeList = new PropertyList("children");

            for (Integer childOrdinal : entry.getValue()) {
                typeList.add(new PropertySimple("ordinal", childOrdinal));
            }

            configEntry.put(typeList);

            list.add(configEntry);
        }

        ret.put(list);

        return ret;
    }
}
