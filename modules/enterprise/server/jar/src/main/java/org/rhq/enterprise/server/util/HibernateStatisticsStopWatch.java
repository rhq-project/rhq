/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.util;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.persistence.EntityManager;

import org.hibernate.stat.Statistics;

import org.rhq.core.server.PersistenceUtility;

/**
 * @author Joseph Marques
 */
public class HibernateStatisticsStopWatch {

    private Statistics stats;

    long queryExecutions; // Get global number of executed queries
    long transations; // The number of transactions we know to have completed
    long entityLoads; // Get global number of entity loads
    long connects; //  Get the global number of connections asked by the sessions
    long time;

    public HibernateStatisticsStopWatch(EntityManager entityManager) {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        this.stats = PersistenceUtility.getStatisticsService(entityManager, platformMBeanServer);
    }

    public void start() {
        //stats.clear();
        queryExecutions = -stats.getQueryExecutionCount();
        transations = -stats.getTransactionCount();
        entityLoads = -stats.getEntityLoadCount();
        connects = -stats.getConnectCount();
        time = -System.currentTimeMillis();
    }

    public void stop() {
        queryExecutions += stats.getQueryExecutionCount();
        transations += stats.getTransactionCount();
        entityLoads += stats.getEntityLoadCount();
        connects += stats.getConnectCount();
        time += System.currentTimeMillis();
    }

    public Statistics getStats() {
        return stats;
    }

    public long getQueryExecutions() {
        return queryExecutions;
    }

    public long getTransations() {
        return transations;
    }

    public long getEntityLoads() {
        return entityLoads;
    }

    public long getConnects() {
        return connects;
    }

    public long getTime() {
        return time;
    }

    public String toString() {
        return "HibernateStats" //
            + "[ queries=" + queryExecutions //
            + ", xactions=" + transations //
            + ", loads=" + entityLoads //
            + ", connects=" + connects //
            + ", time=" + time + " ]";
    }
}
