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

/**
 * A composite exposing how many times an error type is attached to a resource and when it last occurred.
 *
 * @author Thomas Segismont
 */
public final class ResourceErrorTypeComposite {
    private final int resourceId;
    private final ResourceErrorType resourceErrorType;
    private final long count;
    private final long lastOccurred;

    public ResourceErrorTypeComposite(int resourceId, ResourceErrorType resourceErrorType, long count, long lastOccurred) {
        this.resourceId = resourceId;
        this.resourceErrorType = resourceErrorType;
        this.count = count;
        this.lastOccurred = lastOccurred;
    }

    public int getResourceId() {
        return resourceId;
    }

    public ResourceErrorType getResourceErrorType() {
        return resourceErrorType;
    }

    public long getCount() {
        return count;
    }

    public long getLastOccurred() {
        return lastOccurred;
    }
}
