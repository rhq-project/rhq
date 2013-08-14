/*
 *
 *  RHQ Management Platform
 *  Copyright (C) 2005-2013 Red Hat, Inc.
 *  All rights reserved.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, version 2, as
 *  published by the Free Software Foundation, and/or the GNU Lesser
 *  General Public License, version 2.1, also as published by the Free
 *  Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License and the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  and the GNU Lesser General Public License along with this program;
 *  if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.cassandra.schema.exception;


/**
 * @author Stefan Negrea
 */
@SuppressWarnings("serial")
public class InstalledSchemaTooOldException extends Exception {

    public InstalledSchemaTooOldException() {
        super(
            "Storage schema is not properly updated. The schema manager contains updates not yet applied to the storage cluster installation."
                + "Please re-run the server installer to update the current schema to the latest version.");
    }
}