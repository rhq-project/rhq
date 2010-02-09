package org.rhq.enterprise.gui.coregui.client;

import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.inventory.configuration.ConfigurationGwtService;
import org.rhq.enterprise.gui.coregui.client.inventory.configuration.ConfigurationGwtServiceAsync;
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
import com.smartgwt.client.types.TabBarControls;
import com.smartgwt.client.util.KeyCallback;
import com.smartgwt.client.util.Page;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuButton;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.events.ItemClickEvent;
import com.smartgwt.client.widgets.menu.events.ItemClickHandler;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;


/**
 * @author Greg Hinkle
 */
public class CoreGUI implements EntryPoint {
    ResourceGWTServiceAsync resourceService;
    ConfigurationGwtServiceAsync configurationService;

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
        configurationService = ConfigurationGwtService.App.getInstance();


        DOM.setInnerHTML(RootPanel.get("Loading-Panel").getElement(), "");


        final TabSet topTabSet = new TabSet();
        topTabSet.setTabBarPosition(Side.TOP);
        topTabSet.setWidth(1200);
        topTabSet.setHeight(900);


        Tab tableTab = new Tab("Resource Search Table");
        Tab treeTab = new Tab("Resource Tree");
        final Tab configTab = new Tab("Configuration Editor");


        // Agent:  resource (10005) type (10060)
        // Raw: 10003 / 10023
        // both:  10002 / 10022

        configTab.setPane(new ConfigurationEditor(10005, 10060));
        treeTab.setPane(new ResourceTreeView());
        tableTab.setPane(new ResourceSearchView());


        topTabSet.addTab(configTab);
        topTabSet.addTab(tableTab);
        topTabSet.addTab(treeTab);

        topTabSet.setTop(50);


        final Menu configSelectMenu = new Menu();
        configSelectMenu.addItem(new MenuItem("Agent"));
        configSelectMenu.addItem(new MenuItem("Raw Only"));
        configSelectMenu.addItem(new MenuItem("Structured and Raw"));
        configSelectMenu.addItem(new MenuItem("List Of Maps"));
        configSelectMenu.addItemClickHandler(new ItemClickHandler() {
            public void onItemClick(ItemClickEvent itemClickEvent) {
                int x = configSelectMenu.getItemNum(itemClickEvent.getItem());
                System.out.println("Loading: "+ x);
                topTabSet.removeTab(configTab);
                switch (x) {
                    case 0:
                        configTab.setPane(new ConfigurationEditor(10005, 10060));
                        break;
                    case 1:
                        configTab.setPane(new ConfigurationEditor(10003, 10023));
                        break;
                    case 2:
                        configTab.setPane(new ConfigurationEditor(10002, 10022));
                        break;
                    case 3:
                        configTab.setPane(new ConfigurationEditor(10149, 10134));
                        break;
                }
                topTabSet.addTab(configTab,0);
                topTabSet.selectTab(0);
                topTabSet.redraw();

            }
        });


        topTabSet.setTabBarControls(TabBarControls.TAB_SCROLLER, TabBarControls.TAB_PICKER, new MenuButton("Config Resource",configSelectMenu));


        topTabSet.draw();
    }


}


