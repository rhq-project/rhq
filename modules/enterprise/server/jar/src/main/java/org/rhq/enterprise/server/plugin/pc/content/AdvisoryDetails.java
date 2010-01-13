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

package org.rhq.enterprise.server.plugin.pc.content;

import java.util.ArrayList;
import java.util.List;

public class AdvisoryDetails {
    private String advisory;
    private String advisory_type;

    private String advisory_rel;
    private String advisory_name;
    private String description;
    private String synopsis;
    private String topic;
    private String solution;
    private long issue_date;
    private long update_date;
    private List<AdvisoryPackageDetails> pkgs;
    private List<AdvisoryCVEDetails> cves;
    private List<AdvisoryBugDetails> bugs;

    public AdvisoryDetails(String advisory, String advisory_type, String synopsis) {
        setAdvisory(advisory);
        setAdvisory_type(advisory_type);
        setSynopsis(synopsis);
        pkgs = new ArrayList<AdvisoryPackageDetails>();
        cves = new ArrayList<AdvisoryCVEDetails>();
        bugs = new ArrayList<AdvisoryBugDetails>();
    }

    public String getAdvisory() {
        return advisory;
    }

    public void setAdvisory(String advisory) {
        this.advisory = advisory;
    }

    public String getAdvisory_type() {
        return advisory_type;
    }

    public void setAdvisory_type(String advisoryType) {
        advisory_type = advisoryType;
    }

    public String getAdvisory_rel() {
        return advisory_rel;
    }

    public void setAdvisory_rel(String advisoryRel) {
        advisory_rel = advisoryRel;
    }

    public String getAdvisory_name() {
        return advisory_name;
    }

    public void setAdvisory_name(String advisoryName) {
        advisory_name = advisoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public long getIssue_date() {
        return issue_date;
    }

    public void setIssue_date(long issueDate) {
        issue_date = issueDate;
    }

    public long getUpdate_date() {
        return update_date;
    }

    public void setUpdate_date(long updateDate) {
        update_date = updateDate;
    }

    public void addPkg(AdvisoryPackageDetails pkg) {
        pkgs.add(pkg);
    }

    public void addPkgs(List<AdvisoryPackageDetails> pkgsIn) {
        pkgs.addAll(pkgsIn);
    }

    public List<AdvisoryPackageDetails> getPkgs() {
        return pkgs;
    }

    public void setPkgs(List<AdvisoryPackageDetails> pkgsIn) {
        pkgs = pkgsIn;
    }

    public void addCVE(AdvisoryCVEDetails cve) {
        cves.add(cve);
    }

    public void addCVEs(List<AdvisoryCVEDetails> cvesIn) {
        cves.addAll(cvesIn);
    }

    public List<AdvisoryCVEDetails> getCVEs() {
        return cves;
    }

    public void setCVEs(List<AdvisoryCVEDetails> cvesIn) {
        cves = cvesIn;
    }

    public void addBug(AdvisoryBugDetails bug) {
        bugs.add(bug);
    }

    public void addBugs(List<AdvisoryBugDetails> bugsIn) {
        bugs.addAll(bugsIn);
    }

    public List<AdvisoryBugDetails> getBugs() {
        return bugs;
    }

    public void setBugs(List<AdvisoryBugDetails> bugsIn) {
        bugs = bugsIn;
    }

    public String toString() {
        return "AdvisoryDetails = " + advisory + ", type = " + advisory_type + ", synopsis = " + synopsis;
    }

}
