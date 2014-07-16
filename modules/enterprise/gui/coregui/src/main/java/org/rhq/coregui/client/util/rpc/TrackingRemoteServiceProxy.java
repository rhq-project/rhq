/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.coregui.client.util.rpc;

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.impl.RemoteServiceProxy;
import com.google.gwt.user.client.rpc.impl.RequestCallbackAdapter.ResponseReader;
import com.google.gwt.user.client.rpc.impl.RpcStatsContext;
import com.google.gwt.user.client.rpc.impl.Serializer;

import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.util.Log;

/**
 * A custom {@link RemoteServiceProxy} that injects additional management and monitoring functionality into the 
 * RPC lifecycle.  Below are the list of extensions:
 * 
 * <ul>
 *   <li>Conditionally sends requests based off of the client-side loggedIn state.  Once the user's client-side session
 *       has expired, communication back to the server is halted by silently dropping requests.  Obviously, for this to
 *       work, the methods that are used prior to being authenticated can not be wrapped with this proxy.</li>
 *   <li>Wrap the existing {@link RequestCallback} in a {@link TrackingRequestCallback}, which will provide 1) a
 *       fall-back mechanism for exceptions/errors that occur while the user is logged out, and 2) a notification
 *       mechanism to send the {@link RPCTracker} events which will tell the activityIndicator when to spin.</li>
 *   <li>Put the user's sessionId into the header of the request.</li>
 * 
 * @author Joseph Marques
 */
public class TrackingRemoteServiceProxy extends RemoteServiceProxy {

    /** Don't block methods used during the login or logout process. Declare the exceptions here. */
    private static final Set<String> bypassMethods = new HashSet<String>();
    static {
        bypassMethods.add("SubjectGWTService_Proxy.findSubjectsByCriteria");
        bypassMethods.add("SubjectGWTService_Proxy.processSubjectForLdap");
        bypassMethods.add("SubjectGWTService_Proxy.logout");
    }

    public TrackingRemoteServiceProxy(String moduleBaseURL, String remoteServiceRelativePath,
        String serializationPolicyName, Serializer serializer) {
        super(moduleBaseURL, remoteServiceRelativePath, serializationPolicyName, serializer);
    }

    /*
     * This method is currently not called by the RPC framework.  When it is, we can remove the sessionId 
     * logic from the GWTServiceLookup class.
     * 
     * For background information, please see http://code.google.com/p/google-web-toolkit/issues/detail?id=5668
     */
    @Override
    protected <T> RequestBuilder doPrepareRequestBuilder(ResponseReader responseReader, String methodName,
        RpcStatsContext statsContext, String requestData, AsyncCallback<T> callback) {

        RequestBuilder rb = super.doPrepareRequestBuilder(responseReader, methodName, statsContext, requestData,
            callback);

        String sessionId = UserSessionManager.getSessionId();
        if (sessionId != null) {
            if (Log.isDebugEnabled()) {
                Log.debug("SessionRpcRequestBuilder is adding sessionId to request for (" + methodName + ")");
            }
            rb.setHeader(UserSessionManager.SESSION_NAME, sessionId);
        } else {
            Log.error("SessionRpcRequestBuilder missing sessionId for request (" + methodName + ")");
        }

        return rb;
    }

    // TODO: add handled to capture timeout failure and retry (at least once) to add resilience to GWT service calls?
    @Override
    protected <T> RequestCallback doCreateRequestCallback(ResponseReader responseReader, String methodName,
        RpcStatsContext statsContext, AsyncCallback<T> callback) {

        RequestCallback original = super.doCreateRequestCallback(responseReader, methodName, statsContext, callback);
        TrackingRequestCallback trackingCallback = new TrackingRequestCallback(statsContext.getRequestId(), methodName,
                original);

        RPCTracker.getInstance().register(trackingCallback);

        return trackingCallback;
    }

    @Override
    protected <T> Request doInvoke(ResponseReader responseReader, String methodName, RpcStatsContext statsContext,
        String requestData, AsyncCallback<T> callback) {

        if (Log.isDebugEnabled()) {
            Log.debug("RPC method invocation: " + methodName);
        }
        if (bypassMethods.contains(methodName) || !UserSessionManager.isLoggedOut()) {
            return super.doInvoke(responseReader, methodName, statsContext, requestData, callback);
        }

        return null;
    }
}
