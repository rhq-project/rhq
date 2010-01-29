/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.server.inventory.resource;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceGWTService;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.HibernateDetachUtility;
import org.rhq.enterprise.server.util.LookupUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * @author Greg Hinkle
 */
public class ResourceGWTServiceImpl extends RemoteServiceServlet implements ResourceGWTService {

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        System.out.println("Loading GWT RPC Services");
    }

    public PageList<Resource> findResourcesByCriteria(ResourceCriteria criteria) {
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        try {
            PageList<Resource> result = resourceManager.findResourcesByCriteria(subjectManager.getOverlord(), criteria);
            HibernateDetachUtility.nullOutUninitializedFields(result,
                    HibernateDetachUtility.SerializationType.SERIALIZATION);
            return result;
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException(e);
        }
    }


}