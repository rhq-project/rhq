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

package org.rhq.bundle.ant.type;

import java.io.InputStream;
import java.util.Map;

/**
 * A class wrapping the details of content being handed over to the target resource component during an ANT bundle
 * deployment. 
 *
 * @author Thomas Segismont
 */
public class HandoverInfo {
    private final InputStream content;
    private final String filename;
    private final String action;
    private final Map<String, String> params;
    private final boolean revert;

    private HandoverInfo(InputStream content, String filename, String action, Map<String, String> params, boolean revert) {
        this.content = content;
        this.filename = filename;
        this.action = action;
        this.params = params;
        this.revert = revert;
    }

    /**
     * @return an {@link java.io.InputStream} for reading the content to be handed over
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
     * @return true if the bundle is being reverted, false otherwise
     */
    public boolean isRevert() {
        return revert;
    }

    @Override
    public String toString() {
        return "HandoverInfo[" + "content=" + content + ", filename='" + filename + '\'' + ", action='" + action + '\''
            + ", params=" + params + ']';
    }

    public static class Builder {
        private InputStream content;
        private String filename;
        private String action;
        private Map<String, String> params;
        private boolean revert;

        /**
         * @param content an {@link java.io.InputStream} for reading the content to be handed over
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
         * @param revert flag indicating if the bundle deployment is reverting a version
         */
        public Builder setRevert(boolean revert) {
            this.revert = revert;
            return this;
        }

        public HandoverInfo createHandoverInfo() {
            return new HandoverInfo(content, filename, action, params, revert);
        }
    }
}
