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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.rhq.core.domain.configuration.Configuration;

/**
 * @author Greg Hinkle
 */
@Entity
@SequenceGenerator(name = "RHQ_DASHBOARD_PORTLET_ID_SEQ", sequenceName = "RHQ_DASHBOARD_PORTLET_ID_SEQ")
@Table(name = "RHQ_DASHBOARD_PORTLET")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement
public class DashboardPortlet implements Serializable {

    private static final long serialVersionUID = 1L;

    // This is the only unique key. dashboard+portletKey+name does not have to be unique
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "RHQ_DASHBOARD_PORTLET_ID_SEQ")
    @Id
    private int id;

    // A non-displayed, persisted identifier for the portlet.
    @Column(name = "PORTLET_KEY")
    private String portletKey;

    // A displayed, persisted, editable name for the portlet.
    @Column(name = "NAME")
    private String name;

    @Column(name = "COL")
    private int column;

    @Column(name = "COL_INDEX")
    private int index;

    @Column(name = "HEIGHT")
    private int height = 300;

    @JoinColumn(name = "CONFIGURATION_ID", referencedColumnName = "ID")
    @OneToOne(cascade = { CascadeType.ALL })
    private Configuration configuration = new Configuration();

    @ManyToOne(optional = false)
    @JoinColumn(name = "DASHBOARD_ID", nullable = false)
    private Dashboard dashboard;

    public DashboardPortlet() {
    }

    public DashboardPortlet(String name, String portletKey, int height) {
        this.name = name;
        this.portletKey = portletKey;
        this.height = height;
    }

    public int getId() {
        return id;
    }

    public String getPortletKey() {
        return portletKey;
    }

    public String getName() {
        return name;
    }

    public int getColumn() {
        return column;
    }

    public int getIndex() {
        return index;
    }

    public int getHeight() {
        return height;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setPortletKey(String portletKey) {
        this.portletKey = portletKey;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public DashboardPortlet deepCopy(boolean keepIds) {
        DashboardPortlet newPortlet = new DashboardPortlet();
        if (keepIds) {
            newPortlet.id = this.id;
        }
        newPortlet.name = this.name;
        newPortlet.portletKey = this.portletKey;
        newPortlet.column = this.column;
        newPortlet.index = this.index;
        newPortlet.height = this.height;
        newPortlet.configuration = this.configuration != null ? this.configuration.deepCopy(keepIds) : null;
        return newPortlet;
    }

    public String toString() {
        return "DashboardPortlet[id=" + id + ",key=" + portletKey + ",name=" + name + "]";
    }
}
