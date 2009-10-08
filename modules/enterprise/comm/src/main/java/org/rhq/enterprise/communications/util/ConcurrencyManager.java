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
package org.rhq.enterprise.communications.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import mazz.i18n.Logger;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * This is basically a thread gatekeeper that allows a thread to ask for permission to continue.
 *
 * <p>Under the covers, this object uses a set of named counting semaphores. A calling thread asks for permission by
 * giving a name to {@link #getPermit(String)}; if no more permits are available on the named semaphore, this manager
 * will throw a runtime exception. In effect, it will abort a thread unless that thread is permitted to continue.
 * Because this manager maintains a dynamic set of named counting semaphores, you can have groups of threads that are
 * allowed to operate independently of other groups of threads (i.e. each group of threads can use their own named
 * semaphore).
 *
 * <p>Each counting semaphore will be given a default number of total permits. You can set a custom number of permits
 * per counting semaphore by passing in a set of names with their associated number-of-permits-allowed to the
 * constructor.</p>
 *
 * @author John Mazzitelli
 */
public class ConcurrencyManager {
    public static class Permit {
        private String name;
        private int managerId;
    }

    private static final Logger LOG = CommI18NFactory.getLogger(ConcurrencyManager.class);
    private static final int DEFAULT_PERMITS = 50;

    // keys on permit name, value is the number of permits allowed to be concurrently held, this never changes after instantiation
    private final Map<String, Integer> numPermitsAllowed = new HashMap<String, Integer>();

    // the named counting semaphores (key=semaphore name, value=semaphore)
    private final Map<String, Semaphore> semaphores = new HashMap<String, Semaphore>();

    // Number of threads that were denied permission to a named semaphore (key=semaphore name, value=denied count).
    // The count will be reset whenever a semaphore permit is released (in effect, this tracks the number
    // of consecutive denials since the last release of a semaphore permit).
    private final Map<String, AtomicInteger> deniedCounts = new HashMap<String, AtomicInteger>();

    public ConcurrencyManager(Map<String, Integer> newPermitsAllowed) {
        if (newPermitsAllowed != null) {
            numPermitsAllowed.putAll(newPermitsAllowed);
        }
    }

    /**
     * Asks to obtain a permit to continue. The given name is that of the semaphore whose permit the caller will attempt
     * to acquire. If <code>name</code> is <code>null</code>, this method will return always (in effect, the null
     * semaphore allows an unlimited number of permits).
     *
     * @param  name the name of the semaphore to acquire the permit from (note: this has nothing to do with the name of
     *              a thread or thread group) (may be <code>null</code>)
     *
     * @return the permit that allows the thread to continue. The caller must eventually
     *         {@link #releasePermit(Permit) release it}.
     *
     * @throws NotPermittedException if the calling thread cannot obtain a permit
     */
    public Permit getPermit(String name) throws NotPermittedException {
        Permit permit = new Permit();
        permit.name = name;
        permit.managerId = this.hashCode(); // identifes this manager as the originator of this permit

        if (name == null) {
            return permit;
        }

        Semaphore semaphore = getSemaphore(name);
        if (semaphore == null) {
            // there is no limit, always allow it
            permit.name = null;
            return permit;
        }

        boolean permitted = semaphore.tryAcquire();

        if (!permitted) {
            int deniedCount = getDeniedCount(name).incrementAndGet(); // don't worry about this not being atomic with aquire, no biggie
            long sleepBeforeRetry = getSleepBeforeRetryHint(deniedCount);
            throw new NotPermittedException(sleepBeforeRetry);
        }

        return permit;
    }

    /**
     * Returns the permission that was previously granted to the caller.
     *
     * @param permit the permit that was previously granted that is now being released (may be <code>null</code>)
     */
    public void releasePermit(Permit permit) {
        // ignore this permit if it is null, indicated an infinite limit or if it was not granted by this specific concurrency manager instance
        if ((permit != null) && (permit.name != null) && (permit.managerId == this.hashCode())) {
            Semaphore semaphore = getSemaphore(permit.name);
            if (semaphore != null) {
                semaphore.release();
            }

            getDeniedCount(permit.name).set(0); // we can reset this now; don't worry about this not being atomic with the release, no biggie
        }

        return;
    }

    /**
     * Returns a copy of the map with all named permits and how many are allowed to be concurrently held. The returned
     * map is a copy and not backed by this manager. Note that this returns the map of explicitly declared or determined
     * permits allowed, it will not return information on concurrency limits that may have defaults defined in system
     * properties but has not actually been used yet by this concurrency manager.
     *
     * @return map keyed on permit names whose values are the number of times the permit can be concurrently held
     */
    public Map<String, Integer> getAllConfiguredNumberOfPermitsAllowed() {
        return new HashMap<String, Integer>(numPermitsAllowed);
    }

    /**
     * If the named semaphore has a configured number-of-permits-allowed (numPermitsAllowed) then the number of permits
     * allowed is returned. Otherwise, this checks system properties - if there is a system property with the given
     * name, the value of the system property is returned. Its possible this method will return a value for <code>
     * name</code> where that name in the map returned by {@link #getAllConfiguredNumberOfPermitsAllowed()} has no value
     * - this is due to the fact that this method checks system properties as a fall back.
     *
     * @param  name the name of the semaphore
     *
     * @return number of permits allowed on the named semaphore
     */
    public int getConfiguredNumberOfPermitsAllowed(String name) {
        Integer number = numPermitsAllowed.get(name);

        if (number == null) {
            number = DEFAULT_PERMITS;

            String numberString = System.getProperty(name);

            if (numberString != null) {
                try {
                    number = Integer.parseInt(numberString);
                } catch (NumberFormatException e) {
                    LOG.warn(CommI18NResourceKeys.INVALID_PERMITS_CONFIG, numberString, name, number);
                }
            }
        }

        return number.intValue();
    }

    /**
     * If this semaphore does not yet exist, create it. Note that this class does NOT cleanup or otherwise destroy
     * semaphores once they are created so make sure you use this class such that it only manages a fixed set of named
     * semaphores over the lifetime of this manager. If you want to change the number of permits, you should discard
     * this entire manager object and create a new one so it rebuilds the semaphores with the new permit count. We could
     * aquire/release existing semaphores to change the permit counts, but that's not guaranteed to give us what we want
     * because there might be threads acquiring/releasing permits at the same time we resize so its not guaranteed that
     * you'll know the number of availability permits when we resize it. Its better to throw away our manager object and
     * rebuild a new one. That's what ServiceContainer.s/getConcurrencyManager javadoc is trying to allude to. This
     * method may return <code>null</code> if the number of permits for the limit is 0 or less (which indicates there is
     * no hard limit - allow as many as we can).
     *
     * @param  name semaphore name
     *
     * @return the semaphore, or <code>null</code> if there is no limit for the given name
     */
    private Semaphore getSemaphore(String name) {
        Semaphore semaphore;

        synchronized (semaphores) {
            semaphore = semaphores.get(name);

            if (semaphore == null) {
                int permits = getConfiguredNumberOfPermitsAllowed(name);
                if (permits > 0) {
                    semaphore = new Semaphore(permits, false);
                    semaphores.put(name, semaphore);
                }
            }
        }

        return semaphore;
    }

    private AtomicInteger getDeniedCount(String name) {
        AtomicInteger deniedCount;

        synchronized (deniedCounts) {
            deniedCount = deniedCounts.get(name);

            if (deniedCount == null) {
                deniedCount = new AtomicInteger(0);
                deniedCounts.put(name, deniedCount);
            }
        }

        return deniedCount;
    }

    private long getSleepBeforeRetryHint(int deniedCount) {
        // If the denied count is low, there is some but not alot of contention.
        // In this case, we can tell the client to just wait a little bit before retrying.
        // If the denied count is high, then there is alot of contention and we'll
        // want to ask the client to wait longer to help clear up the heavy load.  This
        // is really just to put some space between large amounts of requests.
        // We'll never ask the client to wait less than 5 seconds or more than 15 seconds
        // BTW: there is no reasoning behind the numbers I've picked - its really just to
        // put some different/larger distances between large amounts of requests.  I could have
        // made it random and probably get the same results.  We could just return
        // a fixed 5000ms delay time and probably still get good performance, but hey,
        // this looks smrter.

        if (deniedCount > 100) {
            return 15000L; // never more than 15s
        }

        return 5000L + ((deniedCount / 10) * 1000L); // never less than 5s, never more than 15s
    }
}