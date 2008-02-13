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
package org.rhq.core.domain.content.composite;

import java.io.Serializable;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.PackageVersion;

/**
 * Composite that simply holds a {@link PackageVersion} ID and that package version's
 * {@link PackageVersion#getMetadata() metadata}.
 *
 * @author John Mazzitelli
 */
public class PackageVersionMetadataComposite implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int packageVersionId;
    private final byte[] metadata;
    private final String packageName;
    private final String version;
    private final String packageTypeName;
    private final String archName;

    public PackageVersionMetadataComposite(int packageVersionId, byte[] metadata, String packageName, String version,
        String packageTypeName, String architectureName) {
        this.packageVersionId = packageVersionId;
        this.metadata = metadata;
        this.packageName = packageName;
        this.version = version;
        this.packageTypeName = packageTypeName;
        this.archName = architectureName;
    }

    public int getPackageVersionId() {
        return packageVersionId;
    }

    public byte[] getMetadata() {
        return metadata;
    }

    public PackageDetailsKey getPackageDetailsKey() {
        return new PackageDetailsKey(packageName, version, packageTypeName, archName);
    }
}