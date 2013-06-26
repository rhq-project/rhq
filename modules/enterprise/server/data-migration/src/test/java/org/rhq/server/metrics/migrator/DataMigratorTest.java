/*
 * RHQ Management Platform
 * Copyright 2013, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.rhq.server.metrics.migrator;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import javax.persistence.EntityManager;

import com.datastax.driver.core.Session;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import org.rhq.server.metrics.migrator.DataMigrator.DataMigratorConfiguration;
import org.rhq.server.metrics.migrator.DataMigrator.DatabaseType;
import org.rhq.server.metrics.migrator.workers.AggregateDataMigrator;
import org.rhq.server.metrics.migrator.workers.RawDataMigrator;

@PrepareForTest({ DataMigrator.class, DataMigratorConfiguration.class, RawDataMigrator.class })
public class DataMigratorTest {

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @Test
    public void testSetup() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion
        DatabaseType databaseType = DatabaseType.Postgres;
        EntityManager mockEntityManager = mock(EntityManager.class);
        Session mockCassandraSession = mock(Session.class);

        DataMigratorConfiguration mockConfig = PowerMockito.mock(DataMigratorConfiguration.class);
        PowerMockito.whenNew(DataMigratorConfiguration.class)
            .withArguments(eq(mockEntityManager), eq(mockCassandraSession), eq(databaseType), eq(false)).thenReturn(mockConfig);

        //create object to test and inject required dependencies
        DataMigrator objectUnderTest = new DataMigrator(mockEntityManager, mockCassandraSession, databaseType);

        //run code under test
        objectUnderTest.runRawDataMigration(false);
        objectUnderTest.runRawDataMigration(true);

        objectUnderTest.run1HAggregateDataMigration(false);
        objectUnderTest.run1HAggregateDataMigration(true);

        objectUnderTest.run6HAggregateDataMigration(false);
        objectUnderTest.run6HAggregateDataMigration(true);

        objectUnderTest.run1DAggregateDataMigration(false);
        objectUnderTest.run1DAggregateDataMigration(true);


        //verify the results (assert and mock verification)
        PowerMockito.verifyNew(DataMigratorConfiguration.class).withArguments(eq(mockEntityManager), eq(mockCassandraSession),
            eq(databaseType), eq(false));
        PowerMockito.verifyPrivate(mockConfig, times(2)).invoke("setRunRawDataMigration", true);
        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setRunRawDataMigration", false);

        PowerMockito.verifyPrivate(mockConfig, times(2)).invoke("setRun1HAggregateDataMigration", true);
        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setRun1HAggregateDataMigration", false);

        PowerMockito.verifyPrivate(mockConfig, times(2)).invoke("setRun6HAggregateDataMigration", true);
        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setRun6HAggregateDataMigration", false);

        PowerMockito.verifyPrivate(mockConfig, times(2)).invoke("setRun1DAggregateDataMigration", true);
        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setRun1DAggregateDataMigration", false);

        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setDeleteDataImmediatelyAfterMigration", false);
        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setDeleteAllDataAtEndOfMigration", false);

        PowerMockito.verifyNoMoreInteractions(mockConfig);

        verifyNoMoreInteractions(mockEntityManager);
        verifyNoMoreInteractions(mockCassandraSession);
    }

    @Test
    public void testEstimateTask() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion
        DatabaseType databaseType = DatabaseType.Postgres;

        EntityManager mockEntityManager = mock(EntityManager.class);
        Session mockCassandraSession = mock(Session.class);
        DataMigratorConfiguration mockConfig = PowerMockito.mock(DataMigratorConfiguration.class);
        PowerMockito.whenNew(DataMigratorConfiguration.class)
            .withArguments(eq(mockEntityManager), eq(mockCassandraSession), eq(databaseType), eq(false)).thenReturn(mockConfig);

        when(mockConfig.isRunRawDataMigration()).thenReturn(true);
        when(mockConfig.isRun1HAggregateDataMigration()).thenReturn(false);
        when(mockConfig.isRun6HAggregateDataMigration()).thenReturn(false);
        when(mockConfig.isRun1DAggregateDataMigration()).thenReturn(false);

        RawDataMigrator mockRawDataMigrator = mock(RawDataMigrator.class);
        PowerMockito.whenNew(RawDataMigrator.class).withArguments(eq(mockConfig)).thenReturn(mockRawDataMigrator);
        long estimateExpected = 1234L;
        when(mockRawDataMigrator.estimate()).thenReturn(estimateExpected);

        //create object to test and inject required dependencies
        DataMigrator objectUnderTest = new DataMigrator(mockEntityManager, mockCassandraSession, databaseType);

        //run code under test
        objectUnderTest.run1HAggregateDataMigration(false);
        objectUnderTest.run6HAggregateDataMigration(false);
        objectUnderTest.run1DAggregateDataMigration(false);

        long estimateActual = objectUnderTest.estimate();

        //verify the results (assert and mock verification)
        PowerMockito.verifyNew(DataMigratorConfiguration.class).withArguments(eq(mockEntityManager), eq(mockCassandraSession),
            eq(databaseType), eq(false));

        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setRunRawDataMigration", true);

        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setRun1HAggregateDataMigration", true);
        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setRun1HAggregateDataMigration", false);

        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setRun6HAggregateDataMigration", true);
        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setRun6HAggregateDataMigration", false);

        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setRun1DAggregateDataMigration", true);
        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setRun1DAggregateDataMigration", false);

        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setDeleteDataImmediatelyAfterMigration", false);
        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setDeleteAllDataAtEndOfMigration", false);

        verify(mockConfig, times(1)).isRunRawDataMigration();
        verify(mockConfig, times(1)).isRun1HAggregateDataMigration();
        verify(mockConfig, times(1)).isRun6HAggregateDataMigration();
        verify(mockConfig, times(1)).isRun1DAggregateDataMigration();
        verify(mockConfig, times(1)).isDeleteAllDataAtEndOfMigration();

        PowerMockito.verifyNoMoreInteractions(mockConfig);

        PowerMockito.verifyNew(RawDataMigrator.class).withArguments(eq(mockConfig));
        verify(mockRawDataMigrator, times(1)).estimate();
        verifyNoMoreInteractions(mockRawDataMigrator);

        verifyNoMoreInteractions(mockEntityManager);
        verifyNoMoreInteractions(mockCassandraSession);

        Assert.assertEquals(estimateActual, (long) (estimateExpected + estimateExpected * .15));
    }

    @Test
    public void testMigrateTask() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion
        DatabaseType databaseType = DatabaseType.Postgres;

        EntityManager mockEntityManager = mock(EntityManager.class);
        Session mockCassandraSession = mock(Session.class);
        DataMigratorConfiguration mockConfig = PowerMockito.mock(DataMigratorConfiguration.class);
        PowerMockito.whenNew(DataMigratorConfiguration.class)
            .withArguments(eq(mockEntityManager), eq(mockCassandraSession), eq(databaseType), eq(false)).thenReturn(mockConfig);

        when(mockConfig.isRunRawDataMigration()).thenReturn(false);
        when(mockConfig.isRun1HAggregateDataMigration()).thenReturn(true);
        when(mockConfig.isRun6HAggregateDataMigration()).thenReturn(true);
        when(mockConfig.isRun1DAggregateDataMigration()).thenReturn(true);

        AggregateDataMigrator mockAggregateDataMigrator = mock(AggregateDataMigrator.class);
        PowerMockito.whenNew(AggregateDataMigrator.class).withArguments(any(), eq(mockConfig))
            .thenReturn(mockAggregateDataMigrator);

        //create object to test and inject required dependencies
        DataMigrator objectUnderTest = new DataMigrator(mockEntityManager, mockCassandraSession, databaseType);

        //run code under test
        objectUnderTest.runRawDataMigration(false);
        objectUnderTest.migrateData();

        //verify the results (assert and mock verification)
        PowerMockito.verifyNew(DataMigratorConfiguration.class).withArguments(eq(mockEntityManager), eq(mockCassandraSession),
            eq(databaseType), eq(false));

        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setRunRawDataMigration", true);
        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setRunRawDataMigration", false);

        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setRun1HAggregateDataMigration", true);
        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setRun6HAggregateDataMigration", true);
        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setRun1DAggregateDataMigration", true);

        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setDeleteDataImmediatelyAfterMigration", false);
        PowerMockito.verifyPrivate(mockConfig, times(1)).invoke("setDeleteAllDataAtEndOfMigration", false);

        verify(mockConfig, times(1)).isRunRawDataMigration();
        verify(mockConfig, times(1)).isRun1HAggregateDataMigration();
        verify(mockConfig, times(1)).isRun6HAggregateDataMigration();
        verify(mockConfig, times(1)).isRun1DAggregateDataMigration();
        verify(mockConfig, times(1)).isDeleteAllDataAtEndOfMigration();

        PowerMockito.verifyNoMoreInteractions(mockConfig);

        PowerMockito.verifyNew(AggregateDataMigrator.class, times(3)).withArguments(any(), eq(mockConfig));
        verify(mockAggregateDataMigrator, times(3)).migrate();
        verifyNoMoreInteractions(mockAggregateDataMigrator);

        verifyNoMoreInteractions(mockEntityManager);
        verifyNoMoreInteractions(mockCassandraSession);
    }
}

