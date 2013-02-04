/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.server.metrics;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.server.metrics.domain.ResultSetMapper;

/**
 * @author John Sanda
 */
public class Query<T>  {

    private PreparedStatement statement;

    private Connection connection;

    private ResultSetMapper<T> resultSetMapper;

    public Query(Connection connection, PreparedStatement statement, ResultSetMapper<T> resultSetMapper) {
        this.connection = connection;
        this.statement = statement;
        this.resultSetMapper = resultSetMapper;
    }

    public void forEach(QueryCallback<T> callback) {
        ResultSet resultSet = null;
        try {
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                //T row = resultSetMapper.map(resultSet);
                //callback.invoke(row);
            }
        } catch(SQLException e) {
            throw new CQLException(e);
        } finally {
            JDBCUtil.safeClose(resultSet);
            JDBCUtil.safeClose(statement);
            JDBCUtil.safeClose(connection);
        }
    }

}
