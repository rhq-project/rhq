package org.rhq.enterprise.gui.coregui.client;

import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceGWTService;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceTreeDatasource;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Side;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.WidgetCanvas;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.menu.events.MenuItemClickEvent;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;
import com.smartgwt.client.widgets.tree.TreeGrid;
import com.smartgwt.client.widgets.tree.events.NodeContextClickEvent;
import com.smartgwt.client.widgets.tree.events.NodeContextClickHandler;


/**
 * @author Greg Hinkle
 */
public class CoreGUI implements EntryPoint {
  ResourceGWTServiceAsync resourceService;


    public void onModuleLoad() {
        resourceService = ResourceGWTService.App.getInstance();



        DOM.setInnerHTML(RootPanel.get("Loading-Panel").getElement(), "");


        final TabSet topTabSet = new TabSet();
        topTabSet.setTabBarPosition(Side.TOP);
        topTabSet.setWidth100();
        topTabSet.setHeight100();


        Tab tableTab = new Tab("Resource Search Table");

        Tab treeTab = new Tab("Resource Tree");



        TreeGrid resourceTree = new TreeGrid();
        resourceTree.setWidth(500);
        resourceTree.setHeight100();
        resourceTree.setNodeIcon("icons/16/person.png");
        resourceTree.setFolderIcon("icons/16/person.png");
        resourceTree.setShowOpenIcons(false);
        resourceTree.setShowDropIcons(false);
        resourceTree.setClosedIconSuffix("");
        resourceTree.setAutoFetchData(true);
        resourceTree.setAnimateFolders(false);
        resourceTree.setDataSource(new ResourceTreeDatasource());
        treeTab.setPane(new WidgetCanvas(resourceTree));
//        ((Layout)treeTab.getPane()).add.addChild(resourceTree);

        final Menu contextMenu = new Menu();
        MenuItem item = new MenuItem("Expand node");
        item.addClickHandler(new com.smartgwt.client.widgets.menu.events.ClickHandler() {
            public void onClick(MenuItemClickEvent event) {
                TreeGrid treeGrid = (TreeGrid)event.getTarget();
                System.out.println("You right clicked: " + treeGrid.getSelectedRecord());
            }
        });


        /* Do menu support datasources? GH: Seemingly they'll only load once when done this way
        contextMenu.setDataSource(new DataSource() {
            {
                setClientOnly(false);
                setDataProtocol(DSProtocol.CLIENTCUSTOM);
                setDataFormat(DSDataFormat.CUSTOM);
            }
            protected Object transformRequest(DSRequest request) {
                System.out.println("Looking up menu info");
                return request;
            }
        });
        */

        
//        contextMenu.addItem(item);
        resourceTree.setContextMenu(contextMenu);
        resourceTree.addNodeContextClickHandler(new NodeContextClickHandler() {
            public void onNodeContextClick(NodeContextClickEvent nodeContextClickEvent) {
                nodeContextClickEvent.getNode();
                contextMenu.setItems(new MenuItem(nodeContextClickEvent.getNode().getName()));
                if (nodeContextClickEvent.getNode() instanceof ResourceTreeDatasource.ResourceTreeNode) {
                    contextMenu.addItem(new MenuItem("Type: " + ((ResourceTreeDatasource.ResourceTreeNode)nodeContextClickEvent.getNode()).getResourceType().getName()));
                }
                contextMenu.addItem(new MenuItemSeparator());
                MenuItem operations = new MenuItem("Operations");
                Menu opSubMenu = new Menu();
                opSubMenu.setItems(new MenuItem("Start"), new MenuItem("Stop"), new MenuItem("Restart"));
                operations.setSubmenu(opSubMenu);
                contextMenu.addItem(operations);
                contextMenu.showContextMenu();
            }
        });





        VerticalPanel resourceSearch = new VerticalPanel();
        HorizontalPanel searchPanel;
        final TextBox searchBox = new TextBox();
        searchBox.setText("agent");

        searchPanel = new HorizontalPanel();
        searchPanel.add(new Label("Resource Search"));
        searchPanel.add(searchBox);

        resourceSearch.add(searchPanel);


        final ResourceDatasource datasource = new ResourceDatasource();
        final ListGrid listGrid = new ListGrid();
        listGrid.setWidth(1000);
        listGrid.setHeight(800);
        listGrid.setDataSource(datasource);
        listGrid.setAutoFetchData(true);
        listGrid.setAlternateRecordStyles(true);
//        listGrid.setAutoFitData(Autofit.HORIZONTAL);
//        listGrid.getField("currentAvailability").setAlign(Alignment.CENTER);
        resourceSearch.add(listGrid);

        
        searchBox.addKeyPressHandler(new KeyPressHandler() {
            public void onKeyPress(KeyPressEvent event) {
                if (event.getCharCode() == KeyCodes.KEY_ENTER) {
                    datasource.setQuery(searchBox.getText());
                    Criteria c = new Criteria("name", searchBox.getText());
                    long start = System.currentTimeMillis();
                    listGrid.fetchData(c);
                    System.out.println("Loaded in: " + (System.currentTimeMillis() - start));
                }
            }
        });


        tableTab.setPane(new WidgetCanvas(resourceSearch));//resourceSearch); //.addChild(resourceSearch);


        /*

        IButton button = new IButton("say hello");
        button.moveTo(300,50);
        button.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                SC.say("oh, Hi there");
                
            }
        });
        button.draw();*/



//        RootPanel.get().add(topTabSet);

        topTabSet.addTab(tableTab);
        topTabSet.addTab(treeTab);

        topTabSet.draw();
    }



/*
This displays a core gwt table
    public void updateSearch(String searchString) {
        final long start = System.currentTimeMillis();
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterName(searchString);

        resourceService.findResourcesByCriteria(criteria, new AsyncCallback<PageList<Resource>>() {
            public void onFailure(Throwable caught) {
                Window.alert("Failed to load " + caught.getMessage());
            }

            public void onSuccess(PageList<Resource> result) {
                mainPanel.clear();

                FlexTable table = new FlexTable();
                mainPanel.add(table);

                int r = 0;
                int c = 0;

                table.setText(r,c++,"id");
                table.setText(r,c++,"name");
                table.setText(r,c++,"description");
                table.setText(r,c++,"type");
                table.setText(r,c++,"availability");

                mainPanel.add(table);

                for (Resource res : result) {
                    r++;
                    c=0;
                    table.setText(r,c++, String.valueOf(res.getId()));
                    table.setText(r,c++, res.getName());
                    table.setText(r,c++, res.getDescription());
                    table.setText(r,c++, String.valueOf(res.getResourceType().getId()));
                    table.setText(r,c++, String.valueOf(res.getCurrentAvailability().getAvailabilityType().getName()));

                }

                mainPanel.add(new Label("Found " + result.size() + " resources"));
                mainPanel.add(new Label("\tTook: " + (System.currentTimeMillis() - start) + "ms"));
            }
        });
    }
    */
}
