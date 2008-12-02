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
package org.rhq.enterprise.communications.command.server;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.rhq.enterprise.communications.command.CommandResponse;

/**
 * Contains the statistics collected by the {@link CommandProcessor}.
 * 
 * The processor object is the only object that can update metric data
 * held in this object and it will do so in a thread-safe manner.
 * Objects that read data from this object automatically do so in a thread-safe manner,
 * but the callers may get inconsistent data if the processor updates data
 * in between calling multiple getters - but this isn't dangerous
 * so we'll leave it as is (the individual getters makes it easy to expose
 * this information as individual metrics so we can graph them).
 * But the locking done here helps minimize those instances where metric data
 * looks inconsistent to callers.
 *
 * @author John Mazzitelli
 */
public class CommandProcessorMetrics implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The total number of incoming commands this command processor has received and successfully processed.
     */
    long numberSuccessfulCommands = 0L;

    /**
     * The total number of commands that were received but were not successfully processed due to an error.
     */
    long numberFailedCommands = 0L;

    /**
     * The total number of commands that were not permitted to execute due to high concurrency.
     */
    long numberDroppedCommands = 0L;

    /**
     * The total number of commands that were not permitted to execute due to processing suspension.
     */
    long numberNotProcessedCommands = 0L;

    /**
     * The average time (in milliseconds) that successful commands take to complete.
     */
    long averageExecutionTime = 0L;

    /**
     * Call time data for individual command types (or subtypes if remote pojo executions).
     */
    private Map<String, Calltime> calltimes = new HashMap<String, Calltime>();

    /**
     * The lock that will ensure thread-safety.
     */
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Resets all the metric data to 0 and clears the calltime data.
     */
    public void clear() {
        // not thread-safe, because we might ask for this information in the middle of clearing
        // everything so we might get half of these to read 0 but the other half their old values
        writeLock();
        try {
            numberSuccessfulCommands = 0L;
            numberFailedCommands = 0L;
            numberDroppedCommands = 0L;
            numberNotProcessedCommands = 0L;
            averageExecutionTime = 0L;
            calltimes.clear();
        } finally {
            writeUnlock();
        }
    }

    /**
     * Return the calltime data that includes the different command types/pojo invocations.
     * Note that the calltime min/max/avg times are only for the calls that were successful.
     * 
     * @return calltime data
     */
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

    /**
     * Returns the total number of commands that were received but failed to be processed succesfully. This count is
     * incremented when a command was executed by its command service but the command response was
     * {@link CommandResponse#isSuccessful() not successful}. This does not count
     * {@link #getNumberDroppedCommands() dropped} or
     * {@link #getNumberNotProcessedCommands() unprocessed} commands.
     *
     * @return count of failed commands
     */
    public long getNumberFailedCommands() {
        readLock();
        try {
            return numberFailedCommands;
        } finally {
            readUnlock();
        }
    }

    /**
     * Returns the total number of commands that were received but were not permitted to be executed and were dropped.
     * This normally occurs when the limit of concurrent command invocations has been reached.
     *
     * @return count of commands not permitted to complete
     */
    public long getNumberDroppedCommands() {
        readLock();
        try {
            return numberDroppedCommands;
        } finally {
            readUnlock();
        }
    }

    /**
     * Returns the total number of commands that were received but were not processed.
     * This normally occurs when global processing of commands has been suspended.
     *
     * @return count of commands not processed.
     */
    public long getNumberNotProcessedCommands() {
        readLock();
        try {
            return numberNotProcessedCommands;
        } finally {
            readUnlock();
        }
    }

    /**
     * Returns the total number of commands that were received and processed succesfully. This count is incremented when
     * a command was executed by its command service and the command response was
     * {@link CommandResponse#isSuccessful() succesful}.
     *
     * @return count of commands succesfully processed
     */
    public long getNumberSuccessfulCommands() {
        readLock();
        try {
            return numberSuccessfulCommands;
        } finally {
            readUnlock();
        }
    }

    /**
     * Returns the average execution time (in milliseconds) it took to execute all
     * {@link #getNumberSuccessfulCommands() successful commands}.
     *
     * @return average execute time for all successful commands.
     */
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
     * if this represents a succesful call (i.e. <code>failure</code> is <code>false</code>).
     * We do not want to skew the min/max/avg results from failed commands because
     * they almost always fail-fast and will have very fast execution times.  
     * 
     * This is packaged-scoped because only the CommandProcessor should be
     * adding calltime data to this object. It will ensure thread-safety
     * just like it ensures thread-safety when updating the other metric data.
     *
     * @param type the type of invocation whose min/max/avg is to be stored
     * @param executionTime the time, in milliseconds, that the type invocation took
     * @param failure will be <code>true</code> if the invocation that was executed
     *        actually resulted in a failure; this will be <code>false</code> if
     *        the invocation succeeded
     */
    void addCallTimeData(String type, long executionTime, boolean failure) {
        Calltime calltime = calltimes.get(type);
        if (calltime == null) {
            calltime = new Calltime();
            calltimes.put(type, calltime);
        }

        calltime.count++;
        if (failure) {
            calltime.failures++;
        } else {
            if (executionTime > calltime.max) {
                calltime.max = executionTime;
            }
            if (executionTime < calltime.min) {
                calltime.min = executionTime;
            }
            long successes = calltime.count - calltime.failures;
            calltime.avg = (((successes - 1) * calltime.avg) + executionTime) / successes;
        }

        return;
    }

    /**
     * The CommandProcessor must call this prior to updating the metric data in this object.
     * 
     * This is packaged scoped so only the CommandProcessor can call it.
     * 
     * @return if <code>true</code> the write lock was acquired and must be unlocked
     *         if <code>false</code>, the write lock failed to be aquired
     */
    boolean writeLock() {
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

    /**
     * CommandProcessor needs to call this to unlock the write lock.
     *
     * This is packaged scoped so only the CommandProcessor can call it.
     */
    void writeUnlock() {
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
     * for invocations to a particular command. The count of the number
     * of times an invocation was executed is also kept.  Note that the min/max/avg
     * times will only be for successful commands.
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
            return count - failures; // ok if not thread-safe, good enough for what we need
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
