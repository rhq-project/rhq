/*
 * Main.java
 *
 * Created on September 16, 2007, 6:14 PM
 */
package ch.randelshofer.tree.demo;

import ch.randelshofer.gui.ProgressView;
import ch.randelshofer.tree.*;
import ch.randelshofer.tree.hypertree.HyperTree;
import ch.randelshofer.tree.hypertree.SwingHTView;
import ch.randelshofer.tree.circlemap.*;
import ch.randelshofer.tree.rectmap.*;
import ch.randelshofer.tree.sunray.*;
import ch.randelshofer.tree.sunburst.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.*;
import java.util.prefs.Preferences;
import javax.swing.*;
import org.jhotdraw.gui.*;

/**
 *
 * @author  werni
 */
public class Main extends javax.swing.JFrame {

    private JFileChooser directoryChooser;
    private JFileChooser fileChooser;
    private File rootFile;
    private TreeNode rootNode;
    private Preferences prefs;
    private NodeInfo info;
    private DropTargetListener dropHandler = new DropTargetListener() {

        /**
         * Called when a drag operation has
         * encountered the <code>DropTarget</code>.
         * <P>
         * @param dtde the <code>DropTargetDragEvent</code>
         */
        public void dragEnter(DropTargetDragEvent event) {
            if (event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                event.acceptDrag(DnDConstants.ACTION_COPY);
            } else {
                event.rejectDrag();
            }
        }

        /**
         * The drag operation has departed
         * the <code>DropTarget</code> without dropping.
         * <P>
         * @param dte the <code>DropTargetEvent</code>
         */
        public void dragExit(DropTargetEvent event) {
        // Nothing to do
        }

        /**
         * Called when a drag operation is ongoing
         * on the <code>DropTarget</code>.
         * <P>
         * @param dtde the <code>DropTargetDragEvent</code>
         */
        public void dragOver(DropTargetDragEvent event) {
            if (event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                event.acceptDrag(DnDConstants.ACTION_COPY);
            } else {
                event.rejectDrag();
            }
        }

        /**
         * The drag operation has terminated
         * with a drop on this <code>DropTarget</code>.
         * This method is responsible for undertaking
         * the transfer of the data associated with the
         * gesture. The <code>DropTargetDropEvent</code>
         * provides a means to obtain a <code>Transferable</code>
         * object that represents the data object(s) to
         * be transfered.<P>
         * From this method, the <code>DropTargetListener</code>
         * shall accept or reject the drop via the
         * acceptDrop(int dropAction) or rejectDrop() methods of the
         * <code>DropTargetDropEvent</code> parameter.
         * <P>
         * Subsequent to acceptDrop(), but not before,
         * <code>DropTargetDropEvent</code>'s getTransferable()
         * method may be invoked, and data transfer may be
         * performed via the returned <code>Transferable</code>'s
         * getTransferData() method.
         * <P>
         * At the completion of a drop, an implementation
         * of this method is required to signal the success/failure
         * of the drop by passing an appropriate
         * <code>boolean</code> to the <code>DropTargetDropEvent</code>'s
         * dropComplete(boolean success) method.
         * <P>
         * Note: The actual processing of the data transfer is not
         * required to finish before this method returns. It may be
         * deferred until later.
         * <P>
         * @param dtde the <code>DropTargetDropEvent</code>
         */
        public void drop(DropTargetDropEvent event) {
            if (event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                event.acceptDrop(DnDConstants.ACTION_COPY);

                try {
                    java.util.List<File> files = (java.util.List<File>) event.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (files.size() == 1) {

                        File file = files.get(0);
                        if (file.isDirectory()) {
                            openDirectory(file);
                        } else {
                            openFile(file);
                        }
                    }
                } catch (IOException e) {
                    JOptionPane.showConfirmDialog(
                            Main.this,
                            "Could not access the dropped data.",
                            "Treeviz: Drop Failed",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                } catch (UnsupportedFlavorException e) {
                    JOptionPane.showConfirmDialog(
                            Main.this,
                            "Unsupported data flavor.",
                            "Treeviz: Drop Failed",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                event.rejectDrop();
            }
        }

        /**
         * Called if the user has modified
         * the current drop gesture.
         * <P>
         * @param dtde the <code>DropTargetDragEvent</code>
         */
        public void dropActionChanged(DropTargetDragEvent event) {
        // Nothing to do
        }
    };

    /**
     * Creates new form Main
     */
    public Main() {
        initComponents();
        setSize(400, 400);
        prefs = Preferences.userNodeForPackage(getClass());
        viewAsHypertreeRadio.setSelected(prefs.get("viewAs", "hyperbolic").equals("hyperbolic"));
        viewAsSunburstRadio.setSelected(prefs.get("viewAs", "hyperbolic").equals("sunburst"));
        viewAsSunrayRadio.setSelected(prefs.get("viewAs", "hyperbolic").equals("sunray"));
        viewAsIcicleRadio.setSelected(prefs.get("viewAs", "hyperbolic").equals("icicle"));
        viewAsIcerayRadio.setSelected(prefs.get("viewAs", "hyperbolic").equals("iceray"));
        viewAsCircleMapRadio.setSelected(prefs.get("viewAs", "hyperbolic").equals("circlemap"));
        viewAsRectangleMapRadio.setSelected(prefs.get("viewAs", "hyperbolic").equals("rectanglemap"));
        new DropTarget(this, dropHandler);
        new DropTarget(getContentPane(), dropHandler);
        new DropTarget(viewPanel, dropHandler);
    }

    private void updateView() {
        if (rootNode == null) {
            viewPanel.removeAll();
            validate();
            repaint();

        } else {
            if (viewAsHypertreeRadio.isSelected()) {
                updateHTView();
            } else if (viewAsSunburstRadio.isSelected()) {
                updateSBView();
            } else if (viewAsSunrayRadio.isSelected()) {
                updateScBView();
            } else if (viewAsIcicleRadio.isSelected()) {
                updateIcView();
            } else if (viewAsIcerayRadio.isSelected()) {
                updateIdView();
            } else if (viewAsCircleMapRadio.isSelected()) {
                updateCMView();
            } else if (viewAsRectangleMapRadio.isSelected()) {
                updateRMView();
            }
        }
    }

    private void updateSBView() {
        final ProgressView p = new ProgressView("Sunburst Tree", "Calculating layout...");
        p.setIndeterminate(true);
        Worker worker = new Worker() {

            public Object construct() {
                SunburstModel sunbursttree = new SunburstModel(rootNode, info);
                return sunbursttree;
            }

            public void finished(Object o) {
                p.close();
                SunburstModel model = (SunburstModel) o;
                SunburstView view = model.getView();
                //  view.setFont(new Font("Dialog", Font.PLAIN, 9));
                histogram.setWeighter(model.getInfo().getWeighter());
                histogram.setColorizer(model.getInfo().getColorizer());
                viewPanel.removeAll();
                viewPanel.add(view);
                new DropTarget(view, dropHandler);
                validate();
                repaint();
            }
        };
        worker.start();
    }

    private void updateScBView() {
        final ProgressView p = new ProgressView("Sunray Tree", "Calculating layout...");
        p.setIndeterminate(true);
        Worker worker = new Worker() {

            public Object construct() {
                SunrayModel scatterbursttree = new SunrayModel(rootNode, info);
                return scatterbursttree;
            }

            public void finished(Object o) {
                p.close();
                SunrayModel model = (SunrayModel) o;
                SunrayView view = model.getView();
                //  view.setFont(new Font("Dialog", Font.PLAIN, 9));
                histogram.setWeighter((model.getInfo().getWeighter()));
                histogram.setColorizer((model.getInfo().getColorizer()));
                viewPanel.removeAll();
                viewPanel.add(view);
                new DropTarget(view, dropHandler);
                validate();
                repaint();
            }
        };
        worker.start();
    }

    private void updateHTView() {
        final ProgressView p = new ProgressView("Hyperbolic Tree", "Calculating layout...");
        p.setIndeterminate(true);
        Worker worker = new Worker() {

            public Object construct() {
                HyperTree tree = new HyperTree(rootNode, info);
                return tree;
            }

            public void finished(Object o) {
                p.close();
                HyperTree model = (HyperTree) o;
                SwingHTView view = model.getView();
                // view.setFont(new Font("Dialog", Font.PLAIN, 9));
                histogram.setWeighter((model.getInfo().getWeighter()));
                histogram.setColorizer((model.getInfo().getColorizer()));
                viewPanel.removeAll();
                viewPanel.add(view);
                new DropTarget(view, dropHandler);
                validate();
                repaint();
            }
        };
        worker.start();
    }

    private void updateCMView() {
        final ProgressView p = new ProgressView("Circular Treemap", "Calculating layout...");
        p.setIndeterminate(true);
        Worker worker = new Worker() {

            public Object construct() {
                CirclemapModel model = new CirclemapModel(rootNode, info);
                return model;
            }

            public void finished(Object o) {
                p.close();
                CirclemapModel model = (CirclemapModel) o;
                CirclemapView view = model.getView();
                // view.setFont(new Font("Dialog", Font.PLAIN, 9));
                histogram.setWeighter(model.getInfo().getWeighter());
                histogram.setColorizer(model.getInfo().getColorizer());
                viewPanel.removeAll();
                viewPanel.add(view);
                new DropTarget(view, dropHandler);
                validate();
                repaint();
            }
        };
        worker.start();
    }

    private void updateRMView() {
        final ProgressView p = new ProgressView("Rectangular Treemap", "Calculating layout...");
        p.setIndeterminate(true);
        Worker worker = new Worker() {

            public Object construct() {
                RectmapModel model = new RectmapModel(rootNode, info);
                return model;
            }

            public void finished(Object o) {
                p.close();
                RectmapModel model = (RectmapModel) o;
                RectmapView view = model.getView();
                // view.setFont(new Font("Dialog", Font.PLAIN, 9));
                histogram.setWeighter(model.getInfo().getWeighter());
                histogram.setColorizer(model.getInfo().getColorizer());
                viewPanel.removeAll();
                viewPanel.add(view);
                new DropTarget(view, dropHandler);
                validate();
                repaint();
            }
        };
        worker.start();
    }

    private void updateIcView() {
        final ProgressView p = new ProgressView("Icicle Tree", "Calculating layout...");
        p.setIndeterminate(true);
        Worker worker = new Worker() {

            public Object construct() {
                IcicleModel tree = new IcicleModel(rootNode, info);
                return tree;
            }

            public void finished(Object o) {
                p.close();
                IcicleModel model = (IcicleModel) o;
                IcicleView view = model.getView();
                //  view.setFont(new Font("Dialog", Font.PLAIN, 9));
                histogram.setWeighter(model.getInfo().getWeighter());
                histogram.setColorizer(model.getInfo().getColorizer());
                viewPanel.removeAll();
                viewPanel.add(view);
                new DropTarget(view, dropHandler);
                validate();
                repaint();
            }
        };
        worker.start();
    }

    private void updateIdView() {
        final ProgressView p = new ProgressView("Iceray Tree", "Calculating layout...");
        p.setIndeterminate(true);
        Worker worker = new Worker() {

            public Object construct() {
                IcerayModel tree = new IcerayModel(rootNode, info);
                return tree;
            }

            public void finished(Object o) {
                p.close();
                IcerayModel model = (IcerayModel) o;
                IcerayView view = model.getView();
                //  view.setFont(new Font("Dialog", Font.PLAIN, 9));
                histogram.setWeighter(model.getInfo().getWeighter());
                histogram.setColorizer(model.getInfo().getColorizer());
                viewPanel.removeAll();
                viewPanel.add(view);
                new DropTarget(view, dropHandler);
                validate();
                repaint();
            }
        };
        worker.start();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        viewAsButtonGroup = new javax.swing.ButtonGroup();
        viewPanel = new javax.swing.JPanel();
        statusPanel = new javax.swing.JPanel();
        histogram = new ch.randelshofer.tree.demo.JHistogram();
        jMenuBar1 = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        openDirectoryMenuItem = new javax.swing.JMenuItem();
        openFileMenuItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        viewAsHypertreeRadio = new javax.swing.JRadioButtonMenuItem();
        viewAsSunburstRadio = new javax.swing.JRadioButtonMenuItem();
        viewAsSunrayRadio = new javax.swing.JRadioButtonMenuItem();
        viewAsIcicleRadio = new javax.swing.JRadioButtonMenuItem();
        viewAsIcerayRadio = new javax.swing.JRadioButtonMenuItem();
        viewAsCircleMapRadio = new javax.swing.JRadioButtonMenuItem();
        viewAsRectangleMapRadio = new javax.swing.JRadioButtonMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Tree Visualizer");
        viewPanel.setLayout(new java.awt.BorderLayout());

        getContentPane().add(viewPanel, java.awt.BorderLayout.CENTER);

        histogram.setLayout(new java.awt.FlowLayout());

        statusPanel.add(histogram);

        getContentPane().add(statusPanel, java.awt.BorderLayout.SOUTH);

        fileMenu.setText("File");
        openDirectoryMenuItem.setText("Open Directory\u2026");
        openDirectoryMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openDirectory(evt);
            }
        });

        fileMenu.add(openDirectoryMenuItem);

        openFileMenuItem.setText("Open File...");
        openFileMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openFile(evt);
            }
        });

        fileMenu.add(openFileMenuItem);

        jMenuBar1.add(fileMenu);

        viewMenu.setText("View");
        viewAsButtonGroup.add(viewAsHypertreeRadio);
        viewAsHypertreeRadio.setSelected(true);
        viewAsHypertreeRadio.setText("Hyperbolic Tree");
        viewAsHypertreeRadio.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                viewAsItemChanged(evt);
            }
        });

        viewMenu.add(viewAsHypertreeRadio);

        viewAsButtonGroup.add(viewAsSunburstRadio);
        viewAsSunburstRadio.setText("Sunburst Tree");
        viewAsSunburstRadio.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                viewAsItemChanged(evt);
            }
        });

        viewMenu.add(viewAsSunburstRadio);

        viewAsButtonGroup.add(viewAsSunrayRadio);
        viewAsSunrayRadio.setText("Sunray Tree");
        viewAsSunrayRadio.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                viewAsItemChanged(evt);
            }
        });

        viewMenu.add(viewAsSunrayRadio);

        viewAsButtonGroup.add(viewAsIcicleRadio);
        viewAsIcicleRadio.setText("Icicle Tree");
        viewAsIcicleRadio.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                viewAsItemChanged(evt);
            }
        });

        viewMenu.add(viewAsIcicleRadio);

        viewAsButtonGroup.add(viewAsIcerayRadio);
        viewAsIcerayRadio.setText("Iceray Tree");
        viewAsIcerayRadio.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                viewAsItemChanged(evt);
            }
        });

        viewMenu.add(viewAsIcerayRadio);

        viewAsButtonGroup.add(viewAsCircleMapRadio);
        viewAsCircleMapRadio.setText("Circular Treemap");
        viewAsCircleMapRadio.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                viewAsItemChanged(evt);
            }
        });

        viewMenu.add(viewAsCircleMapRadio);

        viewAsButtonGroup.add(viewAsRectangleMapRadio);
        viewAsRectangleMapRadio.setText("Rectangular Treemap");
        viewAsRectangleMapRadio.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                viewAsItemChanged(evt);
            }
        });

        viewMenu.add(viewAsRectangleMapRadio);

        jMenuBar1.add(viewMenu);

        helpMenu.setText("Help");
        aboutMenuItem.setText("About");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                about(evt);
            }
        });

        helpMenu.add(aboutMenuItem);

        jMenuBar1.add(helpMenu);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    private void openFile(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openFile
        if (fileChooser == null) {
            fileChooser = new JFileChooser();
        // fileChooser.setAcceptAllFileFilterUsed(false);
        // fileChooser.setFileFilter(new Exten)
        }
        if (JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(this)) {
            openFile(fileChooser.getSelectedFile());
        }
    }//GEN-LAST:event_openFile

    private void openFile(File file) {
        rootFile = file;
        new Worker() {

            public Object construct() {
                DemoTree tree;

                // Try TreevizFileSystemXMLTree
                try {
                    tree = new TreevizFileSystemXMLTree(rootFile);
                    return tree;
                } catch (IOException ex) {
                    ex.printStackTrace();
                    // continue
                }

                // Try generic XMLTree
                    try {
                        tree = new XMLTree(rootFile);
                        return tree;
                    } catch (IOException ex) {
                        return ex;
                    }
            }

            public void finished(Object result) {
                if (result instanceof Throwable) {
                    ((Throwable) result).printStackTrace();
                } else {
                    rootNode = ((DemoTree) result).getRoot();
                    info = ((DemoTree) result).getInfo();
                    setTitle("Tree Visualizer: " + info.getName(new TreePath(rootNode)));
                    updateView();
                }
            }
            }.start();
    }

    private void about(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_about
        JOptionPane.showMessageDialog(this, "<html>TreeViz "+Main.class.getPackage().getImplementationVersion()+"<br>" +
                "Copyright © 2007-2008<br>" +
                "Werner Randelshofer<br>" +
                "All rights reserved.", "About", JOptionPane.PLAIN_MESSAGE);
    }//GEN-LAST:event_about

    private void viewAsItemChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_viewAsItemChanged
        String view = null;
        if (viewAsHypertreeRadio.isSelected()) {
            view = "hyperbolic";
        } else if (viewAsSunburstRadio.isSelected()) {
            view = "sunburst";
        } else if (viewAsSunrayRadio.isSelected()) {
            view = "sunray";
        } else if (viewAsIcicleRadio.isSelected()) {
            view = "icicle";
        } else if (viewAsIcerayRadio.isSelected()) {
            view = "iceray";
        } else if (viewAsCircleMapRadio.isSelected()) {
            view = "circlemap";
        } else if (viewAsRectangleMapRadio.isSelected()) {
            view = "rectanglemap";
        }
        if (view != null) {
            prefs.put("viewAs", view);
            updateView();
        }

    }//GEN-LAST:event_viewAsItemChanged

    private void openDirectory(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openDirectory
        if (directoryChooser == null) {
            directoryChooser = new JFileChooser();
            directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        if (JFileChooser.APPROVE_OPTION == directoryChooser.showOpenDialog(this)) {
            openDirectory(directoryChooser.getSelectedFile());
        }

    }//GEN-LAST:event_openDirectory

    private void openDirectory(File dir) {
        rootFile = dir;
        new Worker() {

            public Object construct() {
                FileNode root = new FileNode(rootFile);
                return root;
            }

            public void finished(Object result) {
                rootNode = (FileNode) result;
                info = new FileNodeInfo();
                setTitle("Tree Visualizer: " + rootFile.getName());
                updateView();
            }
            }.start();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception e) {
                // UIManager does the right thing for us
                }

                ToolTipManager.sharedInstance().setDismissDelay(60000); // display tooltip for 10 minutes
                new Main().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenu helpMenu;
    private ch.randelshofer.tree.demo.JHistogram histogram;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem openDirectoryMenuItem;
    private javax.swing.JMenuItem openFileMenuItem;
    private javax.swing.JPanel statusPanel;
    private javax.swing.ButtonGroup viewAsButtonGroup;
    private javax.swing.JRadioButtonMenuItem viewAsCircleMapRadio;
    private javax.swing.JRadioButtonMenuItem viewAsHypertreeRadio;
    private javax.swing.JRadioButtonMenuItem viewAsIcerayRadio;
    private javax.swing.JRadioButtonMenuItem viewAsIcicleRadio;
    private javax.swing.JRadioButtonMenuItem viewAsRectangleMapRadio;
    private javax.swing.JRadioButtonMenuItem viewAsSunburstRadio;
    private javax.swing.JRadioButtonMenuItem viewAsSunrayRadio;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JPanel viewPanel;
    // End of variables declaration//GEN-END:variables
}
