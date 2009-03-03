package org.rhq.enterprise.gui.measurement.schedule.autogroup;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;

import org.rhq.core.domain.measurement.composite.MeasurementScheduleComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.gui.util.StringUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.common.time.DurationComponent;
import org.rhq.enterprise.server.measurement.MeasurementScheduleManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ListAutoGroupMeasurementScheduleUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = "ListAutoGroupMeasurementScheduleUIBean";

    private MeasurementScheduleManagerLocal measurementScheduleManager = LookupUtil.getMeasurementScheduleManager();
    private DurationComponent duration;

    public DurationComponent getDuration() {
        return duration;
    }

    public void setDuration(DurationComponent duration) {
        this.duration = duration;
    }

    public String disableSelected() {
        int parentResourceId = FacesContextUtility.getRequiredRequestParameter("parent", Integer.class);
        int childResourceType = FacesContextUtility.getRequiredRequestParameter("type", Integer.class);
        int[] measurementDefinitionIds = getSelectedAutoGroupScheduleList();
        try {
            measurementScheduleManager.disableMeasurementSchedulesForAutoGroup(getSubject(), measurementDefinitionIds, parentResourceId,
                childResourceType);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Disabled " + measurementDefinitionIds.length + " schedules.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to disable selected schedules.", e);
        }
        return "success";
    }

    public String enableAndSetSelected() {
        long collectionInterval = duration.getMillis();
        int parentResourceId = FacesContextUtility.getRequiredRequestParameter("parent", Integer.class);
        int childResourceType = FacesContextUtility.getRequiredRequestParameter("type", Integer.class);
        int[] measurementDefinitionIds = getSelectedAutoGroupScheduleList();
        try {
            measurementScheduleManager.updateMeasurementSchedulesForAutoGroup(getSubject(), measurementDefinitionIds, parentResourceId,
                childResourceType, collectionInterval);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Enabled and set " + measurementDefinitionIds.length + " schedules.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to enabled and set selected schedules.", e);
        }
        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListAutoGroupMeasurementScheduleDataModel(PageControlView.AutoGroupMeasurementScheduleList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListAutoGroupMeasurementScheduleDataModel extends PagedListDataModel<MeasurementScheduleComposite> {
        public ListAutoGroupMeasurementScheduleDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<MeasurementScheduleComposite> fetchPage(PageControl pc) {
            int parentResourceId = FacesContextUtility.getRequiredRequestParameter("parent", Integer.class);
            int childResourceType = FacesContextUtility.getRequiredRequestParameter("type", Integer.class);
            return measurementScheduleManager.getMeasurementSchedulesForAutoGroup(getSubject(), parentResourceId, childResourceType, pc);
        }
    }

    private int[] getSelectedAutoGroupScheduleList() {
        String[] resourceSchedules = FacesContextUtility.getRequest().getParameterValues("selectedAutoGroupSchedules");
        return StringUtility.getIntArray(resourceSchedules);
    }

}
