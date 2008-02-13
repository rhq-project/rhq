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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.rhq.enterprise.communications.command.Command;
import org.rhq.enterprise.communications.command.CommandExecutor;
import org.rhq.enterprise.communications.command.CommandResponse;
import org.rhq.enterprise.communications.command.CommandType;
import org.rhq.enterprise.communications.command.client.RemoteInputStream;
import org.rhq.enterprise.communications.command.impl.generic.GenericCommandResponse;
import org.rhq.enterprise.communications.command.server.CommandService;
import org.rhq.enterprise.communications.command.server.CommandServiceMBean;
import org.rhq.enterprise.communications.util.StreamUtil;

/**
 * A test command service that processes data from an input stream and sends that data back in the response to confirm
 * the stream data was received. If {@link #RETURN_COUNT_ONLY_PARAM} is a parameter in the command, the command results
 * is a Long equal to the number of bytes in the stream. If that paramter does not exist, the full stream data is
 * slurped into memory and returned as a String in the command results.
 *
 * @author John Mazzitelli
 */
public class SimpleTestStreamService extends CommandService {
    /**
     * the parameter to hold the test input stream
     */
    public static final String INPUT_STREAM_PARAM = "the-input-stream";

    /**
     * A parameter whose value can be anything (even null) - if it is defined in the command then only the number of
     * bytes in the stream is returned in the results. If it is not specified, the full stream data is returned in the
     * results.
     */
    public static final String RETURN_COUNT_ONLY_PARAM = "return-count-only";

    /**
     * The test command that this service processes.
     */
    public static final CommandType COMMAND_TYPE = new CommandType("test-stream");

    /**
     * @see CommandServiceMBean#getSupportedCommandTypes()
     */
    public CommandType[] getSupportedCommandTypes() {
        return new CommandType[] { COMMAND_TYPE };
    }

    /**
     * @see CommandExecutor#execute(Command, InputStream, OutputStream)
     */
    public CommandResponse execute(Command command, InputStream in, OutputStream out) {
        RemoteInputStream stream = (RemoteInputStream) command.getParameterValue(INPUT_STREAM_PARAM);

        prepareRemoteInputStream(stream);

        GenericCommandResponse response;

        if (command.hasParameterValue(RETURN_COUNT_ONLY_PARAM)) {
            try {
                long stream_length = count(stream);
                response = new GenericCommandResponse(command, true, new Long(stream_length), null);
            } catch (Exception e) {
                response = new GenericCommandResponse(command, false, null, e);
            }
        } else {
            byte[] stream_data = StreamUtil.slurp(stream);
            response = new GenericCommandResponse(command, true, new String(stream_data), null);
        }

        return response;
    }

    /**
     * Counts the number of bytes in the stream but does not slurp it all into memory. Test large streams with this.
     *
     * @param  input the stream whose bytes are to be counted
     *
     * @return the number of bytes in the stream
     *
     * @throws Exception
     */
    public long count(InputStream input) throws Exception {
        long num_bytes = 0L;

        try {
            int bufferSize = 32768;
            input = new BufferedInputStream(input, bufferSize);
            byte[] buffer = new byte[bufferSize];
            for (int bytesRead = input.read(buffer); bytesRead != -1; bytesRead = input.read(buffer)) {
                num_bytes += bytesRead;
            }
        } finally {
            input.close();
        }

        return num_bytes;
    }
}