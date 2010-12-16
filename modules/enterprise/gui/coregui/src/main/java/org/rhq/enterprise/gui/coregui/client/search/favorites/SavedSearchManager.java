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
package org.rhq.enterprise.gui.coregui.client.search.favorites;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SavedSearchCriteria;
import org.rhq.core.domain.search.SavedSearch;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.SearchGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.search.AbstractSearchBar;

/**
 * @author Joseph Marques
 */
public class SavedSearchManager {

    private final SearchGWTServiceAsync searchService = GWTServiceLookup.getSearchService();

    private LinkedHashMap<String, SavedSearch> savedSearches = new LinkedHashMap<String, SavedSearch>();
    private AbstractSearchBar searchBar;

    public SavedSearchManager(AbstractSearchBar searchBar) {
        this.searchBar = searchBar;
        load();
    }

    public synchronized String getPatternByName(String name) {
        SavedSearch savedSearch = savedSearches.get(name);
        if (savedSearch == null) {
            return null;
        }
        return savedSearch.getPattern();
    }

    public synchronized SavedSearch getSavedSearchByName(String name) {
        return savedSearches.get(name);
    }

    public synchronized SavedSearch getSavedSearchById(Integer savedSearchId) {
        for (SavedSearch next : savedSearches.values()) {
            if (next.getId().equals(savedSearchId)) {
                return next;
            }
        }
        return null;
    }

    public synchronized void updatePatternByName(final String name, final String pattern) {
        SavedSearch savedSearch = savedSearches.get(name);
        if (savedSearch == null) { // created case
            final SavedSearch newSavedSearch = new SavedSearch(searchBar.getSearchSubsystem(), name, pattern,
                UserSessionManager.getSessionSubject());
            searchService.createSavedSearch(newSavedSearch, new AsyncCallback<Integer>() {

                public void onFailure(Throwable caught) {
                    Log.debug("Error: created saved search [" + name + "] with pattern [" + pattern + "]: "
                        + caught.getMessage());
                }

                public void onSuccess(Integer result) {
                    newSavedSearch.setId(result);
                    savedSearches.put(name, newSavedSearch);
                }
            });

        } else { // update case
            searchService.updateSavedSearch(savedSearch, new AsyncCallback<Void>() {

                public void onFailure(Throwable caught) {
                    Log.debug("Error: updating saved search [" + name + "] with pattern [" + pattern + "]: "
                        + caught.getMessage());
                }

                public void onSuccess(Void result) {
                    SavedSearch savedSearch = savedSearches.remove(name);
                    savedSearch.setPattern(pattern);
                    savedSearches.put(name, savedSearch);
                }
            });
        }
    }

    public synchronized List<String> getPatternNamesMRU() {
        List<String> results = new LinkedList<String>();
        for (String name : savedSearches.keySet()) {
            results.add(0, name);
        }
        return results;
    }

    public synchronized void removePatternByName(final String name) {
        SavedSearch savedSearch = savedSearches.get(name);
        if (savedSearch == null) {
            return;
        }
        searchService.deleteSavedSearch(savedSearch.getId(), new AsyncCallback<Void>() {

            public void onFailure(Throwable caught) {
                Log.debug("Error: removing saved search [" + name + "]: " + caught.getMessage());
            }

            public void onSuccess(Void result) {
                savedSearches.remove(name);
            }
        });
    }

    public synchronized void renamePattern(final String oldName, final String newName) {
        final SavedSearch savedSearch = savedSearches.remove(oldName);
        if (savedSearch == null) {
            return;
        }
        savedSearch.setName(newName);
        savedSearches.put(newName, savedSearch);
        searchService.updateSavedSearch(savedSearch, new AsyncCallback<Void>() {

            public void onFailure(Throwable caught) {
                savedSearch.setName(oldName); // revive old name because server-side change failed
                Log.debug("Error: renaming saved search from [" + oldName + "] to [" + newName + "]: "
                    + caught.getMessage());
            }

            public void onSuccess(Void result) {
                // do nothing, reference was already renamed in-band
            }
        });
    }

    public synchronized int getSavedSearchCount() {
        return savedSearches.size();
    }

    private synchronized void load() {
        Subject currentUser = UserSessionManager.getSessionSubject();
        SavedSearchCriteria criteria = new SavedSearchCriteria();
        criteria.addFilterSubjectId(currentUser.getId());
        criteria.addFilterSearchSubsystem(searchBar.getSearchSubsystem());
        searchService.findSavedSearchesByCriteria(criteria, new AsyncCallback<List<SavedSearch>>() {

            public void onSuccess(List<SavedSearch> result) {
                try {
                    savedSearches.clear();
                    Collections.sort(result, new Comparator<SavedSearch>() {

                        public int compare(SavedSearch first, SavedSearch second) {
                            return first.getName().compareTo(second.getName());
                        }
                    });
                    for (SavedSearch next : result) {
                        savedSearches.put(next.getName(), next);
                    }
                } finally {
                    searchBar.onSavedSearchManagerLoaded();
                }
            }

            public void onFailure(Throwable caught) {
                try {
                    Log.debug("Error: loading saved searches: " + caught.getMessage());
                } finally {
                    searchBar.onSavedSearchManagerLoaded();
                }
            }
        });
    }

}
