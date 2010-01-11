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

import java.util.List;

/**
 * Responsible for retrieving configuration property values from the database.
 *
 * @author Jason Dobies
 */
public interface DynamicConfigurationPropertyLocal {

    /**
     * Translates the provided property value key into its corresponding domain query and returns the
     * results.
     *
     * @param key indicates the data being retrieved from the database; should be one of the constants
     *            in {@link PropertyExpressionEvaluator}
     * @return list of values matching the requested key; empty list if the key is not supported
     */
    List<String> lookupValues(String key);

}
