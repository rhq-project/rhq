/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.server.util;

import java.util.Iterator;
import java.util.List;

/**
 * A convenience class for getting around the http://ora-01795.ora-code.com/ issue, which limits the size of the
 * in-clause parameters to 1000.  Can be used to take a large collection of ids, and iterate over them in batches.
 * The default batch size using the single-argument constructor is 1000, but the constructor which takes two arguments
 * allows overriding the batch size to any positive integer.
 * 
 * @author Joseph Marques
 * @author John Sanda
 */
public class BatchIterator<T> implements Iterable<List<T>> {

    public static final int DEFAULT_BATCH_SIZE = 1000;

    private int batchSize;
    private int index;
    private List<T> data;

    @Override
    public Iterator<List<T>> iterator() {
        return new Iterator<List<T>>() {
            @Override
            public boolean hasNext() {
                return hasMoreBatches();
            }

            @Override
            public List<T> next() {
                return getNextBatch();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("This iterator does not support the remove operation");
            }
        };
    }

    public BatchIterator(List<T> data) {
        this(data, DEFAULT_BATCH_SIZE);
    }

    public BatchIterator(List<T> data, int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batch size must be greater than zero");
        }
        this.batchSize = batchSize;
        this.index = 0;
        this.data = data;
    }

    public boolean hasMoreBatches() {
        return index < data.size();
    }

    public List<T> getNextBatch() {
        List<T> batch = null;

        if (index + batchSize < data.size()) {
            batch = data.subList(index, index + batchSize);
            index += batchSize;
        } else {
            batch = data.subList(index, data.size());
            index = data.size();
        }

        return batch;
    }
}
