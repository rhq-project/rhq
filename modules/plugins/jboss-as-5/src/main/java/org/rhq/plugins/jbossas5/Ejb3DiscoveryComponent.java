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
package org.rhq.plugins.jbossas5;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.beans.metadata.plugins.AbstractBeanMetaData;
import org.jboss.deployers.spi.management.ManagementView;
import org.jboss.deployers.spi.management.KnownDeploymentTypes;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedDeployment;
import org.jboss.managed.api.ManagedObject;
import org.jboss.profileservice.spi.NoSuchDeploymentException;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;

/**
 * Discover EJB 3 beans
 * @author Heiko W. Rupp
 * @author Ian Springer
 */
public  class Ejb3DiscoveryComponent extends AbstractManagedDeploymentDiscoveryComponent
{
    private final Log log = LogFactory.getLog(this.getClass());

    @Override
    public Set<DiscoveredResourceDetails> discoverResources(
            ResourceDiscoveryContext<ProfileServiceComponent> discoveryContext)
    {
        Set<DiscoveredResourceDetails> discoveredResources = super.discoverResources(discoveryContext);
        Set<DiscoveredResourceDetails> resultingResources = new HashSet<DiscoveredResourceDetails>();
        ManagementView managementView =
                discoveryContext.getParentResourceComponent().getConnection().getManagementView();

        for (DiscoveredResourceDetails discoveredResource : discoveredResources)
        {
            try {
                ManagedDeployment managedDeployment = managementView.getDeployment(discoveredResource.getResourceKey());
                Map<String,ManagedObject> mosMap = managedDeployment.getManagedObjects();
                for (String key : mosMap.keySet()) {

                    // e.g. jboss.j2ee:jar=profileservice-secured.jar,name=SecureDeploymentManager,service=EJB3
                    if (key.startsWith("jboss.j2ee") && key.contains("service=EJB3")) {
                        ManagedObject mo = mosMap.get(key);
                        Object o = mo.getAttachment();
                        if (!(o instanceof AbstractBeanMetaData))
                            continue;
                        AbstractBeanMetaData meta = (AbstractBeanMetaData) o;

                        // check what type of bean we have
                        String bean = meta.getBean();
                        if (discoveryContext.getResourceType().getName().equals("EJB3 Session Bean")) {
                            if (!bean.equals("org.jboss.ejb3.stateless.StatelessContainer") &&
                                    !bean.equals("org.jboss.ejb3.stateful.StatefulContainer"))  // TODO check
                                continue;
                        } else if (discoveryContext.getResourceType().getName().equals("EJB3 Message Driven bean")) { // TODO check
                            if (!bean.equals("org.jboss.ejb3.message.MessageDrivenContainer"))  // TODO check
                                continue;
                        }


                        DiscoveredResourceDetails detail = new DiscoveredResourceDetails(discoveryContext.getResourceType(),
                                key,key,null,"EJB", null,null);
                        Configuration config = new Configuration();
                        config.put(new PropertySimple("managedObjectName", mo.getName()));
                        config.put(new PropertySimple(AbstractManagedDeploymentComponent.DEPLOYMENT_NAME_PROPERTY,discoveredResource.getResourceKey()));
                        config.put(new PropertySimple(AbstractManagedDeploymentComponent.DEPLOYMENT_TYPE_NAME_PROPERTY,
                                KnownDeploymentTypes.JavaEEEnterpriseBeans3x));
                        detail.setPluginConfiguration(config);
                        resultingResources.add(detail);
                        if(log.isDebugEnabled())
                            log.debug("Discovered : " + detail);

                    }
                }
            } catch (NoSuchDeploymentException e) {
                e.printStackTrace();  //TODO optimize
                continue;
            }

        }
        return resultingResources;
    }

    protected boolean accept(ManagedDeployment managedDeployment) {
        return true;  // We accept everything
    }

}