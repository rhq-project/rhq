package org.rhq.server.metrics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hibernate.ejb.Ejb3Configuration;

import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author Thomas Segismont
 */
public class ExistingDataJPABulkExportSource extends ExistingDataBulkExportSource {

    public ExistingDataJPABulkExportSource(EntityManager entityManager, String selectNativeQuery, File workDirectory,
        String fileName) {
        super(entityManager, selectNativeQuery, workDirectory, fileName);
    }

    public void exportExistingData() throws Exception {
        BufferedWriter fileWriter = null;
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            fileWriter = new BufferedWriter(new FileWriter(getExistingDataFile()), IO_BUFFER_SIZE);
            connection = getConnection();
            statement = connection.prepareStatement(getSelectNativeQuery());
            int columnCount = resultSet.getMetaData().getColumnCount();
            resultSet = statement.executeQuery();
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

    public static void main(String[] args) throws Exception {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        Logger.getLogger("org.rhq").setLevel(Level.DEBUG);
        EntityManagerFactory entityManagerFactory = null;
        EntityManager entityManager = null;
        ExistingDataBulkExportSource source = null;
        try {
            entityManagerFactory = createEntityManager();
            entityManager = entityManagerFactory.createEntityManager();
            source = new ExistingPostgresDataBulkExportSource(entityManager,
                "SELECT  schedule_id, time_stamp, value, minvalue, maxvalue FROM RHQ_MEASUREMENT_DATA_NUM_1D",
                // "SELECT  count(*) FROM RHQ_MEASUREMENT_DATA_NUM_1D limit 1"
                new File(System.getProperty("java.io.tmpdir")), "poupoupidou.txt");
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            source.exportExistingData();
            source.startReading();
            int rowIndex = 0;
            int maxResults = 30000;
            for (;;) {
                List<Object[]> existingData = source.getExistingData(rowIndex, maxResults);
                if (existingData.size() < maxResults) {
                    break;
                } else {
                    rowIndex += maxResults;
                }
            }
            stopWatch.stop();
            System.out.println("Execution: " + stopWatch);
        } finally {
            if (source != null) {
                source.stopReading();
            }
            if (entityManager != null) {
                entityManager.close();
            }
            if (entityManagerFactory != null) {
                entityManagerFactory.close();
            }
        }
    }

    private static EntityManagerFactory createEntityManager() throws Exception {
        Properties properties = new Properties();
        properties.put("javax.persistence.provider", "org.hibernate.ejb.HibernatePersistence");
        properties.put("hibernate.connection.username", "rhqadmin");
        properties.put("hibernate.connection.password", "rhqadmin");
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        String driverClassName = "org.postgresql.Driver";
        try {
            //Required to preload the driver manually.
            //Without this the driver load will fail due to the packaging.
            Class.forName(driverClassName);
        } catch (ClassNotFoundException e) {
            throw new Exception("Postgres SQL Driver class could not be loaded. Missing class: " + driverClassName);
        }
        properties.put("hibernate.driver_class", driverClassName);
        properties.put("hibernate.connection.url", "jdbc:postgresql://localhost:5432/rhqdev");
        Ejb3Configuration configuration = new Ejb3Configuration();
        configuration.setProperties(properties);
        return configuration.buildEntityManagerFactory();
    }

}
