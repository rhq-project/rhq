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
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ImmutableHttpProcessor;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;

import org.rhq.helpers.ui.ManagedHive.Validation;
import org.rhq.helpers.ui.RemoteApi.Protocol;

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
 *  -              ii)temporarily adds a few more bees
 *   
 *  PIQL for process identification used in discovery
 *  
 *  @author Simeon Pinder
 */
public class ManagedHive extends JFrame {

    static Logger LOG = Logger.getLogger(ManagedHive.class.getName());
    private static String REMOTE_PROTOCOL = "--remote-protocol";
    private static String REMOTE_PROTOCOL_PORT = "--remote-port";
    private static String HELP = "--help";
    private static String HELP1 = "-h";
    /******************* Startup/initialization & Components *************/
    /** Simple command line launch mechanism
     * @param args
     */
    public static void main(String[] args) {
        //allow way to switch between protocols at server startup
        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                String argument = args[i];
                argument = argument.toLowerCase();
                //determine remote protocol type
                if (argument.startsWith(REMOTE_PROTOCOL)) {
                    //default to JSON over socket
                    ManagedHive.defaultProtocol = Protocol.JSON;
                    //split on = to get value
                    String[] selection = argument.split("=");
                    if ((selection != null) && (selection.length == 2) && (selection[1].equalsIgnoreCase("HTTP"))) {
                        ManagedHive.defaultProtocol = Protocol.HTTP_JSON;
                    }
                }
                //determine remote protocol port. This is optional and first available will be 
                //selected if the requested port is not actually available.
                if (argument.startsWith(REMOTE_PROTOCOL_PORT)) {
                    //already defaulted to 9876
                    //split on = to get value
                    String[] selection = argument.split("=");
                    if ((selection != null) && (selection.length == 2)) {
                        String value = selection[1];
                        int valueInt = -1;
                        try {
                            valueInt = Integer.parseInt(value);
                            if ((valueInt > -1) && (valueInt < 66000)) {
                                ManagedHive.defaultRemoteApiPort = valueInt;
                            }//otherwise ignore
                        } catch (NumberFormatException nfe) {
                        }
                    }
                }

                if ((argument.startsWith(HELP) || argument.startsWith(HELP1))) {
                    String acceptableInput = "Ex. java -jar ManagedHive.jar -Drhq.virtual.hive "
                        + "--remote-protocol=http --remote-port=9876 \n"
                        + "Ex. java -jar ManagedHive.jar -h \t(displays acceptable input and exits)\n"
                        + "Ex. java -jar ManagedHive.jar --help \t(displays acceptable input and exits)\n"
                        + "Ex. java -jar ManagedHive.jar \t\t(no arguments, runs with defaults. No discovery differentiator)";
                    System.out.println(acceptableInput);
                    System.exit(0);//bail
                }
            }
        }
        new ManagedHive();
    }

    public ManagedHive() {
        //create ui layout
        initializeUi();
        //initial hive setup
        for (int i = 0; i < basePopulation; i++) {
            addBee();
            try {
                Thread.sleep(3);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }

        //now start the remote api
        initializeRemoteApi();
    }

    /******************* Management capabilities **************************/



    /******************* UI Logic & Components **************************/
    private int space = 7;//horizontal spacing between components
    protected static int basePopulation = 50;//resident hive population
    //swarm time should be (2 or 3)* 30s to allow RHQ to clearly collect angry status.
    protected static int swarmTime = 60 * 1000;//ms. 
    protected static Hive hiveComponent;
    protected static Random generator = new Random(System.currentTimeMillis());
    protected static int beeWidth = 15;
    protected static int beeHeight = 15;
    protected static JTextField currentPopulation;
    protected static ManagedHive CONTROLLER = null;
    protected static Runnable angryTimer = null;
    protected static int angryPackSize = 50;
    protected static int beeAdditionAmount = 13;
    protected static JLabel mood = null;
    protected static JLabel miEnabled = null;
    protected static JTextField swarmTimeDisplayField;
    protected static JTextField populationBaseField;
    protected static JTextField swarmTimeUpdateField;
    protected static JTextField beeCountUpdateField;
    protected static JButton updateConfiguration;
    protected static ObjectMapper mapper = new ObjectMapper();
    protected static ServerSocket apiHandler = null;
    protected static int defaultRemoteApiPort = 9876;
    protected static Protocol defaultProtocol = Protocol.JSON;
    protected static JTabbedPane tabbedPane = null;
    protected static JComboBox remoteOptions = null;
    protected static JTextField remoteApiPort = null;
    protected static JButton remoteApiStart = null;
    protected static JTextArea inbound = null;
    protected static JTextArea outbound = null;

    public enum Validation {
        POPULATION_BASE(50, 2500, "Population Base"), SWARM_TIME(30000, (60000 * 30), "Swarm Time"), BEES_TO_ADD(5,
            150, "Number of bees");
        private int lowest;
        private int highest;
        private String name;
        private JTextField field_value = null;

        public int getLowest() {
            return lowest;
        }

        public int getHighest() {
            return highest;
        }

        public JTextField getField() {
            return field_value;
        }

        public void setField(JTextField field) {
            field_value = field;
        }

        public String getValidationDetails() {
            String validationErrorMessage = name + " values must be an integer >= '" + getLowest() + "' and <= '"
                + getHighest()
                + "'.";
            return validationErrorMessage;
        }

        private Validation(int lowest, int highest, String name) {
            this.lowest = lowest;
            this.highest = highest;
            this.name = name;
        }
    };

    protected static Vector<Validation> validationList = new Vector<Validation>();
    static {
        for (Validation v : Validation.values()) {
            validationList.add(v);
        }
    }

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
        TitledBorder titledBorder1 = new TitledBorder("Realtime Values:");
        titledBorder1.setTitleColor(Color.gray);
        monitorRow.setBorder(titledBorder1);
        top.add(monitorRow);

        //operation row
        JPanel operationsRow = new JPanel();
        operationsRow.setLayout(new BoxLayout(operationsRow, BoxLayout.X_AXIS));
        TitledBorder operationsBorder = new TitledBorder("Operations:");
        operationsBorder.setTitleColor(Color.gray);
        operationsRow.setBorder(operationsBorder);
        top.add(operationsRow);

        {
            //monitor row shows current state of the hive
            JLabel currentPopulationLabel = new JLabel("Current Bee count");
            monitorRow.add(currentPopulationLabel);
            monitorRow.add(Box.createHorizontalStrut(space));
            currentPopulation = new JTextField("" + basePopulation);
            currentPopulation.setEditable(false);
            monitorRow.add(currentPopulation);
            monitorRow.add(Box.createHorizontalStrut(space));

            JLabel maxPopulationLabel = new JLabel("Swarm Time Left(ms)");
            monitorRow.add(maxPopulationLabel);
            monitorRow.add(Box.createHorizontalStrut(space));
            swarmTimeDisplayField = new JTextField("" + swarmTime);
            swarmTimeDisplayField.setEditable(false);
            monitorRow.add(swarmTimeDisplayField);
            monitorRow.add(Box.createHorizontalStrut(space));//spacer

            mood = new JLabel();
            mood.setOpaque(true);
            mood.setBackground(Color.green);
            mood.setText("Calm");
            monitorRow.add(mood);
            monitorRow.add(Box.createHorizontalStrut(space));
            monitorRow.add(Box.createHorizontalStrut(70));
        }

        //Shake hive button
        JButton shake = new JButton("Shake Hive");
        shake.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeShakeOperation();
            }
        });

        //add some more bees operation
        JButton addNBees = new JButton("Add Bees");
        addNBees.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                executeAddBeeOperation();
            }
        });

        {//populate the operations row.
            JTextField instructions = new JTextField(
                "*Use these operations to modify the number of bees protecting the hive.*");
            instructions.setEditable(false);
            operationsRow.add(instructions);
            operationsRow.add(Box.createHorizontalStrut(space));
            operationsRow.add(shake);
            monitorRow.add(Box.createHorizontalStrut(space));
            operationsRow.add(addNBees);
        }

        //configuration row
        JPanel configurationRow = new JPanel();
        configurationRow.setLayout(new BoxLayout(configurationRow, BoxLayout.X_AXIS));
        TitledBorder configurationBorder = new TitledBorder("Configuration:");
        configurationBorder.setTitleColor(Color.gray);
        configurationRow.setBorder(configurationBorder);
        top.add(configurationRow);
        
        {//populate the configuration row.
         //population base
            JLabel basePopulationLabel = new JLabel("Base population");
            configurationRow.add(basePopulationLabel);
            configurationRow.add(Box.createHorizontalStrut(space));
            populationBaseField = new JTextField("" + basePopulation);
            populationBaseField.getDocument().addDocumentListener(new ConfigurationFieldsListener());
            for (Validation v : validationList) {
                if (v.compareTo(Validation.POPULATION_BASE) == 0) {
                    //replace the component
                    int index = validationList.indexOf(v);
                    v.setField(populationBaseField);
                    validationList.set(index, v);
                }
            }
            configurationRow.add(populationBaseField);

            //swarm time
            JLabel swarmTimeLabel = new JLabel("Swarm Time(ms)");
            configurationRow.add(swarmTimeLabel);
            configurationRow.add(Box.createHorizontalStrut(space));
            swarmTimeUpdateField = new JTextField("" + swarmTime);
            swarmTimeUpdateField.getDocument().addDocumentListener(new ConfigurationFieldsListener());
            for (Validation v : validationList) {
                if (v.compareTo(Validation.SWARM_TIME) == 0) {
                    //replace the component
                    int index = validationList.indexOf(v);
                    v.setField(swarmTimeUpdateField);
                    validationList.set(index, v);
                }
            }
            configurationRow.add(swarmTimeUpdateField);

            //add bee amount 
            JLabel beeAdditionAmountLabel = new JLabel("Bees to add");
            configurationRow.add(beeAdditionAmountLabel);
            configurationRow.add(Box.createHorizontalStrut(space));
            beeCountUpdateField = new JTextField("" + beeAdditionAmount);
            beeCountUpdateField.getDocument().addDocumentListener(new ConfigurationFieldsListener());
            for (Validation v : validationList) {
                if (v.compareTo(Validation.BEES_TO_ADD) == 0) {
                    //replace the component
                    int index = validationList.indexOf(v);
                    v.setField(beeCountUpdateField);
                    validationList.set(index, v);
                }
            }
            configurationRow.add(beeCountUpdateField);

            //update button
            updateConfiguration = new JButton("Update configuration");
            updateConfiguration.setEnabled(false);
            updateConfiguration.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    //validate defaults and apply updates if possible.
                    final ArrayList<String> validationMessages = new ArrayList<String>();
                    for (Validation rule : validationList) {
                        JTextField field = rule.getField();
                        if (rule.getField() == null)
                            return;//bail if field not set
                        String newValueString = field.getText();
                        int newValue = -1;
                        try {
                            newValue = Integer.valueOf(newValueString);
                            //apply rules
                            if ((newValue >= rule.getLowest()) && (newValue <= rule.getHighest())) {
                                switch (rule) {
                                case POPULATION_BASE:
                                    basePopulation = newValue;
                                    break;
                                case SWARM_TIME:
                                    swarmTime = newValue;
                                    break;
                                case BEES_TO_ADD:
                                    beeAdditionAmount = newValue;
                                    break;
                                default:
                                    break;
                                }
                            } else {
                                //generate validation message.
                                validationMessages.add(rule.getValidationDetails());
                            }
                        } catch (NumberFormatException nfe) {
                            //generate validation message.
                            validationMessages.add(rule.getValidationDetails());
                        }
                    }
                    //kick off population adjustments if any
                    ManagedHive.hiveComponent.removeBee();
                    //disable the update configuration button.
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {

                            //display validation messages and reset fields
                            if (!validationMessages.isEmpty()) {
                                String message = "The following validation errors were detected:";
                                for (String m : validationMessages) {
                                    message += m;
                                }
                                message += "Resetting fields to previous selections.";
                                //custom title, warning icon
                                JOptionPane.showMessageDialog(CONTROLLER, message, "Input validation:",
                                    JOptionPane.WARNING_MESSAGE);
                                //reset
                                populationBaseField.setText(basePopulation + "");
                                swarmTimeUpdateField.setText(swarmTime + "");
                                beeCountUpdateField.setText(beeAdditionAmount + "");
                            }
                            //disable edit button
                            updateConfiguration.setEnabled(false);
                        }
                    });
                }
            });
            configurationRow.add(updateConfiguration);
        }
        //create tabbed pane
        tabbedPane = new JTabbedPane(SwingConstants.TOP);
        tabbedPane.addTab("Manage Hive", top);
        JPanel remote = new JPanel();
        {//remote api panel
            remote.setLayout(new BorderLayout());
            JPanel column = new JPanel();
            column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
            //managed interface reporting
            miEnabled = new JLabel();
            miEnabled.setOpaque(true);
            //            miEnabled.setBackground(Color.gray);
            //            miEnabled.setText("Remote Api(disabled)");
            //spinder: default to on. Restart impl has unknown issue/problematic
            miEnabled.setBackground(Color.green);
            miEnabled.setText("Remote Api( enabled)");
            column.add(miEnabled);
            //
            remoteOptions = new JComboBox(Protocol.values());
            remoteOptions.setSize(30, 10);
            remoteOptions.setSelectedItem(defaultProtocol);
            remoteOptions.setEnabled(false);
            column.add(remoteOptions);
            JPanel portRow = new JPanel();
            portRow.setLayout(new BoxLayout(portRow, BoxLayout.X_AXIS));
            JLabel label = new JLabel("Port:");
            remoteApiPort = new JTextField("" + defaultRemoteApiPort);
            remoteApiPort.setEnabled(false);

            portRow.add(label);
            portRow.add(Box.createHorizontalStrut(space));
            portRow.add(remoteApiPort);
            column.add(portRow);
            remoteApiStart = new JButton("Enable");
            //spinder 10/9/12: disabling for now as not sure how to bounce the remote server
            // without hanging the UI.
            remoteApiStart.setEnabled(false);
            remoteApiStart.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    //flip the switch
                    if (miEnabled.getText().indexOf("disabled") == -1) {
                        miEnabled.setBackground(Color.gray);
                        miEnabled.setText("Remote Api(disabled)");
                        remoteApiStart.setText("Enable");
                    } else {//enable the remote api
                        //update the remote api
                        ///retrieve the selection
                        String selection = remoteOptions.getSelectedItem()+"";
                        Protocol selectedProtocol = null;
                        for(Protocol p: Protocol.values()){
                            if(p.name().equalsIgnoreCase(selection)){
                                selectedProtocol = p;
                            }
                        }
                        ManagedHive.defaultProtocol = selectedProtocol;
                        ///retrieve specified port
                        String port = remoteApiPort.getText();
                        int selectedPort = defaultRemoteApiPort;
                        try{
                           selectedPort = Integer.parseInt(port);
                        }catch(NumberFormatException nfe){
                            //
                        }
                        miEnabled.setBackground(Color.green);
                        miEnabled.setText("Remote Api( enabled)");
                        remoteApiStart.setText("Disable");
                    }
                }
            });
            column.add(remoteApiStart);
            column.add(Box.createVerticalStrut(100));
            remote.add(column, BorderLayout.WEST);
            //create the Center panel to demonstrate UI message
            JPanel messagingPanel = new JPanel();
            {
                messagingPanel.setLayout(new BoxLayout(messagingPanel, BoxLayout.Y_AXIS));
                ManagedHive.inbound = new JTextArea();
                ManagedHive.inbound.setBorder(new TitledBorder("Inbound"));
                JScrollPane jspTop = new JScrollPane(inbound);
                messagingPanel.add(jspTop);
                ManagedHive.outbound = new JTextArea();
                ManagedHive.outbound.setBorder(new TitledBorder("Outbound"));
                JScrollPane jspBottom = new JScrollPane(outbound);
                messagingPanel.add(jspBottom);
                remote.add(messagingPanel, BorderLayout.CENTER);
            }
        }
        tabbedPane.addTab("Remote Api", remote);

        // center
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
        // build center panel
        buildCenterPanel(center);

        // final component layout
        getContentPane().add(tabbedPane, BorderLayout.NORTH);
        getContentPane().add(center, BorderLayout.CENTER);
        this.setSize(650, 500);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        setVisible(true);

        //assigned shared reference.
        CONTROLLER = this;
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

    /** Bundles up the add Bee operation semantics
     */
    protected static void executeAddBeeOperation() {
        //retrieve addition amount
        int addAmount = beeAdditionAmount;
        for (int i = 0; i < addAmount; i++) {
            addBee();
        }
    }

    /** Bundles up the shake operation semantics
     */
    protected static void executeShakeOperation() {
        for (int i = 0; i < angryPackSize; i++) {
            addBee();
        }
        //kick the hive into angry mode and set angry timer
        if (angryTimer == null) {
            angryTimer = new SwarmTimer();
            Thread t = new Thread(angryTimer);
            t.start();
            //speed up the bees.
            BeeFlight.setDelay(2);
            //update the ui to reflect hive mood.
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ManagedHive.mood.setText("Angry!");
                    ManagedHive.mood.setBackground(Color.red);
                }
            });
        } else {//reset angry timer
            SwarmTimer swarmResponseManager = (SwarmTimer) angryTimer;
            swarmResponseManager.setExpireTime(swarmTime);
        }
    }

    /**
     * Adds a bouncing ball to the canvas and starts a thread to make it bounce
     */
    public static void addBee() {
        Bee b = null;
        //tweak the start position
        int newX = generator.nextInt(BeeFlight.delta);
        int newY = generator.nextInt(BeeFlight.delta);
        b = new Bee(newX, newY);
        hiveComponent.add(b);
        Runnable r = new BeeFlight(b, hiveComponent);
        Thread t = new Thread(r);
        t.start();
    }

    private void initializeRemoteApi() {
            RemoteApi.remoteApiServer(defaultRemoteApiPort);
    }
}

