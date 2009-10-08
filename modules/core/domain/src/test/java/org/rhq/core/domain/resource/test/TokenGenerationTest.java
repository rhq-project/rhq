 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.core.domain.resource.test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.testng.annotations.Test;
import org.rhq.core.domain.resource.Agent;

@Test
public class TokenGenerationTest {
    public void testUniqueTokenGeneration1() throws Exception {
        final Collection<String> tokens = Collections.synchronizedSet(new HashSet<String>());
        final int maxThreads = 20;
        final CountDownLatch latch = new CountDownLatch(maxThreads); // will open when all threads are done

        for (int i = 0; i < maxThreads; i++) {
            new Thread(new Runnable() {
                public void run() {
                    String token = Agent.generateRandomToken("seed"); // purposefully pass in the same string, see if we can break it
                    if (!tokens.add(token)) {
                        System.out.println("DUPLICATE TOKEN=" + token);
                    }

                    latch.countDown();
                }
            }).start();
        }

        assert latch.await(120, TimeUnit.SECONDS) : "Did not wait long enough for the threads to finish";
        assert tokens.size() == maxThreads : "One or more generated tokens were duplicated: " + maxThreads + " : "
            + tokens.size() + " : " + tokens;
    }

    public void testUniqueTokenGeneration2() throws Exception {
        final Collection<String> tokens = Collections.synchronizedSet(new HashSet<String>());
        final int maxThreads = 20;
        final CountDownLatch latch = new CountDownLatch(maxThreads); // will open when all threads are done

        for (int i = 0; i < maxThreads; i++) {
            final String seed = "seed" + i; // pass in a unique seed for each call to generate the token
            new Thread(new Runnable() {
                public void run() {
                    String token = Agent.generateRandomToken(seed);
                    if (!tokens.add(token)) {
                        System.out.println("DUPLICATE TOKEN=" + token);
                    }

                    latch.countDown();
                }
            }).start();
        }

        assert latch.await(120, TimeUnit.SECONDS) : "Did not wait long enough for the threads to finish";
        assert tokens.size() == maxThreads : "One or more generated tokens were duplicated: " + maxThreads + " : "
            + tokens.size() + " : " + tokens;
    }
}