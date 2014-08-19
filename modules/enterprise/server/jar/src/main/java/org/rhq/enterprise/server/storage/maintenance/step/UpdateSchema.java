package org.rhq.enterprise.server.storage.maintenance.step;

import org.rhq.cassandra.schema.Table;
import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.OperationHistory;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.operation.bean.ResourceOperationSchedule;
import org.rhq.core.domain.util.PageList;
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

    protected OperationHistory executeOperation(String storageNodeAddress, String operation, Configuration parameters) {
        StorageNode node = storageNodeManager.findStorageNodeByAddress(storageNodeAddress);
        int resourceId = node.getResource().getId();
        ResourceOperationSchedule operationSchedule = operationManager.scheduleResourceOperation(
            subjectManager.getOverlord(), resourceId, operation, 0, 0, 0, StartStorageClient.DEFAULT_OPERATION_TIMEOUT,
            parameters.deepCopyWithoutProxies(), "");
        return waitForOperationToComplete(operationSchedule);
    }

    private OperationHistory waitForOperationToComplete(ResourceOperationSchedule schedule) {
        try {
            ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();
            criteria.addFilterJobId(schedule.getJobId());

            Thread.sleep(5000);
            PageList<ResourceOperationHistory> results = operationManager.findResourceOperationHistoriesByCriteria(
                subjectManager.getOverlord(), criteria);
            if (results.isEmpty()) {
                throw new RuntimeException("Failed to find resource operation history for " + schedule);
            }
            OperationHistory history = results.get(0);


            while (history.getStatus() == OperationRequestStatus.INPROGRESS) {
                Thread.sleep(5000);
                history = operationManager.getOperationHistoryByHistoryId(subjectManager.getOverlord(),
                    history.getId());
            }
            return history;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
