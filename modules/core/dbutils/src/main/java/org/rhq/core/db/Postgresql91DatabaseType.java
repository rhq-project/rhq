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
package org.rhq.core.db;

/**
 * Postgres 9.1 database which extends the Postgres database.
 * It is important to extend 8.3+ and not any previous version as the
 * code will enable async commit in some place for instanceof 8.3
 *
 * Postgres 9.1 brings WAL-less tables.
 *
 * @author Heiko W. Rupp
 */
public class Postgresql91DatabaseType extends Postgresql90DatabaseType {
    /**
     * @see org.rhq.core.db.DatabaseType#getName()
     */
    @Override
    public String getName() {
        return PostgresqlDatabaseType.VENDOR_NAME + " 9.1+";
    }

    /**
     * @see org.rhq.core.db.DatabaseType#getVersion()
     */
    @Override
    public String getVersion() {
        return "9.1+";
    }

    /**
     * Postgres 9.1 and later is using a standard conforming escape character out of the box
     * unlike earlier versions of Postgres, which means we need to follow this convention
     * here.
     * @return escape char
     */
    @Override
    public String getEscapeCharacter() {
        String result = System.getProperty("rhq.server.database.escape-character");

        return (null == result) ? "\\" : result;
    }

}