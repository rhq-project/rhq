package org.rhq.core.domain.shared;

public interface TransactionCallback {

    void execute() throws Exception;

}
