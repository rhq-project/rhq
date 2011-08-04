/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.util.selenium;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.Messages;

/**
 * Interface to all locatable components.
 * Implement this to ensure Selenium testability for the implementing component.
 */
public interface Locatable {

    Messages MSG = CoreGUI.getMessages();

    /**
     * Returns the locatorId.  This can be useful for constructing more granular locatorIds. For example, if
     * the widget contains sub-widgets.  Note, this is the raw locatorId for the widget, to get the fully
     * formed ID, typically ofthe form "simpleClassname_locatorId" Call {@link getID()}.
     * 
     * @return the locatorId
     */
    String getLocatorId();

    /** 
     * Extends this widget's original locatorId with an extension. This can be useful for constructing more 
     * granular locatorIds. For example, if the widget contains sub-widgets.
     * <pre>
     * ID Format: "getLocatorId()_extension"
     * </pre>
     * 
     * @param extension not null or empty.
     *
     * @return the new, extended locatorId
     */
    String extendLocatorId(String extension);

}
