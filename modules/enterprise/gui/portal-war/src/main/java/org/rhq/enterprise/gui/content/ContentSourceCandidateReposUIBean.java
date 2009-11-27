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

import javax.faces.model.DataModel;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.common.paging.PageControlView;
import org.rhq.enterprise.gui.common.paging.PagedListDataModel;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.content.ContentSourceManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

public class ContentSourceCandidateReposUIBean extends PagedDataTableUIBean {

    public static final String MANAGED_BEAN_NAME = ContentSourceCandidateReposUIBean.class.getSimpleName();

    public ContentSourceCandidateReposUIBean() {
    }

    @Override
    public DataModel getDataModel() {
        if (dataModel == null) {
            dataModel = new ContentSourceCandidateReposDataModel(PageControlView.ContentSourceCandidateReposList,
                MANAGED_BEAN_NAME);
        }

        return dataModel;
    }

    private class ContentSourceCandidateReposDataModel extends PagedListDataModel<Repo> {
        public ContentSourceCandidateReposDataModel(PageControlView view, String beanName) {
            super(view, beanName);
        }

        @Override
        @SuppressWarnings("unchecked")
        public PageList<Repo> fetchPage(PageControl pc) {
            Subject subject = EnterpriseFacesContextUtility.getSubject();
            int id = Integer.valueOf(FacesContextUtility.getRequiredRequestParameter("id"));

            ContentSourceManagerLocal manager = LookupUtil.getContentSourceManager();

            PageList<Repo> results = manager.getCandidateRepos(subject, id, pc);
            return results;
        }
    }
}