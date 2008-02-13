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
     * Find ResourceComposite helper objects. Useful for listing Resources by type.
     *
     * @param  subject
     * @param  category
     * @param  typeName
     * @param  parentResourceId
     * @param  searchString
     * @param  pageControl
     *
     * @return PageList<ResourceComposite> of any matching resources
     */
    PageList<ResourceComposite> findResourceComposites(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "category")
    ResourceCategory category, @WebParam(name = "typeName")
    String typeName, @WebParam(name = "parentResourceId")
    int parentResourceId, @WebParam(name = "searchString")
    String searchString, @WebParam(name = "pageControl")
    PageControl pageControl);

    Resource getResourceTree(@WebParam(name = "rootResourceId")
    int rootResourceId);

    PageList<Resource> getResourcesByCategory(@WebParam(name = "subject")
    Subject subject, @WebParam(name = "category")
    ResourceCategory category, @WebParam(name = "inventoryStatus")
    InventoryStatus inventoryStatus, @WebParam(name = "pageControl")
    PageControl pageControl);
}