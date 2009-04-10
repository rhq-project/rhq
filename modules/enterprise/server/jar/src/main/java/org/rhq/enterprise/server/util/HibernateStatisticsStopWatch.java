package org.rhq.enterprise.server.util;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.persistence.EntityManager;

import org.hibernate.stat.Statistics;

import org.rhq.core.domain.util.PersistenceUtility;

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
    }

    public void stop() {
        Statistics stats = getStats();
        values.put("QueryExecutionCount", stats.getQueryExecutionCount() - values.get("QueryExecutionCount"));
        values.put("TransactionCount", stats.getTransactionCount() - values.get("TransactionCount"));
        values.put("EntityLoadCount", stats.getEntityLoadCount() - values.get("EntityLoadCount"));
        values.put("ConnectCount", stats.getConnectCount() - values.get("ConnectCount"));
    }

    public String toString() {
        return "HibernateStats" //
            + "[ QueryExecutions=" + values.get("QueryExecutionCount") //
            + ", Transactions=" + values.get("TransactionCount") //
            + ", EntityLoads=" + values.get("EntityLoadCount") //
            + ", Connects=" + values.get("ConnectCount") + " ]";
    }

    private Statistics getStats() {
        MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
        Statistics stats = PersistenceUtility.getStatisticsService(entityManager, platformMBeanServer);
        return stats;
    }
}
