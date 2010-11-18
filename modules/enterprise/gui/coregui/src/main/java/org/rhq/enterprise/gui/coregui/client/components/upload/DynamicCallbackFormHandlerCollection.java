package org.rhq.enterprise.gui.coregui.client.components.upload;

import java.util.ArrayList;

/**
 * Helper class for widgets that accept
 * {@link com.google.gwt.user.client.ui.FormHandler FormHandlers}. This subclass
 * of ArrayList assumes that all items added to it will be of type
 * {@link com.google.gwt.user.client.ui.FormHandler}.
 */
@SuppressWarnings("serial")
public class DynamicCallbackFormHandlerCollection extends ArrayList<DynamicFormHandler> {

    /**
     * Fires a {@link DynamicFormHandler#onSubmitComplete(DynamicFormSubmitCompleteEvent)}
     * on all handlers in the collection.
     *
     * @param sender
     *            the object sending the event
     * @param results
     *            the results of the form submission
     */
    public void fireOnComplete(DynamicCallbackForm sender, String results) {
        DynamicFormSubmitCompleteEvent event = new DynamicFormSubmitCompleteEvent(sender, results);

        for (DynamicFormHandler handler : this) {
            handler.onSubmitComplete(event);
        }
    }

}
