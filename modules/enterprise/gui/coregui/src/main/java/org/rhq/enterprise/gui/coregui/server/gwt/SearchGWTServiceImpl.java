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
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.enterprise.gui.coregui.client.gwt.SearchGWTService;
import org.rhq.enterprise.gui.coregui.server.util.SerialUtility;
import org.rhq.enterprise.server.search.SavedSearchManagerLocal;
import org.rhq.enterprise.server.search.execution.SearchAssistManager;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Joseph Marques
 */
public class SearchGWTServiceImpl extends AbstractGWTServiceImpl implements SearchGWTService {

    private static final long serialVersionUID = 1L;

    private SavedSearchManagerLocal savedSearchManager = LookupUtil.getSavedSearchManager();

    public List<SearchSuggestion> getTabAwareSuggestions(SearchSubsystem searchSubsystem, String expression,
        int caretPosition, String tab) throws RuntimeException {
        try {
            SearchAssistManager searchAssistManager = new SearchAssistManager(getSessionSubject(), searchSubsystem);
            List<SearchSuggestion> results = searchAssistManager.getTabAwareSuggestions(expression, caretPosition, tab);
            return results;
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public List<SearchSuggestion> getSuggestions(SearchSubsystem searchSubsystem, String expression, int caretPosition)
        throws RuntimeException {
        try {
            SearchAssistManager searchAssistManager = new SearchAssistManager(getSessionSubject(), searchSubsystem);
            List<SearchSuggestion> results = searchAssistManager.getSuggestions(expression, caretPosition);
            return results;
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public int createSavedSearch(SavedSearch savedSearch) throws RuntimeException {
        try {
            return savedSearchManager.createSavedSearch(getSessionSubject(), savedSearch);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void updateSavedSearchName(int savedSearchId, final String newName) throws RuntimeException {
        try {
            SavedSearch savedSearch = getSubjectSavedSearch(savedSearchId);
            savedSearch.setName(newName);
            savedSearchManager.updateSavedSearch(getSessionSubject(), savedSearch);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void updateSavedSearchPattern(int savedSearchId, final String newPattern) throws RuntimeException {
        try {
            SavedSearch savedSearch = getSubjectSavedSearch(savedSearchId);
            savedSearch.setPattern(newPattern);
            savedSearchManager.updateSavedSearch(getSessionSubject(), savedSearch);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public void deleteSavedSearch(int savedSearchId) throws RuntimeException {
        try {
            savedSearchManager.deleteSavedSearch(getSessionSubject(), savedSearchId);
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    public List<SavedSearch> findSavedSearchesByCriteria(SavedSearchCriteria criteria) throws RuntimeException {
        try {
            return SerialUtility.prepare(savedSearchManager.findSavedSearchesByCriteria(getSessionSubject(), criteria),
                "SearchService.findRolesByCriteria");
        } catch (Throwable t) {
            throw new RuntimeException(ThrowableUtil.getAllMessages(t));
        }
    }

    private SavedSearch getSubjectSavedSearch(int savedSearchId) {
        SavedSearchCriteria criteria = new SavedSearchCriteria();
        criteria.addFilterSubjectId(getSessionSubject().getId()); // ensure user can only fetch his/her own
        criteria.addFilterId(savedSearchId);
        List<SavedSearch> results = findSavedSearchesByCriteria(criteria);
        if (results.isEmpty()) {
            return null;
        } else {
            return results.get(0);
        }
    }

}