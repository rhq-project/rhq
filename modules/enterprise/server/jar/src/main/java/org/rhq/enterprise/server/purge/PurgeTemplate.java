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

/**
 * A template for purging data tables.<br>
 * <br>
 * When the {@link #execute()} method is called, a range of row keys are selected. Then the corresponding rows are
 * deleted. The process is repeated until no rows matching the criteria exist.<br>
 * <br>
 * Each iteration of the process runs in its own transation.
 *
 * @author Thomas Segismont
 */
abstract class PurgeTemplate<KEY> {
    private static final Log LOG = LogFactory.getLog(PurgeTemplate.class);

    private static final String BATCH_SIZE_SYSTEM_PROPERTY = "org.rhq.enterprise.server.purge.PurgeTemplate.BATCH_SIZE";
    private static final int BATCH_SIZE = Integer.getInteger(BATCH_SIZE_SYSTEM_PROPERTY, 3000);
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

    /**
     * @return the maximum number of rows to delete inside a single transaction
     */
    public int getBatchSize() {
        return BATCH_SIZE;
    }

    public int execute() {
        int deleted = 0;
        try {

            for (;;) {

                userTransaction.begin();

                List<KEY> selectedKeys = findRowKeys();
                if (selectedKeys.isEmpty()) {
                    userTransaction.rollback();
                    break;
                }

                deleted += deleteRows(selectedKeys);

                userTransaction.commit();

                if (selectedKeys.size() < BATCH_SIZE) {
                    break;
                }

            }

        } catch (Exception e) {
            LOG.error(getEntityName() + ": could not fully process the batched purge", e);
        } finally {
            try {
                if (userTransaction.getStatus() == Status.STATUS_ACTIVE) {
                    userTransaction.rollback();
                }
            } catch (Throwable ignore) {
            }
        }

        return deleted;
    }

    private List<KEY> findRowKeys() throws Exception {
        List<KEY> selectedKeys = new ArrayList<KEY>(getBatchSize());

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {

            String findRowKeysQuery = getFindRowKeysQuery(databaseType);

            connection = dataSource.getConnection();
            preparedStatement = connection.prepareStatement(findRowKeysQuery);
            setFindRowKeysQueryParams(preparedStatement);

            resultSet = preparedStatement.executeQuery();

            while (resultSet.next() && selectedKeys.size() < getBatchSize()) {
                selectedKeys.add(getKeyFromResultSet(resultSet));
            }

            return selectedKeys;

        } finally {
            JDBCUtil.safeClose(connection, preparedStatement, resultSet);
        }
    }

    private String getFindRowKeysQuery(DatabaseType databaseType) {
        if (DatabaseTypeFactory.isPostgres(databaseType)) {
            return getFindRowKeysQueryPostgres();
        } else if (DatabaseTypeFactory.isOracle(databaseType)) {
            return getFindRowKeysQueryOracle();
        }
        throw new UnsupportedOperationException(databaseType.getName());
    }

    /**
     * @return the query selecting row keys on Postgres servers
     */
    protected abstract String getFindRowKeysQueryPostgres();

    /**
     * @return the query selecting row keys on Oracle servers
     */
    protected abstract String getFindRowKeysQueryOracle();

    /**
     * Set the row keys selection query parameters. Implementations should use the {@link #getBatchSize()} method here.
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

            String deleteRowByKeyQuery = getDeleteRowByKeyQuery(databaseType);

            connection = dataSource.getConnection();
            preparedStatement = connection.prepareStatement(deleteRowByKeyQuery);

            for (KEY key : selectedKeys) {
                setDeleteRowByKeyQueryParams(preparedStatement, key);
                preparedStatement.addBatch();
            }

            int[] batchResults = preparedStatement.executeBatch();

            return evalDeletedRows(batchResults);

        } finally {
            JDBCUtil.safeClose(connection, preparedStatement, null);
        }
    }

    private String getDeleteRowByKeyQuery(DatabaseType databaseType) {
        if (DatabaseTypeFactory.isPostgres(databaseType)) {
            return getDeleteRowByKeyQueryPostgres();
        } else if (DatabaseTypeFactory.isOracle(databaseType)) {
            return getDeleteRowByKeyQueryOracle();
        }
        throw new UnsupportedOperationException(databaseType.getName());
    }

    /**
     * @return the query deleting a row by key on Postgres servers
     */
    protected abstract String getDeleteRowByKeyQueryPostgres();

    /**
     * @return the query deleting a row by key on Oracle servers
     */
    protected abstract String getDeleteRowByKeyQueryOracle();

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
}