class Hive extends JComponent {
    //entire hive population.
    private static Vector<Bee> population = new Vector<Bee>();

    public int getCurrentPopulation() {
        return population.size();
    }

    public void add(Bee b) {
        synchronized (population) {
            population.add(b);
        }
    }

    public void removeBee() {
        synchronized (population) {
            if (population.size() > 0) {
                population.remove(0);
            }
            //if population falls below basePopulation level then add another bee
            if (population.size() < ManagedHive.basePopulation) {
                int delta = ManagedHive.basePopulation - population.size();
                for (int i = 0; i <= delta; i++) {//replenish
                    ManagedHive.addBee();
                }
            }
        }
    }

    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        synchronized (population) {

            for (Bee b : population) {
                g2.fill(b.getShape());
            }
            //update the fields
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ManagedHive.currentPopulation.setText(ManagedHive.hiveComponent.getCurrentPopulation() + "");
                    if (ManagedHive.angryTimer != null) {
                        ManagedHive.swarmTimeDisplayField.setText(((SwarmTimer) ManagedHive.angryTimer).getExpireTime() + "");
                    } else {
                        ManagedHive.swarmTimeDisplayField.setText("0");
                    }
                    //include updates for a few more fields as well now that not only updated by GUI
                    //after UI edit starts do not update the UI for 7 seconds
                    //to allow UI user to makes changes.
                    long currentTime = System.currentTimeMillis();
                    if (currentTime > (ConfigurationFieldsListener.getLastEditStart() + (7 * 1000))) {
                        ManagedHive.populationBaseField.setText(ManagedHive.basePopulation + "");
                        ManagedHive.swarmTimeUpdateField.setText(ManagedHive.swarmTime + "");
                        ManagedHive.beeCountUpdateField.setText(ManagedHive.beeAdditionAmount + "");
                        //disable edit button
                        ManagedHive.updateConfiguration.setEnabled(false);
                    }
                }
            });
        }
    }
}

