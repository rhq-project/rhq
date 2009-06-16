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

package org.rhq.enterprise.server.remote;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Provides metrics for remote invocations from things like the remote CLI.
 * 
 * @author John Mazzitelli
 */
public class RemoteSafeInvocationHandlerMetrics implements RemoteSafeInvocationHandlerMetricsMBean {

    private long numberSuccessfulInvocations = 0L;
    private long numberFailedInvocations = 0L;
    private long averageExecutionTime = 0L;

    private Map<String, Calltime> calltimes = new HashMap<String, Calltime>();
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public void clear() {
        writeLock();
        try {
            numberSuccessfulInvocations = 0L;
            numberFailedInvocations = 0L;
            averageExecutionTime = 0L;
            calltimes.clear();
        } finally {
            writeUnlock();
        }
    }

    public Map<String, Calltime> getCallTimeData() {
        // just do a shallow copy - this allows us to avoid exceptions when we want to concurrently
        // traverse the calltimes and add to them but it avoids unnecesary duplication of MinMaxAvg
        // objects (thus we are more efficient both in performance and space). Note that MinMaxAvg
        // is allowed to be concurrently accessed - even though its possible you could try to get
        // its data while we are writing new min/max/avg/count values. Even though the data won't be
        // entirely accurate and up-to-date in that case, its good enough for our purposes.
        readLock();
        try {
            return new HashMap<String, Calltime>(calltimes);
        } finally {
            readUnlock();
        }
    }

    public long getNumberFailedInvocations() {
        readLock();
        try {
            return numberFailedInvocations;
        } finally {
            readUnlock();
        }
    }

    public long getNumberSuccessfulInvocations() {
        readLock();
        try {
            return numberSuccessfulInvocations;
        } finally {
            readUnlock();
        }
    }

    public long getNumberTotalInvocations() {
        readLock();
        try {
            return numberSuccessfulInvocations + numberFailedInvocations;
        } finally {
            readUnlock();
        }
    }

    public long getAverageExecutionTime() {
        readLock();
        try {
            return averageExecutionTime;
        } finally {
            readUnlock();
        }
    }

    /**
     * Add a newly collected metric value for a particular type of invocation to
     * the stored calltime data. This will update the min/max/avg data, but only
     * if this represents a succesful call (i.e. <code>successful</code> is <code>true</code>).
     * We do not want to skew the min/max/avg results from failed invocations because
     * they almost always fail-fast and will have very fast execution times.  
     * 
     * This is packaged-scoped because only the handler should be
     * adding calltime data to this object.
     *
     * @param type the type of invocation whose min/max/avg is to be stored
     * @param executionTime the time, in milliseconds, that the type invocation took
     * @param successful <code>true</code> if the invocation was successful
     */
    void addData(String type, long executionTime, boolean successful) {
        writeLock();
        try {
            if (type == null) {
                type = "(unknown)";
            }

            Calltime calltime = calltimes.get(type);
            if (calltime == null) {
                calltime = new Calltime();
                calltimes.put(type, calltime);
            }

            calltime.count++;
            if (successful) {
                this.numberSuccessfulInvocations++;
                this.averageExecutionTime = (((this.numberSuccessfulInvocations - 1) * this.averageExecutionTime) + executionTime
                    / this.numberSuccessfulInvocations);

                if (executionTime > calltime.max) {
                    calltime.max = executionTime;
                }
                if (executionTime < calltime.min) {
                    calltime.min = executionTime;
                }
                long successes = calltime.getSuccesses();
                calltime.avg = (((successes - 1) * calltime.avg) + executionTime) / successes;
            } else {
                this.numberFailedInvocations++;
                calltime.failures++;
            }
        } finally {
            writeUnlock();
        }

        return;
    }

    private boolean writeLock() {
        // we try to be good stewards of this object by trying to be thread safe
        // but do not block the processor here for too long - we don't want to
        // prevent the processor from processing messages for too long just to
        // synchronize metric data. If we timeout or are interrupted, return immediately.
        // Note also that we don't want to throw any exceptions to the caller, try our
        // best to be fault tolerant in here.
        try {
            return lock.writeLock().tryLock(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // don't wait any longer, just return immediately
        } catch (Exception e) {
            lock = new ReentrantReadWriteLock(); // something really bad happened, let's create a new one to be safe
        }
        return false;
    }

    private void writeUnlock() {
        try {
            lock.writeLock().unlock();
        } catch (Exception e) {
            // Note that we don't want to throw any exceptions to the caller, try our
            // best to be fault tolerant in here. This exception occurred probably because
            // the caller didn't have the lock due to a timeout or interrupt in writeLock.
        }
    }

    private boolean readLock() {
        // we try to be good stewards of this object by trying to be thread safe
        // but if we can't get the lock, just return and let's read the data
        // unlocked.  Nothing dangerous will happen, at worst we might read
        // inconsistent metric data for this thread, nothing too serious to worry about.
        try {
            return lock.readLock().tryLock(30, TimeUnit.SECONDS);
        } catch (Exception e) {
        }
        return false;
    }

    private void readUnlock() {
        try {
            lock.readLock().unlock();
        } catch (Exception e) {
            // Note that we don't want to throw any exceptions to the caller, try our
            // best to be fault tolerant in here. This exception occurred probably because
            // the caller didn't have the lock due to a timeout or interrupt in readLock.
        }
    }

    /**
     * Used to store the minimum, maximum and average times (in milliseconds)
     * for invocations to a particular invocation. The count of the number
     * of times an invocation was executed is also kept.  Note that the min/max/avg
     * times will only be for successful invocations.
     */
    public class Calltime implements Serializable {
        private static final long serialVersionUID = 1L;

        private long count = 0;
        private long failures = 0;
        private long min = Long.MAX_VALUE;
        private long max = Long.MIN_VALUE;
        private long avg = 0;

        public long getCount() {
            return count;
        }

        public long getFailures() {
            return failures;
        }

        public long getSuccesses() {
            return count - (failures); // ok if not thread-safe, good enough for what we need
        }

        public long getMinimum() {
            return min;
        }

        public long getMaximum() {
            return max;
        }

        public long getAverage() {
            return avg;
        }

        @Override
        public String toString() {
            return "" + count + ':' + failures + ':' + min + ':' + max + ':' + avg;
        }
    }
}
