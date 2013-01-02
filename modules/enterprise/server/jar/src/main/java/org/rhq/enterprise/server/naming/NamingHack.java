/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

package org.rhq.enterprise.server.naming;

import java.lang.reflect.Field;
import java.util.Hashtable;

import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Lukas Krejci
 */
public class NamingHack {

    private static final Log LOG = LogFactory.getLog(NamingHack.class);

    //in case this method is called multiple times, which can happen during the tests,
    //remember the builder encountered the first time...
    private static InitialContextFactoryBuilder originalBuilder;

    public static void bruteForceInitialContextFactoryBuilder() {
        LOG.info("Modifying the naming subsystem to enable secure execution of scripts inside the server...");

        //it is important to synchronize on the NamingManager class because the get/setInitialContextFactoryBuilder
        //methods in that class are static synchronized. By holding the lock on the NamingManager class we prevent
        //any other thread to check for the initial context factory builder (and therefore de-facto do any JNDI lookup).
        synchronized (NamingManager.class) {

            if (NamingManager.hasInitialContextFactoryBuilder()) {
                //if anyone ever changes the implementation of the NamingManager, we will break here...
                //but as of now, we have no other option but to do this and wait for 
                //https://issues.jboss.org/browse/AS7-6109 to be resolved and incorporated into our underlying container

                try {
                    Field f = NamingManager.class.getDeclaredField("initctx_factory_builder");
                    f.setAccessible(true);

                    if (originalBuilder == null) {
                        originalBuilder = (InitialContextFactoryBuilder) f.get(null);
                    }

                    f.set(null, null);
                } catch (Exception e) {
                    LOG.error(
                        "Failed to install a custom initial context factory builder. RHQ installation is unsecure!", e);

                    return;
                }
            }

            try {
                InitialContextFactory defaultFactory = null;
                if (originalBuilder != null) {
                    defaultFactory = originalBuilder
                        .createInitialContextFactory(new Hashtable<String, String>());
                }

                NamingManager.setInitialContextFactoryBuilder(new AccessCheckingInitialContextFactoryBuilder(
                    defaultFactory, originalBuilder == null));
            } catch (Exception e) {
                LOG.error("Failed to install a custom initial context factory builder. RHQ installation is unsecure!",
                    e);

                if (originalBuilder != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Trying to restore the original initial context factory builder: " + originalBuilder);
                    }

                    try {
                        NamingManager.setInitialContextFactoryBuilder(originalBuilder);
                    } catch (Exception e2) {
                        LOG.error("Failed to restore the original initial context factory builder. The JNDI lookup may"
                            + " not function properly from this point on...", e2);
                    }
                }
            }
        }
    }

}
