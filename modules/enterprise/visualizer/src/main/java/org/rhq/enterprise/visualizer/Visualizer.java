/*
 * JBoss, a division of Red Hat.
 * Copyright 2008, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.enterprise.visualizer;


import ch.randelshofer.tree.DefaultNodeInfo;
import ch.randelshofer.tree.TreeNode;
import ch.randelshofer.tree.TreePath;
import ch.randelshofer.tree.TreeView;
import ch.randelshofer.tree.hypertree.HyperTree;
import ch.randelshofer.tree.sunburst.SunburstModel;
import org.jdesktop.swingx.JXFrame;
import org.jdesktop.swingx.JXLoginPane;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.JXTree;
import org.jdesktop.swingx.auth.DefaultUserNameStore;
import org.jdesktop.swingx.auth.LoginService;
import org.jdesktop.swingx.auth.PasswordStore;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerRemote;
import org.rhq.enterprise.server.report.DataAccessManagerRemote;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;
import org.rhq.enterprise.server.resource.ResourceTypeManagerRemote;
import org.rhq.enterprise.visualizer.gridview.GridView;
import org.rhq.enterprise.visualizer.tableview.TableView;

import javax.security.auth.login.LoginException;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * @author Greg Hinkle
 */
public class Visualizer {

    static Subject s;

    static RemoteClient client = new RemoteClient("localhost", 7080);

    static Map<Integer, RandelshoferTreeNodeResource> resources = new HashMap<Integer, RandelshoferTreeNodeResource>();

    static JSplitPane splitPaneCenter;
    static TreeView treeView;
    static JToolBar toolbar;
    static SelectionPathPanel selectionPathPanel;
    static JTree leftHandTree;
    static DefaultTreeModel jtreeModel;
    static Runnable updater;

