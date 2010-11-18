package org.rhq.enterprise.gui.coregui.client.util.selenium;

import com.google.gwt.core.client.GWT;

import org.rhq.enterprise.gui.coregui.client.Messages;

/**
 * TODO
 */
public interface Locatable {

    Messages MSG = GWT.create(Messages.class);

    /**
     * Returns the locatorId.  This can be useful for constructing more granular locatorIds. For example, if
     * the widget contains sub-widgets.  Note, this is the raw locatorId for the widget, to get the fully
     * formed ID, typically ofthe form "simpleClassname_locatorId" Call {@link getID()}.
     * 
     * @return the locatorId
     */
    String getLocatorId();

    /** 
     * Extends this widget's original locatorId with an extension. This can be useful for constructing more 
     * granular locatorIds. For example, if the widget contains sub-widgets.
     * <pre>
     * ID Format: "getLocatorId()_extension"
     * </pre>
     * 
     * @param extension not null or empty.
     *
     * @return the new, extended locatorId
     */
    String extendLocatorId(String extension);

}
