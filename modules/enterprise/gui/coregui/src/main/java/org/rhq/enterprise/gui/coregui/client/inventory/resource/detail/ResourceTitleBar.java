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
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagEditorView;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceTitleBar extends LocatableHLayout {
    private static final String FAV_ICON = "Favorite_24_Selected.png";
    private static final String NOT_FAV_ICON = "Favorite_24.png";

    private Resource resource;

    private Img badge;
    private Img favoriteButton;
    private HTMLFlow title;
    private Img availabilityImage;
    private boolean favorite;

    public ResourceTitleBar(String locatorId) {
        super(locatorId);
        setWidth100();
        setHeight(30);
        setPadding(5);
        setMembersMargin(5);
    }

    public void update() {
        for (Canvas child : getChildren()) {
            child.destroy();
        }

        this.title = new HTMLFlow();
        this.title.setWidth("*");

        this.availabilityImage = new Img("resources/availability_grey_24.png", 24, 24);

        this.favoriteButton = new Img(NOT_FAV_ICON, 24, 24);

        this.favoriteButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                Set<Integer> favorites = toggleFavoriteLocally();
                CoreGUI.getUserPreferences().setFavoriteResources(favorites, new UpdateFavoritesCallback());
            }
        });

        badge = new Img("types/Service_up_24.png", 24, 24);

        TagEditorView tagEditorView = new TagEditorView(extendLocatorId("TagEdit"), resource.getTags(), false,
            new TagsChangedCallback() {
                public void tagsChanged(final HashSet<Tag> tags) {
                    GWTServiceLookup.getTagService().updateResourceTags(resource.getId(), tags,
                        new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError("Failed to update resource tags", caught);
                            }

                            public void onSuccess(Void result) {
                                CoreGUI.getMessageCenter().notify(
                                    new Message("Resource tags updated", Message.Severity.Info));
                                // update what is essentially our local cache
                                resource.setTags(tags);
                            }
                        });
                }
            });

        loadTags(tagEditorView);

        addMember(badge);
        addMember(title);
        addMember(tagEditorView);
        addMember(availabilityImage);
        addMember(favoriteButton);
    }

    private void loadTags(final TagEditorView tagEditorView) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterId(resource.getId());
        criteria.fetchTags(true);
        GWTServiceLookup.getResourceService().findResourcesByCriteria(criteria,
            new AsyncCallback<PageList<Resource>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Could not load resource tags", caught);
                }

                public void onSuccess(PageList<Resource> result) {
                    LinkedHashSet<Tag> tags = new LinkedHashSet<Tag>();
                    tags.addAll(result.get(0).getTags());
                    tagEditorView.setTags(tags);
                }
            });
    }

    public void setResource(Resource resource) {
        if (this.resource == null || this.resource.getId() != resource.getId()) {
            this.resource = resource;
            update();

            this.title.setContents("<span class=\"SectionHeader\">" + resource.getName()
                + "</span>&nbsp;<span class=\"subtitle\">" + resource.getResourceType().getName() + "</span>");

            Set<Integer> favorites = CoreGUI.getUserPreferences().getFavoriteResources();
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
        this.favoriteButton.setTooltip("Click to " + (favorite ? "remove" : "add") + " this Resource as a favorite.");
    }

    private Set<Integer> toggleFavoriteLocally() {
        this.favorite = !this.favorite;
        Set<Integer> favorites = CoreGUI.getUserPreferences().getFavoriteResources();
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
            CoreGUI.getMessageCenter().notify(
                new Message((favorite ? "Added " : "Removed ") + " Resource "
                    + ResourceTitleBar.this.resource.getName() + " as a favorite.", Message.Severity.Info));
            updateFavoriteButton();
        }

        public void onFailure(Throwable throwable) {
            CoreGUI.getMessageCenter().notify(
                new Message("Failed to " + (favorite ? "add " : "remove ") + " Resource "
                    + ResourceTitleBar.this.resource.getName() + " as a favorite.", Message.Severity.Error));
            // Revert back to our original favorite status, since the server update failed.
            toggleFavoriteLocally();
        }
    }
}
