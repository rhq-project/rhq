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

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import org.jboss.remoting.transport.ConnectorMBean;
import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.command.client.ClientCommandSender;
import org.rhq.enterprise.communications.command.client.ClientCommandSenderConfiguration;
import org.rhq.enterprise.communications.command.client.RemoteInputStream;
import org.rhq.enterprise.communications.command.client.RemoteOutputStream;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * The superclass for all command services. Command services are providers of {@link CommandType command types} - that
 * is, they support the invocation of one or more {@link Command commands}.
 *
 * <p>Extending this class gives all command services the same interface, which is how the {@link CommandProcessor} can
 * be extended to execute arbitrary commands.</p>
 *
 * <p>All command services are registered with the {@link CommandServiceDirectoryMBean command service directory}, which
 * allows the command processor to find this service and its supported commands easily.</p>
 *
 * <p>Note that subclasses do not need define their own MBean interface; they need only inherit this service's MBean
 * interface in order to plug into the command processor infrastructure.</p>
 *
 * <p>This class is typically used to process requests of the same kind of command, or perhaps different versions of the
 * same kind of command. To facilitate easier support for multiple commands, see {@link MultipleCommandService}. Use
 * that class if a command service expects to process many different kinds of commands. That is not to say direct
 * subclasses of this class cannot support multiple commands - they can. However, it typically will involve the execute
 * method containing a series of <code>if-then-else</code> statements to check the <code>instanceof</code> value of the
 * incoming {@link Command} object.</p>
 *
 * @author John Mazzitelli
 */
public abstract class CommandService extends CommandMBean implements CommandServiceMBean {
    /**
     * If this service was registered by a container, that container's reference will be stored here.
     */
    private ServiceContainer m_container = null;

    /**
     * Ensures that the name this command service is to be registered on is valid. See {@link KeyProperty} for more
     * information on the valid key properties.
     *
     * <p>If the name is invalid, an exception is thrown and the service will not get registered.</p>
     *
     * @throws IllegalArgumentException if the name does not follow the required format needed to interact with the
     *                                  command processor
     *
     * @see    MBeanRegistration#preRegister(MBeanServer, ObjectName)
     */
    @Override
    public ObjectName preRegister(MBeanServer mbs, ObjectName name) throws Exception {
        if (!KeyProperty.TYPE_COMMAND.equals(name.getKeyProperty(KeyProperty.TYPE))) {
            String errorMsg = CommI18NFactory.getMsgWithLoggerLocale().getMsg(
                CommI18NResourceKeys.INVALID_CMD_SERVICE_NAME, name, KeyProperty.TYPE, KeyProperty.TYPE_COMMAND);
            throw new IllegalArgumentException(errorMsg);
        }

        return super.preRegister(mbs, name);
    }

    /**
     * If this service was registered by the {@link ServiceContainer}, that container's reference will be returned. If
     * this service object has not yet been registered or registered by some other mechanism, this will return <code>
     * null</code>.
     *
     * @return the server-side components container that registered this service
     */
    public ServiceContainer getServiceContainer() {
        return m_container;
    }

    /**
     * When this service is added by the {@link ServiceContainer}, that container's reference will be set via this
     * method. This allows this command service to obtain its container (in case this service needs things like the
     * {@link ServiceContainer#getClientConfiguration() client configuration}.
     *
     * @param container
     */
    public void setServiceContainer(ServiceContainer container) {
        m_container = container;
    }

    /**
     * Gets the connector providing our remote services. The returned connector is the object that is accepting from
     * remote clients the commands that this command service processes.
     *
     * @return this service's associated connector
     *
     * @throws Exception if failed to get the connector
     */
    protected ConnectorMBean getConnector() throws Exception {
        ConnectorMBean connector;

        connector = (ConnectorMBean) MBeanServerInvocationHandler.newProxyInstance(getMBeanServer(),
            ServiceContainer.OBJECTNAME_CONNECTOR, ConnectorMBean.class, false);

        return connector;
    }

