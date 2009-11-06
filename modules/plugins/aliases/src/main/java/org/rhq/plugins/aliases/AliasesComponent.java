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
package org.rhq.plugins.aliases;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.helper.AugeasNode;
import net.augeas.Augeas;

import java.util.List;

public class AliasesComponent extends AugeasConfigurationComponent {

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        super.start(resourceContext);
    }

    public void stop() {
        return;
    }

    public AvailabilityType getAvailability() {
        AvailabilityType aType = super.getAvailability();
        return aType;
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Configuration config = super.loadResourceConfiguration();
        return config;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
            super.updateResourceConfiguration(report);
    }

    @Override
    protected AugeasNode getNewListMemberNode(AugeasNode listNode, PropertyDefinitionMap listMemberPropDefMap, int listIndex) {
        PropertyDefinitionMap member = listMemberPropDefMap;
        AugeasNode node = new AugeasNode(listNode, "0" + listIndex);
        return node;
    }

    protected AugeasNode getExistingChildNodeForListMemberPropertyMap(AugeasNode parentNode,
                                                                      PropertyDefinitionList propDefList, PropertyMap propMap) {
        // First find all child nodes with the same 'name' value as the PropertyMap.
        Augeas augeas = getAugeas();

        String name = propMap.getSimple("name").getStringValue();
        String nameFilter = parentNode.getPath() + "/name";
        List<String> namePaths = augeas.match(String.format(nameFilter + "/name[.='%s']", name));
        if (namePaths == null || namePaths.isEmpty()) {
            return null;
        }
        AugeasNode node = new AugeasNode(namePaths.get(0));
        return node.getParent();
    }
}
