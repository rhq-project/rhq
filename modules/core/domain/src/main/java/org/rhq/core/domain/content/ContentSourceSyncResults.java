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
 * Represents the results of a content source sync request. When a {@link ContentSource} synchronizes with its remote
 * backend, the results of that are stored in this object.
 *
 * @author John Mazzitelli
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = ContentSourceSyncResults.QUERY_GET_INPROGRESS_BY_CONTENT_SOURCE_ID, query = "SELECT cssr "
        + "  FROM ContentSourceSyncResults cssr " + " WHERE cssr.contentSource.id = :contentSourceId "
        + "   AND status = 'INPROGRESS' " + " ORDER BY cssr.startTime DESC "),
    @NamedQuery(name = ContentSourceSyncResults.QUERY_GET_ALL_BY_CONTENT_SOURCE_ID, query = "SELECT cssr "
        + "  FROM ContentSourceSyncResults cssr " + " WHERE cssr.contentSource.id = :contentSourceId ") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_CONTENT_SRC_SYNC_ID_SEQ")
@Table(name = "RHQ_CONTENT_SRC_SYNC")
public class ContentSourceSyncResults implements Serializable {
    public static final String QUERY_GET_INPROGRESS_BY_CONTENT_SOURCE_ID = "ContentSourceSyncResults.getInProgressByCSId";
    public static final String QUERY_GET_ALL_BY_CONTENT_SOURCE_ID = "ContentSourceSyncResults.getAllByCSId";

    private static final long serialVersionUID = 1L;

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ")
    @Id
    private int id;

    @JoinColumn(name = "CONTENT_SRC_ID", referencedColumnName = "ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY)
    private ContentSource contentSource;

    @Column(name = "STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private ContentSourceSyncStatus status;

    @Column(name = "START_TIME", nullable = false)
    private long startTime;

    @Column(name = "END_TIME", nullable = true)
    private Long endTime;

    @Column(name = "RESULTS", nullable = true)
    private String results;

    protected ContentSourceSyncResults() {
        this.startTime = System.currentTimeMillis();
        this.status = ContentSourceSyncStatus.INPROGRESS;
    }

    public ContentSourceSyncResults(ContentSource contentSource) {
        this();
        this.contentSource = contentSource;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * The content source that performed the sync.
     */
    public ContentSource getContentSource() {
        return contentSource;
    }

    public void setContentSource(ContentSource contentSource) {
        this.contentSource = contentSource;
    }

    /**
     * The status that typically indicates if the sync request succeeded, failed or is currently in progress.
     */
    public ContentSourceSyncStatus getStatus() {
        return status;
    }

    public void setStatus(ContentSourceSyncStatus status) {
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

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder("ContentSourceSyncResults: ");
        str.append("start-time=[" + new Date(startTime));
        str.append("], end-time=[" + ((endTime != null) ? new Date(endTime) : "---"));
        str.append("], status=[" + status);
        str.append("], content-source=[" + contentSource);
        str.append("]");
        return str.toString();
    }
}