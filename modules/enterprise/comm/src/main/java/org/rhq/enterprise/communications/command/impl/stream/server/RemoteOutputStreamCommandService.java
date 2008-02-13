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
package org.rhq.enterprise.communications.command.impl.stream.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import mazz.i18n.Logger;
import org.jboss.remoting.invocation.NameBasedInvocation;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandExecutor;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.command.impl.stream.RemoteOutputStreamCommand;
import org.rhq.enterprise.communications.command.impl.stream.RemoteOutputStreamCommandResponse;
import org.rhq.enterprise.communications.command.server.CommandMBean;
import org.rhq.enterprise.communications.command.server.CommandService;
import org.rhq.enterprise.communications.command.server.CommandServiceMBean;
import org.rhq.enterprise.communications.i18n.CommI18NFactory;
import org.rhq.enterprise.communications.i18n.CommI18NResourceKeys;
import org.rhq.enterprise.communications.util.ClassUtil;

/**
 * Processes client requests to write remoted output streams.
 *
 * @author John Mazzitelli
 */
public class RemoteOutputStreamCommandService extends CommandService {
    private static final Logger LOG = CommI18NFactory.getLogger(RemoteOutputStreamCommandService.class);

    /**
     * Used for its monitor lock to synchronize access to the maps, timer and index counter.
     */
    private final Object m_lock = new Object();

    /**
     * An index counter that is incremented each time a new stream is added - these index numbers end up being keys to
     * the stream map and LAT map and are the stream IDs returned by the add method.
     */
    private long m_index;

    /**
     * The output streams that this service effectively remotes to its clients. The key values are the streams' index
     * numbers.
     */
    private final Map<Long, OutputStream> m_remotedOutputStreams;

    /**
     * The last access times for all the streams - the key values are the streams' index numbers.
     */
    private final Map<Long, AtomicLong> m_lastAccessTimes;

    /**
     * The maximum amount of milliseconds a stream is allowed to be idle before it will be removed and no longer
     * accessible to clients.
     */
    private long m_maxIdleTime;

    /**
     * This timer runs tasks that check when an output stream has been idle for a long time and closes/removes those
     * idle streams.
     */
    private Timer m_idleTimer;

    /**
     * Constructor for {@link RemoteOutputStreamCommandService}.
     */
    public RemoteOutputStreamCommandService() {
        m_index = 0L;
        m_remotedOutputStreams = new HashMap<Long, OutputStream>();
        m_lastAccessTimes = new HashMap<Long, AtomicLong>();
        m_maxIdleTime = 30000L;
        m_idleTimer = null;
    }

    /**
     * Adds the given output stream to this service, effectively allowing remote clients to access this stream. The
     * returned value is the ID that clients need to use to identify this stream as the one the client wants to access
     * (see {@link RemoteOutputStreamCommand#setStreamId(Long)}).
     *
     * @param  stream the new stream to remote
     *
     * @return the stream's ID
     */
    public Long addOutputStream(OutputStream stream) {
        Long stream_id;
        AtomicLong lat = new AtomicLong(System.currentTimeMillis());

        synchronized (m_lock) {
            stream_id = Long.valueOf(++m_index);

            m_remotedOutputStreams.put(stream_id, stream);
            m_lastAccessTimes.put(stream_id, lat);

            if (m_idleTimer == null) {
                m_idleTimer = new Timer("RHQ Idle Output Stream Timer Thread", true);
            }

            createIdleTimerTask(stream_id, lat);
        }

        LOG.debug(CommI18NResourceKeys.ADDED_REMOTE_OUTSTREAM, stream_id);

        return stream_id;
    }

    /**
     * Removes the stream associated with the given ID from this service, effectively making this stream inaccessible to
     * remote clients. This method also ensures the output stream is closed.
     *
     * @param  stream_id identifies the stream to remove
     *
     * @return <code>true</code> if the stream ID was valid and a stream was removed; <code>false</code> if the ID
     *         referred to a non-existent stream (which could mean either the stream was never registered at all or it
     *         was registered but has already been removed)
     */
    public boolean removeOutputStream(Long stream_id) {
        OutputStream doomed_stream;
        AtomicLong doomed_lat;

        synchronized (m_lock) {
            doomed_stream = m_remotedOutputStreams.remove(stream_id);
            doomed_lat = m_lastAccessTimes.remove(stream_id);

            if ((m_remotedOutputStreams.size() == 0) && (m_idleTimer != null)) {
                m_idleTimer.cancel();
                m_idleTimer = null;
            }
        }

        // just to be doubly sure we leave no resources hanging around, let's ensure the stream is closed
        if (doomed_stream != null) {
            try {
                doomed_stream.close();
            } catch (Throwable t) {
            }

            LOG.debug(CommI18NResourceKeys.REMOVED_REMOTE_OUTSTREAM, stream_id);
        }

        // set the LAT to quickly force the idle timer task for this stream to exit
        if (doomed_lat != null) {
            doomed_lat.set(0L);
        }

        return (doomed_stream != null);
    }

