/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.gui.configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

import javax.faces.component.UIComponentBase;
import javax.faces.component.UIPanel;
import javax.faces.component.UIParameter;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlInputTextarea;
import javax.faces.component.html.HtmlPanelGrid;
import javax.faces.context.FacesContext;

import org.rhq.core.gui.util.FacesComponentIdFactory;
import org.rhq.core.gui.util.FacesComponentUtility;

/**
 
 * @author Adam Young
 */
public class RawConfigUIComponent extends UIComponentBase {
    private static final String family = "rhq";
    private static final CharSequence TABLE_STYLE = "<style type=\"text/css\">" + "td.raw-config-table {"
        + "vertical-align: top;" + "valign: top;" + "}" + "</style>";
    private FacesComponentIdFactory idFactory;
    private HtmlInputTextarea inputTextarea;
    private ArrayList<String> configPathList;
    public HtmlCommandLink saveLink;
    private Vector<HtmlCommandLink> pathCommandLinks;
    private ArrayList<UIParameter> readOnlyParms;
    private boolean readOnly;

    @Override
    public String getFamily() {
        return family;
    }

    private void buildChildren() {

        if (getChildCount() > 0) {
            return;
        }

        FacesComponentUtility.addVerbatimText(this, TABLE_STYLE);

        UIPanel rawPanel = FacesComponentUtility.addBlockPanel(this, idFactory, "");

        HtmlPanelGrid grid = FacesComponentUtility.addPanelGrid(rawPanel, idFactory, 2, "summary-props-table");
        grid.setParent(this);

        grid.setColumnClasses("raw-config-table");

        HtmlPanelGrid panelLeft = FacesComponentUtility.addPanelGrid(grid, idFactory, 1, "summary-props-table");
        panelLeft.setBgcolor("#a4b2b9");

        FacesComponentUtility.addOutputText(panelLeft, idFactory, "Raw Configurations Paths", "");

        int rawCount = 0;

        configPathList = new ArrayList<String>();
        Collections.sort(configPathList);
        String oldDirname = "";

        pathCommandLinks = new Vector<HtmlCommandLink>();
        for (String s : configPathList) {

            String dirname = s.substring(0, s.lastIndexOf("/") + 1);
            String basename = s.substring(s.lastIndexOf("/") + 1, s.length());
            if (!dirname.equals(oldDirname)) {
                FacesComponentUtility.addOutputText(panelLeft, idFactory, dirname, "");
                oldDirname = dirname;
            }
            UIPanel nextPath = FacesComponentUtility.addBlockPanel(panelLeft, idFactory, "");
            HtmlCommandLink link = FacesComponentUtility.addCommandLink(nextPath, idFactory);
            FacesComponentUtility.addOutputText(link, idFactory, "", "");
            FacesComponentUtility.addOutputText(link, idFactory, basename, "");
            FacesComponentUtility.addParameter(link, idFactory, "path", s);
            FacesComponentUtility.addParameter(link, idFactory, "whichRaw", Integer.toString(rawCount++));
            FacesComponentUtility.addParameter(link, idFactory, "showRaw", Boolean.TRUE.toString());
            readOnlyParms.add(FacesComponentUtility.addParameter(link, idFactory, "readOnly", Boolean
                .toString(readOnly)));

            pathCommandLinks.add(link);
        }

        UIPanel panelRight = FacesComponentUtility.addBlockPanel(grid, idFactory, "summary-props-table");
        UIPanel editPanel = FacesComponentUtility.addBlockPanel(panelRight, idFactory, "summary-props-table");
        this.inputTextarea = createConfigTextArea(readOnly);
        editPanel.getChildren().add(this.inputTextarea);
        inputTextarea.setParent(editPanel);

    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        // TODO Auto-generated method stub
        buildChildren();

    }

    public ArrayList<String> getConfigPathList() {
        return configPathList;
    }

    public void setConfigPathList(ArrayList<String> configPathList) {
        this.configPathList = configPathList;
    }

    private HtmlInputTextarea createConfigTextArea(boolean readOnly) {
        HtmlInputTextarea inputTextarea = new HtmlInputTextarea();
        inputTextarea.setId("rawconfigtextarea");
        inputTextarea.setCols(80);
        inputTextarea.setRows(40);
        inputTextarea.setReadonly(readOnly);

        return inputTextarea;
    }

}
