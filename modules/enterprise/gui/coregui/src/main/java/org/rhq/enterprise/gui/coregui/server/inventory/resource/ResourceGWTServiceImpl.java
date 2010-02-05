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

import java.io.ByteArrayOutputStream;
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
public class ResourceGWTServiceImpl extends RemoteServiceServlet implements ResourceGWTService {

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        System.out.println("Loading GWT RPC Services");
    }

    public ResourceGWTServiceImpl() {
    }

    public PageList<Resource> findResourcesByCriteria(ResourceCriteria criteria) {
        ResourceManagerLocal resourceManager = LookupUtil.getResourceManager();
        SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();
        try {
            PageList<Resource> result = resourceManager.findResourcesByCriteria(subjectManager.getOverlord(), criteria);
            for (Resource resource : result) {
                resource.setAgent(null);
            }


            HibernateDetachUtility.nullOutUninitializedFields(result,
                    HibernateDetachUtility.SerializationType.SERIALIZATION);


            String[] importantFields = {
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

            List<String> goodFields = Arrays.asList(importantFields);


            long start = System.currentTimeMillis();

            ByteArrayOutputStream baos = new ByteArrayOutputStream(50000);
            ObjectOutputStream o = new ObjectOutputStream(baos);
            o.writeObject(result);
            o.flush();
            System.out.println("Page " + criteria.getPageControl().getStartRow() + " : " + criteria.getPageControl().getPageSize());
            System.out.println("Serialized size before: " + baos.size());

            Field[] fields = Resource.class.getDeclaredFields();
            for (Resource res : result) {
                for (Field f : fields) {
                    if (!Modifier.isFinal(f.getModifiers())) {
                        if (!goodFields.contains(f.getName())) {
                            if (Object.class.isAssignableFrom(f.getType())) {
//                                System.out.println("clearing " + f.getName());
                                f.setAccessible(true);
                                f.set(res, null);
                            } else {
//                                System.out.println("Can't do " + f.getType());
                            }
                        }
                    }
                }
            }

            baos = new ByteArrayOutputStream(50000);
            o = new ObjectOutputStream(baos);
            o.writeObject(result);
            o.flush();
            System.out.println("Serialized size after: " + baos.size());

            System.out.println("Took: " + (System.currentTimeMillis() - start) + "ms");

            return result;
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException(e);
        }
    }





}