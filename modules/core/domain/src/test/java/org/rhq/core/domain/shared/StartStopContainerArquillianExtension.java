/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.core.domain.shared;

import java.io.File;

import org.jboss.arquillian.container.spi.Container;
import org.jboss.arquillian.container.spi.Container.State;
import org.jboss.arquillian.container.spi.ContainerRegistry;
import org.jboss.arquillian.container.spi.event.container.AfterUnDeploy;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.event.enrichment.BeforeEnrichment;
import org.jboss.arquillian.test.spi.event.suite.AfterSuite;

/**
 * @author Jay Shaughnessy
 */
public class StartStopContainerArquillianExtension implements LoadableExtension {

    // Not actually using these extensions at the moment, but keeping around as an example.
    // Just commenting out the Observer registrations...

    @Override
    public void register(ExtensionBuilder builder) {
        //System.out.println("*** IN REGISTER ");
        //builder.observer(StartCustomContainers.class);
        //builder.observer(CloseCustomContainers.class);
        //builder.observer(RemoveDeployMarker.class);
    }

    public static class StartCustomContainers {
        public void start(@Observes
        BeforeEnrichment event, ContainerRegistry registry) {
            System.out.println("*** IN START ");
            Container c = registry.getContainer("RHQAS7");
            try {
                if (!State.STARTED.equals(c.getState())) {
                    System.out.println("*** STARTING ");
                    c.start();
                } else {
                    System.out.println("*** SKIP START ");

                }
            } catch (Exception e) {
                System.err.println("Could not start custom container " + c.getName());
                e.printStackTrace();
            }
        }
    }

    public static class CloseCustomContainers {
        public void stop(@Observes
        AfterSuite event, ContainerRegistry registry) {
            System.out.println("*** IN STOP ");
            Container c = registry.getContainer("RHQAS7");
            try {
                if (State.STARTED.equals(c.getState())) {
                    System.out.println("*** STOPPING ");
                    c.stop();
                }
            } catch (Exception e) {
                System.err.println("Could not stop custom container " + c.getName());
                e.printStackTrace();
            }
        }
    }

    public static class RemoveDeployMarker {
        public void stop(@Observes
        AfterUnDeploy event, ContainerRegistry registry) {
            System.out.println("*** IN REMOVE MARKER ");
            try {
                File marker = new File("target/test-domain-deployment.done");
                if (marker.delete()) {
                    System.out.println("*** REMOVED MARKER ");

                } else {
                    System.out.println("*** SKIP, NO MARKER ");
                }
            } catch (Exception e) {
                System.err.println("Could not remove deployment marker " + e);
                e.printStackTrace();
            }
        }
    }

}
