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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;
import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.stat.Statistics;

import org.rhq.core.server.PersistenceUtility;

/**
 * @author Joseph Marques
 */
public class HibernatePerformanceMonitor {
    private static final Log log = LogFactory.getLog(HibernatePerformanceMonitor.class);

    private ConcurrentMap<Long, HibernateStatisticsStopWatch> watches;
    private static HibernatePerformanceMonitor singleton = new HibernatePerformanceMonitor();
    private AtomicLong idGenerator = new AtomicLong(0);

    private HibernatePerformanceMonitor() {
        watches = new ConcurrentHashMap<Long, HibernateStatisticsStopWatch>();
    }

    public static HibernatePerformanceMonitor get() {
        return singleton;
    }

    public static boolean isLoggingEnabled() {
        return log.isDebugEnabled();
    }

    public void zeroStats() {
        if (isLoggingEnabled()) {
            EntityManager entityManager = LookupUtil.getEntityManager();
            MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
            Statistics stats = PersistenceUtility.getStatisticsService(entityManager, platformMBeanServer);
            stats.clear();
        }
    }

    public long start() {
        if (isLoggingEnabled()) {
            EntityManager entityManager = LookupUtil.getEntityManager();
            HibernateStatisticsStopWatch watch = new HibernateStatisticsStopWatch(entityManager);
            long id = idGenerator.incrementAndGet();
            watches.put(id, watch);
            watch.start();
            return id;
        }
        return 0;
    }

    public void stop(long id, String logPrefix) {
        if (isLoggingEnabled()) {
            HibernateStatisticsStopWatch watch = watches.remove(id);
            if (watch == null) {
                return; // could happen if debugging was turned on and the start() call was already skipped
            }
            watch.stop();

            String cause = "";
            if (watch.getQueryExecutions() != 0) {
                if ((watch.getConnects() / (double) (watch.getEntityLoads() + watch.getQueryExecutions())) >= 5.0) {
                    cause = "(perf: N+1 issue?) ";// might indicate need for LEFT JOIN FETCHes
                }
                if ((watch.getTransations() / (double) watch.getQueryExecutions()) >= 5.0) {
                    cause = "(perf: xaction nesting?) "; // might indicate excessive @REQUIRES_NEW
                } else if (watch.getTransations() > 10) {
                    cause = "(perf: too many xactions?)";
                }
            }
            if (watch.getTime() > 3000) {
                cause = "(perf: slowness?) "; // might indicate inefficient query or table contention
            }

            String callingContext = " for " + (logPrefix == null ? "(unknown)" : logPrefix);
            log.debug(watch.toString() + cause + callingContext);

            /* these queries are global, not per transaction
            if (logPrefix != null && (logPrefix.contains("URL") || logPrefix.contains("GWT:"))) {
                String[] queries = watch.getStats().getQueries();
                for (int i = 0; i < queries.length; i++) {
                    String query = queries[i];
                    QueryStatistics queryStats = watch.getStats().getQueryStatistics(query);
                    log.debug("query[" + i + "] " + queryStats);
                    log.debug("query[" + i + "] " + queries[i].replaceAll("\\s+", " "));
                }
            }
            */
        }
    }

}
