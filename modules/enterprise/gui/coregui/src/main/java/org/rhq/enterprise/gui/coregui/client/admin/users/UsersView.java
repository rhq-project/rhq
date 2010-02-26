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
package org.rhq.enterprise.gui.coregui.client.admin.users;

import org.rhq.enterprise.gui.coregui.client.Presenter;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.enterprise.gui.coregui.client.places.Place;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import java.util.List;

/**
 * @author Greg Hinkle
 */
public class UsersView extends SectionStack implements Presenter {




    @Override
    protected void onInit() {
        super.onInit();

        setVisibilityMode(VisibilityMode.MULTIPLE);
        setWidth100();
        setHeight100();

         final UsersDataSource datasource = UsersDataSource.getInstance();

        VLayout gridHolder = new VLayout();
        gridHolder.setWidth100();
        gridHolder.setHeight100();

        final ListGrid listGrid = new ListGrid();
        listGrid.setWidth100();
        listGrid.setHeight100();
        listGrid.setDataSource(datasource);
        listGrid.setAutoFetchData(true);
        listGrid.setAutoFitData(Autofit.HORIZONTAL);
//        listGrid.setAutoFitData(Autofit.HORIZONTAL);
        listGrid.setAlternateRecordStyles(true);
//        listGrid.setAutoFitData(Autofit.HORIZONTAL);
        listGrid.setSelectionType(SelectionStyle.SIMPLE);
        listGrid.setSelectionAppearance(SelectionAppearance.CHECKBOX);

        listGrid.setShowFilterEditor(true);

        ListGridField idField = new ListGridField("id", "Id", 55);
        idField.setType(ListGridFieldType.INTEGER);
        
        ListGridField nameField = new ListGridField("username", "Name", 100);


        ListGridField descriptionField = new ListGridField("name", "Name", 150);
        ListGridField emailField = new ListGridField("email", "Email Address", 100);
        
        listGrid.setFields(idField, nameField, descriptionField, emailField);


        gridHolder.addMember(listGrid);

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setMembersMargin(15);

        final IButton removeButton = new IButton("Remove");
        removeButton.setDisabled(true);
        removeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                SC.confirm("Are you sure you want to delete " + listGrid.getSelection().length + " resources?",
                        new BooleanCallback() {
                            public void execute(Boolean aBoolean) {

                            }
                        }
                );
            }
        });



        final Label tableInfo = new Label("Total: " + listGrid.getTotalRows());
        tableInfo.setWrap(false);

        toolStrip.addMember(removeButton);
        toolStrip.addMember(new LayoutSpacer());
        toolStrip.addMember(tableInfo);

        gridHolder.addMember(toolStrip);


        listGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                int selectedCount = ((ListGrid)selectionEvent.getSource()).getSelection().length;
                tableInfo.setContents("Total: " + listGrid.getTotalRows() + " (" + selectedCount + " selected)");
                removeButton.setDisabled(selectedCount == 0);
            }
        });

        final SectionStackSection detailsSection = new SectionStackSection("Details");
        detailsSection.setItems(new Label("Select a user to edit..."));
        detailsSection.setExpanded(false);

        listGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                for (Canvas c : detailsSection.getItems())
                    c.destroy();
                
                detailsSection.setItems(new UserEditView());
                detailsSection.setExpanded(true);
                expandSection(1);
            }
        });


        SectionStackSection gridSection = new SectionStackSection("Search Users");
        gridSection.setExpanded(true);
        gridSection.setItems(gridHolder);
        addSection(gridSection);
        addSection(detailsSection);

    }

    public boolean fireDisplay(Place place, List<Place> children) {
        if (!place.equals(getPlace())) {
            return false;
        }
        if (children.size() > 0) {
            int userId = Integer.parseInt(children.get(0).getId());
        }
        return true;
    }

    public Place getPlace() {
        return new Place("users", "Users");
    }
}
