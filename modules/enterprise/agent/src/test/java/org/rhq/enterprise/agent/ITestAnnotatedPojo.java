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
import org.rhq.core.communications.command.annotation.Asynchronous;
import org.rhq.core.communications.command.annotation.DisableSendThrottling;
import org.rhq.core.communications.command.annotation.Timeout;
import org.rhq.enterprise.communications.command.server.discovery.AutoDiscoveryException;

/**
 * A POJO that is annotated.
 *
 * @author John Mazzitelli
 */
@Asynchronous(false)
@DisableSendThrottling
@Timeout(11000)
public interface ITestAnnotatedPojo {
    /**
     * Guaranteed delivery is turned off but it is asynchronous.
     *
     * @param  str a string to be echoed back
     *
     * @return the string echoed back
     */
    @Asynchronous
    public String volatileMethod(String str);

    /**
     * A method that is to be have delivery guaranteed.
     *
     * @param  str a string to be echoed back
     *
     * @return the string echoed back
     */
    @Asynchronous(guaranteedDelivery = true)
    public String guaranteedMethod(String str);

    /**
     * A method that has a short timeout - the method implementation should sleep for longer than the timeout to confirm
     * the annotated timeout value takes effect.
     *
     * @param  str a string to be echoed back
     *
     * @return the string echoed back
     */
    @Timeout(500)
    public String shortAnnotatedTimeout(String str);

    /**
     * This method is send throttled.
     *
     * @param  num a number to echo back
     *
     * @return the number echoed back
     */
    @DisableSendThrottling(false)
    public int sendThrottled(int num);

    /**
     * This method is explicitly not send throttled.
     *
     * @param  num a number to echo back
     *
     * @return the number echoed back
     */
    @DisableSendThrottling
    public int notSendThrottled(int num);

    /**
     * This attempts to return a non-serializable object. There is no throws clause here.
     *
     * @param  o some object - use this if you want to test trying to send a non-serializable object
     *
     * @return something, but it doesn't matter, cause it will be non-serializable and cause an exception
     */
    public Object returnNonSerializableObject(Object o);

    /**
     * This will throw an exception that is not serializable. There is no throws clause here.
     */
    public void throwNonSerializableException();

    /**
     * Will throw the given throwable.
     *
     * @param  t the throwable to throw
     *
     * @throws Throwable the given throwable <code>t</code>
     */
    public void throwThrowable(Throwable t) throws Throwable;

    /**
     * Will throw the given exception. This method explicitly declares a throws clause; specifically a java.* exception
     * and an application exception. Note that <code>e</code> can be anything; if its a runtime exception, it will be
     * thrown as-is; otherwise, it must match one of the types declared in the throws clause.
     *
     * @param  e
     *
     * @throws IOException
     * @throws AutoDiscoveryException
     */
    @Timeout(6000000)
    public void throwSpecificException(Exception e) throws IOException, AutoDiscoveryException;
}