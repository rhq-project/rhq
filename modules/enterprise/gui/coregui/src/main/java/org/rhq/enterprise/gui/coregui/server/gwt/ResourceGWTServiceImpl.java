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
package org.rhq.enterprise.gui.coregui.server.gwt;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.RawConfiguration;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.util.HibernateDetachUtility;
import org.rhq.enterprise.server.util.LookupUtil;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Greg Hinkle
 */
public class ResourceGWTServiceImpl extends AbstractGWTServiceImpl implements ResourceGWTService {

    private ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();


    private static String[] importantFields = {
            "serialVersionUID",
//                    "ROOT                            \n" +
//                    "ROOT_ID                         \n" +
            "id",

//                    "uuid                            \n" +
//                    "resourceKey                     \n" +
            "name",

//                    "connected                       \n" +
//                    "version                         \n" +
            "description",

//                    "ctime                           \n" +
//                    "mtime                           \n" +
//                    "itime                           \n" +
//                    "modifiedBy                      \n" +
//                    "location                        \n" +
            "resourceType",
//                    "childResources                  \n" +
            "parentResource",
//                    "resourceConfiguration           \n" +
//                    "pluginConfiguration             \n" +
//                    "agent                           \n" +
//                    "alertDefinitions                \n" +
//                    "resourceConfigurationUpdates    \n" +
//                    "pluginConfigurationUpdates      \n" +
//                    "implicitGroups                  \n" +
//                    "explicitGroups                  \n" +
//                    "contentServiceRequests          \n" +
//                    "createChildResourceRequests     \n" +
//                    "deleteResourceRequests          \n" +
//                    "operationHistories              \n" +
//                    "installedPackages               \n" +
//                    "installedPackageHistory         \n" +
//                    "resourceRepos                   \n" +
//                    "schedules                       \n" +
//                    "availability                    \n" +
            "currentAvailability"
//                    "resourceErrors                  \n" +
//                    "eventSources                    \n" +
//                    "productVersion                  "}

    };

    private static Set<String> importantFieldsSet = new HashSet<String>(Arrays.asList(importantFields));

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        System.out.println("Loading GWT RPC Services");
    }

    public ResourceGWTServiceImpl() {
    }

    public PageList<Resource> findResourcesByCriteria(ResourceCriteria criteria) {
        try {
            PageList<Resource> result = resourceManager.findResourcesByCriteria(getSessionSubject(), criteria);
            for (Resource resource : result) {
                resource.setAgent(null);
            }

            ObjectFilter.filterFields(result, importantFieldsSet);

            return SerialUtility.prepare(result, "ResourceService.findResourceByCriteria");
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException(e);
        }
    }

    public List<Resource> getResourceLineage(int resourceId) {
        return SerialUtility.prepare(resourceManager.getResourceLineage(resourceId), "ResourceService.getResourceLineage");
    }


    public List<Resource> getResourceLineageAndSiblings(int resourceId) {
        return SerialUtility.prepare(resourceManager.getResourceLineageAndSiblings(resourceId), "ResourceService.getResourceLineage");
    }


    public Resource getPlatformForResource(int resourceId) {
        return SerialUtility.prepare(resourceManager.getRootResourceForResource(resourceId), "ResourceService.getPlatformForResource");
    }

    public RawConfiguration dummy(RawConfiguration config) {
        System.out.println(config.getPath());
        return config;
    }



}