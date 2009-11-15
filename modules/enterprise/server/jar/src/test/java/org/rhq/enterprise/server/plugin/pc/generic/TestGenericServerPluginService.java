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
package org.rhq.enterprise.server.plugin.pc.generic;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rhq.enterprise.server.plugin.pc.AbstractTypeServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainerConfiguration;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginLifecycleListener;
import org.rhq.enterprise.server.plugin.pc.ServerPluginManager;
import org.rhq.enterprise.server.plugin.pc.ServerPluginService;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * Used as a mock service for the generic server plugin container.
 */
public class TestGenericServerPluginService extends ServerPluginService implements TestGenericServerPluginServiceMBean {
    public enum State {
        INITIALIZED, STARTED, STOPPED, UNINITIALIZED
    }

    // public so tests can directly set these
    public TestMasterServerPluginContainer master;
    public TestGenericServerPluginContainer genericPC;
    public MasterServerPluginContainerConfiguration masterConfig;

    public TestGenericServerPluginService() {
        // build the config at constructor time so tests have it even before the PC is initialized
        File dir = new File(System.getProperty("java.io.tmpdir"), "test-server-plugins");
        this.masterConfig = new MasterServerPluginContainerConfiguration(dir, dir, dir, null);
    }

    @Override
    public MasterServerPluginContainer createMasterPluginContainer() {
        this.master = new TestMasterServerPluginContainer();
        this.master.initialize(this.masterConfig);
        return this.master;
    }

    /**
     * The test master PC
     */
    class TestMasterServerPluginContainer extends MasterServerPluginContainer {
        @Override
        protected List<AbstractTypeServerPluginContainer> createPluginContainers() {
            ArrayList<AbstractTypeServerPluginContainer> pcs = new ArrayList<AbstractTypeServerPluginContainer>(1);
            genericPC = new TestGenericServerPluginContainer(this);
            pcs.add(genericPC);
            return pcs;
        }

        @Override
        protected ClassLoader createRootServerPluginClassLoader() {
            return this.getClass().getClassLoader();
        }

        @Override
        protected Map<URL, ? extends ServerPluginDescriptorType> preloadAllPlugins() throws Exception {
            // if our test never setup any plugins, ignore it and just return an empty map
            File pluginDir = getConfiguration().getPluginDirectory();
            if (pluginDir == null || pluginDir.listFiles() == null || pluginDir.listFiles().length == 0) {
                return new HashMap<URL, ServerPluginDescriptorType>();
            } else {
                return super.preloadAllPlugins();
            }
        }
    }

    /**
     * The test generic PC.
     */
    class TestGenericServerPluginContainer extends GenericServerPluginContainer {
        public State state = State.UNINITIALIZED;

        public TestGenericServerPluginContainer(MasterServerPluginContainer master) {
            super(master);
        }

        @Override
        protected ServerPluginManager createPluginManager() {
            TestGenericPluginManager pm = new TestGenericPluginManager(this);
            return pm;
        }

        @Override
        public synchronized void initialize() throws Exception {
            if (state == State.UNINITIALIZED) {
                state = State.INITIALIZED;
            } else {
                System.out.println("!!! PC LIFECYCLE WAS BAD - THIS IS A BUG !!!");
                throw new IllegalStateException("not uninitialized: " + state);
            }
            super.initialize();
        }

        @Override
        public synchronized void start() {
            if (state == State.INITIALIZED) {
                state = State.STARTED;
            } else {
                System.out.println("!!! PC LIFECYCLE WAS BAD - THIS IS A BUG !!!");
                throw new IllegalStateException("not initialized: " + state);
            }
            super.start();
        }

        @Override
        public synchronized void stop() {
            if (state == State.STARTED) {
                state = State.STOPPED;
            } else {
                System.out.println("!!! PC LIFECYCLE WAS BAD - THIS IS A BUG !!!");
                throw new IllegalStateException("not started: " + state);
            }
            super.stop();
        }

        @Override
        public synchronized void shutdown() {
            if (state == State.STOPPED) {
                state = State.UNINITIALIZED;
            } else {
                System.out.println("!!! PC LIFECYCLE WAS BAD - THIS IS A BUG !!!");
                throw new IllegalStateException("not stopped: " + state);
            }
            super.shutdown();
        }
    }

    /**
     * The test plugin manager.
     */
    class TestGenericPluginManager extends ServerPluginManager {
        public final Map<String, ServerPluginLifecycleListener> listeners;

        public TestGenericPluginManager(TestGenericServerPluginContainer pc) {
            super(pc);
            listeners = new HashMap<String, ServerPluginLifecycleListener>();
        }

        @Override
        protected ServerPluginLifecycleListener createServerPluginComponent(ServerPluginEnvironment environment)
            throws Exception {
            ServerPluginLifecycleListener listener = super.createServerPluginComponent(environment);
            listeners.put(environment.getPluginName(), listener);
            return listener;
        }
    }
}