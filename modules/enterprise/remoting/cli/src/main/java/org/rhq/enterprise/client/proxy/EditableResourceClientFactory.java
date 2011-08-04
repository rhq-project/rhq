/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.client.proxy;

import java.util.List;

import javassist.util.proxy.MethodHandler;

import org.rhq.bindings.client.ResourceClientFactory;
import org.rhq.bindings.client.ResourceClientProxy;
import org.rhq.bindings.client.RhqFacade;
import org.rhq.enterprise.client.ClientMain;

/**
 * This is a specialization of the {@link ResourceClientFactory} class that provides the interactive
 * configuration editing features.
 *
 * @author Lukas Krejci
 */
public class EditableResourceClientFactory extends ResourceClientFactory {

    private ClientMain client;
    
    public EditableResourceClientFactory(ClientMain client) {
        super(client.getRemoteClient(), client.getPrintWriter());
        this.client = client;
    }

    @Override
    protected Class<?> getPluginConfigurableInterface() {
        return org.rhq.enterprise.client.proxy.EditableResourceClientProxy.EditablePluginConfigurable.class;
    }
    
    @Override
    protected Class<?> getResourceConfigurableInterface() {
        return org.rhq.enterprise.client.proxy.EditableResourceClientProxy.EditableResourceConfigurable.class;
    }
    
    @Override
    protected MethodHandler instantiateMethodHandler(ResourceClientProxy proxy, List<Class<?>> interfaces,
        RhqFacade remoteClient) {
        return new EditableResourceClientProxy.MethodHandler(proxy, remoteClient, client);
    }
}
