/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.coregui.client.admin.topology;

import static org.rhq.coregui.client.admin.topology.AffinityGroupWithCountsDatasource.Fields.FIELD_NAME;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;

import org.rhq.core.domain.cloud.AffinityGroup;
import org.rhq.coregui.client.BookmarkableView;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedToolStrip;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.message.Message;

/**
 * Shows details of a affinity group.
 * 
 * @author Jirka Kremser
 */
public class AffinityGroupDetailView extends EnhancedVLayout implements BookmarkableView {

    private final int affinityGroupId;

    private static final int SECTION_COUNT = 3;
    private final SectionStack sectionStack;
    private SectionStackSection detailsSection = null;
    private SectionStackSection agentSection = null;
    private SectionStackSection serverSection = null;

    private volatile int initSectionCount = 0;

    public AffinityGroupDetailView(int affinityGroupId) {
        super();
        this.affinityGroupId = affinityGroupId;
        setHeight100();
        setWidth100();
        setOverflow(Overflow.AUTO);

        sectionStack = new SectionStack();
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth100();
        sectionStack.setHeight100();
        sectionStack.setMargin(5);
        sectionStack.setOverflow(Overflow.VISIBLE);
    }

    @Override
    protected void onInit() {
        super.onInit();
        GWTServiceLookup.getTopologyService().getAffinityGroupById(affinityGroupId, new AsyncCallback<AffinityGroup>() {
            public void onSuccess(final AffinityGroup affinityGroup) {
                prepareDetailsSection(sectionStack, affinityGroup);
            }

            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(
                    MSG.view_adminTopology_message_fetchAgroupFail(String.valueOf(affinityGroupId)), caught);
                initSectionCount = SECTION_COUNT;
                return;
            }
        });
        prepareAgentSection(sectionStack);
        prepareServerSection(sectionStack);
    }

    public boolean isInitialized() {
        return initSectionCount >= SECTION_COUNT;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        // wait until we have all of the sections before we show them. We don't use InitializableView because,
        // it seems they are not supported (in the applicable renderView()) at this level.
        new Timer() {
            final long startTime = System.currentTimeMillis();

            public void run() {
                if (isInitialized()) {
                    if (null != detailsSection) {
                        sectionStack.addSection(detailsSection);
                    }
                    if (null != agentSection) {
                        sectionStack.addSection(agentSection);
                    }
                    if (null != serverSection) {
                        sectionStack.addSection(serverSection);
                    }

                    addMember(sectionStack);
                    markForRedraw();

                } else {
                    // don't wait forever, give up after 20s and show what we have
                    long elapsedMillis = System.currentTimeMillis() - startTime;
                    if (elapsedMillis > 20000) {
                        initSectionCount = SECTION_COUNT;
                    }
                    schedule(100); // Reschedule the timer.
                }
            }
        }.run(); // fire the timer immediately
    }

    private void prepareAgentSection(SectionStack stack) {
        SectionStackSection section = new SectionStackSection(MSG.view_adminTopology_affinityGroups_agetnMembers());
        section.setExpanded(true);
        AgentTableView agentsTable = new AgentTableView(affinityGroupId, true);
        section.setItems(agentsTable);

        agentSection = section;
        initSectionCount++;
        return;
    }

    private void prepareServerSection(SectionStack stack) {
        SectionStackSection section = new SectionStackSection(MSG.view_adminTopology_affinityGroups_serverMembers());
        section.setExpanded(true);
        ServerTableView serverTable = new ServerTableView(affinityGroupId, true);
        section.setItems(serverTable);

        serverSection = section;
        initSectionCount++;
        return;
    }

    private void prepareDetailsSection(SectionStack stack, final AffinityGroup affinityGroup) {
        final DynamicForm form = new DynamicForm();
        form.setMargin(10);
        form.setWidth100();
        form.setWrapItemTitles(false);
        form.setNumCols(2);

        final TextItem nameItem = new TextItem(FIELD_NAME.propertyName(), FIELD_NAME.title());
        nameItem.setValue(affinityGroup.getName());

        EnhancedToolStrip footer = new EnhancedToolStrip();
        footer.setPadding(5);
        footer.setWidth100();
        footer.setMembersMargin(15);

        IButton saveButton = new EnhancedIButton(MSG.common_button_save(), ButtonColor.BLUE);
        saveButton.setOverflow(Overflow.VISIBLE);
        saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                affinityGroup.setName(nameItem.getValueAsString());
                GWTServiceLookup.getTopologyService().updateAffinityGroup(affinityGroup, new AsyncCallback<Void>() {
                    public void onSuccess(Void result) {
                        Message msg = new Message(MSG.view_adminTopology_message_agroupRenamed(
                            String.valueOf(affinityGroupId), affinityGroup.getName(), nameItem.getValueAsString()),
                            Message.Severity.Info);
                        CoreGUI.getMessageCenter().notify(msg);
                    }

                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_adminTopology_message_agroupRenamingFail(String.valueOf(affinityGroupId),
                                affinityGroup.getName()) + " " + caught.getMessage(), caught);
                    }
                });
            }
        });
        footer.addMember(saveButton);
        form.setItems(nameItem);
        SectionStackSection section = new SectionStackSection(MSG.common_title_details());
        section.setExpanded(false);
        section.setItems(form, footer);

        detailsSection = section;
        initSectionCount++;
    }

    @Override
    public void renderView(ViewPath viewPath) {
        Log.debug("AffinityGroupDetailView: " + viewPath);
    }
}
