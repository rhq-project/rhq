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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.plugins.samba;

import net.augeas.Augeas;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.helper.AugeasNode;

import java.util.List;

/**
 *
 */
public class SambaShareComponent extends AugeasConfigurationComponent implements DeleteResourceFacet {
    public void start(ResourceContext resourceContext)
        throws Exception {        
        super.start(resourceContext);
    }

    @Override
    protected String getResourceConfigurationRootPath() {
        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        String targetName = pluginConfig.getSimple("targetName").getStringValue();
        String targetPath = "/files/etc/samba/smb.conf/target[.='" + targetName + "']";
        AugeasNode targetNode = new AugeasNode(targetPath);
        Augeas augeas = getAugeas();
        augeas.load();
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
        return super.loadResourceConfiguration();
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        super.updateResourceConfiguration(report);
    }

    public void deleteResource() throws Exception {
        String rootPath = getResourceConfigurationRootPath();
        Augeas augeas = getAugeas();
        augeas.remove(rootPath);
        augeas.save();
        return;        
    }
}
