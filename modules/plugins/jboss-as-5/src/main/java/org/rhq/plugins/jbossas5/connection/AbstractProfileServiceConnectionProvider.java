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
package org.rhq.plugins.jbossas5.connection;

import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Ian Springer
 */
public abstract class AbstractProfileServiceConnectionProvider implements ProfileServiceConnectionProvider
{
    private final Log log = LogFactory.getLog(this.getClass());
    
    private ProfileServiceConnectionImpl existingConnection;
    private boolean connected;

    public final ProfileServiceConnection connect()
    {
        ProfileServiceConnectionImpl connection = doConnect();
        this.connected = true;
        if (this.existingConnection == null)
            this.existingConnection = connection;
        return this.existingConnection;
    }

    protected abstract ProfileServiceConnectionImpl doConnect();

    public boolean isConnected()
    {
        return this.connected;
    }

    public final void disconnect() {
        this.connected = false;
        doDisconnect();
    }

    protected abstract void doDisconnect();

    public ProfileServiceConnection getExistingConnection()
    {
        return this.existingConnection;
    }

    protected InitialContext createInitialContext(Properties env)
    {
        InitialContext initialContext;
        this.log.debug("Creating JNDI InitialContext with env [" + env + "]...");              
        try
        {
            initialContext = new InitialContext(env);
        }
        catch (NamingException e)
        {
            throw new RuntimeException("Failed to create JNDI InitialContext.", e);
        }
        this.log.debug("Created JNDI InitialContext [" + initialContext + "].");
        return initialContext;
    }

    protected Object lookup(InitialContext initialContext, String name)
    {
        try
        {
            return initialContext.lookup(name);
        }
        catch (NamingException e)
        {
            throw new RuntimeException("Failed to lookup JNDI name '" + name + "' from InitialContext.", e);
        }
    }
}
