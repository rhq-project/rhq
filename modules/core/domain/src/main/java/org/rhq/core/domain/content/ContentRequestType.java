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
package org.rhq.core.domain.content;

/**
 * Indicates the operation attempted in a {@link ContentServiceRequest content request}.
 *
 * @author Jason Dobies
 */
public enum ContentRequestType {
    /**
     * Request is to create/deploy new {@link Package}s to a resource.
     */
    DEPLOY("Deploy"),

    /**
     * Request is to delete {@link Package}s from a resource.
     */
    DELETE("Delete"),

    /**
     * Request is to retrieve the content of {@link Package}s.
     */
    GET_BITS("Retrieve Package Bits");

    // Attributes  --------------------------------------------

    /**
     * Used by the user interface when rendering instances of this enumeration.
     */
    private String displayName;

    // Constructors  --------------------------------------------

    ContentRequestType(String displayName) {
        this.displayName = displayName;
    }

    // Object Overridden Methods  --------------------------------------------

    @Override
    public String toString() {
        return displayName;
    }
}