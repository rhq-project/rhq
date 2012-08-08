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

package org.rhq.bindings.client;

import java.util.Map;

import org.rhq.core.domain.auth.Subject;

/**
 * This is an interface through which the script can communicate with RHQ server.
 *
 * @author Lukas Krejci
 */
public interface RhqFacade {

    /**
     * @return the user the facade is authenticated as
     */
    Subject getSubject();

    Subject login(String user, String password) throws Exception;

    void logout();

    boolean isLoggedIn();

    /**
     * This map is constructed using all the elements in the {@link RhqManager} enum which are then proxied
     * using this instance.
     * 
     * @return a map of all available proxied managers keyed by their names.
     */
    Map<RhqManager, Object> getScriptingAPI();

    /**
     * Unlike the {@link #getScriptingAPI()} method that returns objects with modified signatures
     * meant to be used by the scripting environment, this method provides the access to the "raw"
     * remote API interface implementation backed by this RHQ facade implementation.
     * 
     * @param remoteApiIface one of the RHQ's remote API interfaces of which the proxied instance
     * should be returned
     * @return the proxy of the remote API interface backed by this facade
     */
    <T> T getProxy(Class<T> remoteApiIface);
}