    /**
     * Configures the max idle time taken from this service's container configuration.
     *
     * @see CommandMBean#startService()
     */
    @Override
    public void startService() {
        super.startService();

        m_maxIdleTime = getServiceContainer().getConfiguration().getRemoteStreamMaxIdleTime();

        return;
    }

    /**
     * @see CommandMBean#stopService()
     */
    @Override
    public void stopService() {
        super.stopService();

        synchronized (m_lock) {
            Long[] doomed_ids = m_remotedOutputStreams.keySet().toArray(new Long[0]);

            for (int i = 0; i < doomed_ids.length; i++) {
                removeOutputStream(doomed_ids[i]); // also forces the streams to close and eventually kills timer
            }
        }

        return;
    }

    /**
     * Takes the remote stream access request, which has the NameBasedInvocation parameter, and convert that to a method
     * call on the target stream (using reflection). Then return the Object returned from the method call on the target
     * stream in the response. Note that the invocation signature must match one of the methods on <code>
     * OutputStream</code>.
     *
     * @see CommandExecutor#execute(Command, InputStream, OutputStream)
     */
    public CommandResponse execute(Command command, InputStream in, OutputStream out) {
        RemoteOutputStreamCommand remote_command = new RemoteOutputStreamCommand(command);
        NameBasedInvocation invocation = remote_command.getNameBasedInvocation();
        String method_name = invocation.getMethodName();
        Object[] params = invocation.getParameters();
        String[] signature = invocation.getSignature();
        Class<?>[] class_signature = new Class[signature.length];

        RemoteOutputStreamCommandResponse response;

        try {
            // get the stream that the command wants to access
            Long stream_id = remote_command.getStreamId();
            OutputStream the_stream;

            synchronized (m_lock) {
                the_stream = m_remotedOutputStreams.get(stream_id);

                if (the_stream == null) {
                    throw new IllegalArgumentException(LOG.getMsgString(CommI18NResourceKeys.INVALID_OUTSTREAM_ID,
                        stream_id, remote_command));
                }

                setLastAccess(stream_id, System.currentTimeMillis());
            }

            LOG.debug(CommI18NResourceKeys.INVOKING_OUTSTREAM_FROM_REMOTE_CLIENT, stream_id, method_name);

            // use reflection to make the call
            for (int x = 0; x < signature.length; x++) {
                class_signature[x] = ClassUtil.getClassFromTypeName(signature[x]);
            }

            Method method = OutputStream.class.getMethod(method_name, class_signature);
            Object results = method.invoke(the_stream, params);

            response = new RemoteOutputStreamCommandResponse(remote_command, results);

            // if the client has asked to close the stream, then we will ask that this service be deregistered as soon as possible
            // once the stream is closed, a client should not request anything else from our service (obviously, there is nothing else
            // this service can provide since the stream is now useless)
            if ("close".equals(method_name)) {
                setLastAccess(stream_id, 0L);
            }
        } catch (Exception e) {
            LOG.warn(e, CommI18NResourceKeys.FAILED_TO_INVOKE_OUTSTREAM_METHOD, method_name, remote_command);
            response = new RemoteOutputStreamCommandResponse(remote_command, e);
        }

        return response;
    }

    /**
     * Supports {@link RemoteOutputStreamCommand#COMMAND_TYPE remote output stream commands}.
     *
     * @see CommandServiceMBean#getSupportedCommandTypes()
     */
    public CommandType[] getSupportedCommandTypes() {
        return new CommandType[] { RemoteOutputStreamCommand.COMMAND_TYPE };
    }

    /**
     * Sets the last access time to the given timestamp for the stream identified by the given ID.
     *
     * @param stream_id identifies the stream that has just been accessed
     * @param timestamp the timestamp to be considered the new last access time for the stream
     */
    private void setLastAccess(Long stream_id, long timestamp) {
        AtomicLong lat;

        synchronized (m_lock) {
            lat = m_lastAccessTimes.get(stream_id);
        }

        // do not set the timestamp if it is 0 or less because that means the stream was closed or removed and should be reaped
        if ((lat != null) && (lat.get() > 0L)) {
            lat.set(timestamp);
        }

        return;
    }

    /**
     * This method will create a <code>TimerTask</code> whose responsibility is to check when the identified stream has
     * been idle for longer than the allowed max idle time and, when it is, to remove that stream from this service.
     *
     * @param stream_id the ID of the stream that this task will check
     * @param lat       the object containing the last time the stream was accessed
     */
    private void createIdleTimerTask(final Long stream_id, final AtomicLong lat) {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if ((System.currentTimeMillis() - lat.get()) > m_maxIdleTime) {
                    try {
                        if (removeOutputStream(stream_id)) {
                            LOG.debug(CommI18NResourceKeys.TIMER_TASK_REMOVED_IDLE_OUTSTREAM, stream_id, m_maxIdleTime);
                        }
                    } catch (Exception e) {
                        LOG.warn(CommI18NResourceKeys.TIMER_TASK_CANNOT_REMOVE_OUTSTREAM, stream_id, e);
                    }

                    // we don't need to run this task anymore - make sure we kill it
                    cancel();

                    return;
                }
            }
        };

        m_idleTimer.schedule(task, 5000L, 5000L);

        return;
    }
}