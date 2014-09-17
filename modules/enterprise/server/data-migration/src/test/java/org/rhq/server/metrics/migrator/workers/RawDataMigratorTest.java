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

package org.rhq.server.metrics.migrator.workers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import com.datastax.driver.core.Query;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;

import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockObjectFactory;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;

import org.rhq.server.metrics.migrator.DataMigrator.DataMigratorConfiguration;
import org.rhq.server.metrics.migrator.DataMigrator.DatabaseType;
import org.rhq.server.metrics.migrator.datasources.ScrollableDataSource;


@PrepareForTest({ RawDataMigrator.class })
public class RawDataMigratorTest {

    @ObjectFactory
    public IObjectFactory getObjectFactory() {
        return new PowerMockObjectFactory();
    }

    @Test
    public void testEstimateTask() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion
        DatabaseType databaseType = DatabaseType.Oracle;
        DataMigratorConfiguration mockConfig = mock(DataMigratorConfiguration.class);
        when(mockConfig.getDatabaseType()).thenReturn(databaseType);

        MetricsIndexMigrator mockMetricsIndexUpdateAccumulator = mock(MetricsIndexMigrator.class);
        PowerMockito.whenNew(MetricsIndexMigrator.class).withArguments(eq(MigrationTable.RAW), eq(mockConfig))
            .thenReturn(mockMetricsIndexUpdateAccumulator);

        EntityManager mockEntityManager = mock(EntityManager.class);
        when(mockConfig.getEntityManager()).thenReturn(mockEntityManager);

        org.hibernate.Session mockHibernateSession = mock(org.hibernate.Session.class);
        when(mockEntityManager.getDelegate()).thenReturn(mockHibernateSession);
        SessionFactory mockSessionFactory = mock(SessionFactory.class);
        when(mockHibernateSession.getSessionFactory()).thenReturn(mockSessionFactory);
        StatelessSession mockStatelessSession = mock(StatelessSession.class);
        when(mockSessionFactory.openStatelessSession()).thenReturn(mockStatelessSession);

        org.hibernate.SQLQuery mockQuery = mock(org.hibernate.SQLQuery.class);
        when(mockStatelessSession.createSQLQuery(any(String.class))).thenReturn(mockQuery);

        when(mockQuery.uniqueResult()).thenReturn("1000");

        ScrollableDataSource mockDataSource = mock(ScrollableDataSource.class);
        PowerMockito.whenNew(ScrollableDataSource.class)
            .withArguments(eq(mockEntityManager), eq(databaseType), any(), anyInt()).thenReturn(mockDataSource);
        when(mockDataSource.getData(eq(0), anyInt())).thenReturn(new ArrayList<Object[]>());

        //create object to test and inject required dependencies
        RawDataMigrator objectUnderTest = new RawDataMigrator(mockConfig);

        //run code under test
        long estimateActual = objectUnderTest.estimate();

        //verify the results (assert and mock verification)
        PowerMockito.verifyNew(MetricsIndexMigrator.class).withArguments(eq(MigrationTable.RAW), eq(mockConfig));
        PowerMockito.verifyNew(ScrollableDataSource.class, times(15)).withArguments(eq(mockEntityManager),
            eq(databaseType), any(), anyInt());

        verify(mockStatelessSession, times(15)).createSQLQuery(any(String.class));
        verify(mockDataSource, times(15)).initialize();
        verify(mockDataSource, times(15)).getData(eq(0), anyInt());
        verify(mockDataSource, times(15)).close();
        verify(mockMetricsIndexUpdateAccumulator, times(1)).drain();

        verifyNoMoreInteractions(mockDataSource);
        verifyNoMoreInteractions(mockMetricsIndexUpdateAccumulator);

