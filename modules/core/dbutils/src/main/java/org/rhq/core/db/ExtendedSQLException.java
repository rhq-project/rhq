/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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

import java.sql.SQLException;

/**
 * Wraps another SQLException, providing one additional field for specifying the SQL that caused the exception.
 * 
 * @author Ian Springer
 */
public class ExtendedSQLException extends SQLException {

    private String sql;

    public ExtendedSQLException(SQLException sqlException, String sql) {
        super(sqlException.getMessage(), sqlException.getSQLState(), sqlException.getErrorCode(), sqlException);
        setNextException(sqlException.getNextException());
        setStackTrace(sqlException.getStackTrace());
        this.sql = sql;
    }

    public String getSQL() {
        return sql;
    }

}
