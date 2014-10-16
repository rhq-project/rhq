/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.coregui.client.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.smartgwt.client.widgets.HTMLFlow;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.common.ProductInfo;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.PermissionsLoadedListener;
import org.rhq.coregui.client.PermissionsLoader;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.admin.AdministrationView;
import org.rhq.coregui.client.admin.AgentPluginTableView;
import org.rhq.coregui.client.admin.DownloadsView;
import org.rhq.coregui.client.admin.ServerPluginTableView;
import org.rhq.coregui.client.admin.SystemSettingsView;
import org.rhq.coregui.client.admin.roles.RolesView;
import org.rhq.coregui.client.admin.storage.StorageNodeAdminView;
import org.rhq.coregui.client.admin.templates.AlertDefinitionTemplateTypeView;
import org.rhq.coregui.client.admin.templates.DriftDefinitionTemplateTypeView;
import org.rhq.coregui.client.admin.templates.IgnoreResourceTypesView;
import org.rhq.coregui.client.admin.templates.MetricTemplateTypeView;
import org.rhq.coregui.client.admin.templates.MissingPolicyResourceTypesView;
import org.rhq.coregui.client.admin.topology.AffinityGroupTableView;
import org.rhq.coregui.client.admin.topology.AgentTableView;
import org.rhq.coregui.client.admin.topology.PartitionEventTableView;
import org.rhq.coregui.client.admin.topology.ServerTableView;
import org.rhq.coregui.client.admin.users.UsersView;
import org.rhq.coregui.client.alert.AlertHistoryView;
import org.rhq.coregui.client.bundle.BundleTopView;
import org.rhq.coregui.client.components.AboutModalWindow;
import org.rhq.coregui.client.components.view.ViewName;
import org.rhq.coregui.client.dashboard.DashboardsView;
import org.rhq.coregui.client.dashboard.portlets.platform.PlatformSummaryPortlet;
import org.rhq.coregui.client.drift.DriftHistoryView;
import org.rhq.coregui.client.footer.FavoritesMenu;
import org.rhq.coregui.client.help.HelpView;
import org.rhq.coregui.client.help.RhAccessView;
import org.rhq.coregui.client.inventory.InventoryView;
import org.rhq.coregui.client.inventory.resource.detail.configuration.ResourceConfigurationHistoryListView;
import org.rhq.coregui.client.inventory.resource.discovery.ResourceAutodiscoveryView;
import org.rhq.coregui.client.operation.OperationHistoryView;
import org.rhq.coregui.client.report.AlertDefinitionReportView;
import org.rhq.coregui.client.report.ReportTopView;
import org.rhq.coregui.client.report.inventory.DriftComplianceReport;
import org.rhq.coregui.client.report.inventory.ResourceInstallReport;
import org.rhq.coregui.client.report.measurement.MeasurementOOBView;
import org.rhq.coregui.client.report.tag.TaggedView;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.MessageBar;
import org.rhq.coregui.client.util.message.MessageCenterView;

/**
 * @author Greg Hinkle
 * @author Joseph Marques
 * @author Ian Springer
 * @author Libor Zoubek
 */
public class MenuBarView extends EnhancedVLayout {

    public static final String MSG_CENTER_BTN_CONTENT = "<span class='fa fa-flag'> ";
    public static final String BTN_FAV_ID = "fav-btn";
    public static final String BTN_MSG_CENTER_ID = "msg-center-btn";

