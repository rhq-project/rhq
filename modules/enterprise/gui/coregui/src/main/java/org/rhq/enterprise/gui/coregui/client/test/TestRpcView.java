package org.rhq.enterprise.gui.coregui.client.test;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.TestGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Ian Springer
 */
public class TestRpcView extends LocatableVLayout {

    public TestRpcView(String locatorId) {
        super(locatorId);
        setMargin(10);

        DynamicForm form = new DynamicForm();
        form.setWidth(400);

        final FormItem item = new FormItem("seconds");
        item.setTitle("Seconds to sleep");
        item.setRequired(true);

        ButtonItem button = new ButtonItem("execute", "Execute");
        button.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                TestGWTServiceAsync testService = GWTServiceLookup.getTestService();
                final int seconds = Integer.valueOf((String)item.getValue());
                testService.sleep(seconds, new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("sleep(" + seconds + ") RPC call failed.", caught);
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(new Message("sleep(" + seconds + ") RPC completed."));
                    }
                });
            }
        });
        form.setItems(item, button);

        addMember(form);
    }

}
