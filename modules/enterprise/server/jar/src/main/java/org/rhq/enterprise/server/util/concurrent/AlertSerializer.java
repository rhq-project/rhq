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
package org.rhq.enterprise.server.util.concurrent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AlertSerializer {
    private final Log log = LogFactory.getLog(AlertSerializer.class);

    private static Map<Integer, ReentrantReadWriteLock> locks = new HashMap<Integer, ReentrantReadWriteLock>();
    private static Map<Integer, Long> lockTimes = Collections.synchronizedMap(new HashMap<Integer, Long>());
    private static AlertSerializer singleton = new AlertSerializer();;

    // synchronize so only one thread creates our singleton
    public static synchronized AlertSerializer getSingleton() {
        return singleton;
    }

    public void lock(int alertDefinitionId) {
        String msg = "tid= " + Thread.currentThread().getId() + ": alertDefinitionId=" + alertDefinitionId;
        boolean debug = this.log.isDebugEnabled();

        ReentrantReadWriteLock lock = null;
        logDebug(debug, msg, ": about to synchronize");
        synchronized (this) {
            logDebug(debug, msg, ": synchronized");
            lock = AlertSerializer.locks.get(alertDefinitionId);
            if (lock == null) {
                logDebug(debug, msg, ": creating new lock");
                lock = new ReentrantReadWriteLock();
                AlertSerializer.locks.put(alertDefinitionId, lock);
            }
        }

        logDebug(debug, msg, ": acquiring write lock");
        long start = System.currentTimeMillis();
        lock.writeLock().lock();
        long end = System.currentTimeMillis();
        long duration = end - start;
        AlertSerializer.lockTimes.put(alertDefinitionId, Long.valueOf(end));
        if (duration < 5000L) {
            logDebug(debug, msg, ": acquired write lock in millis=" + duration);
        } else {
            this.log.info(msg + ": acquired write lock in millis=" + duration);
        }

        return;
    }

    public void unlock(int alertDefinitionId) {
        String msg = "tid= " + Thread.currentThread().getId() + ": alertDefinitionId=" + alertDefinitionId;
        boolean debug = this.log.isDebugEnabled();

        ReentrantReadWriteLock lock = AlertSerializer.locks.get(alertDefinitionId);

        if (lock != null) {
            Long lockedTime = AlertSerializer.lockTimes.get(alertDefinitionId);
            long duration = System.currentTimeMillis() - ((lockedTime != null) ? lockedTime : Long.MAX_VALUE);

            if (duration < 5000L) {
                logDebug(debug, msg, ": releasing write lock after being locked for millis=" + duration);
            } else {
                this.log.info(msg + ": releasing write lock after being locked for millis=" + duration);
            }

            lock.writeLock().unlock();
            logDebug(debug, msg, ": released write lock");
        } else {
            this.log.warn(msg + ": cannot release write lock");
        }

        return;
    }

    private void logDebug(boolean enabled, String arg1, String arg2) {
        if (enabled) {
            this.log.debug(arg1 + arg2);
        }
    }
}