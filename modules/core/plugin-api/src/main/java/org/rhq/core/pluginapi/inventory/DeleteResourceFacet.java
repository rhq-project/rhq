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
package org.rhq.core.pluginapi.inventory;

/**
 * Components that implement this facet are responsible for deleting resources managed by the component.
 *
 * @author Jason Dobies
 */
public interface DeleteResourceFacet {
    /**
     * Indicates the resource has been deleted. It is at the discretion of the plugin as to what to do when a resource
     * has been deleted. For instance, when deleting a service that is running inside a server, the plugin needs to
     * remove things belonging to that service and the configuration of the owning server must reflect the deletion of
     * the service.
     *
     * @throws Exception if failed to delete the resource
     */
    void deleteResource() throws Exception;
}