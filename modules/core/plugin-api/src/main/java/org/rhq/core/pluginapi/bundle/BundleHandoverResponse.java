/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.core.pluginapi.bundle;

/**
 * A response object returned by plugin component classes implementing
 * {@link BundleHandoverFacet#handleContent(BundleHandoverRequest)}.
 *
 * @author Thomas Segismont
 * @see org.rhq.core.pluginapi.bundle.BundleHandoverFacet
 */
public class BundleHandoverResponse {

    public enum FailureType {
        /**
         * Bundle target resource component does not know the requested action.
         */
        INVALID_ACTION,
        /**
         * A parameter required to execute the action is missing.
         */
        MISSING_PARAMETER,
        /**
         * A parameter value is invalid (too long, not a number, ... etc).
         */
        INVALID_PARAMETER,
        /**
         * A problem occured at the plugin container level.
         */
        PLUGIN_CONTAINER,
        /**
         * A problem occured during execution inside the bundle target resource component.
         */
        EXECUTION
    }

    private final boolean success;
    private final String message;
    private final Throwable throwable;
    private final FailureType failureType;

    private BundleHandoverResponse(boolean success, String message, Throwable throwable, FailureType failureType) {
        this.success = success;
        this.message = message;
        this.throwable = throwable;
        this.failureType = failureType;
    }

    public static BundleHandoverResponse success() {
        return success(null);
    }

    public static BundleHandoverResponse success(String message) {
        return new BundleHandoverResponse(true, message, null, null);
    }

    /**
     * @throws java.lang.IllegalArgumentException if <code>failureType</code> is null
     */
    public static BundleHandoverResponse failure(FailureType failureType) {
        return failure(failureType, null, null);
    }

    /**
     * @throws java.lang.IllegalArgumentException if <code>failureType</code> is null
     */
    public static BundleHandoverResponse failure(FailureType failureType, String message) {
        return failure(failureType, message, null);
    }

    /**
     * @throws java.lang.IllegalArgumentException if <code>failureType</code> is null
     */
    public static BundleHandoverResponse failure(FailureType failureType, String message, Throwable throwable) {
        if (failureType == null) {
            throw new IllegalArgumentException("failureType is null");
        }
        return new BundleHandoverResponse(false, message, throwable, failureType);
    }

    /**
     * @return true if "handover" finished successfully, false otherwise
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return a message supplied by the resource component, null by default
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the throwable caught by the resource component, null by default
     */
    public Throwable getThrowable() {
        return throwable;
    }

    /**
     * @return the type of failure if {@link #isSuccess()} returns false, null otherwise
     */
    public FailureType getFailureType() {
        return failureType;
    }
}
