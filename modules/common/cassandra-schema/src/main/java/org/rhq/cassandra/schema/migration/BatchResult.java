package org.rhq.cassandra.schema.migration;

import com.datastax.driver.core.querybuilder.Batch;

/**
 * @author John Sanda
 */
public class BatchResult {

    private Batch batch;

    private boolean succeeded;

    public BatchResult(Batch batch, boolean succeeded) {
        this.batch = batch;
        this.succeeded = succeeded;
    }

    public Batch getBatch() {
        return batch;
    }

    public boolean succeeded() {
        return succeeded;
    }
}
