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
package org.rhq.core.domain.search;

import java.io.Serializable;

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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.auth.Subject;

/**
 * The data model for saved searches.  Each users has his or her own set of
 * saved searches, but inventory managers are allowed to "promote" their 
 * saved searches to GLOBAL status, which makes them available to all users
 * under that installation of RHQ.
 * 
 * There are two levels of pre-computed data within this structured.  After
 * the {@link SavedSearch} is created, the pattern will be translated into its 
 * JPQL equivalent and stored.  If the {@link SavedSearch} is ever modified
 * and saved, the JPQL will be re-computed.
 * 
 * Periodically, the count-query version of the {@link SavedSearch} will be
 * executed (using the stored JPQL), and the number of matching records will
 * be stored. If this {@link SavedSearch} ever needs to be displayed on the 
 * user interface, it will by default retrieve this cached result count.
 * 
 * @author Joseph Marques
 */
@Entity
@SequenceGenerator(name = "id", sequenceName = "RHQ_SAVED_SEARCH_ID_SEQ")
@Table(name = "RHQ_SAVED_SEARCH")
public class SavedSearch implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "id")
    private Integer id;

    @Column(name = "CONTEXT", nullable = false)
    @Enumerated(EnumType.STRING)
    private SearchSubsystem searchSubsystem;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "PATTERN", nullable = false)
    private String pattern;

    @Column(name = "LAST_COMPUTE_TIME", nullable = false)
    private long lastComputeTime;

    @Column(name = "RESULT_COUNT")
    private Long resultCount;

    @JoinColumn(name = "SUBJECT_ID", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Subject subject;

    @Column(name = "SUBJECT_ID", insertable = false, updatable = false)
    private int subjectId;

    @Column(name = "GLOBAL", nullable = false)
    private boolean global;

    protected SavedSearch() {
        // no-arg ctor for Hibernate
    }

    public SavedSearch(SearchSubsystem context, String name, String pattern, Subject subject) {
        // call setters to go through parameter validation
        setSearchSubsystem(context);
        setPattern(pattern);
        setSubject(subject);
        setName(name); // name can be null, to allow for saving searches quickly

        this.description = null;
        this.lastComputeTime = 0; // further imply that computation needs to occur
        this.resultCount = null; // NULL resultCount implies either computation failed or hasn't begun yet
        this.global = false; // user must promote saved search to be a global after creation
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public SearchSubsystem getSearchSubsystem() {
        return searchSubsystem;
    }

    private void setSearchSubsystem(SearchSubsystem searchSubsystem) {
        if (searchSubsystem == null) {
            throw new IllegalArgumentException("All saved searches must be bound to a SearchSubsystem");
        }
        this.searchSubsystem = searchSubsystem;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        if (pattern == null || pattern.trim().equals("")) {
            throw new IllegalArgumentException("All saved searches must have a non-empty pattern");
        }
        this.pattern = pattern;
    }

    public long getLastComputeTime() {
        return lastComputeTime;
    }

    public void setLastComputeTime(long lastComputeTime) {
        this.lastComputeTime = lastComputeTime;
    }

    public Long getResultCount() {
        return resultCount;
    }

    public void setResultCount(Long resultCount) {
        this.resultCount = resultCount;
    }

    public Subject getSubject() {
        return subject;
    }

    private void setSubject(Subject subject) {
        if (subject == null) {
            throw new IllegalArgumentException("All saved searches must be owned by a specific user");
        }
        this.subject = subject;
        this.subjectId = subject.getId();
    }

    public int getSubjectId() {
        return subjectId;
    }

    public boolean isGlobal() {
        return global;
    }

    public void setGlobal(boolean global) {
        this.global = global;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + subjectId;
        result = (prime * result) + searchSubsystem.hashCode();
        result = (prime * result) + ((name == null) ? 0 : name.hashCode());
        result = (prime * result) + pattern.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof SavedSearch))) {
            return false;
        }

        final SavedSearch other = (SavedSearch) obj;

        if (subjectId != other.subjectId) {
            return false;
        }

        if (searchSubsystem != other.searchSubsystem) {
            return false;
        }

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        if (!pattern.equals(other.pattern)) {
            return false;
        }

        if (lastComputeTime != other.lastComputeTime) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "SavedSearch [" //
            + "id=" + id //
            + ", searchSubsystem=" + searchSubsystem //
            + ", description=" + description //
            + ", global=" + global //
            + ", lastComputeTime=" + lastComputeTime //
            + ", name=" + name //
            + ", pattern=" + pattern //
            + ", resultCount=" + resultCount //
            + ", subjectId=" + subjectId + "]";
    }

}
