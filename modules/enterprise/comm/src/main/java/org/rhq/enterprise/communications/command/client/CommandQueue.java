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
package org.rhq.enterprise.communications.command.client;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import mazz.i18n.Logger;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * This is the queue that will be used by {@link ClientCommandSender} to queue up command tasks that have to be sent.
 * The queue implements {@link BlockingQueue}; if you instantiate it with a capacity of 0 or less, it will actually be
 * unbounded (i.e. theoretical capacity at {@link Integer#MAX_VALUE}).
 *
 * <p>This queue supports "throttling", that is, only permits X number of commands at most to be taken from the queue in
 * Y number of milliseconds. Throttling is disabled unless specifically enabled in the configuration given this object
 * when this object is constructed or by calling {@link #enableQueueThrottling()}. Throttling affects
 * {@link #poll(long)} and {@link #take()}. Throttling does not affect how fast you can add commands to the queue - you
 * can queue commands as fast as you can.</p>
 *
 * <p>Because this is going to be a work queue for a {@link ThreadPoolExecutor}, and because of the strict type required
 * of work queues in its constructor, I must implement BlockingQueue&lt;Runnable>, but I really wanted
 * &lt;ClientCommandSenderTask> because the runnables stored in this queue should really only be of type
 * {@link ClientCommandSenderTask}.</p>
 *
 * @author John Mazzitelli
 */
class CommandQueue implements BlockingQueue<Runnable> {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(CommandQueue.class);

    /**
     * The real queue that contains commands that need to be sent. We delegate to this object.
     */
    private LinkedBlockingQueue<Runnable> m_queue;

    /**
     * The semaphore that helps throttle taking things off the queue. Will be <code>null</code> then throttling is
     * disabled.
     */
    private Semaphore m_throttleSemaphore;

    /**
     * Used for its monitor lock for throttling - no thread can change the throttle mode or aquire/release a semaphore
     * permit without first owning this lock.
     */
    private Object m_throttleLock;

    /**
     * If queue throttling is enabled, this is the maximum number of commands that can be sent during the burst period.
     */
    private long m_queueThrottleMaxCommands;

    /**
     * If queue throttling is enabled, this is the length of time (in milliseconds) of the burst period.
     */
    private long m_queueThrottleBurstPeriodDurationMillis;

    /**
     * This will be the number of threads waiting to acquire a semaphore permit. Only used when we need to disable
     * throttling - we have to know how many threads we need to wake up out of the semaphore acquire method.
     */
    private AtomicInteger m_waitingForAcquire;

    /**
     * Constructor for {@link CommandQueue} that provides a bounded queue with a maximum capacity specified in the
     * configuration.
     *
     * <p>If the queue capacity (see {@link ClientCommandSenderConfiguration#queueSize}) is less than or equal to 0, the
     * queue will be <b>unbounded</b>. Be careful using an unbounded queue since it may grow to use up alot of (or all)
     * available memory.</p>
     *
     * @param  config the configuration that tells this queue things like its capacity and queue throttling settings.
     *
     * @throws IllegalArgumentException if config is <code>null</code>
     */
    public CommandQueue(ClientCommandSenderConfiguration config) {
        if (config == null) {
            throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.COMMAND_QUEUE_NULL_CONFIG));
        }

        m_throttleLock = new Object();
        m_throttleSemaphore = null;
        m_waitingForAcquire = new AtomicInteger(0);

        if (config.queueSize > 0) {
            m_queue = new LinkedBlockingQueue<Runnable>(config.queueSize);
        } else {
            m_queue = new LinkedBlockingQueue<Runnable>();
        }

        setQueueThrottleParameters(config);

        if (config.enableQueueThrottling) {
            enableQueueThrottling();
        } else {
            disableQueueThrottling();
        }

        return;
    }

    /**
     * Allows the caller to modify this queue's throttling configuration - specifically
     * {@link ClientCommandSenderConfiguration#queueThrottleMaxCommands} and
     * {@link ClientCommandSenderConfiguration#queueThrottleBurstPeriodMillis}. If queue throttling is already enabled,
     * these will take effect as soon as possible (though not necessarily immediately).
     *
     * @param config the object that contains this object's new configuration
     */
    public void setQueueThrottleParameters(ClientCommandSenderConfiguration config) {
        synchronized (m_throttleLock) {
            m_queueThrottleMaxCommands = config.queueThrottleMaxCommands;
            m_queueThrottleBurstPeriodDurationMillis = config.queueThrottleBurstPeriodMillis;

            LOG.debug(CommI18NResourceKeys.COMMAND_QUEUE_CONFIGURED, m_queueThrottleMaxCommands,
                m_queueThrottleBurstPeriodDurationMillis);
        }

        return;
    }

    /**
     * This enables throttling which limits the number of commands that can be taken from the queue. A maximum number of
     * commands can be taken from the queue in the burst period. After that, the {@link #take()} and {@link #poll(long)}
     * will block until the throttling enables more commands to be taken from the queue.
     */
    public void enableQueueThrottling() {
        synchronized (m_throttleLock) {
            // if we are already enabled for throttling, let's disable it so we can reconfigure our semaphore and thread
            if (isThrottlingEnabled()) {
                disableQueueThrottling();
            }

            m_throttleSemaphore = new Semaphore((int) m_queueThrottleMaxCommands);

            ThrottleRunnable throttleRunnable = new ThrottleRunnable((int) m_queueThrottleMaxCommands,
                m_queueThrottleBurstPeriodDurationMillis);
            Thread thread = new Thread(throttleRunnable, "RHQ Command Queue Throttle Thread");
            thread.setDaemon(true);
            thread.start();

            LOG.debug(CommI18NResourceKeys.COMMAND_QUEUE_ENABLED, m_queueThrottleMaxCommands,
                m_queueThrottleBurstPeriodDurationMillis);
        }

        return;
    }

    /**
     * Disables throttling - taking from the queue will now occur as fast as possible.
     */
    public void disableQueueThrottling() {
        synchronized (m_throttleLock) {
            if (m_throttleSemaphore != null) {
                // let any and all threads currently waiting to acquire a throttle semaphore to get one
                m_throttleSemaphore.release(m_waitingForAcquire.get() + 1000); // add a 1000 just for good measure
            }

            m_throttleSemaphore = null;
            m_throttleLock.notifyAll(); // this should kill the throttle thread immediately
        }

        LOG.debug(CommI18NResourceKeys.COMMAND_QUEUE_DISABLED);

        return;
    }

    /**
     * Returns <code>true</code> if throttling is enabled; <code>false</code> if commands can be taken from the queue as
     * fast as possible.
     *
     * @return <code>true</code> if throttling is enabled; <code>false</code> otherwise
     */
    public boolean isThrottlingEnabled() {
        synchronized (m_throttleLock) {
            return m_throttleSemaphore != null;
        }
    }

    public void put(Runnable obj) throws InterruptedException {
        m_queue.put(obj);
    }

    public boolean offer(Runnable obj, long timeout, TimeUnit timeunit) throws InterruptedException {
        return m_queue.offer(obj, timeout, timeunit);
    }

    public boolean offer(Runnable o) {
        try {
            return offer(o, 0L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * If throttling is enabled, the attempt to take the command from the queue will occur only after this thread is
     * allowed to do so under the throttling algorithm.
     *
     * @return the command that was taken
     *
     * @throws InterruptedException
     */
    public Runnable take() throws InterruptedException {
        acquireSemaphorePermit();
        Runnable ret_obj = m_queue.take();
        return ret_obj;
    }

    /**
     * If throttling is enabled, the attempt to take the command from the queue will occur only after this thread is
     * allowed to do so under the throttling algorithm. Only then will the <code>timeout</code> counter start.
     * Therefore, when throttling is enabled, the <code>timeout</code> timer won't start until after the throttling
     * allows it to attempt to take from the queue.
     *
     * @param  timeout
     * @param  timeunit
     *
     * @return the command that was taken
     *
     * @throws InterruptedException
     */
    public Runnable poll(long timeout, TimeUnit timeunit) throws InterruptedException {
        acquireSemaphorePermit();
        Runnable ret_val = m_queue.poll(timeout, timeunit);
        return ret_val;
    }

    /**
     * If throttling is enabled, the attempt to take the command from the queue will occur only after this thread is
     * allowed to do so under the throttling algorithm. Only then will the <code>timeout</code> counter start.
     * Therefore, when throttling is enabled, the <code>timeout</code> timer won't start until after the throttling
     * allows it to attempt to take from the queue.
     *
     * @see java.util.Queue#poll()
     */
    public Runnable poll() {
        try {
            return poll(0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }

    public Runnable peek() {
        return m_queue.peek(); // bypasses throttle
    }

    /**
     * This quickly drains the queue. Throttling is ignored even if throttling is enabled, this will drain all items as
     * fast as possible.
     *
     * @see BlockingQueue#drainTo(Collection, int)
     */
    public int drainTo(Collection<? super Runnable> c, int maxElements) {
        return m_queue.drainTo(c, maxElements);
    }

    /**
     * This quickly drains the queue. Throttling is ignored even if throttling is enabled, this will drain all items as
     * fast as possible. Upon return, the queue will be empty.
     *
     * <p>Call this method when you are sure no threads will be placing items on the queue, otherwise, this will never
     * return as it will continue to drain until there are no more items left in the queue.</p>
     *
     * @see BlockingQueue#drainTo(Collection)
     */
    public int drainTo(Collection<? super Runnable> c) {
        return m_queue.drainTo(c);
    }

    public int remainingCapacity() {
        return m_queue.remainingCapacity();
    }

    public boolean add(Runnable o) {
        if (!offer(o)) {
            throw new IllegalStateException();
        }

        return true;
    }

    public boolean addAll(Collection<? extends Runnable> c) {
        boolean changed = false;

        for (Runnable task : c) {
            if (!offer(task)) {
                throw new IllegalArgumentException("Cannot add: " + task);
            } else {
                changed = true;
            }
        }

        return changed;
    }

    public void clear() {
        drainTo(new LinkedList<Runnable>());
    }

    public boolean contains(Object o) {
        return m_queue.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        return m_queue.containsAll(c);
    }

    public Runnable element() {
        return m_queue.element();
    }

    public boolean isEmpty() {
        return m_queue.isEmpty();
    }

    public Iterator<Runnable> iterator() {
        return m_queue.iterator();
    }

    public Runnable remove() {
        Runnable task = poll();

        if (task == null) {
            throw new NoSuchElementException();
        }

        return task;
    }

    public boolean remove(Object o) {
        return m_queue.remove(o); // bypasses throttle
    }

    public boolean removeAll(Collection<?> c) {
        return m_queue.removeAll(c); // bypasses throttle
    }

    public boolean retainAll(Collection<?> c) {
        return m_queue.retainAll(c); // bypasses throttle
    }

    public int size() {
        return m_queue.size();
    }

    public Object[] toArray() {
        return m_queue.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return m_queue.toArray(a);
    }

    /**
     * Acquires a semaphore permit, but only if throttling is enabled.
     *
     * @throws InterruptedException if thread was interrupted while blocked trying to acquire the permit
     */
    private void acquireSemaphorePermit() throws InterruptedException {
        Semaphore semaphore = null;

        // we do NOT want to block in the acquire() inside of the synchronized block
        // just get the semaphore object and exit the sync block - we'll acquire outside of it
        // this will allow the throttle lock to be acquired should someone want to disable throttling
        // while we are waiting to acquire the semaphore permit

        synchronized (m_throttleLock) {
            if (m_throttleSemaphore != null) {
                semaphore = m_throttleSemaphore;
                m_waitingForAcquire.incrementAndGet();
            }
        }

        if (semaphore != null) {
            semaphore.acquire();
            m_waitingForAcquire.decrementAndGet();
        }

        return;
    }

    /**
     * This thread sleeps for X milliseconds and when it awakes, releases N semaphore permits. This performs the
     * throttling.
     */
    private class ThrottleRunnable implements Runnable {
        private long m_sleepMillis;
        private int m_numPermits;

        /**
         * Constructor for {@link ThrottleRunnable}.
         *
         * @param num_permits  the maximum number of semaphore permits to release each cycle
         * @param sleep_millis the number of milliseconds to sleep before releasing semaphore permits
         */
        public ThrottleRunnable(int num_permits, long sleep_millis) {
            m_numPermits = num_permits;
            m_sleepMillis = sleep_millis;
        }

        /**
         * @see java.lang.Runnable#run()
         */
        public void run() {
            synchronized (m_throttleLock) {
                while (m_throttleSemaphore != null) {
                    try {
                        m_throttleLock.wait(m_sleepMillis);
                    } catch (InterruptedException e) {
                        // told to wake up, throttling was probably disabled
                    }

                    if (m_throttleSemaphore != null) {
                        // make sure only at most m_numPermits are available to be acquired
                        m_throttleSemaphore.release(m_numPermits - m_throttleSemaphore.availablePermits());
                    }
                }
            }

            // the queue was told not to throttle anymore, exit the thread
            return;
        }
    }
}