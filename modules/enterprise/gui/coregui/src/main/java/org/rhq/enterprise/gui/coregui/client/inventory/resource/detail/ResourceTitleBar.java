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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Cursor;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
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
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagEditorView;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.async.Command;
import org.rhq.enterprise.gui.coregui.client.util.async.CountDownLatch;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedHLayout;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceTitleBar extends EnhancedVLayout {

    //represents row of Resource title details[icon,title,show-details,tags,availability,favorites]
    private EnhancedHLayout top;
    //represents normally closed region of Resource details[to verbosely describe resource across all tabs]
    private EnhancedHLayout details;
    private static final String FAV_ICON = "Favorite_24_Selected.png";
    private static final String NOT_FAV_ICON = "Favorite_24.png";
    private static final String EXPANDED_ICON = "[SKIN]/ListGrid/row_expanded.png";
    private static final String COLLAPSED_ICON = "[SKIN]/ListGrid/row_collapsed.png";
    private static final String COLLAPSED_TOOLTIP = MSG.view_portlet_inventory_tooltip_expand();
    private static final String EXPANDED_TOOLTIP = MSG.view_portlet_inventory_tooltip_collapse();
    private static final String PLUGIN_ERRORS_ICON = "[SKIN]/Dialog/warn.png";
    private static final String LOADING_ICON = "ajax-loader.gif";

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

    private Timer resourceAvailAndErrorsRefreshTime = new Timer() {
        @Override
        public void run() {
            refreshAvailAndResourceErrors();
        }
    };

    public ResourceTitleBar() {
        super();
        //define two rows of content
        top = new EnhancedHLayout();
        top.setPadding(0);
        top.setMembersMargin(0);
        top.setHeight(30);

        details = new EnhancedHLayout();
        details.setWidth100();

        //modify VLayout settings
        setWidth100();
        setHeight(30);
        setMembersMargin(0);
        setPadding(5);
    }

    public void update() {
        //clean up old widgets
        for (Canvas child : getChildren()) {
            child.destroy();
        }

        this.title = new HTMLFlow();
        this.title.setWidth("*");

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
        expandCollapseArrow = new Img(COLLAPSED_ICON, 16, 16);
        expandCollapseArrow.setTooltip(COLLAPSED_TOOLTIP);
        expandCollapseArrow.setLayoutAlign(VerticalAlignment.BOTTOM);
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
                winModal.setTitle(MSG.common_title_component_errors());

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

        loading = new Img(LOADING_ICON, 16, 16);
        loading.setVisible(false);
        loading.setValign(VerticalAlignment.CENTER);

        //top information
        top.addMember(expandCollapseArrow);
        top.addMember(badge);
        top.addMember(title);
        top.addMember(loading);
        top.addMember(pluginErrors);
        top.addMember(availabilityImage);
        top.addMember(favoriteButton);

        detailsForm = new EnhancedHLayout();
        detailsForm.setWidth100();
        detailsForm.setHeight(10);
        detailsForm.setAlign(Alignment.LEFT);

        detailsFormSummary = new OverviewForm(resourceComposite, this);
        detailsFormSummary.setWidth100();
        detailsFormSummary.setPadding(3);
        detailsFormSummary.setMargin(3);
        detailsFormSummary.setLayoutAlign(Alignment.LEFT);

        //condense details for display
        detailsFormSummary.setHeaderEnabled(false);
        detailsFormSummary.setDisplayCondensed(true);
        detailsForm.addMember(detailsFormSummary);

        SpacerItem widthSpace = new SpacerItem();
        widthSpace.setWidth(40);
        DynamicForm wrappedSpacer = new DynamicForm();
        wrappedSpacer.setFields(widthSpace);
        detailsForm.addMember(wrappedSpacer);
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

        resourceAvailAndErrorsRefreshTime.scheduleRepeating(15000);
    }

    @Override
    protected void onUnload() {
        if (resourceAvailAndErrorsRefreshTime != null) {
            resourceAvailAndErrorsRefreshTime.cancel();
        }

        super.onUnload();
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

            badge.setSrc(ImageManager.getResourceLargeIcon(this.resource));

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
        refreshErrors(null);
    }

    public void refreshAvailAndResourceErrors() {
        CountDownLatch latch = CountDownLatch.create(2, new Command() {
            @Override
            public void execute() {
                loading.setVisible(false);
                markForRedraw();
            }
        });

        loading.setVisible(true);
        loading.markForRedraw();

        refreshAvailability(latch);
        refreshErrors(latch);
    }

    private void refreshErrors(final CountDownLatch latch) {
        GWTServiceLookup.getResourceService().findResourceErrors(resourceComposite.getResource().getId(),
            new AsyncCallback<List<ResourceError>>() {
                public void onFailure(Throwable caught) {
                    pluginErrors.setVisible(false);
                    CoreGUI.getErrorHandler().handleError(
                        MSG.dataSource_resourceErrors_error_fetchFailure(String.valueOf(resourceComposite.getResource()
                            .getId())), caught);

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
                CoreGUI.getErrorHandler().handleError("I18N: Failed to refresh the availability", caught);
                    //MSG.dataSource_resourceErrors_error_fetchFailure(String.valueOf(resourceComposite.getResource()
                    //    .getId())), caught);
                if (latch != null) {
                    latch.countDown();
                } else {
                    markForRedraw();
                }
            }

            @Override
            public void onSuccess(ResourceAvailability result) {
                availabilityImage.setSrc(ImageManager.getAvailabilityLargeIconFromAvailType(result.getAvailabilityType()));
                resource.setCurrentAvailability(result);
                availabilityImage.markForRedraw();
                if (latch != null) {
                    latch.countDown();
                } else {
                    markForRedraw();
                }
            }
        });
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
