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
 * Oracle9 database which extends from the Oracle8 database.
 *
 * @author John Mazzitelli
 *
 */
public class Oracle9DatabaseType extends Oracle8DatabaseType {
    /**
     * @see DatabaseType#getName()
     */
    public String getName() {
        return OracleDatabaseType.VENDOR + "9";
    }

    /**
     * @see DatabaseType#getVersion()
     */
    public String getVersion() {
        return "9";
    }

    public String getHibernateDialect() {
        return "org.hibernate.dialect.Oracle9Dialect";
    }

}