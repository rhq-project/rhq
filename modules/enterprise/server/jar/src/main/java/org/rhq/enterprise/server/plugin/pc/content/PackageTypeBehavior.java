/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugin.pc.content;

import java.util.Comparator;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.PackageVersionFormatDescription;
import org.rhq.core.domain.content.ValidatablePackageDetailsKey;

/**
 * The package types are either defined in the agent plugin descriptors or by the server-side
 * package type plugins. The server-side plugins define a "behaviorClass" that implements 
 * this interface to provide additional runtime "hooks" controlling the package type
 * behavior.
 *
 * @author Lukas Krejci
 */
public interface PackageTypeBehavior {

    /**
     * Validate the package before it gets stored.
     * 
     * @param key the package details key
     * @param origin who is uploading the package
     * @throws PackageDetailsValidationException
     */
    void validateDetails(ValidatablePackageDetailsKey key, Subject origin) throws PackageDetailsValidationException;

    /**
     * This comparator will be used to determine the latest version of a package.
     * It should sort the package versions from the "oldest" to the "youngest", i.e. when comparing
     * <br/>
     * <code>comparator.compare(older, younger)</code>
     * <br/>
     * the comparator should return a value &lt; 0.
     * 
     * @param packageTypeName the name of the package type
     * @return a comparator to use when sorting the package versions or null if {@link PackageVersion#DEFAULT_COMPARATOR} should be used. 
     */
    Comparator<PackageVersion> getPackageVersionComparator(String packageTypeName);

    /**
     * Description of the package version format mandated by the package type. 
     * 
     * @return the package version format description
     */
    PackageVersionFormatDescription getPackageVersionFormat(String packageTypeName);
}
