/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.resource;

import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.grid.events.FieldStateChangedEvent;
import com.smartgwt.client.widgets.grid.events.FieldStateChangedHandler;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;

/**
 * @author Greg Hinkle
 */
public class FavoriteResourcesPortlet extends ResourceSearchView implements Portlet {

    public static final String KEY = "Favorite Resources";

    public static final String CFG_TABLE_PREFS = "tablePreferences";

    private DashboardPortlet storedPortlet;
    private PortletWindow portletWindow;

    public FavoriteResourcesPortlet() {
        super();
        setOverflow(Overflow.HIDDEN);

        setShowHeader(false);
        setShowFooter(false);



    }

    @Override
    protected void configureTable() {
        super.configureTable();

        Set<Integer> favoriteIds = CoreGUI.getUserPreferences().getFavoriteResources();

        Integer[] favArray = favoriteIds.toArray(new Integer[favoriteIds.size()]);


        Criteria criteria = new Criteria();
        if (favoriteIds.isEmpty()) {
            criteria.addCriteria("id", -1);
        } else {
            criteria.addCriteria("resourceIds", favArray);
        }

        refresh(criteria);


        getListGrid().addFieldStateChangedHandler(new FieldStateChangedHandler() {
            public void onFieldStateChanged(FieldStateChangedEvent fieldStateChangedEvent) {
                String state = getListGrid().getViewState();

                storedPortlet.getConfiguration().put(new PropertySimple(CFG_TABLE_PREFS, state));
                portletWindow.save();
            }
        });

    }

    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        this.portletWindow = portletWindow;
        this.storedPortlet = storedPortlet;

        if (storedPortlet.getConfiguration().getSimple(CFG_TABLE_PREFS) != null) {
            String state = storedPortlet.getConfiguration().getSimple(CFG_TABLE_PREFS).getStringValue();
            getListGrid().setViewState(state);
        }


    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow("This portlet displays your favorite resources");
    }


    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance() {
            return GWT.create(FavoriteResourcesPortlet.class);
        }
    }

}
