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

package org.rhq.enterprise.server.test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainerConfiguration;
import org.rhq.enterprise.server.plugin.pc.ServerPluginService;
import org.rhq.enterprise.server.plugin.pc.alert.AlertServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.bundle.BundleServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.content.ContentServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.content.PackageTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.entitlement.EntitlementServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.generic.GenericServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.perspective.PerspectiveServerPluginContainer;

/**
 * An MBean to use as a ServerPluginService for tests that actually don't care
 * about the server plugin services but need the server to have the server plugin
 * infrastructure started up.
 *
 * @author Lukas Krejci
 */
public class StandardServerPluginService extends ServerPluginService implements StandardServerPluginServiceMBean {

    private static final Log LOG = LogFactory.getLog(StandardServerPluginService.class);

    public static class TestMasterServerPluginContainer extends MasterServerPluginContainer {
        private List<AbstractTypeServerPluginContainer> serverPluginContainers = new ArrayList<AbstractTypeServerPluginContainer>();
        
        public TestMasterServerPluginContainer(
            List<Class<? extends AbstractTypeServerPluginContainer>> pluginContainerClasses) {
            
            for (Class<? extends AbstractTypeServerPluginContainer> cls : pluginContainerClasses) {
                try {
                    Constructor<? extends AbstractTypeServerPluginContainer> ctor = cls
                        .getConstructor(MasterServerPluginContainer.class);
                    AbstractTypeServerPluginContainer container = ctor.newInstance(this);
                    serverPluginContainers.add(container);
                } catch (Exception e) {
                    LOG.error("Failed to instantiate server plugin container class: " + cls.getName(), e);
                }
            }
        }
        
        @Override
        protected List<AbstractTypeServerPluginContainer> createPluginContainers() {
            return serverPluginContainers;
        }
    }

    // public so tests can directly set these
    public TestMasterServerPluginContainer master;
    public MasterServerPluginContainerConfiguration masterConfig;

    /**
     * This list contains all the standard server plugin container classes by default.
     * Modify it before the master plugin container is started, if you don't want 
     * some of these deployed or if you want to supply a custom one tailored for your test.
     */
    public List<Class<? extends AbstractTypeServerPluginContainer>> pluginContainerClasses;

    public StandardServerPluginService() {
        File dir = new File(System.getProperty("java.io.tmpdir"), "test-server-plugins");
        this.masterConfig = new MasterServerPluginContainerConfiguration(dir, dir, dir, null);
        pluginContainerClasses = new ArrayList<Class<? extends AbstractTypeServerPluginContainer>>();
        pluginContainerClasses.add(AlertServerPluginContainer.class);
        pluginContainerClasses.add(BundleServerPluginContainer.class);
        pluginContainerClasses.add(ContentServerPluginContainer.class);
        pluginContainerClasses.add(EntitlementServerPluginContainer.class);
        pluginContainerClasses.add(GenericServerPluginContainer.class);
        pluginContainerClasses.add(PackageTypeServerPluginContainer.class);
        pluginContainerClasses.add(PerspectiveServerPluginContainer.class);
    }

    @Override
    protected MasterServerPluginContainer createMasterPluginContainer() {
        this.master = new TestMasterServerPluginContainer(pluginContainerClasses);
        this.master.initialize(this.masterConfig);
        return this.master;
    }
}
