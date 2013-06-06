/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.enterprise.startup.deployment;

import static org.jboss.msc.service.ServiceBuilder.DependencyType.REQUIRED;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;

/**
 * A DUP which makes the ShutdownListener session bean automatically depend on all other sessions beans.
 *
 * @author Thomas Segismont
 */
public class RhqShutdownBeanDependenciesProcessor implements DeploymentUnitProcessor {

    public static final String SHUTDOWN_LISTENER_CLASS_NAME = "org.rhq.enterprise.server.core.ShutdownListener";

    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = context.getDeploymentUnit();
        EEModuleDescription moduleDescription = unit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        Collection<ComponentDescription> componentDescriptions = moduleDescription.getComponentDescriptions();

        if (componentDescriptions == null || componentDescriptions.isEmpty()
            || !RhqDeploymentMarker.isRhqDeployment(unit)) {
            // Only process sub deployments of the RHQ EAR
            return;
        }

        Collection<SessionBeanComponentDescription> sessionBeanComponentDescriptions = getSessionBeanComponentDescriptions(componentDescriptions);

        SessionBeanComponentDescription shutdownBeanComponentDescription = extractShutdownBeanDescription(sessionBeanComponentDescriptions);

        for (SessionBeanComponentDescription sessionBeanComponentDescription : sessionBeanComponentDescriptions) {
            shutdownBeanComponentDescription.addDependency(sessionBeanComponentDescription.getStartServiceName(),
                REQUIRED);
        }
    }

    private Collection<SessionBeanComponentDescription> getSessionBeanComponentDescriptions(
        Collection<ComponentDescription> componentDescriptions) {
        Collection<SessionBeanComponentDescription> sessionBeanComponentDescriptions = new LinkedList<SessionBeanComponentDescription>();
        for (ComponentDescription componentDescription : componentDescriptions) {
            if (componentDescription instanceof SessionBeanComponentDescription) {
                sessionBeanComponentDescriptions.add((SessionBeanComponentDescription) componentDescription);
            }
        }
        return sessionBeanComponentDescriptions;
    }

    private SessionBeanComponentDescription extractShutdownBeanDescription(
        Collection<SessionBeanComponentDescription> sessionBeanComponentDescriptions) {
        for (Iterator<SessionBeanComponentDescription> iterator = sessionBeanComponentDescriptions.iterator(); iterator
            .hasNext();) {
            SessionBeanComponentDescription sessionBeanComponentDescription = iterator.next();
            if (sessionBeanComponentDescription.getComponentClassName().equals(SHUTDOWN_LISTENER_CLASS_NAME)) {
                iterator.remove();
                return sessionBeanComponentDescription;
            }
        }
        return null;
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
