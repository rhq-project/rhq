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
package org.rhq.core.domain.resource;

/**
 * Indicates what data is required when creating a new resource.
 *
 * @author Jason Dobies
 */
public enum ResourceCreationDataType {
    /**
     * Indicated the resource configuration values must be specified at resource creation time.
     */
    CONFIGURATION,

    /**
     * Indicates one or more pieces of content must be uploaded to the plugin at resource creation time.
     */
    CONTENT
}