        Assert.assertNotEquals(estimateActual, 0);
    }

    @Test
    public void testMigrateTask() throws Exception {
        //tell the method story as it happens: mock or create dependencies and configure
        //those dependencies to get the method under test to completion
        DatabaseType databaseType = DatabaseType.Oracle;
        DataMigratorConfiguration mockConfig = mock(DataMigratorConfiguration.class);
        when(mockConfig.getDatabaseType()).thenReturn(databaseType);

        Session mockCassandraSession = mock(Session.class);
        when(mockConfig.getSession()).thenReturn(mockCassandraSession);

        MetricsIndexMigrator mockMetricsIndexUpdateAccumulator = mock(MetricsIndexMigrator.class);
        PowerMockito.whenNew(MetricsIndexMigrator.class).withArguments(eq(MigrationTable.RAW), eq(mockConfig))
            .thenReturn(mockMetricsIndexUpdateAccumulator);

        EntityManager mockEntityManager = mock(EntityManager.class);
        when(mockConfig.getEntityManager()).thenReturn(mockEntityManager);

        org.hibernate.Session mockHibernateSession = mock(org.hibernate.Session.class);
        when(mockEntityManager.getDelegate()).thenReturn(mockHibernateSession);
        SessionFactory mockSessionFactory = mock(SessionFactory.class);
        when(mockHibernateSession.getSessionFactory()).thenReturn(mockSessionFactory);
        StatelessSession mockStatelessSession = mock(StatelessSession.class);
        when(mockSessionFactory.openStatelessSession()).thenReturn(mockStatelessSession);

        org.hibernate.SQLQuery mockQuery = mock(org.hibernate.SQLQuery.class);
        when(mockStatelessSession.createSQLQuery(any(String.class))).thenReturn(mockQuery);

        when(mockQuery.uniqueResult()).thenReturn("1000");

        ScrollableDataSource mockDataSource = mock(ScrollableDataSource.class);
        PowerMockito.whenNew(ScrollableDataSource.class).withArguments(eq(mockEntityManager), eq(databaseType), any())
            .thenReturn(mockDataSource);

        List<Object[]> resultList = new ArrayList<Object[]>();
        resultList.add(new Object[] { 100, 100, 100 });
        resultList.add(new Object[] { 100, System.currentTimeMillis() - 100l, 100 });

        for (int index = 0; index < 15; index++) {
            when(mockDataSource.getData(eq(0), anyInt())).thenReturn(resultList);
            when(mockDataSource.getData(eq(2), anyInt())).thenReturn(new ArrayList<Object[]>());
        }

        ResultSetFuture mockResultSetFuture = mock(ResultSetFuture.class);
        when(mockCassandraSession.executeAsync(any(Query.class))).thenReturn(mockResultSetFuture);

        //create object to test and inject required dependencies
        RawDataMigrator objectUnderTest = new RawDataMigrator(mockConfig);

        //run code under test
        objectUnderTest.migrate();

        //verify the results (assert and mock verification)
        PowerMockito.verifyNew(MetricsIndexMigrator.class).withArguments(eq(MigrationTable.RAW), eq(mockConfig));
        PowerMockito.verifyNew(ScrollableDataSource.class, times(15)).withArguments(eq(mockEntityManager),
            eq(databaseType), any());

        verify(mockDataSource, times(15)).initialize();
        verify(mockDataSource, times(15)).getData(eq(0), anyInt());
        verify(mockDataSource, times(15)).getData(eq(2), anyInt());
        verify(mockDataSource, times(15)).close();

        verify(mockMetricsIndexUpdateAccumulator, times(15)).add(eq(100), anyInt());
        verify(mockMetricsIndexUpdateAccumulator, times(1)).drain();

        verify(mockCassandraSession, times(15)).executeAsync(any(Query.class));
        verify(mockResultSetFuture, times(15)).get();

        verifyNoMoreInteractions(mockDataSource);
        verifyNoMoreInteractions(mockCassandraSession);
        verifyNoMoreInteractions(mockResultSetFuture);
        verifyNoMoreInteractions(mockMetricsIndexUpdateAccumulator);
    }
}

