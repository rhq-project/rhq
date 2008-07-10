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
package org.rhq.enterprise.server.resource;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * @author Greg Hinkle
 */

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService
public interface ResourceManagerRemote {
    Resource getResourceById(Subject user, int id);

    /**
     * This finder query can be used to find resources with various combinations of attributes in their composite form.
     * Except for the user parameter, the other parameters can be left null so that the query will not filter by that
     * attribute.
     *
     * @param  subject An authenticated system user.
     * @param  category       Limit the search to a given {@link ResourceCategory}
     * @param  type           Limit the search to to a given {@link ResourceType}
     * @param  parentResource Limit the search to children of a given parent resource
     * @param  searchString A SQL <i>like</i> expression that is used to search the name and description.
     * @param  pageControl  Controls the <i>paging</i> of the items returned.
     *
     * @return The requested list of pages.
     */
    PageList<ResourceComposite> findResourceComposites(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "category")
    ResourceCategory category, @WebParam(name = "typeName")
    String typeName, @WebParam(name = "parentResourceId")
    int parentResourceId, @WebParam(name = "searchString")
    String searchString, @WebParam(name = "pageControl")
    PageControl pageControl);

    /**
     * Loads an entire resource sub-tree from the database. The resources themselves are loaded with the default eager
     * loading except that the child resources will be loaded, as well as the parent of the resource with the given ID.
     * So take note that only the root resource will have its parent resource loaded, the child parents can all be
     * determined by the caller by walking the tree.
     *
     * <p><b>Warning!</b>This may be very expensive and return a very large set of results.</p>
     *
     * @param  rootResourceId the root resourceId to be fetched
     *
     * @return a preloaded resource tree
     */
    Resource getResourceTree(@WebParam(name = "rootResourceId") int rootResourceId,
                             @WebParam(name = "includeDescendants") boolean includeDescendants);

    /**
     * Get a list of resources by the specified category.
     * @param subject An authenticated system user.
     * @param category A resource category used to restrict the result set.
     * @param inventoryStatus The inventory status used to restrict the result set.
     * @param pageControl
     * @return Controls the <i>paging</i> of the items returned.
     */
    PageList<Resource> getResourcesByCategory(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "category")
    ResourceCategory category, @WebParam(name = "inventoryStatus")
    InventoryStatus inventoryStatus, @WebParam(name = "pageControl")
    PageControl pageControl);
}