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

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.impl.remotepojo.RemotePojoInvocationCommandResponse;

/**
 * This is kind of "future" that can be used to wait for a remote POJO invocation. You typically use this to wait for an
 * asynchronous remote pojo invocation. Because this implements {@link CommandResponseCallback}, it can be used as the
 * callback to {@link ClientRemotePojoFactory#setAsynch(boolean, CommandResponseCallback)}.
 *
 * <p>This does not implement Future because its semantics are slightly different, but it does work in a similar manner.
 * Unlike Future, this object is not cancelable, but like Future, you can {@link #get()} the results blocking
 * indefinitely or you can supply a timeout via {@link #get(long, TimeUnit)}. This object can support multiple
 * "calculation results" - once you retreive the results, you should {@link #reset()} it in order to prepare this object
 * to receive another result. If you do not reset this object, the get methods will never block again, they will always
 * immediately return the last results it received. For this reason, it is preferable that you retreive the results via
 * {@link #get(long, TimeUnit)} or {@link #getAndReset(long, TimeUnit)} to force the reset to occur.</p>
 *
 * <p>This class is multi-thread safe, however, you must ensure that you call {@link #get()} to retrieve the results
 * before the callback method {@link #commandSent(CommandResponse)} is called again. If you do not, you will lose the
 * results for that previous invocation.</p>
 *
 * <p>Example usage:</p>
 *
 * <pre>
 * ClientRemotePojoFactory factory = ...
 * RemotePojoInvocationFuture future = new RemotePojoInvocationFuture();
 * factory.setAsync(true, future);
 * MyRemoteAPI pojo = factory.getRemotePojo(MyRemoteAPI.class);
 *
 * pojo.aMethodCall(); // will be sent asynchronously
 * MyObject o = (MyObject) future.getAndReset(); // blocks until aMethodCall really finishes
 *
 * pojo.anotherCall(); // another asynchronous request
 * AnotherObject o = (AnotherObject) future.getAndReset(); // blocks until anotherCall really finishes
 * </pre>
 *
 * @author John Mazzitelli
 */
public class RemotePojoInvocationFuture implements CommandResponseCallback {
    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    /**
     * When non-<code>null</code>, is the results of the last pojo invocation.
     */
    private RemotePojoInvocationCommandResponse m_results = null;

    /**
     * This will reset this object such that it can prepare to accept another remote pojo invocation result. Calling
     * this method will result in this object clearing out any current calculation result, thus making {@link #get()}
     * block until a new response is received via {@link #commandSent(CommandResponse)}. Note that this method will
     * block if another thread is currently waiting in {@link #get()} - it will unblock once that thread is done
     * waiting.
     */
    public void reset() {
        synchronized (this) {
            m_results = null;
        }

        return;
    }

    /**
     * This stores the new response in this object as this future's calculation result (any previous result is lost).
     * This new response's results object will be returned by {@link #get()}.
     *
     * @see CommandResponseCallback#commandSent(CommandResponse)
     */
    public void commandSent(CommandResponse response) {
        synchronized (this) {
            if (response instanceof RemotePojoInvocationCommandResponse) {
                m_results = (RemotePojoInvocationCommandResponse) response;
            } else {
                m_results = new RemotePojoInvocationCommandResponse(response);
            }

            this.notify();
        }

        return;
    }

    /**
     * Same as {@link #get()}, but before this method returns, this object is {@link #reset()}.
     *
     * @return the invocation results
     *
     * @throws InterruptedException
     * @throws ExecutionException
     *
     * @see    java.util.concurrent.Future#get()
     */
    public Object getAndReset() throws InterruptedException, ExecutionException {
        Object results = get();
        reset(); // note that if a concurrent thread calls get again, this will block; this rarely, if ever, should occur
        return results;
    }

    /**
     * Same as {@link #get(long, TimeUnit)}, but before this method returns, this object is {@link #reset()}.
     *
     * @param  timeout the maximum amount of time to wait
     * @param  unit    the unit of time that <code>timeout</code> is specified in
     *
     * @return the invocation results
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws TimeoutException
     *
     * @see    #get(long, TimeUnit)
     */
    public Object getAndReset(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
        TimeoutException {
        Object results = get(timeout, unit);
        reset(); // note that if a concurrent thread calls get again, this will block; this rarely, if ever, should occur
        return results;
    }

    /**
     * Blocks indefinitely until the remote invocation results are available, at which time those results are returned.
     * If an exception occurred during the invocation, that exception is stored as the cause in the thrown
     * {@link ExecutionException}.
     *
     * @return the remote invocation results
     *
     * @throws InterruptedException if the current thread waiting for the results is interrupted
     * @throws ExecutionException   if the remote invocation threw an exception.
     */
    public Object get() throws InterruptedException, ExecutionException {
        try {
            return get(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // this should never happen - it would mean we waited for Long.MAX_VALUE milliseconds!
            throw new ExecutionException(e);
        }
    }

    /**
     * Blocks for, at most, the specified amount of time or until the remote invocation results are available, at which
     * time those results are returned. If an exception occurred during the invocation, that exception is stored as the
     * cause in the thrown {@link ExecutionException}.
     *
     * @param  timeout the maximum amount of time to wait
     * @param  unit    the unit of time that <code>timeout</code> is specified in
     *
     * @return the remote invocation results
     *
     * @throws InterruptedException if the current thread waiting for the results is interrupted
     * @throws ExecutionException   if the remote invocation threw an exception.
     * @throws TimeoutException     if the given amount of time has expired before the results have been received
     */
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        RemotePojoInvocationCommandResponse result;
        long wait_ms = unit.toMillis(timeout);

        synchronized (this) {
            if (m_results == null) {
                this.wait(wait_ms);
            }

            result = m_results;
        }

        if (result == null) {
            throw new TimeoutException();
        } else if (!result.isSuccessful()) {
            ExecutionException exc_to_throw;
            Throwable result_exc = result.getException();

            if ((result_exc instanceof InvocationTargetException) && (result_exc.getCause() != null)) {
                exc_to_throw = new ExecutionException(result_exc.getCause());
            } else {
                exc_to_throw = new ExecutionException(result_exc);
            }

            throw exc_to_throw;
        }

        // the command response results object is the object returned by the remote method
        return result.getResults();
    }

    /**
     * Returns <code>true</code> if results are available and can be retrieved via the get methods without blocking.
     * Once the first remote invocation is done, this method will always return <code>true</code>, until this object is
     * {@link #reset()}.
     *
     * @return <code>true</code> if results are available; <code>false</code> if the invocation has not completed yet
     */
    public boolean isDone() {
        Object current_results = m_results; // assignment does not require synchronization
        return current_results != null;
    }
}