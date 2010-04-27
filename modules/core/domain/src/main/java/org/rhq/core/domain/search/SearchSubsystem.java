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
package org.rhq.core.domain.search;

import java.util.HashMap;
import java.util.Map;

import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;

/**
 * The search mechanism is generic and, thus, suited to finding data in any
 * RHQ subsystem, including its own.  Each individual page in the UI can have 
 * different advanced syntax.  This enum will serve as a unique identifier 
 * for which subsystem is currently active.  You can think of it as a piece of
 * meta-data that annotates each saved search, representing which context it
 * belongs to.  This will also affect which grammar is used to auto-complete
 * search expressions.
 * 
 * @author Joseph Marques
 */
public enum SearchSubsystem {

    RESOURCE(Resource.class), //
    GROUP(ResourceGroup.class);

    private Class<?> entityClass;
    private static Map<Class<?>, SearchSubsystem> subsystems;
    static {
        subsystems = new HashMap<Class<?>, SearchSubsystem>();
        for (SearchSubsystem next : SearchSubsystem.values()) {
            SearchSubsystem.subsystems.put(next.entityClass, next);
        }
    }

    private SearchSubsystem(Class<?> entityClass) {
        this.entityClass = entityClass;
    }

    public Class<?> getEntityClass() {
        return this.entityClass;
    }

    public static SearchSubsystem get(Class<?> entityClass) {
        return SearchSubsystem.subsystems.get(entityClass);
    }

    /**
     * A Java bean style getter to allow us to access the enum name from JSP or Facelets pages
     *
     * @return the enum name
     */
    public String getName() {
        return name();
    }
}
