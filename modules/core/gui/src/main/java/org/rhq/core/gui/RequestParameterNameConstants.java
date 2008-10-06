 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.gui;

/**
 * A set of constants representing HTTP request parameter names.
 *
 * @author Ian Springer
 * @author Mark Spritzler
 */
public final class RequestParameterNameConstants {
    /**
     * A request parameter that specifies the @see AbstractResourceConfigurationUpdate id for a Resource Configuration
     * to be viewed or edited.
     */
    public static final String CONFIG_ID_PARAM = "configId";

    public static final String LIST_NAME_PARAM = "listName";

    public static final String LIST_INDEX_PARAM = "listIndex";

    public static final String FUNCTION_PARAM = "function";

    public static final String MAP_NAME_PARAM = "mapName";

    public static final String MEMBER_NAME_PARAM = "memberName";

    private RequestParameterNameConstants() {
    }
}