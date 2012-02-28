/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.test.shrinkwrap;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Filter;

/**
 * Provides a filtered view on an archive. By default all the assets in the archive
 * are visible.
 *
 * @author Lukas Krejci
 */
public interface FilteredView extends Archive<FilteredView> {

    /**
     * Sets the path filter. Only the paths in the archive that conform to the
     * filter are seemingly going to be present in the archive after this call.
     * <p>
     * To see the whole contents of the archive, just call this method again with
     * a null argument.
     * <p>
     * Casting this archive using the {@link #as(Class)} method will preserve the 
     * filtering.
     */
    FilteredView filterContents(Filter<ArchivePath> filter);

}
