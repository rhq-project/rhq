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

import java.util.EnumSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlEnum;

/**
 * Represents a resource's current inventory status.
 */
@XmlEnum
@XmlAccessorType(XmlAccessType.FIELD)
public enum InventoryStatus {
    /**
     * <ul>
     *    <li>NEW: Auto-discovered but not yet imported into the inventory.</li> 
     *    
     *    <li>IGNORED: Auto-discovered but explicitly moved to a state that will suppress it from showing up in future 
     *                 discoveries.  Resources in this state will not be shown in the inventory browser.</li>
     *                 
     *    <li>COMMITTED: Resources in this state will be visible in the inventory browser.  The act of importing
     *                   resources changes their state from NEW to COMMITTED.  Note: resources that are factory-created
     *                   or manually added will also appear in the inventory browser and, thus, will be COMMITTED.</li>
     *
     *     <li>DELETED: This state is deprecated since RHQ 4.14 and exists only for API compatibility reason.</li>
     *                 
     *    <li>UNINVENTORIED: Resources can be removed from the inventory.  Since this is an expensive operation, these
     *                       resources are temporarily marked as UNINVENTORIED which will suppress them from showing up
     *                       in the inventory browser.  Then, in the background, all history will be purged for these
     *                       resources, after which they are removed from the database completely.</li>
     * </ul>
     */
    NEW, IGNORED, COMMITTED, DELETED, UNINVENTORIED;

    /**
     * @return Returns the InventoryStatus set representing the resources currently in inventory, omitting
     * the resources marked for special processing (like UNINVNETORIED).
     */
    static public EnumSet<InventoryStatus> getInInventorySet() {
        return EnumSet.of(NEW, IGNORED, COMMITTED);
    }
}