/*
 * JBoss, a division of Red Hat.
 * Copyright 2008, Red Hat Middleware, LLC. All rights reserved.
 */

package org.rhq.enterprise.visualizer;


import ch.randelshofer.tree.DefaultNodeInfo;
import ch.randelshofer.tree.NodeInfo;
import ch.randelshofer.tree.TreeNode;
import ch.randelshofer.tree.TreePath;
import ch.randelshofer.tree.sunburst.SunburstModel;
import ch.randelshofer.tree.sunburst.SunburstView;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.client.RemoteClient;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.auth.SubjectManagerRemote;
import org.rhq.enterprise.server.report.DataAccessManagerRemote;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceManagerRemote;
import org.rhq.enterprise.server.util.LookupUtil;

import javax.security.auth.login.LoginException;
import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/**
 * @author Greg Hinkle
 */
public class HeatMapTreeVizOld {

    public static final String SQL =
            "select r.id, r.parent_resource_id, r.name, a.availability_type \n" +
                    "from rhq_resource r join rhq_availability a on r.id = a.resource_id\n" +
                    "where  a.end_time is null";

    static Subject s;

    static RemoteClient client = new RemoteClient("localhost", 7080);


    public static Map<Integer, SimpleResource> getResourcesViaJDBC() throws SQLException, ClassNotFoundException {
        Class.forName("org.postgresql.Driver");
//        Connection c = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/rhq", "rhqadmin", "rhqadmin");
        Connection c = DriverManager.getConnection("jdbc:postgresql://jon04.qa.atl2.redhat.com:5432/rhq", "rhqadmin", "rhqadmin");

//        Class.forName("oracle.jdbc.driver.OracleDriver");
//        Connection c = DriverManager.getConnection("jdbc:oracle:thin:@jon05.qa.atl2.redhat.com:1521:jon", "jon", "jon");

        ResultSet rset = c.createStatement().executeQuery(SQL);

        Map<Integer, SimpleResource> resources = new HashMap<Integer, SimpleResource>();
        while (rset.next()) {
            resources.put(
                    new Integer(rset.getInt(1)),
                    new SimpleResource(rset.getInt(1), rset.getString(3), rset.getInt(4) == 1, rset.getInt(2)));
        }
        return resources;
    }

    public static Map<Integer, SimpleResource> getResourcesViaServer() throws LoginException {

//        SubjectManagerRemote smr = LookupUtil.getSubjectManagerRemote();
//        s = smr.login("rhqadmin","rhqadmin");


        DataAccessManagerRemote dataAccess = client.getDataAccessManagerRemote();
        Map<Integer, SimpleResource> resources = new HashMap<Integer, SimpleResource>();


        List<Object[]> result = dataAccess.executeQuery(s, "select r.id, r.parentResource.id, r.name, a.availabilityType " +
                "FROM Resource r JOIN r.availability a with a.endTime is null");
        for (Object[] row : result) {
            resources.put(
                    (Integer) row[0],
                    new SimpleResource((Integer) row[0],
                            (String) row[2],
                            ((AvailabilityType) row[3]) == AvailabilityType.UP,
                            ((Integer) row[1] == null ? 0 : (Integer) row[1])));

        }
        return resources;
    }

    public static Map<Integer, SimpleResource> getResourcesViaServer2() throws Throwable {

        SubjectManagerRemote subjectManager = client.getSubjectManagerRemote();

        s = subjectManager.login("rhqadmin", "rhqadmin");


        DataAccessManagerRemote dataAccess = client.getDataAccessManagerRemote();
        Map<Integer, SimpleResource> resources = new HashMap<Integer, SimpleResource>();


        List<Object[]> result = dataAccess.executeQuery(s,
                "select r.id, r.parentResource.id, r.name, a.availabilityType " +
                        "FROM Resource r JOIN r.availability a with a.endTime is null");
        for (Object[] row : result) {
            resources.put(
                    (Integer) row[0],
                    new SimpleResource((Integer) row[0],
                            (String) row[2],
                            ((AvailabilityType) row[3]) == AvailabilityType.UP,
                            ((Integer) row[1] == null ? 0 : (Integer) row[1])));

        }
        return resources;
    }

