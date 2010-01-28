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
package org.rhq.enterprise.server.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.resource.ResourceType;

/**
 * Translates the configuration property value lookup key into a named query in the system.
 *
 * @see DynamicConfigurationPropertyBean
 *
 * @author Jason Dobies
 */
public class PropertyExpressionEvaluator {

    public static final String KEY_USERS = "users";
    public static final String KEY_ROLES = "roles";
    public static final String KEY_PACKAGE_TYPES = "package-types";
    public static final String KEY_RESOURCE_TYPE_PLUGIN = "resource-type-with-plugin";

    /**
     * @see #getQueryNameForKey(String)
     */
    private static final Map<String, String> KEY_TO_QUERY_NAME;
    static {
        Map<String, String> temp = new HashMap<String, String>();

        temp.put(KEY_USERS, Subject.QUERY_DYNAMIC_CONFIG_VALUES);
        temp.put(KEY_ROLES, Role.QUERY_DYNAMIC_CONFIG_VALUES);
        temp.put(KEY_PACKAGE_TYPES, PackageType.QUERY_DYNAMIC_CONFIG_VALUES);
        temp.put(KEY_RESOURCE_TYPE_PLUGIN, ResourceType.QUERY_DYNAMIC_CONFIG_WITH_PLUGIN);

        KEY_TO_QUERY_NAME = Collections.unmodifiableMap(temp);
    }

    /**
     * Returns the name of the query to use to retrieve the configuration values for properties
     * defined with the given key. Each query referenced by the return result should only return
     * a list of <code>String</code> objects, not any form of complex object (such as <code>Resource</code>
     * or <code>Subject</code>).
     *
     * @param key plugin writer specified key to indicate what values to look up; this should be one
     *            of the KEY_* values defined in this class.
     * @return name of a named query, where the query is defined elsewhere; <code>null</code> if the
     *         key is not supported by the system
     */
    public static String getQueryNameForKey(String key) {
        return KEY_TO_QUERY_NAME.get(key);
    }

}
