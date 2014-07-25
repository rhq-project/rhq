/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.core.domain.dashboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.annotations.Cascade;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.group.ResourceGroup;

/**
 * @author Greg Hinkle
 * @author Jay Shaughnessy
 */
@Entity
@SequenceGenerator(allocationSize = org.rhq.core.domain.util.Constants.ALLOCATION_SIZE, name = "RHQ_DASHBOARD_ID_SEQ", sequenceName = "RHQ_DASHBOARD_ID_SEQ")
@Table(name = "RHQ_DASHBOARD")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Dashboard implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_DASHBOARD_ID_SEQ")
    @Id
    private int id = 0;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "CATEGORY", nullable = false)
    @Enumerated(EnumType.STRING)
    private DashboardCategory category;

    // currently unused
    @Column(name = "SHARED", nullable = false)
    private boolean shared = false;

    @JoinColumn(name = "CONFIGURATION_ID", referencedColumnName = "ID", nullable = true)
    @OneToOne(cascade = { CascadeType.ALL }, optional = true)
    private Configuration configuration = new Configuration();

    @JoinColumn(name = "SUBJECT_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Subject owner;

    @JoinColumn(name = "RESOURCE_ID", nullable = true)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    private Resource resource;

    @JoinColumn(name = "GROUP_ID", nullable = true)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    private ResourceGroup group;

    @OneToMany(mappedBy = "dashboard", fetch = FetchType.EAGER, orphanRemoval = true)
    @Cascade({ org.hibernate.annotations.CascadeType.PERSIST, org.hibernate.annotations.CascadeType.MERGE })
    private Set<DashboardPortlet> portlets = new HashSet<DashboardPortlet>();

    public static final String CFG_COLUMNS = "columns";
    public static final String CFG_WIDTHS = "widths";
    public static final String CFG_BACKGROUND = "background";
    public static final String CFG_REFRESH_RATE = "refresh";

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isShared() {
        return shared;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
    }

    public Subject getOwner() {
        return owner;
    }

    public void setOwner(Subject owner) {
        this.owner = owner;
    }

    public DashboardCategory getCategory() {
        return category;
    }

    public void setCategory(DashboardCategory category) {
        this.category = category;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public ResourceGroup getGroup() {
        return group;
    }

    public void setGroup(ResourceGroup group) {
        this.group = group;
    }

    public int getColumns() {
        return Integer.valueOf(configuration.getSimpleValue(CFG_COLUMNS, "1"));
    }

    public void setColumns(int columns) {
        configuration.put(new PropertySimple(CFG_COLUMNS, columns));
    }

    public String[] getColumnWidths() {
        return configuration.getSimpleValue(CFG_WIDTHS, "").split(",");
    }

    public void setColumnWidths(String... columnWidths) {
        if (null == columnWidths || columnWidths.length == 0) {
            return;
        }

        // note - this impl is a little verbose but it avoids a smartgwt javascript problem with
        // varargs handling. Not sure how bad the problem is but it definitely occured with SmartGwt 2.4
        // when a single String was pssed as the varArg list.  Be wary of directly using varargs array
        // element 0...
        StringBuilder sb = new StringBuilder();
        sb.append(columnWidths[0]);
        for (int i = 1; i < columnWidths.length; ++i) {
            sb.append(",");
            sb.append(columnWidths[i]);
        }
        configuration.put(new PropertySimple(CFG_WIDTHS, sb));
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Set<DashboardPortlet> getPortlets() {
        return portlets;
    }

    public List<DashboardPortlet> getPortlets(int column) {

        ArrayList<DashboardPortlet> columnPortlets = new ArrayList<DashboardPortlet>();
        for (DashboardPortlet p : this.portlets) {
            if (p.getColumn() == column) {
                columnPortlets.add(p);
            }
        }

        Collections.sort(columnPortlets, new Comparator<DashboardPortlet>() {
            public int compare(DashboardPortlet o1, DashboardPortlet o2) {
                return (o1.getIndex() < o2.getIndex() ? -1 : (o1.getIndex() == o2.getIndex() ? 0 : 1));
            }
        });

        return columnPortlets;
    }

    public boolean removePortlet(DashboardPortlet storedPortlet) {
        if (!portlets.contains(storedPortlet)) {
            return false;
        }

        // lower the index by 1 for  portlets in the same column, below the one being removed
        int col = storedPortlet.getColumn();
        int index = storedPortlet.getIndex();

        portlets.remove(storedPortlet);

        for (Iterator<DashboardPortlet> i = portlets.iterator(); i.hasNext();) {
            DashboardPortlet next = i.next();
            if (col == next.getColumn() && index < next.getIndex()) {
                next.setIndex(next.getIndex() - 1);
            }
        }

        return true;
    }

    /**
     * This can be used to safely add a portlet without knowing the current portlet positioning on the
     * Dashboard. It adds the portlet to the bottom of column with the least portlets.
     *
     * @param storedPortlet  MODIFIED with assigned column, index
     */
    public void addPortlet(DashboardPortlet storedPortlet) {
        int[] columnCounts = new int[getColumns()];
        Arrays.fill(columnCounts, 0);
        // set column counts
        for (DashboardPortlet dashboardPortlet : portlets) {
            ++(columnCounts[dashboardPortlet.getColumn()]);
        }
        // get best column
        int bestColumn = -1;
        int minPortlets = Integer.MAX_VALUE;
        for (int column = 0; (column < columnCounts.length); ++column) {
            if (columnCounts[column] < minPortlets) {
                bestColumn = column;
                minPortlets = columnCounts[column];
            }
        }
        // addPortlet to best Column
        addPortlet(storedPortlet, bestColumn, minPortlets);
    }

    /**
     * Call this only if you are sure the column and index are valid, not already used and not leaving gaps.
     *
     * @param storedPortlet MODIFIED with assigned column, index
     * @param column
     * @param index
     */
    public void addPortlet(DashboardPortlet storedPortlet, int column, int index) {
        storedPortlet.setColumn(column);
        storedPortlet.setIndex(index);
        storedPortlet.setDashboard(this);
        portlets.add(storedPortlet);
    }

    public Dashboard deepCopy(boolean keepIds) {

        Dashboard newDashboard = new Dashboard();
        if (keepIds) {
            newDashboard.id = this.id;
        }
        newDashboard.name = this.name;
        newDashboard.owner = this.owner;
        newDashboard.shared = this.shared;
        newDashboard.configuration = this.configuration.deepCopy(keepIds);

        for (DashboardPortlet portlet : portlets) {
            DashboardPortlet newPortlet = portlet.deepCopy(keepIds);
            newPortlet.setDashboard(this);
            newDashboard.addPortlet(newPortlet, newPortlet.getColumn(), newPortlet.getIndex());
        }
        return newDashboard;
    }

}
