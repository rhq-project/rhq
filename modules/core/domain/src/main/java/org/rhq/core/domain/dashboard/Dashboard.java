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

/**
 * @author Greg Hinkle
 */
@Entity
@SequenceGenerator(name = "RHQ_DASHBOARD_ID_SEQ", sequenceName = "RHQ_DASHBOARD_ID_SEQ")
@Table(name = "RHQ_DASHBOARD")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class Dashboard implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_DASHBOARD_ID_SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "SHARED", nullable = false)
    private boolean shared = false;

    @JoinColumn(name = "CONFIGURATION_ID", referencedColumnName = "ID")
    @OneToOne(cascade = { CascadeType.ALL })
    private Configuration configuration = new Configuration();

    @JoinColumn(name = "SUBJECT_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private Subject owner;

    @OneToMany(mappedBy = "dashboard", fetch = FetchType.EAGER)
    @Cascade( { org.hibernate.annotations.CascadeType.PERSIST, org.hibernate.annotations.CascadeType.MERGE,
        org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
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

    public int getColumns() {
        return configuration.getSimple(CFG_COLUMNS).getIntegerValue();
    }

    public void setColumns(int columns) {
        configuration.put(new PropertySimple(CFG_COLUMNS, columns));
    }

    public String[] getColumnWidths() {
        return configuration.getSimple(CFG_WIDTHS).getStringValue().split(",");
    }

    public void setColumnWidths(String... columnWidths) {
        configuration.put(new PropertySimple(CFG_WIDTHS, columnWidths));
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

        List<DashboardPortlet> columnPortlets = new ArrayList<DashboardPortlet>();
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
     * @param storedPortlet, MODIFIED with assigned column, index
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
     * @param storedPortlet, MODIFIED with assigned column, index
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
