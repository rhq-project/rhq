/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.pc.component;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.rhq.core.pc.component.ComponentInvocationContextImpl.LocalContext;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

/**
 * @author Thomas Segismont
 */
@Test
public class ComponentInvocationContextImplTest {

    private ComponentInvocationContextImpl componentInvocationContext = new ComponentInvocationContextImpl();

    @Test(timeOut = 1000 * 30)
    public void newThreadsShouldSeeComponentInvocationContextInitialValue() throws Exception {
        InterruptedStatusRecorder recorder = new InterruptedStatusRecorder() {
            @Override
            public void run() {
                value = componentInvocationContext.isInterrupted();
            }
        };
        Thread thread = startRecorderThread(recorder);
        thread.join();
        assertEquals(recorder.getRecordedValue(), false);
    }

    @Test(timeOut = 1000 * 30)
    public void threadShouldSeeLocalContextValue() throws Exception {
        final LocalContext localContext = new LocalContext();
        InterruptedStatusRecorder recorder = new InterruptedStatusRecorder() {
            @Override
            public void run() {
                componentInvocationContext.setLocalContext(localContext);
                while (!componentInvocationContext.isInterrupted()) {
                    try {
                        Thread.sleep(SECONDS.toMillis(1));
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                value = componentInvocationContext.isInterrupted();
            }
        };
        Thread thread = startRecorderThread(recorder);
        localContext.markInterrupted();
        thread.join();
        assertEquals(recorder.getRecordedValue(), true);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void localContextCannotBeNull() {
        componentInvocationContext.setLocalContext(null);
    }

    private Thread startRecorderThread(InterruptedStatusRecorder recorder) {
        Thread thread = new Thread(recorder);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private abstract class InterruptedStatusRecorder implements Runnable {

        boolean value;

        boolean getRecordedValue() {
            return value;
        }
    }
}