/** Represents the cartesion/graphical components to
 *  define a bee.
 */
class Bee {

    //properties of typical cartesion component. 
    private int xWidth = ManagedHive.beeWidth;
    private int yWidth = ManagedHive.beeHeight;

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
        //randomly change dimensions to simulate flying bee
        int nextX = ManagedHive.generator.nextInt(ManagedHive.beeWidth);
        int nextY = ManagedHive.generator.nextInt(ManagedHive.beeHeight);
        if (nextX < 1)
            nextX = 1;
        if (nextY < 1)
            nextY = 1;
        if ((x > 0) && (y > 0)) {
            return new Ellipse2D.Double(x, y, nextX, nextY);
        } else {
            return new Ellipse2D.Double(x, y, 0, 0);
        }
    }

    //return to invisible
    public void clear() {
        xWidth = 0;
        xWidth = 0;
        ManagedHive.hiveComponent.removeBee();
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

    public static int DELAY = 5;

    public static int getDelay() {
        return DELAY;
    }

    public static void setDelay(int delay) {
        if ((delay >= 2) || (delay <= 6)) {//2 <delay <= 6  >
            DELAY = delay;
        }//otherwise ignore
    }

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

//swarm anger timer
class SwarmTimer implements Runnable {
    private static int timeToLive;

    public SwarmTimer() {
        timeToLive = ManagedHive.swarmTime;//default to 1 minute
    }

    @Override
    public void run() {
        try {
            while (timeToLive > 0) {
                Thread.sleep(1000);//sleep for a second
                timeToLive = timeToLive - 1000;
            }
            //reset visual hive state flags
            //update the fields
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ManagedHive.mood.setText("Calm");
                    ManagedHive.mood.setBackground(Color.green);
                    //speed up the bees.
                    BeeFlight.setDelay(5);
                }
            });
            //null out angrySwarm
            ManagedHive.angryTimer = null;
        } catch (InterruptedException e) {
        }
    }

    public void setExpireTime(int swarmTime) {
        //only accept swarm times less than 10 mins and greater then 1 min(s).
        if ((swarmTime >= Validation.SWARM_TIME.getLowest()) || (swarmTime <= Validation.SWARM_TIME.getHighest())) {
            timeToLive = swarmTime;
        } else {
            ManagedHive.LOG.log(Level.WARNING, "New value '" + swarmTime + "' is not acceptable. "
                + Validation.SWARM_TIME.getValidationDetails());
        }
    }

    public static int getExpireTime() {
        return timeToLive;
    }
}

