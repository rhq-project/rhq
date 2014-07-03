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
 * Bundle "handover" context class.
 *
 * The plugin container creates an instance of this class when a bundle recipe requires a content handover to the
 * plugin component instance.
 *
 * @author Thomas Segismont
 * @see BundleHandoverFacet#handleContent(BundleHandoverRequest)
 */
public final class BundleHandoverContext {
    private final boolean revert;

    private BundleHandoverContext(boolean revert) {
        this.revert = revert;
    }

    /**
     * @return true if the bundle is being reverted, false otherwise
     */
    public boolean isRevert() {
        return revert;
    }

    @Override
    public String toString() {
        return "BundleHandoverContext[" + "revert=" + revert + ']';
    }

    public static class Builder {
        private boolean revert;

        /**
         * @param revert flag indicating if the bundle deployment is reverting a version
         */
        public Builder setRevert(boolean revert) {
            this.revert = revert;
            return this;
        }

        public BundleHandoverContext create() {
            return new BundleHandoverContext(revert);
        }
    }
}
