package org.rhq.enterprise.gui.coregui.client;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceGWTService;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceTreeDatasource;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;


import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.grid.ColumnTree;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.tree.TreeGrid;


/**
 * @author Greg Hinkle
 */
public class CoreGUI implements EntryPoint {
  ResourceGWTServiceAsync resourceService;

    private VerticalPanel mainPanel;
    private HorizontalPanel searchPanel;
    private TextBox searchBox;

    public void onModuleLoad() {
        resourceService = ResourceGWTService.App.getInstance();

        mainPanel = new VerticalPanel();


        DOM.setInnerHTML(RootPanel.get("Loading-Panel").getElement(), "");


        searchPanel = new HorizontalPanel();
        searchPanel.add(new Label("Resource Search"));
        searchBox = new TextBox();
        searchPanel.add(searchBox);

        RootPanel.get().add(searchPanel);

        RootPanel.get().add(mainPanel);

//        TreeGrid resourceTree = new TreeGrid();
//        resourceTree.setWidth(500);
//        resourceTree.setHeight(400);
//        resourceTree.setNodeIcon("icons/16/person.png");
//        resourceTree.setFolderIcon("icons/16/person.png");
//        resourceTree.setShowOpenIcons(false);
//        resourceTree.setShowDropIcons(false);
//        resourceTree.setClosedIconSuffix("");
//        resourceTree.setAutoFetchData(true);
//        resourceTree.setDataSource(new ResourceTreeDatasource());
//        mainPanel.add(resourceTree);


        final ResourceDatasource datasource = new ResourceDatasource();
        final ListGrid listGrid = new ListGrid();
        listGrid.setWidth(1000);
        listGrid.setHeight(800);
        listGrid.setDataSource(datasource);
        listGrid.setAutoFetchData(true);
        listGrid.setAlternateRecordStyles(true);
//        listGrid.setAutoFitData(Autofit.HORIZONTAL);
//        listGrid.getField("currentAvailability").setAlign(Alignment.CENTER);
        mainPanel.add(listGrid);

        
        searchBox.addKeyPressHandler(new KeyPressHandler() {
            public void onKeyPress(KeyPressEvent event) {
                if (event.getCharCode() == KeyCodes.KEY_ENTER) {
                    datasource.setQuery(searchBox.getText());
                    Criteria c = new Criteria("name", searchBox.getText());
                    listGrid.fetchData(c);
                }
            }
        });

        IButton button = new IButton("say hello");
        button.moveTo(300,50);
        button.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                SC.say("oh, Hi there");
                
            }
        });
        button.draw();
    }




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
}
