package org.rhq.core.domain.util;

public interface TransactionCallback {

    void execute() throws Exception;

}
