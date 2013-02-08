/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.server.core.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.server.Services;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * This will provide our co-located management service.
 * This is useful because it helps us avoid performing any management
 * over a remote connector. Since we know we are running co-located
 * in the same app server container that we want to manage, this 
 * service will provide us with a management client that does not go
 * over a remote connector to talk to our app server container.
 *
 * @author John Mazzitelli
 */
public class ManagementService implements ServiceActivator {

    private static volatile ModelController controller;
    private static volatile ExecutorService executor;

    /**
     * The caller should call ModelControllerClient.close() when finished with the client.
     * 
     * @return the ModelControllerClient
     */
    public static ModelControllerClient getClient() {
        return controller.createClient(executor);
    }

    @Override
    public void activate(ServiceActivatorContext context) throws ServiceRegistryException {
        final GetModelControllerService service = new GetModelControllerService();
        context.getServiceTarget()
            .addService(ServiceName.of("rhq", "server", "management", "client", "getter"), service)
            .addDependency(Services.JBOSS_SERVER_CONTROLLER, ModelController.class, service.modelControllerValue)
            .install();
    }

    private class GetModelControllerService implements Service<Void> {
        private InjectedValue<ModelController> modelControllerValue = new InjectedValue<ModelController>();

        @Override
        public Void getValue() throws IllegalStateException, IllegalArgumentException {
            return null;
        }

        @Override
        public void start(StartContext context) throws StartException {
            ManagementService.executor = Executors.newFixedThreadPool(5, new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setDaemon(true);
                    t.setName("ManagementServiceModelControllerClientThread");
                    return t;
                }
            });
            ManagementService.controller = modelControllerValue.getValue();
        }

        @Override
        public void stop(StopContext context) {
            try {
                ManagementService.executor.shutdownNow();
            } finally {
                ManagementService.executor = null;
                ManagementService.controller = null;
            }
        }

    }
}
