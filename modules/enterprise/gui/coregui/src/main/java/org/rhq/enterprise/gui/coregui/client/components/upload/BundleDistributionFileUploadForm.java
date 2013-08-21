/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.components.upload;

import com.google.gwt.core.client.GWT;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;

public class BundleDistributionFileUploadForm extends FileUploadForm {

    private int bundleVersionId;
    private String createInitialBundleVersionToken;

    public BundleDistributionFileUploadForm(boolean showUploadButton) {

        super(CoreGUI.getMessages().view_upload_bundleDistFile(), "0", true, showUploadButton, null);

        setAction(GWT.getModuleBaseURL() + "BundleDistributionFileUploadServlet");
    }

    /**
     * If this component successfully uploaded a bundle distribution file and created the bundle version, this
     * will return the new bundle version's ID. Otherwise, 0 is returned.
     * 
     * @return the new bundle version ID
     */
    public int getBundleVersionId() {
        return this.bundleVersionId;
    }

    /**
     * If this component uploaded a bundle distribution file but failed to create the bundle version
     * due to a BundleNotFoundException, this will return the token necessary to then call
     * createInitialBundleVersionViaToken(). Otherwise, null is returned.
     * 
     * @return the token required for a a subsequent call to createInitialBundleVersionViaToken().
     */
    public String getCreateInitialBundleVersionToken() {
        return this.createInitialBundleVersionToken;
    }

    protected boolean processSubmitCompleteResults(String submitCompleteEventResults) {
        bundleVersionId = parseResponse(submitCompleteEventResults);
        return (bundleVersionId > 0 || null != createInitialBundleVersionToken);
    }

    private int parseResponse(String results) {
        // the upload servlet will respond with "BundleNotFoundException [createInitialBundleVersionToken]" to indicate
        // that the BV create failed because the bundle does not yet exist. 
        String bundleNotFoundMsgPrefix = "BundleNotFoundException [";
        int startBundleNotFoundMsgPrefix = results.indexOf(bundleNotFoundMsgPrefix);
        if (startBundleNotFoundMsgPrefix >= 0) {
            int endBundleNotFoundMsgPrefix = startBundleNotFoundMsgPrefix + bundleNotFoundMsgPrefix.length();
            int startBundleNotFoundMsgPostfix = results.indexOf(']', endBundleNotFoundMsgPrefix);
            if (startBundleNotFoundMsgPostfix < 0) {
                return 0; // this should never happen, we should always have the ending "]" bracket
            }
            this.createInitialBundleVersionToken = results.substring(endBundleNotFoundMsgPrefix,
                startBundleNotFoundMsgPostfix);
            return 0;
        }

        // the upload servlet will respond with "success [bundleVersionId]" on success
        String successMsgPrefix = "success [";
        int startSuccessMsgPrefix = results.indexOf(successMsgPrefix);
        if (startSuccessMsgPrefix < 0) {
            return 0; // must mean it wasn't a success - results is probably an error message
        }
        int endSuccessMsgPrefix = startSuccessMsgPrefix + successMsgPrefix.length();
        int startSuccessMsgPostfix = results.indexOf(']', endSuccessMsgPrefix);
        if (startSuccessMsgPostfix < 0) {
            return 0; // this should never happen, if we have "success [" we should always have the ending "]" bracket
        }
        String bundleVersionIdString = results.substring(endSuccessMsgPrefix, startSuccessMsgPostfix);
        int id = 0;
        try {
            id = Integer.parseInt(bundleVersionIdString);
        } catch (Exception e) {
            CoreGUI.getErrorHandler().handleError(MSG.view_upload_error_bundleDistFile(), e);
        }
        return id;
    }
}