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
package org.rhq.core.db;

/**
 * H2 database version 1.2 which extends the abstract support for H2 database.
 * 
 * @author Joseph Marques
 */
public class H2v12DatabaseType extends H2DatabaseType {
    /**
     * @see DatabaseType#getName()
     */
    @Override
    public String getName() {
        return H2DatabaseType.VENDOR_NAME + "v1.2";
    }

    /**
     * @see DatabaseType#getVersion()
     */
    @Override
    public String getVersion() {
        return "1.2";
    }
}