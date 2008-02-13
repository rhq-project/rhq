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
 * The major interface which all objects wishing to use XmlParser must implement. The TagHander provides information
 * about the tags name, and which subtags are valid.
 */
public interface XmlTagHandler {
    /**
     * Retrieve the name of the tag, without angled brackets. (i.e. "foo" for tags named <foo>)
     *
     * @return The name of the tag
     */
    public String getName();

    /**
     * Retrieve information about which subtags are valid within the given tag. This method will only be called once for
     * each object.
     *
     * @return An array of tag information objects, indicating which subtags are valid.
     */
    public XmlTagInfo[] getSubTags();
}