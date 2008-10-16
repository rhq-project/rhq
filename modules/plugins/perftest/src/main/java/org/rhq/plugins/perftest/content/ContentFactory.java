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
package org.rhq.plugins.perftest.content;

import java.util.Set;
import org.rhq.core.domain.content.PackageType;
import org.rhq.core.domain.content.transfer.ResourcePackageDetails;

/**
 * @author Jason Dobies
 */
public interface ContentFactory {
    /**
     * Returns a set of artifacts. The generation of the set is determined by the underlying implementation.
     *
     * @param  packageType domain object passed into the call to the resource component, needed in the creation of the
     *                     artifact details
     *
     * @return set of artifacts; will not be <code>null</code>
     */
    Set<ResourcePackageDetails> discoverContent(PackageType packageType);
}