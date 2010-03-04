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

    /**
     * resourceUIBean is injected through JSF in inventory-beans.xml. ResourceUIBean is injected because this class
     * needs access to the currently requested resource id, but the id is not available as a parameter with all
     * requests, particularly with the raw config editor. The raw config editor however extends the life of the
     * current ResourceUIBean beyond the current request so that the id can be accessed through ResourceUIBean. 
     */
    private ResourceUIBean resourceUIBean;

    public FavoritesUIBean() {
    }

    public ResourceUIBean getResourceUIBean() {
        return resourceUIBean;
    }

    public void setResourceUIBean(ResourceUIBean resourceUIBean) {
        this.resourceUIBean = resourceUIBean;
    }

    public boolean isFavorite() {
        if (favorite == null) {
            log.debug("isFavorite for " + resourceUIBean.getId());
            favorite = QuickFavoritesUtil.determineIfFavoriteResource(resourceUIBean.getId());
        }
        return favorite;
    }

    public String toggleFavorite() {
        log.debug("toggleFavorite for " + resourceUIBean.getId());
        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        WebUserPreferences preferences = user.getWebPreferences();
        WebUserPreferences.FavoriteResourcePortletPreferences favoriteResourcePreferences = preferences
            .getFavoriteResourcePortletPreferences();

        boolean isFav = favoriteResourcePreferences.isFavorite(resourceUIBean.getId());
        if (isFav) {
            favoriteResourcePreferences.removeFavorite(resourceUIBean.getId());
            log.debug("Removing favorite: " + resourceUIBean.getId());
        } else {
            favoriteResourcePreferences.addFavorite(resourceUIBean.getId());
            log.debug("Adding favorite: " + resourceUIBean.getId());
        }

        preferences.setFavoriteResourcePortletPreferences(favoriteResourcePreferences);

        favorite = !isFav;

        return null;
    }

}