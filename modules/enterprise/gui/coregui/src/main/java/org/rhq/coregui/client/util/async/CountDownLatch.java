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
package org.rhq.coregui.client.util.async;

import org.rhq.coregui.client.util.Log;

/**
 * A Simple version of CountDownLatch.
 * That executes a Command.execute() when finished.
 *
 * @author Mike Thompson
 */
public final class CountDownLatch {
    private final Command command;

    private int count;

    private CountDownLatch(int count, Command command) {
        this.command = command;
        this.count = count;
    }

    /**
     * Creates a countdown latch.
     *
     * @param count number of
     * @param command to execute after {@code count}
     * @return a new countdownlatch.
     */
    public static CountDownLatch create(int count, Command command) {
        return new CountDownLatch(count, command);
    }

    /**
     * countDown or decrement the count by one.
     *
     * @throws IllegalStateException if this counter has already been ticked the
     *         expected number of times.
     */
    public void countDown() {
        if(count <= 0){
           Log.error("Illegal State in countDownLatch.count : "+ count);
        }
        count--;
        if (count == 0) {
            // we have met our conditions execute the command
            command.execute();
        }
    }
}
