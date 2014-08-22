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
package org.rhq.coregui.client.inventory.groups.detail;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite.GroupAvailabilityType;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.components.tagging.TagEditorView;
import org.rhq.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.enhanced.EnhancedHLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class ResourceGroupTitleBar extends EnhancedVLayout {
    private static final String FAV_ICON = "Favorite_24_Selected.png";
    private static final String NOT_FAV_ICON = "Favorite_24.png";

    private static final String COLLAPSED_TOOLTIP = MSG.view_titleBar_group_summary_collapsedTooltip();
    private static final String EXPANDED_TOOLTIP = MSG.view_titleBar_group_summary_expandedTooltip();

    private ResourceGroup group;
    boolean isAutoCluster;
    boolean isAutoGroup;

    private Img expandCollapseArrow;
    private Img badge;
    private Img favoriteButton;
    private HTMLFlow title;
    private Img explicitAvailabilityImage;
    private Img implicitAvailabilityImage;
    private boolean favorite;
    private boolean supportsFavorite;
    private GeneralProperties generalProperties;

    public ResourceGroupTitleBar(boolean isAutoGroup, boolean isAutoCluster) {
        super();

        this.isAutoGroup = isAutoGroup;
        this.isAutoCluster = isAutoCluster;
        this.supportsFavorite = (!(this.isAutoGroup || this.isAutoCluster));

        setWidth100();
        setHeight(30);
        setPadding(5);
        setMembersMargin(5);
    }

    public void update() {
        for (Canvas child : getChildren()) {
            child.destroy();
        }

        final EnhancedHLayout hlayout = new EnhancedHLayout();
        hlayout.setStyleName("resourceSummary");

        addMember(hlayout);

        this.title = new HTMLFlow();
        this.title.setWidth("*");

        this.explicitAvailabilityImage = new Img(ImageManager.getAvailabilityLargeIcon(null), 24, 24);
        this.explicitAvailabilityImage.setTooltip(MSG.view_group_detail_explicitAvail());
        this.implicitAvailabilityImage = new Img(ImageManager.getAvailabilityLargeIcon(null), 24, 24);
        this.implicitAvailabilityImage.setTooltip(MSG.view_group_detail_implicitAvail());

        if (this.supportsFavorite) {
            this.favoriteButton = new Img(NOT_FAV_ICON, 24, 24);

            this.favoriteButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent clickEvent) {
                    Set<Integer> favorites = toggleFavoriteLocally();
                    UserSessionManager.getUserPreferences().setFavoriteResourceGroups(favorites,
                        new UpdateFavoritesCallback());
                }
            });
        }

        expandCollapseArrow = new Img("[SKIN]/ListGrid/row_collapsed.png", 16, 16);
        expandCollapseArrow.setTooltip(COLLAPSED_TOOLTIP);
        expandCollapseArrow.setLayoutAlign(VerticalAlignment.CENTER);
        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(this.group.getId());
        // for autoclusters and autogroups we need to add more criteria
        if (isAutoCluster) {
            criteria.addFilterVisible(false);
        } else if (isAutoGroup) {
            criteria.addFilterVisible(false);
            criteria.addFilterPrivate(true);
        }

        GWTServiceLookup.getResourceGroupService().findResourceGroupCompositesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroupComposite>>() {
                @Override
                public void onSuccess(PageList<ResourceGroupComposite> result) {
                    if (result == null || result.size() != 1) {
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_titleBar_group_failInfo(group.getName(),
                                String.valueOf(ResourceGroupTitleBar.this.group.getId())));
                        return;
                    }

                    ResourceGroupComposite resultComposite = result.get(0);
                    setGroupIcons(resultComposite);

                    // Localize the default group name for an AutoCluster with disparate members (see setGroup()).
                    if (isAutoCluster) {
                        resultComposite.getResourceGroup().setName(group.getName());
                    }

                    generalProperties = new GeneralProperties(resultComposite, ResourceGroupTitleBar.this,
                        (!(isAutoGroup || isAutoCluster)));
                    generalProperties.setVisible(false);

                    ResourceGroupTitleBar.this.addMember(generalProperties);
                    expandCollapseArrow.addClickHandler(new ClickHandler() {
                        private boolean collapsed = true;

                        @Override
                        public void onClick(ClickEvent event) {
                            collapsed = !collapsed;
                            if (collapsed) {
                                expandCollapseArrow.setSrc("[SKIN]/ListGrid/row_collapsed.png");
                                expandCollapseArrow.setTooltip(COLLAPSED_TOOLTIP);
                                generalProperties.hide();
                            } else {
                                expandCollapseArrow.setSrc("[SKIN]/ListGrid/row_expanded.png");
                                expandCollapseArrow.setTooltip(EXPANDED_TOOLTIP);
                                generalProperties.show();
                            }
                            ResourceGroupTitleBar.this.markForRedraw();
                        }
                    });
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_titleBar_group_failInfo(group.getName(),
                            String.valueOf(ResourceGroupTitleBar.this.group.getId())), caught);
                }
            });

        badge = new Img(ImageManager.getGroupLargeIcon(GroupCategory.MIXED), 24, 24);
        badge.setLayoutAlign(VerticalAlignment.CENTER);


        TagEditorView tagEditorView = new TagEditorView(group.getTags(), false, new TagsChangedCallback() {
            public void tagsChanged(final HashSet<Tag> tags) {
                GWTServiceLookup.getTagService().updateResourceGroupTags(group.getId(), tags,
                    new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(
                                MSG.view_titleBar_common_updateTagsFailure(group.getName()), caught);
                        }

                        public void onSuccess(Void result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message(MSG.view_titleBar_common_updateTagsSuccessful(group.getName()),
                                    Message.Severity.Info));
                            // update what is essentially our local cache
                            group.setTags(tags);
                        }
                    });
            }
        });

        loadTags(tagEditorView);

        hlayout.addMember(expandCollapseArrow);
        hlayout.addMember(badge);
        hlayout.addMember(title);
        hlayout.addMember(explicitAvailabilityImage);
        hlayout.addMember(implicitAvailabilityImage);
        if (this.supportsFavorite) {
            hlayout.addMember(favoriteButton);
        }

        //conditionally add tags. Defaults to true, not available in JON builds.
        if (CoreGUI.isTagsEnabledForUI()) {
            addMember(tagEditorView);
        }
    }

    private void loadTags(final TagEditorView tagEditorView) {
        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(group.getId());
        // for autoclusters and autogroups we need to add more criteria
        if (isAutoCluster) {
            criteria.addFilterVisible(false);
        } else if (isAutoGroup) {
            criteria.addFilterVisible(false);
            criteria.addFilterPrivate(true);
        }
        criteria.fetchTags(true);

        GWTServiceLookup.getResourceGroupService().findResourceGroupsByCriteria(criteria,
            new AsyncCallback<PageList<ResourceGroup>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_titleBar_common_loadTagsFailure(group.getName()),
                        caught);
                }

                public void onSuccess(PageList<ResourceGroup> result) {
                    LinkedHashSet<Tag> tags = new LinkedHashSet<Tag>();
                    tags.addAll(result.get(0).getTags());
                    tagEditorView.setTags(tags);
                }
            });
    }

    public void setGroup(ResourceGroupComposite groupComposite, boolean isRefresh) {
        this.group = groupComposite.getResourceGroup();

        // Localize the default group name for an AutoCluster with disparate members.  This is safe for AutoCluster
        // backing groups because they can't be edited, can't be set as a favorite, and the name is not used
        // for subsequent querying. If an autoCluster contains disparate resource names the server names the group
        // "Group of <ResourceTypeName>" because it can't name the group after a common resource name.  This typically
        // happens if the cluster group (i.e. root group) members are themselves disparate. In general this is not the
        // case, because recursive compat groups are typically used specifically for groups of logically equivalent
        // resources, like cloned AS instances.  The problem is that it is not localized. Change it on the fly.
        if (isAutoCluster) {
            String typeName = group.getResourceType().getName();
            String cannedName = "Group of " + typeName;
            if (cannedName.equals(group.getName())) {
                group.setName(MSG.group_tree_groupOfResourceType(typeName));
            }
        }

        update();

        displayGroupName(group.getName());

        Set<Integer> favorites = UserSessionManager.getUserPreferences().getFavoriteResourceGroups();
        this.favorite = favorites.contains(group.getId());
        updateFavoriteButton();

        setGroupIcons(groupComposite);
        markForRedraw();
    }

    void displayGroupName(String groupName) {
        if (!group.getName().equals(groupName)) {
            group.setName(groupName); // the name must have been changed by the user via the editable field
        }

        String catName = null;
        switch (group.getGroupCategory()) {
        case COMPATIBLE: {
            catName = MSG.view_group_summary_compatible();
            break;
        }
        case MIXED: {
            catName = MSG.view_group_summary_mixed();
            break;
        }
        }

        this.title.setContents("<span class=\"SectionHeader\">" + group.getName()
            + "</span>&nbsp;<span class=\"subtitle\">" + catName + "</span>");
        this.title.markForRedraw();
    }

    private void setGroupIcons(ResourceGroupComposite composite) {
        GroupAvailabilityType explicitGroupAvailType = composite.getExplicitAvailabilityType();

        this.badge.setSrc(ImageManager.getGroupLargeIcon(this.group.getGroupCategory(), explicitGroupAvailType));
        this.explicitAvailabilityImage.setSrc(ImageManager.getAvailabilityGroupLargeIcon(explicitGroupAvailType));

        if (composite.getResourceGroup().isRecursive()) {
            GroupAvailabilityType implicitGroupAvailType = composite.getImplicitAvailabilityType();
            this.implicitAvailabilityImage.setSrc(ImageManager.getAvailabilityGroupLargeIcon(implicitGroupAvailType));
        } else {
            this.implicitAvailabilityImage.setVisible(false);
        }
    }

    private void updateFavoriteButton() {
        if (!this.supportsFavorite) {
            return;
        }

        this.favoriteButton.setSrc(favorite ? FAV_ICON : NOT_FAV_ICON);
        if (favorite) {
            this.favoriteButton.setTooltip(MSG.view_titleBar_common_clickToRemoveFav());
        } else {
            this.favoriteButton.setTooltip(MSG.view_titleBar_common_clickToAddFav());
        }
    }

    private Set<Integer> toggleFavoriteLocally() {
        this.favorite = !this.favorite;
        Set<Integer> favorites = UserSessionManager.getUserPreferences().getFavoriteResourceGroups();
        if (this.favorite) {
            favorites.add(group.getId());
        } else {
            favorites.remove(group.getId());
        }
        return favorites;
    }

    public class UpdateFavoritesCallback implements AsyncCallback<Subject> {
        public void onSuccess(Subject subject) {
            String m;
            if (favorite) {
                m = MSG.view_titleBar_common_addedFav(ResourceGroupTitleBar.this.group.getName());
            } else {
                m = MSG.view_titleBar_common_removedFav(ResourceGroupTitleBar.this.group.getName());
            }
            CoreGUI.getMessageCenter().notify(new Message(m, Message.Severity.Info));
            updateFavoriteButton();
        }

        public void onFailure(Throwable throwable) {
            String m;
            if (favorite) {
                m = MSG.view_titleBar_common_addedFavFailure(ResourceGroupTitleBar.this.group.getName());
            } else {
                m = MSG.view_titleBar_common_removedFavFailure(ResourceGroupTitleBar.this.group.getName());
            }
            CoreGUI.getErrorHandler().handleError(m, throwable);
            // Revert back to our original favorite status, since the server update failed.
            toggleFavoriteLocally();
        }
    }
}
