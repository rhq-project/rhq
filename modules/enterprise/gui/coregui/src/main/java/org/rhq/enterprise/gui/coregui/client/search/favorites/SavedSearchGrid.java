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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Grid;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SavedSearchCriteria;
import org.rhq.core.domain.search.SavedSearch;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.search.SearchBar;

/**
 * @author Joseph Marques
 */
public class SavedSearchGrid extends Grid {

    private SavedSearchSelectionHandler patternSelectionHandler;
    private SearchSubsystem searchSubsystem;
    private List<SavedSearch> data = new ArrayList<SavedSearch>();

    public interface SavedSearchSelectionHandler {
        public void handleSelection(int rowIndex, int columnIndex, SavedSearch savedSearch);
    }

    public void setSavedSearchSelectionHandler(SavedSearchSelectionHandler handler) {
        this.patternSelectionHandler = handler;
    }

    class SavedSearchRowFormatter extends RowFormatter {
        private int savedSearchCount;

        public SavedSearchRowFormatter() {
            this(0);
        }

        public SavedSearchRowFormatter(int savedSearchCount) {
            this.savedSearchCount = savedSearchCount;
        }

        @Override
        public String getStyleName(int row) {
            // add -bottom decoration for all rows except last
            if (row < savedSearchCount) {
                return " savedSearchesPanel-row";
            }
            return "";
        }

        @Override
        public String getStylePrimaryName(int row) {
            return getStyleName(row);
        }
    }

    public SavedSearchGrid(SearchSubsystem searchSubsystem) {
        super(0, 2); // assume no rows to start, but we'll always have 2 columns

        setRowFormatter(new SavedSearchRowFormatter());
        sinkEvents(Event.ONMOUSEOVER | Event.ONMOUSEOUT | Event.ONCLICK);
        setCellSpacing(0);
        setCellPadding(5);
        setStyleName("savedSearchesGrid");

        this.searchSubsystem = searchSubsystem;
    }

    @Override
    public void onBrowserEvent(Event event) {
        Element td = getEventTargetCell(event);
        if (td == null) {
            return;
        }
        Element tr = DOM.getParent(td);
        Element table = DOM.getParent(tr);
        switch (DOM.eventGetType(event)) {
        case Event.ONCLICK: {
            int columnIndex = DOM.getChildIndex(tr, td);
            int rowIndex = DOM.getChildIndex(table, tr);
            SavedSearch savedSearch = data.get(rowIndex); // get request-cached element that should be in that row
            patternSelectionHandler.handleSelection(rowIndex, columnIndex, savedSearch);
            if (columnIndex == 0) {
                onRowOut(tr);
            }
            break;
        }
        case Event.ONMOUSEOVER: {
            onRowOver(tr);
            break;
        }
        case Event.ONMOUSEOUT: {
            onRowOut(tr);
            break;
        }
        }
    }

    protected void onRowOut(Element row) {
        Element actionCell = DOM.getChild(row, 1);
        DOM.setStyleAttribute(actionCell, "background", "");
        DOM.setStyleAttribute(row, "backgroundColor", "white");
    }

    protected void onRowOver(Element row) {
        Element actionCell = DOM.getChild(row, 1);
        DOM.setStyleAttribute(actionCell, "backgroundImage", "url(" + SearchBar.TRASH + ")");
        DOM.setStyleAttribute(actionCell, "backgroundRepeat", "no-repeat");
        DOM.setStyleAttribute(actionCell, "backgroundPosition", "center");
        DOM.setStyleAttribute(actionCell, "width", "24px");
        DOM.setStyleAttribute(actionCell, "height", "24px");
        DOM.setStyleAttribute(row, "backgroundColor", "rgb(222,222,222)");
    }

    public void updateModel(final AsyncCallback<List<SavedSearch>> callback) {
        Subject subject = UserSessionManager.getSessionSubject();
        SavedSearchCriteria criteria = new SavedSearchCriteria();
        criteria.addFilterSubjectId(subject.getId());
        criteria.addFilterSearchSubsystem(searchSubsystem);
        criteria.addSortName(PageOrdering.ASC);

        GWTServiceLookup.getSearchService().findSavedSearchesByCriteria(criteria,
            new AsyncCallback<List<SavedSearch>>() {
                @Override
                public void onSuccess(List<SavedSearch> userSavedSearches) {
                    data = userSavedSearches; // cache for correct client-side lookup on selection event
                    clear(true);
                    resizeRows(userSavedSearches.size());

                    int i = 0;
                    SavedSearchRowFormatter rowFormatter = new SavedSearchRowFormatter(data.size());
                    for (SavedSearch nextSavedSearch : userSavedSearches) {
                        setHTML(i, 0, stylize(nextSavedSearch));
                        setHTML(i, 1, trashify());
                        i++;
                    }
                    setRowFormatter(rowFormatter);

                    callback.onSuccess(userSavedSearches);
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Could not load saved searches", caught);

                    callback.onFailure(caught);
                }
            });
    }

    private static String stylize(SavedSearch savedSearch) {
        String name = savedSearch.getName();
        String pattern = savedSearch.getPattern();
        String count = savedSearch.getResultCount() == null ? "" : "(" + savedSearch.getResultCount() + ")";
        return "<span class=\"savedSearchesPanel-top\">" + name + "</span> " + count + "<br/>" //
            + "<span class=\"savedSearchesPanel-bottom\">" + pattern + "</span>";
    }

    private static String trashify() {
        return "<div name=\"action\"></div>";
    }

    public String getSelectedItem() {
        return "";
    }

    public int size() {
        return data.size();
    }
}
