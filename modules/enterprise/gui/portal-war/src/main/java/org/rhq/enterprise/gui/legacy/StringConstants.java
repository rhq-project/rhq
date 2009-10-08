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
package org.rhq.enterprise.gui.legacy;


/**
 * Simple string constants
 */
public interface StringConstants {

    public static final String GUIDE_WINDOW_PROPS = "height=500,width=350,menubar=no,toolbar=no,status=no,resizable=yes,scrollbars=yes";

    //---------------------------------------file names

    public static final String PROPS_TAGLIB = "/WEB-INF/taglib.properties";

    public static final String PROPS_USER_PREFS = "/WEB-INF/DefaultUserPreferences.properties";

    /**
     * file list delimitor
     */
    public static String DIR_PIPE_SYM = "|";
    public static String DIR_COMMA_SYM = ",";

    public static final String UNIT_FORMAT_PREFIX_KEY = "unit.format.";

    /**
     * CONFIG FILTERS
     */
    public static final String MINUTES_LABEL = "minutes";
    public static final String HOURS_LABEL = "hours";
    public static final String DAYS_LABEL = "days";

    public static final String COOKIE_HIDE_HELP = "hq-hide-help";
    public static final String INTROHELP_LOC = "/firstlogin.jsp";

    public static final String CHANGE_OWNER_TITLE = "common.title.Edit";

    /**
     * AJAX Constants
     */
    public static final String AJAX_ELEMENT = "element";
    public static final String AJAX_OBJECT = "object";
}