/* Listener for JTextFields that re-enable the JButton on chance.
 */
class ConfigurationFieldsListener implements DocumentListener {
    private static long currentTimeEditStart = -1;

    public static long getLastEditStart() {
        return currentTimeEditStart;
    }
    @Override
    public void removeUpdate(DocumentEvent e) {
        enableEdit();
    }
    private void enableEdit() {
        ManagedHive.updateConfiguration.setEnabled(true);
        currentTimeEditStart = System.currentTimeMillis();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        enableEdit();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        enableEdit();
    }
}

/**
 * Responsible for running the remote api server and defining
 * the protocol for interacting with the management server.
 *   -every line to server and client should postpend a newline character. Ex. \n.
 *      multiline json with newline characters are not supported. Use spaces or tabs.
 *   -a request to server with no json body is assumed to be a request for current state
 *   -a request with json content is assumed request to update state to values passed in.
 *       Note: server side validation may still not accepte invalid values.
 *   -by setting the action field to "Shake" or "Add", case insensitive is assumed a 
 *       request to execute that action.
 *   -only ONE of following request paths is possible for each operation 
 *      i)request for current state : (empty request)
 *      ii)request for operation or : (non empty request WITH action field set)
 *      iii)request to update configuration : (non empty request WITHOUT action field set)
 */
