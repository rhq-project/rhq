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

import org.testng.annotations.Test;

/**
 * Tests the send throttling.
 *
 * @author John Mazzitelli
 */
@Test(groups = "comm-client")
public class SendThrottleTest {
    /**
     * Tests send throttling.
     *
     * @throws Exception
     */
    public void testSendThrottlingEnabled() throws Exception {
        long previous_time;
        ClientCommandSenderConfiguration config = createConfig(true, 2L, 2000L);
        SendThrottle throttle = new SendThrottle(config);

        assert throttle.isSendThrottlingEnabled() : "Send throttling should have been enabled";

        previous_time = System.currentTimeMillis();
        throttle.waitUntilOkToSend(); // #1
        throttle.waitUntilOkToSend(); // #2 - quiet period begins - next wait will pause
        assert timeDifference(previous_time) < 1000L : "Should not have taken this long, should not have had a quiet period";

        throttle.waitUntilOkToSend(); // #1
        assert timeDifference(previous_time) >= 2000L : "Should have taken longer, throttling was enabled";

        previous_time = System.currentTimeMillis();
        throttle.waitUntilOkToSend(); // #2 - quiet period begins - next wait will pause
        assert timeDifference(previous_time) < 1000L : "Should not have taken this long, should not have had a quiet period";

        throttle.waitUntilOkToSend(); // #1
        assert timeDifference(previous_time) >= 2000L : "Should have taken longer, throttling was enabled";

        return;
    }

    /**
     * Tests send throttling when its disabled.
     *
     * @throws Exception
     */
    public void testSendThrottlingDisabled() throws Exception {
        long previous_time;
        ClientCommandSenderConfiguration config = createConfig(false, 2L, 1000L);
        SendThrottle throttle = new SendThrottle(config);

        assert !throttle.isSendThrottlingEnabled() : "Send throttling should not have been enabled";

        previous_time = System.currentTimeMillis();
        throttle.waitUntilOkToSend();
        throttle.waitUntilOkToSend();
        throttle.waitUntilOkToSend();
        throttle.waitUntilOkToSend();
        assert timeDifference(previous_time) < 1000L : "Should not have taken so long, throttling was disabled";

        return;
    }

    /**
     * Creates a config with the given params set.
     *
     * @param  throttled    indicates if the throttling should be enabled
     * @param  max_commands the maximum number of commands that be sent before quiet period starts
     * @param  quiet_period duration, in millis, of the quiet period
     *
     * @return the config
     */
    private ClientCommandSenderConfiguration createConfig(boolean throttled, long max_commands, long quiet_period) {
        ClientCommandSenderConfiguration config = new ClientCommandSenderConfiguration();
        config.enableSendThrottling = throttled;
        config.sendThrottleMaxCommands = max_commands;
        config.sendThrottleQuietPeriodDurationMillis = quiet_period;

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
}