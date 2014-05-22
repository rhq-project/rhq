package org.rhq.server.metrics.aggregation;

import java.util.ArrayList;
import java.util.List;

import org.rhq.core.util.exception.ThrowableUtil;

/**
 * @author John Sanda
 */
class BatchException extends Exception {

    private int startScheduleId;

    private List<Throwable> errors;

    public BatchException(int startScheduleId, List<Throwable> errors) {
        this.startScheduleId = startScheduleId;
        this.errors = errors;
    }

    int getStartScheduleId() {
        return startScheduleId;
    }

    List<Throwable> getErrors() {
        return errors;
    }

    List<String> getErrorMessages() {
        List<String> messages = new ArrayList<String>(errors.size());
        for (Throwable error : errors) {
            messages.add(ThrowableUtil.getRootMessage(error));
        }
        return messages;
    }

    List<Throwable> getRootCauses() {
        List<Throwable> rootCauses = new ArrayList<Throwable>(errors.size());
        for (Throwable error : errors) {
            rootCauses.add(ThrowableUtil.getRootCause(error));
        }
        return rootCauses;
    }
}
