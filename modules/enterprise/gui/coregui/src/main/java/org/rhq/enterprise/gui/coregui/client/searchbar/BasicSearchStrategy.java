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
package org.rhq.enterprise.gui.coregui.client.searchbar;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.KeyUpEvent;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordClickEvent;

import org.rhq.core.domain.criteria.SavedSearchCriteria;
import org.rhq.core.domain.search.SavedSearch;
import org.rhq.core.domain.search.SearchSubsystem;
import org.rhq.core.domain.search.SearchSuggestion;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * This is the general search strategy implementation used for most searches.
 * These strategy classes operate by providing the standard functionality
 * needed by searches as defined by the super class {@link AbstractSearchStrategy}
 * This strategy works in conjunction with the {@link FavoritesSearchStrategy} for
 * saved searches.
 * @author Mike Thompson
 */
public class BasicSearchStrategy extends AbstractSearchStrategy {

    public BasicSearchStrategy(EnhancedSearchBar searchBar) {
        super(searchBar);
    }

    @Override
    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {

        //Log.debug("Using BasicSearch Formatter");

        String name = record.getAttribute(ATTR_NAME);
        String kind = record.getAttribute(ATTR_KIND);

        String style = "font-variant: small-caps; font-weight: bold; font-size: 11px; float: left; margin-left: 2px; width: 50px;";
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='height:20px;width:400px;float:left;white-space:nowrap;overflow:hidden;' >");
        String color = (kind.equals(SearchSuggestion.Kind.GlobalSavedSearch.getDisplayName()) || kind
            .equals(SearchSuggestion.Kind.UserSavedSearch.getDisplayName())) ? "color:green;" : "color:grey";
        sb.append("<span style='" + style + color + "' >");
        sb.append(kind);
        sb.append("</span>");
        sb.append("<span style='" + style + "' >");
        sb.append(name);
        sb.append("</span>");
        sb.append("</div>");

        return sb.toString();
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
        Log.debug("BasicSearchStrategy click");
        String kind = event.getRecord().getAttribute(ATTR_KIND);
        if (kind.equals("SAVED") || kind.equals("GLOBAL")) {
            searchBar.switchToSavedSearchMode();
            String savedSearchName = event.getRecord().getAttribute(ATTR_VALUE);
            searchBar.getSaveSearchTextItem().setValue(savedSearchName);
            assignSavedSearchExpression(savedSearchName, searchBar.getSearchTextItem());
        } else {

            String clickedExpression = event.getRecord().getAttribute(ATTR_NAME);
            searchBar.getSearchTextItem().setValue(clickedExpression);
            if (null != clickedExpression && clickedExpression.length() > 0) {
                getTabAwareSearchSuggestions(SearchSubsystem.RESOURCE, clickedExpression, clickedExpression.length());
            }
        }
    }

