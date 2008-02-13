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
package org.rhq.core.domain.content.transfer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import org.rhq.core.domain.content.PackageDetailsKey;

/**
 * @author Jason Dobies
 */
public class RemoveIndividualPackageResponse implements Serializable {
    // Constants  --------------------------------------------

    private static final long serialVersionUID = 1L;

    // Attributes  --------------------------------------------

    private PackageDetailsKey key;

    private ContentResponseResult result;
    private String errorMessage;

    // Constructors  --------------------------------------------

    public RemoveIndividualPackageResponse(PackageDetailsKey key, ContentResponseResult result) {
        if (key == null) {
            throw new IllegalArgumentException("key cannot be null");
        }

        if (result == null) {
            throw new IllegalArgumentException("result cannot be null");
        }

        this.key = key;
        this.result = result;
    }

    // Public  --------------------------------------------

    public PackageDetailsKey getKey() {
        return key;
    }

    public ContentResponseResult getResult() {
        return result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Convienence method that sets the error message to the given throwable's stack trace dump. If the given throwable
     * is <code>null</code>, the error message will be set to <code>null</code> as if passing <code>null</code> to
     * {@link #setErrorMessage(String)}.
     *
     * @param t throwable whose message and stack trace will make up the error message (may be <code>null</code>)
     */
    public void setErrorMessageFromThrowable(Throwable t) {
        if (t != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            t.printStackTrace(new PrintStream(baos));
            setErrorMessage(baos.toString());
        } else {
            setErrorMessage(null);
        }
    }
}