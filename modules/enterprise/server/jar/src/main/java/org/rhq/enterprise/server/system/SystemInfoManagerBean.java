package org.rhq.enterprise.server.system;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.common.jbossas.client.controller.CoreJBossASClient;
import org.rhq.common.jbossas.client.controller.MCCHelper;
import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.core.domain.common.ServerDetails;
import org.rhq.core.domain.common.composite.SystemSetting;
import org.rhq.core.domain.common.composite.SystemSettings;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.criteria.AlertDefinitionCriteria;
import org.rhq.core.domain.criteria.Criteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;

/**
 * Get system information
 * @author Heiko W. Rupp
 */
@Stateless
public class SystemInfoManagerBean implements  SystemInfoManagerLocal{

    Log log = LogFactory.getLog("SystemInfoManager");

    @EJB SystemManagerLocal systemManager;
    @EJB MeasurementScheduleManagerLocal scheduleManager;
    @EJB ResourceManagerLocal resourceManager;
    @EJB AlertManagerLocal alertManager;
    @EJB AlertDefinitionManagerLocal alertDefinitionManager;


    @Override
    public Map<String, String> getSystemInformation(Subject caller) {

        Map<String,String> result = new HashMap<String, String>();

        ServerDetails details = systemManager.getServerDetails(caller);
        Map<ServerDetails.Detail,String> detailsMap = details.getDetails();
        for (Map.Entry<ServerDetails.Detail,String> detail : detailsMap.entrySet()) {
                result.put(detail.getKey().toString(),detail.getValue());
        }
        ProductInfo productInfo = details.getProductInfo();
        result.put("BuildNumber", productInfo.getBuildNumber());
        result.put("FullName", productInfo.getFullName());
        result.put("Name", productInfo.getName());

        try {
            CoreJBossASClient coreClient = new CoreJBossASClient(MCCHelper.getModelControllerClient());
            result.put("AS version",coreClient.getAppServerVersion());
            result.put("AS product version", coreClient.getServerProductVersion());
            result.put("AS product name", coreClient.getServerProductName());
            result.put("AS config dir", coreClient.getAppServerConfigDir());
        } catch (Exception e) {
            result.put("AS*", "Not able to get AS props due to " + e.getMessage());
        }

        SystemSettings systemSettings=systemManager.getSystemSettings(caller);
        Map<String,String> settingsMap = systemSettings.toMap();
        // Don't use putAll(), as we need to filter out passwords
        for (Map.Entry<String,String> detail : settingsMap.entrySet()) {
            String key = detail.getKey();
            if (key.equals(SystemSetting.LDAP_BIND_PW.getInternalName())
                || key.equals(SystemSetting.HELP_PASSWORD.getInternalName())) {
                if (detail.getValue()==null)
                    result.put(key,"- null -");
                else
                    result.put(key,"- non null -");
            }
            else
                result.put(key,detail.getValue());
        }

        // system stats
        result.putAll(getStats(caller));

        return result;
    }

    @Override
    public void dumpToLog(Subject caller) {
        Map<String,String> infoMap = getSystemInformation(caller);

        StringBuilder builder = new StringBuilder("\n");
        for (Map.Entry<String,String> entry : infoMap.entrySet()) {
            builder.append(entry.getKey());
            builder.append(": [");
            builder.append(entry.getValue());
            builder.append("]\n");
        }
        log.info("SystemInformation: ********" + builder.toString() + "********");
    }

    private Map<String,String> getStats(Subject caller) {

        Map<String,String> result = new HashMap<String, String>();

        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterResourceCategories(ResourceCategory.PLATFORM);
        criteria.setRestriction(Criteria.Restriction.COUNT_ONLY);
        PageList<Resource> resList = resourceManager.findResourcesByCriteria(caller,criteria);
        result.put("PlatformCount",String.valueOf(resList.getTotalSize()));
        criteria = new ResourceCriteria();
        criteria.addFilterResourceCategories(ResourceCategory.SERVER);
        criteria.setRestriction(Criteria.Restriction.COUNT_ONLY);
        resList = resourceManager.findResourcesByCriteria(caller,criteria);
        result.put("ServerCount",String.valueOf(resList.getTotalSize()));
        criteria = new ResourceCriteria();
        criteria.addFilterResourceCategories(ResourceCategory.SERVICE);
        criteria.setRestriction(Criteria.Restriction.COUNT_ONLY);
        resList = resourceManager.findResourcesByCriteria(caller,criteria);
        result.put("ServiceCount",String.valueOf(resList.getTotalSize()));

        AlertCriteria alertCriteria = new AlertCriteria();
        alertCriteria.setRestriction(Criteria.Restriction.COUNT_ONLY);
        PageList<Alert> alertList = alertManager.findAlertsByCriteria(caller,alertCriteria);
        result.put("AlertCount",String.valueOf(alertList.getTotalSize()));

        AlertDefinitionCriteria alertDefinitionCriteria = new AlertDefinitionCriteria();
        alertDefinitionCriteria.setRestriction(Criteria.Restriction.COUNT_ONLY);
        PageList<AlertDefinition> defList = alertDefinitionManager.findAlertDefinitionsByCriteria(caller,alertDefinitionCriteria);
        result.put("AlertDefinitionCount",String.valueOf(defList.getTotalSize()));

        // status.setSchedules(-1); // TODO number of (active) schedules

        result.put("SchedulesPerMinute", String.valueOf(scheduleManager.getScheduledMeasurementsPerMinute()));

        return result;
    }
}
