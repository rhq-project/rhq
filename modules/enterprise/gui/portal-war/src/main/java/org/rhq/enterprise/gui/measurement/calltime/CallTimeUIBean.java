package org.rhq.enterprise.gui.measurement.calltime;

import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.util.WebUtility;
import org.rhq.enterprise.server.common.EntityContext;
import org.rhq.enterprise.server.measurement.CallTimeDataManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Backing JSF managed bean for <code>/rhq/common/monitor/response.xhtml</code>.
 */
public class CallTimeUIBean extends PagedDataTableUIBean {

    private final Log log = LogFactory.getLog(this.getClass());

    public static final String MANAGED_BEAN_NAME = "CallTimeUIBean";

    private CallTimeDataManagerLocal callTimeDataManager = LookupUtil.getCallTimeDataManager();
    private MeasurementScheduleManagerLocal scheduleManager = LookupUtil.getMeasurementScheduleManager();

    private EntityContext context;

    public CallTimeUIBean() {
        context = WebUtility.getEntityContext();
    }

    public EntityContext getContext() {
        return this.context;
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListResourceMeasurementScheduleDataModel(PageControlView.CallTimeHistory, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListResourceMeasurementScheduleDataModel extends PagedListDataModel<CallTimeDataComposite> {
        public ListResourceMeasurementScheduleDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<CallTimeDataComposite> fetchPage(PageControl pc) {
            Subject subject = getSubject();
            WebUser user = EnterpriseFacesContextUtility.getWebUser();
            MeasurementPreferences preferences = user.getMeasurementPreferences();
            MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();

            PageList<CallTimeDataComposite> results = new PageList<CallTimeDataComposite>();
            if (context.category == EntityContext.Category.Resource) {
                int resourceId = getResource().getId();
                List<MeasurementSchedule> callTimeSchedules = scheduleManager
                    .findMeasurementSchedulesForResourceAndType(subject, resourceId, DataType.CALLTIME, null, false);
                if (callTimeSchedules.size() == 0) {
                    FacesContextUtility.addMessage(FacesMessage.SEVERITY_WARN,
                        "This resource does not support response time metrics.");
                } else if (callTimeSchedules.size() > 1) {
                    FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                        "This resource defines multiple resource time metrics - only one is allowed.");
                } else {
                    int scheduleId = callTimeSchedules.get(0).getId();
                    results = callTimeDataManager.getCallTimeDataForResource(subject, scheduleId,
                        rangePreferences.begin, rangePreferences.end, pc);
                }
            } else if (context.category == EntityContext.Category.ResourceGroup) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_WARN,
                    "Response time metrics are not yet supported for compatible groups.");
            } else if (context.category == EntityContext.Category.AutoGroup) {
                FacesContextUtility.addMessage(FacesMessage.SEVERITY_WARN,
                    "Response time metrics are not yet supported for auto-groups.");
            } else {
                log.error(context.getUnknownContextMessage());
            }

            return results;
        }
    }
}
