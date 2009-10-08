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
 * Oracle10 database which extends the Oracle9 database.
 *
 * @author John Mazzitelli
 *
 */
public class Oracle10DatabaseType extends Oracle9DatabaseType {
    /**
     * @see DatabaseType#getName()
     */
    public String getName() {
        return OracleDatabaseType.VENDOR + "10";
    }

    /**
     * @see DatabaseType#getVersion()
     */
    public String getVersion() {
        return "10";
    }

    public String getHibernateDialect() {
        return "org.hibernate.dialect.Oracle10gDialect";
    }
}