class RemoteApi implements HttpRequestHandler {
    private static String host = "localhost";
    private static int port = ManagedHive.defaultRemoteApiPort;//default port
    private static boolean continueToRun = true;
    private static HttpService httpService = null;

    //supported operations.
    public enum Operation {
        Shake, Add
    };

    //supported response states
    public enum State {
        Success, Fail
    };

    //supported remote api interfaces
    public enum Protocol {
        JSON, HTTP_JSON
    };

    public static void updateServer(boolean newState) {
        continueToRun = newState;
    }

    public static void remoteApiServer(int port){
        //try to launch the requested port if not get random available one
        try {
            ManagedHive.apiHandler = new ServerSocket(port);
        } catch (IOException ioe) {
            try {
                ManagedHive.apiHandler = new ServerSocket(0);
            } catch (IOException e) {//if fails here give up, and spit out stack trace
                e.printStackTrace();
            }//have OS select free on for us.
            port = ManagedHive.apiHandler.getLocalPort();
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ManagedHive.remoteApiPort.setText(ManagedHive.apiHandler.getLocalPort() + "");
                }
            });
        }
        // initialization
        SyncBasicHttpParams params = null;
        if (ManagedHive.defaultProtocol.compareTo(Protocol.HTTP_JSON) == 0) {
            // Set up the HTTP protocol processor
            params = new SyncBasicHttpParams();
            params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, 300)
                .setIntParameter(CoreConnectionPNames.SOCKET_BUFFER_SIZE, 8 * 1024)
                .setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false)
                .setBooleanParameter(CoreConnectionPNames.TCP_NODELAY, true)
                .setParameter(CoreProtocolPNames.ORIGIN_SERVER, "HttpComponents/1.1");
            HttpProcessor httpproc = new ImmutableHttpProcessor(new HttpResponseInterceptor[] { new ResponseDate(),
                new ResponseServer(), new ResponseContent(), new ResponseConnControl() });

            // Set up request handlers
            HttpRequestHandlerRegistry reqistry = new HttpRequestHandlerRegistry();
            reqistry.register("*", new RemoteApi());

            // Set up the HTTP service
            httpService = new HttpService(httpproc, new DefaultConnectionReuseStrategy(),
                new DefaultHttpResponseFactory(), reqistry, params);
        }

        while (continueToRun) {
            Socket connectionSocket;
            try {
                connectionSocket = ManagedHive.apiHandler.accept();
                if (ManagedHive.defaultProtocol.compareTo(Protocol.HTTP_JSON) == 0) {
                    HttpContext context = new BasicHttpContext(null);
                    DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
                    conn.bind(connectionSocket, params);
                    try {
                        httpService.handleRequest(conn, context);
                    } catch (SocketTimeoutException ste) {
                        //ignore the exception.
                    }
                    //bail. request processing complete.
                    continue;
                }

                BufferedReader inFromClient = new BufferedReader(new InputStreamReader(
                    connectionSocket.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(connectionSocket.getOutputStream());
                String clientRequest = inFromClient.readLine();
                //generate response. Empty request then return state otherwise attempt to load new state
                clientRequest = clientRequest.trim();
                final String clientRequestFinal = clientRequest;
                String response = "";
                boolean simpleReadRequest = false;
                final RequestStatus status = new RequestStatus();//to respond to non-readonly requests
                if (clientRequest.isEmpty()) {//empty request, return state
                    ApplicationState state = new ApplicationState();
                    response = ManagedHive.mapper.writeValueAsString(state);
                    simpleReadRequest = true;
                } else {//request for state update or operation execution
                    processActionableRequests(clientRequest, status);
                }
                if (!simpleReadRequest) {//not a simple read so return status to users that want to know.
                    response = ManagedHive.mapper.writeValueAsString(status);
                }
                //append newLine
                response += "\n";
                final String complete = response;
                outToClient.writeBytes(response);
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if ((status != null) && (status.getDetail() == null)) {
                            ManagedHive.inbound.setText("(request empty - replying with current state)" + "");
                            ManagedHive.outbound.setText(complete + "");
                        } else {
                            ManagedHive.inbound.setText(clientRequestFinal + "");
                            ManagedHive.outbound.setText(complete);
                        }
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            catch (HttpException e) {
                e.printStackTrace();
            }
        }
    }

    private static void processActionableRequests(String clientRequest, RequestStatus status) {
        try {
            ApplicationState state = ManagedHive.mapper.readValue(clientRequest, ApplicationState.class);
            //detect operations request
            String action = state.getAction();
            action = action.trim();
            if (!action.isEmpty()) {
                //attempt to locate valid action
                Operation operation = null;
                for (Operation op : Operation.values()) {
                    if (op.name().equalsIgnoreCase(action)) {
                        operation = op;
                    }
                }
                //action on that action
                if (operation != null) {
                    status.setStatus(State.Success.name());
                    switch (operation) {
                    case Shake:
                        ManagedHive.executeShakeOperation();
                        status.setDetail("Successfully executed " + operation.Shake.name());
                        break;
                    case Add:
                        ManagedHive.executeAddBeeOperation();
                        status.setDetail("Successfully executed " + operation.Add.name() + " Bee");
                        break;
                    }
                } else {// unknown operation.
                    status.setStatus(State.Fail.name());
                    status.setDetail("Unable to recognise operation '" + action + "'. Only the following operations ["
                        + State.values() + "] allowed.");
                }
            } else {//this is not an action but request to update configuration
                //look at configuration values and if within range then update otherwise don't
                ///beePopulationBase
                int value = state.getBeePopulationBase();
                if ((value >= Validation.POPULATION_BASE.getLowest())
                    && (value <= Validation.POPULATION_BASE.getHighest())) {
                    if (ManagedHive.basePopulation != value) {
                        ManagedHive.basePopulation = value;
                    }
                }
                ///swarmTimeBase
                value = state.getSwarmTimeBase();
                if ((value >= Validation.SWARM_TIME.getLowest()) && (value <= Validation.SWARM_TIME.getHighest())) {
                    if (ManagedHive.swarmTime != value) {
                        ManagedHive.swarmTime = value;
                    }
                }
                ///beesToAdd
                value = state.getBeesToAdd();
                if ((value >= Validation.BEES_TO_ADD.getLowest()) && (value <= Validation.BEES_TO_ADD.getHighest())) {
                    if (ManagedHive.beeAdditionAmount != value) {
                        ManagedHive.beeAdditionAmount = value;
                    }
                }
                status.setStatus(State.Success.name());
                status.setDetail("The requested updates were applied where"
                    + " possible and ignored when validation rules were violated.");
                //kick off population adjustments if any
                ManagedHive.hiveComponent.removeBee();
            }
        } catch (Exception ex) {
            status.setStatus(State.Fail.name());
            status.setDetail(ex.getMessage());
        }
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException,
        IOException {
        //filter out non GET/POST requests
        String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
        if (!method.equals("GET") && !method.equals("POST")) {
            throw new MethodNotSupportedException(method + " method not supported");
        }
        //if request includes content, Ex Post
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
            String received = EntityUtils.toString(entity);
            RequestStatus status = new RequestStatus();
            processActionableRequests(received, status);
            String bodyText = ManagedHive.mapper.writeValueAsString(status);
            StringEntity body = new StringEntity(bodyText);
            response.setEntity(body);
            return;
        }

        //otherwise handle read request
        response.setStatusCode(HttpStatus.SC_OK);
        ApplicationState status = new ApplicationState();
        String bodyText = ManagedHive.mapper.writeValueAsString(status);
        StringEntity body = new StringEntity(bodyText);
        response.setEntity(body);
    }
}

/**
 * Class bundles up the current application state for serialization to JSON.
 */
class ApplicationState {
    /** Constructor will query the relevant properties at instantiation.
     */
    public ApplicationState() {//populates current state
        //real time
        currentBeePopulation = ManagedHive.hiveComponent.getCurrentPopulation();
        swarmTimeLeft = ((SwarmTimer) ManagedHive.angryTimer).getExpireTime();
        hiveAngry = (swarmTimeLeft > 0) ? true : false;
        //current configuration settings
        beePopulationBase = ManagedHive.basePopulation;
        swarmTimeBase = ManagedHive.swarmTime;
        beesToAdd = ManagedHive.angryPackSize;
        action = "";//empty as only useful when executing operations
    }

    public int getCurrentBeePopulation() {
        return currentBeePopulation;
    }

    public void setCurrentBeePopulation(int currentBeePopulation) {
        this.currentBeePopulation = currentBeePopulation;
    }

    public int getSwarmTimeLeft() {
        return swarmTimeLeft;
    }

    public void setSwarmTimeLeft(int swarmTimeLeft) {
        this.swarmTimeLeft = swarmTimeLeft;
    }

    public boolean isHiveAngry() {
        return hiveAngry;
    }

    public void setHiveAngry(boolean hiveAngry) {
        this.hiveAngry = hiveAngry;
    }

    public int getBeePopulationBase() {
        return beePopulationBase;
    }

    public void setBeePopulationBase(int beePopulationBase) {
        this.beePopulationBase = beePopulationBase;
    }

    public int getSwarmTimeBase() {
        return swarmTimeBase;
    }

    public void setSwarmTimeBase(int swarmTimeBase) {
        this.swarmTimeBase = swarmTimeBase;
    }

    public int getBeesToAdd() {
        return beesToAdd;
    }

    public void setBeesToAdd(int beesToAdd) {
        this.beesToAdd = beesToAdd;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    private int currentBeePopulation;
    private int swarmTimeLeft;
    private boolean hiveAngry;
    private int beePopulationBase;
    private int swarmTimeBase;
    private int beesToAdd;
    private String action;
}

class RequestStatus {
    @Override
    public String toString() {
        return "{" + "status:" + getStatus() + ",detail:" + getDetail() + "}";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }


    private String status;
    private String detail;
}
