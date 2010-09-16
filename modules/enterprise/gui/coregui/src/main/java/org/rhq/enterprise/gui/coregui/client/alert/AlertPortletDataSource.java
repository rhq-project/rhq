package org.rhq.enterprise.gui.coregui.client.alert;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.rpc.RPCResponse;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;

public class AlertPortletDataSource extends AlertDataSource {
    //configuration attributes
    private int alertRangeCompleted = -1;
    private int alertPriorityIndex = -1;
    private long alertTimeRange = -1;
    private String alertResourcesToUse = "all";
    private Integer[] alertFilterResourceIds = {};

    /** Override the executeFetch for AlertPortlet to allow specifying smaller than total
     *  result displays.
     */
    protected void executeFetch(final DSRequest request, final DSResponse response) {
        final long start = System.currentTimeMillis();

        AlertCriteria criteria = new AlertCriteria();
        criteria.fetchAlertDefinition(true);
        criteria.fetchRecoveryAlertDefinition(true);
        // TODO: Uncomment the below once the bad performance of it has been fixed.
        //criteria.fetchConditionLogs(true);
        PageControl pc = new PageControl(0, getAlertRangeCompleted());
        criteria.setPageControl(pc);//display per page
        criteria.addFilterStartTime(getAlertTimeRange());//alert age
        if (getAlertResourcesToUse().equalsIgnoreCase("selected")) {
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
                System.out.println(result.size() + " alerts fetched in: " + fetchTime + "ms");
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
}
