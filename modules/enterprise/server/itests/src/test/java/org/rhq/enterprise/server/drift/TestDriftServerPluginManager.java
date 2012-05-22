/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation version 2 of the License.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the Free Software
 *  * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package org.rhq.enterprise.server.drift;

import java.io.File;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.plugin.PluginStatusType;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginType;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.drift.DriftServerPluginManager;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorMetadataParser;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
* @author John Sanda
*/
public class TestDriftServerPluginManager extends DriftServerPluginManager {
    public TestDriftServerPluginManager(DriftServerPluginContainer pc) {
        super(pc);
    }

    @Override
    protected ServerPlugin getPlugin(ServerPluginEnvironment env) {
        try {
            Configuration pluginConfig = null;
            Configuration scheduledJobsConfig = null;
            ConfigurationDefinition configDef;

            ServerPluginDescriptorType pluginDescriptor = env.getPluginDescriptor();

            configDef = ServerPluginDescriptorMetadataParser.getPluginConfigurationDefinition(pluginDescriptor);
            if (configDef != null) {
                pluginConfig = configDef.getDefaultTemplate().createConfiguration();
            }

            configDef = ServerPluginDescriptorMetadataParser.getScheduledJobsDefinition(pluginDescriptor);
            if (configDef != null) {
                scheduledJobsConfig = configDef.getDefaultTemplate().createConfiguration();
            }

            File pluginFile = new File(env.getPluginUrl().toURI());
            ServerPlugin plugin = new ServerPlugin(0, env.getPluginKey().getPluginName(), pluginFile.getName(),
                pluginDescriptor.getDisplayName(), true, PluginStatusType.INSTALLED, pluginDescriptor
                    .getDescription(), "", MessageDigestGenerator.getDigestString(pluginFile), pluginDescriptor
                    .getVersion(), pluginDescriptor.getVersion(), pluginConfig, scheduledJobsConfig,
                new ServerPluginType(pluginDescriptor).stringify(), System.currentTimeMillis(), System
                    .currentTimeMillis());
            return plugin;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
