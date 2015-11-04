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

package org.rhq.enterprise.server.purge;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * A template for purging data tables.<br>
 * <br>
 * When the {@link #execute()} method is called, row keys are selected and stored in a file. Then the corresponding rows
 * are deleted in batches.
 *
 * @author Thomas Segismont
 */
abstract class PurgeTemplate<KEY extends Serializable> {
    private static final Log LOG = LogFactory.getLog(PurgeTemplate.class);

    private static final String BATCH_SIZE_SYSTEM_PROPERTY = "org.rhq.enterprise.server.purge.PurgeTemplate.BATCH_SIZE";
    private static final int BATCH_SIZE = Integer.getInteger(BATCH_SIZE_SYSTEM_PROPERTY, 30000);
    static {
        LOG.info(BATCH_SIZE_SYSTEM_PROPERTY + " = " + BATCH_SIZE);
    }

    private final DataSource dataSource;
    private final UserTransaction userTransaction;
    private final DatabaseType databaseType;

    /**
     * @param dataSource the source of JDBC connections to the database
     * @param userTransaction the transaction management interface
     */
    public PurgeTemplate(DataSource dataSource, UserTransaction userTransaction) {
        this.dataSource = dataSource;
        this.userTransaction = userTransaction;
        databaseType = DatabaseTypeFactory.getDefaultDatabaseType();
    }

    /**
     * @return the name of the data being purged, used for logging purpose
     */
    protected abstract String getEntityName();

    public int execute() {
        int deleted = 0;

        KeysInfo keysInfo = null;
        ObjectInputStream keysStream = null;
        try {

            keysInfo = loadKeys();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Loaded " + keysInfo.count + " key(s) of " + getEntityName());
            }

            keysStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(keysInfo.keysFile)));
            List<KEY> selectedKeys = new ArrayList<KEY>(BATCH_SIZE);

            for (int i = 1; i <= keysInfo.count; i++) {

                @SuppressWarnings("unchecked")
                KEY key = (KEY) keysStream.readObject();
                selectedKeys.add(key);

                if (selectedKeys.size() == BATCH_SIZE || i == keysInfo.count) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Deleting " + selectedKeys.size() + " row(s) of " + getEntityName());
                    }
                    deleted += deleteRows(selectedKeys);
                    selectedKeys.clear();
                }
            }

        } catch (Exception e) {
            LOG.error(getEntityName() + ": could not fully process the batched purge", e);
        } finally {
            rollbackIfTransactionActive();
            StreamUtil.safeClose(keysStream);
            if (keysInfo != null && keysInfo.keysFile != null) {
                keysInfo.keysFile.delete();
            }
        }

        return deleted;
    }

    private KeysInfo loadKeys() throws Exception {
        File keysFile = File.createTempFile(getClass().getSimpleName(), null);
        int count = 0;

        ObjectOutputStream objectOutputStream = null;
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {

            objectOutputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(keysFile)));

            userTransaction.begin();

            String findRowKeysQuery = getFindRowKeysQuery(databaseType);

            connection = dataSource.getConnection();
            preparedStatement = connection.prepareStatement(findRowKeysQuery);
            setFindRowKeysQueryParams(preparedStatement);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                if(count % BATCH_SIZE == 0) {
                    objectOutputStream.reset();
                }
                objectOutputStream.writeObject(getKeyFromResultSet(resultSet));
                count++;
            }

            userTransaction.commit();

        } finally {
            JDBCUtil.safeClose(connection, preparedStatement, resultSet);
            StreamUtil.safeClose(objectOutputStream);
            rollbackIfTransactionActive();
        }

        return new KeysInfo(keysFile, count);
    }

    /**
     * @return the query selecting row keys
     */
    protected abstract String getFindRowKeysQuery(DatabaseType databaseType);

    /**
     * Set the row keys selection query parameters.
     *
     * @param preparedStatement the prepared statement created for the row keys selection query
     *
     * @throws SQLException
     */
    protected abstract void setFindRowKeysQueryParams(PreparedStatement preparedStatement) throws SQLException;

    /**
     * @return the row key extracted from the <code>resultSet</code> columns values
     *
     * @throws SQLException
     */
    protected abstract KEY getKeyFromResultSet(ResultSet resultSet) throws SQLException;

    private int deleteRows(List<KEY> selectedKeys) throws Exception {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        try {

            userTransaction.begin();

            String deleteRowByKeyQuery = getDeleteRowByKeyQuery(databaseType);

            connection = dataSource.getConnection();
            preparedStatement = connection.prepareStatement(deleteRowByKeyQuery);

            for (KEY key : selectedKeys) {
                setDeleteRowByKeyQueryParams(preparedStatement, key);
                preparedStatement.addBatch();
            }

            int[] batchResults = preparedStatement.executeBatch();

            userTransaction.commit();

            return evalDeletedRows(batchResults);

        } finally {
            JDBCUtil.safeClose(connection, preparedStatement, null);
            rollbackIfTransactionActive();
        }
    }

    /**
     * @return the query deleting a row by key
     */
    protected abstract String getDeleteRowByKeyQuery(DatabaseType databaseType);

    /**
     * Set the deletion query parameters. Implementations should use the <code>key</code> provided.
     *
     * @param preparedStatement the prepared statement created for the row deletion query
     * @param key the row key object representation
     *
     * @throws SQLException
     */
    protected abstract void setDeleteRowByKeyQueryParams(PreparedStatement preparedStatement, KEY key)
        throws SQLException;

    private void rollbackIfTransactionActive() {
        try {
            if (userTransaction.getStatus() == Status.STATUS_ACTIVE) {
                userTransaction.rollback();
            }
        } catch (Throwable ignore) {
        }
    }

    private int evalDeletedRows(int[] results) {
        int total = 0, failed = 0;
        for (int result : results) {
            if (result == Statement.EXECUTE_FAILED) {
                failed++;
            } else if (result == Statement.SUCCESS_NO_INFO) {
                // Pre v12 Oracle servers return -2 because they don't track batch update counts
                total++;
            } else {
                total += result;
            }
        }
        if (failed > 0) {
            LOG.warn(getEntityName() + ": " + failed + " row(s) not purged");
        }
        return total;
    }

    private static class KeysInfo {
        final File keysFile;
        final int count;

        private KeysInfo(File keysFile, int count) {
            this.keysFile = keysFile;
            this.count = count;
        }
    }
}
