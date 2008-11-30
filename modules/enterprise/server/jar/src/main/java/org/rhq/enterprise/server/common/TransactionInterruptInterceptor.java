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
package org.rhq.enterprise.server.common;

import java.lang.reflect.Method;
import java.util.Hashtable;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import com.arjuna.ats.arjuna.common.Uid;
import com.arjuna.ats.arjuna.coordinator.BasicAction;
import com.arjuna.ats.arjuna.coordinator.CheckedAction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.exception.ThrowableUtil;

/**
 * This EJB3 interceptor's job is to install a new JBossTM CheckedAction
 * on the request thread's current transaction.
 * 
 * The reason for this is technical, but in short, this allows SLSB methods
 * to be notified when a transaction has timed out so the SLSB can abort
 * what it is doing if it deems appropriate.
 * 
 * The current thread's transaction will get a new CheckedAction that will
 * simply interrupt all threads that are currently associated with it.
 * 
 * @author John Mazzitelli
 */
public class TransactionInterruptInterceptor {
    private static Log LOG = LogFactory.getLog(TransactionInterruptInterceptor.class);

    @AroundInvoke
    public Object addCheckedActionToTransactionManager(InvocationContext invocation_context) throws Exception {
        BasicAction currentTx = null;
        CheckedAction previousCheckedAction = null;

        try {
            currentTx = BasicAction.Current();

            // Don't bother doing anything if the thread is currently not in a transaction.
            // But if it is in a tx, then install our new CheckedAction unless the method
            // does not want to be told about the transaction timeout (it tells us this
            // via the InterruptOnTransactionTimeout(false) annotation).
            if (currentTx != null) {
                Method method = invocation_context.getMethod();
                InterruptOnTransactionTimeout anno = method.getAnnotation(InterruptOnTransactionTimeout.class);
                boolean interrupt = (anno != null) ? anno.value() : InterruptOnTransactionTimeout.DEFAULT_VALUE;
                TransactionInterruptCheckedAction newCheckedAction = new TransactionInterruptCheckedAction(interrupt);
                previousCheckedAction = currentTx.setCheckedAction(newCheckedAction);
            }
        } catch (Throwable t) {
            LOG.warn("Failure - if the transaction is aborted, its threads cannot be notified. Cause: "
                + ThrowableUtil.getAllMessages(t));
        }

        try {
            return invocation_context.proceed();
        } finally {
            if (currentTx != null && previousCheckedAction != null) {
                try {
                    currentTx.setCheckedAction(previousCheckedAction);
                } catch (Exception e) {
                    // paranoia - this should never happen, but ignore it if it does, keep the request going
                }
            }
        }
    }

    public class TransactionInterruptCheckedAction extends CheckedAction {
        private final boolean interruptThreads;

        public TransactionInterruptCheckedAction(boolean interrupt) {
            this.interruptThreads = interrupt;
        }

        @Override
        public synchronized void check(boolean isCommit, Uid actUid, Hashtable list) {
            try {
                // we only interrupt threads if we are not committing and we were told to interrupt threads
                boolean interrupt = this.interruptThreads && !isCommit;

                String template = "Transaction [" + actUid + "] is " + ((isCommit) ? "committing" : "aborting")
                    + " with active thread [{0}]. interrupting=[" + interrupt + ']';

                for (Object item : list.values()) {
                    Thread thread = (Thread) item;

                    // Show the full stacks to give a good indication of where the threads currently are;
                    // if we are configured to not show this information, just log a single-line warning
                    String logMsg = template.replace("{0}", thread.getName());
                    if (LOG.isInfoEnabled()) {
                        try {
                            Throwable t = new Throwable("STACK TRACE OF ACTIVE THREAD IN TERMINATING TRANSACTION");
                            t.setStackTrace(thread.getStackTrace());
                            LOG.info(logMsg, t);
                        } catch (Exception e) {
                            LOG.warn(logMsg); // paranoia - just in case throwable API doesn't behave
                        }
                    } else {
                        LOG.warn(logMsg);
                    }

                    if (interrupt) {
                        thread.interrupt();
                    }
                }
            } catch (Exception e) {
                LOG.warn(this.getClass() + ": check failed", e);
            }

            return; // don't bother calling super.check() - all it does is log warnings but is essentially a no-op
        }
    }
}