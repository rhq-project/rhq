/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.gwt;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.auth.Subject;

/**
 * @author Simeon Pinder
 */
public interface LdapGWTService extends RemoteService {

    /**
     * @return Map with all LDAP groups available
     */
    Set<Map<String, String>> findAvailableGroups();

    /**
     * Updates the role with the ldap groups selected.
     */
    void addLdapGroupsToRole(int roleId, List<String> groupIds);

    /** In setting the LDAP groups for this role, all previous group
     *  assignments for this role are removed before most up to date
     *  list of valid LDAP groups is assigned.
     * 
     * @param roleId
     * @param groupIds
     */
    void setLdapGroupsForRole(int roleId, List<String> groupIds);

    /**
     * 
     * @param currentSubject
     * @param user
     * @param password
     * @return
     */
    Subject checkSubjectForLdapAuth(Subject currentSubject, String user, String password);

    /** 
     * 
     * @param currentSubject
     * @param user
     * @param password
     * @return
     */
    void updateLdapGroupAssignmentsForSubject(Subject subject);

    /** Finds ldap groups already assigned to this role.
     * 
     * @param currentRoleId
     * @return
     */
    Set<Map<String, String>> findLdapGroupsAssignedToRole(int currentRoleId);

    /** Boolean response about whether ldap configured..
     * 
     * @return
     */
    Boolean checkLdapConfiguredStatus();

}
