package org.rhq.enterprise.gui.measurement.schedule.resource;

import javax.faces.application.FacesMessage;
import javax.faces.model.DataModel;

import org.rhq.core.domain.measurement.MeasurementSchedule;
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

public class ListResourceMeasurementScheduleUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = "ListResourceMeasurementScheduleUIBean";

    private MeasurementScheduleManagerLocal measurementScheduleManager = LookupUtil.getMeasurementScheduleManager();
    private DurationComponent duration;

    public DurationComponent getDuration() {
        return duration;
    }

    public void setDuration(DurationComponent duration) {
        this.duration = duration;
    }

    public String disableSelected() {
        int resourceId = FacesContextUtility.getRequiredRequestParameter("id", Integer.class);
        int[] measurementDefinitionIds = getSelectedResourceScheduleList();
        try {
            measurementScheduleManager.disableSchedules(getSubject(), resourceId, measurementDefinitionIds);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Disabled " + measurementDefinitionIds.length
                + " schedules.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to disable selected schedules.", e);
        }
        return "success";
    }

    public String enableSelected() {
        int resourceId = FacesContextUtility.getRequiredRequestParameter("id", Integer.class);
        int[] measurementDefinitionIds = getSelectedResourceScheduleList();
        try {
            measurementScheduleManager.enableSchedules(getSubject(), resourceId, measurementDefinitionIds);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Enabled " + measurementDefinitionIds.length
                + " schedules.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to enable selected schedules.", e);
        }
        return "success";
    }

    public String enableAndSetSelected() {
        long collectionInterval = duration.getMillis();
        int resourceId = FacesContextUtility.getRequiredRequestParameter("id", Integer.class);
        int[] measurementDefinitionIds = getSelectedResourceScheduleList();
        try {
            measurementScheduleManager.updateSchedules(getSubject(), resourceId, measurementDefinitionIds,
                collectionInterval);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Enabled and set "
                + measurementDefinitionIds.length + " schedules.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Failed to enabled and set selected schedules.", e);
        }
        return "success";
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ListResourceMeasurementScheduleDataModel(PageControlView.ResourceMeasurementScheduleList,
                MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListResourceMeasurementScheduleDataModel extends PagedListDataModel<MeasurementSchedule> {
        public ListResourceMeasurementScheduleDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<MeasurementSchedule> fetchPage(PageControl pc) {
            int resourceId = FacesContextUtility.getRequiredRequestParameter("id", Integer.class);
            return measurementScheduleManager.findSchedulesForResource(getSubject(), resourceId, pc);
        }
    }

    private int[] getSelectedResourceScheduleList() {
        String[] resourceSchedules = FacesContextUtility.getRequest().getParameterValues("selectedResourceSchedules");
        return StringUtility.getIntArray(resourceSchedules);
    }

}
