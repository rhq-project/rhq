package org.rhq.enterprise.gui.coregui.client;

import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceGWTService;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSearchView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceTreeView;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.RootPanel;
import com.smartgwt.client.core.KeyIdentifier;
import com.smartgwt.client.types.Side;
import com.smartgwt.client.util.KeyCallback;
import com.smartgwt.client.util.Page;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;


/**
 * @author Greg Hinkle
 */
public class CoreGUI implements EntryPoint {
  ResourceGWTServiceAsync resourceService;


    public void onModuleLoad() {

        if (!GWT.isScript()) {
            KeyIdentifier debugKey = new KeyIdentifier();
            debugKey.setCtrlKey(true);
            debugKey.setKeyName("D");
            Page.registerKey(debugKey, new KeyCallback() {
                public void execute(String keyName) {
                    SC.showConsole();
                }
            });
        }

//        MenuBarView menuBarView = new MenuBarView();
//        WidgetCanvas menuCanvas = new WidgetCanvas(menuBarView);
//        menuCanvas.setTop(0);
//        menuCanvas.setWidth100();
//        menuCanvas.draw();

        resourceService = ResourceGWTService.App.getInstance();


        DOM.setInnerHTML(RootPanel.get("Loading-Panel").getElement(), "");


        final TabSet topTabSet = new TabSet();
        topTabSet.setTabBarPosition(Side.TOP);
        topTabSet.setWidth(1200);
        topTabSet.setHeight(900);


        Tab tableTab = new Tab("Resource Search Table");
        Tab treeTab = new Tab("Resource Tree");
        Tab configTab = new Tab("Configuration Editor");


        configTab.setPane(new ConfigurationEditor());
        treeTab.setPane(new ResourceTreeView());
        tableTab.setPane(new ResourceSearchView());


        topTabSet.addTab(configTab);
        topTabSet.addTab(tableTab);
        topTabSet.addTab(treeTab);

        topTabSet.setTop(50);
        topTabSet.draw();
    }



}


