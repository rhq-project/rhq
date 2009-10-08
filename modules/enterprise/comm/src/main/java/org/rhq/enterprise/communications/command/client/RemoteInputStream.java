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
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import mazz.i18n.Logger;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.rhq.enterprise.communications.ServiceContainer;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.impl.stream.RemoteInputStreamCommand;
import org.rhq.enterprise.communications.command.impl.stream.RemoteInputStreamCommandResponse;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;

/**
 * This is an input stream that actually pulls down the stream data from a remote server. Note that this extends <code>
 * InputStream</code> so it can be used as any normal stream object; however, all methods are overridden to actually
 * delegate the methods to the remote stream.
 *
 * <p>In order to be able to use this object, you should understand how input streams are remoted. First, an input
 * stream must be {@link ServiceContainer#addRemoteInputStream(InputStream) assigned a server-side service component}.
 * At that point, your server is ready to begin accepting remote commands to read the input stream. That's where this
 * object comes in. You instantiate this object by giving its constructor the ID of the stream and the service container
 * where that input stream was registered. Note that, for convienence, the constructor
 * {@link RemoteInputStream#RemoteInputStream(InputStream, ServiceContainer)} is provided to both add the input stream
 * to the server-side services container and instantiate this object with the appropriate stream ID. This
 * {@link RemoteInputStream} object can then be passed to a remote client in some way (typically by serializing it as
 * part of a {@link Command}). The remote endpoint must then tell this object (after its been deserialized) which
 * {@link #setClientCommandSender(ClientCommandSender) client sender} to use when communicating back to the server
 * (which it needs to do when pulling the stream data). We need that remote endpoint to give this object a sender
 * because its the remote endpoint's job to configure that sender with things like its keystore and truststore locations
 * (when transporting over SSL). In order to configure that client sender, the remote endpoint must create the sender
 * such that it uses the {@link #getServerEndpoint() server endpoint} as its target. After the remote endpoint sets up
 * its sender in this object, it is free to operate on this object as if it were a "normal" <code>InputStream</code>.
 * Note that remote input streams should be {@link #close() closed} in order to clean up server-side resources in a
 * timely manner.</p>
 *
 * @author John Mazzitelli
 */
public class RemoteInputStream extends InputStream implements Serializable {
    /**
     * Logger
     */
    private static final Logger LOG = CommI18NFactory.getLogger(RemoteInputStream.class);

    /**
     * the UID to identify the serializable version of this class
     */
    private static final long serialVersionUID = 1L;

    private static Method AVAILABLE;
    private static Method CLOSE;
    private static Method MARK;
    private static Method MARKSUPPORTED;
    private static Method READ;
    private static Method READBYTEARRAY;
    private static Method READBYTEARRAY_LEN;
    private static Method RESET;
    private static Method SKIP;

