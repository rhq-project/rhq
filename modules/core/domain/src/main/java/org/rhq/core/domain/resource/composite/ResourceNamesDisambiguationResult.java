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

package org.rhq.core.domain.resource.composite;

import java.io.Serializable;
import java.util.List;

/**
 * This class is a result of resource disambiguation.
 * This is used in ResourceManager. When supplied a list of results, it returns an
 * instance of this class describing how the results should be disambiguated.
 * 
 * @author Lukas Krejci
 */
public class ResourceNamesDisambiguationResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<DisambiguationReport<T>> resolution;

    public ResourceNamesDisambiguationResult(List<DisambiguationReport<T>> resolution) {
        this.resolution = resolution;
    }

    /**
     * Lists the disambiguation results.
     * Each record contains the original value, disambiguated parents, the resource type name and 
     * the type plugin name.
     */
    public List<DisambiguationReport<T>> getResolution() {
        return resolution;
    }

    public String toString() {
        return "ResourceNamesDisambiguationResult(resolution=" + resolution + ")";
    }
}
