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
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.EntityManager;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Permission.Target;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.Agent;
import org.rhq.core.domain.resource.InventoryStatus;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.enterprise.server.auth.SessionManager;

public class SessionTestHelper {

    private static AtomicInteger idGenerator = new AtomicInteger(0);

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

        // We want to return the attached object but sessionmanager.put will return a detached copy of newSubject.
        // Copy over the sessionId and pass back the attached Subject.
        Subject sessionSubject = SessionManager.getInstance().put(newSubject, 1000 * 60 * 10); // 10 mins timeout
        newSubject.setSessionId(sessionSubject.getSessionId());
        return newSubject;
    }

    public static void simulateLogin(Subject subject) {
        Subject sessionSubject = SessionManager.getInstance().put(subject, 1000 * 60 * 10); // 10 mins timeout
        subject.setSessionId(sessionSubject.getSessionId());
        return;
    }

    public static void simulateLogout(Subject subject) {
        Integer sessionId = subject.getSessionId();
        if (sessionId != null) {
            SessionManager.getInstance().invalidate(sessionId);
        }
        return;
    }

    public static Role createNewRoleForSubject(EntityManager em, Subject subject, String roleName) {
        return createNewRoleForSubject(em, subject, roleName, new Permission[0]);
    }

    public static Role createNewRoleForSubject(EntityManager em, Subject subject, String roleName,
        Permission... permissions) {
        roleName = preprocess(roleName);

        Role newRole = new Role(roleName);
        newRole.setFsystem(false);

        for (Permission perm : permissions) {
            newRole.addPermission(perm);
        }

        subject.getRoles().add(newRole);
        newRole.getSubjects().add(subject);

        em.persist(subject);
        em.persist(newRole);
        em.flush();

        return newRole;
    }

    public static void addRolePermissions(EntityManager em, Role role, Permission... permissions) {
        role.getPermissions().addAll(Arrays.asList(permissions));
        em.merge(role);
        em.flush();
    }

    public static void removeRolePermissions(EntityManager em, Role role, Permission... permissions) {
        role.getPermissions().removeAll(Arrays.asList(permissions));
        em.merge(role);
        em.flush();
    }

    public static ResourceType createNewResourceType(EntityManager em) {
        ResourceType type = new ResourceType(preprocess("testType"), "testPlugin", ResourceCategory.PLATFORM, null);
        ConfigurationDefinition resourceConfigDef = new ConfigurationDefinition("Fake resconfig def",
            "Resource config def for fake test resource");
        resourceConfigDef.put(new PropertyDefinitionSimple("fake", "fake property", false, PropertySimpleType.BOOLEAN));
        resourceConfigDef.put(new PropertyDefinitionSimple("fakeReadOnly", "fake readonly property", false,
            PropertySimpleType.BOOLEAN));
        resourceConfigDef.getPropertyDefinitions().get("fakeReadOnly").setReadOnly(true);
        type.setResourceConfigurationDefinition(resourceConfigDef);

        ConfigurationDefinition pluginConfigDef = new ConfigurationDefinition("Fake pluginconfig def",
            "Plugin config def for fake test resource");
        pluginConfigDef.put(new PropertyDefinitionSimple("fake", "fake property", false, PropertySimpleType.BOOLEAN));
        pluginConfigDef.put(new PropertyDefinitionSimple("fakeReadOnly", "fake readonly property", false,
            PropertySimpleType.BOOLEAN));
        pluginConfigDef.getPropertyDefinitions().get("fakeReadOnly").setReadOnly(true);
        type.setPluginConfigurationDefinition(pluginConfigDef);

        em.persist(type);
        em.flush();

        return type;
    }

    public static ResourceGroup createNewCompatibleGroupForRole(EntityManager em, Role role, String groupName) {
        return createNewCompatibleGroupForRole(em, role, groupName, null);
    }

    public static ResourceGroup createNewCompatibleGroupForRole(EntityManager em, Role role, String groupName,
        ResourceType type) {
        if (type == null) {
            type = createNewResourceType(em);
        }
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

    /** This uses only EM, it does not call the SLSB for adding resources to a group! */
    public static Resource createNewResourceForGroup(EntityManager em, ResourceGroup group, String resourceName) {
        ResourceType type = group.getResourceType();
        return createNewResourceForGroup(em, group, resourceName, type, true);
    }

    /** This uses only EM, it does not call the SLSB for adding resources to a group! */
    public static Resource createNewResourceForGroup(EntityManager em, ResourceGroup group, String resourceName,
        ResourceType type, boolean doFlush) {
        return createNewResourceForGroup(em, group, resourceName, type, AvailabilityType.UP, doFlush);
    }

    /** This uses only EM, it does not call the SLSB for adding resources to a group! */
    public static Resource createNewResourceForGroup(EntityManager em, ResourceGroup group, String resourceName,
        ResourceType type, AvailabilityType avail, boolean doFlush) {

        if (type == null) {
            type = createNewResourceType(em);
        }

        resourceName = preprocess(resourceName);
        Resource resource = new Resource(resourceName, resourceName, type);
        resource.setUuid("" + new Random().nextInt());
        resource.setInventoryStatus(InventoryStatus.COMMITTED);
        resource.initCurrentAvailability(); // the only way I can see to intialize the avail list
        resource.setCurrentAvailability(new ResourceAvailability(resource, avail));
        resource.getAvailability().clear();
        resource.getAvailability().add(new Availability(resource, 0L, avail));

        group.addExplicitResource(resource);
        resource.getExplicitGroups().add(group);

        /*
         * Single resource implies the implicit resource list should mirror the explicit one
         */
        group.addImplicitResource(resource);
        resource.getImplicitGroups().add(group);

        em.persist(resource);
        if (doFlush) {
            em.flush();
        }

        return resource;
    }

    public static Agent createNewAgent(EntityManager em, String agentName) {
        agentName = preprocess(agentName);
        String address = preprocess("localhost");
        int port = 16163;
        String endPoint = "socket://" + address + ":" + port + "/?rhq.communications.connector.rhqtype=agent";

        Agent agent = new Agent(agentName, address, port, endPoint, agentName);

        em.persist(agent);
        em.flush();

        return agent;
    }

    public static Resource createNewResource(EntityManager em, String resourceName) {
        return createNewResource(em, resourceName, null);
    }

    public static Resource createNewResource(EntityManager em, String resourceName, ResourceType type) {
        if (type == null) {
            type = new ResourceType(preprocess("testType"), "testPlugin", ResourceCategory.PLATFORM, null);
            em.persist(type);
        }

        resourceName = preprocess(resourceName);
        Resource resource = new Resource(resourceName, resourceName, type);
        resource.setUuid("" + new Random().nextInt());
        resource.setInventoryStatus(InventoryStatus.COMMITTED);
        resource.setCurrentAvailability(new ResourceAvailability(resource, AvailabilityType.UP));

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
        return name += (System.currentTimeMillis() + "-" + idGenerator.getAndIncrement());
    }
}