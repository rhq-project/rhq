/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.plugins.samba;

import java.util.List;

import net.augeas.Augeas;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.helper.AugeasNode;

/**
 * TODO
 */
public class SambaShareComponent extends AugeasConfigurationComponent<SambaServerComponent> {
    public static final String TARGET_NAME_PROP = "targetName";

    public static final String NAME_RESOURCE_CONFIG_PROP = "name";

    static final String RESOURCE_TYPE_NAME = "Samba Share";

    public void start(ResourceContext<SambaServerComponent> resourceContext) throws Exception {
        super.start(resourceContext);
    }

    @Override
    protected String getResourceConfigurationRootPath() {
        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        String targetName = pluginConfig.getSimple(TARGET_NAME_PROP).getStringValue();
        String targetPath = "/files/etc/samba/smb.conf/target[.='" + targetName + "']";
        AugeasNode targetNode = new AugeasNode(targetPath);
        Augeas augeas = getAugeas();

        List<String> matches = augeas.match(targetNode.getPath());
        return matches.get(0);
    }

    public void stop() {
        super.stop();
    }

    public AvailabilityType getAvailability() {
        return super.getAvailability();
    }

    public Configuration loadResourceConfiguration() throws Exception {
        Configuration resourceConfig = super.loadResourceConfiguration();
        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        String targetName = pluginConfig.getSimple(TARGET_NAME_PROP).getStringValue();
        resourceConfig.put(new PropertySimple(NAME_RESOURCE_CONFIG_PROP, targetName));
        return resourceConfig;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        super.updateResourceConfiguration(report);
    }

    public void deleteResource() throws Exception {
        initAugeas();
        String rootPath = getResourceConfigurationRootPath();
        try {
            Augeas augeas = getAugeas();
            augeas.remove(rootPath);
            augeas.save();
        } finally {
            close();
        }
    }

}