    /**
     * Returns the subsystem that this command service is registered under.
     *
     * @return the subsystem or <code>null</code> if not registered
     */
    protected String getSubsystem() {
        ObjectName obj_name = getObjectName();
        String subsystem = null;

        if (obj_name != null) {
            subsystem = obj_name.getKeyProperty(KeyProperty.SUBSYSTEM);
        }

        return subsystem;
    }

    /**
     * Returns the command service ID that this command service is registered under - will be <code>null</code> if not
     * registered or was not registered with {@link ServiceContainer#addCommandService(CommandService)}.
     *
     * @return the command service ID or <code>null</code> if not registered
     */
    protected CommandServiceId getCommandServiceId() {
        ObjectName obj_name = getObjectName();
        CommandServiceId id = null;

        if (obj_name != null) {
            String index = obj_name.getKeyProperty(KeyProperty.ID);

            if (index != null) {
                id = new CommandServiceId(index);
            }
        }

        return id;
    }

    /**
     * This is a convienence method for use by those subclass services that need to process incoming
     * {@link RemoteInputStream remote input streams} that were received from a client. Remote input streams require a
     * connection back to the client that sent the stream - this is so this service can pull down the input stream data.
     * This method prepares the given remote input stream with a properly configured client command sender targeted to
     * send commands back to the client that sent the remote input stream.
     *
     * @param remote_stream the stream to be prepared with a sender
     */
    protected void prepareRemoteInputStream(RemoteInputStream remote_stream) {
        String server_endpoint = remote_stream.getServerEndpoint();
        ClientCommandSenderConfiguration config = getServiceContainer().getClientConfiguration();

        // we know we only make synchronous calls with non-guaranteed commands, so disable guaranteed delivery
        // setting the filename to null also ensures our sender doesn't try to access the spool file at all
        config.commandSpoolFileName = null;

        // ensure that we do not throttle the stream commands
        config.enableQueueThrottling = false;
        config.enableSendThrottling = false;

        // do not do any polling
        config.serverPollingIntervalMillis = 0;

        // i don't think we ever do async calls to remote streams, but just in case,
        // we can't interleave them, so make sure we make calls serially
        config.maxConcurrent = 1;

        ClientCommandSender sender = getServiceContainer().createClientCommandSender(server_endpoint, config);
        sender.startSending();
        remote_stream.setClientCommandSender(sender);

        return;
    }

    /**
     * This is a convienence method for use by those subclass services that need to process incoming
     * {@link RemoteOutputStream remote output streams} that were received from a client. Remote output streams require
     * a connection back to the client that sent the stream - this is so this service can push down the output stream
     * data. This method prepares the given remote output stream with a properly configured client command sender
     * targeted to send commands back to the client that sent the remote output stream.
     *
     * @param remote_stream the stream to be prepared with a sender
     */
    protected void prepareRemoteOutputStream(RemoteOutputStream remote_stream) {
        String server_endpoint = remote_stream.getServerEndpoint();
        ClientCommandSenderConfiguration config = getServiceContainer().getClientConfiguration();

        // we know we only make synchronous calls with non-guaranteed commands, so disable guaranteed delivery
        // setting the filename to null also ensures our sender doesn't try to access the spool file at all
        config.commandSpoolFileName = null;

        // ensure that we do not throttle the stream commands
        config.enableQueueThrottling = false;
        config.enableSendThrottling = false;

        // do not do any polling
        config.serverPollingIntervalMillis = 0;

        // i don't think we ever do async calls to remote streams, but just in case,
        // we can't interleave them, so make sure we make calls serially
        config.maxConcurrent = 1;

        ClientCommandSender sender = getServiceContainer().createClientCommandSender(server_endpoint, config);
        sender.startSending();
        remote_stream.setClientCommandSender(sender);

        return;
    }
}