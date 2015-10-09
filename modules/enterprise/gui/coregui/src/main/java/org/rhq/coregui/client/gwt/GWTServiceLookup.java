/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.coregui.client.gwt;

import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.util.Log;

/**
 * This lookup service retrieves each RPC service and sets a custom RpcRequestBuilder that adds the login session
 * id to be security checked on the server.  Where timeouts can be specified: -1=default, 0=disable,
 * >0=specifiedValueInMilliseconds.
 * <p><b>IMPORTANT:</b></p>
 * <p>
 * A lookup for any given service returns a cached instance of the service.  That instance is given a new
 * RequestBuilder with the requested timeout (or the default if a no-timeout lookup signature is used). That means the
 * the timeout lives <i>only until the next lookup is performed for the same service!</i>.  So, it is highly
 * recommended that you chain the service lookup directly to the service method being invoked.  Do not declare
 * the service in advance, especially as a class-level field where it may be instantiated at unexpected times.
 * The lookups are lightweight, so don't be afraid to perform a lookup for each service call. For example:
 * <pre>GWTServiceLookup.getBundleService().deleteBundleDestination(args)</pre>
 * </p>
 * @author Greg Hinkle
 * @author Joseph Marques
 * @author John Mazzitelli
 */
public class GWTServiceLookup {

    public static AlertDefinitionGWTServiceAsync getAlertDefinitionService() {
        return secure(AlertDefinitionGWTServiceAsync.Util.getInstance());
    }

    public static AlertTemplateGWTServiceAsync getAlertTemplateService() {
        return secure(AlertTemplateGWTServiceAsync.Util.getInstance());
    }

    public static GroupAlertDefinitionGWTServiceAsync getGroupAlertDefinitionService() {
        return secure(GroupAlertDefinitionGWTServiceAsync.Util.getInstance());
    }

    public static GroupDefinitionExpressionBuilderGWTServiceAsync getGroupDefinitionExpressionBuilderService() {
        return secure(GroupDefinitionExpressionBuilderGWTServiceAsync.Util.getInstance());
    }

    public static ConfigurationGWTServiceAsync getConfigurationService() {
        return secure(ConfigurationGWTServiceAsync.Util.getInstance());
    }

    public static ConfigurationGWTServiceAsync getConfigurationService(int timeout) {
        return secure(ConfigurationGWTServiceAsync.Util.getInstance(), timeout);
    }

    public static DriftGWTServiceAsync getDriftService() {
        return secure(DriftGWTServiceAsync.Util.getInstance());
    }

    public static PluginGWTServiceAsync getPluginService() {
        return secure(PluginGWTServiceAsync.Util.getInstance());
    }

    public static ResourceGWTServiceAsync getResourceService() {
        return secure(ResourceGWTServiceAsync.Util.getInstance());
    }

    public static ResourceGWTServiceAsync getResourceService(int timeout) {
        return secure(ResourceGWTServiceAsync.Util.getInstance(), timeout);
    }

    public static ResourceGroupGWTServiceAsync getResourceGroupService() {
        return secure(ResourceGroupGWTServiceAsync.Util.getInstance());
    }

    public static ResourceGroupGWTServiceAsync getResourceGroupService(int timeout) {
        return secure(ResourceGroupGWTServiceAsync.Util.getInstance(), timeout);
    }

    public static ResourceTypeGWTServiceAsync getResourceTypeGWTService() {
        return secure(ResourceTypeGWTServiceAsync.Util.getInstance());
    }

    public static ResourceTypeGWTServiceAsync getResourceTypeGWTService(int timeout) {
        return secure(ResourceTypeGWTServiceAsync.Util.getInstance(), timeout);
    }

    public static RoleGWTServiceAsync getRoleService() {
        return secure(RoleGWTServiceAsync.Util.getInstance());
    }

    public static SubjectGWTServiceAsync getSubjectService() {
        return secure(SubjectGWTServiceAsync.Util.getInstance());
    }

    public static SystemGWTServiceAsync getSystemService() {
        return secure(SystemGWTServiceAsync.Util.getInstance());
    }

    public static MeasurementDataGWTServiceAsync getMeasurementDataService() {
        return secure(MeasurementDataGWTServiceAsync.Util.getInstance());
    }

    public static MeasurementDataGWTServiceAsync getMeasurementDataService(int timeout) {
        return secure(MeasurementDataGWTServiceAsync.Util.getInstance(), timeout);
    }

    public static MeasurementChartsGWTServiceAsync getMeasurementChartsService() {
        return secure(MeasurementChartsGWTServiceAsync.Util.getInstance());
    }

    public static MeasurementScheduleGWTServiceAsync getMeasurementScheduleService() {
        return secure(MeasurementScheduleGWTServiceAsync.Util.getInstance());
    }

    public static AlertGWTServiceAsync getAlertService() {
        return secure(AlertGWTServiceAsync.Util.getInstance());
    }

    public static AlertGWTServiceAsync getAlertService(int timeout) {
        return secure(AlertGWTServiceAsync.Util.getInstance(), timeout);
    }

    public static OperationGWTServiceAsync getOperationService() {
        return secure(OperationGWTServiceAsync.Util.getInstance());
    }

