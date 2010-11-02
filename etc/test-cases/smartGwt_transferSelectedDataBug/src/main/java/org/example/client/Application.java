package org.example.client;

import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSource;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.rpc.RPCResponse;
import com.smartgwt.client.types.DSDataFormat;
import com.smartgwt.client.types.DSProtocol;
import com.smartgwt.client.types.DragDataAction;
import com.smartgwt.client.types.DragTrackerMode;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.TransferImgButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.events.DoubleClickEvent;
import com.smartgwt.client.widgets.events.DoubleClickHandler;
import com.smartgwt.client.widgets.events.KeyPressEvent;
import com.smartgwt.client.widgets.events.KeyPressHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.RecordDropEvent;
import com.smartgwt.client.widgets.grid.events.RecordDropHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VStack;

/**
 * Entry point for test SmartGWT GWT module.
 */
public class Application implements EntryPoint {

    private HLayout hLayout;

    private Set<Integer> selection = new HashSet<Integer>();

    private ListGridRecord[] initialSelection;

    private ListGrid leftGrid;
    private ListGrid rightGrid;
    private DataSource dataSource;

    private TransferImgButton addButton;
    private TransferImgButton removeButton;
    private TransferImgButton addAllButton;
    private TransferImgButton removeAllButton;

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
            public void onUncaughtException(Throwable t) {
                System.err.println("--- UNCAUGHT EXCEPTION ---");
                t.printStackTrace();
            }
        });
        
        hLayout = new HLayout();
        hLayout.setMargin(10);

        // LEFT SIDE
        SectionStack leftSectionStack = new SectionStack();
        leftSectionStack.setWidth(250);
        leftSectionStack.setHeight(250);

        SectionStackSection leftSection = new SectionStackSection("Available Items");
        leftSection.setCanCollapse(false);
        leftSection.setExpanded(true);

        leftGrid = new ListGrid();
        leftGrid.setCanDragRecordsOut(true);
        leftGrid.setCanAcceptDroppedRecords(true);
        leftGrid.setDragTrackerMode(DragTrackerMode.ICON);
        leftGrid.setDragDataAction(DragDataAction.COPY);

        dataSource = new ApplicationDataSource();
        leftGrid.setDataSource(dataSource);
        leftGrid.setFetchDelay(700);
        leftGrid.setAutoFetchData(true);
        leftGrid.setFields(new ListGridField("name", "Name"));

        leftSection.setItems(leftGrid);
        leftSectionStack.setSections(leftSection);
        hLayout.addMember(leftSectionStack);

        // CENTER BUTTONS
        VStack moveButtonStack = new VStack(6);
        moveButtonStack.setAlign(VerticalAlignment.CENTER);
        moveButtonStack.setWidth(40);

        addButton = new TransferImgButton(TransferImgButton.RIGHT);
        addButton.setDisabled(true);
        removeButton = new TransferImgButton(TransferImgButton.LEFT);
        removeButton.setDisabled(true);
        addAllButton = new TransferImgButton(TransferImgButton.RIGHT_ALL);
        addAllButton.setDisabled(true);
        removeAllButton = new TransferImgButton(TransferImgButton.LEFT_ALL);
        removeAllButton.setDisabled(true);

        moveButtonStack.addMember(addButton);
        moveButtonStack.addMember(removeButton);
        moveButtonStack.addMember(addAllButton);
        moveButtonStack.addMember(removeAllButton);

        hLayout.addMember(moveButtonStack);

        // RIGHT SIDE
        SectionStack rightSectionStack = new SectionStack();
        rightSectionStack.setWidth(250);
        rightSectionStack.setHeight(250);

        SectionStackSection rightSection = new SectionStackSection("Assigned Items");
        rightSection.setCanCollapse(false);
        rightSection.setExpanded(true);

        rightGrid = new ListGrid();
        rightGrid.setCanReorderRecords(true);
        rightGrid.setCanDragRecordsOut(true);
        rightGrid.setCanAcceptDroppedRecords(true);
        rightGrid.setDragTrackerMode(DragTrackerMode.ICON);
                
        rightGrid.setFields(new ListGridField("name", "Name"));

        rightSection.setItems(rightGrid);
        rightSectionStack.setSections(rightSection);
        hLayout.addMember(rightSectionStack);

        addButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                rightGrid.transferSelectedData(leftGrid);
                select(rightGrid.getSelection());
                updateButtons();
            }
        });

        removeButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                deselect(rightGrid.getSelection());
                rightGrid.removeSelectedData();
                updateButtons();
            }
        });
        addAllButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                leftGrid.selectAllRecords();
                rightGrid.transferSelectedData(leftGrid);
                select(leftGrid.getSelection());
                updateButtons();
            }
        });
        removeAllButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                rightGrid.selectAllRecords();
                deselect(rightGrid.getSelection());
                rightGrid.removeSelectedData();
                updateButtons();
            }
        });

        leftGrid.addDoubleClickHandler(new DoubleClickHandler() {
            public void onDoubleClick(DoubleClickEvent event) {
                rightGrid.transferSelectedData(leftGrid);
                select(rightGrid.getSelection());
                updateButtons();
            }
        });

        rightGrid.addDoubleClickHandler(new DoubleClickHandler() {
            public void onDoubleClick(DoubleClickEvent event) {
                deselect(rightGrid.getSelection());
                rightGrid.removeSelectedData();
                updateButtons();
            }
        });

        leftGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                updateButtons();
            }
        });

        rightGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                updateButtons();
            }
        });

        rightGrid.addKeyPressHandler(new KeyPressHandler() {
            public void onKeyPress(KeyPressEvent event) {
                if ("Delete".equals(event.getKeyName())) {
                    deselect(rightGrid.getSelection());
                    rightGrid.removeSelectedData();
                }
            }
        });

        leftGrid.addRecordDropHandler(new RecordDropHandler() {
            public void onRecordDrop(RecordDropEvent recordDropEvent) {
                deselect(recordDropEvent.getDropRecords());
            }
        });

        rightGrid.addRecordDropHandler(new RecordDropHandler() {
            public void onRecordDrop(RecordDropEvent recordDropEvent) {
                select(recordDropEvent.getDropRecords());
            }
        });

        if (initialSelection != null) {
            rightGrid.setData(initialSelection);
            for (ListGridRecord record : initialSelection) {
                selection.add(record.getAttributeAsInt("id"));
            }
        }

        updateButtons(); // initialize their state

        hLayout.draw();
    }

    protected void updateButtons() {
        addButton.setDisabled(!leftGrid.anySelected() || leftGrid.getTotalRows() == 0);
        removeButton.setDisabled(!rightGrid.anySelected() || rightGrid.getTotalRows() == 0);
        addAllButton.setDisabled(leftGrid.getTotalRows() == 0);
        removeAllButton.setDisabled(rightGrid.getTotalRows() == 0);
    }

    protected void select(ListGridRecord[] records) {
        leftGrid.deselectAllRecords();
        for (ListGridRecord record : records) {
            record.setEnabled(false);
            selection.add(record.getAttributeAsInt("id"));
        }
        rightGrid.markForRedraw();
    }

    protected void deselect(ListGridRecord[] records) {
        HashSet<Integer> toRemove = new HashSet<Integer>();
        for (ListGridRecord record : records) {
            toRemove.add(record.getAttributeAsInt("id"));
        }
        selection.removeAll(toRemove);

        for (Integer id : toRemove) {
            Record record = leftGrid.getDataAsRecordList().find("id", id);
            if (record != null) {
                ((ListGridRecord)record).setEnabled(true);
            }
        }
        leftGrid.markForRedraw();
    }

    private class ApplicationDataSource extends DataSource {
        private ApplicationDataSource() {
            setDataProtocol(DSProtocol.CLIENTCUSTOM);
            setDataFormat(DSDataFormat.CUSTOM);

            DataSourceIntegerField idField = new DataSourceIntegerField("id");
            idField.setPrimaryKey(true);
            idField.setHidden(true);
            addField(idField);
        }

        @Override
        protected Object transformRequest(DSRequest request) {
            try {
                DSResponse response = new DSResponse();
                response.setAttribute("clientContext", request.getAttributeAsObject("clientContext"));
                response.setStatus(0);

                switch (request.getOperationType()) {
                    case FETCH:
                        response.setStatus(RPCResponse.STATUS_SUCCESS);

                        ListGridRecord[] records = createRecords();

                        response.setData(records);
                        response.setTotalRows(records.length);
                        processResponse(request.getRequestId(), response);
                        break;
                    default:
                        super.transformRequest(request);
                        break;
                }
            }
            catch (Throwable t) {
                System.err.println("error");
                return null;
            }
            return request.getData();
        }

        private ListGridRecord[] createRecords() {
            int id = 0;

            ListGridRecord record0 = new ListGridRecord();
            record0.setAttribute("id", id++);
            record0.setAttribute("name", "Larry");

            ListGridRecord record1 = new ListGridRecord();
            record1.setAttribute("id", id++);
            record1.setAttribute("name", "Moe");

            ListGridRecord record2 = new ListGridRecord();
            record2.setAttribute("id", id++);
            record2.setAttribute("name", "Curly");

            ListGridRecord[] records = new ListGridRecord[]{
                record0, record1, record2
            };
            return records;
        }
    }
}

