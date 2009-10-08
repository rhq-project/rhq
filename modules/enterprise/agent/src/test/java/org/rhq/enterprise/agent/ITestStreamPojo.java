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

import java.io.InputStream;

/**
 * The interface to the test pojo that we will remote.
 *
 * @author John Mazzitelli
 */
public interface ITestStreamPojo {
    /**
     * Given an input stream, this slurps in the entire stream data and returns it as a string.
     *
     * @param  stream the stream to read the string data from
     *
     * @return the streamed data as a string
     */
    String streamData(InputStream stream);

    /**
     * Given input streams, this slurps in the entire stream data and returns them as strings.
     *
     * @param  stream1 the stream to read the string data from
     * @param  stream2 the stream to read the string data from
     *
     * @return the streamed data as a strings - stream1 in first element, stream2 in second
     */
    String[] streamData(InputStream stream1, InputStream stream2);

    /**
     * Given an input stream, this slurps in the entire stream data and returns it as a string. This simply has
     * arguments before and after the stream to make sure the POJO invocation mechanism can find the stream in any
     * argument - the extra parameters will be appended to the end of the return value after the stream data.
     *
     * @param  string1  a string to be appended to the return value
     * @param  integer2 an integer whose string value is to be appended to the return value
     * @param  stream   the stream to read the string data from
     * @param  string3  a string to be appended to the return value.
     *
     * @return the streamed data as a string
     */
    String streamData(String string1, int integer2, InputStream stream, String string3);
}