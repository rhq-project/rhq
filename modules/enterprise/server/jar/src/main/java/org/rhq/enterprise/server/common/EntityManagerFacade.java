package org.rhq.enterprise.server.common;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.rhq.enterprise.server.RHQConstants;

@Stateless
public class EntityManagerFacade implements EntityManagerFacadeLocal {

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    public Query createQuery(String queryStr) {
        return entityManager.createQuery(queryStr);
    }

    public void clear() {
        entityManager.clear();
    }

    public void flush() {
        entityManager.flush();
    }

}
