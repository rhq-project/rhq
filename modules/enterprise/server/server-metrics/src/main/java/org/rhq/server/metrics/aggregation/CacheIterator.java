package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

import org.rhq.server.metrics.CacheMapper;
import org.rhq.server.metrics.domain.NumericMetric;

/**
* @author John Sanda
*/
class CacheIterator<T extends NumericMetric> implements Iterator<List<T>> {

    private CacheMapper<T> mapper;

    private T currentMetric;

    private Iterator<Row> rowIterator;

    public CacheIterator(CacheMapper<T> mapper, ResultSet resultSet) {
        this.mapper = mapper;
        rowIterator = resultSet.iterator();
        if (rowIterator.hasNext()) {
            currentMetric = mapper.map(rowIterator.next());
        }
    }

    @Override
    public boolean hasNext() {
        return rowIterator.hasNext() || currentMetric != null;
    }

    @Override
    public List<T> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        List<T> metrics = new ArrayList<T>();
        metrics.add(currentMetric);

        while (rowIterator.hasNext()) {
            T nextMetric = mapper.map(rowIterator.next());
            if (currentMetric.getScheduleId() == nextMetric.getScheduleId()) {
                currentMetric = nextMetric;
                metrics.add(currentMetric);
            } else {
                currentMetric = nextMetric;
                return metrics;
            }
        }
        currentMetric = null;
        return metrics;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
