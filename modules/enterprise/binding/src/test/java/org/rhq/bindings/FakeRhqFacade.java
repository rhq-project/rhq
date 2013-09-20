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

package org.rhq.bindings;

import java.util.Collections;
import java.util.Map;

import org.rhq.bindings.client.AbstractRhqFacade;
import org.rhq.bindings.client.RhqFacade;
import org.rhq.bindings.client.RhqManager;
import org.rhq.core.domain.auth.Subject;

public class FakeRhqFacade extends AbstractRhqFacade {

    public Subject getSubject() {
        return null;
    }

    public Subject login(String user, String password) throws Exception {
        return null;
    }

    public void logout() {

    }

    public boolean isLoggedIn() {
        return false;
    }
    
    public Map<RhqManager, Object> getScriptingAPI() {
        return Collections.emptyMap();
    }

    @Override
    public <T> T getProxy(Class<T> remoteApiIface) {
        return null;
    }
}
