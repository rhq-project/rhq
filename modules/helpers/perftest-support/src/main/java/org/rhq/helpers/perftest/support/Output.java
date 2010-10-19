/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support;

import java.io.IOException;

import org.dbunit.dataset.stream.IDataSetConsumer;

/**
 * Implementations of this interface wrap the {@link IDataSetConsumer} instances and are able to
 * close the system resources when the consumer instance is no longer needed.
 *
 * @author Lukas Krejci
 */
public interface Output {
    
    /**
     * The returned consumer is assumed to consume a data set and produce some kind of output
     * (the specification of which is left for the subclasses to define).
     * 
     * @return the dbUnit data set consumer
     * @throws Exception
     */
    IDataSetConsumer getConsumer() throws Exception;

    /**
     * Closes any system resources that were used by the consumer.
     * 
     * @throws IOException
     */
    void close() throws IOException;
}