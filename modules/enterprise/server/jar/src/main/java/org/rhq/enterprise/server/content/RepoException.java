/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.server.content;

/**
 * General exception used to convey issues in CRUD operations on a repo or repo group.
 *
 * @see org.rhq.core.domain.content.Repo
 * @see org.rhq.core.domain.content.RepoGroup
 */
public class RepoException extends ContentException {

    private static final long serialVersionUID = 1L;

    private RepoExceptionType type = RepoExceptionType.GENERAL;

    // Default no-arg constructor required by JAXB
    public RepoException() {
    }

    public RepoException(String message) {
        super(message);
    }

    public RepoException(Throwable cause) {
        super(cause);
    }

    public RepoException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Programmatic hook to determine if a repo operation failure was related to the repo name
     * already existing.
     *
     * @return
     */
    public RepoExceptionType getType() {
        return type;
    }

    public void setType(RepoExceptionType type) {
        this.type = type;
    }

    public enum RepoExceptionType {
        GENERAL, NAME_ALREADY_EXISTS
    }
}
