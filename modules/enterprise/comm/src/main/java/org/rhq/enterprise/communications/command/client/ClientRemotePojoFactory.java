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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import mazz.i18n.Logger;
import org.jboss.remoting.CannotConnectException;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.rhq.core.communications.command.annotation.Asynchronous;
import org.rhq.core.communications.command.annotation.DisableSendThrottling;
import org.rhq.core.communications.command.annotation.Timeout;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.impl.remotepojo.RemotePojoInvocationCommand;
import org.rhq.enterprise.communications.command.impl.remotepojo.RemotePojoInvocationCommandResponse;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * This class is a factory of proxies that can be used to invoke methods on a remote POJO. This class provides
 * configuration settings like {@link #isDeliveryGuaranteed() guaranteed delivery}, {@link #getTimeout() the timeout},
 * {@link #isAsynch() async send mode} and {@link #isSendThrottled() send throttling}. These configuration settings
 * define how the proxies will behave. If a remote POJO interface is annotated to indicate those settings (e.g. with
 * {@link Timeout} or other remote POJO annotations), the annotations will take effect and override the settings in this
 * class unless this object was told to {@link #setIgnoreAnnotations(boolean) ignore those annotations}.
 *
 * @author John Mazzitelli
 */
public class ClientRemotePojoFactory {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(ClientRemotePojoFactory.class);

    /**
     * Used to send the invocation command.
     */
    private final ClientCommandSender m_clientCommandSender;

    /**
     * If <code>true</code>, any annotations found in the remote POJO interface will be ignored; all settings will be
     * derived from the settings in this object (thus allowing you override settings hardcoded in a POJO interface's
     * annotations).
     */
    private boolean m_ignoreAnnotations;

    /**
     * Will be <code>true</code> if remote invocations will be sent asynchronously. Will be <code>false</code> when the
     * remote POJO invocations should be made synchronously and thus act like a "normal" POJO method invocation. The
     * current value of this flag will be the one set on the proxy when {@link #getRemotePojo(Class)} is called.
     */
    private boolean m_asyncModeEnabled;

    /**
     * This is only used when this object's asynchronous mode is enabled and will be the callback that will be notified
     * when an asynchronous call is completed. This may be <code>null</code> indicating that nothing needs to be
     * notified when the asynchronous call is finished. The current value will be the one set on the proxy when
     * {@link #getRemotePojo(Class)} is called.
     */
    private CommandResponseCallback m_asyncCallback;

    /**
     * If not <code>null</code>, this is the request timeout that is to be used when sending remote POJO command
     * requests. It overrides the default timeout as configured in the client command sender.
     */
    private Long m_timeoutMillis;

    /**
     * Flag to indicate if POJO calls are to be submitted with guaranteed delivery enabled.
     */
    private boolean m_deliveryGuaranteed;

    /**
     * Flag to indicate if POJO calls are to be send-throttleable, that is, they must pass the send-throttle before it
     * can be sent.
     */
    private boolean m_sendThrottled;

    /**
     * Constructor for {@link ClientRemotePojoFactory}.
     *
     * @param ccs the object that will be used to send the remote POJO invocation command
     */
    public ClientRemotePojoFactory(ClientCommandSender ccs) {
        m_clientCommandSender = ccs;
        m_ignoreAnnotations = false;
        m_asyncModeEnabled = false;
        m_asyncCallback = null;
        m_timeoutMillis = null;
        m_deliveryGuaranteed = false;
        m_sendThrottled = true;

        return;
    }

    /**
     * This method returns a proxy to the remote POJO. The returned object can be used to make RPC calls to the remote
     * POJO as if the POJO was a local object. The returned proxy will have its async mode set
     * {@link #isAsynch() accordingly}. If asynchronous mode is to be enabled in this proxy, its callback will be set to
     * the object that was passed to {@link #setAsynch(boolean, CommandResponseCallback)} (which may or may not be a
     * <code>null</code> callback).
     *
     * @param  targetInterface
     *
     * @return the proxy to the remote POJO
     */
    @SuppressWarnings("unchecked")
    public <T> T getRemotePojo(Class<T> targetInterface) {
        // lets quickly get in and out of a synchronous block - store our current async mode and timeout in local variables
        boolean async_mode;
        CommandResponseCallback async_callback;
        Long timeout_millis;
        boolean guaranteed_delivery;
        boolean send_throttled;
        boolean ignore_annotations;

        synchronized (this) {
            ignore_annotations = m_ignoreAnnotations;
            async_mode = m_asyncModeEnabled;
            async_callback = m_asyncCallback;
            timeout_millis = m_timeoutMillis;
            guaranteed_delivery = m_deliveryGuaranteed;
            send_throttled = m_sendThrottled;
        }

        Class[] interfaces = new Class[] { targetInterface };
        ClassLoader class_loader = Thread.currentThread().getContextClassLoader();
        RemotePojoProxyHandler proxy_handler = new RemotePojoProxyHandler(targetInterface.getName(),
            ignore_annotations, async_mode, async_callback, timeout_millis, guaranteed_delivery, send_throttled);
        T proxy = (T) Proxy.newProxyInstance(class_loader, interfaces, proxy_handler);

        return proxy;
    }

    /**
     * If <code>true</code>, any remote POJO proxy returned by {@link #getRemotePojo(Class)} will ignore all annotations
     * define in that remote POJO's interface which allows you to override settings hardcoded in the annotations with
     * settings defined in this class (e.g. {@link #isSendThrottled()} will override any {@link DisableSendThrottling}
     * annotation defined in the remote POJO interface). If <code>false</code>, annotations that exist in the remote
     * POJO interface will take effect - thus overriding any of the settings defined in this object.
     *
     * @return flag to indicate of remote POJO interface annotations are ignored or not
     */
    public boolean isIgnoreAnnotations() {
        return m_ignoreAnnotations;
    }

    /**
     * Indicates if remote POJO interface annotations are to be ignored or not. Please see the description of
     * {@link #isIgnoreAnnotations()} for more information.
     *
     * @param ignore flag to indicate of remote POJO interface annotations are ignored or not
     */
    public void setIgnoreAnnotations(boolean ignore) {
        m_ignoreAnnotations = ignore;
    }

    /**
     * Returns <code>true</code> if new remote POJO proxies should make their invocations asynchronously; <code>
     * false</code> means the invocations will be synchronous (and thus act like a "normal" method call).
     *
     * @return the asynchronous flag
     *
     * @see    #setAsynch(boolean, CommandResponseCallback)
     */
    public boolean isAsynch() {
        synchronized (this) {
            return m_asyncModeEnabled;
        }
    }

    /**
     * Tells this object to make any new remote POJO proxies (via {@link #getRemotePojo(Class)}) such that they send
     * their invocations asynchronously. If <code>is_async</code> is <code>true</code>, then the optional <code>
     * callback</code> will be used as the callback object that will be notified when the asynchronous remote invocation
     * has been completed. The callback will receive {@link RemotePojoInvocationCommandResponse} objects as the
     * response. If <code>is_async</code> is <code>false</code>, all proxy remote POJO invocations will be synchronous,
     * and will thus act just like a "normal" POJO method call.
     *
     * <p>Setting the async mode will <b>not</b> effect any currently existing remote POJO proxies. If you change the
     * async mode, you will need to create a new proxy via {@link #getRemotePojo(Class)} and use that proxy to pick up
     * the new async mode.</p>
     *
     * <p>You can call this method to set a <code>callback</code> with <code>is_async</code> set to <code>false</code>;
     * this allows you to set a callback for use with any POJOs that might be annotated as {@link Asynchronous}.</p>
     *
     * <p>It is recommended that, if you want a callback, you use {@link RemotePojoInvocationFuture} as the callback
     * implementation because that object is able to directly handle {@link RemotePojoInvocationCommandResponse} objects
     * and can perform blocked waits.</p>
     *
     * @param is_async indicates if new proxies to remote POJOs should be in asynchronous mode
     * @param callback the callback to be notified when the invocation is complete (may be <code>null</code>)
     *
     * @see   RemotePojoInvocationFuture
     */
    public void setAsynch(boolean is_async, CommandResponseCallback callback) {
        synchronized (this) {
            m_asyncModeEnabled = is_async;
            m_asyncCallback = callback;
        }

        return;
    }

    /**
     * Returns the timeout (in milliseconds) that any new {@link #getRemotePojo(Class) proxies} will use for all POJO
     * calls. If <code>null</code> is returned, the default will be used (the default is defined by the
     * {@link ClientCommandSender} that was passed into this object's constructor). The timeout indicates how much time
     * the client should wait for the command to return with a response. If the timeout is exceeded, the command will
     * abort.
     *
     * @return the timeout (in milliseconds) that each command will be configured with; if <code>null</code>, a default
     *         will be used
     */
    public Long getTimeout() {
        synchronized (this) {
            return m_timeoutMillis;
        }
    }

    /**
     * Sets the timeout (in milliseconds) that all POJO calls will be configured with. See {@link #getTimeout()} for
     * more information.
     *
     * <p>Note that setting a new timeout value will <b>not</b> affect any existing proxies that this object previously
     * created via prior calls to {@link #getRemotePojo(Class)}. You must get a new remote POJO proxy in order for this
     * new timeout to take effect.</p>
     *
     * @param timeoutMillis the timeout (in milliseconds) that each command will be configured with;
     *                      if<code>null</code>, a default will be used
     */
    public void setTimeout(Long timeoutMillis) {
        synchronized (this) {
            m_timeoutMillis = timeoutMillis;
        }
    }

    /**
     * Returns the flag to indicate if the remote POJO calls should be made with guaranteed delivery.
     *
     * @return <code>true</code> if POJO calls should be made with guaranteed delivery; <code>false</code> otherwise
     */
    public boolean isDeliveryGuaranteed() {
        synchronized (this) {
            return m_deliveryGuaranteed;
        }
    }

    /**
     * Sets the flag to indicate if the remote POJO calls should be made with guaranteed delivery.
     *
     * <p>Note that setting a new guaranteed delivery value will <b>not</b> affect any existing proxies that this object
     * previously created via prior calls to {@link #getRemotePojo(Class)}. You must get a new remote POJO proxy in
     * order for this new flag to take effect.</p>
     *
     * @param guaranteed <code>true</code> if the remote POJO call should be made with guaranteed delivery
     */
    public void setDeliveryGuaranteed(boolean guaranteed) {
        synchronized (this) {
            m_deliveryGuaranteed = guaranteed;
        }
    }

    /**
     * Returns the flag to indicate if the remote POJO calls should be "send-throttleable", that is, will need to pass
     * the send-throttle before it will be sent.
     *
     * @return <code>true</code> if POJO calls are to be send-throttled
     */
    public boolean isSendThrottled() {
        synchronized (this) {
            return m_sendThrottled;
        }
    }

    /**
     * Sets the flag to indicate if the remote POJO calls should be "send-throttleable".
     *
     * <p>Note that setting a new send-throttled flag will <b>not</b> affect any existing proxies that this object
     * previously created via prior calls to {@link #getRemotePojo(Class)}. You must get a new remote POJO proxy in
     * order for this new flag to take effect.</p>
     *
     * @param throttled <code>true</code> if the remote POJO call should be send-throttled
     */
    public void setSendThrottled(boolean throttled) {
        synchronized (this) {
            m_sendThrottled = throttled;
        }
    }

    /**
     * The actual proxy object that submits the remote POJO invocation request. Each proxy will have its own
     * asynchronous mode enabled or disabled.
     */
    private class RemotePojoProxyHandler implements InvocationHandler {
        private String m_targetInterfaceName;
        private boolean m_proxyHandlerIgnoreAnnotations;
        private boolean m_proxyHandlerAsyncModeEnabled;
        private CommandResponseCallback m_proxyHandlerAsyncCallback;
        private Long m_proxyHandlerTimeout;
        private boolean m_proxyHandlerDeliveryGuaranteed;
        private boolean m_proxyHandlerSendThrottled;

        /**
         * Creates a new {@link RemotePojoProxyHandler} object with the async mode set appropriately. Note that the
         * <code>async_callback</code> will be ignored if <code>async_mode_enabled</code> is <code>false</code>.
         *
         * <p>If <code>timeout</code> is not <code>null</code>, it will be the timeout value that will override the
         * timeout value as configured in the client command sender. If it is <code>null</code>, the timeout used will
         * be the timeout that the client command sender defaults to.</p>
         *
         * @param target_interface_name the target interface that our proxy is to mimic
         * @param ignore_annotations    if <code>true</code>, the remote interface's annotations will be ignored
         * @param async_mode_enabled    if <code>true</code>, the method invocations to the remote POJO will be
         *                              asynchronous
         * @param async_callback        if not <code>null</code>, this is the callback that will be notified when
         *                              asynchronous invocations are complete
         * @param timeout_millis        if not <code>null</code>, will be the requests' timeout in milliseconds
         * @param delivery_guaranteed   if <code>true</code>, the call should be made with guaranteed delivery
         * @param send_throttled        if <code>true</code>, the call will only be made after it passes the send
         *                              throttle
         */
        public RemotePojoProxyHandler(String target_interface_name, boolean ignore_annotations,
            boolean async_mode_enabled, CommandResponseCallback async_callback, Long timeout_millis,
            boolean delivery_guaranteed, boolean send_throttled) {
            m_targetInterfaceName = target_interface_name;
            m_proxyHandlerIgnoreAnnotations = ignore_annotations;
            m_proxyHandlerAsyncModeEnabled = async_mode_enabled;
            m_proxyHandlerAsyncCallback = async_callback;
            m_proxyHandlerTimeout = timeout_millis;
            m_proxyHandlerDeliveryGuaranteed = delivery_guaranteed;
            m_proxyHandlerSendThrottled = send_throttled;
        }

        /**
         * This is called when a method on the remote POJO should be invoked.
         *
         * @see java.lang.reflect.InvocationHandler#invoke(Object, Method, Object[])
         */
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            String[] paramSig = createParamSignature(method.getParameterTypes());
            NameBasedInvocation invocation = new NameBasedInvocation(methodName, args, paramSig);
            RemotePojoInvocationCommandResponse response = null;
            Throwable throwable = null;

            RemotePojoInvocationCommand cmd = new RemotePojoInvocationCommand();
            cmd.setNameBasedInvocation(invocation);
            cmd.setTargetInterfaceName(m_targetInterfaceName);

            boolean async_mode_enabled = determineAsynchronous(method);
            Long timeout = determineTimeout(method);
            boolean guaranteed_delivery = determineGuaranteedDelivery(method);
            boolean send_throttle_enabled = determineSendThrottling(method);

            if (send_throttle_enabled) {
                cmd.getConfiguration().setProperty(ClientCommandSender.CMDCONFIG_PROP_SEND_THROTTLE,
                    Boolean.toString(true));
            }

            if (timeout != null) {
                cmd.getConfiguration().setProperty(ClientCommandSender.CMDCONFIG_PROP_TIMEOUT, timeout.toString());
            }

            try {
                if (async_mode_enabled) {
                    if (guaranteed_delivery) {
                        m_clientCommandSender.sendAsynchGuaranteed(cmd, m_proxyHandlerAsyncCallback);
                    } else {
                        m_clientCommandSender.sendAsynch(cmd, m_proxyHandlerAsyncCallback);
                    }
                } else {
                    CommandResponse sendsync_response = m_clientCommandSender.sendSynch(cmd);
                    response = new RemotePojoInvocationCommandResponse(sendsync_response);

                    // throw the exception if one occurred
                    if (response.getException() != null) {
                        throw response.getException();
                    }
                }
            } catch (CannotConnectException cce) {
                // we can perform some retry or failover mechanism here - see JBoss/Remoting TransporterClient for an example
                throwable = cce;
            } catch (InvocationTargetException ite) {
                Throwable root = ite.getCause();
                throwable = (root == null) ? ite : root;
            } catch (Throwable e) {
                throwable = e;
            }

            // if some failure occured, we need to do different things depending on the enablement of the async mode.
            // if async mode is enabled, let's create a command response to indicate the error and notify the callback
            // if async mode is disabled (i.e. we are in sync mode), then just throw the exception.
            // TODO [mazz]: do we want this behavior - the callback will get the failure response and that same response
            //              will be returned not sure if we just want to throw the exception no matter what
            if (throwable != null) {
                LOG.debug(CommI18NResourceKeys.CLIENT_REMOTE_POJO_INVOKER_EXECUTION_FAILURE, methodName, ThrowableUtil
                    .getAllMessages(throwable, true));

                if (async_mode_enabled) {
                    response = new RemotePojoInvocationCommandResponse(cmd, throwable);

                    if (m_proxyHandlerAsyncCallback != null) {
                        m_proxyHandlerAsyncCallback.commandSent(response);
                    }
                } else {
                    throw throwable;
                }
            }

            Object invoke_return = null;

            if (response != null) {
                invoke_return = response.getResults();

                // if the returned object is a remote stream, we need to tell it how to request stream data
                // by giving it the client command sender that will be used to send those requests
                if (invoke_return instanceof RemoteInputStream) {
                    ((RemoteInputStream) invoke_return).setClientCommandSender(m_clientCommandSender);
                } else if (invoke_return instanceof RemoteOutputStream) {
                    ((RemoteOutputStream) invoke_return).setClientCommandSender(m_clientCommandSender);
                }
            }

            return invoke_return;
        }

        /**
         * Converts the Class array to a String array of consisting of the class names
         *
         * @param  args class args
         *
         * @return the class array converted to strings
         */
        private String[] createParamSignature(Class<?>[] args) {
            if ((args == null) || (args.length == 0)) {
                return new String[] {};
            }

            String[] paramSig = new String[args.length];
            for (int x = 0; x < args.length; x++) {
                paramSig[x] = args[x].getName();
            }

            return paramSig;
        }

        /**
         * Determines if the method invocation should be performed asynchronously or not.
         *
         * @param  method the method to be invoked
         *
         * @return <code>true</code> if method to be invoked asynchronously; <code>false</code> otherwise
         */
        private boolean determineAsynchronous(Method method) {
            boolean ret_async_mode_enabled = m_proxyHandlerAsyncModeEnabled;

            if (!m_proxyHandlerIgnoreAnnotations) {
                Asynchronous annotation = method.getAnnotation(Asynchronous.class);

                if (annotation == null) {
                    annotation = method.getDeclaringClass().getAnnotation(Asynchronous.class);
                }

                if (annotation != null) {
                    ret_async_mode_enabled = annotation.value();
                }
            }

            return ret_async_mode_enabled;
        }

        /**
         * Determines what the timeout value should be for the method invocation.
         *
         * @param  method the method to be invoked
         *
         * @return the timeout value - will be <code>null</code> if no timeout is defined
         */
        private Long determineTimeout(Method method) {
            Long ret_timeout = m_proxyHandlerTimeout;

            if (!m_proxyHandlerIgnoreAnnotations) {
                Timeout annotation = method.getAnnotation(Timeout.class);

                if (annotation == null) {
                    annotation = method.getDeclaringClass().getAnnotation(Timeout.class);
                }

                if (annotation != null) {
                    ret_timeout = Long.valueOf(annotation.value());
                }
            }

            return ret_timeout;
        }

        /**
         * Determines if the method invocation should have invoked with guaranteed delivery enabled.
         *
         * @param  method the method to be invoked
         *
         * @return <code>true</code> if the method invocation is guaranteed to be delivered; <code>false</code>
         *         otherwise
         */
        private boolean determineGuaranteedDelivery(Method method) {
            boolean ret_delivery_guaranteed = m_proxyHandlerDeliveryGuaranteed;

            if (!m_proxyHandlerIgnoreAnnotations) {
                Asynchronous annotation = method.getAnnotation(Asynchronous.class);

                if (annotation == null) {
                    annotation = method.getDeclaringClass().getAnnotation(Asynchronous.class);
                }

                if (annotation != null) {
                    ret_delivery_guaranteed = annotation.guaranteedDelivery();
                }
            }

            return ret_delivery_guaranteed;
        }

        /**
         * Determines if the method invocation should have send throttling enabled or disabled.
         *
         * @param  method the method to be invoked
         *
         * @return <code>true</code> if send throttling is to be enabled; <code>false</code> otherwise
         */
        private boolean determineSendThrottling(Method method) {
            boolean ret_send_throttled = m_proxyHandlerSendThrottled;

            if (!m_proxyHandlerIgnoreAnnotations) {
                DisableSendThrottling annotation = method.getAnnotation(DisableSendThrottling.class);

                if (annotation == null) {
                    annotation = method.getDeclaringClass().getAnnotation(DisableSendThrottling.class);
                }

                if (annotation != null) {
                    ret_send_throttled = !annotation.value();
                }
            }

            return ret_send_throttled;
        }
    }
}