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
package org.rhq.core.domain.content;

import java.io.Serializable;
import java.util.Date;

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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Represents the results of a repo sync request. When a {@link Repo} synchronizes with its remote
 * backend, the results of that are stored in this object.
 *
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = RepoSyncResults.QUERY_GET_INPROGRESS_BY_REPO_ID, query = "SELECT rssr "
        + "  FROM RepoSyncResults rssr " + " WHERE rssr.repo.id = :repoId " + "   AND status = 'INPROGRESS' "
        + " ORDER BY rssr.startTime DESC "),
    @NamedQuery(name = RepoSyncResults.QUERY_GET_ALL_BY_REPO_ID, query = "SELECT rssr "
        + "  FROM RepoSyncResults rssr " + " WHERE rssr.repo.id = :repoId ") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_REPO_SYNC_ID_SEQ")
@Table(name = "RHQ_REPO_SYNC")
public class RepoSyncResults implements Serializable, ContentSyncResults {
    public static final String QUERY_GET_INPROGRESS_BY_REPO_ID = "RepoSyncResults.getInProgressByRepoId";
    public static final String QUERY_GET_ALL_BY_REPO_ID = "RepoSyncResults.getAllByRepoId";

    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQ")
    @Id
    private int id;

    @JoinColumn(name = "REPO_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Repo repo;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private ContentSyncStatus status;

    @Column(name = "START_TIME", nullable = false)
    private long startTime;

    @Column(name = "END_TIME", nullable = true)
    private Long endTime;

    @Column(name = "RESULTS", nullable = true)
    private String results;

    @Column(name = "PERCENT_COMPLETE", nullable = true)
    private Long percentComplete;

    protected RepoSyncResults() {
        this.startTime = System.currentTimeMillis();
        this.status = ContentSyncStatus.INPROGRESS;
    }

    public RepoSyncResults(Repo repoIn) {
        this();
        this.repo = repoIn;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * The Repo that performed the sync.
     */
    public Repo getRepo() {
        return repo;
    }

    public void setRepo(Repo repoIn) {
        this.repo = repoIn;
    }

    /**
     * The status that typically indicates if the sync request succeeded, failed or is currently in progress.
     */
    public ContentSyncStatus getStatus() {
        return status;
    }

    public void setStatus(ContentSyncStatus status) {
        this.status = status;
    }

    /**
     * Indicates when the sync began.
     */
    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    /**
     * Indicates when the sync stopped.
     */
    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    /**
     * Tells you the results of the sync.
     */
    public String getResults() {
        return results;
    }

    public void setResults(String results) {
        this.results = results;
    }

    public Long getPercentComplete() {
        return percentComplete;
    }

    public void setPercentComplete(Long percentComplete) {
        this.percentComplete = percentComplete;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(this.getClass().getName().substring(
            this.getClass().getName().lastIndexOf(".") + 1)
            + ": ");
        str.append("start-time=[" + new Date(startTime));
        str.append("], end-time=[" + ((endTime != null) ? new Date(endTime) : "---"));
        str.append("], percentComplete=[" + status);
        str.append("], status=[" + percentComplete);
        str.append("], repo=[" + repo);
        str.append("]");
        return str.toString();
    }

    /**
     * Convienence method to append a string to the results of this RepoSyncResults.
     * @param msg to append
     */
    public void appendResults(String msg) {
        StringBuffer existing = new StringBuffer(this.getResults());
        existing.append("\n");
        existing.append(msg);
        this.setResults(existing.toString());
    }
}