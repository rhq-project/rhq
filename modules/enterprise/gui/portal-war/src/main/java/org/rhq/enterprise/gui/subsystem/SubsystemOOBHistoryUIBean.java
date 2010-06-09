/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.subsystem;

import javax.faces.model.DataModel;

import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.core.server.MeasurementConverter;
import org.rhq.core.util.IntExtractor;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.ResourceNameDisambiguatingPagedListDataModel;
import org.rhq.enterprise.server.measurement.MeasurementOOBManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

import java.util.List;

/**
 * Backing bean fr the OOB subsysems view, oobHistory.xhtml
 *
 * @author Heiko W. Rupp
 */
public class SubsystemOOBHistoryUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = "SubsystemOOBHistoryUIBean";
    private static final String FORM_PREFIX = "oobHistorySubsystemForm:";

    private int selectedSchedule;
    private String metricFilter;
    private String resourceFilter;
    private String parentFilter;


    private MeasurementOOBManagerLocal manager = LookupUtil.getOOBManager();
    
    private static final IntExtractor<MeasurementOOBComposite> RESOURCE_ID_EXTRACTOR = new IntExtractor<MeasurementOOBComposite>() {
        public int extract(MeasurementOOBComposite value) {
            return value.getResourceId();
        }
    };
        
    public SubsystemOOBHistoryUIBean() {

    }

    public int getSelectedSchedule() {
        return selectedSchedule;
    }

    public void setSelectedSchedule(int selectedSchedule) {
        this.selectedSchedule = selectedSchedule;
    }

    public String getResourceFilter() {
        return resourceFilter;
    }

    public void setResourceFilter(String resourceFilter) {
        this.resourceFilter = resourceFilter;
    }

    public String getParentFilter() {
        return parentFilter;
    }

    public void setParentFilter(String parentFilter) {
        this.parentFilter = parentFilter;
    }

    public String getMetricFilter() {
        return metricFilter;
    }

    public void setMetricFilter(String metricFilter) {
        this.metricFilter = metricFilter;
    }

    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ResultsDataModel(PageControlView.SubsystemOOBHistory, MANAGED_BEAN_NAME);
        }
        return dataModel;
    }


    private class ResultsDataModel extends ResourceNameDisambiguatingPagedListDataModel<MeasurementOOBComposite> {

        public ResultsDataModel(PageControlView view, String beanName) {
            super(view, beanName, true);
        }

        public PageList<MeasurementOOBComposite> fetchDataForPage(PageControl pc) {
            getDataFromRequest();
            String metricFilter = getMetricFilter();
            String resourceFilter = getResourceFilter();
            String parentFilter = getParentFilter();

            PageList<MeasurementOOBComposite> result;

            result = manager.getSchedulesWithOOBs(getSubject(), metricFilter, resourceFilter, parentFilter, pc);

            applyFormatting(result);

            return result;
        }

        protected IntExtractor<MeasurementOOBComposite> getResourceIdExtractor() {
            return RESOURCE_ID_EXTRACTOR;
        }
        
        private void getDataFromRequest() {
            SubsystemOOBHistoryUIBean outer = SubsystemOOBHistoryUIBean.this;
            outer.metricFilter = FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX + "metricFilter");
            outer.resourceFilter = FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX + "resourceFilter");
            outer.parentFilter = FacesContextUtility.getOptionalRequestParameter(FORM_PREFIX + "parentFilter");
        }

        private void applyFormatting(List<MeasurementOOBComposite> composites) {
            for (MeasurementOOBComposite oob : composites) {
                oob.setFormattedOutlier(MeasurementConverter.format(oob.getOutlier(), oob.getUnits(), true));
                formatBaseband(oob);
            }
        }

        private void formatBaseband(MeasurementOOBComposite oob) {
            String min = MeasurementConverter.format(oob.getBlMin(), oob.getUnits(), true);
            String max = MeasurementConverter.format(oob.getBlMax(), oob.getUnits(), true);
            oob.setFormattedBaseband(min + ", " + max);
        }
    }
}
