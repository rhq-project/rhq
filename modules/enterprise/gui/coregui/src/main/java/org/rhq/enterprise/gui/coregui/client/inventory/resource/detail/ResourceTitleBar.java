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

import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;

import com.smartgwt.client.types.SelectionType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.ImgButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;


/**
 * @author Greg Hinkle
 */
public class ResourceTitleBar extends HLayout {

    Resource resource;
    ImgButton favoriteButton;
    HTMLFlow title = new HTMLFlow();
    boolean favorite;

    public static final String FAV_ICON = "Favorite_24_Selected.png";
    public static final String NOT_FAV_ICON = "Favorite_24.png";


    public ResourceTitleBar() {
        super();
        setWidth100();
        setHeight(60);
    }

    @Override
    protected void onInit() {
        super.onInit();

        favoriteButton = new ImgButton();
        favoriteButton.setSrc(NOT_FAV_ICON);
        favoriteButton.setShowHover(false);
        favoriteButton.setShowDownIcon(false);
        favoriteButton.setShowRollOverIcon(false);
        favoriteButton.setShowFocusedIcon(false);

        favoriteButton.setActionType(SelectionType.CHECKBOX);
        favoriteButton.setWidth(24);
        favoriteButton.setHeight(24);

        favoriteButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                favorite = !favorite;

//                favoriteButton.setIcon(favorite ? FAV_ICON : NOT_FAV_ICON);

            }
        });


        addMember(title);
        addMember(favoriteButton);
    }

    public void setResource(Resource resource) {
        this.resource = resource;

        title.setContents("<h2>" + resource.getName() + "</h2>");

        favorite = CoreGUI.getUserPreferences().getFavoriteResources().contains(resource.getId());

        markForRedraw();
    }


}
