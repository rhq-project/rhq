package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.List;

import com.datastax.driver.core.ResultSet;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * @author John Sanda
 */
class BatchFailureListener implements FutureFallback<ResultSet> {

    private List<Throwable> errors = new ArrayList<Throwable>();

    @Override
    public ListenableFuture<ResultSet> create(Throwable t) throws Exception {
        errors.add(t);
        return null;
    }

    List<Throwable> getErrors() {
        return errors;
    }
}
