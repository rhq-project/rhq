/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.admin;

import java.util.HashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.SectionStackSection;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.TitleBar;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.SystemGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableSectionStack;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A simple page to view the downloads the server provides (like
 * the agent update binary and the CLI distro).
 * 
 * @author John Mazzitelli
 */
public class DownloadsView extends LocatableVLayout {

    public static final ViewName VIEW_ID = new ViewName("Downloads", MSG.view_adminConfig_downloads());
    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_CONFIGURATION_VIEW_ID + "/" + VIEW_ID;

    private final SystemGWTServiceAsync systemManager = GWTServiceLookup.getSystemService();

    private LocatableSectionStack sectionStack;
    private SectionStackSection agentSection;
    private SectionStackSection cliSection;
    private SectionStackSection bundleSection;
    private SectionStackSection connectorsSection;

    public DownloadsView(String locatorId) {
        super(locatorId);
        setHeight100();
        setWidth100();

        TitleBar titleBar = new TitleBar(this, MSG.view_adminConfig_downloads(), "global/Download_24.png");
        addMember(titleBar);

        sectionStack = new LocatableSectionStack(extendLocatorId("stack"));
        sectionStack.setShowResizeBar(true);
        sectionStack.setVisibilityMode(VisibilityMode.MULTIPLE);
        sectionStack.setWidth100();
        sectionStack.setHeight100();

        agentSection = new SectionStackSection(MSG.view_admin_downloads_agentDownload());
        cliSection = new SectionStackSection(MSG.view_admin_downloads_cliDownload());
        bundleSection = new SectionStackSection(MSG.view_admin_downloads_bundleDownload());
        connectorsSection = new SectionStackSection(MSG.view_admin_downloads_connectorsDownload());

        agentSection.setExpanded(false);
        cliSection.setExpanded(false);
        bundleSection.setExpanded(false);
        connectorsSection.setExpanded(false);

        sectionStack.addSection(agentSection);
        sectionStack.addSection(cliSection);
        sectionStack.addSection(bundleSection);
        sectionStack.addSection(connectorsSection);

        addMember(sectionStack);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        prepareAgentSection();
        prepareCLISection();
        prepareBundleSection();
        prepareConnectorsSection();
    }

    private void prepareAgentSection() {
        systemManager.getAgentVersionProperties(new AsyncCallback<HashMap<String, String>>() {
            @Override
            public void onSuccess(HashMap<String, String> result) {
                String version = result.get("rhq-agent.latest.version");
                String build = result.get("rhq-agent.latest.build-number");
                String md5 = result.get("rhq-agent.latest.md5");

                LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("agentForm"));
                form.setMargin(10);
                form.setWidth100();

                StaticTextItem versionItem = new StaticTextItem("agentVersion", MSG
                    .view_admin_downloads_agent_version());
                versionItem.setValue(version);
                versionItem.setWrapTitle(false);

                StaticTextItem buildItem = new StaticTextItem("agentBuild", MSG
                    .view_admin_downloads_agent_buildNumber());
                buildItem.setValue(build);
                buildItem.setWrapTitle(false);

                StaticTextItem md5Item = new StaticTextItem("agentMd5", MSG.view_admin_downloads_agent_md5());
                md5Item.setValue(md5);
                md5Item.setWrapTitle(false);

                LinkItem linkItem = new LinkItem("agentLink");
                linkItem.setTitle(MSG.view_admin_downloads_agent_link_label());
                linkItem.setLinkTitle(MSG.view_admin_downloads_agent_link_value(version, build));
                linkItem.setValue("/agentupdate/download");

                form.setItems(versionItem, buildItem, md5Item, linkItem);

                agentSection.setItems(form);
                agentSection.setExpanded(true);
                sectionStack.markForRedraw();
            }

            @Override
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("Cannot get agent version info", caught);
            }
        });
    }

    private void prepareCLISection() {
        // TODO Auto-generated method stub
    }

    private void prepareBundleSection() {
        // TODO Auto-generated method stub
    }

    private void prepareConnectorsSection() {
        // TODO Auto-generated method stub
    }

    @Deprecated
    private void cleanSectionStackSection(SectionStackSection section) {
        Canvas[] items = section.getItems();
        if (items != null) {
            for (Canvas canvas : items) {
                canvas.destroy();
            }
        }
    }
}
