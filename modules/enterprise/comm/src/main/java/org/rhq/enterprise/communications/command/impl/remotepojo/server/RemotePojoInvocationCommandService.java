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
package org.rhq.enterprise.communications.command.impl.remotepojo.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Hashtable;
import java.util.Map;
import mazz.i18n.Logger;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.rhq.core.communications.command.annotation.LimitedConcurrency;
import org.rhq.core.util.exception.WrappedRemotingException;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandExecutor;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.command.client.RemoteInputStream;
import org.rhq.enterprise.communications.command.client.RemoteOutputStream;
import org.rhq.enterprise.communications.command.impl.remotepojo.RemotePojoInvocationCommand;
import org.rhq.enterprise.communications.command.impl.remotepojo.RemotePojoInvocationCommandResponse;
import org.rhq.enterprise.communications.command.server.CommandMBean;
import org.rhq.enterprise.communications.command.server.CommandService;
import org.rhq.enterprise.communications.command.server.CommandServiceMBean;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;
import org.rhq.enterprise.communications.util.ClassUtil;
import org.rhq.enterprise.communications.util.ConcurrencyManager;
import org.rhq.enterprise.communications.util.ConcurrencyManager.Permit;
import org.rhq.enterprise.communications.util.NotPermittedException;

/**
 * Processes a client request to invoke a remoted POJO.
 *
 * @author John Mazzitelli
 */
