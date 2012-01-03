package org.rhq.jndi.test;
import java.util.Properties;

import javax.naming.CompoundName;

import org.jnp.server.Main;
import org.jnp.server.NamingBeanImpl;

import org.jboss.logging.Logger;

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

/**
 * 
 *
 * @author Lukas Krejci
 */
public class Server {
    private static final Logger LOG = Logger.getLogger(Server.class);
    
    private static Server INSTANCE;
    
    private Main jnpServer;
    
    public static void main(String[] args) throws Exception {
        LOG.debug("System properties: " + System.getProperties());
        Server.start();
    }
    
    private Server() {
        jnpServer = new Main("org.rhq.jndi.access.test.server");
    }
    
    public static synchronized Server getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Server();
        }
        
        return INSTANCE;
    }
    
    public static void start() throws Exception {
        LOG.debug("Initializing the JNP server");
        
        NamingBeanImpl nbi = new NamingBeanImpl();
        getInstance().jnpServer.setNamingInfo(nbi);
        nbi.start();
        
        LOG.debug("Binding kachny");
        
        nbi.getNamingInstance().bind(new CompoundName("kachny", new Properties()), "KACHNY!", String.class.getName());
        
        LOG.debug("Starting the JNP server");
        
        getInstance().jnpServer.start();
    }
    
    public static void stop() {
        LOG.debug("Stopping the JNP server");
        getInstance().jnpServer.stop();
    }
}
