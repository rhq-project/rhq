package org.rhq.enterprise.gui.coregui.client.util.selenium;

public interface Locatable {

    /**
     * Returns the locatorId.  This can be useful for constructing more granular locatorIds. For example, if
     * the widget contains sub-widgets.  Note, this is the raw locatorId for the widget, to get the fully
     * formed ID, typically ofthe form "simpleClassname-locatorId" Call {@link getID()}.
     * 
     * @return the locatorId
     */
    public String getLocatorId();

    /** 
     * Extends this widget's original locatorId with an extension. This can be useful for constructing more 
     * granular locatorIds. For example, if the widget contains sub-widgets.
     * <pre>
     * ID Format: "getLocatorId()-extension"
     * </pre>
     * 
     * @param extension not null or empty.
     *
     * @return the new, extended locatorId
     */
    public String extendLocatorId(String extension);
}