    public static Resource getResource(int id) {
        ResourceManagerRemote resourceManager = null;
        try {
            resourceManager = client.getResourceManagerRemote();
        } catch (Throwable throwable) {
            throwable.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
//        ResourceManagerRemote remote= LookupUtil.getResourceManagerRemote();
        //getDataAccess();
        return resourceManager.getResource(s, id);
    }

    public static void main(String[] args) throws Throwable, ClassNotFoundException, LoginException {


        Map<Integer, SimpleResource> resources = getResourcesViaServer2();

        final List<SimpleResource> roots = new ArrayList<SimpleResource>();
        for (SimpleResource res : resources.values()) {
            if (res.parentId != 0) {
                SimpleResource parent = resources.get(res.parentId);
                parent.children.add(res);
            } else {
                roots.add(res);
            }
        }

        setWeight(roots);
        for (SimpleResource res : roots) {
            setStuff(res);
        }


        JFrame frame = new JFrame("test");
        frame.setSize(600, 600);

//
// Build the Tree with JTreeMap classes
//

        SimpleResource rootNode = new SimpleResource(-1, "system", true, -1);

        buildTree(rootNode, roots);

//
// Build the JTreeMap

        NodeInfo info = new DefaultNodeInfo() {
            public void init(TreeNode treeNode) {
            }

            public Color getColor(TreePath<TreeNode> treeNodeTreePath) {
                return ((SimpleResource) treeNodeTreePath.getLastPathComponent()).up ? Color.green : Color.red;
            }

            public String getName(TreePath<TreeNode> treeNodeTreePath) {
                return ((SimpleResource) treeNodeTreePath.getLastPathComponent()).name;
            }

            public String getTooltip(TreePath<TreeNode> treeNodeTreePath) {
                return getResource(((SimpleResource) treeNodeTreePath.getLastPathComponent()).id).toString();
            }
        };

        SunburstModel treeModel = new SunburstModel(rootNode, info);
        SunburstView treeView = treeModel.getView();

//        HyperTree treeModel = new HyperTree(rootNode,info);
//        SwingHTView treeView = treeModel.getView();

//        RectmapModel treeModel = new RectmapModel(rootNode, info);
//        RectmapView treeView = treeModel.getView();

//

//
// put in a tree view with a JTree on the left and a JTreeMap on the right
//
        JPanel view = new JPanel();
        view.setLayout(new BorderLayout());

        JSplitPane splitPaneCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        view.add(splitPaneCenter, BorderLayout.CENTER);


        JScrollPane jScrollPane1 = new JScrollPane();
        splitPaneCenter.setLeftComponent(jScrollPane1);
        splitPaneCenter.setRightComponent(treeView);

        DefaultTreeModel jtreeModel = new DefaultTreeModel(rootNode.getResourceTreeNode());
        final JTree jtreeView = new JTree(jtreeModel);
        jtreeView.setCellRenderer(new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                ResourceTreeNode node = (ResourceTreeNode) value;
                Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);    //To change body of overridden methods use File | Settings | File Templates.
                if (node.simpleResource.up)
                    c.setForeground(Color.green);
                else
                    c.setForeground(Color.red);
                return c;
            }
        });
        jScrollPane1.getViewport().add(jtreeView);
