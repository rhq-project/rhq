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
package org.rhq.core.domain.drift;

/**
 * The type of drift being reported. 
 *
 * @author Jay Shaughnesssy
 */
public enum DriftCategory {
    FILE_ADDED("A"),

    FILE_CHANGED("C"),

    FILE_REMOVED("R");

    private final String code;

    DriftCategory(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static DriftCategory fromCode(String code) {
        for (DriftCategory category : DriftCategory.values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException(code + " is not a DriftCategory code");
    }

    public static String[] names() {
        String[] names = new String[values().length];
        int i = 0;
        for (DriftCategory c : values()) {
            names[i++] = c.name();
        }
        return names;
    }
}