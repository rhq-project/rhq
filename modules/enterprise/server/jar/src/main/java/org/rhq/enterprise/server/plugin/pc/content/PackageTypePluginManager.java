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

package org.rhq.enterprise.server.plugin.pc.content;

import java.util.HashMap;
import java.util.Map;

import org.rhq.core.clientapi.agent.metadata.ConfigurationMetadataParser;
import org.rhq.core.clientapi.agent.metadata.InvalidPluginDescriptorException;
import org.rhq.core.domain.content.PackageType;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.content.ContentManagerLocal;
import org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginManager;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.packagetype.PackageTypeDefinitionType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.packagetype.PackageTypePluginDescriptorType;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class PackageTypePluginManager extends ServerPluginManager {

    private Map<PackageType, ServerPluginEnvironment> pluginsByPackageTypeId;

    public PackageTypePluginManager(AbstractTypeServerPluginContainer pc) {
        super(pc);
        pluginsByPackageTypeId = new HashMap<PackageType, ServerPluginEnvironment>();
    }

    @Override
    protected void loadPlugin(ServerPluginEnvironment env, boolean enabled) throws Exception {
        super.loadPlugin(env, enabled);

        PackageTypePluginDescriptorType descriptor = (PackageTypePluginDescriptorType) env.getPluginDescriptor();

        try {
            //check the behavior class exists
            String behaviorClassName = descriptor.getBehaviorClass();

            Class<?> behaviorClass = loadPluginClass(env, behaviorClassName, false);

            if (!AbstractPackageTypeBehavior.class.isAssignableFrom(behaviorClass)) {
                throw new Exception("The behavior class '" + behaviorClassName + "' of the plugin '"
                    + env.getPluginKey().getPluginName() + "' does not inherit from AbstractPackageTypeBehavior class.");
            }

            //check that all the package types defined by this plugin exist
            for (PackageTypeDefinitionType def : descriptor.getPackageType()) {
                PackageType pt = ensurePackageTypeExists(def);
                pluginsByPackageTypeId.put(pt, env);
            }
        } catch (Exception e) {
            // do not deploy this plugin - its stinky
            try {
                unloadPlugin(env.getPluginKey().getPluginName());
            } catch (Exception ignore) {
            }
            throw e;
        }
    }

    private PackageType ensurePackageTypeExists(PackageTypeDefinitionType def) throws InvalidPluginDescriptorException {
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        ContentManagerLocal cm = LookupUtil.getContentManager();

        PackageType packageType = cm.findPackageType(subjectManager.getOverlord(), null, def.getName());

        if (packageType == null) {
            //TODO support tying the package type to the resource types?
            packageType = new PackageType(def.getName(), null);
            packageType.setDescription(def.getDescription());
            packageType.setDisplayName(def.getDisplayName());
            packageType.setSupportsArchitecture(def.isSupportsArchitecture());
            packageType.setCreationData(false);
            packageType.setDeploymentConfigurationDefinition(ConfigurationMetadataParser.parse(def.getName(),
                def.getConfiguration()));
            packageType.setDiscoveryInterval(-1);
            packageType.setPackageExtraPropertiesDefinition(null);

            packageType = cm.persistServersidePackageType(packageType);
        }

        return packageType;
    }

    public AbstractPackageTypeBehavior<? extends ServerPluginComponent> getBehavior(int packageTypeId) throws Exception {
        return getBehavior(findByPackageTypeId(packageTypeId));
    }

    public AbstractPackageTypeBehavior<? extends ServerPluginComponent> getBehavior(String packageTypeName)
        throws Exception {
        return getBehavior(findByPackageTypeName(packageTypeName));
    }

    private AbstractPackageTypeBehavior<? extends ServerPluginComponent> getBehavior(ServerPluginEnvironment env)
        throws Exception {
        if (env == null) {
            return null;
        }

        PackageTypePluginDescriptorType descriptor = (PackageTypePluginDescriptorType) env.getPluginDescriptor();

        String behaviorClassName = descriptor.getBehaviorClass();

        @SuppressWarnings("unchecked")
        AbstractPackageTypeBehavior<ServerPluginComponent> ret = (AbstractPackageTypeBehavior<ServerPluginComponent>) instantiatePluginClass(
            env, behaviorClassName);

        ret.pluginComponent = getServerPluginComponent(env.getPluginKey().getPluginName());

        return ret;
    }

    private ServerPluginEnvironment findByPackageTypeId(int id) {
        for (Map.Entry<PackageType, ServerPluginEnvironment> entry : pluginsByPackageTypeId.entrySet()) {
            if (entry.getKey().getId() == id) {
                return entry.getValue();
            }
        }

        return null;
    }

    private ServerPluginEnvironment findByPackageTypeName(String name) {
        for (Map.Entry<PackageType, ServerPluginEnvironment> entry : pluginsByPackageTypeId.entrySet()) {
            if (entry.getKey().getName().equals(name)) {
                return entry.getValue();
            }
        }

        return null;
    }
}
