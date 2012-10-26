package org.rhq.enterprise.server.test;

public interface TransactionCallbackReturnable<T extends Object> {

    T execute() throws Exception;

}
