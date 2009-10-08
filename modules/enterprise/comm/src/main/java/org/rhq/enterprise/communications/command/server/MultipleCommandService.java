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
package org.rhq.enterprise.communications.command.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandExecutor;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.command.impl.generic.GenericCommandResponse;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * Provides some infrastructure to more easily facilitate the ability to execute different {@link Command commands}.
 * This class provides another level of indirection to locate the executors of commands. Where {@link CommandService} is
 * the object that explicitly executes the requested command, this class will actually hand off the command to the
 * actual {@link CommandExecutor} object that will execute the command.
 *
 * <p>This is similar to what the {@link CommandProcessor} does; however, this class allows a single, deployed MBean to
 * handle multiple, distinctly different, command types (whereas the command processor looks for
 * {@link CommandService command service MBeans} which do not easily support distinctly different commands (short of
 * implementing a series of <code>if-else</code> statements to perform <code>instanceof</code> checks on the incoming
 * command). Therefore, this class allows a single, deployed MBean to handle any number of commands, allowing for a
 * smaller deployment configuration (rather than being forced to deploy X MBeans to handle X commands; this allows you
 * to deploy 1 MBean to handle X commands).</p>
 *
 * <p>Usually, this class is helpful when a command service needs to execute distinctly different commands, but where
 * those commands are logically related to one another.</p>
 *
 * <p>Subclasses of this class should not override {@link CommandServiceMBean#getSupportedCommandTypes()}, instead they
 * must implement {@link #getSupportedCommandTypeExecutors()}.</p>
 *
 * <p>Subclasses also should not override {@link #execute(Command, InputStream, OutputStream)}.</p>
 *
 * <p>The executors this class uses can be instantiated on a per-request basis, or they may be reused (in which case,
 * the implementor of the {@link CommandExecutor} class must ensure its thread safety).</p>
 *
 * @author John Mazzitelli
 */
public abstract class MultipleCommandService extends CommandService {
    /**
     * contans a map keyed on {@link CommandType} whose values must be
     * {@link MultipleCommandService.CommandTypeExecutor}
     */
    private Map<CommandType, CommandTypeExecutor> m_executors;

    /**
     * Given a command to execute, this method will lookup that command's type to determine what
     * {@link CommandExecutor executor} should be used. Once the executor is determined, this method delegates to that
     * object to execute the command.
     *
     * @throws IllegalArgumentException if the command is of an invalid or unknown type
     *
     * @see    CommandExecutor#execute(Command, InputStream, OutputStream)
     */
    public CommandResponse execute(Command command, InputStream in, OutputStream out) {
        // find what executor should be responsible for executing the command
        CommandType commandTypeToExecute = command.getCommandType();
        CommandTypeExecutor typeExecutor = getExecutors().get(commandTypeToExecute);

        if (typeExecutor == null) {
            // this should never really happen; I can't think of a case where it would under normal circumstances
            throw new IllegalArgumentException(getLog()
                .getMsgString(CommI18NResourceKeys.UNKNOWN_COMMAND_TYPE, command));
        }

        // get the executor instance and hand off the command to it
        CommandResponse retResponse;

        try {
            retResponse = typeExecutor.getExecutor().execute(command, null, null);
        } catch (Throwable t) {
            retResponse = new GenericCommandResponse(command, false, null, t);
        }

        return retResponse;
    }

    /**
     * Subclasses to this class do not override this method; instead, they need to implement
     * {@link #getSupportedCommandTypeExecutors()}.
     *
     * @see CommandServiceMBean#getSupportedCommandTypes()
     */
    public CommandType[] getSupportedCommandTypes() {
        Set<CommandType> executorCommandTypes = getExecutors().keySet();

        return executorCommandTypes.toArray(new CommandType[executorCommandTypes.size()]);
    }

    /**
     * Gets the set of executors whose map keys are the {@link CommandType command types} and whose map values are
     * {@link CommandTypeExecutor} objects.
     *
     * <p>This is the method that actually calls the subclass' {@link #getSupportedCommandTypeExecutors()} and builds
     * the map from that. We create a <code>Map</code> as opposed to just using the array to make lookups by command
     * type faster.</p>
     *
     * @return map of executors
     */
    protected Map<CommandType, CommandTypeExecutor> getExecutors() {
        if (m_executors == null) {
            // synch just to avoid the rare occurrence of getting here concurrently
            // if this happens, we just rebuild the map twice, but that is harmless
            synchronized (this) {
                m_executors = new HashMap<CommandType, CommandTypeExecutor>();

                CommandTypeExecutor[] supportedExecutors = getSupportedCommandTypeExecutors();
                for (int i = 0; i < supportedExecutors.length; i++) {
                    m_executors.put(supportedExecutors[i].m_type, supportedExecutors[i]);
                }
            }
        }

        return m_executors;
    }

    /**
     * Returns a set of {@link CommandTypeExecutor} objects that define what command types this service supports and the
     * executors that will execute commands of those types.
     *
     * <p>The returned array should be fixed during the lifetime of this service (or at least during its registration in
     * an MBeanServer). Changes to the supported command types during runtime will not be detected once the
     * {@link CommandServiceDirectory} has discovered this service. As a corollary to this rule, this method must be
     * ready to provide the array of support command types at the time it is registered on an MBeanServer (in other
     * words, this method will be called, specifically by the {@link CommandServiceDirectory directory}, as soon as this
     * service is registered).</p>
     *
     * <p>Unlike direct subclasses to {@link CommandService}, subclasses of this class do not need to override
     * {@link CommandServiceMBean#getSupportedCommandTypes()}; instead, they override this method to inform the
     * framework not only what command types the subclass supports but also what executors should handle the execution
     * of commands.</p>
     *
     * @return array of supported command types/executors
     */
    protected abstract CommandTypeExecutor[] getSupportedCommandTypeExecutors();

    /**
     * An inner class used only by the {@link MultipleCommandService} that encasulates a supported command type and the
     * executor that should be used to execute commands of that type. Note the two constructors - one takes a <code>
     * java.lang.Class</code> and one an instance of {@link CommandExecutor}. If the <code>java.lang.Class</code>
     * constructor is used, then for each command request that is issued to this command service, a new instance should
     * be created to handle each command. If the {@link CommandExecutor} constructor is used, that means each command
     * that comes in should be handed off to that specific instance. In that case, the executor instance <b>must</b>
     * ensure thread-safety, since commands may come in concurrently.
     *
     * <p>Subclasses create instances of these objects and return them in
     * {@link MultipleCommandService#getSupportedCommandTypeExecutors()}.</p>
     *
     * @author John Mazzitelli
     */
    protected class CommandTypeExecutor {
        /**
         * the type of command this executor will handle
         */
        public final CommandType m_type;

        /**
         * the executor's class - this must implement {@link CommandExecutor}
         */
        public final Class m_executorClass;

        /**
         * if not <code>null</code>, this instance will handle all command executions for the command type (must be
         * thread safe!)
         */
        private CommandExecutor m_executorInstance;

        /**
         * Creates a new object that defines what class to instantiate for each new command to execute. Each new
         * instantiation of the given class will handle only a single command execution.
         *
         * @param  type          the type of command to be handed off to new instances of the given executor class
         * @param  executorClass class of the executor to instantiate when new commands are to be executed
         *
         * @throws IllegalArgumentException if the given class is an interface, an abstract class or not assignable to
         *                                  {@link CommandExecutor}; also if any parameter is <code>null</code>. Note
         *                                  that this is also thrown if the class is a {@link MultipleCommandService}
         *                                  object, since that would result in an infinite recursive loop.
         */
        public CommandTypeExecutor(CommandType type, Class executorClass) {
            if (type == null) {
                throw new IllegalArgumentException("type=null");
            }

            if (executorClass == null) {
                throw new IllegalArgumentException("executorClass=null");
            }

            boolean isAssignable = CommandExecutor.class.isAssignableFrom(executorClass);
            boolean isInterface = executorClass.isInterface();
            boolean isAbstract = Modifier.isAbstract(executorClass.getModifiers());
            boolean isRecursive = MultipleCommandService.class.isAssignableFrom(executorClass);

            if (!isAssignable || isInterface || isAbstract || isRecursive) {
                throw new IllegalArgumentException(getLog().getMsgString(CommI18NResourceKeys.INVALID_EXECUTOR_CLASS,
                    executorClass, CommandExecutor.class, MultipleCommandService.class));
            }

            m_type = type;
            m_executorClass = executorClass;
            m_executorInstance = null;
        }

        /**
         * Creates a new object that defines what executor instance to use to execute all commands of the given <code>
         * type</code>. Because all commands will be handed off to the given executor instance, that instance must
         * ensure thread-safety.
         *
         * @param  type             the type of command that will be handled by the given executor instance
         * @param  executorInstance the executor that will handle all commands of the given type
         *
         * @throws IllegalArgumentException if any parameter is <code>null</code> or if the instance is a
         *                                  {@link MultipleCommandService} object, since that would result in an
         *                                  infinite recursive loop.
         */
        public CommandTypeExecutor(CommandType type, CommandExecutor executorInstance) {
            if (type == null) {
                throw new IllegalArgumentException("type=null");
            }

            if (executorInstance == null) {
                throw new IllegalArgumentException("executorInstance=null");
            }

            if (executorInstance instanceof MultipleCommandService) {
                throw new IllegalArgumentException(getLog().getMsgString(
                    CommI18NResourceKeys.INVALID_EXECUTOR_INSTANCE, executorInstance.getClass(),
                    MultipleCommandService.class));
            }

            m_type = type;
            m_executorClass = executorInstance.getClass();
            m_executorInstance = executorInstance;
        }

        /**
         * Returns the executor instance that should be used to execute the next command.
         *
         * @return command executor that should be used to execute the next command
         *
         * @throws RuntimeException failed to create the executor instance (should rarely, if ever, occur)
         */
        public CommandExecutor getExecutor() {
            try {
                return (m_executorInstance != null) ? m_executorInstance : (CommandExecutor) m_executorClass
                    .newInstance();
            } catch (Exception e) {
                // convert to a runtime exception since this should rarely occur
                throw new RuntimeException(getLog().getMsgString(CommI18NResourceKeys.CANNOT_CREATE_EXECUTOR), e);
            }
        }
    }
}