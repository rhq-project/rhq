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
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.persistence.EntityManager;

import org.hibernate.stat.Statistics;

import org.rhq.core.domain.util.PersistenceUtility;

/**
 * @author Joseph Marques
 */
public class HibernateStatisticsStopWatch {

    private EntityManager entityManager;

    private Map<String, Long> values;

    public HibernateStatisticsStopWatch(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void start() {
        values = new HashMap<String, Long>();

        Statistics stats = getStats();
        values.put("QueryExecutionCount", stats.getQueryExecutionCount());
        values.put("TransactionCount", stats.getTransactionCount());
        values.put("EntityLoadCount", stats.getEntityLoadCount());
        values.put("ConnectCount", stats.getConnectCount());
        values.put("Time", System.currentTimeMillis());
    }

    public void stop() {
        Statistics stats = getStats();
        values.put("QueryExecutionCount", stats.getQueryExecutionCount() - values.get("QueryExecutionCount"));
        values.put("TransactionCount", stats.getTransactionCount() - values.get("TransactionCount"));
        values.put("EntityLoadCount", stats.getEntityLoadCount() - values.get("EntityLoadCount"));
        values.put("ConnectCount", stats.getConnectCount() - values.get("ConnectCount"));
        values.put("Time", System.currentTimeMillis() - values.get("Time"));
    }

    public String toString() {
        return "HibernateStats" //
            + "[ QueryExecutions=" + values.get("QueryExecutionCount") //
            + ", Transactions=" + values.get("TransactionCount") //
            + ", EntityLoads=" + values.get("EntityLoadCount") //
            + ", Connects=" + values.get("ConnectCount") //
            + ", Time=" + values.get("Time") + " ]";
    }

    private Statistics getStats() {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        Statistics stats = PersistenceUtility.getStatisticsService(entityManager, platformMBeanServer);
        return stats;
    }
}
