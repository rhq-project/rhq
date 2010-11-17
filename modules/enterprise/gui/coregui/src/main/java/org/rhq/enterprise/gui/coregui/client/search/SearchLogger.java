package org.rhq.enterprise.gui.coregui.client.search;

import com.allen_sauer.gwt.log.client.Log;

public class SearchLogger {

    public static void debug(String message) {
        Log.debug("SearchBar: " + message);
    }
    
}
