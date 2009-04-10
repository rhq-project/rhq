package org.rhq.enterprise.server.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.util.HibernateStatisticsStopWatch;

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

    public long start() {
        if (log.isDebugEnabled()) {
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
        if (log.isDebugEnabled()) {
            HibernateStatisticsStopWatch watch = watches.remove(id);
            watch.stop();
            if (watch == null) {
                return;
            }
            log.debug(watch.toString() + (logPrefix == null ? "(unknown)" : " for " + logPrefix + " "));
        }
    }

}
