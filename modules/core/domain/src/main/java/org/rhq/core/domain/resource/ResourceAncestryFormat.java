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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlEnum;

import org.rhq.enterprise.server.resource.ResourceManagerRemote;

/**
 * Used to request the desired diplay format for resource ancestry.
 * @see {@link ResourceManagerRemote#getResourcesAncestry(org.rhq.core.domain.auth.Subject, Integer[], ResourceAncestryFormat)}
 * 
 * @author Jay Shaughnessy
 */
@XmlEnum
@XmlAccessorType(XmlAccessType.FIELD)
public enum ResourceAncestryFormat {
    /**
     * <ul>
     *    <li>RAW: The raw, encoded value.  This is already provided by the Resource.ancestry field. 
     *    
     *    <li>SIMPLE: Short, name only format: (eg. parentName < grandParentName < etc...)
     *                 
     *    <li>VERBOSE: Verbose, MultiLine format incorporating name, type and indentation.
     * </ul>
     */
    RAW, SIMPLE, VERBOSE
}