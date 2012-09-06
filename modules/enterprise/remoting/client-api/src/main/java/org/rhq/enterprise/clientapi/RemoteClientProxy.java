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
package org.rhq.enterprise.clientapi;

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bindings.client.AbstractRhqFacadeProxy;
import org.rhq.bindings.client.RhqManager;
import org.rhq.core.domain.server.ExternalizableStrategy;

/**
 * This class acts as a local SLSB proxy to make remote invocations
 * to SLSB Remotes over a remoting invoker.
 *
 * @author Greg Hinkle
 * @author Lukas Krejci
 */
public class RemoteClientProxy extends AbstractRhqFacadeProxy<RemoteClient> {

    private static final Log LOG = LogFactory.getLog(RemoteClientProxy.class);

    public RemoteClientProxy(RemoteClient client, RhqManager manager) {
        super(client, manager);
    }

    @Deprecated
    public Class<?> getRemoteInterface() {
        return this.getManager().remote();
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try {
            return super.invoke(proxy, method, args);
        } catch (Throwable t) {
            LOG.debug(t);
            throw t;
        }
    }

    protected Object doInvoke(Object proxy, Method originalMethod, Object[] args) throws Throwable  {
        ExternalizableStrategy.setStrategy(ExternalizableStrategy.Subsystem.REFLECTIVE_SERIALIZATION);

        return getRhqFacade().remoteInvoke(getManager(), originalMethod, Object.class, args);
    }
}
