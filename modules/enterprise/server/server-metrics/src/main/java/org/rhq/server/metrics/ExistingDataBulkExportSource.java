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

package org.rhq.server.metrics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.jdbc.connections.spi.ConnectionProvider;

import org.rhq.core.util.stream.StreamUtil;

/**
 * @author Thomas Segismont
 */
public abstract class ExistingDataBulkExportSource implements ExistingDataSource {

    private static final Log LOG = LogFactory.getLog(ExistingDataBulkExportSource.class);

    protected static final int IO_BUFFER_SIZE = 1024 * 64;

    protected static final String DELIMITER = "|";

    private EntityManager entityManager;

    private String selectNativeQuery;

    private File workDirectory;

    private String fileName;

    private File existingDataFile;

    private BufferedReader existingDataFileReader;

    private int currentIndex;

    public ExistingDataBulkExportSource(EntityManager entityManager, String selectNativeQuery, File workDirectory,
        String fileName) {
        this.entityManager = entityManager;
        this.selectNativeQuery = selectNativeQuery;
        this.workDirectory = workDirectory;
        this.fileName = fileName;
        existingDataFile = new File(workDirectory, fileName);
    }

    protected String getSelectNativeQuery() {
        return selectNativeQuery;
    }

    protected File getExistingDataFile() {
        return existingDataFile;
    }

    protected Connection getConnection() throws SQLException {
        Session session = (Session) entityManager.getDelegate();
        SessionFactoryImplementor sfi = (SessionFactoryImplementor) session.getSessionFactory();
        ConnectionProvider cp = sfi.getConnectionProvider();
        return cp.getConnection();
    }

    public abstract void exportExistingData() throws Exception;

    public void startReading() throws Exception {
        if (!existingDataFile.exists() && !existingDataFile.isFile() && !existingDataFile.canRead()) {
            throw new IllegalStateException();
        }
        existingDataFileReader = new BufferedReader(new FileReader(existingDataFile));
        currentIndex = 0;
    }

    public void stopReading() {
        StreamUtil.safeClose(existingDataFileReader);
    }

    @Override
    public List<Object[]> getExistingData(int fromIndex, int maxResults) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading lines " + fromIndex + "-" + (fromIndex + maxResults));
        }
        if (fromIndex != currentIndex) {
            throw new IllegalStateException();
        }
        List<Object[]> results = new LinkedList<Object[]>();
        for (int i = 0; i < maxResults; i++) {
            String nextLine = existingDataFileReader.readLine();
            if (nextLine == null) {
                break;
            }
            currentIndex++;
            StringTokenizer stringTokenizer = new StringTokenizer(nextLine, DELIMITER);
            Object[] row = new Object[stringTokenizer.countTokens()];
            for (int j = 0; j < row.length; j++) {
                row[j] = stringTokenizer.nextToken();
            }
            results.add(row);
        }
        return results;
    }

}
