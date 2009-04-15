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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
            if (watch == null) {
                // could happen if debugging was turned on and the start() call was already skipped
                return;
            }
            watch.stop();
            log.debug(watch.toString() + (logPrefix == null ? "(unknown)" : " for " + logPrefix + " "));
        }
    }

}