    public static BundleGWTServiceAsync getBundleService() {
        return secure(BundleGWTServiceAsync.Util.getInstance());
    }

    public static BundleGWTServiceAsync getBundleService(int timeout) {
        return secure(BundleGWTServiceAsync.Util.getInstance(), timeout);
    }

    public static ResourceBossGWTServiceAsync getResourceBossService() {
        return secure(ResourceBossGWTServiceAsync.Util.getInstance());
    }

    /**
     * Consider using {@link PermissionsLoader} instead of using
     * this authorization service directly.
     */
    public static AuthorizationGWTServiceAsync getAuthorizationService() {
        return secure(AuthorizationGWTServiceAsync.Util.getInstance());
    }

    public static AvailabilityGWTServiceAsync getAvailabilityService() {
        return secure(AvailabilityGWTServiceAsync.Util.getInstance());
    }

    public static TagGWTServiceAsync getTagService() {
        return secure(TagGWTServiceAsync.Util.getInstance());
    }

    public static RemoteInstallGWTServiceAsync getRemoteInstallService(int timeout) {
        return secure(RemoteInstallGWTServiceAsync.Util.getInstance(), timeout);
    }

    public static RepoGWTServiceAsync getRepoService() {
        return secure(RepoGWTServiceAsync.Util.getInstance());
    }

    public static ContentGWTServiceAsync getContentService() {
        return secure(ContentGWTServiceAsync.Util.getInstance());
    }

    public static SearchGWTServiceAsync getSearchService() {
        return secure(SearchGWTServiceAsync.Util.getInstance());
    }

    public static DashboardGWTServiceAsync getDashboardService() {
        return secure(DashboardGWTServiceAsync.Util.getInstance());
    }

    public static EventGWTServiceAsync getEventService() {
        return secure(EventGWTServiceAsync.Util.getInstance());
    }

    public static ClusterGWTServiceAsync getClusterService() {
        return secure(ClusterGWTServiceAsync.Util.getInstance());
    }

    public static LdapGWTServiceAsync getLdapService() {
        return secure(LdapGWTServiceAsync.Util.getInstance());
    }

    public static AgentGWTServiceAsync getAgentService() {
        return secure(AgentGWTServiceAsync.Util.getInstance());
    }

    public static PlatformUtilizationGWTServiceAsync getPlatformUtilizationService() {
        return secure(PlatformUtilizationGWTServiceAsync.Util.getInstance());
    }

    public static PlatformUtilizationGWTServiceAsync getPlatformUtilizationService(int timeout) {
        return secure(PlatformUtilizationGWTServiceAsync.Util.getInstance(), timeout);
    }

    public static TopologyGWTServiceAsync getTopologyService() {
        return secure(TopologyGWTServiceAsync.Util.getInstance());
    }

    public static StorageGWTServiceAsync getStorageService() {
        return secure(StorageGWTServiceAsync.Util.getInstance());
    }

    @SuppressWarnings("unchecked")
    private static <T> T secure(Object sdt) {
        return (T) secure(sdt, -1);
    }

    @SuppressWarnings("unchecked")
    private static <T> T secure(Object sdt, int timeout) {
        if (!(sdt instanceof ServiceDefTarget))
            return null;

        SessionRpcRequestBuilder rb = new SessionRpcRequestBuilder(timeout);
        ((ServiceDefTarget) sdt).setRpcRequestBuilder(rb);

        return (T) sdt;
    }

    public static class SessionRpcRequestBuilder extends RpcRequestBuilder {

        private static int DEBUG_TIMEOUT_FUDGE_FACTOR = 30000;
        private static int DEFAULT_RPC_TIMEOUT = 30000;

        private int timeout;

        public SessionRpcRequestBuilder(int timeout) {
            super();

            int rpcTimeout = CoreGUI.get().getRpcTimeout();
            if (rpcTimeout > -1) {
                this.timeout = rpcTimeout;
            } else {
                this.timeout = (timeout < 0) ? DEFAULT_RPC_TIMEOUT : timeout;
            }

            if (CoreGUI.isDebugMode() && (this.timeout != 0)) {
                // debug mode is slow, so give requests more time to complete otherwise you'll get
                // weird exceptions whose messages are extremely unhelpful in finding root cause
                this.timeout += DEBUG_TIMEOUT_FUDGE_FACTOR;
            }
        }

        @Override
        protected RequestBuilder doCreate(String serviceEntryPoint) {
            RequestBuilder rb = super.doCreate(serviceEntryPoint);
            rb.setTimeoutMillis(this.timeout);

            String sessionId = UserSessionManager.getSessionId();
            if (sessionId != null) {
                if (Log.isDebugEnabled()) {
                    Log.debug("SessionRpcRequestBuilder is adding sessionId(" + sessionId + ") to request("
                        + serviceEntryPoint + ")");
                }
                rb.setHeader(UserSessionManager.SESSION_NAME, sessionId);
            } else {
                Log.error("SessionRpcRequestBuilder missing sessionId for request(" + serviceEntryPoint + ") ");
            }

            return rb;
        }

    }

}
