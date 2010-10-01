package org.rhq.enterprise.gui.coregui.client.alert;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.rpc.RPCResponse;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.recent.alerts.RecentAlertsPortlet;

public class AlertPortletDataSource extends AlertDataSource {
    //configuration attributes
    private int alertRangeCompleted = -1;
    private int alertPriorityIndex = -1;
    private long alertTimeRange = -1;
    private String alertResourcesToUse = "all";
    private Integer[] alertFilterResourceIds = {};
    private DashboardPortlet portlet = null;

    public AlertPortletDataSource() {
        super();
    }

    public AlertPortletDataSource(DashboardPortlet recentAlertsPortlet) {
        super();
        this.portlet = recentAlertsPortlet;
    }

    /** Override the executeFetch for AlertPortlet to allow specifying smaller than total
     *  result displays.
     */
    protected void executeFetch(final DSRequest request, final DSResponse response) {
        final long start = System.currentTimeMillis();
        //retrieve previous settings from portlet config
        if ((portlet != null) && (this.portlet instanceof DashboardPortlet)) {
            //must check for whether portlet config
            PropertySimple property = portlet.getConfiguration().getSimple(
                RecentAlertsPortlet.ALERT_RANGE_RESOURCES_VALUE);
            if ((property != null) && (property.getStringValue() != null)) {
                //retrieve and translate to int
                String retrieved = property.getStringValue();
                if (retrieved.trim().equalsIgnoreCase(RecentAlertsPortlet.RESOURCES_SELECTED)) {
                    setAlertResourcesToUse(RecentAlertsPortlet.RESOURCES_SELECTED);
                } else {
                    setAlertResourcesToUse(RecentAlertsPortlet.RESOURCES_ALL);
                }
                //if 'selected' then check for previously set resource ids to filter on
                if (getAlertResourcesToUse().equals(RecentAlertsPortlet.RESOURCES_SELECTED)) {
                    Integer[] alertResourceFilterIds = null;
                    alertResourceFilterIds = extractFilterResourceIds(portlet, alertResourceFilterIds);
                    if (alertFilterResourceIds != null) {
                        setAlertFilterResourceId(alertFilterResourceIds);
                    }
                }
            } else {//create setting
                portlet.getConfiguration().put(
                    new PropertySimple(RecentAlertsPortlet.ALERT_RANGE_RESOURCES_VALUE,
                        RecentAlertsPortlet.defaultResourceValue));
                setAlertResourcesToUse(RecentAlertsPortlet.RESOURCES_ALL);
            }
        }

        AlertCriteria criteria = new AlertCriteria();
        criteria.fetchAlertDefinition(true);
        criteria.fetchRecoveryAlertDefinition(true);
        // TODO: Uncomment the below once the bad performance of it has been fixed.
        //criteria.fetchConditionLogs(true);
        PageControl pc = new PageControl(0, getAlertRangeCompleted());
        criteria.setPageControl(pc);//display per page
        criteria.addFilterStartTime(getAlertTimeRange());//alert age
        if ((getAlertResourcesToUse().equalsIgnoreCase(RecentAlertsPortlet.RESOURCES_SELECTED))
            && (getAlertFilterResourceIds().length > 0)) {
            //add resource ids to filter on
            criteria.addFilterResourceIds(getAlertFilterResourceIds());
        }
        if (getAlertPriorityIndex() > 0) {//add priority selection
            criteria.addFilterPriority(AlertPriority.getByLegacyIndex(getAlertPriorityIndex()));
        }

        getAlertService().findAlertsByCriteria(criteria, new AsyncCallback<PageList<Alert>>() {

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Failed to fetch alerts data", caught);
                response.setStatus(RPCResponse.STATUS_FAILURE);
                processResponse(request.getRequestId(), response);
            }

            public void onSuccess(PageList<Alert> result) {
                long fetchTime = System.currentTimeMillis() - start;
                com.allen_sauer.gwt.log.client.Log.info(result.size() + " alerts fetched in: " + fetchTime + "ms");
                response.setData(buildRecords(result));
                response.setTotalRows(result.size());
                processResponse(request.getRequestId(), response);
            }
        });
    }

    public int getAlertRangeCompleted() {
        return alertRangeCompleted;
    }

    public void setAlertRangeCompleted(int alertRangeCompleted) {
        this.alertRangeCompleted = alertRangeCompleted;
    }

    public int getAlertPriorityIndex() {
        return alertPriorityIndex;
    }

    public void setAlertPriorityIndex(int alertPriorityIndex) {
        this.alertPriorityIndex = alertPriorityIndex;
    }

    public long getAlertTimeRange() {
        return alertTimeRange;
    }

    public void setAlertTimeRange(long alertTimeRange) {
        this.alertTimeRange = alertTimeRange;
    }

    public String getAlertResourcesToUse() {
        return alertResourcesToUse;
    }

    public void setAlertResourcesToUse(String resourcesToUse) {
        this.alertResourcesToUse = resourcesToUse;
    }

    public Integer[] getAlertFilterResourceIds() {
        return alertFilterResourceIds;
    }

    public void setAlertFilterResourceId(Integer[] alertFilterResourceId) {
        this.alertFilterResourceIds = alertFilterResourceId;
    }

    public Integer[] extractFilterResourceIds(DashboardPortlet storedPortlet, Integer[] filterResourceIds) {
        PropertyList propertyList = storedPortlet.getConfiguration().getList(
            RecentAlertsPortlet.ALERT_RANGE_RESOURCE_IDS);
        if ((propertyList != null) && (propertyList.getList() != null) && (!propertyList.getList().isEmpty())
            && (propertyList.getList().get(0) != null)) {
            Property container = propertyList.getList().get(0);
            if (container instanceof PropertyList) {
                PropertyList anotherList = (PropertyList) container;
                if (anotherList.getList() != null) {
                    filterResourceIds = new Integer[anotherList.getList().size()];
                    int index = 0;
                    for (Property p : anotherList.getList()) {
                        filterResourceIds[index++] = ((PropertySimple) p).getIntegerValue();
                    }
                }
            }
        }
        return filterResourceIds;
    }

}
