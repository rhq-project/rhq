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
package org.rhq.enterprise.gui.util;

import java.io.IOException;

import javax.faces.context.FacesContext;
import javax.faces.context.ExternalContext;
import javax.faces.event.ActionEvent;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlOutputLink;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;

import org.richfaces.component.html.HtmlListShuttle;

/**
 * 
 *
 * @author Greg Hinkle
 */
public class IncludeUIBean {


    public void menuItemListener(ActionEvent event) {
        UIComponent c = event.getComponent();
        UIComponent child = c.getChildren().get(0);
        if (child instanceof HtmlOutputLink) {
            String link = (String) ((HtmlOutputLink)child).getValue();

            FacesContext facesContext = FacesContext.getCurrentInstance();
            ExternalContext externalContext = facesContext.getExternalContext();
            HttpServletResponse resp = (HttpServletResponse) externalContext.getResponse();
            try {
                resp.sendRedirect(link);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

        }

    }

    public void setContent(Object content) {
    }


    public Object getContent() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        ExternalContext externalContext = facesContext.getExternalContext();

        HttpServletRequest req = (HttpServletRequest) externalContext.getRequest();
        HttpServletResponse resp = (HttpServletResponse) externalContext.getResponse();
        String path = "/portal/SingleTile.jsp?portlet=.dashContent.searchResources";
//        path = "/Dashboard.do";
        RequestDispatcher dispatcher = req.getRequestDispatcher(path);
        try {
            dispatcher.include(req, resp);
        } catch (ServletException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }
}
