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

import java.util.concurrent.TimeUnit;
import org.testng.annotations.Test;

/**
 * Tests the command queue and queue throttling.
 *
 * @author John Mazzitelli
 */
@Test
public class CommandQueueTest {
    /**
     * Tests putting alot of things in the queue and taking them out while not throttled.
     *
     * @throws Exception
     */
    public void testBigQueue() throws Exception {
        CommandQueue q = new CommandQueue(createConfig(10000, false, 0L, 0L));

        assert !q.isThrottlingEnabled() : "Queue throttling should have been disabled";

        for (int i = 0; i < 10000; i++) {
            q.put(new QueueItem(i));
            assert q.size() == (i + 1);
        }

        for (int i = 0; i < 10000; i++) {
            int num = ((QueueItem) q.take()).asInt();
            assert num == i : "The queue did not take the items out in FIFO order";
            assert q.size() == (10000 - (i + 1));
        }

        return;
    }

    /**
     * Tests putting alot of things in the queue and taking them out while throttled.
     *
     * @throws Exception
     */
    public void testBigQueueThrottled() throws Exception {
        CommandQueue q = new CommandQueue(createConfig(500, true, 5L, 250L));

        assert q.isThrottlingEnabled() : "Queue throttling should have been enabled";

        for (int i = 0; i < 500; i++) {
            q.put(new QueueItem(i));
        }

        for (int i = 0; i < 500; i++) {
            int num = ((QueueItem) q.take()).asInt();
            assert num == i : "The queue did not take the items out in FIFO order";
        }

        return;
    }

    /**
     * Tests queue throttling.
     *
     * @throws Exception
     */
    public void testQueueThrottling() throws Exception {
        long previous_time;
        CommandQueue q = new CommandQueue(createConfig(5, true, 2L, 1000L));

        assert q.isThrottlingEnabled() : "Queue throttling should have been enabled";

        q.put(new QueueItem("first"));
        q.put(new QueueItem("second"));
        q.put(new QueueItem("third"));
        q.put(new QueueItem("fourth"));
        q.put(new QueueItem("fifth"));

        previous_time = System.currentTimeMillis();
        assert q.take().equals(new QueueItem("first"));
        assert q.take().equals(new QueueItem("second"));

        assert timeDifference(previous_time) < 1000L : "Should not have taken so long, we took no more than the max";

        previous_time = System.currentTimeMillis();
        assert q.take().equals(new QueueItem("third"));
        assert q.take().equals(new QueueItem("fourth"));
        assert q.take().equals(new QueueItem("fifth"));

        assert timeDifference(previous_time) >= 1000L : "Should have been throttled, we took more than the max per burst";

        q.disableQueueThrottling();
        assert !q.isThrottlingEnabled();

        q.put(new QueueItem("first"));
        q.put(new QueueItem("second"));
        q.put(new QueueItem("third"));
        q.put(new QueueItem("fourth"));
        q.put(new QueueItem("fifth"));
        previous_time = System.currentTimeMillis();
        assert q.take().equals(new QueueItem("first"));
        assert q.take().equals(new QueueItem("second"));
        assert q.take().equals(new QueueItem("third"));
        assert q.take().equals(new QueueItem("fourth"));
        assert q.take().equals(new QueueItem("fifth"));
        assert timeDifference(previous_time) < 1000L : "Should not have been throttled";

        return;
    }

    /**
     * Tests simple queue ops.
     *
     * @throws Exception
     */
    public void testQueueing() throws Exception {
        CommandQueue q = new CommandQueue(createConfig(5, false, 0L, 0L));

        assert q.remainingCapacity() == 5;
        q.put(new QueueItem("first"));
        q.put(new QueueItem("second"));
        q.put(new QueueItem("third"));
        q.put(new QueueItem("fourth"));
        q.put(new QueueItem("fifth"));
        assert !q.offer(new QueueItem("NO ROOM"), 500L, TimeUnit.MILLISECONDS) : "The queue should have been full, should not have been able to put this";
        assert q.take().equals(new QueueItem("first"));
        assert q.take().equals(new QueueItem("second"));
        assert q.take().equals(new QueueItem("third"));
        assert q.take().equals(new QueueItem("fourth"));
        assert q.take().equals(new QueueItem("fifth"));
        assert q.poll(500L, TimeUnit.MILLISECONDS) == null;

        assert !q.isThrottlingEnabled() : "We never configured queue throttling";
    }

    /**
     * Creates a config with the given params set.
     *
     * @param  q_size             the size of the queue
     * @param  throttled          indicates if the throttling should be enabled
     * @param  burst_max_commands the maximum number of commands that can be taken during burst period
     * @param  burst_period       the millisecond time period for the burst
     *
     * @return the config
     */
    private ClientCommandSenderConfiguration createConfig(int q_size, boolean throttled, long burst_max_commands,
        long burst_period) {
        ClientCommandSenderConfiguration config = new ClientCommandSenderConfiguration();
        config.queueSize = q_size;
        config.enableQueueThrottling = throttled;
        config.queueThrottleMaxCommands = burst_max_commands;
        config.queueThrottleBurstPeriodMillis = burst_period;

        return config;
    }

    /**
     * Returns the number of milliseconds that have elapsed since the given previous time (compared to the current
     * time).
     *
     * @param  previous_time
     *
     * @return current time minus the previous time
     */
    private long timeDifference(long previous_time) {
        return System.currentTimeMillis() - previous_time;
    }

    private class QueueItem implements Runnable {
        private String str;

        public QueueItem(String s) {
            str = s.toString(); // force NPE - shouldn't happen in our tests
        }

        public QueueItem(Integer i) {
            str = String.valueOf(i.intValue()); // force NPE - shouldn't happen in our tests
        }

        public int asInt() {
            return Integer.parseInt(str);
        }

        @Override
        public String toString() {
            return str;
        }

        @Override
        public int hashCode() {
            return str.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            return str.equals(obj.toString());
        }

        public void run() {
        }
    }
}