    public static final MenuItem[] MENU_ITEMS = {
        new MenuItem(DashboardsView.VIEW_ID),
        new MenuItem(InventoryView.VIEW_ID)
            .subItems(
                new MenuItem(InventoryView.RESOURCES_SECTION_VIEW_ID, true),
                new MenuItem(ResourceAutodiscoveryView.VIEW_ID, Permission.MANAGE_INVENTORY, InventoryView.VIEW_ID, InventoryView.RESOURCES_SECTION_VIEW_ID),
                new MenuItem(InventoryView.PAGE_ALL_RESOURCES, InventoryView.VIEW_ID, InventoryView.RESOURCES_SECTION_VIEW_ID),
                new MenuItem(InventoryView.PAGE_PLATFORMS, InventoryView.VIEW_ID, InventoryView.RESOURCES_SECTION_VIEW_ID),
                new MenuItem(InventoryView.PAGE_SERVERS_TOP, InventoryView.VIEW_ID, InventoryView.RESOURCES_SECTION_VIEW_ID),
                new MenuItem(InventoryView.PAGE_SERVERS, InventoryView.VIEW_ID, InventoryView.RESOURCES_SECTION_VIEW_ID),
                new MenuItem(InventoryView.PAGE_SERVICES, InventoryView.VIEW_ID, InventoryView.RESOURCES_SECTION_VIEW_ID),
                new MenuItem(InventoryView.PAGE_IGNORED_RESOURCES, InventoryView.VIEW_ID, InventoryView.RESOURCES_SECTION_VIEW_ID),
                new MenuItem(InventoryView.PAGE_UNAVAIL_SERVERS, InventoryView.VIEW_ID, InventoryView.RESOURCES_SECTION_VIEW_ID),
                new MenuItem(InventoryView.GROUPS_SECTION_VIEW_ID, true),
                new MenuItem(InventoryView.PAGE_ALL_GROUPS, InventoryView.VIEW_ID, InventoryView.GROUPS_SECTION_VIEW_ID),
                new MenuItem(InventoryView.PAGE_DYNAGROUP_DEFINITIONS, Permission.MANAGE_INVENTORY, InventoryView.VIEW_ID, InventoryView.GROUPS_SECTION_VIEW_ID),
                new MenuItem(InventoryView.PAGE_COMPATIBLE_GROUPS, InventoryView.VIEW_ID, InventoryView.GROUPS_SECTION_VIEW_ID),
                new MenuItem(InventoryView.PAGE_MIXED_GROUPS, InventoryView.VIEW_ID, InventoryView.GROUPS_SECTION_VIEW_ID),
                new MenuItem(InventoryView.PAGE_PROBLEM_GROUPS, InventoryView.VIEW_ID, InventoryView.GROUPS_SECTION_VIEW_ID)
        ),
        new MenuItem(ReportTopView.VIEW_ID)
            .subItems(
                new MenuItem(ReportTopView.SECTION_SUBSYSTEMS_VIEW_ID, true),
                new MenuItem(TaggedView.VIEW_ID,ReportTopView.VIEW_ID,ReportTopView.SECTION_SUBSYSTEMS_VIEW_ID),
                new MenuItem(MeasurementOOBView.VIEW_ID,ReportTopView.VIEW_ID,ReportTopView.SECTION_SUBSYSTEMS_VIEW_ID),
                new MenuItem(ResourceConfigurationHistoryListView.VIEW_ID,Permission.MANAGE_INVENTORY,ReportTopView.VIEW_ID,ReportTopView.SECTION_SUBSYSTEMS_VIEW_ID),
                new MenuItem(OperationHistoryView.SUBSYSTEM_VIEW_ID,Permission.MANAGE_INVENTORY,ReportTopView.VIEW_ID,ReportTopView.SECTION_SUBSYSTEMS_VIEW_ID),
                new MenuItem(AlertHistoryView.SUBSYSTEM_VIEW_ID,Permission.MANAGE_INVENTORY,ReportTopView.VIEW_ID,ReportTopView.SECTION_SUBSYSTEMS_VIEW_ID),
                new MenuItem(AlertDefinitionReportView.VIEW_ID,ReportTopView.VIEW_ID,ReportTopView.SECTION_SUBSYSTEMS_VIEW_ID),
                new MenuItem(DriftHistoryView.SUBSYSTEM_VIEW_ID,Permission.MANAGE_INVENTORY,ReportTopView.VIEW_ID,ReportTopView.SECTION_SUBSYSTEMS_VIEW_ID),
                new MenuItem(ReportTopView.SECTION_INVENTORY_VIEW_ID, true),
                new MenuItem(PlatformSummaryPortlet.VIEW_ID,ReportTopView.VIEW_ID,ReportTopView.SECTION_INVENTORY_VIEW_ID),
                new MenuItem(ResourceInstallReport.VIEW_ID,Permission.MANAGE_INVENTORY,ReportTopView.VIEW_ID,ReportTopView.SECTION_INVENTORY_VIEW_ID),
                new MenuItem(DriftComplianceReport.VIEW_ID,Permission.MANAGE_INVENTORY,ReportTopView.VIEW_ID,ReportTopView.SECTION_INVENTORY_VIEW_ID)
            ),
        new MenuItem(BundleTopView.VIEW_ID),
        new MenuItem(AdministrationView.VIEW_ID)
            .subItems(
            new MenuItem(UsersView.VIEW_ID.withTitle(AdministrationView.SECTION_SECURITY_VIEW_ID.getTitle()),
                AdministrationView.VIEW_ID, AdministrationView.SECTION_SECURITY_VIEW_ID)
                .subItems(
                    new MenuItem(UsersView.VIEW_ID, AdministrationView.VIEW_ID,AdministrationView.SECTION_SECURITY_VIEW_ID),
                    new MenuItem(RolesView.VIEW_ID, AdministrationView.VIEW_ID,AdministrationView.SECTION_SECURITY_VIEW_ID)
                 ),
                 new MenuItem(ServerTableView.VIEW_ID.withTitle(AdministrationView.SECTION_TOPOLOGY_VIEW_ID.getTitle()),
                     AdministrationView.VIEW_ID, AdministrationView.SECTION_TOPOLOGY_VIEW_ID)
                    .subItems(
                        new MenuItem(ServerTableView.VIEW_ID, AdministrationView.VIEW_ID,AdministrationView.SECTION_TOPOLOGY_VIEW_ID),
                        new MenuItem(StorageNodeAdminView.VIEW_ID, AdministrationView.VIEW_ID,AdministrationView.SECTION_TOPOLOGY_VIEW_ID),
                        new MenuItem(AgentTableView.VIEW_ID, AdministrationView.VIEW_ID,AdministrationView.SECTION_TOPOLOGY_VIEW_ID),
                        new MenuItem(AffinityGroupTableView.VIEW_ID, AdministrationView.VIEW_ID,AdministrationView.SECTION_TOPOLOGY_VIEW_ID),
                        new MenuItem(PartitionEventTableView.VIEW_ID, AdministrationView.VIEW_ID,AdministrationView.SECTION_TOPOLOGY_VIEW_ID)
                     ),
                 new MenuItem(
                     DownloadsView.VIEW_ID.withTitle(AdministrationView.SECTION_CONFIGURATION_VIEW_ID.getTitle()),
                     Permission.MANAGE_SETTINGS, AdministrationView.VIEW_ID,AdministrationView.SECTION_CONFIGURATION_VIEW_ID)
                    .subItems(
                        new MenuItem(SystemSettingsView.VIEW_ID,Permission.MANAGE_SETTINGS, AdministrationView.VIEW_ID,AdministrationView.SECTION_CONFIGURATION_VIEW_ID),
                        new MenuItem(AlertDefinitionTemplateTypeView.VIEW_ID, AdministrationView.VIEW_ID,AdministrationView.SECTION_CONFIGURATION_VIEW_ID),
                        new MenuItem(DriftDefinitionTemplateTypeView.VIEW_ID, AdministrationView.VIEW_ID,AdministrationView.SECTION_CONFIGURATION_VIEW_ID),
                        new MenuItem(MetricTemplateTypeView.VIEW_ID, AdministrationView.VIEW_ID,AdministrationView.SECTION_CONFIGURATION_VIEW_ID),
                        new MenuItem(IgnoreResourceTypesView.VIEW_ID, AdministrationView.VIEW_ID,AdministrationView.SECTION_CONFIGURATION_VIEW_ID),
                        new MenuItem(MissingPolicyResourceTypesView.VIEW_ID, AdministrationView.VIEW_ID,AdministrationView.SECTION_CONFIGURATION_VIEW_ID),
                        new MenuItem(DownloadsView.VIEW_ID, AdministrationView.VIEW_ID,AdministrationView.SECTION_CONFIGURATION_VIEW_ID),
                        new MenuItem(AgentPluginTableView.VIEW_ID, Permission.MANAGE_SETTINGS, AdministrationView.VIEW_ID,AdministrationView.SECTION_CONFIGURATION_VIEW_ID),
                        new MenuItem(ServerPluginTableView.VIEW_ID, Permission.MANAGE_SETTINGS, AdministrationView.VIEW_ID,AdministrationView.SECTION_CONFIGURATION_VIEW_ID)
                    ),
                new MenuItem(
                    AdministrationView.PAGE_REPOS_VIEW_ID.withTitle(AdministrationView.SECTION_CONTENT_VIEW_ID.getTitle()),
                    Permission.MANAGE_REPOSITORIES, AdministrationView.VIEW_ID, AdministrationView.SECTION_CONTENT_VIEW_ID)
                    .subItems(
                        new MenuItem(AdministrationView.PAGE_CONTENT_SOURCES_VIEW_ID, Permission.MANAGE_REPOSITORIES, AdministrationView.VIEW_ID,AdministrationView.SECTION_CONTENT_VIEW_ID),
                        new MenuItem(AdministrationView.PAGE_REPOS_VIEW_ID, AdministrationView.VIEW_ID,AdministrationView.SECTION_CONTENT_VIEW_ID)
                    )
                ),
        new MenuItem(HelpView.VIEW_ID)
    };

