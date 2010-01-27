/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.ws.test.util;

import java.util.ArrayList;
import java.util.List;

import org.rhq.enterprise.server.ws.ObjectFactory;
import org.rhq.enterprise.server.ws.Role;
import org.rhq.enterprise.server.ws.RoleCriteria;
import org.rhq.enterprise.server.ws.Subject;
import org.rhq.enterprise.server.ws.WebservicesRemote;

/**
 * @author Jason Dobies
 */
public class WsSubjectUtility {

    private static final String ADMIN_USERNAME = "rhqadmin";
    private static final String ADMIN_PASSWORD = "rhqadmin";

    private WebservicesRemote service;
    private ObjectFactory objectFactory;

    /**
     * Initializes a new instance to use the given connection to a running server.
     *
     * @param service must be in a state where remote calls can be made (i.e. the server is running)
     */
    public WsSubjectUtility(WebservicesRemote service) {
        this.service = service;
        this.objectFactory = new ObjectFactory();
    }

    /**
     * Logs in and returns a WS representation of the default admin user.
     *
     * @return WS subject representing the default admin
     * @throws Exception if there is an error in the remote call
     * @see #ADMIN_USERNAME
     * @see #ADMIN_PASSWORD
     */
    public Subject admin() throws Exception {
        Subject admin = service.login(ADMIN_USERNAME, ADMIN_PASSWORD);
        return admin;
    }

    /**
     * Retrieves a WS subject object for the given credentials. If the user does not exist,
     * it will be created first, assigning it to all roles in the system. The subject will be logged
     * in as part of this call.
     * <p/>
     * Calls to this should be accompanied by a call to {#deleteUser(String} to clean up.
     *
     * @param username may not be <code>null</code>
     * @param password may not be <code>null</code>
     * @return WS subject object that can be used for remote calls that require a subject
     * @throws Exception if anything goes wrong, most likely in the remote calls
     * @see #deleteUser(int)
     */
    public Subject createOrLoginUser(String username, String password) throws Exception {

        // If the user already exists, return that
        try {
            Subject user = service.login(username, password);
            if (user != null) {
                return user;
            }
        }
        catch (Throwable e) {
            // Will occur if the user does not exist, so this should occur often
        }

        // Otherwise, create the user
        Subject newUser = new Subject();
        newUser.setName(username);
        newUser.setFirstName("WsSubjectUtility Created");
        newUser.setLastName("WsSubjectUtility Created");
        newUser.setEmailAddress(username + "@wssubjectutility");
        newUser.setDepartment("WsSubjectUtility Created");

        Subject admin = admin();
        Subject created = service.createSubject(admin, newUser);

        service.createPrincipal(admin, username, password);

        // Assign to all roles
        RoleCriteria criteria = objectFactory.createRoleCriteria();
        List<Role> allRoles = service.findRolesByCriteria(admin, criteria);
        List<Integer> roleIds = rolesToIds(allRoles);

        service.addRolesToSubject(admin, created.getId(), roleIds);

        return created;
    }

    /**
     * Deletes an existing user from the system.
     *
     * @param subjectId must represent a valid subject in the system
     * @throws Exception if there is an error in the remoting
     * @see #deleteUsers(List)
     */
    public void deleteUser(int subjectId) throws Exception {
        List<Integer> deleteUs = new ArrayList<Integer>(1);
        deleteUs.add(subjectId);
        service.deleteSubjects(admin(), deleteUs);
    }

    /**
     * Deletes a number of users from the system.
     *
     * @param deleteUs IDs of one or more users in the system to delete
     * @throws Exception if there is an error in the remoting
     */
    public void deleteUsers(List<Integer> deleteUs) throws Exception {
        service.deleteSubjects(admin(), deleteUs);
    }

    /**
     * Simple transformer from list of {@link Role} objects to a list of their respective IDs.
     *
     * @param roles cannot be <code>null</code>
     * @return new list of the same size as <code>roles</code>
     */
    private List<Integer> rolesToIds(List<Role> roles) {
        List<Integer> roleIds = new ArrayList<Integer>(roles.size());
        for (Role role : roles) {
            roleIds.add(role.getId());
        }
        return roleIds;
    }
}
