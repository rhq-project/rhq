package org.rhq.enterprise.server.subsystem;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.composite.ConfigurationUpdateComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.core.domain.util.PersistenceUtility;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;

/**
 * @author Joseph Marques
 */
@Stateless
public class ConfigurationSubsystemManagerBean implements ConfigurationSubsystemManagerLocal {

    //private final Log log = LogFactory.getLog(ConfigurationSubsystemManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @SuppressWarnings("unchecked")
    public PageList<ConfigurationUpdateComposite> getResourceConfigurationUpdates(Subject subject,
        String resourceFilter, String parentFilter, Long startTime, Long endTime, ConfigurationUpdateStatus status,
        PageControl pc) {
        pc.initDefaultOrderingField("cu.id", PageOrdering.DESC);

        String queryName = null;
        if (authorizationManager.isInventoryManager(subject)) {
            queryName = ResourceConfigurationUpdate.QUERY_FIND_ALL_COMPOSITES_ADMIN;
        } else {
            queryName = ResourceConfigurationUpdate.QUERY_FIND_ALL_COMPOSITES;
        }

        Query queryCount = PersistenceUtility.createCountQuery(entityManager, queryName);
        Query query = PersistenceUtility.createQueryWithOrderBy(entityManager, queryName, pc);

        if (authorizationManager.isInventoryManager(subject) == false) {
            queryCount.setParameter("subjectId", subject.getId());
            query.setParameter("subjectId", subject.getId());
        }

        resourceFilter = PersistenceUtility.formatSearchParameter(resourceFilter);
        parentFilter = PersistenceUtility.formatSearchParameter(parentFilter);

        queryCount.setParameter("resourceFilter", resourceFilter);
        query.setParameter("resourceFilter", resourceFilter);
        queryCount.setParameter("parentFilter", parentFilter);
        query.setParameter("parentFilter", parentFilter);
        queryCount.setParameter("startTime", startTime);
        query.setParameter("startTime", startTime);
        queryCount.setParameter("endTime", endTime);
        query.setParameter("endTime", endTime);
        queryCount.setParameter("status", status);
        query.setParameter("status", status);

        long totalCount = (Long) queryCount.getSingleResult();
        List<ConfigurationUpdateComposite> updates = query.getResultList();

        return new PageList<ConfigurationUpdateComposite>(updates, (int) totalCount, pc);
    }
}
