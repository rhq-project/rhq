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
import java.util.Collection;
import java.util.List;

/**
 * Accumulates all the synced advisories into a collection
 * @author Pradeep Kilambi
 */
public class AdvisorySyncReport {
    private List<AdvisoryDetails> advs = new ArrayList<AdvisoryDetails>();
    private List<AdvisoryDetails> deletedadvs = new ArrayList<AdvisoryDetails>();
    private int repoId;

    public int getRepoId() {
        return repoId;
    }

    public void setRepoId(int repoIdIn) {
        this.repoId = repoIdIn;
    }

    public AdvisorySyncReport(int repoIdIn) {
        repoId = repoIdIn;

    }

    public List<AdvisoryDetails> getAdvisory() {
        return advs;
    }

    public void addAdvisory(AdvisoryDetails advIn) {
        advs.add(advIn);
    }

    public void addAdvisorys(Collection<AdvisoryDetails> advsIn) {
        advs.addAll(advsIn);
    }

    public List<AdvisoryDetails> getDeletedAdvisorys() {
        return deletedadvs;
    }

    public void addDeletedAdvisory(AdvisoryDetails advsIn) {
        deletedadvs.add(advsIn);
    }

    public void addDeletedAdvisorys(Collection<AdvisoryDetails> advsIn) {
        deletedadvs.addAll(advsIn);
    }
}
