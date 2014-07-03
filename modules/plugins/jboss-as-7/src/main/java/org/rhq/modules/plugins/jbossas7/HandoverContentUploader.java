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

package org.rhq.modules.plugins.jbossas7;

import static org.rhq.core.pluginapi.bundle.BundleHandoverResponse.FailureType.EXECUTION;
import static org.rhq.modules.plugins.jbossas7.ASConnection.verbose;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.JsonNode;

import org.rhq.core.pluginapi.bundle.BundleHandoverRequest;
import org.rhq.core.pluginapi.bundle.BundleHandoverResponse;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author Thomas Segismont
 */
class HandoverContentUploader {
    private static final Log LOG = LogFactory.getLog(CliExecutor.class);

    private final BundleHandoverRequest request;
    private final ASConnection asConnection;
    private String filename;
    private InputStream content;
    private String runtimeName;
    private BundleHandoverResponse failureResponse;
    private String hash;

    HandoverContentUploader(BundleHandoverRequest request, ASConnection asConnection) {
        this.request = request;
        this.asConnection = asConnection;
    }

    /**
     * @return true if content upload is successful, false otherwiser
     */
    boolean upload() {
        filename = request.getFilename();
        content = request.getContent();
        runtimeName = request.getParams().get("runtimeName");
        if (StringUtil.isBlank(runtimeName)) {
            runtimeName = filename;
        }

        ASUploadConnection uploadConnection = new ASUploadConnection(asConnection, filename);
        OutputStream out = uploadConnection.getOutputStream();
        if (out == null) {
            failureResponse = BundleHandoverResponse.failure(EXECUTION,
                "An error occured while the agent was preparing for content download");
            return false;
        }

        try {
            StreamUtil.copy(content, out, false);
        } finally {
            StreamUtil.safeClose(content);
        }

        JsonNode uploadResult = uploadConnection.finishUpload();
        if (verbose) {
            LOG.info(uploadResult);
        }

        if (ASUploadConnection.isErrorReply(uploadResult)) {
            failureResponse = BundleHandoverResponse.failure(EXECUTION,
                ASUploadConnection.getFailureDescription(uploadResult));
            return false;
        }

        JsonNode resultNode = uploadResult.get("result");
        hash = resultNode.get("BYTES_VALUE").getTextValue();

        return true;
    }

    /**
     * @return the filename of the content
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @return the runtime name to set when deploying the uploaded content
     */
    public String getRuntimeName() {
        return runtimeName;
    }

    /**
     * @return null, unless {@link #upload()} returned false
     */
    public BundleHandoverResponse getFailureResponse() {
        return failureResponse;
    }

    /**
     * @return the hash of the uploaded content
     */
    public String getHash() {
        return hash;
    }
}
