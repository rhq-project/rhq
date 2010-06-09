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

import java.util.List;

import org.rhq.core.domain.criteria.SavedSearchCriteria;
import org.rhq.core.domain.search.SavedSearch;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.core.domain.search.SearchSuggestion;
import org.rhq.enterprise.gui.coregui.client.gwt.SearchGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.search.SavedSearchManagerLocal;
import org.rhq.enterprise.server.search.execution.SearchAssistManager;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 */
public class SearchGWTServiceImpl extends AbstractGWTServiceImpl implements SearchGWTService {

    private SavedSearchManagerLocal savedSearchManager = LookupUtil.getSavedSearchManager();

    @Override
    public List<SearchSuggestion> getSuggestions(SearchSubsystem searchSubsystem, String expression, int caretPosition) {
        SearchAssistManager searchAssistManager = new SearchAssistManager(getSessionSubject(), searchSubsystem);
        List<SearchSuggestion> results = searchAssistManager.getSuggestions(expression, caretPosition);
        return results;
    }

    @Override
    public int createSavedSearch(SavedSearch savedSearch) {
        return savedSearchManager.createSavedSearch(getSessionSubject(), savedSearch);
    }

    @Override
    public void updateSavedSearch(SavedSearch savedSearch) {
        savedSearchManager.updateSavedSearch(getSessionSubject(), savedSearch);
    }

    @Override
    public void deleteSavedSearch(int savedSearchId) {
        savedSearchManager.deleteSavedSearch(getSessionSubject(), savedSearchId);
    }

    @Override
    public List<SavedSearch> findSavedSearchesByCriteria(SavedSearchCriteria criteria) {
        return SerialUtility.prepare(savedSearchManager.findSavedSearchesByCriteria(getSessionSubject(), criteria),
            "SearchService.findRolesByCriteria");
    }

}