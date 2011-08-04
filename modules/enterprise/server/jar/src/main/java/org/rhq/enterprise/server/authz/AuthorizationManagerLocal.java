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
package org.rhq.enterprise.server.authz;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.Group;

/**
 * A manager that provides methods for checking the current user's JON permissions and for setting permissions on roles.
 *
 * @author Ian Springer
 * @author Joseph Marques
 */
@Local
public interface AuthorizationManagerLocal {
    /**
     * Returns true if the current user has some role attached to some group that contains this resource.
     *
     * @param  subject    the current subject or caller
     * @param  resourceId the id of some Resource to check permissions against
     *
     * @return true if the current user has some role attached to some group that contains this resource
     */
    boolean canViewResource(Subject subject, int resourceId);

    /**
     * Returns true if the current user has a role attached to a group that contains the specified resources. Note that
     * this method will return true if the resources span multiple groups so long has the user is in one or more roles
     * granting view permission for those groups containing the resources.
     *
     * @param subject The current subject or caller
     * @param resourceIds The resource ids against which we are checking whether the subject has access
     * @return true only if the subject has a role attached to a group that contains all of the specified resources
     */
    boolean canViewResources(Subject subject, List<Integer> resourceIds);

    /**
     * Returns true if the current user has some role attached to this group.
     *
     * @param  subject the current subject or caller
     * @param  groupId the id of some Group to check permissions against
     *
     * @return true if the current user has some role attached to this group
     */
    boolean canViewGroup(Subject subject, int groupId);

    /**
     * Returns true if the current user has some role attached to this auto-group.
     *
     * @param  subject the current subject or caller
     * @param  parentResourceId the id of the parent resource filter for this auto-group
     * @param  resourceTypeId the id of the resource type filter for this auto-group
     *
     * @return true if the current user has some role attached to this auto-group
     */
    boolean canViewAutoGroup(Subject subject, int parentResourceId, int resourceTypeId);

    /**
     * Returns true if the current user possesses either: 1) the specified resource permission for the specified
     * resource, or 2) the global MANAGE_INVENTORY permission which, by definition, gives full access to the inventory
     * (all resources and all groups)
     *
     * @param  subject    the current subject or caller
     * @param  permission a resource permission (i.e. permission.getTarget() == Permission.Target.RESOURCE)
     * @param  resourceId the id of some Resource to check permissions against
     *
     * @return true if the current user possesses the specified resource permission for the specified resource
     */
    boolean hasResourcePermission(Subject subject, Permission permission, int resourceId);

    /**
     * Returns true if the current user possesses either: 1) the specified resource permission for *all* of the
     * specified resources, or 2) the global MANAGE_INVENTORY permission which, by definition, gives full access to the
     * inventory (all resources and all groups) NOTE: The size of the collection must be less than or equal to 1000 (due
     * to an Oracle limitation).
     *
     * @param  subject     the current subject or caller
     * @param  permission  a resource permission (i.e. permission.getTarget() == Permission.Target.RESOURCE)
     * @param  resourceIds the ids of some Resources to check permissions against (size of collection must be <= 1000)
     *
     * @return true if the current user possesses the specified resource permission for the specified resource
     */
    boolean hasResourcePermission(Subject subject, Permission permission, Collection<Integer> resourceIds);

    /**
     * Returns true if the current user possesses either: 1) the specified resource permission for the specified group,
     * or 2) the global MANAGE_INVENTORY permission which, by definition, gives full access to the inventory (all
     * resources and all groups)
     *
     * @param  subject    the current subject or caller
     * @param  permission a resource permission (i.e. permission.getTarget() == Permission.Target.RESOURCE)
     * @param  groupId    the id of some Group to check permissions against
     *
     * @return true if the current user possesses the specified resource permission for the specified group
     */
    boolean hasGroupPermission(Subject subject, Permission permission, int groupId);

    /**
     * Returns true if the current user possesses either: 1) the specified resource permission for the specified
     * auto-group, or 2) the global MANAGE_INVENTORY permission which, by definition, gives full access to the
     * inventory (all resources and all groups)
     *
     * @param  subject          the current subject or caller
     * @param  permission       a resource permission (i.e. permission.getTarget() == Permission.Target.RESOURCE)
     * @param  parentResourceId the id of the parent resource of the auto-group to check permissions against
     * @param  parentResourceId the id of the resource type filter of the auto-group to check permissions against
     *
     * @return true if the current user possesses the specified resource permission for the specified auto-group
     */
    boolean hasAutoGroupPermission(Subject subject, Permission permission, int parentResourceId, int resourceTypeId);

