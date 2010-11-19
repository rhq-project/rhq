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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceError;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagEditorView;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary.OverviewForm;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary.ResourceErrorsDataSource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.summary.ResourceErrorsView;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableImg;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceTitleBar extends LocatableVLayout {

    //represents row of Resource title details[icon,title,show-details,tags,availability,favorites]
    private LocatableHLayout top;
    //represents normally closed region of Resource details[to verbosely describe resource across all tabs]
    private LocatableHLayout details;
    private static String COMPONENT_ERROR_COUNT_MSG = MSG.common_title_component_errors();
    private static final String FAV_ICON = "Favorite_24_Selected.png";
    private static final String NOT_FAV_ICON = "Favorite_24.png";

    private Resource resource;
    private ResourceComposite resourceComposite;

    private Img badge;
    private Img favoriteButton;
    private HTMLFlow title;
    private Img availabilityImage;
    private boolean favorite;
    private LocatableHLayout detailsForm;
    private OverviewForm detailsFormSummary;
    private LinkItem pluginErrors;

    public ResourceTitleBar(String locatorId) {
        super(locatorId);
        //define two rows of content
        top = new LocatableHLayout(locatorId + "_Top");
        top.setPadding(5);
        top.setMembersMargin(5);
        top.setHeight(30);

        details = new LocatableHLayout(locatorId + "_Details");
        details.setWidth100();
        details.setHeight(10);//initialize to small amount of pixels

        //modify VLayout settings
        setWidth100();
        setHeight(30);
        setPadding(0);
        setMembersMargin(0);
        setLayoutMargin(0);
    }

    public void update() {
        //clean up old widgets
        for (Canvas child : getChildren()) {
            child.destroy();
        }

        this.title = new HTMLFlow();
        this.title.setWidth("*");

        this.availabilityImage = new Img("resources/availability_grey_24.png", 24, 24);

        this.favoriteButton = new LocatableImg(this.extendLocatorId("Favorite"), NOT_FAV_ICON, 24, 24);

        this.favoriteButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                Set<Integer> favorites = toggleFavoriteLocally();
                UserSessionManager.getUserPreferences().setFavoriteResources(favorites, new UpdateFavoritesCallback());
            }
        });

        badge = new Img("types/Service_up_24.png", 24, 24);

        TagEditorView tagEditorView = new TagEditorView(extendLocatorId("TagEdit"), resource.getTags(), false,
            new TagsChangedCallback() {
                public void tagsChanged(final HashSet<Tag> tags) {
                    GWTServiceLookup.getTagService().updateResourceTags(resource.getId(), tags,
                        new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler()
                                    .handleError(MSG.view_resource_title_tagUpdateFailed(), caught);
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

        //creating link to put in resource title bar
        pluginErrors = new LinkItem("plugin-errors");
        pluginErrors.setTitle("");
        pluginErrors.setLinkTitle(COMPONENT_ERROR_COUNT_MSG + " (0)");
        GWTServiceLookup.getResourceService().findResourceErrors(resourceComposite.getResource().getId(),
            new AsyncCallback<List<ResourceError>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.dataSource_resourceErrors_error_fetchFailure(String.valueOf(resourceComposite.getResource()
                            .getId())), caught);
                }

                public void onSuccess(List<ResourceError> result) {
                    if (result.isEmpty()) {
                        pluginErrors.setLinkTitle(COMPONENT_ERROR_COUNT_MSG + " (" + result.size() + ")");
                    } else {
                        pluginErrors.setLinkTitle("<font color='red'>" + COMPONENT_ERROR_COUNT_MSG + " ("
                            + result.size() + ")</font>");
                    }
                    markForRedraw();
                }
            });

        //define tool tip
        pluginErrors.setPrompt(MSG.view_resource_title_component_errors_tooltip());

        //define click action to pop open detailed view of Component plugin errors
        pluginErrors.addClickHandler(new com.smartgwt.client.widgets.form.fields.events.ClickHandler() {
            @Override
            public void onClick(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
                final Window winModal = new Window();
                winModal.setWidth("70%");
                winModal.setHeight("70%");
                winModal.setTitle(MSG.common_title_component_errors() + ":");
                winModal.setShowMinimizeButton(false);
                winModal.setIsModal(true);
                winModal.setShowModalMask(true);
                winModal.setShowCloseButton(true);
                winModal.centerInPage();
                winModal.addCloseClickHandler(new CloseClickHandler() {
                    @Override
                    public void onCloseClick(CloseClientEvent event) {
                        winModal.destroy();
                    }
                });

                LocatableVLayout form = new LocatableVLayout(extendLocatorId("_Modal_Form"));
                form.setAlign(VerticalAlignment.CENTER);
                form.setLayoutMargin(20);
                form.setWidth100();
                form.setHeight("40%");
                ResourceErrorsView errorsGrid = new ResourceErrorsView(extendLocatorId("errors"), MSG
                    .view_summaryOverview_header_detectedErrors(), null, null,
                    new String[] { ResourceErrorsDataSource.Field.DETAIL });
                errorsGrid.setWidth100();
                errorsGrid.setHeight("400");

                Resource resource = resourceComposite.getResource();
                ResourceErrorsDataSource errors = new ResourceErrorsDataSource(resource.getId());

                errorsGrid.setShowFooter(false);
                errorsGrid.setDataSource(errors);
                form.addMember(errorsGrid);
                winModal.addChild(form);

                winModal.show();
            }
        });

        //top information
        top.addMember(badge);
        top.addMember(title);
        top.addMember(tagEditorView);
        DynamicForm wrappedPluginErrors = new DynamicForm();
        wrappedPluginErrors.setFields(pluginErrors);
        top.addMember(wrappedPluginErrors);
        top.addMember(availabilityImage);
        top.addMember(favoriteButton);

        //detail information
        //checkbox
        final CheckboxItem displayMore = new CheckboxItem();
        final String moreDetails = MSG.common_title_show_more();
        displayMore.setName("resourceDetails");
        displayMore.setTitle(moreDetails);
        displayMore.setWidth(120);
        //conditionally expand more details section.
        displayMore.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                Boolean displayMoreDetails = (Boolean) event.getValue();
                if (displayMoreDetails) {
                    detailsFormSummary.show();
                } else {
                    detailsFormSummary.hide();
                }
                markForRedraw();
            }
        });

        //wrap checkbox for inclusion in details form.
        LocatableDynamicForm wrappedCheckbox = new LocatableDynamicForm(extendLocatorId("Title_Bar_Checkbox"));
        wrappedCheckbox.setFields(displayMore);
        detailsForm = new LocatableHLayout(extendLocatorId("_Resource_Details"));
        detailsForm.setWidth100();
        detailsForm.setHeight(10);
        detailsForm.setAlign(Alignment.LEFT);
        detailsForm.addMember(wrappedCheckbox);

        detailsFormSummary = new OverviewForm(extendLocatorId("Title_Optional_Summary"), resourceComposite);
        detailsFormSummary.setWidth100();
        detailsFormSummary.setPadding(0);
        detailsFormSummary.setMargin(0);
        detailsFormSummary.setLayoutAlign(Alignment.LEFT);

        //condense details for display
        detailsFormSummary.setHeaderEnabled(false);
        detailsFormSummary.setDisplayCondensed(true);
        detailsFormSummary.hide();
        detailsForm.addMember(detailsFormSummary);

        SpacerItem widthSpace = new SpacerItem();
        widthSpace.setWidth(40);
        DynamicForm wrappedSpacer = new DynamicForm();
        wrappedSpacer.setFields(widthSpace);
        detailsForm.addMember(wrappedSpacer);
        details.addChild(detailsForm);

        //order the components
        addMember(top);
        addMember(details);
        top.markForRedraw();
        details.markForRedraw();
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

    public void setResource(ResourceComposite resourceComposite) {
        if (this.resource == null || this.resource.getId() != resource.getId()) {
            this.resource = resourceComposite.getResource();
            this.resourceComposite = resourceComposite;
            update();

            this.title.setContents("<span class=\"SectionHeader\">" + resource.getName()
                + "</span>&nbsp;<span class=\"subtitle\">" + resource.getResourceType().getName() + "</span>");

            Set<Integer> favorites = UserSessionManager.getUserPreferences().getFavoriteResources();
            this.favorite = favorites.contains(resource.getId());
            updateFavoriteButton();

            this.availabilityImage.setSrc("resources/availability_"
                + (resource.getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP ? "green" : "red")
                + "_24.png");

            String category = this.resource.getResourceType().getCategory().getDisplayName();

            String avail = (resource.getCurrentAvailability() != null && resource.getCurrentAvailability()
                .getAvailabilityType() != null) ? (resource.getCurrentAvailability().getAvailabilityType().name()
                .toLowerCase()) : "down";
            badge.setSrc("types/" + category + "_" + avail + "_24.png");

            markForRedraw();
        }
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
            CoreGUI.getMessageCenter().notify(new Message(msg, Message.Severity.Info));

            // Revert back to our original favorite status, since the server update failed.
            toggleFavoriteLocally();
        }
    }
}
