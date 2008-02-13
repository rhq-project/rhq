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
package org.rhq.enterprise.agent;

import java.io.IOException;
import org.rhq.enterprise.communications.command.server.discovery.AutoDiscoveryException;

/**
 * An annotated POJO used to test the effect of the annotations.
 *
 * @author John Mazzitelli
 */
public class SimpleTestAnnotatedPojo implements ITestAnnotatedPojo {
    /**
     * @see ITestAnnotatedPojo#volatileMethod(String)
     */
    public String volatileMethod(String str) {
        return str;
    }

    /**
     * @see ITestAnnotatedPojo#guaranteedMethod(String)
     */
    public String guaranteedMethod(String str) {
        return str;
    }

    /**
     * This should throw an exception because it should timeout before this method returns.
     *
     * @see ITestAnnotatedPojo#shortAnnotatedTimeout(String)
     */
    public String shortAnnotatedTimeout(String str) {
        try {
            // make sure this sleeps longer than the @Timeout annotation specified on this method in the interface
            Thread.sleep(1000L);
            return str;
        } catch (InterruptedException e) {
            return e.toString();
        }
    }

    /**
     * @see ITestAnnotatedPojo#sendThrottled(int)
     */
    public int sendThrottled(int num) {
        return num;
    }

    /**
     * @see ITestAnnotatedPojo#notSendThrottled(int)
     */
    public int notSendThrottled(int num) {
        return num;
    }

    /**
     * @see org.rhq.enterprise.agent.ITestAnnotatedPojo#returnNonSerializableObject(java.lang.Object)
     */
    public Object returnNonSerializableObject(Object o) {
        return new Thread(); // Thread is not serializable
    }

    /**
     * @see org.rhq.enterprise.agent.ITestAnnotatedPojo#throwNonSerializableException()
     */
    public void throwNonSerializableException() {
        class NotSerializableException extends RuntimeException {
            Thread t = new Thread();
        }

        throw new NotSerializableException();
    }

    /**
     * @see ITestAnnotatedPojo#throwThrowable(Throwable)
     */
    public void throwThrowable(Throwable t) throws Throwable {
        throw t;
    }

    /**
     * @see org.rhq.enterprise.agent.ITestAnnotatedPojo#throwSpecificException(java.lang.Exception)
     */
    public void throwSpecificException(Exception e) throws IOException, AutoDiscoveryException {
        try {
            throw e;
        } catch (IOException ioe) {
            throw ioe;
        } catch (AutoDiscoveryException ade) {
            throw ade;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e1) {
            throw new RuntimeException(
                "Wrote the test wrong - only pass in a RuntimeException or one that matches the throws clause", e1);
        }
    }
}