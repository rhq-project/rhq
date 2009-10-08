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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import mazz.i18n.Logger;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.impl.stream.RemoteOutputStreamCommand;
import org.rhq.enterprise.communications.command.impl.stream.RemoteOutputStreamCommandResponse;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * This is an output stream that actually sends the stream data to a remote server. Note that this extends <code>
 * OutputStream</code> so it can be used as any normal stream object; however, all methods are overridden to actually
 * delegate the methods to the remote stream.
 *
 * <p>In order to be able to use this object, you should understand how output streams are remoted. First, an input
 * stream must be {@link ServiceContainer#addRemoteOutputStream(OutputStream) assigned a server-side service component}.
 * At that point, your server is ready to begin accepting remote commands to write the output stream. That's where this
 * object comes in. You instantiate this object by giving its constructor the ID of the stream and the service container
 * where that output stream was registered. Note that, for convienence, the constructor
 * {@link RemoteOutputStream#RemoteOutputStream(OutputStream, ServiceContainer)} is provided to both add the output
 * stream to the server-side services container and instantiate this object with the appropriate stream ID. This
 * {@link RemoteOutputStream} object can then be passed to a remote client in some way (typically by serializing it as
 * part of a {@link Command}). The remote endpoint must then tell this object (after its been deserialized) which
 * {@link #setClientCommandSender(ClientCommandSender) client sender} to use when communicating back to the server
 * (which it needs to do when writing the stream data). We need that remote endpoint to give this object a sender
 * because its the remote endpoint's job to configure that sender with things like its keystore and truststore locations
 * (when transporting over SSL). In order to configure that client sender, the remote endpoint must create the sender
 * such that it uses the {@link #getServerEndpoint() server endpoint} as its target. After the remote endpoint sets up
 * its sender in this object, it is free to operate on this object as if it were a "normal" <code>OutputStream</code>.
 * Note that remote output streams should be {@link #close() closed} in order to clean up server-side resources in a
 * timely manner.</p>
 *
 * @author John Mazzitelli
 */
public class RemoteOutputStream extends OutputStream implements Serializable {
    private static final Logger LOG = CommI18NFactory.getLogger(RemoteOutputStream.class);

    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    private static Method CLOSE;
    private static Method FLUSH;
    private static Method WRITE_INT;
    private static Method WRITE_BYTEARRAY;
    private static Method WRITE_BYTEARRAY_INT_INT;

    static {
        try {
            CLOSE = OutputStream.class.getMethod("close", new Class[0]);
            FLUSH = OutputStream.class.getMethod("flush", new Class[0]);
            WRITE_INT = OutputStream.class.getMethod("write", new Class[] { Integer.TYPE });
            WRITE_BYTEARRAY = OutputStream.class.getMethod("write", new Class[] { byte[].class });
            WRITE_BYTEARRAY_INT_INT = OutputStream.class.getMethod("write", new Class[] { byte[].class, Integer.TYPE,
                Integer.TYPE });
        } catch (Exception e) {
            LOG.error(e, CommI18NResourceKeys.INVALID_OUTPUT_STREAM_METHOD);
        }
    }

    /**
     * The sender that will be used to actually send the command request to the remote server where the stream actually
     * lives. This is transient so when this object is deserialized, the owner of this object must set the sender to a
     * new one.
     */
    private transient ClientCommandSender m_sender;

    /**
     * Identifies the specific remote stream this object will operate on.
     */
    private final Long m_streamId;

    /**
     * This identifies the server endpoint that remote "clients" will use to connect to when they want to read in the
     * contents of the stream data.
     */
    private final String m_serverEndpoint;

    /**
     * Creates a new {@link RemoteOutputStream} object. This constructor is the same as
     * {@link RemoteOutputStream#RemoteOutputStream(Long, ServiceContainer)} but additionally adds the stream's
     * server-side component that is required in order for the stream to be remotely accessible (see
     * {@link ServiceContainer#addRemoteOutputStream(OutputStream)}).
     *
     * @param  stream the stream to remote
     * @param  server the server-side container that is responsible for remoting the stream
     *
     * @throws Exception if failed to add the remote output stream to the given container
     */
    public RemoteOutputStream(OutputStream stream, ServiceContainer server) throws Exception {
        this(server.addRemoteOutputStream(stream), server);
    }

    /**
     * Creates a new {@link RemoteOutputStream} object. Because the remote output stream needs a server-side component
     * to be able to serve up the stream data to remote endpoints, the given <code>server</code> is needed so this
     * object knows that server component's endpoint.
     *
     * @param id     identifies the stream this object will remotely access
     * @param server the server where the output stream service is registered and awaiting for the remote endpoint to
     *               begin sending it requests
     */
    public RemoteOutputStream(Long id, ServiceContainer server) {
        m_streamId = id;
        m_serverEndpoint = server.getServerEndpoint();
    }

    /**
     * Returns the endpoint of the server where the remote output stream is actually located. Its this server that must
     * be the endpoint of any given {@link #setClientCommandSender(ClientCommandSender) client sender}.
     *
     * @return the output stream's server endpoint
     */
    public String getServerEndpoint() {
        return m_serverEndpoint;
    }

    /**
     * Sets the sender that this output stream object will use to send the remote invocation requests. Note that the
     * target endpoint of that client sender must be the server identified by {@link #getServerEndpoint()}. Therefore,
     * before a caller calls this method, it must ensure that any sender it passes in has been configured to send its
     * messages to {@link #getServerEndpoint() the output stream's server endpoint}.
     *
     * @param sender object used to send the requests to the remote server
     */
    public void setClientCommandSender(ClientCommandSender sender) {
        m_sender = sender;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "RemoteOutputStream: stream-id=[" + m_streamId + "]; server-endpoint=[" + m_serverEndpoint + "]";
    }

    @Override
    public void close() throws IOException {
        sendRequest(CLOSE, null);
    }

    @Override
    public void flush() throws IOException {
        sendRequest(FLUSH, null);
    }

    @Override
    public void write(int b) throws IOException {
        sendRequest(WRITE_INT, new Object[] { new Integer(b) });
    }

    @Override
    public void write(byte[] b) throws IOException {
        sendRequest(WRITE_BYTEARRAY, new Object[] { b });
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        sendRequest(WRITE_BYTEARRAY_INT_INT, new Object[] { b, off, len });
    }

    /**
     * Builds the command to execute the method on the remote stream and submit the request.
     *
     * @param  method the method to invoke on the remote stream
     * @param  args   the arguments to pass to the invoked method on the remote stream
     *
     * @return the results of the invocation on the remote stream
     *
     * @throws RemoteIOException if either the sending of the request failed of the remote output stream actually
     *                           encountered a problem
     */
    protected Object sendRequest(Method method, Object[] args) throws RemoteIOException {
        if (m_sender == null) {
            throw new RemoteIOException(LOG.getMsgString(CommI18NResourceKeys.REMOTE_OUTPUT_STREAM_HAS_NO_SENDER,
                m_streamId, m_serverEndpoint));
        }

        RemoteOutputStreamCommandResponse response;
        RemoteOutputStreamCommand cmd = new RemoteOutputStreamCommand();

        cmd.setNameBasedInvocation(new NameBasedInvocation(method, args));
        cmd.setStreamId(m_streamId);

        try {
            response = new RemoteOutputStreamCommandResponse(m_sender.sendSynch(cmd));
        } catch (Exception e) {
            throw new RemoteIOException(e);
        }

        if (!response.isSuccessful()) {
            throw new RemoteIOException(response.getException());
        }

        return response.getResults();
    }
}