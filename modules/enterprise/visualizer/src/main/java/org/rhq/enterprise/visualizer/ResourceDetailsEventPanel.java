/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.visualizer;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.event.composite.EventComposite;
import org.rhq.enterprise.client.RemoteClient;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Greg Hinkle
 */
public class ResourceDetailsEventPanel extends JPanel {


    RemoteClient client;
    RandelshoferTreeNodeResource node;
    Subject subject;
    int resourceId;
    JXTable eventTable;
EventTableModel model;
    boolean running = true;

    public ResourceDetailsEventPanel(Subject subject, RemoteClient client, RandelshoferTreeNodeResource node) {
        this.subject = subject;
        this.client = client;
        this.node = node;
        this.resourceId = node.getId();
        init();
        setPreferredSize(new Dimension(270, 500));
    }


    public static void display(Subject subject, RemoteClient client, RandelshoferTreeNodeResource node) {

        JFrame frame = new JFrame(node.getName() + " Events");
        frame.setSize(600, 400);


        frame.setContentPane(new ResourceDetailsEventPanel(subject, client, node));
        frame.setVisible(true);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    }


    public void init() {

        model = new EventTableModel();
        model.reload();
        eventTable = new JXTable(model);

        eventTable.setColumnControlVisible(true);
        eventTable.setHighlighters(new ColorHighlighter() {
            protected Component doHighlight(Component component, ComponentAdapter componentAdapter) {
                int realRow = eventTable.convertRowIndexToModel(componentAdapter.row);
                EventSeverity sev = (EventSeverity) componentAdapter.getValueAt(realRow, 0);
                Color c = null;
                switch (sev) {
                    case DEBUG:
                        c = Color.blue;
                        break;
                    case INFO:
                        c = Color.green;
                        break;
                    case WARN:
                        c = Color.yellow;
                        break;
                    case ERROR:
                        c = Color.orange;
                        break;
                    case FATAL:
                        c = Color.red;
                        break;
                }

                c = new Color(c.getRed(),c.getGreen(), c.getBlue(),50);
                component = super.doHighlight(component, componentAdapter);
                component.setBackground(c);
                return component;

            }
        });

        eventTable.setSortOrder(2, org.jdesktop.swingx.decorator.SortOrder.DESCENDING);
        eventTable.getColumn(2).setCellRenderer(new DateCellRenderer());
        JScrollPane scrollPane = new JScrollPane(eventTable);
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

        Thread t = new Thread(new Runnable() {
            public void run() {
                while (running) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    refresh();
                }
            }
        });
        t.start();
    }


    public void refresh() {
        this.model.reload();
        repaint();
    }

    private class EventTableModel extends AbstractTableModel {
        public final String[] COLUMNS = {"Severity", "Detail", "Timestamp"};

        LinkedList<EventComposite> data = new LinkedList<EventComposite>();
        long lastUpdate;

        public void reload() {

            long now = System.currentTimeMillis();
            if (lastUpdate == 0) {
                lastUpdate = now - (1000L * 60 * 60);
            }


            String query =
                    "SELECT new org.rhq.core.domain.event.composite.EventComposite(" +
                            "  ev.detail, ev.id, substr(evs.location, 1, 30), ev.severity, " +
                            "  ev.timestamp, evs.resourceId, ev.ackUser, ev.ackTime)\n" +
                            "FROM Event ev JOIN ev.source evs\n" +
                            "WHERE evs.resourceId = " + resourceId + "\n" +
                            "  and ev.timestamp between " + lastUpdate + " and " + now;

            List events = client.getDataAccessManagerRemote().executeQuery(subject, query);
            System.out.println("New events: " + events.size());
            data.addAll(events);

            lastUpdate = now;

            fireTableDataChanged();

        }


        public int getRowCount() {
            return data.size();
        }

        public String getColumnName(int columnIndex) {
            return COLUMNS[columnIndex];
        }

        public int getColumnCount() {
            return COLUMNS.length;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            Object val = null;
            EventComposite e = data.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    val = e.getSeverity();
                    break;
                case 1:
                    val = e.getEventDetail();
                    break;
                case 2:
                    val = e.getTimestamp();
                    break;
            }
            return val;
        }
    }

    public static class DateCellRenderer extends DefaultTableCellRenderer {
        // implements javax.swing.table.TableCellRenderer
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);    //To change body of overridden methods use File | Settings | File Templates.

            setText(new RelativeDateFormatter().format((Date)value));
            return c;
        }
    }

    public static class RelativeDateFormatter extends DateFormat {
        final public static String DATE_FORMAT = "dd-MMM-yyyy";
        final public static String TIME_FORMAT = "hh:mm:ss a";

        public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
            return new StringBuffer(formatRelativeDate(date, true));
        }

        public Date parse(String source, ParsePosition pos) {
            return null;
        }

        public static String formatRelativeDate(final Date date, final boolean showTime) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            Calendar now = Calendar.getInstance();
            final int today = now.get(Calendar.DAY_OF_YEAR);
            final SimpleDateFormat timeFormat = new SimpleDateFormat(" " + TIME_FORMAT);
            final String time = showTime ? timeFormat.format(calendar.getTime()) : "";

            if (now.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)) {
                if (today == calendar.get(Calendar.DAY_OF_YEAR)) {
                    return "Today" + time;
                } else if (today - 1 == calendar.get(Calendar.DAY_OF_YEAR)) {
                    return "Yesterday" + time;
                } else if (today + 1 == calendar.get(Calendar.DAY_OF_YEAR)) {
                    return "Tomorrow" + time;
                }
            }
            final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
            return dateFormat.format(date) + time;
        }
    }

}
