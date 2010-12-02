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
package org.rhq.enterprise.gui.coregui.server.gwt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SubjectCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.SubjectGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.exception.LoginException;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Greg Hinkle
 */
public class SubjectGWTServiceImpl extends AbstractGWTServiceImpl implements SubjectGWTService {

    private static final long serialVersionUID = 1L;

    private final Log log = LogFactory.getLog(this.getClass());

    private SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();

    public void createPrincipal(String username, String password) {
        try {
            subjectManager.createPrincipal(getSessionSubject(), username, password);
        } catch (RuntimeException e) {
            handleException(e);
        }
    }

    public Subject createSubject(Subject subjectToCreate) {
        try {
            return SerialUtility.prepare(subjectManager.createSubject(getSessionSubject(), subjectToCreate),
                "SubjectManager.createSubject");
        } catch (RuntimeException e) {
            handleException(e);
            return null;
        }
    }

    public Subject createSubject(Subject subjectToCreate, String password) {
        try {
            return SerialUtility.prepare(subjectManager.createSubject(getSessionSubject(), subjectToCreate, password),
                "SubjectManager.createSubject");
        } catch (RuntimeException e) {
            handleException(e);
            return null;
        }
    }

    public void deleteSubjects(int[] subjectIds) {
        try {
            subjectManager.deleteSubjects(getSessionSubject(), subjectIds);
        } catch (RuntimeException e) {
            handleException(e);
        }
    }

    public Subject login(String username, String password) {
        try {
            return SerialUtility.prepare(subjectManager.login(username, password), "SubjectManager.login");
        } catch (LoginException e) {
            throw new RuntimeException("LoginException: " + e.getMessage());
        } catch (RuntimeException e) {
            handleException(e);
            return null;
        }
    }

    public void logout(Subject subject) {
        try {
            subjectManager.logout(subject.getSessionId());
        } catch (RuntimeException e) {
            handleException(e);
        }
    }

    public Subject updateSubject(Subject subjectToModify, String newPassword) {
        try {
            return SerialUtility.prepare(subjectManager.updateSubject(getSessionSubject(), subjectToModify, newPassword),
                "SubjectManager.updateSubject");
        } catch (RuntimeException e) {
            handleException(e);
            return null;
        }
    }

    public Subject processSubjectForLdap(Subject subjectToModify, String password) {
        //no permissions check as embedded in the SLSB call.
        try {
            Subject processedSubject = subjectManager.processSubjectForLdap(subjectToModify, password);
            //if sessionId has changed then need to refresh the WebUser
            //            if (subjectToModify.getSessionId() != processedSubject.getSessionId()) {
            //                HttpServletRequest request = requestReference;
            //                if ((request != null) && (request.getSession() != null)) {
            //                    HttpSession session = request.getSession();
            //
            //                    //                    //move this to the sessionAccessServlet
            //                    //                    WebUser webUser = new WebUser(processedSubject, false);
            //                    //                    session.invalidate();
            //                    //                    session = request.getSession(true);
            //                    //                    SessionUtils.setWebUser(session, webUser);
            //                    //                    // look up the user's permissions
            //                    //                    Set<Permission> all_permissions = LookupUtil.getAuthorizationManager()
            //                    //                        .getExplicitGlobalPermissions(processedSubject);
            //                    //
            //                    //                    Map<String, Boolean> userGlobalPermissionsMap = new HashMap<String, Boolean>();
            //                    //                    for (Permission permission : all_permissions) {
            //                    //                        userGlobalPermissionsMap.put(permission.toString(), Boolean.TRUE);
            //                    //                    }
            //                    //                    //load all session attributes
            //                    //                    session.setAttribute(Constants.USER_OPERATIONS_ATTR, userGlobalPermissionsMap);
            //                }
            //            }
            return SerialUtility.prepare(processedSubject, "SubjectManager.processSubjectForLdap");
        } catch (LoginException e) {
            throw new RuntimeException("LoginException: " + e.getMessage());
        } catch (RuntimeException e) {
            handleException(e);
            return null;
        }
    }

    public PageList<Subject> findSubjectsByCriteria(SubjectCriteria criteria) {
        try {
            return SerialUtility.prepare(subjectManager.findSubjectsByCriteria(getSessionSubject(), criteria),
                "SubjectManager.findSubjectsByCriteria");
        } catch (RuntimeException e) {
            handleException(e);
            return null;
        }
    }
    
    public boolean isUserWithPrincipal(String username) {
        return subjectManager.isUserWithPrincipal(username);
    }

    private void handleException(Exception e) {
        log.error("Unexpected error.", e);
        throw new RuntimeException(ThrowableUtil.getAllMessages(e));
    }
    
}
