package org.rhq.enterprise.server.storage.maintenance.step;

import org.rhq.cassandra.schema.Table;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.enterprise.server.storage.maintenance.StepFailureException;
import org.rhq.enterprise.server.storage.maintenance.StepFailureStrategy;
import org.rhq.server.metrics.StorageSession;

/**
 * @author John Sanda
 */
public class UpdateSchema extends BaseStepRunner {

    protected static final int DEFAULT_OPERATION_TIMEOUT = 300;

    @Override
    public void execute() throws StepFailureException {
        Configuration configuration = step.getConfiguration();
        Integer replicationFactor = configuration.getSimple("replicationFactor").getIntegerValue();
        updateReplicationFactor(replicationFactor);

        PropertySimple gcGraceSeconds = configuration.getSimple("gcGraceSeconds");
        if (gcGraceSeconds != null) {
            updateGCGraceSeconds(gcGraceSeconds.getIntegerValue());
        }
    }

    private void updateReplicationFactor(int replicationFactor) {
        StorageSession session = storageClientManager.getSession();
        session.execute("ALTER KEYSPACE rhq WITH replication = {'class': 'SimpleStrategy', 'replication_factor': "
            + replicationFactor + "}");
        session.execute("ALTER KEYSPACE system_auth WITH replication = {'class': 'SimpleStrategy', "
            + "'replication_factor': " + replicationFactor + "}");
    }

    private void updateGCGraceSeconds(int seconds) {
        StorageSession session = storageClientManager.getSession();
        for (Table table : Table.values()) {
            session.execute("ALTER TABLE " + table.getTableName() + " WITH gc_grace_seconds = " + seconds);
        }
        session.execute("ALTER TABLE rhq.schema_version WITH gc_grace_seconds = " + seconds);
    }

    @Override
    public StepFailureStrategy getFailureStrategy() {
        return StepFailureStrategy.ABORT;
    }
}
