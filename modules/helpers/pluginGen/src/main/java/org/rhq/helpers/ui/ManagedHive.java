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
package org.rhq.helpers.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

/** Is a basic ui that generates a very simple managed
 *  graphical resource that can be managed/monitored
 *  by an RHQ plugin.
 *  
 *  Simulates a bee hive where:
 *  - can configure i)current bee population count
 *  -               ii)swarm time response
 *  - can monitor  i)bee population count
 *  -              ii)whether hive is angry
 *  - can execute operations and 
 *  -              i)shake the hive to upset the bees
 *   
 *  PIQL for process identification used in discovery
 *  
 *  @author Simeon Pinder
 */
public class ManagedHive extends JFrame {

    /******************* Startup/initialization & Components *************/
    /** Simple command line launch mechanism
     * @param args
     */
    public static void main(String[] args) {
        new ManagedHive();
    }

    public ManagedHive() {
        //create ui layout
        initializeUi();
        //initial hive setup
        //        initializeHive();
    }

    /******************* Management capabilities **************************/

    /******************* UI Logic & Components **************************/
    //    private JTextField hiveDirectory;
    private int space = 7;//horizontal spacing between components
    private int initialPopulation = 50;
    private int swarmTime = 10000;//ms.
    private Hive hiveComponent;

    /** Responsible for putting together the layout components.
     * 
     */
    private void initializeUi() {

        setTitle("Managed Hive:");//titling
        //ui organization
        getContentPane().setLayout(new BorderLayout());
        // top panel definition
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBorder(LineBorder.createGrayLineBorder());

        //monitor row
        JPanel monitorRow = new JPanel();
        monitorRow.setLayout(new BoxLayout(monitorRow, BoxLayout.X_AXIS));
        top.add(monitorRow);

        //configuration/operation row
        JPanel interactionRow = new JPanel();
        interactionRow.setLayout(new BoxLayout(interactionRow, BoxLayout.X_AXIS));
        top.add(interactionRow);

        {
            //monitor row shows current state of the hive
            JLabel currentPopulationLabel = new JLabel("Bee count");
            monitorRow.add(currentPopulationLabel);
            monitorRow.add(Box.createHorizontalStrut(space));
            JTextField currentPopulation = new JTextField("" + initialPopulation);
            currentPopulation.setEditable(false);
            monitorRow.add(currentPopulation);
            monitorRow.add(Box.createHorizontalStrut(space));

            JLabel maxPopulationLabel = new JLabel("Swarm Time(ms)");
            monitorRow.add(maxPopulationLabel);
            monitorRow.add(Box.createHorizontalStrut(space));
            JTextField maxPopulation = new JTextField("" + swarmTime);
            maxPopulation.setEditable(false);
            monitorRow.add(maxPopulation);
            monitorRow.add(Box.createHorizontalStrut(space));//spacer

            JLabel mood = new JLabel();
            mood.setOpaque(true);
            mood.setBackground(Color.green);
            mood.setText("Calm");
            monitorRow.add(mood);
            monitorRow.add(Box.createHorizontalStrut(space));
        }

        //Shake hive button
        JButton shake = new JButton("Shake Hive");
        shake.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addBee();
            }
        });
        interactionRow.add(shake);

        // center
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
        // build center panel
        buildCenterPanel(center);

        // final component layout
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(center, BorderLayout.CENTER);
        this.setSize(500, 500);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        setVisible(true);
    }

    private void buildCenterPanel(final JPanel center) {
        hiveComponent = new Hive();
        hiveComponent.setBorder(new LineBorder(Color.black));
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                center.add(hiveComponent);
            }
        });
    }

    /**
     * Adds a bouncing ball to the canvas and starts a thread to make it bounce
     */
    public void addBee() {
        //      Bee b = new Bee();
        Bee b = null;
        //tweak the start position
        Random generator = new Random(System.currentTimeMillis());
        int newX = generator.nextInt(BeeFlight.delta);
        int newY = generator.nextInt(BeeFlight.delta);
        b = new Bee(newX, newY);
        //      comp.add(b);
        hiveComponent.add(b);
        //      Runnable r = new BeeFlight(b, comp);
        Runnable r = new BeeFlight(b, hiveComponent);
        Thread t = new Thread(r);
        t.start();
    }
}

class Hive extends JComponent {
    //entire hive population.
    private static ArrayList<Bee> population = new ArrayList<Bee>();

    public void add(Bee b) {
        population.add(b);
    }

    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        for (Bee b : population) {
            g2.fill(b.getShape());
        }
    }
}

/** Represents the cartesion/graphical components to
 *  define a bee.
 */
class Bee {

    //properties of typical cartesion component. 
    private int xWidth = 15;
    private int yWidth = 15;

    //cartesion components
    private double x = 0;
    private double y = 0;

    //movement delta
    private double dx = 1;
    private double dy = 1;

    public Bee(int newX, int newY) {
        this.x = newX;
        this.y = newY;
    }

    /**
     * Defines the shape of the bee at each call.
     */
    public Ellipse2D getShape() {
        //todo: randomly change dimensions to simulate busy 
        return new Ellipse2D.Double(x, y, xWidth, yWidth);
    }

    //return to invisible
    public void clear() {
        xWidth = 0;
        xWidth = 0;
    }

    /**
     * Moves the bee along. Change directions if it hits the side.
     */
    public void move(Rectangle2D bounds) {
        //the new position of the point
        x += dx;
        y += dy;
        //if the new X would result in out of the box then reverse.
        if (x < bounds.getMinX()) {
            x = bounds.getMinX();//hit the side
            dx = -dx; //bounce
        }
        if (x + xWidth >= bounds.getMaxX()) {
            x = bounds.getMaxX() - xWidth;//hit the side
            dx = -dx;//bounce
        }

        //if the new Y would result in out of the box then reverse.
        if (y < bounds.getMinY()) {
            y = bounds.getMinY();
            dy = -dy;//bounce
        }
        if (y + yWidth >= bounds.getMaxY()) {
            y = bounds.getMaxY() - yWidth;
            dy = -dy;//bounce
        }
    }
}

/**
* A thread for animating the bee's flight.
*/
class BeeFlight implements Runnable {
    //attributes
    private Bee bee;

    private Component component;

    public static final int STEPS = 10000;

    public static final int DELAY = 5;

    public static int delta = 300;

    //operations
    public BeeFlight(Bee aBee, Component aComponent) {
        bee = aBee;
        component = aComponent;
    }

    public void run() {
        try {
            for (int i = 1; i <= STEPS; i++) {
                bee.move(component.getBounds());
                component.repaint();
                Thread.sleep(DELAY);
            }
            //kill the bee.
            bee.clear();
            component.repaint();

        } catch (InterruptedException e) {
        }
    }
}
