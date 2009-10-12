/*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.jboss.on.common.jbossas;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Ian Springer
 */
public class SecurityDomainInfo {
    private final Log log = LogFactory.getLog(this.getClass());

    private Map<String, String> usersToPasswordsMap = new HashMap();
    private Map<String, Set<String>> usersToRolesMap = new HashMap();
    private Map<String, Set<String>> rolesToUsersMap = new HashMap();

    public SecurityDomainInfo(File usersPropsFile, File rolesPropsFile) throws Exception {
        parseUsersPropertiesFiles(usersPropsFile);
        parseRolesPropertiesFiles(rolesPropsFile);
    }

    public Set<String> getUsers(String role) {
        if (this.rolesToUsersMap.containsKey(role)) {
            return this.rolesToUsersMap.get(role);
        }
        else {
            return Collections.emptySet();
        }
    }

    public Set<String> getRoles(String user) {
        if (!this.usersToRolesMap.containsKey(user)) {
            throw new IllegalArgumentException("No such user: " + user);
        }
        return this.usersToRolesMap.get(user);
    }

    public String getPassword(String user) {
        if (!this.usersToPasswordsMap.containsKey(user)) {
            throw new IllegalArgumentException("No such user: " + user);
        }
        return this.usersToPasswordsMap.get(user);
    }

    // Property Syntax: user=password
    private void parseUsersPropertiesFiles(File usersPropsFile) throws Exception {
        Properties usersProps = parsePropertiesFile(usersPropsFile);

        for (Object userObj : usersProps.keySet()) {
            String user = (String)userObj;
            String password = usersProps.getProperty(user);
            this.usersToPasswordsMap.put(user, password);
            this.usersToRolesMap.put(user, new HashSet());
        }
    }

    // Property Syntax: user=role1,role2,...
    private void parseRolesPropertiesFiles(File rolesPropsFile) throws Exception {
        Properties rolesProps = parsePropertiesFile(rolesPropsFile);

        for (Object userObj : rolesProps.keySet()) {
            String user = (String)userObj;
            String roles = rolesProps.getProperty(user);
            String[] rolesArray = roles.split(",[ \t]*");
            if (rolesArray.length == 0) {
                continue;
            }

            Set<String> rolesForUser;
            if (this.usersToRolesMap.containsKey(user)) {
                rolesForUser = this.usersToRolesMap.get(user);
            }
            else {
                rolesForUser = new HashSet();
                this.usersToRolesMap.put(user, rolesForUser);
            }

            for (String role : rolesArray) {
                // Update the users-to-roles map.
                rolesForUser.add(role);

                // Update the roles-to-users map.
                Set<String> users;
                if (this.rolesToUsersMap.containsKey(role)) {
                    users = this.rolesToUsersMap.get(role);
                }
                else {
                    users = new LinkedHashSet();
                    this.rolesToUsersMap.put(role, users);
                }
                users.add(user);
            }
        }
    }

    private Properties parsePropertiesFile(File propsFile) throws IOException {
        Properties props = new Properties();
        FileInputStream inputStream = new FileInputStream(propsFile);
        try {
            props.load(inputStream);
        } finally {
            try {
                inputStream.close();
            }
            catch (IOException e) {
                log.error("Failed to close properties file " + propsFile);
            }
        }
        return props;
    }
}
