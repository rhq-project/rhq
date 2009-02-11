/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.inventory.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.WebUserPreferences;
import org.rhq.enterprise.gui.legacy.action.resource.common.QuickFavoritesUtil;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.gui.util.WebUtility;

/**
 * A session-scoped bean for toggling the current resource in request scope
 * 
 * @author Joseph Marques
 */
public class FavoritesUIBean {

    protected final Log log = LogFactory.getLog(FavoritesUIBean.class);

    private Boolean favorite; // true if this resource has been added to the favorites dashboard portlet
    private int id;

    public FavoritesUIBean() {
    }

    public boolean isFavorite() {
        log.debug("isFavorite for " + id);
        id = WebUtility.getResourceId(FacesContextUtility.getRequest());
        this.favorite = QuickFavoritesUtil.determineIfFavoriteResource(FacesContextUtility.getRequest());
        return this.favorite;
    }

    public int getId() {
        return id;
    }

    public String toggleFavorite() {
        log.debug("toggleFavorite for " + id);
        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        WebUserPreferences preferences = user.getWebPreferences();
        WebUserPreferences.FavoriteResourcePortletPreferences favoriteResourcePreferences = preferences
            .getFavoriteResourcePortletPreferences();

        int resourceId = WebUtility.getResourceId(FacesContextUtility.getRequest());

        boolean isFav = favoriteResourcePreferences.isFavorite(resourceId);
        if (isFav) {
            favoriteResourcePreferences.removeFavorite(resourceId);
            log.info("Removing favorite: " + resourceId);
        } else {
            favoriteResourcePreferences.addFavorite(resourceId);
            log.info("Adding favorite: " + resourceId);
        }

        preferences.setFavoriteResourcePortletPreferences(favoriteResourcePreferences);
        preferences.persistPreferences();

        favorite = !isFav;
        log.debug("Setting favorite to: " + this.favorite);

        return null;
    }

    public void setFavorite(boolean favorite) {
        log.debug("setFavorite(" + favorite + ") for " + id);
        this.favorite = favorite;
    }
}