    static {
        try {
            AVAILABLE = InputStream.class.getMethod("available", new Class[0]);
            CLOSE = InputStream.class.getMethod("close", new Class[0]);
            MARK = InputStream.class.getMethod("mark", new Class[] { Integer.TYPE });
            MARKSUPPORTED = InputStream.class.getMethod("markSupported", new Class[0]);
            READ = InputStream.class.getMethod("read", new Class[0]);
            READBYTEARRAY = InputStream.class.getMethod("read", new Class[] { byte[].class });
            READBYTEARRAY_LEN = InputStream.class.getMethod("read", new Class[] { byte[].class, Integer.TYPE,
                Integer.TYPE });
            RESET = InputStream.class.getMethod("reset", new Class[0]);
            SKIP = InputStream.class.getMethod("skip", new Class[] { Long.TYPE });
        } catch (Exception e) {
            LOG.error(e, CommI18NResourceKeys.INVALID_INPUT_STREAM_METHOD);
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
     * Creates a new {@link RemoteInputStream} object. This constructor is the same as
     * {@link RemoteInputStream#RemoteInputStream(Long, ServiceContainer)} but additionally adds the stream's
     * server-side component that is required in order for the stream to be remotely accessible (see
     * {@link ServiceContainer#addRemoteInputStream(InputStream)}).
     *
     * @param  stream the stream to remote
     * @param  server the server-side container that is responsible for remoting the stream
     *
     * @throws Exception if failed to add the remote input stream to the given container
     */
    public RemoteInputStream(InputStream stream, ServiceContainer server) throws Exception {
        this(server.addRemoteInputStream(stream), server);
    }

    /**
     * Creates a new {@link RemoteInputStream} object. Because the remote input stream needs a server-side component to
     * be able to serve up the stream data to remote endpoints, the given <code>server</code> is needed so this object
     * knows that server component's endpoint.
     *
     * @param id     identifies the stream this object will remotely access
     * @param server the server where the input stream service is registered and awaiting for the remote endpoint to
     *               begin sending it requests
     */
    public RemoteInputStream(Long id, ServiceContainer server) {
        m_streamId = id;
        m_serverEndpoint = server.getServerEndpoint();
    }

    /**
     * Returns the endpoint of the server where the remote input stream is actually located. Its this server that must
     * be the endpoint of any given {@link #setClientCommandSender(ClientCommandSender) client sender}.
     *
     * @return the input stream's server endpoint
     */
    public String getServerEndpoint() {
        return m_serverEndpoint;
    }

    /**
     * Sets the sender that this input stream object will use to send the remote invocation requests. Note that the
     * target endpoint of that client sender must be the server identified by {@link #getServerEndpoint()}. Therefore,
     * before a caller calls this method, it must ensure that any sender it passes in has been configured to send its
     * messages to {@link #getServerEndpoint() the input stream's server endpoint}.
     *
     * @param sender object used to send the requests to the remote server
     */
    public void setClientCommandSender(ClientCommandSender sender) {
        m_sender = sender;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "RemoteInputStream: stream-id=[" + m_streamId + "]; server-endpoint=[" + m_serverEndpoint + "]";
    }

    /**
     * @see java.io.InputStream#available()
     */
    @Override
    public int available() throws IOException {
        return ((Integer) sendRequest(AVAILABLE, null)).intValue();
    }

    /**
     * @see java.io.InputStream#close()
     */
    @Override
    public void close() throws IOException {
        sendRequest(CLOSE, null);
    }

    /**
     * @see java.io.InputStream#mark(int)
     */
    @Override
    public void mark(int readlimit) {
        try {
            sendRequest(MARK, new Object[] { new Integer(readlimit) });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see java.io.InputStream#markSupported()
     */
    @Override
    public boolean markSupported() {
        try {
            return ((Boolean) sendRequest(MARKSUPPORTED, null)).booleanValue();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @see java.io.InputStream#read()
     */
    @Override
    public int read() throws IOException {
        return ((Integer) sendRequest(READ, null)).intValue();
    }

    /**
     * @see java.io.InputStream#read(byte[])
     */
    @Override
    public int read(byte[] b) throws IOException {
        return ((Integer) sendRequest(READBYTEARRAY, new Object[] { b })).intValue();
    }

    /**
     * @see java.io.InputStream#read(byte[], int, int)
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return ((Integer) sendRequest(READBYTEARRAY_LEN, new Object[] { b, off, len })).intValue();
    }

    /**
     * @see java.io.InputStream#reset()
     */
    @Override
    public void reset() throws IOException {
        sendRequest(RESET, null);
    }

    /**
     * @see java.io.InputStream#skip(long)
     */
    @Override
    public long skip(long n) throws IOException {
        return ((Long) sendRequest(SKIP, new Object[] { n })).longValue();
    }

    /**
     * Builds the command to execute the method on the remote stream and submit the request.
     *
     * @param  method the method to invoke on the remote stream
     * @param  args   the arguments to pass to the invoked method on the remote stream
     *
     * @return the results of the invocation on the remote stream
     *
     * @throws RemoteIOException if either the sending of the request failed of the remote input stream actually
     *                           encountered a problem
     */
    protected Object sendRequest(Method method, Object[] args) throws RemoteIOException {
        if (m_sender == null) {
            throw new RemoteIOException(LOG.getMsgString(CommI18NResourceKeys.REMOTE_INPUT_STREAM_HAS_NO_SENDER,
                m_streamId, m_serverEndpoint));
        }

        RemoteInputStreamCommandResponse response;
        RemoteInputStreamCommand cmd = new RemoteInputStreamCommand();

        cmd.setNameBasedInvocation(new NameBasedInvocation(method, args));
        cmd.setStreamId(m_streamId);

        try {
            response = new RemoteInputStreamCommandResponse(m_sender.sendSynch(cmd));
        } catch (Exception e) {
            throw new RemoteIOException(e);
        }

        if (!response.isSuccessful()) {
            throw new RemoteIOException(response.getException());
        }

        // let's mimic pass by reference by copying the bytes read into the byte[] array parameter.
        // note that the only way that read_bytes could be non-null was if our command had
        // a byte[] as its first parameter.  Therefore, read_bytes!=null directly infers that args[0] is a byte[]
        // and they will have identical sizes.
        byte[] read_bytes = response.getBytesReadFromStream();
        if (read_bytes != null) {
            // two sanity checks here:
            // first, args[0] should be a byte[], if not, it is a bug
            // second, the arrays passed in the command and got back in the response should be the same length, its a bug otherwise
            assert (args.length > 0) && (args[0] instanceof byte[]);
            assert read_bytes.length == ((byte[]) args[0]).length;

            System.arraycopy(read_bytes, 0, args[0], 0, read_bytes.length);
        }

        return response.getResults();
    }
}