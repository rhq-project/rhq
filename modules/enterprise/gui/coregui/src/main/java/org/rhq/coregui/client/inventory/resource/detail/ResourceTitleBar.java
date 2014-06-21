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
package org.rhq.coregui.client.inventory.resource.detail;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Cursor;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.SpacerItem;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.ResourceAvailability;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.components.tagging.TagEditorView;
import org.rhq.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.coregui.client.dashboard.AutoRefreshUtil;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.AutoRefresh;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.async.Command;
import org.rhq.coregui.client.util.async.CountDownLatch;
import org.rhq.coregui.client.util.enhanced.EnhancedHLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceTitleBar extends EnhancedVLayout {

    //represents row of Resource title details[icon,title,show-details,tags,availability,favorites]
    private EnhancedHLayout top;
    //represents normally closed region of Resource details[to verbosely describe resource across all tabs]
    private EnhancedHLayout details;
    private static final String FAV_ICON = "[SKIN]/Favorite_24_Selected.png";
    private static final String NOT_FAV_ICON = "[SKIN]/Favorite_24.png";
    private static final String EXPANDED_ICON = "[SKIN]/ListGrid/row_expanded.png";
    private static final String COLLAPSED_ICON = "[SKIN]/ListGrid/row_collapsed.png";
    private static final String COLLAPSED_TOOLTIP = MSG.view_portlet_inventory_tooltip_expand();
    private static final String EXPANDED_TOOLTIP = MSG.view_portlet_inventory_tooltip_collapse();
    private static final String PLUGIN_ERRORS_ICON = "[SKIN]/Dialog/warn.png";
    private static final String LOADING_ICON = "[SKIN]/loading.gif";

    private Img expandCollapseArrow;

    private Resource resource;
    private ResourceComposite resourceComposite;

    private Img badge;
    private Img favoriteButton;
    private HTMLFlow title;
    private Img availabilityImage;
    private boolean favorite;
    private EnhancedHLayout detailsForm;
    private OverviewForm detailsFormSummary;
    private Img pluginErrors;
    private Img loading;
    private final ResourceTreeView platformTree;

    private class AvailAndErrorRefresher implements AutoRefresh {
        private Timer availAndErrorsRefreshTimer;
        private volatile boolean refreshingAvail;
        private volatile boolean refreshingErrors;
        private final int intervalMillis;

        public AvailAndErrorRefresher(int intervalMillis) {
            this.intervalMillis = intervalMillis;
        }

        @Override
        public boolean isRefreshing() {
            return refreshingAvail || refreshingErrors;
        }

        @Override
        public void startRefreshCycle() {
            availAndErrorsRefreshTimer = AutoRefreshUtil
                .startRefreshCycle(this, ResourceTitleBar.this, availAndErrorsRefreshTimer, intervalMillis);
        }

        @Override
        public void refresh() {
            refresh(true, true);
        }

        public void refresh(boolean availability, boolean errors) {
            int cnt = 0;

            if (availability && !refreshingAvail) {
                refreshingAvail = true;
                ++cnt;
            }

            if (errors && !refreshingErrors) {
                refreshingErrors = true;
                ++cnt;
            }

            if (cnt == 0) {
                return;
            }

            CountDownLatch latch = CountDownLatch.create(cnt, new Command() {
                @Override
                public void execute() {
                    loading.setStyleDependentName("hidden", true);
                    //loading.setVisible(false);
                    markForRedraw();

                    //the checks at the start of the refresh() method above ensure that there is at most
                    //1 avail check and 1 error check running at any single time.
                    //we can therefore be sure here that if refreshing* variable is true, it has been set so when
                    //the request that this latch is guarding has been started.
                    if (refreshingAvail) {
                        refreshingAvail = false;
                    }
                    if (refreshingErrors) {
                        refreshingErrors = false;
                    }
                }
            });

            // loading.setVisible(true);
            loading.setStyleDependentName("hidden", false);

            loading.markForRedraw();

            if (refreshingAvail) {
                refreshAvailability(latch);
            }

            if (refreshingErrors) {
                refreshErrors(latch);
            }
        }

        public void stop() {
            AutoRefreshUtil.onDestroy(availAndErrorsRefreshTimer);
        }

        private void refreshErrors(final CountDownLatch latch) {
            GWTServiceLookup.getResourceService().findResourceErrors(resourceComposite.getResource().getId(),
                new AsyncCallback<List<ResourceError>>() {
                    public void onFailure(Throwable caught) {
                        pluginErrors.setVisible(false);

                        if (!UserSessionManager.isLoggedOut()) {
                            CoreGUI.getErrorHandler().handleError(
                                MSG.dataSource_resourceErrors_error_fetchFailure(String.valueOf(resourceComposite.getResource()
                                    .getId())), caught);
                        }

                        if (latch != null) {
                            latch.countDown();
                        } else {
                            markForRedraw();
                        }
                    }

                    public void onSuccess(List<ResourceError> result) {
                        pluginErrors.setVisible(!result.isEmpty());

                        if (latch != null) {
                            latch.countDown();
                        } else {
                            markForRedraw();
                        }
                    }
                });
        }

        private void refreshAvailability(final CountDownLatch latch) {

            final AvailabilityType currentAvail = resource.getCurrentAvailability().getAvailabilityType();

            GWTServiceLookup.getResourceService().getLiveResourceAvailability(resource.getId(),
                new AsyncCallback<ResourceAvailability>() {

                    @Override
                    public void onFailure(Throwable caught) {

                        availabilityImage.setSrc(ImageManager.getAvailabilityLargeIconFromAvailType(currentAvail));
                        badge.setStyleName("resource-detail-" + currentAvail.getName());
                        if (!UserSessionManager.isLoggedOut()) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_inventory_resource_loadFailed(String.valueOf(resource.getId())), caught);
                        }

                        if (latch != null) {
                            latch.countDown();
                        } else {
                            markForRedraw();
                        }
                    }

                    @Override
                    public void onSuccess(ResourceAvailability result) {
                        availabilityImage.setSrc(ImageManager.getAvailabilityLargeIconFromAvailType(currentAvail));
                        resource.setCurrentAvailability(result);
                        badge.setStyleName("resource-detail-" + currentAvail.getName());

                        availabilityImage.markForRedraw();
                        if (latch != null) {
                            latch.countDown();
                        } else {
                            markForRedraw();
                        }

                        if (currentAvail != result.getAvailabilityType()) {
                            platformTree.refreshResource(resource);
                        }
                    }
                });
        }
    }
    private AvailAndErrorRefresher availAndErrorRefresher = new AvailAndErrorRefresher(15000);

    public ResourceTitleBar(ResourceTreeView platformTree) {
        super();

        this.platformTree = platformTree;

        //define two rows of content
        top = new EnhancedHLayout();
        details = new EnhancedHLayout();
        details.setWidth100();

        //modify VLayout settings
        setWidth100();
    }

    public void update() {
        //clean up old widgets
        for (Canvas child : getChildren()) {
            child.destroy();
        }

        this.title = new HTMLFlow();
        this.title.setWidth100();

        this.availabilityImage = new Img(ImageManager.getAvailabilityLargeIcon(null), 24, 24);

        this.favoriteButton = new Img(NOT_FAV_ICON, 24, 24);
        this.favoriteButton.setCursor(Cursor.POINTER);
        this.favoriteButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                Set<Integer> favorites = toggleFavoriteLocally();
                UserSessionManager.getUserPreferences().setFavoriteResources(favorites, new UpdateFavoritesCallback());
            }
        });

        badge = new Img(ImageManager.getResourceLargeIcon(ResourceCategory.SERVICE), 24, 24);

        TagEditorView tagEditorView = new TagEditorView(resource.getTags(), !resourceComposite.getResourcePermission()
            .isInventory(), new TagsChangedCallback() {
            public void tagsChanged(final HashSet<Tag> tags) {
                GWTServiceLookup.getTagService().updateResourceTags(resource.getId(), tags, new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_resource_title_tagUpdateFailed(), caught);
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_titleBar_common_updateTagsSuccessful(resource.getName()),
                                Message.Severity.Info));
                        // update what is essentially our local cache
                        resource.setTags(tags);
                    }
                });
            }
        });

        loadTags(tagEditorView);

        //add expand/collapse icon
        expandCollapseArrow = new Img(COLLAPSED_ICON, 24, 24);
        expandCollapseArrow.setTooltip(COLLAPSED_TOOLTIP);
        expandCollapseArrow.addClickHandler(new ClickHandler() {
            private boolean collapsed = true;

            @Override
            public void onClick(ClickEvent event) {
                collapsed = !collapsed;
                if (collapsed) {
                    expandCollapseArrow.setSrc(COLLAPSED_ICON);
                    expandCollapseArrow.setTooltip(COLLAPSED_TOOLTIP);
                    details.hide();
                } else {
                    expandCollapseArrow.setSrc(EXPANDED_ICON);
                    expandCollapseArrow.setTooltip(EXPANDED_TOOLTIP);
                    details.show();
                }
                ResourceTitleBar.this.markForRedraw();
            }
        });

        pluginErrors = new Img(PLUGIN_ERRORS_ICON, 24, 24);
        pluginErrors.setVisible(false);

        //define tool tip
        pluginErrors.setPrompt(MSG.view_resource_title_component_errors_tooltip());
        pluginErrors.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final Window winModal = new Window();
                winModal.setShowMinimizeButton(false);
                winModal.setShowModalMask(true);
                winModal.setShowCloseButton(true);
                winModal.setWidth("70%");
                winModal.setHeight("70%");
                winModal.setIsModal(true);
                winModal.setShowResizer(true);
                winModal.setCanDragResize(true);
                winModal.centerInPage();
                winModal.setTitle(MSG.common_title_componentErrors());

                Label disposalReminder = new Label();
                disposalReminder.setHeight(12);
                disposalReminder.setPadding(4);
                disposalReminder.setAlign(Alignment.CENTER);
                disposalReminder.setValign(VerticalAlignment.CENTER);
                disposalReminder.setContents(MSG.view_resource_title_component_errors_cleanup());

                EnhancedVLayout form = new EnhancedVLayout();
                form.setAlign(VerticalAlignment.CENTER);
                form.setLayoutMargin(10);
                form.setWidth100();
                form.setHeight100();

                Resource resource = resourceComposite.getResource();
                ResourceErrorsDataSource errors = new ResourceErrorsDataSource(resource.getId());

                ResourceErrorsView errorsGrid = new ResourceErrorsView(null, ResourceTitleBar.this);

                errorsGrid.setDataSource(errors);

                form.addMember(errorsGrid);
                winModal.addItem(disposalReminder);
                winModal.addItem(form);
                winModal.setPadding(2);

                winModal.show();
            }
        });

        loading = new Img(LOADING_ICON, 24, 24);
        // loading.setVisible(false);
        loading.setAlign(Alignment.RIGHT);
        loading.setStyleName("spinner");
        loading.setStyleDependentName("hidden", true);
        //top information
        top.addMember(expandCollapseArrow);
        top.addMember(badge);
        top.addMember(title);

        top.addMember(pluginErrors);
        top.addMember(loading);
        top.addMember(availabilityImage);
        top.addMember(favoriteButton);
        top.setStyleName("resourceSummary");

        detailsForm = new EnhancedHLayout();
        detailsForm.setWidth100();
        detailsForm.setHeight(10);
        detailsForm.setAlign(Alignment.LEFT);

        detailsFormSummary = new OverviewForm(resourceComposite, this);
        detailsFormSummary.setWidth100();
        detailsFormSummary.setStyleName("resourceSummaryDetails");
        detailsFormSummary.setLayoutAlign(Alignment.LEFT);

        //condense details for display
        detailsFormSummary.setHeaderEnabled(false);
        detailsFormSummary.setDisplayCondensed(true);
        detailsForm.addMember(detailsFormSummary);

        SpacerItem widthSpace = new SpacerItem();
        widthSpace.setWidth(40);
        //    DynamicForm wrappedSpacer = new DynamicForm();
        //  wrappedSpacer.setFields(widthSpace);
        // detailsForm.addMember(wrappedSpacer);
        details.addChild(detailsForm);
        details.hide();

        //order the components
        addMember(top);
        //conditionally add tags. Defaults to true, not available in JON builds.
        if (CoreGUI.isTagsEnabledForUI()) {
            addMember(tagEditorView);
        }
        addMember(details);
        ResourceTitleBar.this.markForRedraw();

        availAndErrorRefresher.startRefreshCycle();
    }

    @Override
    protected void onDestroy() {
        availAndErrorRefresher.stop();
        super.onDestroy();
    }

    private void loadTags(final TagEditorView tagEditorView) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterId(resource.getId());
        criteria.fetchTags(true);
        GWTServiceLookup.getResourceService().findResourcesByCriteria(criteria,
            new AsyncCallback<PageList<Resource>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_titleBar_common_loadTagsFailure(resource.getName()),
                        caught);
                }

                public void onSuccess(PageList<Resource> result) {
                    LinkedHashSet<Tag> tags = new LinkedHashSet<Tag>();
                    tags.addAll(result.get(0).getTags());
                    tagEditorView.setTags(tags);
                }
            });
    }

    public void setResource(ResourceComposite resourceComposite, boolean isRefresh) {
        if (this.resourceComposite == null || resourceComposite.getResource().getId() != this.resource.getId()
            || isRefresh) {
            this.resource = resourceComposite.getResource();
            this.resourceComposite = resourceComposite;
            update();

            displayResourceName(resource.getName());

            Set<Integer> favorites = UserSessionManager.getUserPreferences().getFavoriteResources();
            this.favorite = favorites.contains(resource.getId());
            updateFavoriteButton();

            this.availabilityImage.setSrc(ImageManager.getAvailabilityLargeIconFromAvailType(resource
                .getCurrentAvailability().getAvailabilityType()));

            badge.setSrc(ImageManager.getResourceLargeIcon(this.resource.getResourceType().getCategory()));

            markForRedraw();
        }
    }

    public ResourceComposite getResource() {
        return resourceComposite;
    }

    void displayResourceName(String resourceName) {
        if (!resource.getName().equals(resourceName)) {
            resource.setName(resourceName); // the name must have been changed by the user via the editable field
        }
        this.title.setContents("<span class=\"SectionHeader\">" + StringUtility.escapeHtml(resourceName)
            + "</span>&nbsp;<span class=\"subtitle\">" + resource.getResourceType().getName() + "</span>");
        this.title.markForRedraw();
    }

    private void updateFavoriteButton() {
        this.favoriteButton.setSrc(favorite ? FAV_ICON : NOT_FAV_ICON);
        if (favorite) {
            this.favoriteButton.setTooltip(MSG.view_titleBar_common_clickToRemoveFav());
        } else {
            this.favoriteButton.setTooltip(MSG.view_titleBar_common_clickToAddFav());
        }
    }

    private Set<Integer> toggleFavoriteLocally() {
        this.favorite = !this.favorite;
        Set<Integer> favorites = UserSessionManager.getUserPreferences().getFavoriteResources();
        int resourceId = this.resource.getId();
        if (this.favorite) {
            favorites.add(resourceId);
        } else {
            favorites.remove(resourceId);
        }
        return favorites;
    }

    public void refreshResourceErrors() {
        availAndErrorRefresher.refresh(false, true);
    }

    public void refreshAvailAndResourceErrors() {
        availAndErrorRefresher.refresh();
    }

    public class UpdateFavoritesCallback implements AsyncCallback<Subject> {
        public void onSuccess(Subject subject) {
            String msg = null;
            if (favorite) {
                msg = MSG.view_titleBar_common_addedFav(resource.getName());
            } else {
                msg = MSG.view_titleBar_common_removedFav(resource.getName());
            }
            CoreGUI.getMessageCenter().notify(new Message(msg, Message.Severity.Info));
            updateFavoriteButton();
        }

        public void onFailure(Throwable throwable) {
            String msg = null;
            if (favorite) {
                msg = MSG.view_titleBar_common_addedFavFailure(resource.getName());
            } else {
                msg = MSG.view_titleBar_common_removedFavFailure(resource.getName());
            }
            CoreGUI.getErrorHandler().handleError(msg, throwable);

            // Revert back to our original favorite status, since the server update failed.
            toggleFavoriteLocally();
        }
    }

}
