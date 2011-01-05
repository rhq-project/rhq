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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.naming.InitialContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

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
import org.rhq.helpers.perftest.support.Replicator;
import org.rhq.helpers.perftest.support.config.ExportConfiguration;
import org.rhq.helpers.perftest.support.dbsetup.DbSetup;
import org.rhq.helpers.perftest.support.input.FileInputStreamProvider;
import org.rhq.helpers.perftest.support.input.InputStreamProvider;
import org.rhq.helpers.perftest.support.jpa.HibernateFacade;
import org.rhq.helpers.perftest.support.replication.ReplicaDispenser;
import org.rhq.helpers.perftest.support.replication.ReplicaModifier;
import org.rhq.helpers.perftest.support.replication.ReplicaProvider;
import org.rhq.helpers.perftest.support.replication.ReplicationConfiguration;
import org.rhq.helpers.perftest.support.replication.ReplicationResult;

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

    private static class MethodRecord {
        public ReentrantLock lock;
        public ReplicaDispenser replicaDispenser;
    }
    
    private static final Log LOG = LogFactory.getLog(DatabaseSetupInterceptor.class);

    private static final HashMap<Method, MethodRecord> METHOD_RECORDS = new HashMap<Method, MethodRecord>();

    private static final Class<?>[] REPLICA_MODIFIER_METHOD_PARAMETER_TYPES = { int.class, Object.class, Object.class, Class.class };
    
    private static HibernateFacade hibernateFacade = new HibernateFacade();
    
    static {
        try {
            hibernateFacade.initialize(Collections.emptyMap());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize the Hibernate facade, this should not happen.", e);
        }
    }
    
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        MethodRecord methodRecord = null;
        boolean setupRanForThisMethod = false;

        //ensure no-one else is messing with the method locks before we're finished acquiring the lock
        //for our method
        synchronized (METHOD_RECORDS) {
            methodRecord = METHOD_RECORDS.get(methodRecord);
            setupRanForThisMethod = methodRecord != null;
            if (methodRecord == null) {
                methodRecord = new MethodRecord();
                methodRecord.lock = new ReentrantLock();
                METHOD_RECORDS.put(method.getTestMethod().getMethod(), methodRecord);
            }
        }

        methodRecord.lock.lock();

        String dbUrl = "-unknown-";
        Connection jdbcConnection = null;
        Statement statement = null;
        try {
            DatabaseState state = getRequiredDatabaseState(method);

            if (state == null) {
                return;
            }

            if (setupRanForThisMethod && !state.runForEachInvocation()) {
                methodRecord.replicaDispenser.setCurrentTestInvocationNumber(method.getTestMethod().getCurrentInvocationCount());
                ReplicaProvider.setDispenser(methodRecord.replicaDispenser);
                return;
            }

            Date now = new Date();
            System.out.println(">> beforeInvocation(DBInterceptor) " + method.getTestMethod().getMethodName() + " == "
                + now.getTime());

            try {
                InputStreamProvider streamProvider = getInputStreamProvider(state.url(), state.storage(), method);
                IDatabaseConnection connection = new DatabaseDataSourceConnection(new InitialContext(), "java:/RHQDS");

                jdbcConnection = connection.getConnection();
                dbUrl = jdbcConnection.getMetaData().getURL();
                System.out.println("Using database at " + dbUrl);
                System.out.flush();

                setDatabaseType(connection);

                //XXX remove these hacks once we know what's wrong with data importer
                try {
                    statement = jdbcConnection.createStatement();
                    statement.execute("DROP TABLE RHQ_SUBJECT CASCADE");
                } catch (SQLException e) {
                    System.out.println("Don't worry about : " + e.getMessage());
                } finally {
                    if (statement != null)
                        statement.close();
                }

                try {
                    statement = jdbcConnection.createStatement();
                    statement.execute("DROP TABLE RHQ_CONFIG CASCADE");
                } catch (SQLException e) {
                    System.out.println("Don't worry about : " + e.getMessage());
                } finally {
                    if (statement != null)
                        statement.close();
                }

                System.out.flush();

                FileFormat format = state.format();

                Input input = format.getInput(streamProvider);

                try {
                    DbSetup dbSetup = new DbSetup(connection.getConnection());
                    dbSetup.setup(state.dbVersion());
                    
                    Importer.run(connection, input);
                    
                    methodRecord.replicaDispenser = replicate(connection, method, testResult.getInstance(), state.replication());
                                                            
                    dbSetup.upgrade(null);
                } finally {
                    input.close();
                }
            } catch (Exception e) {
                LOG.warn("Failed to setup a database for method '" + method.getTestMethod().getMethodName() + "'.", e);
            }
        } finally {
            methodRecord.lock.unlock();
            if (statement != null)
                try {
                    statement.close();
                } catch (SQLException e) {
                    LOG.error("Failed to close a statement: " + e.getMessage());
                }
            if (jdbcConnection != null)
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
            if (major >= 10) {
                type = new org.dbunit.ext.oracle.Oracle10DataTypeFactory();
            } else {
                type = new org.dbunit.ext.oracle.OracleDataTypeFactory();
            }
        }
        if (type != null) {
            LOG.info("setting db type for dbunit to " + type.getClass().getCanonicalName());
            config.setProperty("http://www.dbunit.org/properties/datatypeFactory", type);
        }
    }

    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
        DatabaseState state = getRequiredDatabaseState(method);

        if (state == null) {
            return;
        }

        Connection jdbcConnection = null;
        Statement statement = null;
        try {
            IDatabaseConnection connection = new DatabaseDataSourceConnection(new InitialContext(), "java:/RHQDS");
            jdbcConnection = connection.getConnection();
            statement = jdbcConnection.createStatement();
            statement.execute("DROP TABLE RHQ_SUBJECT CASCADE");
        } catch (Exception e) {
            System.err.println("== drop subject table failed: " + e.getMessage());
        } finally {
            if (statement != null)
                try {
                    statement.close();
                } catch (SQLException e) {
                    LOG.error("Failed to close a statement: " + e.getMessage());
                }
            if (jdbcConnection != null)
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
        if (annotation == null) {
            boolean skip = false;

            // Filter out methods that are marked as setup/tear down
            Annotation[] annots = javaMethod.getAnnotations();
            for (Annotation an : annots) {
                if (an.annotationType().equals(BeforeMethod.class) || an.annotationType().equals(AfterMethod.class)
                    || an.annotationType().equals(BeforeSuite.class) || an.annotationType().equals(AfterSuite.class)
                    || an.annotationType().equals(BeforeTest.class) || an.annotationType().equals(AfterTest.class)) {
                    
                    skip = true;
                    break;
                }
            }

            if (!skip) {
                annotation = javaMethod.getDeclaringClass().getAnnotation(DatabaseState.class);
            }
        }
        return annotation;
    }

    private static InputStreamProvider getInputStreamProvider(final String url, FileStorage storage,
        final IInvokedMethod method) throws FileNotFoundException {
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
    
    private static ReplicaDispenser replicate(final IDatabaseConnection connection, IInvokedMethod method, final Object instance, DataReplication replicationSetup) throws Exception {
        ReplicationConfiguration config = new ReplicationConfiguration();
                
        InputStreamProvider configFileStream = getInputStreamProvider(replicationSetup.url(), replicationSetup.storage(), method);
        
        ExportConfiguration replicationConfiguration = getReplicationConfiguration(configFileStream);
        
        config.setReplicationConfiguration(replicationConfiguration);
        
        if (!replicationSetup.replicaModifier().isEmpty()) {
            final Method modifierMethod = findMethod(instance.getClass(), replicationSetup.replicaModifier(), REPLICA_MODIFIER_METHOD_PARAMETER_TYPES);
            if (modifierMethod != null) {
                config.setModifier(new ReplicaModifier() {
                    public void modify(Object original, Object replica, Class<?> clazz) {
                        try {
                            modifierMethod.invoke(original, replica, clazz);
                        } catch (InvocationTargetException e) {
                            LOG.error("Failed to invoke replica modifier method.", e);
                        } catch (IllegalAccessException e) {
                            LOG.error("Failed to invoke replica modifier method.", e);
                        }
                    }
                });
            }
        }
        
        List<ReplicationResult> results = new ArrayList<ReplicationResult>();
        int replicaCount = 0;
        switch (replicationSetup.replicaCreationStrategy()) {
        case PER_INVOCATION:
            replicaCount = method.getTestMethod().getInvocationCount();
            break;
        case PER_THREAD:
            replicaCount = method.getTestMethod().getThreadPoolSize();
            break;
        default:;
        }
        
        for(int i = 0; i < replicaCount; ++i) {
            results.add(Replicator.run(config, connection, hibernateFacade));
        }
                
        return new ReplicaDispenser(results, replicationSetup.replicaCreationStrategy());
    }
    
    private static Method findMethod(Class<?> type, String name, Class<?>... params) {
        try {
            return type.getMethod(name, params);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
    
    private static ExportConfiguration getReplicationConfiguration(InputStreamProvider input) {
        InputStream str = null;
        try {
            str = input.createInputStream();
            JAXBContext c = ExportConfiguration.getJAXBContext();
            Unmarshaller um = c.createUnmarshaller();
            return (ExportConfiguration) um.unmarshal(str);
        } catch (JAXBException e) {
            LOG.error("Failed to unmarshal replication configuration.", e);
            return null;
        } catch (IOException e) {
            LOG.error("Failed to unmarshal replication configuration.", e);
            return null;
        } finally {
            if (str != null) {
                try {
                    str.close();
                } catch (IOException e) {
                    LOG.error("Failed to close the input stream for replication configuration.", e);
                }
            }
        }
    }
}
