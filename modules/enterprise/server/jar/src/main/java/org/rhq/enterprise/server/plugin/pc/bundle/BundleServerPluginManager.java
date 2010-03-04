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
package org.rhq.enterprise.server.plugin.pc.bundle;

import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginManager;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.bundle.BundlePluginDescriptorType;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.bundle.BundleType;

/**
 * This loads in all bundle server plugins that can be found. You can obtain a loaded plugin's
 * {@link ServerPluginEnvironment environment}, including its classloader, from this object as well.
 *
 * @author John Mazzitelli
 */
public class BundleServerPluginManager extends ServerPluginManager {
    public BundleServerPluginManager(BundleServerPluginContainer pc) {
        super(pc);
    }

    // TODO override methods like initialize, shutdown, loadPlugin, etc. for custom bundle functionality

    @Override
    public void initialize() throws Exception {
        super.initialize();
    }

    @Override
    protected void loadPlugin(ServerPluginEnvironment env, boolean enabled) throws Exception {
        if (enabled) {
            // validate some things about this plugin that are specific for bundle functionality

            BundlePluginDescriptorType descriptor = (BundlePluginDescriptorType) env.getPluginDescriptor();
            BundleType bt = descriptor.getBundle();
            if (bt == null || bt.getType() == null || bt.getType().length() == 0) {
                // if the xml parser did its job, this will probably never happen, but just in case, make sure there is
                // a non-null, valid bundle type name - we have other code that expects this to be true
                throw new Exception("The bundle plugin [" + env.getPluginKey().getPluginName()
                    + "] did not specify a valid bundle type in its descriptor");
            }

            ServerPluginComponent component = createServerPluginComponent(env);
            if (!(component instanceof BundleServerPluginFacet)) {
                throw new Exception("The bundle plugin [" + env.getPluginKey().getPluginName()
                    + "] has an invalid component [" + component + "]. It does not implement ["
                    + BundleServerPluginFacet.class + "]");
            }
        }

        super.loadPlugin(env, enabled);
    }

    /**
     * Given the {@link BundleType#getName() name of a bundle type}, this will return the stateful plugin component
     * that manages bundles of that type.
     * 
     * @param bundleTypeName
     *
     * @return the plugin component object that will manage bundles of the named bundle type; <code>null</code> if there is no plugin
     *         that can support the given bundle type
     */
    public BundleServerPluginFacet getBundleServerPluginFacet(String bundleTypeName) {
        if (bundleTypeName == null) {
            throw new IllegalArgumentException("bundleTypeName == null");
        }

        for (ServerPluginEnvironment env : getPluginEnvironments()) {
            BundlePluginDescriptorType descriptor = (BundlePluginDescriptorType) env.getPluginDescriptor();
            if (bundleTypeName.equals(descriptor.getBundle().getType())) {
                ServerPluginComponent component = getServerPluginComponent(env.getPluginKey().getPluginName());
                // we know this cast will work because our loadPlugin ensured that this component implements this interface
                return (BundleServerPluginFacet) component;
            }
        }

        return null;
    }
}