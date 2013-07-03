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
