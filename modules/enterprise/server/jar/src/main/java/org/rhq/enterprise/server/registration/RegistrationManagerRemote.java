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
package org.rhq.enterprise.server.registration;

import javax.ejb.Remote;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;

@Remote
public interface RegistrationManagerRemote {
    /**
     * Register the platform in the inventory
     * @param user
     * @param resource
     */
    void registerPlatform(Subject user, Resource resource, int parentId);

    /**
     * Import the inventoried resource as a managed resource object
     * @param user
     * @param resourceId
     */
    void importPlatform(Subject user, Resource resource);

    /**
     * subscribe a platform to a compatible baserepo
     * @param user
     * @param platform
     * @param release
     * @param arch
     */
    void subscribePlatformToBaseRepo(Subject user, Resource platform, String release, String version, String arch)
        throws RegistrationException;

}
