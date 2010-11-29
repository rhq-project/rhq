/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;

import org.rhq.enterprise.gui.coregui.client.util.rpc.TrackingRemoteServiceProxy;

/**
 * Creates a customized client-side proxy for a {@link RemoteService} interface.
 * 
 * @see TrackingRemoteServiceProxy
 * @author Joseph Marques
 */
public class TrackingProxyCreator extends ProxyCreator {

    public TrackingProxyCreator(JClassType type) {
        super(type);
    }

    @Override
    protected Class<? extends RemoteServiceProxy> getProxySupertype() {
        return TrackingRemoteServiceProxy.class;
    }

}
