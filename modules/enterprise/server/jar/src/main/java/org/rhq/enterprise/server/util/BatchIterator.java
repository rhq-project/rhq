package org.rhq.enterprise.server.util;

import java.util.Iterator;
import java.util.List;

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