    /**
     * Returns true if the current user possesses the specified global permission.
     *
     * @param  subject    the current subject or caller
     * @param  permission a global permission (i.e. permission.getTarget() == Permission.Target.GLOBAL)
     *
     * @return true if the current user possesses the specified global permission
     */
    boolean hasGlobalPermission(Subject subject, Permission permission);

    /**
     * Gets the set of permissions that the current user explicitly possesses for the specified {@link Resource}.
     *
     * @param  subject    the current subject or caller
     * @param  resourceId the id of some Resource to check permissions against
     *
     * @return the set of permissions that the current user possesses for the specified {@link Resource} - never null
     */
    Set<Permission> getExplicitResourcePermissions(Subject subject, int resourceId);

    /**
     * Gets the set of permissions that the current user implicitly possesses for the specified {@link Resource}.
     *
     * @param  subject    the current subject or caller
     * @param  resourceId the id of some Resource to check permissions against
     *
     * @return the set of permissions that the current user implicitly possesses for the specified {@link Resource} - never null
     */
    Set<Permission> getImplicitResourcePermissions(Subject subject, int resourceId);

    /**
     * Gets the set of permissions that the current user explicitly possesses for the specified {@link Group}.
     *
     * @param  subject the current subject or caller
     * @param  groupId the id of some Group to check permissions against
     *
     * @return the set of permissions that the current user explicitly possesses for the specified {@link Group} - never null
     */
    Set<Permission> getExplicitGroupPermissions(Subject subject, int groupId);

    /**
     * Gets the set of permissions that the current user implicitly possesses for the specified {@link Group}.
     *
     * @param  subject the current subject or caller
     * @param  groupId the id of some Group to check permissions against
     *
     * @return the set of permissions that the current user implicitly possesses for the specified {@link Group}
     */
    Set<Permission> getImplicitGroupPermissions(Subject subject, int groupId);

    /**
     * Returns whether the subject can manage all resources and all groups in the system, without having to filter
     * operations through the subject-role-group-resource authorization mechanism
     *
     * @param  subject the current subject or caller
     *
     * @return whether this subject has full control over resources and groups
     */
    boolean isInventoryManager(Subject subject);

    /**
     * Gets the set of global permissions that the current user explicitly possesses.
     *
     * @param  subject the current subject or caller
     *
     * @return the set of global permissions that the current user possesses - never null
     */
    Set<Permission> getExplicitGlobalPermissions(Subject subject);

    /**
     * Returns <code>true</code> if and only if the given subject represents either the initial superuser (e.g.
     * rhqadmin) or the internal overlord subject. These are what is known as the "system superusers".
     *
     * @param  subject the subject to check
     *
     * @return <code>true</code> if the given subject is considered one of the built-in system superusers
     */
    boolean isSystemSuperuser(Subject subject);

    /**
     * Returns <code>true</code> if and only if the given subject represents the internal overlord subject.
     *
     * @param  subject the subject to check
     *
     * @return <code>true</code> if the given subject is considered the overlord subject
     */
    boolean isOverlord(Subject subject);
    
    /**
     * Returns true if given subject is able to view given repo.
     * The subject is able to view a repo if it is public or if the subject is the owner of the repo 
     * or if the subject is a member of a role with {@link Permission#MANAGE_REPOSITORIES}.
     *  
     * @param subject
     * @param repoId
     * @return true if subject is able to view the repo, false otherwise
     */
    boolean canViewRepo(Subject subject, int repoId);
    
    /**
     * Returns true if given subject is able to update given repo.
     * The subject is able to update a repo if it is owned by the subject 
     * or if the subject is a member of a role with {@link Permission#MANAGE_REPOSITORIES}.
     *  
     * @param subject
     * @param repoId
     * @return true if subject is able to update the repo, false otherwise
     */
    boolean canUpdateRepo(Subject subject, int repoId);
}