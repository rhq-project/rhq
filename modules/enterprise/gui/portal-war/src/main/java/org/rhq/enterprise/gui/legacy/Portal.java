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
package org.rhq.enterprise.gui.legacy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.rhq.core.clientapi.util.StringUtil;

/**
 * A class representing an individual page layout.
 */
public class Portal {
    private String name;
    private String description;
    private int columns = 1;
    private List portlets;
    private boolean dialog = false;
    private boolean workflowPortal = false;

    private Map workflowParams = null;

    public Portal() {
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getColumns() {
        return this.columns;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public void setColumns(String numCols) {
        setColumns(Integer.parseInt(numCols));
    }

    public List getPortlets() {
        return this.portlets;
    }

    public void setPortlets(List portlets) {
        this.portlets = portlets;
    }

    /**
     * adds a single portlet to the column number provided
     */
    public void addPortlet(Portlet portlet, int column) {
        int colIdx = column - 1;

        if (portlets == null) {
            this.portlets = new ArrayList();

            int size = (this.columns > colIdx) ? this.columns : colIdx;
            this.columns = size;
            for (int i = 0; i < size; i++) {
                portlets.add(i, new ArrayList());
            }
        } else {
            if (colIdx >= this.columns) {
                // grow portlets to the required size
                for (int i = this.columns; i < colIdx; i++) {
                    portlets.add(i, new ArrayList());
                }
            }
        }

        List<Portlet> col = (List) portlets.get(colIdx);
        col.add(portlet);
    }

    /**
     * Attach a list of portlets to the first column of the portal.
     *
     * @param definitions the <code>List</code> of either <code>Portlet</code> instances or <code>String</code>
     *                    instances representing portlet definitions
     */
    public void addPortlets(List definitions) {
        addPortlets(definitions, 1);
    }

    /**
     * Attach a list of portlets to the indicated column of the portal.
     *
     * @param definitions the <code>List</code> of either <code>Portlet</code> instances or <code>String</code>
     *                    instances representing portlet definitions
     * @param column      the column (1-based) to which the portlets are added
     */
    public void addPortlets(List definitions, int column) {
        Iterator i = definitions.iterator();
        while (i.hasNext()) {
            Object o = i.next();
            if (o instanceof Portlet) {
                addPortlet((Portlet) o, column);
            } else {
                addPortlet(new Portlet((String) o), column);
            }
        }
    }

    /**
     * Add a column of portlets to the portal. A new column will be created, rather than adding the portlets to an
     * existing column.
     *
     * @param definitions the <code>List</code> of either <code>Portlet</code> instances or <code>String</code>
     *                    instances representing portlet definitions
     */
    public void addPortletColumn(List definitions) {
        // if we have a null or empty column list, use that column,
        // use that one. otherwise, make a new column.
        int nextColIndex = 1;
        if (portlets != null) {
            Iterator i = portlets.iterator();
            int colcnt = 1;
            while (i.hasNext()) {
                List col = (List) i.next();
                if (col != null) {
                    if (col.size() == 0) {
                        nextColIndex = colcnt;
                        break;
                    }

                    colcnt++;
                    continue;
                }

                nextColIndex = colcnt;
                break;
            }
        }

        addPortlets(definitions, nextColIndex);
    }

    public String toString() {
        return "Portal [" + getName() + "]";
    }

    public boolean isDialog() {
        return this.dialog;
    }

    public void setDialog(boolean dialog) {
        this.dialog = dialog;
    }

    public void setDialog(String dialog) {
        this.dialog = ((dialog != null) && dialog.equalsIgnoreCase("true"));
    }

    /**
     * Getter for property workflowPortal. If you wish to explictly set this screen as part of a workflow, set this to
     * true in your controller action.
     *
     * @return Value of property workflowPortal.
     */
    public boolean isWorkflowPortal() {
        return this.workflowPortal;
    }

    public void setWorkflowPortal(boolean workflowPortal) {
        this.workflowPortal = workflowPortal;
    }

    public void setWorkflowPortal(String workflowPortal) {
        if ("true".equalsIgnoreCase(workflowPortal)) {
            this.workflowPortal = true;
        } else {
            this.workflowPortal = false;
        }
    }

    public Map getWorkflowParams() {
        return this.workflowParams;
    }

    public void setWorkflowParams(Map m) {
        this.workflowParams = m;
    }

    /**
     * Create and return a new instance.
     */
    public static Portal createPortal() {
        Portal portal = new Portal();

        portal.setColumns(1);
        portal.setDialog(false);

        return portal;
    }

    /**
     * Create and return a new named instance with a portlet in the first column.
     *
     * @param portalName  the portal name
     * @param portletName the portlet definition name
     */
    public static Portal createPortal(String portalName, String portletName) {
        Portal portal = createPortal();
        portal.setName(portalName);

        List definitions = new ArrayList();
        definitions.add(portletName);
        portal.addPortlets(definitions);

        return portal;
    }

    public void addPortletsFromString(String stringList, int column) {
        //convert string to portlets then call addPortlets().

        List StringColumn = StringUtil.explode(stringList, "|");

        for (int i = 0; i < StringColumn.size(); i++) {
            String tile = (String) StringColumn.get(i);
            Portlet portlet = new Portlet(tile);
            if (i == 0) {
                portlet.setIsFirst();
            }

            if (i == (StringColumn.size() - 1)) {
                portlet.setIsLast();
            }

            addPortlet(portlet, column);
        }
    }

    /**
     * Participate in workflow in the following circumstances: dialog isWorkflowPortal participate 0 0 1 0 1 0 1 0 0 1 1
     * 1
     */
    public boolean doWorkflow() {
        if (!(isDialog() || isWorkflowPortal()) || (isDialog() && isWorkflowPortal())) {
            return true;
        } else {
            return false;
        }
    }
}