public class RemotePojoInvocationCommandService extends CommandService implements
    RemotePojoInvocationCommandServiceMBean {
    private static final Logger LOG = CommI18NFactory.getLogger(RemotePojoInvocationCommandService.class);

    /**
     * The set of remoted POJOs keyed on their classnames (as Strings).
     */
    private Map<String, Object> m_remotedPojos;

    /**
     * @see CommandMBean#startService()
     */
    @Override
    public void startService() {
        super.startService();

        m_remotedPojos = new Hashtable<String, Object>();
    }

    /**
     * @see CommandMBean#stopService()
     */
    @Override
    public void stopService() {
        super.stopService();

        m_remotedPojos.clear();
    }

    /**
     * @see RemotePojoInvocationCommandServiceMBean#addPojo(Object, String)
     */
    public void addPojo(Object pojo, String interfaceName) {
        try {
            Class<?> interfc = Class.forName(interfaceName, true, pojo.getClass().getClassLoader());

            if (!interfc.isAssignableFrom(pojo.getClass())) {
                throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.INVALID_POJO_INTERFACE, pojo,
                    interfaceName));
            }
        } catch (ClassNotFoundException cnfe) {
            throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.INVALID_INTERFACE, interfaceName,
                cnfe));
        }

        m_remotedPojos.put(interfaceName, pojo);

        return;
    }

    /**
     * @see RemotePojoInvocationCommandServiceMBean#addPojo(Object, Class)
     */
    public <T> void addPojo(T pojo, Class<T> remoteInterface) {
        m_remotedPojos.put(remoteInterface.getName(), pojo);
    }

    /**
     * @see RemotePojoInvocationCommandServiceMBean#removePojo(String)
     */
    public void removePojo(String remoteInterfaceName) {
        m_remotedPojos.remove(remoteInterfaceName);
    }

    /**
     * @see RemotePojoInvocationCommandServiceMBean#removePojo(Class)
     */
    public void removePojo(Class<?> remoteInterface) {
        m_remotedPojos.remove(remoteInterface.getName());
    }

    /**
     * Takes the remote POJO invocation request, which has the NameBasedInvocation parameter, and convert that to a
     * method call on the target POJO (using reflection). Then return the Object returned from the method call on the
     * target POJO in the response.
     *
     * @see CommandExecutor#execute(Command, java.io.InputStream, java.io.OutputStream)
     */
    public CommandResponse execute(Command command, InputStream in, OutputStream out) {
        RemotePojoInvocationCommand remote_pojo_command = new RemotePojoInvocationCommand(command);
        NameBasedInvocation invocation = remote_pojo_command.getNameBasedInvocation();
        String target_interface_name = remote_pojo_command.getTargetInterfaceName();
        String method_name = invocation.getMethodName();
        Object[] params = invocation.getParameters();
        String[] signature = invocation.getSignature();
        Class<?>[] class_signature = new Class[signature.length];

        Permit permit = null;
        Method pojo_method = null;

        RemotePojoInvocationCommandResponse response;

        ConcurrencyManager concurrency_manager = getServiceContainer().getConcurrencyManager();

        try {
            // look up the remote POJO that has the target interface
            Object pojo = m_remotedPojos.get(target_interface_name);

            if (pojo == null) {
                throw new NoSuchMethodException(LOG.getMsgString(CommI18NResourceKeys.NO_POJO_SERVICE, command));
            }

            for (int x = 0; x < signature.length; x++) {
                class_signature[x] = ClassUtil.getClassFromTypeName(signature[x]);
            }

            // If the remote POJO interface method has limited concurrency allowed, we need to make
            // sure we have permission to invoke that method. None of these calls should throw an exception.
            Class<?> target_interface = Class.forName(target_interface_name);
            Method target_method = target_interface.getMethod(method_name, class_signature);
            LimitedConcurrency limited_concurrency = target_method.getAnnotation(LimitedConcurrency.class);
            if ((limited_concurrency != null) && (concurrency_manager != null)) {
                permit = concurrency_manager.getPermit(limited_concurrency.value());
            }

            // if a parameter is a remote stream, we have to create a sender for it to use
            // this is needed in case the remote server that is serving the stream data requires SSL - in that
            // case, our sender needs to have SSL configured properly
            for (int x = 0; x < signature.length; x++) // yes use signature, not param - avoids possible NPE
            {
                if (params[x] instanceof RemoteInputStream) {
                    prepareRemoteInputStream((RemoteInputStream) params[x]);
                } else if (params[x] instanceof RemoteOutputStream) {
                    prepareRemoteOutputStream((RemoteOutputStream) params[x]);
                }
            }

            // use reflection to make the call
            pojo_method = pojo.getClass().getMethod(method_name, class_signature);
            Object response_object = pojo_method.invoke(pojo, params);

            response = new RemotePojoInvocationCommandResponse(remote_pojo_command, response_object);
        } catch (InvocationTargetException e) {
            // we want to make sure we keep the exception as intact as possible
            // so we still want to put the invocation target exception in the response,
            // but that is a java.* exception, so we need to drill down into the cause and wrap that if need be
            Throwable response_exception;

            if (e.getCause() != null) {
                response_exception = new InvocationTargetException(getWrappedException(e.getCause(), pojo_method), e
                    .getMessage());
            } else {
                response_exception = getWrappedException(e, pojo_method);
            }

            response = new RemotePojoInvocationCommandResponse(remote_pojo_command, response_exception);
        } catch (NotPermittedException npe) {
            LOG.warn(CommI18NResourceKeys.COMMAND_NOT_PERMITTED, target_interface_name + '.' + method_name, npe
                .getSleepBeforeRetry());
            response = new RemotePojoInvocationCommandResponse(remote_pojo_command, npe);
        } catch (Exception e) {
            LOG.warn(e, CommI18NResourceKeys.REMOTE_POJO_EXECUTE_FAILURE);
            response = new RemotePojoInvocationCommandResponse(remote_pojo_command, getWrappedException(e, pojo_method));
        } finally {
            if (concurrency_manager != null) {
                concurrency_manager.releasePermit(permit);
            }
        }

        return response;
    }

    /**
     * Supports {@link RemotePojoInvocationCommand#COMMAND_TYPE}.
     *
     * @see CommandServiceMBean#getSupportedCommandTypes()
     */
    public CommandType[] getSupportedCommandTypes() {
        return new CommandType[] { RemotePojoInvocationCommand.COMMAND_TYPE };
    }

    /**
     * Examines the given exception and if it matches a type that can be thrown from the given method, it is returned
     * as-is; otherwise, it is wrapped in an exception that is suitable for sending to a remote client. This method is
     * used because we want to ensure that the given exception can be processed on the other side. If we can be sure
     * that a remote client has the exception class definitions available to it, then we don't have to wrap the
     * exceptions (and we can be sure of this only if the exception is in the throws clause since the client has to have
     * the method definition for it to be able to remotely call the method in the first place). If the given exception
     * is not of a type explicitly declared in the method's throws clause, the exception is wrapped in a serializable,
     * "stringified" form. When wrapped, the client cannot catch the given exception's type explicitly - but since the
     * exception isn't declared in the method's throws clause, the client should have no reason to explicitly catch that
     * specific type (the client should catch java.lang.Exception if it wants to catch this wrapped exception).
     *
     * <p>If <code>e</code> is an exception in a package under "java.", the exception is assumed to be available to all
     * clients and thus not wrapped and returned as-is.</p>
     *
     * <p>If the exception is not one of the <code>java.</code> exceptions, and if <code>method</code> is <code>
     * null</code>, the exception will be wrapped.</p>
     *
     * <p>If the given exception is not serializable, it will be wrapped no matter what.</p>
     *
     * @param  e      the exception to check to see if it matches one of the types thrown by the given method
     * @param  method the method whose throws clause is to be searched to see if any matches the type of <code>e</code>.
     *
     * @return the exception, possibly wrapped in an exception suitable for sending over the wire to a client if need be
     */
    private Throwable getWrappedException(Throwable e, Method method) {
        // if its already wrapped, don't bother wrapping it again
        if (e instanceof WrappedRemotingException) {
            return e;
        }

        // we assume everyone has java.* exception definitions available
        // see if the exception and all its causes are all java.* exceptions
        Throwable check_for_java = e;
        boolean all_java_exceptions = true; // if false, e or one of its causes is not a java.* exception

        while (check_for_java != null) {
            if (!check_for_java.getClass().getName().startsWith("java.")) {
                all_java_exceptions = false;
                break; // don't bother continuing, we found a non-java.* exception
            }

            if (check_for_java.getCause() == check_for_java) {
                check_for_java = null; // reached the end of the causes chain
            } else {
                check_for_java = check_for_java.getCause();
            }
        }

        // if the exception and all its causes are java.*, then just return e as-is, unless its not serializable
        if (all_java_exceptions) {
            try {
                // make sure its serializable
                StreamUtil.serialize(e);
                return e;
            } catch (Exception ignore) {
                // not serializable for some reason (a cause within the exception was an inner class within a non-serialized class?)
                return new WrappedRemotingException(e);
            }
        }

        // if the exception occurred outside of invoking any method - wrap the exception
        if (method == null) {
            return new WrappedRemotingException(e);
        }

        Class<?>[] declared_exceptions = method.getExceptionTypes();

        // if the method did not declare any exceptions, wrap the exception
        if ((declared_exceptions == null) || (declared_exceptions.length == 0)) {
            return new WrappedRemotingException(e);
        }

        // check all declared exceptions and do not wrap the given exception if its one of those types
        for (Class<?> declared_exception : declared_exceptions) {
            // don't use isAssignableFrom since we a declared "java.lang.Exception" would match everything.
            if (declared_exception.getName().equals(e.getClass().getName())) {
                try {
                    // one final check - make sure its serializable
                    StreamUtil.serialize(e);
                    return e;
                } catch (Exception ignore) {
                    // not serializable for some reason (exception was an inner class within a non-serialized class?)
                    return new WrappedRemotingException(e);
                }
            }
        }

        // didn't match any of the method's declared exceptions - wrap it
        return new WrappedRemotingException(e);
    }
}