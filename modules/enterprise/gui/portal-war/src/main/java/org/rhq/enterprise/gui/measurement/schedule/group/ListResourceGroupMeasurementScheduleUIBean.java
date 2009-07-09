package org.rhq.enterprise.gui.measurement.schedule.group;

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

public class ListResourceGroupMeasurementScheduleUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = "ListResourceGroupMeasurementScheduleUIBean";

    private MeasurementScheduleManagerLocal measurementScheduleManager = LookupUtil.getMeasurementScheduleManager();
    private DurationComponent duration;

    public DurationComponent getDuration() {
        return duration;
    }

    public void setDuration(DurationComponent duration) {
        this.duration = duration;
    }

    public String disableSelected() {
        int groupId = FacesContextUtility.getRequiredRequestParameter("groupId", Integer.class);
        int[] measurementDefinitionIds = getSelectedResourceGroupScheduleList();
        try {
            measurementScheduleManager.disableMeasurementSchedulesForCompatGroup(getSubject(),
                measurementDefinitionIds, groupId);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Disabled " + measurementDefinitionIds.length
                + " schedules.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to disable selected schedules.", e);
        }
        return "success";
    }

    public String enableSelected() {
        int groupId = FacesContextUtility.getRequiredRequestParameter("groupId", Integer.class);
        int[] measurementDefinitionIds = getSelectedResourceGroupScheduleList();
        try {
            measurementScheduleManager.enableMeasurementSchedulesForCompatGroup(getSubject(),
                measurementDefinitionIds, groupId);
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_INFO, "Enabled " + measurementDefinitionIds.length
                + " schedules.");
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to enable selected schedules.", e);
        }
        return "success";
    }

    public String enableAndSetSelected() {
        long collectionInterval = duration.getMillis();
        int groupId = FacesContextUtility.getRequiredRequestParameter("groupId", Integer.class);
        int[] measurementDefinitionIds = getSelectedResourceGroupScheduleList();
        try {
            measurementScheduleManager.updateMeasurementSchedulesForCompatGroup(getSubject(), measurementDefinitionIds,
                groupId, collectionInterval);
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
            dataModel = new ListResourceGroupMeasurementScheduleDataModel(
                PageControlView.ResourceGroupMeasurementScheduleList, MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ListResourceGroupMeasurementScheduleDataModel extends
        PagedListDataModel<MeasurementScheduleComposite> {
        public ListResourceGroupMeasurementScheduleDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        public PageList<MeasurementScheduleComposite> fetchPage(PageControl pc) {
            int groupId = FacesContextUtility.getRequiredRequestParameter("groupId", Integer.class);
            return measurementScheduleManager.getMeasurementSchedulesForCompatGroup(getSubject(), groupId, pc);
        }
    }

    private int[] getSelectedResourceGroupScheduleList() {
        String[] resourceSchedules = FacesContextUtility.getRequest().getParameterValues(
            "selectedResourceGroupSchedules");
        return StringUtility.getIntArray(resourceSchedules);
    }

}