    static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);

    public static Color COLOR_UP = new Color(152, 255, 128);
    public static Color COLOR_DOWN = new Color(255, 155, 128);


    private static Long lastCheck;

    private static RandelshoferTreeNodeResource rootNode = new RandelshoferTreeNodeResource(-1, "system", true, 0, -1);

    private static RandelshoferTreeNodeResource selectedNode;


    private static String host = "localhost";
    private int port = 7080;
    private static String user;
    private static String pass;

    private static final JXFrame frame = new JXFrame("test");

    private static ResourceDetailsPanel detailsPanel;

    public static RemoteClient getRemoteClient() {
        return client;
    }


    public enum ViewType {
        sunburst, hypertree, heatmap, gridview, tableview
    }


    private static ViewType currentViewType = ViewType.sunburst;

    public static boolean getResourceUpdates() throws Throwable {

        SubjectManagerRemote subjectManager = client.getSubjectManagerRemote();

        s = subjectManager.login(user, pass);

        DataAccessManagerRemote dataAccess = client.getDataAccessManagerRemote();

        String query = "select r.id, r.parentResource.id, r.name, a.availabilityType, a.startTime " +
                "FROM Resource r JOIN r.availability a with a.endTime is null";

        if (lastCheck != null) {
            query += " and a.startTime > " + lastCheck;
        }
        lastCheck = 0L;

        List<Object[]> result = dataAccess.executeQuery(s, query);

        boolean updates = false;

        for (Object[] row : result) {
            RandelshoferTreeNodeResource newNode =
                    new RandelshoferTreeNodeResource((Integer) row[0],
                            (String) row[2],
                            ((AvailabilityType) row[3]) == AvailabilityType.UP,
                            ((Number) row[4]).longValue(),
                            ((Integer) row[1] == null ? 0 : (Integer) row[1]));

            lastCheck = Math.max(lastCheck, newNode.lastAvailStart);

            if (!resources.containsKey(newNode.getId())) {
                updates = true;
                resources.put((Integer) row[0], newNode);
            } else {
                if (resources.get(newNode.getId()).up != newNode.up) {
                    resources.get(newNode.getId()).up = newNode.up;
                    updates = true;
                }
            }
        }

        final List<RandelshoferTreeNodeResource> roots = new ArrayList<RandelshoferTreeNodeResource>();
        for (RandelshoferTreeNodeResource res : resources.values()) {
            if (res.parentId != 0) {
                RandelshoferTreeNodeResource parent = resources.get(res.parentId);
                if (!parent.children.contains(res)) {
                    parent.children.add(res);
                    res.parent = parent;
                }
            } else {
                if (!rootNode.children.contains(res)) {
                    rootNode.children.add(res);
                    res.parent = rootNode;
                }
            }
        }

        for (RandelshoferTreeNodeResource res : rootNode.children) {
            setStuff(res);
        }

        return updates;
    }


    public static JPopupMenu getPopupMenuForResource(final RandelshoferTreeNodeResource node) {

        JPopupMenu menu = new JPopupMenu();

        menu.add(node.getName());
        menu.addSeparator();

        menu.add(new AbstractAction("Open In Browser") {
            public void actionPerformed(ActionEvent e) {
                String url = "http://" + client.getHost() + ":" + client.getPort() + "/resource/common/monitor/Visibility.do?mode=currentHealth&id=" + node.getId();
                try {
                    URI uri = new URI(url);
                    Desktop.getDesktop().browse(uri);
                } catch (URISyntaxException e1) {
                } catch (IOException e1) {
                }
            }
        });

        menu.add(new AbstractAction("Show Summary Graphs") {
            public void actionPerformed(ActionEvent e) {
                ResourceDetailsPanel.display(s, client, node);
            }
        });

        JMenu measurementsMenu = new JMenu("Individual Graphs");
        List<MeasurementDefinition> defs = getMeasurements(node);
        for (final MeasurementDefinition def : defs) {
            measurementsMenu.add(new AbstractAction(def.getDisplayName()) {
                public void actionPerformed(ActionEvent e) {
                    ResourceDetailsPanel.display(s, client, node, def.getName());
                }
            });
        }
        menu.add(measurementsMenu);

        menu.add(new PopupMenu());

        menu.addSeparator();
        menu.add(new AbstractAction("Show Events") {
            public void actionPerformed(ActionEvent e) {
                ResourceDetailsEventPanel.display(s, client, node);
            }
        });
        return menu;

    }


    public static List<MeasurementDefinition> getMeasurements(RandelshoferTreeNodeResource node) {
        ResourceManagerRemote resoruceManager = client.getResourceManagerRemote();
        ResourceTypeManagerRemote resourceTypeManager = client.getResourceTypeManagerRemote();
        MeasurementDefinitionManagerRemote measDef = client.getMeasurementDefinitionManagerRemote();
        DataAccessManagerRemote dataAccess = client.getDataAccessManagerRemote();

        Resource resource = resoruceManager.getResource(s, node.getId());

        ResourceType type = resourceTypeManager.getResourceTypeByNameAndPlugin(
                client.getSubject(),
                resource.getResourceType().getName(), resource.getResourceType().getPlugin());

        List typeData = dataAccess.executeQuery(s, "SELECT rt.id FROM ResourceType rt WHERE LOWER(rt.name) = LOWER('" +
                resource.getResourceType().getName() +
                "') AND rt.plugin = '" +
                resource.getResourceType().getPlugin() +
                "'");

        int typeId = ((Number) typeData.get(0)).intValue();

                MeasurementDefinitionCriteria criteria = new MeasurementDefinitionCriteria();
        criteria.addFilterResourceTypeId(typeId);
        criteria.addFilterDataType(DataType.MEASUREMENT);
        return measDef.findMeasurementDefinitionsByCriteria(s, criteria);
    }

    public static Resource getResource(int id) {
        if (id < 0) {
            return null;
        }
        ResourceManagerRemote resourceManager = null;
        try {
            resourceManager = client.getResourceManagerRemote();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        return resourceManager.getResource(s, id);
    }

    public static void main(String[] args) throws Throwable, LoginException {


        frame.setSize(800, 700);

        final RHQLoginService loginService = new RHQLoginService();

        List<String> servers = new ArrayList<String>();
        servers.add("localhost:7080");
        servers.add("localhost:7443");

        DefaultUserNameStore userNameStore = new DefaultUserNameStore();
        userNameStore.setPreferences(Preferences.userNodeForPackage(Visualizer.class).node("usr"));


        final JXLoginPane.JXLoginFrame loginFrame =
                JXLoginPane.showLoginFrame(loginService, new PreferencesPasswordStore(), userNameStore, servers);

        JXLoginPane loginPane = loginFrame.getPanel();
        Field scField = JXLoginPane.class.getDeclaredField("serverCombo");
        scField.setAccessible(true);
        JComboBox box = (JComboBox) scField.get(loginPane);
        box.setEditable(true);


        loginFrame.setAlwaysOnTop(true);
        loginFrame.setVisible(true);

        loginFrame.addWindowListener(new WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent e) {
                //make sure the login was successful
                if (loginFrame.getStatus() == JXLoginPane.Status.SUCCEEDED) {

                    user = loginFrame.getPanel().getUserName();
                    pass = new String(loginFrame.getPanel().getPassword());

                    JPanel view = null;
                    try {
                        init();
                        view = load();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }

                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.getContentPane().add(view);
                    frame.setVisible(true);
                    frame.setTitle("RHQ Visualizer - " + user + "@" + host);
                    frame.setIconImage(new ImageIcon(Visualizer.class.getResource("/images/rhq_icon.png")).getImage());
                }
            }
        });
    }

    public static void init() {

        toolbar = new JToolBar();
        toolbar.add(new AbstractAction("Sunburst View", getIcon("sunburst.gif")) {
            public void actionPerformed(ActionEvent e) {
                currentViewType = ViewType.sunburst;
                reload();
                System.out.println("SunburstView");
            }

            public boolean isEnabled() {
                return currentViewType != ViewType.sunburst;
            }
        });

        toolbar.add(new AbstractAction("HyperTree View", getIcon("hypertree.gif")) {
            public void actionPerformed(ActionEvent e) {
                currentViewType = ViewType.hypertree;
                reload();
                System.out.println("HyperTreeView");
            }

            public boolean isEnabled() {
                return currentViewType != ViewType.hypertree;
            }
        });

        toolbar.add(new AbstractAction("Grid View", getIcon("gridview.gif")) {
            public void actionPerformed(ActionEvent e) {
                currentViewType = ViewType.gridview;
                reload();
                System.out.println("GridView");
            }

            public boolean isEnabled() {
                return currentViewType != ViewType.gridview;
            }
        });

        toolbar.add(new AbstractAction("Table View", getIcon("tableview.gif")) {
            public void actionPerformed(ActionEvent e) {
                currentViewType = ViewType.tableview;
                reload();
                System.out.println("TableView");
            }

            public boolean isEnabled() {
                return currentViewType != ViewType.tableview;
            }
        });

        toolbar.add(new JToolBar.Separator());

        toolbar.add(new AbstractAction("Refresh", getIcon("refresh.png")) {
            public void actionPerformed(ActionEvent e) {
                try {
                    getResourceUpdates();
                    frame.validate();
                    frame.repaint();
                } catch (Throwable throwable) {

                }
            }
        });


        frame.setToolBar(toolbar);
//        view.add(toolbar, BorderLayout.NORTH);

        JXStatusBar statusBar = new JXStatusBar();
        JLabel statusLabel = new JLabel("Ready");
        JXStatusBar.Constraint c1 = new JXStatusBar.Constraint();
        c1.setFixedWidth(100);
        statusBar.add(statusLabel, c1);     // Fixed width of 100 with no inserts
        JXStatusBar.Constraint c2 = new JXStatusBar.Constraint(
                JXStatusBar.Constraint.ResizeBehavior.FILL); // Fill with no inserts
        JProgressBar pbar = new JProgressBar();
        statusBar.add(pbar, c2);            // Fill with no inserts - will use remaining space

        frame.setStatusBar(statusBar);

    }

    /**
     * Simple login service that allows everybody to login. This is useful in demos and allows
     * us to avoid having to check for LoginService being null
     */
    private static final class NullLoginService extends LoginService {
        public boolean authenticate(String name, char[] password, String server) throws Exception {
            return true;
        }
    }

    /**
     * Simple PasswordStore that does not remember passwords
     */
    private static final class PreferencesPasswordStore extends PasswordStore {
        private static final char[] EMPTY = new char[0];

        private Preferences p = Preferences.userNodeForPackage(Visualizer.class).node("pwd");

        public boolean set(String username, String server, char[] password) {
            p.put(username + "|" + server, new String(password));
            try {
                p.flush();
            } catch (BackingStoreException e) {
                e.printStackTrace();
            }
            return true;
        }

        public char[] get(String username, String server) {
            String pwd = p.get(username + "|" + server, "");
            return pwd.toCharArray();
        }
    }

    public static class RHQLoginService extends LoginService {

        public boolean authenticate(String s, char[] chars, String s1) throws Exception {


            Subject subject = client.login(s, new String(chars));

//            SubjectManagerRemote subjectManager = client.getSubjectManagerRemote();


//            Subject subject = subjectManager.login(s, new String(chars));
            user = s;
            pass = new String(chars);
            try {
                getResourceUpdates();
                return true;

            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            return false;
        }
    }

    public static void reload() {

        frame.setWaiting(true);
        try {
            setupVisualizerPane();

            for (Component c : toolbar.getComponents()) {
                if (c instanceof JButton) {
                    JButton b = (JButton) c;
                    Action a = b.getAction();
                    b.setAction(null);
                    b.setAction(a);
                }
            }

        } catch (Throwable throwable) {
            throwable.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        frame.setWaiting(false);
    }

    public static JPanel load() throws Throwable, ClassNotFoundException, LoginException {

        CustomPopupFactory.install();

        getResourceUpdates();
//

//
// put in a tree view with a JTree on the left and a JTreeMap on the right
//
        final JPanel view = new JPanel();
        view.setLayout(new BorderLayout());


        splitPaneCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        view.add(splitPaneCenter, BorderLayout.CENTER);

        selectionPathPanel = new SelectionPathPanel();
        view.add(selectionPathPanel, BorderLayout.SOUTH);

        JScrollPane leftTreeScrollPane = new JScrollPane();
        splitPaneCenter.setLeftComponent(leftTreeScrollPane);

        jtreeModel = new DefaultTreeModel(rootNode.getResourceTreeNode());
        leftHandTree = new JXTree(jtreeModel);
        leftHandTree.setPreferredSize(new Dimension(200, 500));
        leftHandTree.setCellRenderer(new DefaultTreeCellRenderer() {
            ImageIcon UP = new ImageIcon(Visualizer.class.getResource("/images/icon_available_green.gif"));
            ImageIcon DOWN = new ImageIcon(Visualizer.class.getResource("/images/icon_available_red.gif"));

            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                SwingTreeNodeResource node = (SwingTreeNodeResource) value;
                Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);    //To change body of overridden methods use File | Settings | File Templates.
                setIcon(node.simpleResource.isUp() ? UP : DOWN);
                return c;
            }
        });


        setupVisualizerPane();

        leftTreeScrollPane.getViewport().add(leftHandTree);
//        leftTreeScrollPane.setPreferredSize(new Dimension(140, jTreeMap.getRoot().getHeight()));
        leftHandTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                // for each selected elements ont the treeView, we zoom the JTreeMap
                if (leftHandTree.getLastSelectedPathComponent() != null) {
                    RandelshoferTreeNodeResource dest = ((SwingTreeNodeResource) leftHandTree.getLastSelectedPathComponent()).simpleResource;

                    updateSelection(dest);
                }
            }
        });

        leftHandTree.setExpandsSelectedPaths(true);
        leftHandTree.setScrollsOnExpand(true);


        selectionPathPanel.addPropertyChangeListener("selection", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                System.out.println("Selection is now: " + evt.getNewValue());
                RandelshoferTreeNodeResource node = (RandelshoferTreeNodeResource) evt.getNewValue();
                updateSelection(node);
            }
        });

        return view;
    }

    public static void updateSelection(RandelshoferTreeNodeResource node) {
        if (treeView != null) {
            treeView.setSelection(node);
        }
//        selectionPathlPanel.setSelectedNode(node);
        javax.swing.tree.TreePath selectionPath =
                node == null ? null : new javax.swing.tree.TreePath(jtreeModel.getPathToRoot(node.getResourceTreeNode()));

        leftHandTree.setSelectionPath(selectionPath);
        leftHandTree.scrollPathToVisible(selectionPath);

        frame.repaint();
    }


    public static void setupVisualizerPane() {
        switch (currentViewType) {
            case sunburst:
                SunburstModel sunburstTreeModel = new SunburstModel(rootNode, new VisualizerNodeInfo());
                treeView = sunburstTreeModel.getView();
                break;
            case hypertree:

                HyperTree hyperTreeModel = new HyperTree(rootNode, new VisualizerNodeInfo());
                treeView = hyperTreeModel.getView();
                break;
            case gridview:
                GridView gridView = new GridView(rootNode, null);
                treeView = gridView;
                break;
            case tableview:
                TableView tableView = new TableView(rootNode);
                treeView = tableView;
                break;
            default:
                treeView = null;
//        RectmapModel sunburstTreeModel = new RectmapModel(rootNode, info);
//        final RectmapView treeView = sunburstTreeModel.getView();
        }


        if (updater != null) {
            executor.remove(updater);
        }
        updater = new Runnable() {
            public void run() {
                try {
                    if (getResourceUpdates()) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                treeView.repaint();
                            }
                        });

                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };
        executor.scheduleWithFixedDelay(updater, 10, 30, TimeUnit.SECONDS);


        treeView.addPropertyChangeListener("selection", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                System.out.println("Selection is now: " + evt.getNewValue());
                RandelshoferTreeNodeResource node = (RandelshoferTreeNodeResource) evt.getNewValue();
                updateSelection(node);
            }
        });
        splitPaneCenter.setRightComponent(treeView.getComponent());
    }


    public static Icon getIcon(String name) {
        return new ImageIcon(Visualizer.class.getResource("/images/" + name));
    }

    private static void buildTree(RandelshoferTreeNodeResource parent, List<RandelshoferTreeNodeResource> children) {
        for (RandelshoferTreeNodeResource res : children) {
            parent.children.add(res);
            res.parent = parent;
        }
    }


    public static int setWeight(List<RandelshoferTreeNodeResource> resources) {
        int total = 1;
        int upCount = 0;
        for (RandelshoferTreeNodeResource res : resources) {
            int childWeight = setWeight(res.children);
            res.size = childWeight;
            total += childWeight;
            upCount += res.up ? 1 : 0;
        }

        for (RandelshoferTreeNodeResource res : resources) {
            res.weight = ((double) res.size) / total;
        }

        return total;
    }

    public static ScheduledThreadPoolExecutor getExecutor() {
        return executor;
    }


    public static JComponent getCustomToolTipComponent(TreeNode node) {

        RandelshoferTreeNodeResource res = ((RandelshoferTreeNodeResource) node);

        JPanel panel = new JPanel() {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
            }
        };
        panel.setLayout(new BorderLayout());

        StringBuilder info = new StringBuilder();
        info.append("<html><b>" + (res.isUp() ? "Up" : "Down") + " since: </b>");
        info.append(new Date(res.getLastAvailStart()));
        info.append("<br>");

        Resource resource = Visualizer.getResource(res.getId());

        info.append("<b>Type: </b>");
        info.append(resource.getResourceType().getName() + " :: " + resource.getResourceType().getPlugin() + "<br>");

        info.append("<b>Description: </b>");
        info.append(resource.getDescription() + "<br>");
        info.append("</html>");

        panel.add(new JLabel(info.toString()), BorderLayout.CENTER);
        String icon = res.isUp() ? "/images/icon_available_green.gif" : "/images/icon_available_red.gif";
        panel.add(new JLabel(res.getName(), getIcon(icon), JLabel.LEFT), BorderLayout.NORTH);

        return panel;
    }

    public static void setStuff(RandelshoferTreeNodeResource res) {
        if (res.children.size() == 0) {
            res.descendents = 0;
            res.upness = res.up ? 1 : 0;
        } else {
            double upness = 0;
            for (RandelshoferTreeNodeResource child : res.children) {
                setStuff(child);
                res.descendents += child.descendents + 1;
                upness += res.upness * (child.descendents + 1);
            }
            res.upness = (upness + (res.up ? 1 : 0)) / (res.descendents + 1);
        }
    }

    public static class VisualizerNodeInfo extends DefaultNodeInfo {
        public void init(TreeNode treeNode) {
        }

        public Color getColor(TreePath<TreeNode> treeNodeTreePath) {
            Color COLOR_UP = new Color(152, 255, 128);
            Color COLOR_DOWN = new Color(255, 155, 128);
            return ((RandelshoferTreeNodeResource) treeNodeTreePath.getLastPathComponent()).up ? COLOR_UP : COLOR_DOWN;
        }

        public String getName(TreePath<TreeNode> treeNodeTreePath) {
            return ((RandelshoferTreeNodeResource) treeNodeTreePath.getLastPathComponent()).name;
        }

        public String getTooltip(TreePath<TreeNode> treeNodeTreePath) {
            RandelshoferTreeNodeResource resourceNode = (RandelshoferTreeNodeResource) treeNodeTreePath.getLastPathComponent();


            return getToolTipText(resourceNode);
        }

    }

    public static String getToolTipText(RandelshoferTreeNodeResource resourceNode) {
        Resource resource = getResource(resourceNode.id);
        if (resource != null) {

            StringBuilder info = new StringBuilder();
            info.append("<html><b>" + (resourceNode.isUp() ? "Up" : "Down") + " since: </b>");
            info.append(new Date(resourceNode.getLastAvailStart()));
            info.append("<br>");

            info.append("<b>Name: </b>");
            info.append(resource.getName() + "<br>");

            info.append("<b>Type: </b>");
            info.append(resource.getResourceType().getName() + " :: " + resource.getResourceType().getPlugin() + "<br>");

            info.append("<b>Description: </b>");
            info.append(resource.getDescription() + "<br>");

            info.append("<b>Key: </b>");
            info.append(resource.getResourceKey() + "<br>");


            info.append("</html>");

            return info.toString();
        }
        return null;
    }

}