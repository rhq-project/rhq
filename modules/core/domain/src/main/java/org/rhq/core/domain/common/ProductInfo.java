/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.core.domain.common;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Product (i.e. RHQ or JON) information.
 *
 * @author Ian Springer
 */
public class ProductInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String shortName;
    private String name;
    private String fullName;
    private String url;
    private String urlDomain;
    private String salesEmail;
    private String supportEmail;
    private String version;
    private String buildNumber;
    private String supportedAgentBuilds;
    private String helpDocRoot;
    private HashMap<String, String> helpViewContent;

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrlDomain() {
        return urlDomain;
    }

    public void setUrlDomain(String urlDomain) {
        this.urlDomain = urlDomain;
    }

    public String getSalesEmail() {
        return salesEmail;
    }

    public void setSalesEmail(String salesEmail) {
        this.salesEmail = salesEmail;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public String getSupportedAgentBuilds() {
        return supportedAgentBuilds;
    }

    public void setSupportedAgentBuilds(String supportedAgentBuilds) {
        this.supportedAgentBuilds = supportedAgentBuilds;
    }

    public String getHelpDocRoot() {
        return helpDocRoot;
    }

    public void setHelpDocRoot(String helpDocRoot) {
        this.helpDocRoot = helpDocRoot;
    }

    public HashMap<String, String> getHelpViewContent() {
        return helpViewContent;
    }

    public void setHelpViewContent(HashMap<String, String> helpViewContent) {
        this.helpViewContent = helpViewContent;
    }
}
