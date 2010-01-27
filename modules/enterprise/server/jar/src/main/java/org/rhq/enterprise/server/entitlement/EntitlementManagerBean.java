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
package org.rhq.enterprise.server.entitlement;

import java.util.Collection;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginEnvironment;
import org.rhq.enterprise.server.plugin.pc.ServerPluginServiceManagement;
import org.rhq.enterprise.server.plugin.pc.entitlement.EntitlementServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.entitlement.EntitlementServerPluginManager;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * SLSB that provides the interface point to the entitlement subsystem and entitlement server side plugins.
 */
@Stateless
public class EntitlementManagerBean implements EntitlementManagerLocal {
    private final Log log = LogFactory.getLog(EntitlementManagerBean.class.getName());

    // TODO: might not even need this if this SLSB never needs to access the database
    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    public void doSomething() {
        // TODO do something "entitlement-y"
        // you can call into the entitlement plugin container to do things that require the entitlement plugin to work
        // you can have the plugin container have any API you can and it interacts with the plugins
        try {
            Collection<ServerPluginEnvironment> envs = getEntitlementServerPluginManager().getPluginEnvironments();
            for (ServerPluginEnvironment env : envs) {
                PluginKey pluginKey = env.getPluginKey();
                log.info("Deployed Entitlement plugin: " + pluginKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * This will return the entitlement server plugin manager. If it is not available, an exception
     * is thrown.
     * 
     * The returned object has information available on all loaded entitlement server plugins.
     * 
     * @return the plugin manager that can be used to look up entitlement plugins and their environments.
     * @throws Exception if the plugin manager is not available (usually because the master plugin container
     *                   hasn't been started)
     */
    private EntitlementServerPluginManager getEntitlementServerPluginManager() throws Exception {
        EntitlementServerPluginManager pluginMgr;
        pluginMgr = (EntitlementServerPluginManager) getEntitlementServerPluginContainer().getPluginManager();
        if (pluginMgr == null) {
            throw new Exception("Entitlement server plugin manager does not exist");
        }
        return pluginMgr;
    }

    /**
     * This will return the entitlement server plugin container. If it is not available, an exception
     * is thrown.
     * 
     * The returned object has the plugin manager that manages all entitlement server plugins.
     * 
     * @return the plugin container that hosts all entitlement server plugins
     * @throws Exception if the plugin container is not available (usually because the master plugin container
     *                   hasn't been started)
     */
    private EntitlementServerPluginContainer getEntitlementServerPluginContainer() throws Exception {
        MasterServerPluginContainer master = getMasterServerPluginContainer();
        EntitlementServerPluginContainer pc = master.getPluginContainerByClass(EntitlementServerPluginContainer.class);
        if (pc == null) {
            throw new Exception("Entitlement server plugin container does not exist");
        }
        return pc;
    }

    /**
     * This will return the master server plugin container. If the master has not been started, an exception
     * is thrown.
     * 
     * @return the master plugin container that hosts all server plugin containers
     * @throws Exception if the master plugin container is not started
     */
    private MasterServerPluginContainer getMasterServerPluginContainer() throws Exception {
        ServerPluginServiceManagement serverPluginService = LookupUtil.getServerPluginService();
        MasterServerPluginContainer master = serverPluginService.getMasterPluginContainer();
        if (master == null) {
            throw new Exception("Master plugin container has not be started");
        }
        return master;
    }
}