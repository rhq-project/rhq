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
package org.rhq.enterprise.gui.legacy.portlet.summaryCounts;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;

public class PrepareAction extends BaseAction {
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        PropertiesForm pForm = (PropertiesForm) form;
        WebUser user = SessionUtils.getWebUser(request.getSession());

        boolean platform = Boolean.valueOf(user.getPreference(".dashContent.summaryCounts.platform"));
        boolean server = Boolean.valueOf(user.getPreference(".dashContent.summaryCounts.server"));
        boolean service = Boolean.valueOf(user.getPreference(".dashContent.summaryCounts.service"));
        boolean groupCompat = Boolean.valueOf(user.getPreference(".dashContent.summaryCounts.group.compat"));
        boolean groupMixed = Boolean.valueOf(user.getPreference(".dashContent.summaryCounts.group.mixed"));
        boolean software = Boolean.valueOf(user.getPreference(".dashContent.summaryCounts.software"));

        pForm.setPlatform(platform);
        pForm.setServer(server);
        pForm.setService(service);
        pForm.setGroupCompat(groupCompat);
        pForm.setGroupMixed(groupMixed);
        pForm.setSoftware(software);

        return null;
    }
}