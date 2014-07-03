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

import java.io.InputStream;
import java.util.Map;

/**
 * A request object consumed by plugin component classes implementing
 * {@link BundleHandoverFacet#handleContent(BundleHandoverRequest)}.
 *
 * @author Thomas Segismont
 * @see org.rhq.core.pluginapi.bundle.BundleHandoverFacet
 */
public class BundleHandoverRequest {
    private final InputStream content;
    private final String filename;
    private final String action;
    private final Map<String, String> params;
    private final BundleHandoverContext context;

    private BundleHandoverRequest(InputStream content, String filename, String action, Map<String, String> params,
        BundleHandoverContext context) {
        this.content = content;
        this.filename = filename;
        this.action = action;
        this.params = params;
        this.context = context;
    }

    /**
     * @return an {@link java.io.InputStream} for reading the content handed over via the
     * {@link org.rhq.core.pluginapi.bundle.BundleHandoverFacet}
     */
    public InputStream getContent() {
        return content;
    }

    /**
     * @return name of the content
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @return the action required by the bundle recipe
     */
    public String getAction() {
        return action;
    }

    /**
     * @return the collection of named parameters supplied by the bundle recipe
     */
    public Map<String, String> getParams() {
        return params;
    }

    /**
     * @return the handover context
     */
    public BundleHandoverContext getContext() {
        return context;
    }

    @Override
    public String toString() {
        return "BundleHandoverRequest[" + "filename='" + filename + '\'' + ", action='" + action + '\'' + ", params="
            + params + ", context=" + context + ']';
    }

    public static class Builder {
        private InputStream content;
        private String filename;
        private String action;
        private Map<String, String> params;
        private BundleHandoverContext context;

        /**
         * @param content an {@link java.io.InputStream} for reading the content handed over via the
         * {@link org.rhq.core.pluginapi.bundle.BundleHandoverFacet}
         */
        public Builder setContent(InputStream content) {
            this.content = content;
            return this;
        }

        /**
         * @param filename name of the content
         */
        public Builder setFilename(String filename) {
            this.filename = filename;
            return this;
        }

        /**
         * @param action the action required by the bundle recipe
         */
        public Builder setAction(String action) {
            this.action = action;
            return this;
        }

        /**
         * @param params the collection of named parameters supplied by the bundle recipe
         */
        public Builder setParams(Map<String, String> params) {
            this.params = params;
            return this;
        }

        /**
         * @param context the handover context
         */
        public Builder setContext(BundleHandoverContext context) {
            this.context = context;
            return this;
        }

        public BundleHandoverRequest createBundleHandoverRequest() {
            return new BundleHandoverRequest(content, filename, action, params, context);
        }
    }
}
