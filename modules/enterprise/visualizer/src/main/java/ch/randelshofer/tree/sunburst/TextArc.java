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
package ch.randelshofer.tree.sunburst;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

/**
 * @author Greg Hinkle
 */
public class TextArc implements ActionListener {


    BufferedImage image;
    JLabel label;
    GeneralPath path;
    CubicCurve2D cubic;
    QuadCurve2D quad;
    Arc2D circle;
    TextDraw textDraw = new TextDraw();

    private void initGeometry(int w, int h) {
        // cubic curve
        double x1 = w / 4.0;
        double y1 = h / 16.0;
        double ctrlx1 = w / 2.0;
        double ctrly1 = h * 2 / 5.0;
        double ctrlx2 = w * 13 / 16.0;
        double ctrly2 = h / 3.0;
        double x2 = w * 7 / 8.0;
        double y2 = h * 7 / 8.0;
        cubic = new CubicCurve2D.Double(x1, y1, ctrlx1, ctrly1,
                ctrlx2, ctrly2, x2, y2);
        // quad curve
        x1 = w / 7.0;
        y1 = h * 15 / 16.0;
        double ctrlx = w / 3.0;
        double ctrly = h / 10.0;
        x2 = w * 11 / 12.0;
        y2 = h * 19 / 24.0;
        quad = new QuadCurve2D.Double(x1, y1, ctrlx, ctrly, x2, y2);
        // circle
        x1 = w / 4.0;
        y1 = h / 8.0;
        double r = Math.min(w, h) / 3.0;
        circle = new Arc2D.Double(x1, y1, 2 * r, 2 * r, 0,20, Arc2D.PIE);
        path = new GeneralPath(quad);
    }

    private JScrollPane getContent() {
        int w = 350;
        int h = 350;
        initGeometry(w, h);
        image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        fillImage();
        label = new JLabel(new ImageIcon(image));
        label.setHorizontalAlignment(JLabel.CENTER);
        return new JScrollPane(label);
    }

    private void fillImage() {
        int w = image.getWidth();
        int h = image.getHeight();
        Graphics2D g2 = image.createGraphics();
        g2.setBackground(UIManager.getColor("Panel.background"));
        g2.clearRect(0, 0, w, h);
        g2.setPaint(Color.blue);
        g2.drawRect(0, 0, w - 1, h - 1);
        g2.setPaint(Color.black);
        String text = "Seems Okay";
        int offset = 75;
        textDraw.draw(g2, path, text, offset);
        g2.dispose();
    }

    public static void main(String[] args) {
        TextArc test = new TextArc();
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.getContentPane().add(test.getContent());
        f.getContentPane().add(test.getControls(), "Last");
        f.setSize(400, 400);
        f.setLocation(200, 200);
        f.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        String ac = e.getActionCommand();
        Shape s = null;
        if (ac.equals("cubic"))
            s = cubic;
        else if (ac.equals("quad"))
            s = quad;
        else if (ac.equals("circle"))
            s = circle;
        path = new GeneralPath(s);
        fillImage();
        label.repaint();
    }

    private JPanel getControls() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createLoweredBevelBorder());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.weightx = 1.0;
        String[] ids = {"cubic", "quad", "circle"};
        ButtonGroup group = new ButtonGroup();
        for (int j = 0; j < ids.length; j++) {
            JRadioButton rb = new JRadioButton(ids[j], j == 1);
            rb.setActionCommand(ids[j]);
            rb.addActionListener(this);
            group.add(rb);
            panel.add(rb, gbc);
        }
        return panel;
    }
}

class TextDraw {
    GeneralPath path;
    String text;
    double[] tokenWidths;
    Point2D.Double[] points;
    int offset;

    protected void draw(Graphics2D g2, GeneralPath path, String text, int offset) {
        this.path = path;
        this.text = text;
        this.offset = offset;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        Font font = g2.getFont().deriveFont(36f);
        g2.setFont(font);
        FontRenderContext frc = g2.getFontRenderContext();
        tokenWidths = getTokenWidths(font, frc);
        // collect path points
        collectLayoutPoints(getPathPoints());
        g2.draw(path);
        // layout text beginning at offset along curve
        String[] tokens = text.split("(?<=[\\w\\s])");
        for (int j = 0; j < points.length - 1; j++) {
            double theta = getAngle(j);
            AffineTransform at =
                    AffineTransform.getTranslateInstance(points[j].x, points[j].y);
            at.rotate(theta);
            g2.setFont(font.deriveFont(at));
            g2.drawString(tokens[j], 0, 0);
        }
    }

    private double getAngle(int index) {
        double dy = points[index + 1].y - points[index].y;
        double dx = points[index + 1].x - points[index].x;
        return Math.atan2(dy, dx);
    }

    private Point2D.Double[] getPathPoints() {
        double flatness = 0.01;
        PathIterator pit = path.getPathIterator(null, flatness);
        int count = 0;
        while (!pit.isDone()) {
            count++;
            pit.next();
        }
        Point2D.Double[] points = new Point2D.Double[count];
        pit = path.getPathIterator(null, flatness);
        double[] coords = new double[6];
        count = 0;
        while (!pit.isDone()) {
            int type = pit.currentSegment(coords);
            switch (type) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    points[count++] = new Point2D.Double(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_CLOSE:
                    break;
                default:
                    System.out.println("unexpected type: " + type);
            }
            pit.next();
        }
        return points;
    }

    private double[] getTokenWidths(Font font, FontRenderContext frc) {
        String[] tokens = text.split("(?<=[\\w\\s])");
        double[] widths = new double[tokens.length];
        for (int j = 0; j < tokens.length; j++) {
            float width = (float) font.getStringBounds(tokens[j], frc).getWidth();
            widths[j] = width;
        }
        return widths;
    }

    private void collectLayoutPoints(Point2D.Double[] p) {
        int index = 0;
        int n = tokenWidths.length;
        double distance = offset;
        points = new Point2D.Double[n + 1];
        for (int j = 0; j < tokenWidths.length; j++) {
            System.out.println("found one");
            index = getNextPointIndex(p, index, distance);
            points[j] = p[index];
            distance = tokenWidths[j];
        }
        index = getNextPointIndex(p, index, tokenWidths[n - 1]);
        points[points.length - 1] = p[index];
    }

    private int getNextPointIndex(Point2D.Double[] p,
                                  int start, double targetDist) {
        for (int j = start; j < p.length; j++) {
            double distance = p[j].distance(p[start]);
            if (distance > targetDist) {
                return j;
            }
        }
        return start;
    }
}


