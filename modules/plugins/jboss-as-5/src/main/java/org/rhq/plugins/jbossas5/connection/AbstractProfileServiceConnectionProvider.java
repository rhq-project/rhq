/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.plugins.jbossas5.connection;

import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Ian Springer
 */
public abstract class AbstractProfileServiceConnectionProvider implements ProfileServiceConnectionProvider {
    private final Log log = LogFactory.getLog(this.getClass());

    private ProfileServiceConnection existingConnection;

    public final ProfileServiceConnection connect() {
        this.existingConnection = doConnect();
        this.existingConnection.init();
        return this.existingConnection;
    }

    protected abstract ProfileServiceConnection doConnect();

    public boolean isConnected() {
        // TODO: Ping the connection to make sure it's not defunct?
        return (this.existingConnection != null);
    }

    public final void disconnect() {
        if (isConnected()) {
            this.existingConnection = null;
            doDisconnect();
        }
    }

    protected abstract void doDisconnect();

    public ProfileServiceConnection getExistingConnection() {
        return this.existingConnection;
    }

    protected InitialContext createInitialContext(Properties env) {
        InitialContext initialContext;
        this.log.debug("Creating JNDI InitialContext with env [" + env + "]...");
        try {
            initialContext = new InitialContext(env);
        } catch (NamingException e) {
            throw new RuntimeException("Failed to create JNDI InitialContext.", e);
        }
        this.log.debug("Created JNDI InitialContext [" + initialContext + "].");
        return initialContext;
    }

    protected Object lookup(InitialContext initialContext, String name) {
        try {
            return initialContext.lookup(name);
        } catch (NamingException e) {
            throw new RuntimeException("Failed to lookup JNDI name '" + name + "' from InitialContext.", e);
        }
    }
}
