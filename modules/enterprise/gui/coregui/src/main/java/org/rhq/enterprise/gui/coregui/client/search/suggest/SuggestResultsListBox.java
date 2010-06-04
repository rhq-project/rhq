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
package org.rhq.enterprise.gui.coregui.client.search.suggest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.user.client.ui.ListBox;

import org.rhq.core.domain.search.SearchSuggestion;
import org.rhq.core.domain.search.SearchSuggestion.Kind;

public class SuggestResultsListBox extends ListBox {
    public static final String FOOTER_MESSAGE = "Start typing for more simple text matches";

    private List<SearchSuggestion> searchSuggestions = new ArrayList<SearchSuggestion>();

    private final List<String> OPERATORS = Arrays.asList("!==", "!=", "==", "=");

    public void setErrorMessage(String error) {
        this.searchSuggestions.clear();
        this.searchSuggestions.add(new SearchSuggestion(Kind.InstructionalTextComment, error));
    }

    public void setSearchSuggestions(List<SearchSuggestion> searchSuggestions) {
        this.searchSuggestions.clear();
        this.searchSuggestions.addAll(searchSuggestions);
    }

    public int render(int maxSuggestions, int maxResultsShown) {
        clear();
        int addedResults = 0;

        if (searchSuggestions.size() == 0) {
            appendFooter(FOOTER_MESSAGE);
            addedResults++;
        }

        for (SearchSuggestion next : searchSuggestions) {
            if (addedResults == maxSuggestions) {
                break;
            }

            appendSuggestItem(next);
            addedResults++;
        }

        setVisibleItemCount(Math.max(2, Math.min(maxResultsShown, addedResults)));

        return addedResults;
    }

    private void appendFooter(String message) {
        SelectElement select = getElement().cast();
        OptionElement option = Document.get().createOptionElement();

        String style = "float: left; margin-left: 2px; font-style: italic; color: gray;";
        String footer = "<span style=\"" + style + "\">" + message + "</span>";

        style = "clear: both;";
        String floatClear = "<br style=\"" + style + "\" />";

        option.setDisabled(true);
        option.setValue(FOOTER_MESSAGE);
        option.setInnerHTML(footer + floatClear);
        select.add(option, null);
    }

    private void appendSuggestItem(SearchSuggestion item) {
        String className = "suggestData ";
        String prefix = "";

        if (item.getKind() == SearchSuggestion.Kind.Simple) {
            className += "suggestDataSimple";
            prefix = "text";
        } else if (item.getKind() == SearchSuggestion.Kind.Advanced) {
            className += "suggestDataAdvanced";
            prefix = "query";
        } else if (item.getKind() == SearchSuggestion.Kind.GlobalSavedSearch
            || item.getKind() == SearchSuggestion.Kind.UserSavedSearch) {
            className += "suggestDataSavedSearch";
            prefix = "saved";
        } else {
        }

        SelectElement select = getElement().cast();
        OptionElement option = Document.get().createOptionElement();
        option.setValue(item.getValue());
        String style = "font-variant: small-caps; font-weight: bold; font-size: 11px; float: left; margin-left: 2px; width: 50px;";
        int marginOffset = 20;
        if (className.endsWith("suggestDataSavedSearch")) {
            style += " color: green;";
            marginOffset += 2;
        } else {
            style += " color: gray;";
            if (className.endsWith("suggestDataSimple")) {
                marginOffset += 8;
            }
        }
        String decoratedPrefix = decorate(prefix, style);
        String highlightedSuggestion = colorOperator(decorate(item.getLabel(), "background-color: yellow;", item
            .getStartIndex(), item.getEndIndex()));
        //String decoratedSuffix = decorate(highlightedSuggestion, "float: left; margin-left: " + marginOffset + "px;");
        String decoratedSuffix = decorate(highlightedSuggestion, "float: left; ");
        String floatClear = "<br style=\"clear: both;\" />";

        String innerHTML = decoratedPrefix + decoratedSuffix + floatClear;
        option.setInnerHTML(innerHTML);
        select.add(option, null);
    }

    // TODO: fixing coloring strategy
    private String colorOperator(String data) {
        for (String operator : OPERATORS) {
            int index = -1;
            while ((index = data.indexOf(operator, index + 1)) != -1) {
                if ((index - 5 >= 0) && data.substring(index - 5, index).equals("style") == false) {
                    break;
                }
            }
            if (index != -1) {
                return decorate(data, "color: blue;", index, index + operator.length());
            }
        }
        return data;
    }

    private String decorate(String data, String style) {
        return decorate(data, style, 0, data.length());
    }

    private String decorate(String data, String style, int startIndex, int endIndex) {
        String before = data.substring(0, startIndex);
        String highlight = data.substring(startIndex, endIndex);
        String after = data.substring(endIndex);
        return before + "<span style=\"" + style + "\">" + highlight + "</span>" + after;
    }
}
