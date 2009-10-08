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
package org.rhq.core.clientapi.server.plugin.content;

import java.util.HashSet;
import java.util.Set;
import org.rhq.core.domain.content.PackageDetails;

/**
 * These are the package details that a {@link ContentSourceAdapter} will use when refering to package versions it finds
 * in the remote repository. It is the same as {@link PackageDetails} with the addition of a resource type name, since
 * that is needed to make package types unique (along with the package type name itself, which is specified in the
 * {@link PackageDetails} superclass).
 *
 * @author John Mazzitelli
 */
public class ContentSourcePackageDetails extends PackageDetails {
    private static final long serialVersionUID = 1L;

    /**
     * Contains a set of specific versions of resources that this package can be applied to. The resource type to which
     * these apply is found in the {@link ContentSourcePackageDetailsKey}. If there are no entries in this list, the
     * package can be applied to any resource of the given type.
     */
    private Set<String> resourceVersions = new HashSet<String>();

    public ContentSourcePackageDetails(ContentSourcePackageDetailsKey key) {
        super(key);
    }

    /**
     * Just a convienence method to return the key cast to the appropriate type.
     *
     * @return the key cast to the appropriate sub-type
     */
    public ContentSourcePackageDetailsKey getContentSourcePackageDetailsKey() {
        return (ContentSourcePackageDetailsKey) super.getKey();
    }

    public Set<String> getResourceVersions() {
        return resourceVersions;
    }

    public void setResourceVersions(Set<String> resourceVersions) {
        if (resourceVersions == null) {
            throw new IllegalArgumentException("resourceVersions cannot be null");
        }

        this.resourceVersions = resourceVersions;
    }

    public void addResourceVersion(String version) {
        this.resourceVersions.add(version);
    }
}