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
package org.rhq.core.domain.content.composite;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.rhq.core.domain.content.Advisory;
import org.rhq.core.domain.content.AdvisoryBuglist;
import org.rhq.core.domain.content.AdvisoryPackage;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.util.StringUtils;

/**
 *
 * @author Pradeep Kilambi
 *
 */
public class AdvisoryDetailsComposite implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Advisory advisory;
    private final String advisoryType;
    private final String advisoryName;
    private final String topic;
    private final String synopsis;
    private final String description;
    private final String solution;
    private final String severity;
    private final Date updateDate;
    private final Date issueDate;
    private final List<PackageVersion> pkgs;
    private String bugid;

    public AdvisoryDetailsComposite(Advisory advisory, String advisoryName, String advisoryType, String topic,
        String synopsis, String description, String solution, String severity, Long updateDate, Long issueDate) {
        this.advisory = advisory;
        this.advisoryName = advisoryName;
        this.advisoryType = advisoryType;
        this.topic = topic;
        this.synopsis = synopsis;
        this.description = description;
        this.solution = solution;
        this.severity = severity;
        this.updateDate = new Date(updateDate.longValue() * 1000L);
        this.issueDate = new Date(issueDate.longValue() * 1000L);
        this.pkgs = getAdvisoryPackages();
        this.bugid = getBugid();

    }

    private List<PackageVersion> getAdvisoryPackages() {
        List<PackageVersion> pkges = new ArrayList<PackageVersion>();
        Set<AdvisoryPackage> apkgs = advisory.getAdvisorypkgs();
        for (AdvisoryPackage apkg : apkgs) {
            pkges.add(apkg.getPkg());
        }
        return pkges;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public Advisory getAdvisory() {
        return advisory;
    }

    public String getAdvisoryType() {
        return advisoryType;
    }

    public String getAdvisoryName() {
        return advisoryName;
    }

    public String getTopic() {
        return topic;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public String getDescription() {
        return description;
    }

    public String getSolution() {
        return solution;
    }

    public String getSeverity() {
        return severity;
    }

    public List<PackageVersion> getPkgs() {
        return pkgs;
    }

    public String getBugid() {
        List<String> bugs = new ArrayList<String>();
        Set<AdvisoryBuglist> abugs = advisory.getAdvisorybugs();
        for (AdvisoryBuglist abug : abugs) {
            bugs.add(abug.getBugid());
        }
        return StringUtils.getListAsString(bugs, " ");
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public Date getIssueDate() {
        return issueDate;
    }

}
