/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support.testng;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseDataSourceConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;

import org.rhq.helpers.perftest.support.FileFormat;
import org.rhq.helpers.perftest.support.Importer;
import org.rhq.helpers.perftest.support.Input;
import org.rhq.helpers.perftest.support.dbsetup.DbSetup;
import org.rhq.helpers.perftest.support.input.FileInputStreamProvider;
import org.rhq.helpers.perftest.support.input.InputStreamProvider;

/**
 * An {@link IInvokedMethodListener method listener} that performs the database setup
 * for appropriately annotated test methods.
 * To add database setup support to a test class, annotate the class with
 * <code>
 * &#64;Listeners({org.rhq.helpers.perftest.support.testng.DatabaseSetupInterceptor.class})
 * </code>
 *
 * @author Lukas Krejci
 * @author Heiko W. Rupp
 */
public class DatabaseSetupInterceptor implements IInvokedMethodListener {

    private static final Log LOG = LogFactory.getLog(DatabaseSetupInterceptor.class);

    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {


        DatabaseState state = getRequiredDatabaseState(method);

        if (state == null) {
            return;
        }

        String dbUrl="-unknown-" ;
        Connection jdbcConnection = null;
        Statement statement = null;
        try {
            InputStreamProvider streamProvider = getInputStreamProvider(state.url(), state.storage(), method);
            IDatabaseConnection connection = new DatabaseDataSourceConnection(new InitialContext(),
                "java:jboss/datasources/RHQDS");
            jdbcConnection = connection.getConnection();
            dbUrl = jdbcConnection.getMetaData().getURL();
            System.out.println("Using database at " + dbUrl);
            System.out.flush();

            setDatabaseType(connection);

            try {
                statement = jdbcConnection.createStatement();
                statement.execute("DROP TABLE RHQ_SUBJECT CASCADE");
            } catch (SQLException e) {
                System.out.println("Don't worry about : " + e.getMessage());
            } finally {
                if (statement!=null)
                    statement.close();
            }

            try {
                statement = jdbcConnection.createStatement();
                statement.execute("DROP TABLE RHQ_CONFIG CASCADE");
            } catch (SQLException e) {
                System.out.println("Don't worry about : " + e.getMessage());
            } finally {
                if (statement!=null)
                    statement.close();
            }


            System.out.flush();
            FileFormat format = state.format();

            Input input = format.getInput(streamProvider);

            try {
                DbSetup dbSetup = new DbSetup(jdbcConnection);
                dbSetup.setup(state.dbVersion());
                Importer.run(connection, input);
                dbSetup.upgrade(null);
            } finally {
                input.close();
            }
        } catch (Exception e) {
            LOG.warn("Failed to setup a database at [ " + dbUrl + "] for method '" + method.getTestMethod().getMethodName() + "'.", e);
        }
        finally {
            if (statement!=null)
                try {
                    statement.close();
                } catch (SQLException e) {
                    LOG.error("Failed to close a statement: " + e.getMessage());
                }
            if (jdbcConnection!=null)
                try {
                    jdbcConnection.close();
                } catch (SQLException e) {
                    LOG.error("Failed to close a JDBC connetion: " + e.getMessage());
                }
        }
        System.out.flush();
        System.err.flush();
    }

    private void setDatabaseType(IDatabaseConnection connection) throws SQLException {
        DatabaseConfig config = connection.getConfig();
        String name = connection.getConnection().getMetaData().getDatabaseProductName().toLowerCase();
        int major = connection.getConnection().getMetaData().getDatabaseMajorVersion();
        IDataTypeFactory type = null;
        if (name.contains("postgres")) {
            type = new org.dbunit.ext.postgresql.PostgresqlDataTypeFactory();
        } else if (name.contains("oracle")) {
            if (major>=10) {
                type = new org.dbunit.ext.oracle.Oracle10DataTypeFactory();
            } else {
                type = new org.dbunit.ext.oracle.OracleDataTypeFactory();
            }
        }
        if (type!=null) {
            LOG.info("setting db type for dbunit to " + type.getClass().getCanonicalName());
            config.setProperty("http://www.dbunit.org/properties/datatypeFactory",type);
        }
    }

/*  Dropping the tables explicitly is a workaround around a problem in the perf tests
 *  but otherwise it should not be necessary at all.
 *  Even then, dropping the table in the beforeInvocation() should be enough and we
 *  shouldn't have to drop it again in the afterInvocation(). That is going to leave
 *  the database corrupted because it is going to miss this table altogether.
 */
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        DatabaseState state = getRequiredDatabaseState(method);

        if (state == null) {
            return;
        }

        Connection jdbcConnection=null;
        Statement statement=null;
        try {
            IDatabaseConnection connection = new DatabaseDataSourceConnection(new InitialContext(),
                "java:jboss/datasources/RHQDS");
            jdbcConnection = connection.getConnection();
            statement = jdbcConnection.createStatement();
            statement.execute("DROP TABLE RHQ_SUBJECT CASCADE");
        } catch (Exception e) {
            System.err.println("== drop subject table failed: " + e.getMessage());
        } finally {
            if (statement!=null)
                try {
                    statement.close();
                } catch (SQLException e) {
                   LOG.error("Failed to close a statement: " + e.getMessage());
                }
            if (jdbcConnection!=null)
                try {
                    jdbcConnection.close();
                } catch (SQLException e) {
                    LOG.error("Failed to close a JDBC connection: " + e.getMessage());
                }
        }


    }
    
    /**
     * Obtain the required database state by looking for the @DatabaseState annotation.
     * Lookup is first done at method level and if not found done at class level.
     * @param method Method that TestNG is about to invoke
     * @return the desired database state or null if @DatabaseState is not given.
     * @see org.rhq.helpers.perftest.support.testng.DatabaseState
     */
    private static DatabaseState getRequiredDatabaseState(IInvokedMethod method) {
        Method javaMethod = method.getTestMethod().getMethod();

        DatabaseState annotation = javaMethod.getAnnotation(DatabaseState.class);
        if (annotation==null) {
//            System.out.println("Method : " + javaMethod.getName());

            boolean skip = false;

            // Filter out methods that are marked as setup/tear down
            Annotation[] annots = javaMethod.getAnnotations();
            for (Annotation an : annots) {
//                System.out.println("       :  " + an.toString());
                if (an.annotationType().equals(BeforeMethod.class) || an.annotationType().equals(AfterMethod.class) ||
                        an.annotationType().equals(BeforeSuite.class) || an.annotationType().equals(AfterSuite.class) ||
                        an.annotationType().equals(BeforeTest.class) || an.annotationType().equals(AfterTest.class)
                )
                    skip = true;
            }

            if (!skip)
                annotation = javaMethod.getDeclaringClass().getAnnotation(DatabaseState.class);
//            else
//                System.out.println("      ..... Skipped");

        }
        return annotation;
    }


    private static InputStreamProvider getInputStreamProvider(final String url, DatabaseStateStorage storage, final IInvokedMethod method)
        throws FileNotFoundException {
        switch (storage) {
        case CLASSLOADER:
            return new InputStreamProvider() {
                public InputStream createInputStream() throws IOException {
                    ClassLoader cl = method.getTestMethod().getMethod().getDeclaringClass().getClassLoader();
                    return cl.getResourceAsStream(url);
                }
            };
        case FILESYSTEM:
            return new FileInputStreamProvider(new File(url));
        default:
            return null;
        }
    }
}
