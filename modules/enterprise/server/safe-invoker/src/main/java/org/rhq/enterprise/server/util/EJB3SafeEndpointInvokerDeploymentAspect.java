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
package org.rhq.enterprise.server.util;

import java.util.Iterator;
import org.jboss.ws.core.server.ServiceEndpointInvoker;
import org.jboss.ws.core.server.ServiceEndpointInvokerEJB21;
import org.jboss.wsf.spi.deployment.Deployment;
import org.jboss.wsf.spi.deployment.DeploymentAspect;
import org.jboss.wsf.spi.deployment.Endpoint;

/**
 * This class installs the EJB3SafeEndpointInvoker into the deployment aspect for the endpoint.
 *
 * @author Greg Hinkle
 */
public class EJB3SafeEndpointInvokerDeploymentAspect extends DeploymentAspect {
    @Override
    public void create(Deployment dep) {
        Iterator i$ = dep.getService().getEndpoints().iterator();
        do {
            if (!i$.hasNext()) {
                break;
            }

            Endpoint ep = (Endpoint) i$.next();
            ServiceEndpointInvoker epInvoker = ep.getAttachment(ServiceEndpointInvoker.class);
            if (epInvoker == null) {
                org.jboss.wsf.spi.deployment.Deployment.DeploymentType depType = ep.getService().getDeployment()
                    .getType();
                if (depType == org.jboss.wsf.spi.deployment.Deployment.DeploymentType.JAXRPC_EJB21) {
                    epInvoker = new ServiceEndpointInvokerEJB21();
                } else {
                    epInvoker = new EJB3SafeEndpointInvoker();
                }

                ep.addAttachment(org.jboss.ws.core.server.ServiceEndpointInvoker.class, epInvoker);
                epInvoker.init(ep);
            }
        } while (true);
    }
}