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

import org.rhq.core.domain.content.ContentSource;
import org.rhq.core.domain.content.PackageBits;
import org.rhq.core.domain.content.PackageVersion;

/**
 * Composite object built by a query that indicates if a package version's contents (its "bits") is loaded/available and
 * (if loaded) if the contents are stored in the database.
 *
 * @author John Mazzitelli
 * @see    PackageBits#QUERY_PACKAGE_BITS_LOADED_STATUS_PACKAGE_VERSION_ID
 */
public class LoadedPackageBitsComposite {
    private final int packageVersionId;
    private final String fileName;
    private final Integer packageBitsId;
    private final boolean isPackageBitsInDatabase;

    public LoadedPackageBitsComposite(int packageVersionId, String fileName, Integer packageBitsId,
        boolean isPackageBitsInDatabase) {
        if ((packageBitsId == null) && isPackageBitsInDatabase) {
            throw new IllegalArgumentException("Does not make sense that there is no bits ID but bits are in DB");
        }

        this.packageVersionId = packageVersionId;
        this.fileName = fileName;
        this.packageBitsId = packageBitsId;
        this.isPackageBitsInDatabase = isPackageBitsInDatabase;
    }

    /**
     * For use directly by JPQL NamedQueries.
     *
     * @param packageVersionId        the ID of the {@link PackageVersion} this composite relates to
     * @param fileName                the name of the package version file
     * @param packageBitsId           the ID of the {@link PackageBits} this composite relates to - if <code>null</code>
     *                                the package bits have not been downloaded yet
     * @param isPackageBitsInDatabase considered <code>true</code> if not <code>null</code> and greater than 0
     */
    public LoadedPackageBitsComposite(int packageVersionId, String fileName, Number packageBitsId,
        Number isPackageBitsInDatabase) {
        this(packageVersionId, fileName, ((packageBitsId != null) ? new Integer(packageBitsId.intValue()) : null),
            (isPackageBitsInDatabase != null) && (isPackageBitsInDatabase.intValue() > 0));
    }

    /**
     * The ID of the {@link PackageVersion} that this composite relates to. It is this package version whose contents is
     * examined.
     *
     * @return package version ID
     */
    public int getPackageVersionId() {
        return packageVersionId;
    }

    /**
     * The name of the {@link PackageVersion#getFileName() package version file}. Useful if the package bits are stored
     * on the filesystem and you want to read in the bits.
     *
     * @return package version file name
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * The ID of the {@link PackageBits} that this composite relates to. If this is <code>null</code>, then the package
     * bits have not been downloaded and are {@link #isPackageBitsAvailable() not available}.
     *
     * @return package bits ID, or <code>null</code>
     */
    public Integer getPackageBitsId() {
        return packageBitsId;
    }

    /**
     * Returns <code>true</code> if the contents of the {@link #getPackageVersionId() package version} is loaded and
     * available. If <code>false</code> is returned, then the contents are not stored anywhere and must be retrieved;
     * usually by either a {@link ContentSource} or by uploading from an agent. If this returns <code>false</code> then
     * you are guaranteed that {@link #isPackageBitsInDatabase()} will return <code>false</code>.
     *
     * <p>If this returns <code>true</code>, you can call {@link #getPackageBitsId()} to get the package bits ID.</p>
     *
     * @return flag to indicate if the package bits are loaded/available
     */
    public boolean isPackageBitsAvailable() {
        return packageBitsId != null;
    }

    /**
     * Returns <code>true</code> if the contents of the {@link #getPackageVersionId() package version} are stored in the
     * database. If this is <code>true</code>, then by definition, you are guaranteed that
     * {@link #isPackageBitsAvailable()} will return <code>true</code>. If this returns <code>false</code>, but
     * {@link #isPackageBitsAvailable()} returns <code>true</code>, then the package bits are available, they just are
     * not stored in the database and to retrieve them you must use an appropriate mechanism based on where the bits are
     * stored (usually cached on a local file system).
     *
     * @return flag to indicate if the package bits are stored in the database
     */
    public boolean isPackageBitsInDatabase() {
        return isPackageBitsInDatabase;
    }
}