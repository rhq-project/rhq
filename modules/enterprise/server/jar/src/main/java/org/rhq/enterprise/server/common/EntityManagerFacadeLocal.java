package org.rhq.enterprise.server.common;

import javax.ejb.Local;
import javax.persistence.Query;

@Local
public interface EntityManagerFacadeLocal {
    Query createQuery(String queryStr);
}