    public static final String LAST_MENU_ITEM_ID = "menu" + (MENU_ITEMS.length - 1);

    public static final ViewName LOGOUT_VIEW_ID = new ViewName("LogOut", MSG.view_menuBar_logout());
    private final ProductInfo productInfo = CoreGUI.get().getProductInfo();
    private String currentlySelectedSection = DashboardsView.VIEW_ID.getName();
    private MessageBar messageBar;
    private MessageCenterView messageCenter;
    private FavoritesMenu favoritesMenu;
    private Set<Permission> globalPermissions;

    public MenuBarView() {
        super();
        setStyleName("overflowVisible"); // force overflow:visible via style - workaround menu issue in FF
    }

    @Override
    protected void onInit() {
        super.onInit();
        messageCenter = new MessageCenterView();
        favoritesMenu = new FavoritesMenu();
        messageBar = new MessageBar();
        messageBar.setVisible(false);

        injectMenuFunctions(this);
        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            @Override
            public void onPermissionsLoaded(Set<Permission> permissions) {
                globalPermissions = (permissions != null) ? permissions : EnumSet.noneOf(Permission.class);
                for (MenuItem item : MENU_ITEMS) {
                    updateMenuVisibility(item);
                }
                addMember(new LinkBar());
                addMember(messageBar);
            }
        });
    }

    private void updateMenuVisibility(MenuItem item) {
        // set visibility based on permission
        if (item.getPermission() != null) {
            item.setHidden(!globalPermissions.contains(item.getPermission()));
        }
        if (TaggedView.VIEW_ID.getName().equals(item.getView().getName())) { // Reports->Tags shown only for RHQ
            item.setHidden(!CoreGUI.isTagsEnabledForUI());
        }
        for (MenuItem child : item.getSubItems()) {
            updateMenuVisibility(child);
        }
    }

    // This is our JSNI method that will be called on form submit
    private native void injectMenuFunctions(MenuBarView view) /*-{
                                                              $wnd.__gwt_clearMessageBar = $entry(function(){
                                                              view.@org.rhq.coregui.client.menu.MenuBarView::clearMessageBar()();
                                                              });

                                                              $wnd.__gwt_showMessageCenter = $entry(function(){
                                                              view.@org.rhq.coregui.client.menu.MenuBarView::showMessageCenterWindow()();
                                                              });

                                                              $wnd.__gwt_showFavoritesMenu = $entry(function(){
                                                              view.@org.rhq.coregui.client.menu.MenuBarView::showFavoritesMenu()();
                                                              });

                                                              $wnd.__gwt_showAboutBox = $entry(function(){
                                                              view.@org.rhq.coregui.client.menu.MenuBarView::showAboutBox()();
                                                              });
                                                              }-*/;

    // called via JSNI - user menu button
    public void clearMessageBar() {
        messageBar.clearMessage(true);
    }

    // called via JSNI - fav menu button
    public void showFavoritesMenu() {
        clearMessageBar();
        favoritesMenu.show();
        this.favoritesMenu.showMenu(DOM.getElementById(BTN_FAV_ID).getAbsoluteBottom(), DOM.getElementById(BTN_FAV_ID)
            .getAbsoluteLeft());
    }

    // called via JSNI - msgcenter menu button
    public void showMessageCenterWindow() {
        this.messageCenter.showMessageCenterWindow();
    }

    // called via JSNI - RHQ on logo click
    public void showAboutBox() {
        new AboutModalWindow(productInfo).show();
    }

    public MessageBar getMessageBar() {
        return messageBar;
    }

    public MessageCenterView getMessageCenter() {
        return messageCenter;
    }

    class LinkBar extends HTMLFlow implements ValueChangeHandler<String> {
        private final Map<String, String> sectionNameToLinkID = new HashMap<String, String>();

        LinkBar() {
            super();
            setStyleName("overflowVisible"); // force overflow:visible via style - workaround menu issue in FF
            this.setContents(createBarContent());
            History.addValueChangeHandler(this);
        }

        @Override
        protected void onDraw() {
            updateActiveMenuItem(History.getToken());
            super.onDraw();
            injectJQueryCode();
        }


        // add jQuery magic for main navbar
        private native void injectJQueryCode() /*-{
            $wnd.$('.navbar-btn-item').on('mouseenter', function() {$wnd.$(this).parent().parent().addClass('navbar-btn-item-hover')});
            $wnd.$('.navbar-btn-item').on('mouseleave', function() {$wnd.$(this).parent().parent().removeClass('navbar-btn-item-hover')});
        }-*/;

        private String getViewLink(ViewName view) {
            return "<a href='#" + view.getName() + "'>" + view.getTitle() + "</a>";
        }

        private String createCspButtonContent() {
            if (CoreGUI.isRHQ()) {
                return "";
            }
            MenuItem search = new MenuItem(RhAccessView.PAGE_SEARCH, RhAccessView.VIEW_ID);
            MenuItem newCase = new MenuItem(RhAccessView.PAGE_NEW_CASE, RhAccessView.VIEW_ID);
            MenuItem myCases = new MenuItem(RhAccessView.PAGE_MY_CASES, RhAccessView.VIEW_ID);

            return "<li class='dropdown'>"
                + "<a href='#' class='dropdown-toggle' data-toggle='dropdown'>"+RhAccessView.VIEW_ID.getTitle()+" <b class='caret'></b></a>"
                + "<ul class='dropdown-menu'>"
                + "<li>"+getViewLink(search.getView())+"</li>"
                + "<li class='dropdown-submenu'><a href='#' tabindex='-1' data-toggle='dropdown'>Support</a>"
                + "<ul class='dropdown-menu'>"
                + "<li>"+getViewLink(newCase.getView())+"</li>"
                + "<li>"+getViewLink(myCases.getView())+"</li>"
                + "</ul></li></ul></li>";
        }

        private String createBarContent() {
            Subject user = UserSessionManager.getSessionSubject();
            StringBuilder sb = new StringBuilder();
            sb.append("<nav class='navbar navbar-default navbar-pf' role='navigation'>"
       +"<div class='navbar-header'>"
         +"<button type='button' class='navbar-toggle collapsed' data-toggle='collapse' data-target='.navbar-collapse-1'>"
           +"<span class='sr-only'>Toggle navigation</span>"
           +"<span class='icon-bar'></span>"
           +"<span class='icon-bar'></span>"
           +"<span class='icon-bar'></span>"
         +"</button>"
         +"<a class='navbar-brand' href='#' onclick='__gwt_showAboutBox(); return false;'>"
                + "<img class='navbar-logo' src='img/"
                + ("RHQ".equals(CoreGUI.get().getProductInfo().getShortName()) ? "logo" : "RH-JON-Login-Logo")
                + ".png'/>"
         +"</a>"
       +"</div>"
       +"<div class='navbar-collapse navbar-collapse-1 collapse'>"
         +"<ul class='nav navbar-nav navbar-utility'>"
                + createCspButtonContent()
           +"<li>"
             +"<a id='"+BTN_FAV_ID+"' onclick='__gwt_showFavoritesMenu(); return false;'><i class='fa fa-star'></i><b class='caret'></b></a>"
           +"</li>"
           +"<li>"
           +"<a id='"+BTN_MSG_CENTER_ID+"' onclick='__gwt_showMessageCenter(); return false;'>"+MSG_CENTER_BTN_CONTENT+"0</a>"
         +"</li>"
                + "<li class='dropdown'>"
                + "<a onclick='__gwt_clearMessageBar(); return false;' class='dropdown-toggle' data-toggle='dropdown'>"
               +"<span class='pficon pficon-user'></span>"
                +user.getName()+" <b class='caret'></b>"
             +"</a>"
             +"<ul class='dropdown-menu'>"
                + "<li><a href='#Administration/Security/Users/"
                + user.getId()
                + "'>"
                + MSG.common_title_settings()
                + "</a></li>"
             + "<li class='divider'></li>"
               +"<li>"
                 +"<a href='#"+LOGOUT_VIEW_ID.getName()+"'>"+LOGOUT_VIEW_ID.getTitle()+"</a>"
               +"</li>"
             +"</ul>"
           +"</li>"
         +"</ul>"
         +"<ul class='nav navbar-nav navbar-primary'>"
             + getMenuItems()
         +"</ul>"
       +"</div>"
                + "</nav>");
            return sb.toString();
        }

        private String getMenuItems() {
            StringBuilder sb = new StringBuilder();
            int i = 0;
            for (MenuItem menuItem : MENU_ITEMS) {
                sb.append(getMenuItemString(menuItem, String.valueOf(i), 0));
                sectionNameToLinkID.put(menuItem.getView().getName(), "menu"+i);
                i++;
            }
            return sb.toString();
        }

        private String getMenuItemString(MenuItem menuItem, String index, int level) {
            if (menuItem.isHidden()) {
                return "";
            }
            String menuId = "menu"+index;
            String menuLevel = "dropdown";
            if (level > 0) {
                menuId = "";
                menuLevel = "dropdown-submenu";
            }
            StringBuilder sb = new StringBuilder();
            ViewName sectionName = menuItem.getView();
            boolean hasChildren = menuItem.hasVisibleChildren();
            if (!hasChildren) {
                sb.append("<li id='" + menuId +"'><a href='#" + sectionName.getName() + "'>" + sectionName.getTitle()
                    + "</a></li>");
            }
            else {
                StringBuilder item = new StringBuilder("<li id='" + menuId +"' class='"+menuLevel+"'>");
                if (level == 0) {
                    item.append("<div class='navbar-btn-group'>");
                    item.append("<a class='navbar-btn-item nbi-link' href='#"+sectionName.getName()+"'>" + sectionName.getTitle() + "</a>");
                    item.append("<a class='navbar-btn-item nbi-caret' data-toggle='dropdown'><b class='caret'></b></a>");
                } else {
                    item.append("<a href='#"+sectionName.getName()+"'>" + sectionName.getTitle() + "</a>");
                }
                item.append("<ul class='dropdown-menu'>");
                int subMenuIndex = 0;
                for (MenuItem subMenu : menuItem.getSubItems()) {
                    ViewName subItem = subMenu.getView();
                    if (subMenu.isSeparator()) {
                        if (subMenuIndex == 0) { // when first is separator we skip divider line
                            item.append("<li class='dropdown-header'>"+subItem.getTitle()+"</li>");
                        } else {
                            item.append("<li class='divider'></li><li class='dropdown-header'>"+subItem.getTitle()+"</li>");
                        }
                    }
                    else {
                        item.append(getMenuItemString(subMenu, "", level + 1));
                    }
                    subMenuIndex++;
                }
                item.append("</ul>");
                if (level == 0) {
                    item.append("</div>");
                }
                item.append("</li>");
                sb.append(item.toString());
            }
            return sb.toString();
        }

        @Override
        public void onValueChange(ValueChangeEvent<String> stringValueChangeEvent) {
            updateActiveMenuItem(stringValueChangeEvent.getValue());
        }

        private void updateActiveMenuItem(String viewPath) {
            String topViewId = viewPath.split("/")[0];
            if ("Resource".equals(topViewId)) {
                topViewId = InventoryView.VIEW_ID.getName();
            }
            currentlySelectedSection = topViewId;
            for (MenuItem item : MENU_ITEMS) {
                updateLinkStyle(item.getView().getName());
            }
        }

        private void updateLinkStyle(String sectionName) {
            String className;
            if (sectionName.equals(currentlySelectedSection)) {
                className = "active";
            } else {
                className = "";
            }
            String itemId = this.sectionNameToLinkID.get(sectionName);
            DOM.getElementById(itemId).setClassName(className);
        }
    }

    public static class MenuItem {
        private final boolean separator;
        private final ArrayList<MenuItem> subItems;
        private final ViewName view;
        private final Permission permission;
        private boolean hidden;

        public MenuItem(ViewName view) {
            this(view, false, null);
        }

        public MenuItem(ViewName view, Permission p) {
            this(view, false, p);
        }

        public MenuItem(ViewName view, Permission p, ViewName... parents) {
            this(fromParents(view, parents), false, p);
        }

        public MenuItem(ViewName view, ViewName... parents) {
            this(fromParents(view, parents), false, null);
        }

        private static ViewName fromParents(ViewName view, ViewName... parents) {
            StringBuilder sb = new StringBuilder();
            for (ViewName v : parents) {
                sb.append(v.getName()+"/");
            }
            return new ViewName(sb.toString()+view.getName(),view.getTitle());
        }

        public MenuItem(ViewName view, boolean separator) {
            this(view,separator,null);
        }

        public MenuItem(ViewName view, boolean separator, Permission p) {
            this.view = view;
            this.separator = separator;
            this.subItems = new ArrayList<MenuBarView.MenuItem>();
            this.permission = p;
        }

        public boolean hasVisibleChildren() {
            for (MenuItem child : getSubItems()) {
                if (!child.isHidden()) {
                    return true;
                }
            }
            return false;
        }

        public MenuItem subItems(MenuItem... items) {
            getSubItems().addAll(Arrays.asList(items));
            return this;
        }

        public ArrayList<MenuItem> getSubItems() {
            return subItems;
        }

        public boolean isSeparator() {
            return separator;
        }

        public ViewName getView() {
            return view;
        }

        public Permission getPermission() {
            return permission;
        }

        public void setHidden(boolean hidden) {
            this.hidden = hidden;
        }

        public boolean isHidden() {
            return hidden;
        }
    }
}
