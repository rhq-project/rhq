package org.rhq.test;

import javax.persistence.EntityManager;
import javax.transaction.TransactionManager;

public interface TransactionCallbackWithContext<T> {

    T execute(TransactionManager tm, EntityManager em) throws Exception;

}
