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
package org.rhq.core.domain.resource;

/**
 * Indicates whether or not resource of a particular type can be created and/or deleted by the user. More specifically,
 * through the <code>ResourceFactoryAgentService</code> inteface into the plugin container.
 *
 * @author Jason Dobies
 */
public enum CreateDeletePolicy {
    /**
     * Resources of this type can neither be created nor deleted by the user.
     */
    NEITHER,

    /**
     * Resources of this type may be created by users, but not deleted.
     */
    CREATE_ONLY,

    /**
     * Resources of this type may be deleted by users, but not created.
     */
    DELETE_ONLY,

    /**
     * Resources of this type can be both created and deleted by the user.
     */
    BOTH
}