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
package org.rhq.enterprise.gui.content;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.composite.AdvisoryDetailsComposite;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentUIManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * 
 * @author Pradeep Kilambi
 *
 */
public class AdvisoryDetailsUIBean {
    private AdvisoryDetailsComposite advisoryDetailsComposite;
    private final Log log = LogFactory.getLog(AdvisoryDetailsUIBean.class);

    public AdvisoryDetailsComposite getAdvisoryDetailsComposite() {
        loadAdvisoryDetailsComposite();
        return this.advisoryDetailsComposite;
    }

    private void loadAdvisoryDetailsComposite() {
        if (this.advisoryDetailsComposite == null) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            Integer id = FacesContextUtility.getRequiredRequestParameter("id", Integer.class);
            ContentUIManagerLocal manager = LookupUtil.getContentUIManager();
            this.advisoryDetailsComposite = manager.loadAdvisoryDetailsComposite(subject, id);
        }
    }
}
