/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.server.metrics.migrator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.Connection;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;

import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author Thomas Segismont
 */
public class ExistingPostgresDataBulkExportSource extends ExistingDataBulkExportSource {

    private final EntityManager entityManager;
    private final String selectNativeQuery;
    private final int maxResults;

    public ExistingPostgresDataBulkExportSource(EntityManager entityManager, String selectNativeQuery) {
        this(entityManager, selectNativeQuery, -1);
    }

    public ExistingPostgresDataBulkExportSource(EntityManager entityManager, String selectNativeQuery, int maxResults) {
        super();
        this.entityManager = entityManager;
        this.selectNativeQuery = selectNativeQuery;
        this.maxResults = maxResults;
    }

    protected void exportExistingData() throws Exception {
        BufferedWriter fileWriter = null;
        Connection connection = null;
        try {
            fileWriter = new BufferedWriter(new FileWriter(getExistingDataFile()), IO_BUFFER_SIZE);
            Session session = (Session) entityManager.getDelegate();
            SessionFactoryImplementor sfi = (SessionFactoryImplementor) session.getSessionFactory();
            ConnectionProvider cp = sfi.getConnectionProvider();
            connection = cp.getConnection();
            CopyManager copyManager = new CopyManager((BaseConnection) connection);

            if (maxResults > 0) {
                copyManager.copyOut("COPY (" + selectNativeQuery + " LIMIT " + maxResults
                    + ") TO STDOUT WITH DELIMITER '" + DELIMITER + "'", fileWriter);
            } else {
                copyManager.copyOut("COPY (" + selectNativeQuery + ") TO STDOUT WITH DELIMITER '" + DELIMITER + "'",
                    fileWriter);
            }
        } finally {
            StreamUtil.safeClose(fileWriter);
            JDBCUtil.safeClose(connection);
        }
    }
}
