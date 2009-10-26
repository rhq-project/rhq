
package ch.randelshofer.tree.hypertree;

import ch.randelshofer.tree.TreeNode;
import ch.randelshofer.tree.TreeView;

import java.awt.Image;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public interface HTView extends TreeView {

    //public boolean getLongNameMode();

    //public boolean getFastMode();

    public void startMouseListening();

    public void stopMouseListening();

    public void repaint();

    public int getHeight();

    public int getWidth();

    public Insets getInsets();

    public TreeNode getNodeUnderTheMouse(MouseEvent event);

    public void translateToOrigin(TreeNode node);

    public void setImage(Image image);

    public void addMouseListener(MouseListener l);

}
