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

package org.rhq.enterprise.server.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.ejb.EJB;
import javax.ejb.Singleton;

import org.rhq.enterprise.server.cassandra.StorageClientManagerBean;
import org.rhq.enterprise.server.core.StartupBean;
import org.rhq.enterprise.server.naming.NamingHack;

/**
 * This is a replacement for the fullblown {@link StartupBean} of the actual RHQ server.
 * @author Lukas Krejci
 */
@Singleton
public class StrippedDownStartupBean {

    @EJB
    StorageClientManagerBean storageClientManager;

    private void secureNaming() {
        NamingHack.bruteForceInitialContextFactoryBuilder();
    }

    public void init() {
        secureNaming();
        // TODO Find a better way to load system properties
        // Cassandra connection info is currently obtained from system properties. I have
        // yet to find a good way to set system properties for the deployment under test.
        // https://github.com/arquillian/arquillian-showcase/tree/master/extensions/systemproperties
        // might be worth looking at.
        //
        // jsanda
        loadCassandraConnectionProps();

        storageClientManager.init();
    }

    public void loadCassandraConnectionProps() {
        InputStream stream = null;
        try {
            stream = getClass().getResourceAsStream("/cassandra-test.properties");
            Properties props = new Properties();
            props.load(stream);

            // DO NOT use System.setProperties(Properties). I previously tried that and it
            // caused some arquillian deployment exception.
            //
            // jsanda
            System.setProperty("rhq.cassandra.username", props.getProperty("rhq.cassandra.username"));
            System.setProperty("rhq.cassandra.password", props.getProperty("rhq.cassandra.password"));
            System.setProperty("rhq.cassandra.seeds", props.getProperty("rhq.cassandra.seeds"));
        } catch (IOException e) {
            throw new RuntimeException(("Failed to load cassandra-test.properties"));
        }
    }
}
