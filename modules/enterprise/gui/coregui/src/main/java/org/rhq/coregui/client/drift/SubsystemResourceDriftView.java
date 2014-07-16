/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.coregui.client.drift;

import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.coregui.client.components.ReportExporter;
import org.rhq.coregui.client.components.table.AbstractTableAction;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;

public class SubsystemResourceDriftView extends DriftHistoryView {
    public SubsystemResourceDriftView(boolean hasWriteAccess) {
        super(MSG.common_title_recent_drifts(), EntityContext.forSubsystemView(), hasWriteAccess);
    }

    @Override
    protected void configureTable() {
        super.configureTable();
        addExportAction();
    }

    private void addExportAction() {
        addTableAction("Export", MSG.common_button_reports_export(), ButtonColor.BLUE, new AbstractTableAction() {
            @Override
            public boolean isEnabled(ListGridRecord[] selection) {
                return enableIfRecordsExist(getListGrid());
            }

            @Override
            public void executeAction(ListGridRecord[] selection, Object actionValue) {

                ReportExporter exporter = ReportExporter.createExporterForRecentDrift("recentDrift",
                    definitionFilter.getValueAsString(), changeSetFilter.getValueAsString(),
                    categoryFilter.getValues(), pathFilter.getValueAsString(), startDateFilter.getValueAsDays(),
                    startDateFilter.getValueAsDays());
                exporter.export();
                refreshTableInfo();
            }

        });

    }
}
