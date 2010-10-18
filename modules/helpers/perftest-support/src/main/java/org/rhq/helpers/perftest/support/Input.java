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

import org.dbunit.dataset.stream.IDataSetProducer;

/**
 * Implementations of this interface wrap the {@link IDataSetProducer} instances and are able to
 * close the system resources when the producer instance is no longer needed.
 *
 * @author Lukas Krejci
 */
public interface Input {
    
    /**
     * The returned producer is assumed to produce a data set that can then be fed into a database.
     * (the specification of the producer is left for the subclasses to define).
     * 
     * @return the dbUnit data set producer
     * @throws Exception
     */
    IDataSetProducer getProducer() throws Exception;

    /**
     * Closes any system resources that were used by the producer and prepares the input to
     * provide a new data set producer with the call to {@link #getProducer()}.
     * 
     * @throws IOException
     */
    void close() throws IOException;
}