#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.HLayout;

/**
 * Entry point for test SmartGWT GWT module.
 */
public class Application implements EntryPoint {
    /**
     * This is called when the browser loads Application.html.
     */
    public void onModuleLoad() {
        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            public void onUncaughtException(Throwable t) {
                System.err.println("--- UNCAUGHT EXCEPTION ---");
                t.printStackTrace();
            }
        });
        
        HLayout hLayout = new HLayout();
        hLayout.setMargin(10);

        // *************** Add widgets to hLayout here. ****************
        Label label = new Label("Hello world!");
        hLayout.addMember(label);

        hLayout.draw();
    }
}

