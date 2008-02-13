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
package org.rhq.core.util.xmlparser;

/**
 * An interface which classes much implement in order to have their attributes handled. If this interface is not
 * implemented, then all attributes for a tag are considered to be 'unknown', and the object must implement the
 * XmlUnAttrHandler to process them.
 */

public interface XmlAttrHandler {
    /**
     * Get a list of attributes which the handler knows about.
     */
    public XmlAttr[] getAttributes();

    /**
     * Called when XmlParser finds a known attributes (as returned by getAttributes()).
     *
     * @param attrNumber An index into the array which was returned from getAttributes. The index represents the found
     *                   attribute
     * @param value      The value of the attribute
     */
    public void handleAttribute(int attrNumber, String value) throws XmlAttrException;
}