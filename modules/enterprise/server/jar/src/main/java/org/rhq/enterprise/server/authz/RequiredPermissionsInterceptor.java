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
package org.rhq.enterprise.server.authz;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * An EJB3 interceptor that checks to ensure a given {@link Subject} has all of the global permissions that are
 * specified via the {@link RequiredPermissions} annotation on the method to be invoked. If the method being invoked is
 * not annotated with {@link RequiredPermissions} or it has an empty list of permissions, this interceptor passes the
 * security check immediately. Otherwise, the method must have a {@link Subject} as its first parameter - that
 * {@link Subject} will be checked to see if it has all the permissions required. If it does not, or if there is no
 * {@link Subject} as the method's first parameter, this interceptor throws an exception and does not allow the method
 * to be invoked.
 *
 * @author John Mazzitelli
 */
public class RequiredPermissionsInterceptor {
    private static Log LOG = LogFactory.getLog(RequiredPermissionsInterceptor.class);

    /**
     * Checks to ensure the method can be invoked.
     *
     * @param  invocation_context the invocation context
     *
     * @return the results of the invocation
     *
     * @throws Exception           if an error occurred further down the interceptor stack
     * @throws PermissionException if the security check fails
     */
    @AroundInvoke
    public Object checkRequiredPermissions(InvocationContext invocation_context) throws Exception {
        try {
            Map<Permission, String> perms_errors_list = new HashMap<Permission, String>();
            Method method = invocation_context.getMethod();
            RequiredPermissions perms_anno = method.getAnnotation(RequiredPermissions.class);
            RequiredPermission perm_anno = method.getAnnotation(RequiredPermission.class);

            // process the list of permissions, if specified
            if (((perms_anno != null) && (perms_anno.value().length > 0))) {
                for (RequiredPermission rq : perms_anno.value()) {
                    perms_errors_list.put(rq.value(), rq.error());
                }
            }

            // process the individual permission, if specified
            if ((perm_anno != null) && (perm_anno.value() != null)) {
                perms_errors_list.put(perm_anno.value(), perm_anno.error());
            }

            // get the subject, if there is one as the first parameter to the method invocation
            Subject subject = null;
            Object[] params = invocation_context.getParameters();
            if ((params != null) && (params.length > 0) && (params[0] instanceof Subject)) {
                subject = (Subject) params[0];
            }

            // Make sure someone is not spoofing another user - ensure the associated session ID is valid.
            // This means that anytime we pass Subject as the first parameter, we are assuming it needs
            // its session validated.  If there is ever a case where we pass Subject as the first parameter
            // to an EJB and we do NOT want to validate its session, you need to annotate that EJB
            // method with @ExcludeDefaultInterceptors so we don't call this interceptor.
            if (subject != null) {
                if (subject.getSessionId() != null) {
                    SubjectManagerLocal subject_manager = LookupUtil.getSubjectManager();

                    // isValidSessionId will also update the session's last-access-time
                    if (!subject_manager.isValidSessionId(subject.getSessionId(), subject.getName())) {
                        // if this happens, it is possible someone is trying to spoof an authenticated user!
                        throw buildPermissionException("The session ID for user [" + subject.getName()
                            + "] is invalid!", invocation_context);
                    }
                } else {
                    throw buildPermissionException("The subject [" + subject.getName() + "] did not have a session",
                        invocation_context);
                }
            }

            // if the method is not annotated or it has no permissions that are required for it to be invoked,
            // don't do anything; otherwise, we need to check the permissions
            if (perms_errors_list.size() > 0) {
                // the method to be invoked has one or more required permissions;
                // therefore, the method must have a Subject as its first argument value
                if (subject == null) {
                    throw buildPermissionException("Method requires permissions but does not have a subject parameter",
                        invocation_context);
                }

                // look these up now - we don't use @EJB because I don't want the container wasting time
                // injecting EJBs if I don't need them for those methods not annotated with @RequiredPermissions
                AuthorizationManagerLocal authorization_manager = LookupUtil.getAuthorizationManager();

                Set<Permission> required_permissions = perms_errors_list.keySet();
                Set<Permission> subject_permissions = authorization_manager.getExplicitGlobalPermissions(subject);

                for (Permission required_permission : required_permissions) {
                    if (!Permission.Target.GLOBAL.equals(required_permission.getTarget())) {
                        throw buildPermissionException("@RequiredPermissions must be Permission.Target.GLOBAL: ["
                            + required_permission + "]", invocation_context);
                    }

                    if (!subject_permissions.contains(required_permission)) {
                        String perm_error = perms_errors_list.get(required_permission);
                        String full_error = "Subject [" + subject.getName() + "] is not authorized for ["
                            + required_permission + "]";

                        if ((perm_error != null) && (perm_error.length() > 0)) {
                            full_error = perm_error + ": " + full_error;
                        }

                        throw buildPermissionException(full_error, invocation_context);
                    }
                }
            }
        } catch (PermissionException pe) {
            LOG.debug("Interceptor detected a permission exception", pe);
            throw pe;
        } catch (Exception e) {
            Exception ex = buildPermissionException("Failed to check required permissions to invoke: ",
                invocation_context, e);
            LOG.debug("Permission Exception", ex);
            throw ex;
        }

        // we are authorized for all the required permissions - let the invocation continue
        return invocation_context.proceed();
    }

    private PermissionException buildPermissionException(String message, InvocationContext context) {
        return buildPermissionException(message, context, null);
    }

    private PermissionException buildPermissionException(String message, InvocationContext context, Exception e) {
        return new PermissionException(message + ": " + getInvocationString(context), e);
    }

    /**
     * Returns a string representation of the given invocation context so it can be displayed in messages.
     *
     * @param  invocation
     *
     * @return string containing information about the invocation
     */
    private String getInvocationString(InvocationContext invocation) {
        StringBuffer buf = new StringBuffer("invocation: ");
        buf.append("method=" + invocation.getMethod().toGenericString());
        buf.append(",context-data=" + invocation.getContextData());
        return buf.toString();
    }
}