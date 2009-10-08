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
package org.rhq.core.gui.configuration;

/**
* A JSF managed bean that is used for the action methods for all buttons rendered for {@link ConfigUIComponent}s that
* take the user to a new page. Note, none of the action methods actually do anything - their only purpose is so they
* can be used to define navigation rules that map the corresponding buttons to the appropriate pages.
*
* @author Ian Springer
*/
public class ConfigHelperUIBean {
    private static final String PROCEED_OUTCOME = "proceed";

    public String accessMap() {
        return PROCEED_OUTCOME;
    }

    public String addNewMap() {
        return PROCEED_OUTCOME;
    }

    public String addNewOpenMapMemberProperty() {
        return PROCEED_OUTCOME;
    }
}