    private void assignSavedSearchExpression(final String savedSearchName, final TextItem searchComboBoxItem) {
        SavedSearchCriteria criteria = new SavedSearchCriteria();
        criteria.addFilterSubjectId(subject.getId());
        criteria.addFilterName(savedSearchName); // null OK
        criteria.setStrict(true);

        GWTServiceLookup.getSearchService().findSavedSearchesByCriteria(criteria,
            new AsyncCallback<List<SavedSearch>>() {
                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_searchBar_savedSearch_failFind(savedSearchName),
                        caught);
                }

                @Override
                public void onSuccess(List<SavedSearch> results) {
                    if (results.size() == 0) {
                        CoreGUI.getMessageCenter().notify(
                            new Message(MSG.view_searchBar_savedSearch_failFind(savedSearchName),
                                Message.Severity.Error));
                    } else {
                        SavedSearch savedSearch = results.get(0);
                        searchComboBoxItem.setValue(savedSearch.getPattern());
                    }
                }
            });

    }

    @Override
    public void searchFocusHandler() {
        Log.debug("focus in BasicSearchStrategy");
        String searchExpression = searchBar.getSearchTextItem().getValueAsString();
        doSearch(searchExpression);
    }

    @Override
    public void searchKeyUpHandler(KeyUpEvent keyUpEvent) {
        Log.debug("Keyup in BasicSearchStrategy: " + keyUpEvent.getKeyName());
        String searchExpression = searchBar.getSearchTextItem().getValueAsString();
        doSearch(searchExpression);

    }

    @Override
    public void searchReturnKeyHandler(KeyUpEvent keyUpEvent) {
        doSearch((String) keyUpEvent.getItem().getValue());
    }

    private void doSearch(String searchExpression) {
        if (null != searchExpression && searchExpression.length() > 0) {
            getTabAwareSearchSuggestions(SearchSubsystem.RESOURCE, searchBar.getSearchTextItem().getValueAsString(),
                searchBar.getSearchTextItem().getValueAsString().length());
        } else {
            Log.debug("Empty Search expression");
            getTabAwareSearchSuggestions(SearchSubsystem.RESOURCE, null, 0);
        }
    }

    @Override
    public int getCellHeight() {
        return 25;
    }

    private void getTabAwareSearchSuggestions(final SearchSubsystem searchSubsystem, final String expression,
        int caretPosition) {

        searchBar.getPickListGrid().setData(new ListGridRecord[] {});

        final long suggestStart = System.currentTimeMillis();

        Log.debug("Searching for: " + expression);
        searchService.getTabAwareSuggestions(searchSubsystem, expression, caretPosition, null,
            new AsyncCallback<List<SearchSuggestion>>() {

                @Override
                public void onSuccess(List<SearchSuggestion> results) {
                    ListGrid searchBarPickListGrid = searchBar.getPickListGrid();
                    DataSource ds = searchBarPickListGrid.getDataSource();

                    // create the datasource if needed
                    if (null == ds) {
                        ds = new DataSource();
                        ds.setClientOnly(true);
                        DataSourceTextField idField = new DataSourceTextField(ATTR_ID, "Id");
                        idField.setPrimaryKey(true);
                        idField.setCanView(false);
                        DataSourceTextField valueField = new DataSourceTextField(ATTR_VALUE, "Value");
                        DataSourceTextField kindField = new DataSourceTextField(ATTR_KIND, "Kind");
                        kindField.setCanView(false);
                        DataSourceTextField nameField = new DataSourceTextField(ATTR_NAME, "Name");
                        nameField.setCanView(false);
                        ds.setFields(idField, valueField, kindField, nameField);

                        searchBarPickListGrid.setDataSource(ds);

                    } else {
                        ds.invalidateCache();
                    }
                    searchBarPickListGrid.setData(new ListGridRecord[] {});

                    for (SearchSuggestion searchSuggestion : results) {
                        Log.debug("search tab aware Suggestions: " + searchSuggestion.getKind() + ", "
                            + searchSuggestion.getValue() + ", " + searchSuggestion.getLabel());
                        ListGridRecord record = new ListGridRecord();
                        record.setAttribute(ATTR_ID, searchSuggestion.getValue());
                        if (null != searchSuggestion.getKind()) {
                            record.setAttribute(ATTR_KIND, searchSuggestion.getKind().getDisplayName());
                        }
                        record.setAttribute(ATTR_NAME, searchSuggestion.getLabel());
                        record.setAttribute(ATTR_VALUE, searchSuggestion.getValue());
                        ds.addData(record);
                    }

                    try {
                        searchBarPickListGrid.fetchData();
                    } catch (Exception e) {
                        Log.debug("Caught exception on fetchData: " + e);
                    }

                    long suggestFetchTime = System.currentTimeMillis() - suggestStart;
                    Log.debug(results.size() + " suggestions searches fetched in: " + suggestFetchTime + "ms");
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.search_failed_to_retrieve_search_suggestion(), caught);
                }

            });
    }

}
