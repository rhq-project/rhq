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
package org.rhq.core.domain.content;

/**
 * Determines how a content source's package bits are downloaded.
 */
public enum DownloadMode {
    /**
     * Never download package bits - in essential, the content source adapter
     * is a simple pass-through, where all requests pull down content
     * directly from the remote repository.
     */
    NEVER,

    /**
     * The package bits will be stored in the database.
     */
    DATABASE,

    /**
     * The package bits will be stored in the file system.
     * Where on the file system is to be determined by some other means.
     */
    FILESYSTEM
}