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
package org.rhq.enterprise.gui.subsystem;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.richfaces.component.html.HtmlTabPanel;

import org.rhq.core.gui.util.FacesContextUtility;

/**
 * thanks to a bug in facelets ALL tabs are evaluated in either 'server' or 'ajax' mode, 
 * even though only one of them is being viewing at any one point in time.  facelets is
 * erroneously evaluating all expression tab values at compile time.
 * 
 * @author Joseph Marques
 */
public class SubsystemViewsUIBean {

    protected final Log log = LogFactory.getLog(SubsystemViewsUIBean.class);

    private HtmlTabPanel tabPanel = new HtmlTabPanel();

    public SubsystemViewsUIBean() {
        String tab = FacesContextUtility.getOptionalRequestParameter("tab", "configurationUpdates");
        // must provide some tab by default, otherwise the panel will render nothing
        tabPanel.setSelectedTab(tab);
    }

    public HtmlTabPanel getTabPanel() {
        return tabPanel;
    }

    public void setTabPanel(HtmlTabPanel tabPanel) {
        this.tabPanel = tabPanel;
    }

    public void processValueChange(ValueChangeEvent event) {
        // intercept the click on some other tab, and reformat the URL as needed through redirection
        String newTabName = (String) event.getNewValue();
        FacesContext context = FacesContextUtility.getFacesContext();
        String subsystemViewMainURL = context.getViewRoot().getViewId();

        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();
        try {
            // perhaps a little nasty, but it works
            response.sendRedirect(subsystemViewMainURL + "?tab=" + newTabName);
        } catch (IOException ioe) {
            log.warn("Could not redirect from subsystem view tab click, resetting with default tab");
        }
    }
}
