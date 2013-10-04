/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.coregui.client.searchbar;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.form.fields.events.KeyUpEvent;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;
import com.smartgwt.client.widgets.grid.events.RecordDoubleClickEvent;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SavedSearchCriteria;
import org.rhq.core.domain.search.SavedSearch;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.message.Message;

/**
 * Search Strategy for the Favorite Saved Searches.
 * If you wish to save your searches for later use this search strategy
 * will facilitate favorite searches.
 *
 * @author Mike Thompson
 */
public class FavoritesSearchStrategy extends AbstractSearchStrategy {

    private boolean isSearchInProgress = false;

    public FavoritesSearchStrategy(EnhancedSearchBar searchBar) {
        super(searchBar);
    }

    @Override
    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {

        String name = record.getAttribute(ATTR_NAME);
        Integer resultCount = record.getAttributeAsInt(ATTR_RESULT_COUNT);
        String pattern = record.getAttribute(ATTR_PATTERN);

        String resultAsString = (resultCount != null) ? "(" + String.valueOf(resultCount) + ")" : "";
        final String styleStr = "font-family:arial;font-size:11px;white-space:nowrap;overflow:hidden;";
        String formatString = "<table>" + "<tr><td ><span style='" + styleStr + "width:170px;color:green;float:left'>"
            + name + "</span></td>" + "<td align='right'><span style='" + styleStr
            + "width:50px;float:right;font-weight:bold'>" + resultAsString + "</span></td></tr>"
            + "<tr><td colSpan=2><span style='" + styleStr + "width:220px;float:left'>" + pattern
            + "</span></td></tr></table>";

        return formatString;
    }

    /**
     * Executed when this field is clicked on.  Note that if {@link
     * com.smartgwt.client.widgets.grid.ListGrid#addRecordClickHandler ListGrid.recordClick} is also defined, it will be fired
     * for fields that define a recordClick handler if the field-level handler returns true. Call {@link com.smartgwt.client.widgets.grid.events.RecordClickEvent#cancel()} from within {@link com.smartgwt.client.widgets.grid.events.RecordClickHandler#onRecordClick} to prevent the
     * grid-level handler from firing.
     *
     * @param event the event
     */
    @Override
    public void onRecordClick(RecordClickEvent event) {
        searchBar.getSaveSearchTextItem().setValue(event.getRecord().getAttribute(ATTR_NAME));
        searchBar.getSearchTextItem().setValue(event.getRecord().getAttribute(ATTR_PATTERN));
        searchBar.getSearchTextItem().focusInItem();
    }

    @Override
    public void onRecordDoubleClick(RecordDoubleClickEvent event) {
        final Record record = event.getRecord();
        final String name = record.getAttribute(ATTR_NAME);

        SC.ask(MSG.view_searchBar_savedSearch_confirmDelete(name), new BooleanCallback() {
            public void execute(Boolean confirmed) {
                if (confirmed) {
                    Integer id = record.getAttributeAsInt(ATTR_ID);

                    GWTServiceLookup.getSearchService().deleteSavedSearch(id, new AsyncCallback<Void>() {

                        @Override
                        public void onSuccess(Void result) {
                            Message message = new Message(MSG.view_searchBar_savedSearch_delete(name),
                                Message.Severity.Info);
                            CoreGUI.getMessageCenter().notify(message);
                            populateSavedSearches();
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            Message message = new Message(MSG.view_searchBar_savedSearch_failDelete(name),
                                Message.Severity.Error);
                            CoreGUI.getMessageCenter().notify(message);
                        }
                    });
                }
            }
        });
    }

    @Override
    public void searchKeyUpHandler(KeyUpEvent keyUpEvent) {
        // do nothing
    }

    @Override
    public void searchFocusHandler() {
        populateSavedSearches();
    }

    private void populateSavedSearches() {

        // avoid concurrent searches
        if (isSearchInProgress) {
            return;
        }

        Log.debug("Search Saved Searches");
        SavedSearchCriteria savedSearchCriteria = new SavedSearchCriteria();
        Subject subject = UserSessionManager.getSessionSubject();
        savedSearchCriteria.addFilterSubjectId(subject.getId());
        savedSearchCriteria.setStrict(true);
        final long startTime = System.currentTimeMillis();
        searchBar.getPickListGrid().setData(new ListGridRecord[] {});

        isSearchInProgress = true;

        searchService.findSavedSearchesByCriteria(savedSearchCriteria, new AsyncCallback<List<SavedSearch>>() {

            @Override
            public void onFailure(Throwable caught) {
                isSearchInProgress = false;
                CoreGUI.getErrorHandler().handleError(MSG.view_searchBar_savedSearch_failFetch(), caught);
            }

            @Override
            public void onSuccess(List<SavedSearch> result) {
                try {
                    long fetchTime = System.currentTimeMillis() - startTime;
                    Log.debug(result.size() + " saved searches fetched in: " + fetchTime + "ms");

                    ListGrid searchBarPickListGrid = searchBar.getPickListGrid();
                    DataSource ds = searchBarPickListGrid.getDataSource();

                    if (null == ds) {
                        ds = new DataSource();
                        ds.setClientOnly(true);
                        DataSourceTextField valueField = new DataSourceTextField(ATTR_ID, "Id");
                        valueField.setPrimaryKey(true);
                        ds.setFields(valueField);
                        searchBarPickListGrid.setDataSource(ds);

                    } else {
                        ds.invalidateCache();
                    }

                    for (SavedSearch savedSearch : result) {
                        Log.debug("savedSearch: " + savedSearch.getName());
                        ListGridRecord record = new ListGridRecord();
                        record.setAttribute(ATTR_ID, savedSearch.getId());
                        record.setAttribute(ATTR_KIND, "Saved");
                        record.setAttribute(ATTR_NAME, savedSearch.getName());
                        record.setAttribute(ATTR_DESCRIPTION, savedSearch.getDescription());
                        record.setAttribute(ATTR_PATTERN, savedSearch.getPattern());
                        if (savedSearch.getResultCount() != null)
                            record.setAttribute(ATTR_RESULT_COUNT, savedSearch.getResultCount());
                        ds.addData(record);
                    }

                    try {
                        searchBarPickListGrid.setData(new ListGridRecord[] {});
                        searchBarPickListGrid.fetchData();
                    } catch (Exception e) {
                        Log.debug("Caught exception on fetchData: " + e);
                    }
                } finally {
                    isSearchInProgress = false;
                }
            }
        });
    }

    @Override
    public int getCellHeight() {
        return 35;
    }

    @Override
    public void searchReturnKeyHandler(KeyUpEvent keyUpEvent) {
        // do nothing
    }
}
