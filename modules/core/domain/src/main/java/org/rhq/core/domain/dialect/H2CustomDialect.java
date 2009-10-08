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
package org.rhq.core.domain.dialect;

import org.hibernate.dialect.H2Dialect;

/**
 * This class extends the basic H2Dialect that comes in the 
 * Hibernate core distribution to force it to use sequences
 * for H2 database.
 * 
 * @author Joseph Marques
 */
public class H2CustomDialect extends H2Dialect {

    @Override
    public boolean supportsIdentityColumns() {
        /*
         * By default, GeneratorType.AUTO strategy will choose IDENTITY if a database supports it.
         * However, the embedded database was originally written using sequences.  Later, SQL Server
         * support was added which required changing the generation strategy from SEQUENCE to AUTO.
         * This broke support for the embedded database because the H2Dialect was trying to use
         * identity data types for key columns, which the H2DatabaseType did not support.  This hack
         * basically tricks Hibernate into believing that H2 doesn't support identity types, which
         * then forces it to fall back to using the SEQUENCE strategy. 
         */
        return false;
    }
}
