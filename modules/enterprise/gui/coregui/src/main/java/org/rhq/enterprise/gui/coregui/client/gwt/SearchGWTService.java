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
package org.rhq.enterprise.gui.coregui.client.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import org.rhq.core.domain.criteria.SavedSearchCriteria;
import org.rhq.core.domain.search.SavedSearch;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.core.domain.search.SearchSuggestion;

/**
 * @author Joseph Marques
 */
public interface SearchGWTService extends RemoteService {

    /*
     * search suggestions
     */
    List<SearchSuggestion> getTabAwareSuggestions(SearchSubsystem searchSubsystem, String expression,
        int caretPosition, String tab) throws RuntimeException;

    List<SearchSuggestion> getSuggestions(SearchSubsystem searchSubsystem, String expression, int caretPosition)
        throws RuntimeException;

    /*
     * saved searches
     */
    int createSavedSearch(SavedSearch savedSearch) throws RuntimeException;

    void updateSavedSearchName(int savedSearchId, final String newName) throws RuntimeException;

    void updateSavedSearchPattern(int savedSearchId, final String newPattern) throws RuntimeException;

    void deleteSavedSearch(int savedSearchId) throws RuntimeException;

    List<SavedSearch> findSavedSearchesByCriteria(SavedSearchCriteria criteria) throws RuntimeException;
}
