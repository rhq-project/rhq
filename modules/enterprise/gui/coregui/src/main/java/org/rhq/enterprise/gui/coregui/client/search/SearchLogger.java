package org.rhq.enterprise.gui.coregui.client.search;

import com.google.gwt.core.client.GWT;

public class SearchLogger {
    public static void debug(String message) {
        GWT.log("SearchBar: " + message);
    }
}
