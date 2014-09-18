/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.server.metrics.migrator.datasources;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;

import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author Thomas Segismont
 */
public class ExistingDataJPABulkExportSource extends ExistingDataBulkExportSource {

    private EntityManager entityManager;
    private String selectNativeQuery;

    public ExistingDataJPABulkExportSource(EntityManager entityManager, String selectNativeQuery) {
        super();
    }

    protected void exportExistingData() throws Exception {
        BufferedWriter fileWriter = null;
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            fileWriter = new BufferedWriter(new FileWriter(getExistingDataFile()), IO_BUFFER_SIZE);
            Session session = (Session) this.entityManager.getDelegate();
            SessionFactoryImplementor sfi = (SessionFactoryImplementor) session.getSessionFactory();
            ConnectionProvider cp = sfi.getConnectionProvider();
            connection = cp.getConnection();
            statement = connection.prepareStatement(this.selectNativeQuery);
            resultSet = statement.executeQuery();
            int columnCount = resultSet.getMetaData().getColumnCount();
            while (resultSet.next()) {
                for (int i = 1; i < columnCount + 1; i++) {
                    if (i > 1) {
                        fileWriter.write(DELIMITER);
                    }
                    fileWriter.write(resultSet.getString(i));
                }
                fileWriter.write("\n");
            }
        } finally {
            StreamUtil.safeClose(fileWriter);
            JDBCUtil.safeClose(connection, statement, resultSet);
        }
    }
}
