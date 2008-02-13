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
package org.rhq.enterprise.server.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import javax.persistence.EntityManager;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Permission.Target;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.auth.SessionManager;

public class SessionTestHelper {
    /*
     * supporting methods to help carry out primary tests
     */
    public static boolean samePermissions(Set<Permission> one, Set<Permission> two) {
        return one.containsAll(two) && two.containsAll(one);
    }

    public static Set<Permission> getAllGlobalPerms() {
        Permission[] allPermissions = Permission.values();
        Set<Permission> globalPermissions = EnumSet.noneOf(Permission.class);
        for (Permission permission : allPermissions) {
            if (permission.getTarget() == Target.GLOBAL) {
                globalPermissions.add(permission);
            }
        }

        return globalPermissions;
    }

    public static Set<Permission> getAllResourcePerms() {
        Permission[] allPermissions = Permission.values();
        Set<Permission> globalPermissions = EnumSet.noneOf(Permission.class);
        for (Permission permission : allPermissions) {
            if (permission.getTarget() == Target.RESOURCE) {
                globalPermissions.add(permission);
            }
        }

        return globalPermissions;
    }

    public static Subject createNewSubject(EntityManager em, String subjectName) {
        subjectName = preprocess(subjectName);

        Subject newSubject = new Subject();
        newSubject.setName(subjectName);
        newSubject.setFactive(true);
        newSubject.setFsystem(false);

        em.persist(newSubject);
        em.flush();

        SessionManager.getInstance().put(newSubject, 1000 * 60);

        return newSubject;
    }

    public static Role createNewRoleForSubject(EntityManager em, Subject subject, String roleName) {
        roleName = preprocess(roleName);

        Role newRole = new Role(roleName);
        newRole.setFsystem(false);

        subject.getRoles().add(newRole);
        newRole.getSubjects().add(subject);

        em.persist(subject);
        em.persist(newRole);
        em.flush();

        return newRole;
    }

    public static ResourceType createNewResourceType(EntityManager em) {
        ResourceType type = new ResourceType(preprocess("testType"), "testPlugin", ResourceCategory.PLATFORM, null);
        em.persist(type);
        em.flush();

        return type;
    }

    public static ResourceGroup createNewCompatibleGroupForRole(EntityManager em, Role role, String groupName) {
        ResourceType type = createNewResourceType(em);
        return createNewGroupForRoleHelper(em, role, groupName, type, false);
    }

    public static ResourceGroup createNewMixedGroupForRole(EntityManager em, Role role, String groupName,
        boolean recursive) {
        return createNewGroupForRoleHelper(em, role, groupName, null, recursive);
    }

    private static ResourceGroup createNewGroupForRoleHelper(EntityManager em, Role role, String groupName,
        ResourceType type, boolean recursive) {
        ResourceGroup newGroup = null;

        groupName = preprocess(groupName);

        if (type == null) {
            newGroup = new ResourceGroup(groupName);
            newGroup.setRecursive(recursive);
        } else {
            newGroup = new ResourceGroup(groupName, type);
        }

        if (role != null) {
            role.getResourceGroups().add(newGroup);
            newGroup.addRole(role);

            em.persist(role);
        }

        em.persist(newGroup);
        em.flush();

        return newGroup;
    }

    public static Resource createNewResourceForGroup(EntityManager em, ResourceGroup group, String resourceName) {
        ResourceType type = group.getResourceType();

        if (type == null) {
            type = createNewResourceType(em);
        }

        resourceName = preprocess(resourceName);
        Resource resource = new Resource(resourceName, resourceName, type);

        group.addExplicitResource(resource);
        resource.getExplicitGroups().add(group);

        /*
         * Single resource implies the implicit resource list should mirror the explicit one
         */
        group.addImplicitResource(resource);
        resource.getImplicitGroups().add(group);

        em.persist(resource);
        em.flush();

        return resource;
    }

    public static Agent createNewAgent(EntityManager em, String agentName) {
        agentName = preprocess(agentName);
        String address = "localhost";
        int port = 16163;
        String endPoint = "socket://" + address + ":" + port + "/?rhq.communications.connector.rhqtype=agent";

        Agent agent = new Agent(agentName, address, port, endPoint, agentName);

        em.persist(agent);
        em.flush();

        return agent;
    }

    public static Resource createNewResource(EntityManager em, String resourceName) {
        resourceName = preprocess(resourceName);
        ResourceType type = new ResourceType(preprocess("testType"), "testPlugin", ResourceCategory.PLATFORM, null);
        Resource resource = new Resource(resourceName, resourceName, type);

        em.persist(type);
        em.persist(resource);
        em.flush();

        return resource;
    }

    public static Collection<Integer> getResourceList(Resource... resources) {
        Collection<Integer> resourceIdList = new ArrayList<Integer>(resources.length + 1);
        for (Resource resource : resources) {
            resourceIdList.add(resource.getId());
        }

        return resourceIdList;
    }

    private static String preprocess(String name) {
        return name += System.currentTimeMillis();
    }
}