package org.rhq.enterprise.server.resource.metadata;

import java.util.ArrayList;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertTemplateManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

@Stateless
public class AlertMetadataManagerBean implements AlertMetadataManagerLocal {

    private static final Log log = LogFactory.getLog(AlertMetadataManagerBean.class);

    @EJB
    private AlertDefinitionManagerLocal alertDefinitionMgr;

    @EJB
    private AlertTemplateManagerLocal alertTemplateMgr;

    @Override
    public void deleteAlertTemplates(final Subject subject, ResourceType resourceType) {
        log.debug("Deleting alert templates for " + resourceType);

        AlertDefinitionCriteria criteria = new AlertDefinitionCriteria();
        criteria.addFilterAlertTemplateResourceTypeId(resourceType.getId());

        //Use CriteriaQuery to automatically chunk/page through criteria query results
        CriteriaQueryExecutor<AlertDefinition, AlertDefinitionCriteria> queryExecutor = new CriteriaQueryExecutor<AlertDefinition, AlertDefinitionCriteria>() {
            @Override
            public PageList<AlertDefinition> execute(AlertDefinitionCriteria criteria) {
                return alertDefinitionMgr.findAlertDefinitionsByCriteria(subject, criteria);
            }
        };

        CriteriaQuery<AlertDefinition, AlertDefinitionCriteria> templates = new CriteriaQuery<AlertDefinition, AlertDefinitionCriteria>(
            criteria, queryExecutor);

        ArrayList<Integer> templateIdList = new ArrayList<Integer>();
        int i = 0;
        for (AlertDefinition template : templates) {
            templateIdList.add(template.getId());
        }

        Integer[] templateIds = new Integer[templateIdList.size()];
        templateIds = templateIdList.toArray(templateIds);

        // Alert definitions associated with individual resources and with groups
        // are deleted as part of resource deletion. This commit adds support for
        // templates which are alert definitions associated with the resource
        // type
        alertTemplateMgr.removeAlertTemplates(subject, templateIds);
        alertDefinitionMgr.purgeUnusedAlertDefinitions();
    }
}
