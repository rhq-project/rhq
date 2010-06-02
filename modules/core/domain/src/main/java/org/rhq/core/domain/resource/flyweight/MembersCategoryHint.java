/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.core.domain.resource.flyweight;

import org.rhq.core.domain.resource.ResourceCategory;

/**
 * Hints about the category of resources contained in a {@link AutoGroupCompositeFlyweight}.
 * 
 * @author Lukas Krejci
 */
public enum MembersCategoryHint {
    PLATFORM, SERVER, SERVICE, MIXED, NONE;

    private String name;
    
    private MembersCategoryHint() {
        StringBuilder bld = new StringBuilder();
        String name = name().toLowerCase();
        bld.append(Character.toUpperCase(name.charAt(0)));
        bld.append(name, 1, name.length());
        this.name = bld.toString();
    }
    
    public static MembersCategoryHint fromResourceCategory(ResourceCategory category) {
        switch (category) {
        case PLATFORM:
            return PLATFORM;
        case SERVER:
            return SERVER;
        case SERVICE:
            return SERVICE;
        default:
            return null;
        }
    }
    
    public String toString() {
        return name;
    }
}
