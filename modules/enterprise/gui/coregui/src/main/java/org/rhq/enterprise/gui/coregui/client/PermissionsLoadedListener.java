/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
package org.rhq.enterprise.gui.coregui.client;

import java.util.Set;

import org.rhq.core.domain.authz.Permission;

/**
 * This is called by the {@link PermissionsLoader} when a set of permissions have been loaded
 * or a failure occurred and the permissions failed to load.
 * 
 * @author Ian Springer
 * @author John Mazzitelli
 */
public interface PermissionsLoadedListener {

    /**
     * The callback method that is called when the {@link PermissionsLoader} finished
     * its remote call. If the call was successful, the given permissions argument will
     * be non-null (but may be empty). If an error occurred that failed to load the permissions,
     * <code>null</code> will be passed in as the value.
     *  
     * @param permissions the permissions if successfully loaded; <code>null</code> if an error
     *                    occurred and the permissions are unknown
     */
    void onPermissionsLoaded(Set<Permission> permissions);
}
