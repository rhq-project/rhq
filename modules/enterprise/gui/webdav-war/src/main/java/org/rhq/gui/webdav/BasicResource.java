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
package org.rhq.gui.webdav;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Request;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Represents a basic managed resource. Provides some hooks to the authentication subsystem to
 * ensure the user is allowed to see the resource.
 * 
 * @see ResourceFolder
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public abstract class BasicResource extends Authenticator implements PropFindableResource {

    private static final Log LOG = LogFactory.getLog(BasicResource.class);

    private final Resource managedResource;

    public BasicResource(Subject subject, Resource managedResource) {
        super(subject);

        if (managedResource == null) {
            throw new NullPointerException("managedResource == null");
        }
        this.managedResource = managedResource;
    }

    protected Resource getManagedResource() {
        return this.managedResource;
    }

    /**
     * This implementation of authorise simply assures that the user has authenticated himself
     * and the user is allowed to view the managed resource. Subclasses may wish to further
     * restrict the authorization (e.g. deny access if the configuration of the resource
     * is to be edited but the user is not allowed to change the configuration).
     */
    public boolean authorise(Request request, Request.Method method, Auth auth) {
        boolean authorized = false;

        if (auth != null) {
            Subject subject = (Subject) auth.getTag();
            if (subject != null) {
                AuthorizationManagerLocal authorizationManager = LookupUtil.getAuthorizationManager();
                authorized = authorizationManager.canViewResource(subject, getManagedResource().getId());
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("BasicResource [" + getManagedResource() + "], auth=[" + auth.getTag() + "], isAuthorized="
                    + authorized);
            }
        }

        return authorized;
    }

    public String checkRedirect(Request request) {
        return null; // no-op
    }
}
