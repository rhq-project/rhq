/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation;

import org.rhq.core.domain.criteria.ResourceOperationHistoryCriteria;
import org.rhq.core.domain.operation.ResourceOperationHistory;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.operation.detail.OperationDetailsView;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * @author Greg Hinkle
 */
public class OperationHistoryView extends VLayout {

    Table table;
    Criteria criteria;

    @Override
    protected void onInit() {
        super.onInit();


    }

    public OperationHistoryView() {
    }

    public OperationHistoryView(Criteria criteria) {
        this.criteria = criteria;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        if (criteria == null) {
            table = new Table("Operation History");
        } else {
            table = new Table("Operation History", criteria);
        }

        table.setDataSource(new OperationHistoryDataSource());


        table.addTableAction("Details", Table.SelectionEnablement.SINGLE,null, new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                ResourceOperationHistory history = (ResourceOperationHistory) selection[0].getAttributeAsObject("entity");

                ResourceOperationHistoryCriteria criteria = new ResourceOperationHistoryCriteria();

                criteria.addFilterId(history.getId());

                criteria.fetchOperationDefinition(true);
                criteria.fetchParameters(true);
                criteria.fetchResults(true);

                GWTServiceLookup.getOperationService().findResourceOperationHistoriesByCriteria(
                        criteria, new AsyncCallback<PageList<ResourceOperationHistory>>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError("Failure loading operation history", caught);
                            }

                            public void onSuccess(PageList<ResourceOperationHistory> result) {
                                ResourceOperationHistory item = result.get(0);
                                OperationDetailsView.displayDetailsDialog(item);
                            }
                        }
                );
            }
        });


        addMember(table);
    }


    public static OperationHistoryView getResourceHistoryView(int resourceId) {
        Criteria criteria = new Criteria("resourceId", String.valueOf(resourceId));

        return new OperationHistoryView(criteria);
    }
}
