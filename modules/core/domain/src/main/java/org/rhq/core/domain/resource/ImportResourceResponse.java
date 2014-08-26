/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

import java.io.Serializable;

/**
 * @author Thomas Segismont
 */
public class ImportResourceResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private Resource resource;
    private boolean resourceAlreadyExisted;

    @SuppressWarnings("unused")
    public ImportResourceResponse() {
        // Needed by GWT
    }

    public ImportResourceResponse(Resource resource, boolean resourceAlreadyExisted) {
        this.resource = resource;
        this.resourceAlreadyExisted = resourceAlreadyExisted;
    }

    public Resource getResource() {
        return resource;
    }

    @SuppressWarnings("unused")
    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public boolean isResourceAlreadyExisted() {
        return resourceAlreadyExisted;
    }

    @SuppressWarnings("unused")
    public void setResourceAlreadyExisted(boolean resourceAlreadyExisted) {
        this.resourceAlreadyExisted = resourceAlreadyExisted;
    }
}
