 /*
  * Jopr Management Platform
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
package com.jboss.jbossnetwork.product.jbpm.handlers;

/**
 * For values that are stored in the JPBM execution context, this class contains the names of the keys against which
 * they are stored.
 *
 * @author Jason Dobies
 */
public class ContextVariables {
    // The following are used by the handlers to make calls elsewhere in the plugin

    public static final String CONTENT_CONTEXT = "contentContext";

    public static final String PACKAGE_DETAILS_KEY = "packageDetailsKey";

    public static final String CONTROL_ACTION_FACADE = "controlActionFacade";

    // The following are used in substitutions into the workflows themselves

    public static final String TIMESTAMP = "timestamp";

    public static final String JBOSS_SERVER_DIR = "jbossServerHomeDir";

    public static final String JBOSS_HOME_DIR = "jbossHomeDir";

    public static final String JBOSS_CLIENT_DIR = "jbossClientDir";

    public static final String DOWNLOAD_DIR = "downloadFolder";

    public static final String PATCH_DIR = "patchFolder";

    public static final String SOFTWARE = "software";
}