//        jScrollPane1.setPreferredSize(new Dimension(140, jTreeMap.getRoot().getHeight()));
        jtreeView.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                // for each selected elements ont the treeView, we zoom the JTreeMap
                ResourceTreeNode dest = (ResourceTreeNode) jtreeView.getLastSelectedPathComponent();

                // if the element is a leaf, we select the parent
                /*treeView.setSelectedNode();
                if (dest != null && dest.isLeaf()) {
                    dest = (TreeMapNode) dest.getParent();
                }
                if (dest == null) {
                    return;
                }
                treeModel.getInfo().

                jTreeMap.zoom(dest);
                jTreeMap.repaint();*/
            }
        });
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(view);
        frame.setVisible(true);
    }

    private static void buildTree(SimpleResource parent, List<SimpleResource> children) {
        for (SimpleResource res : children) {
            parent.children.add(res);
            res.parent = parent;
        }
    }

    static Map<Rectangle, SimpleResource> spaces = new HashMap<Rectangle, SimpleResource>();

    /* public static void paintRes(Graphics2D g, List<SimpleResource> resources, int x, int y, int width, int height) {

            int itemWidth = width / resources.size();
            int itemHeight = (int) (height * 0.25);
            int currentX = x;

            spaces.clear();
            for (SimpleResource res : resources) {
                itemWidth = (int) (res.weight * width);
                g.setColor(res.up ? Color.GREEN : Color.red);
                Rectangle rect = new Rectangle(currentX, y - itemHeight, itemWidth, itemHeight);
                Rectangle fullRect = new Rectangle(currentX, y - height, itemWidth, height);

                g.fill(rect);
                g.setColor(Color.DARK_GRAY);
                g.draw(rect);
                spaces.put(rect, res);
                //System.out.println(res.name + " " + rect);
                if (!res.children.isEmpty()) {
                    boolean allLeafs = true;
                    for (SimpleResource child : res.children) {
                        if (!child.children.isEmpty()) {
                            allLeafs = false;
                            break;
                        }
                    }
    //                if (allLeafs) {
    //                    int total = res.children.size();
    //                    int rowCount = Math.max(1, (int) (((double)total) / 3.0));
    //
    //                    for (int i = 0; i < total; i+=rowCount) {
    //                        if (i != Math.min(i+rowCount,total)) {
    //                            double smallItemHeight = (((double)itemHeight) / 3.0);
    //                            paintRes(g, res.children.subList(i,Math.min(i+rowCount,total)),
    //                                    currentX, (int) (y-(smallItemHeight*(i+1))), itemWidth, (int) ((int) smallItemHeight*1.5));
    //                        }
    //
    //                    }
    //                } else {

                    paintRes(g, res.children, currentX, y - itemHeight, itemWidth, (int) (y - itemHeight));
    //                }
                }

                currentX += itemWidth;
            }
        }
    */

    public static int setWeight(List<SimpleResource> resources) {
        int total = 1;
        int upCount = 0;
        for (SimpleResource res : resources) {
            int childWeight = setWeight(res.children);
            res.size = childWeight;
            total += childWeight;
            upCount += res.up ? 1 : 0;
        }

        for (SimpleResource res : resources) {
            res.weight = ((double) res.size) / total;
            //System.out.println(res.name + " " + res.weight);
        }

        return total;
    }

    public static void setStuff(SimpleResource res) {
        if (res.children.size() == 0) {
            res.descendents = 0;
            res.upness = res.up ? 1 : 0;
        } else {
            double upness = 0;
            for (SimpleResource child : res.children) {
                setStuff(child);
                res.descendents += child.descendents + 1;
                upness += res.upness * (child.descendents + 1);
            }

            res.upness = (upness + (res.up ? 1 : 0)) / (res.descendents + 1);
            System.out.println(res.name + "\t\t" + res.upness);
        }
    }


    private static class SimpleResource implements TreeNode {

        int id;
        int parentId;
        int descendents;
        String name;
        boolean up;
        double upness;
        int size;
        double weight;
        SimpleResource parent;
        List<SimpleResource> children = new ArrayList<SimpleResource>();
        ResourceTreeNode resourceTreeNode;

        private SimpleResource(int id, String name, boolean up, int parent) {
            this.id = id;
            this.name = name;
            this.up = up;
            this.parentId = parent;

            resourceTreeNode = new ResourceTreeNode(this);
        }

        public List<TreeNode> children() {
            return new ArrayList<TreeNode>(children);
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }

        public ResourceTreeNode getResourceTreeNode() {
            return resourceTreeNode;
        }
    }

    public static class ResourceTreeNode implements javax.swing.tree.TreeNode {

        SimpleResource simpleResource;

        public ResourceTreeNode(SimpleResource simpleResource) {
            this.simpleResource = simpleResource;
        }

        public javax.swing.tree.TreeNode getChildAt(int childIndex) {
            return ((SimpleResource) this.simpleResource.children().get(childIndex)).getResourceTreeNode();
        }

        public int getChildCount() {
            return this.simpleResource.children().size();
        }

        public javax.swing.tree.TreeNode getParent() {
            return simpleResource.parent.getResourceTreeNode();
        }

        public int getIndex(javax.swing.tree.TreeNode node) {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean getAllowsChildren() {
            return !simpleResource.isLeaf();
        }

        public boolean isLeaf() {
            return simpleResource.isLeaf();
        }

        public Enumeration children() {
            Vector<ResourceTreeNode> children = new Vector<ResourceTreeNode>();
            for (SimpleResource childResource : simpleResource.children) {
                children.add(childResource.getResourceTreeNode());
            }
            return children.elements();
        }

        public String toString() {
            return simpleResource.name;
        }
    }
}