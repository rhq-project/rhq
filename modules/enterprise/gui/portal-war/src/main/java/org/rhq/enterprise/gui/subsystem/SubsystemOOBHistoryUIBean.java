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

import java.util.List;

import javax.faces.model.DataModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.measurement.MeasurementOOB;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.server.measurement.MeasurementOOBManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * TODO document me
 *
 * @author Heiko W. Rupp
 */
public class SubsystemOOBHistoryUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = "SubsystemOOBHistoryUIBean";
    private static final String FORM_PREFIX = "oobHistorySubsystemForm:";

    private final Log log = LogFactory.getLog(SubsystemOOBHistoryUIBean.class);

    private int selectedSchedule;

    private MeasurementOOBManagerLocal manager = LookupUtil.getOOBManager();

    public SubsystemOOBHistoryUIBean() {

    }

    public int getSelectedSchedule() {
        return selectedSchedule;
    }

    public void setSelectedSchedule(int selectedSchedule) {
        this.selectedSchedule = selectedSchedule;
    }

    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ResultsDataModel(PageControlView.SubsystemOOBHistory, MANAGED_BEAN_NAME);
        }
        return dataModel;
    }

    public List<MeasurementOOB> getDataForSelectedSchedule() {
        List<MeasurementOOB> oobs = manager.getOObsForSchedule(getSubject(),selectedSchedule, 0,System.currentTimeMillis()); // TODO adujust time range
        return oobs;
    }


    private class ResultsDataModel extends PagedListDataModel<MeasurementOOBComposite> {

        public ResultsDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        public PageList<MeasurementOOBComposite> fetchPage(PageControl pc) {



            // TODO implement getting stuff from the page
            PageList<MeasurementOOBComposite> result;

            List<MeasurementOOBComposite> foo = manager.getSchedulesWithOOBs(getSubject(),0,System.currentTimeMillis()); // TODO adjust time range
            result = new PageList<MeasurementOOBComposite>(foo,foo.size(),pc);

            return result;
        }
    }
}
