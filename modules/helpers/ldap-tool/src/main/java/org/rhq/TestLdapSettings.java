package org.rhq;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InvalidSearchFilterException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

//import org.rhq.enterprise.server.util.security.UntrustedSSLSocketFactory;

/* Is a development test tool that allows the user to simulate the RHQ server side
 * LDAP calls during auth/authz operations.
 *
 * The specific LDAP logic below needs to mirror the latest RHQ code and allow the user
 * to test our their configuration without requring a specific RHQ/JON build as a dependency.
 * 
 * NOTE: To avoid a runtime dependency on specific versions of RHQ or JON, the small implementation
 * methods were copied into this class with minimatl changes for logging and ui messaging. The 
 * definitive implementation for each 'copied' method can be found in LDAPGroupManagerBean.
 *
 * @author Simeon Pinder
 */
public class TestLdapSettings extends JFrame {
	//shared fields
	private JTextArea testResults;
	private JCheckBox showPasswords;
	private JCheckBox ssl;
	private JLabel groupPageSizeName;
	private JTextField groupMemberQueryValue;
	private JTextField testUserNameValue;
	private JTextField testUserPasswordValue;
	private HashMap<String, JTextField> fieldMappings;
	private String[] keys;
	private JCheckBox enableLdapReferral;
	private JCheckBox enableVerboseDebugging;
	private JCheckBox enableVerboseGroupParsing;
	private JCheckBox iterativeVerboseLogging;
	private JCheckBox enablePosixGroups;
	private JCheckBox enable32xFeatures;
    private JMenuBar menuBar;
    private String advdb = "**Verbose:debug ----";
    private static final String BASEDN_DELIMITER = ";";

	private static final long serialVersionUID = 1L;
	int textBoxWidth = 20;
	private static JPanel top = null;
	private static JPanel testUserRegion = null;
	private static Properties env=null;
	
	public static void main(String args[]) {
		new TestLdapSettings();
	}
	//After enabling support for Query parsing, we need to warn users of the effects.
	final String warnMessage = "<html>***WARNING: Depending upon<br>" +
			"i)how the ldap server is configured <br>" +
			"ii)client query paging settings <br>" +
			" enabling <b>'more verbose logging'</b>,<br>" +
			" <b>'more detailed group parsing'</b> AND/OR <b>'also log to console'</b> may cause the console to hang/freeze <br>" +
			" as the LDAP tool continues to parse large query results. If that occurs it is <br>" +
			" suggested that you stop this tool and re-run your queries with <b>'also log to console'</b> so that the console logs<br>" +
			" will show which dataset is causing the delay and then you should modify your search|group|member<br>" +
			" filters accordingly to <b>return smaller results</b> and/or <b>consume larger payloads</b>.<br>" +
			"***WARNING</html>";
	
	// Configure window properties
	private TestLdapSettings() {

		setTitle("Check LDAP Settings: Simulates LDAP checks/queries of RHQ LDAP integration");
		getContentPane().setLayout(new BorderLayout());
		menuBar = new JMenuBar();
		JMenu menu = new JMenu("View ***Warning");
        JMenuItem menuItem = new JMenuItem(warnMessage);
        menu.add(menuItem);
		menuBar.add(menu);
		setJMenuBar(menuBar);
		// top panel definition
		top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setBorder(LineBorder.createGrayLineBorder());
		//define checkbox here as it's checked when generating UI.
		showPasswords = new JCheckBox("show passwords:");
		showPasswords.setSelected(false);
		
		keys = new String[] { "URL:", "Search Filter:",
				              "Search Base:","Login Property",
				              "Username:", "Group Search Filter:",
				               "Password:", "Group Member Filter:",
				               };
		fieldMappings = loadUiFields(top, keys);

		//add the two checkboxes for additiona debugging options
		enableLdapReferral= new JCheckBox("[follow] ldap referrals");
		enableLdapReferral.setSelected(false);
		enableVerboseDebugging= new JCheckBox("more verbose logging");
		enableVerboseDebugging.setSelected(false);
		enableVerboseDebugging.setToolTipText(warnMessage);
		enableVerboseGroupParsing= new JCheckBox("more detailed group parsing");
		enableVerboseGroupParsing.setSelected(false);
		enableVerboseGroupParsing.setToolTipText("*Take care when using this mode with a large number of groups* Every group discovered is parsed/listed.");
		iterativeVerboseLogging= new JCheckBox("also log to console");
		iterativeVerboseLogging.setSelected(false);
		iterativeVerboseLogging.setToolTipText("This mode is useful when the test tool is having difficulty returning results from large queries.");
		iterativeVerboseLogging.setToolTipText(warnMessage);
		enablePosixGroups= new JCheckBox("is Posix Group");
		enablePosixGroups.setSelected(false);
		enablePosixGroups.setEnabled(false);

		//put into 3.2.x functionality row
		JPanel jon32xRegion = new JPanel();
		jon32xRegion.setLayout(new FlowLayout(FlowLayout.LEFT));
		LineBorder jon32xLineBorder = new LineBorder(Color.BLACK, 2);
		TitledBorder jon32xBorder = new TitledBorder(jon32xLineBorder, "JON 3.2.x/RHQ 4.8.x specific features:");
		jon32xRegion.setBorder(jon32xBorder);
		enable32xFeatures= new JCheckBox("enable JON 3.2.x/RHQ 4.8.x features");
		enable32xFeatures.setToolTipText("This enables features not available before RHQ 4.8.x/JON 3.2.x.");
		enable32xFeatures.setSelected(false);
		enable32xFeatures.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(enable32xFeatures.isSelected()){
					groupPageSizeName.setEnabled(true);
					groupMemberQueryValue.setEnabled(true);
					groupMemberQueryValue.setEditable(true);
					groupMemberQueryValue.setText("1000");
					enablePosixGroups.setEnabled(true);
				}else{
					groupMemberQueryValue.setText("");
					groupPageSizeName.setEnabled(false);
					groupMemberQueryValue.setEnabled(false);
					groupMemberQueryValue.setEditable(false);
					enablePosixGroups.setEnabled(false);
					enablePosixGroups.setSelected(false);
				}
			}
		});

		jon32xRegion.add(enable32xFeatures);
		groupPageSizeName = new JLabel("Group Query Page Size:");
		groupPageSizeName.setEnabled(false);
		groupMemberQueryValue = new JTextField(10);
		groupMemberQueryValue.setText("1000");
		groupMemberQueryValue.setEditable(false);
		jon32xRegion.add(groupPageSizeName);
		jon32xRegion.add(groupMemberQueryValue);
		jon32xRegion.add(enablePosixGroups);
		top.add(jon32xRegion);
		
		//put into row display
		JPanel advancedDebugRegion = new JPanel();
		advancedDebugRegion.setLayout(new FlowLayout(FlowLayout.LEFT));
		LineBorder advancedBorder = new LineBorder(Color.BLACK, 2);
		TitledBorder debugBorder = new TitledBorder(advancedBorder, "Debug: **Warning --<hover HERE>**");
		advancedDebugRegion.setBorder(debugBorder);
		advancedDebugRegion.add(enableLdapReferral);
		advancedDebugRegion.add(enableVerboseDebugging);
		advancedDebugRegion.add(enableVerboseGroupParsing);
		advancedDebugRegion.add(iterativeVerboseLogging);
		advancedDebugRegion.setToolTipText(warnMessage);
		top.add(advancedDebugRegion);

		JPanel securityPanel = new JPanel();
		securityPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		showPasswords.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
			  SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					//store off existing value
					String existingValue = "";
					String existingTestUserPass = "";
					JTextField current = fieldMappings.get("Password:");
					if(current instanceof JPasswordField){
						JPasswordField pass = ((JPasswordField)current);
						if(pass!=null){
							char[] charArray = pass.getPassword();
							if(charArray.length>0){
							existingValue = new String(charArray);
							}
						}
					}else{
						existingValue = current.getText();
					}
					//save off test user password as well
					if(testUserPasswordValue instanceof JPasswordField){
						JPasswordField pass = ((JPasswordField)testUserPasswordValue);
						if(pass!=null){
							char[] charArray = pass.getPassword();
							if(charArray.length>0){
							existingTestUserPass = new String(charArray);
							}
						}
					}else{
						existingTestUserPass=testUserPasswordValue.getText();
					}
					
					JTextField updatedContainer = null;
					if(showPasswords.isSelected()){
						updatedContainer = new JTextField(textBoxWidth);
						updatedContainer.setText(existingValue);
						testUserPasswordValue = new JTextField(textBoxWidth);
						testUserPasswordValue.setText(existingTestUserPass);
					}else{
						updatedContainer = new JPasswordField(textBoxWidth);
						updatedContainer.setText(existingValue);
						testUserPasswordValue = new JPasswordField(textBoxWidth);
						testUserPasswordValue.setText(existingTestUserPass);
					}
					//locate the JPanel and rebuild it Should be at index 3
					JPanel passwordRow = (JPanel) top.getComponent(3);
//					JTextField jf = (JTextField) passwordRow.getComponent(1);
					//store off existing components
					Component[] existing = new Component[passwordRow.getComponentCount()];
					for(int i=0; i<passwordRow.getComponentCount();i++){
						existing[i] = passwordRow.getComponent(i);
					}
					passwordRow.removeAll();
					for(int j=0;j<existing.length;j++){
						if(j==1){//insert new JTextField instead
							passwordRow.add(updatedContainer);
						}else{
							passwordRow.add(existing[j]);
						}
					}
					//reload testUserRegion
					//store off existing components
					Component[] existingTest = new Component[testUserRegion.getComponentCount()];
					for(int i=0; i<testUserRegion.getComponentCount();i++){
						existingTest[i] = testUserRegion.getComponent(i);
					}
					testUserRegion.removeAll();
					for(int j=0;j<existingTest.length;j++){
						if(j==3){//insert new JTextField instead
							testUserRegion.add(testUserPasswordValue);
						}else{
							testUserRegion.add(existingTest[j]);
						}
					}
					
					top.revalidate();
					top.repaint();
				}
			});	
			}
		});
		securityPanel.add(showPasswords);
		ssl = new JCheckBox("SSL:");
		ssl.setEnabled(false);
		securityPanel.add(ssl);
		top.add(securityPanel);
		
		// test user auth region
		testUserRegion = new JPanel();
		testUserRegion.setLayout(new FlowLayout(FlowLayout.LEFT));
		LineBorder border = new LineBorder(Color.BLUE, 2);
		TitledBorder tBorder = new TitledBorder(border, "Authentication/Authorization Check Credentials: (insert valid ldap user assigned to group)");
		testUserRegion.setBorder(tBorder);
		JLabel testUserName = new JLabel("Test UserName:");
		testUserNameValue = new JTextField(textBoxWidth);
		JLabel testUserPassword = new JLabel("Test Password:");
//		testUserPasswordValue = new JTextField(textBoxWidth);
		testUserPasswordValue = new JPasswordField(textBoxWidth);
		testUserRegion.add(testUserName);
		testUserRegion.add(testUserNameValue);
		testUserRegion.add(testUserPassword);
		testUserRegion.add(testUserPasswordValue);
		top.add(testUserRegion);

		// center
		JPanel center = new JPanel();
		center.setLayout(new BoxLayout(center, BoxLayout.X_AXIS));
		// build center panel
		buildCenterPanel(center);

		// final component layout
		getContentPane().add(top, BorderLayout.NORTH);
		getContentPane().add(center, BorderLayout.CENTER);
		this.setSize(720, 700);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		setVisible(true);
	}

	// define the center display panel.
	private void buildCenterPanel(JPanel center) {
		// First element is Test Button
		JButton test = new JButton("Test Settings");
		center.add(test);
		// second is large text box that display ldap queries
		testResults = new JTextArea("(click button to test settings values: simulates 4 separate checks showing ldap filters used)",
				40, 40);
		JScrollPane jsp = new JScrollPane(testResults);
		center.add(jsp);
		test.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				testResults.setText("");//clear out empty msg
				//trim spaces from all fields
				String ldapServer = fieldMappings.get(keys[0]).getText().trim();
				String searchFilter = fieldMappings.get(keys[1]).getText().trim();
				String searchBase = fieldMappings.get(keys[2]).getText().trim();
				String loginProperty = fieldMappings.get(keys[3]).getText().trim();
				String bindUserName = fieldMappings.get(keys[4]).getText().trim();
				String groupSearchFilter = fieldMappings.get(keys[5]).getText().trim();
				String bindPassword = fieldMappings.get(keys[6]).getText().trim();
				String groupMemberFilter = fieldMappings.get(keys[7]).getText().trim();
				String groupMemberQuerySize = groupMemberQueryValue.getText().trim();
				String testUserName = testUserNameValue.getText().trim();
				String testUserPassword = testUserPasswordValue.getText().trim();
				// validate initial required elements
				String msg = null;
				boolean proceed = true;
				//valid required details set.
				if (ldapServer.isEmpty() || bindUserName.isEmpty()
						|| bindPassword.isEmpty() || searchBase.isEmpty()) {
					msg ="STEP-1:FAIL: "+ keys[0] + ", " + keys[2] + ", " + keys[4] + ", "
							+ keys[6] + " cannot be empty to proceed.";
					log(msg);
					proceed = false;
				}
				env = null;
				InitialLdapContext ctx = null;
				if (proceed) {// attempt initial ldap bind from RHQ server
					msg = "STEP-1:TESTING: Attempting to bind to server:" + ldapServer
							+ "\n with user '" + bindUserName
							+ "' and password entered.";
					log(msg);
					env = getProperties(ldapServer);
					env.setProperty(Context.SECURITY_PRINCIPAL, bindUserName);
					env.setProperty(Context.SECURITY_CREDENTIALS, bindPassword);
					env.setProperty(Context.SECURITY_AUTHENTICATION, "simple");
					//put the rest of the LDAP properties into the Properties instance for use later.
					//there still needs to be separate variables since some are for UI validation.
					env.setProperty(SystemSetting.LDAP_GROUP_FILTER.getInternalName(), groupSearchFilter);
					env.setProperty(SystemSetting.LDAP_GROUP_MEMBER.getInternalName(), groupMemberFilter);
					env.setProperty(SystemSetting.LDAP_BASE_DN.getInternalName(), searchBase);
					env.setProperty(SystemSetting.LDAP_LOGIN_PROPERTY.getInternalName(), loginProperty);
					env.setProperty(SystemSetting.LDAP_BIND_DN.getInternalName(), bindUserName);
					env.setProperty(SystemSetting.LDAP_BIND_PW.getInternalName(), bindPassword);
					env.setProperty(SystemSetting.LDAP_GROUP_QUERY_PAGE_SIZE.getInternalName(), groupMemberQuerySize);
					
					try {
						ctx = new InitialLdapContext(env, null);
						msg = "STEP-1:PASS: LDAP bind credentials are correct. Successfully connected to '"
								+ ldapServer
								+ "'.\n This means the LDAP Bind credentials for the RHQ Server authentication/authorization requests to ldap server "
						        + "are correct.";
						if(enableVerboseDebugging.isSelected()){
							msg+="\n"+advdb+" LDAP simple authentication bind successful.";
						}
						log(msg);
						proceed = true;
					} catch (Exception ex) {
						msg = "STEP-1:FAIL: Unable to connect to the LDAP server with credentials specified.\n";
						msg+="Exception:"+ex.getMessage();
						if(enableVerboseDebugging.isSelected()){
							msg = appendStacktraceToMsg(msg, ex);
						}
						log(msg);
						proceed = false;
					}
				}
				if (proceed) {// retrieve test credentials to test run auth
					// load search controls
					SearchControls searchControls = getSearchControls();
					// validating searchFilter and test user/pass creds
					proceed = true;
					if (testUserName.isEmpty() || (testUserPassword.isEmpty())) {
						msg = "STEP-2:FAIL: Test Username/Password fields cannot be empty for this step.";
						log(msg);
						proceed = false;
					}
					// testing a valid user involves a filtered ldap search
					// using the loginProperty, and optionally searchFilter
					String userDN = "";
					if (proceed) {
						// default loginProperty to cn if it's not set
						if (loginProperty.isEmpty()) {
							loginProperty = "cn";
							if(enableVerboseDebugging.isSelected()){
								String mesg = "As you have not specified a login property, defaulting to 'cn'";
								log(advdb+" "+msg);
							}
						}
						String filter;
						if (!searchFilter.isEmpty()) {
							filter = "(&(" + loginProperty + "=" + testUserName
									+ ")" + "(" + searchFilter + "))";
						} else {
							filter = "(" + loginProperty + "=" + testUserName
									+ ")";
						}
						if(enableVerboseDebugging.isSelected()){
							log(advdb+" The searchfilter is optionally appended to login property for additional shared attribute across users.");
						}
						msg = "STEP-2:TESTING: To validate the test user the following LDAP filtered component will be used to find matching users:\n";
						msg += filter;
						log(msg);
						// test out the search on the target ldap server
						try {
							String[] baseDNs = searchBase.split(";");
							for (int x = 0; x < baseDNs.length; x++) {
								NamingEnumeration answer = ctx.search(
										baseDNs[x], filter, searchControls);
								if(enableVerboseDebugging.isSelected()){
									log(advdb+" this search was excuted against DN component '"+baseDNs[x]+"'.");
								}
								// boolean ldapApiNpeFound = false;
								if (!answer.hasMoreElements()) {
									msg="STEP-2:WARN Unable to locate a matching users for the filter'"+filter+
									"'. Please check your loginProperty. Usually 'cn' or 'uid'";
									log(msg);
									continue;
								}
								// Going with the first match
								SearchResult si = (SearchResult) answer.next();

								// Construct the UserDN
								userDN = si.getName() + "," + baseDNs[x];
								msg = "STEP-2:PASS: The test user '"
										+ testUserName
										+ "' was succesfully located, and the following userDN will be used in authorization check:\n";
								msg += userDN;
								log(msg);

								ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, userDN);
								ctx.addToEnvironment(Context.SECURITY_CREDENTIALS,testUserPassword);
								ctx.addToEnvironment(Context.SECURITY_AUTHENTICATION,"simple");

								// if successful then verified that user and pw
								// are valid ldap credentials
								ctx.reconnect(null);
								msg = "STEP-2:PASS: The user '"
										+ testUserName
										+ "' was succesfully authenticated using userDN '"
										+ userDN + "' and password provided.\n"
										+"*Note: the loginProperty must match the loginProperty listed in dn: for the user. It is the DN that RHQ will lookup and use.";
								log(msg);
							}
						} catch (Exception ex) {
							msg = "STEP-2:FAIL: There was an error while searching for or authenticating the user '"
									+ testUserName + "'\n";
							msg += ex.getMessage();
							if(enableVerboseDebugging.isSelected()){
								msg = appendStacktraceToMsg(msg, ex);
							}
							log(msg);
							proceed=false;
						}
					}
					// with authentication completed, now check authorization.
					// validate filter components to list all available groups
					proceed = false;
					if (!groupSearchFilter.isEmpty()) {
						Set<Map<String, String>> ret = new HashSet<Map<String, String>>();
						String filter = null;
						
			            if (groupSearchFilter.startsWith("(") && groupSearchFilter.endsWith(")")){
			                filter = groupSearchFilter;  // RFC 2254 does not allow for ((expression))
			            }else{
						    filter = String
								.format("(%s)", groupSearchFilter);
			            }
						msg = "STEP-3:TESTING: This ldap filter "
								+ filter
								+ " will be used to locate ALL available LDAP groups";
						log(msg);
						
						Properties systemConfig = populateProperties(env);
						
						ret = buildGroup(systemConfig, filter);
						msg = "STEP-3:TESTING: Using Group Search Filter '"
								+ filter + "', " + ret.size()
								+ " ldap group(s) were located.\n";
						if (ret.size() > 0) {
							HashMap<String, String>[] ldapLists = new HashMap[ret
									.size()];
							ret.toArray(ldapLists);
							// in this mode go beyond the first ten results.
							if (enableVerboseGroupParsing.isSelected()) {
								msg += "STEP-3:PASS: Listing 'all' of the ldap groups located: \n";
								for (int i = 0; i < ret.size(); i++) {
									msg += ldapLists[i] + "\n";
								}
							} else {// otherwise only show first 10[subset of
									// available groups]
								msg += "STEP-3:PASS: Listing a few(<=10) of the ldap groups located: \n";
								for (int i = 0; (i < ret.size() && i < 10); i++) {
									msg += ldapLists[i] + "\n";
								}
							}
							proceed = true;// then can proceed to next step.
						}
						log(msg);
					} else {
						msg = "STEP-3:FAIL: Group Search Filter: cannot be empty to proceed.";
						log(msg);
						proceed=false;
					}
					// retrieve lists of authorized groups available for the
					if (proceed) {
						// check groupMember
						if (!groupMemberFilter.isEmpty()) {
//							Map<String, String> userDetails = new HashMap<String, String>();
//							   userDetails = findLdapUserDetails(userDN);
							   Set<String> userDetails = findAvailableGroupsFor(testUserName);   
							
							   if(!userDetails.isEmpty()){
								   proceed=true;
							   }
						} else {
							msg = "STEP-4:FAIL: Group Member Filter must be non-empty to proceed with simulating authorization check for test user.";
							log(msg);
						}
					}
					if(proceed){
						msg="COMPLETED:PASS: The current settings, for successful steps, should be correct to enter into your RHQ server.";
						msg+="\n\n\n\n When you encounter failures, warnings or other unexpected results you should use an external ";
						msg+="LDAP search utility to check that the generated filters return the expected LDAP results.";
						log(msg);
					}
				}
			}
		});
	}
	
	private String appendStacktraceToMsg(String msg, Exception ex) {
		String moreVerbose = "";
		moreVerbose+=advdb+" Exception type:"+ex.getClass()+"\n";
		moreVerbose+=advdb+" Exception stack trace reference:"+ex.getStackTrace()+"\n";
		if(ex.getStackTrace()!=null){
			StringWriter sw = new StringWriter();
		    PrintWriter pw = new PrintWriter(sw);
		    ex.printStackTrace(pw);
		    moreVerbose+=advdb+" stack trace reference:"+sw.toString();
		}
		msg+="\n"+moreVerbose;
		return msg;
	}
	
	private boolean containsIllegalLdap(String currentValue) {
		boolean invalidData = false;
		if((currentValue!=null)&&(!currentValue.trim().isEmpty())){
			//TODO: spinder 3/17/11: need to figure out regex to filter/detect bad data in returned ldap. Giving up for now.
//			String regex = "(?<=(?:[^\\]|^)(\\\\)+|[^\\]|^)[/,+\"><;=#]|(?<=(?:[^\\]|^)(\\\\)+|[^\\]|^)\\(?!\\|[/,+\"><;=#]| $|(?<=^\\) )|^";
//			regex = "(?<=(?:[^\\\\]|^)(\\\\\\\\)+|[^\\\\]|^)[/,+\\\"><;=#]|(?<=(?:[^\\\\]|^)(\\\\\\\\)+|[^\\\\]|^)\\\\(?!\\\\|[/,+\\\"><;=#]| $|(?<=^\\\\) )|^";
//			System.out.println("++++++++ CURR VAL:"+currentValue+":INV-CHeck:"+currentValue.matches(",+\"\\<;\n=/")+":NEWCHECK:"+(currentValue.matches(regex)));
//			if(currentValue.matches(",+\"\\<;\n=/")){
//				invalidData=true;
//			}
//			String badList = ",+\"\\<;\n=";
			String badList = "+\"\\<;\n";
			for(char car :currentValue.toCharArray()){
			for(char c :badList.toCharArray()){
				if(car == c){
					invalidData=true;
				}
			}
			}

		}
		return invalidData;
	}
    /**
     * @throws NamingException
     * @see org.jboss.security.auth.spi.UsernamePasswordLoginModule#validatePassword(java.lang.String,java.lang.String)
     */
    protected Set<Map<String, String>> buildGroup(Properties systemConfig, String filter) {
        Set<Map<String, String>> groupDetailsMap = new HashSet<Map<String, String>>();
        // Load our LDAP specific properties
        // Load the BaseDN
        String baseDN = (String) systemConfig.get(SystemSetting.LDAP_BASE_DN.getInternalName());

        // Load the LoginProperty
        String loginProperty = (String) systemConfig.get(SystemSetting.LDAP_LOGIN_PROPERTY.getInternalName());
        if (loginProperty == null) {
            // Use the default
            loginProperty = "cn";
        }
        // Load any information we may need to bind
        String bindDN = (String) systemConfig.get(SystemSetting.LDAP_BIND_DN.getInternalName());
        String bindPW = (String) systemConfig.get(SystemSetting.LDAP_BIND_PW.getInternalName());
        if (bindDN != null) {
              systemConfig.setProperty(Context.SECURITY_PRINCIPAL, bindDN);
              systemConfig.setProperty(Context.SECURITY_CREDENTIALS, bindPW);
              systemConfig.setProperty(Context.SECURITY_AUTHENTICATION, "simple");
        }
        try {
            InitialLdapContext ctx = new InitialLdapContext(systemConfig, null);
            SearchControls searchControls = getSearchControls();
            /*String filter = "(&(objectclass=groupOfUniqueNames)(uniqueMember=uid=" + userName
                + ",ou=People, dc=rhndev, dc=redhat, dc=com))";*/

            //modify the search control to only include the attributes we will use
            String[] attributes = { "cn", "description" };
            searchControls.setReturningAttributes(attributes);

            //BZ:964250: add rfc 2696
            //default to 1000 results.  System setting page size from UI should be non-negative integer > 0.
            //additionally as system settings are modifiable via CLI which may not have param checking enabled do some
            //more checking.
            int defaultPageSize = 1000;
            // only if they're enabled in the UI.
			if (enable32xFeatures.isSelected()) {
				String groupPageSize = systemConfig.getProperty(
						SystemSetting.LDAP_GROUP_QUERY_PAGE_SIZE
								.getInternalName(), "" + defaultPageSize);
				if ((groupPageSize != null)
						&& (!groupPageSize.trim().isEmpty())) {
					int passedInPageSize = -1;
					try {
						passedInPageSize = Integer
								.valueOf(groupPageSize.trim());
						if (passedInPageSize > 0) {
							defaultPageSize = passedInPageSize;
							if(enableVerboseDebugging.isSelected()){
								log(advdb
										+ " LDAP Group Query Page Sizing of '"+defaultPageSize+"' is being requested from server.");
							}
						}
					} catch (NumberFormatException nfe) {
						// log issue and do nothing. Go with the default.
						String msg = "LDAP Group Page Size passed in '"
								+ groupPageSize
								+ "' in is invalid. Defaulting to 1000 results."
								+ nfe.getMessage();
						log(msg);
					}
				}
				ctx.setRequestControls(new Control[] { new PagedResultsControl(
						defaultPageSize, Control.CRITICAL) });
			}
            // Loop through each configured base DN.  It may be useful
            // in the future to allow for a filter to be configured for
            // each BaseDN, but for now the filter will apply to all.
            String[] baseDNs = baseDN.split(BASEDN_DELIMITER);

            for (int x = 0; x < baseDNs.length; x++) {
				if (enableVerboseDebugging.isSelected()) {
					log(advdb
							+ " this search was excuted against DN component '"
							+ baseDNs[x] + "'.");
				}
                executeGroupSearch(filter, groupDetailsMap, ctx, searchControls, baseDNs, x);

				// continually parsing pages of results until we're done.
                // only if they're enabled in the UI.
				if (enable32xFeatures.isSelected()) {
					// handle paged results if they're being used here
					byte[] cookie = null;
					Control[] controls = ctx.getResponseControls();
					if (controls != null) {
						for (Control control : controls) {
							if (control instanceof PagedResultsResponseControl) {
								PagedResultsResponseControl pagedResult = (PagedResultsResponseControl) control;
								cookie = pagedResult.getCookie();
							}
						}
					}

					while (cookie != null) {
						String msg = "RFC 2696 is supported by the server and we are paging through the results. "+
					      groupDetailsMap.size()+" results returned so far.";
						if(enableVerboseGroupParsing.isSelected()){
							log(advdb
									+ msg);
						}
						// ensure the next requests contains the session/cookie
						// details
						ctx.setRequestControls(new Control[] { new PagedResultsControl(
								defaultPageSize, cookie, Control.CRITICAL) });
						executeGroupSearch(filter, groupDetailsMap, ctx,
								searchControls, baseDNs, x);
						// empty out cookie
						cookie = null;
						// test for further iterations
						controls = ctx.getResponseControls();
						if (controls != null) {
							for (Control control : controls) {
								if (control instanceof PagedResultsResponseControl) {
									PagedResultsResponseControl pagedResult = (PagedResultsResponseControl) control;
									cookie = pagedResult.getCookie();
								}
							}
						}
					}
				}
            }//end of for loop
        } catch (NamingException e) {
            if (e instanceof InvalidSearchFilterException) {
                InvalidSearchFilterException fException = (InvalidSearchFilterException) e;
                String message = "The ldap group filter defined is invalid ";
                log(message);
            }
            //TODO: check for ldap connection/unavailable/etc. exceptions.
            else {
            	String mesg = "LDAP communication error: " + e.getMessage();
            	log(mesg);
            }
        } catch (IOException iex) {
        	String msg = "Unexpected LDAP communciation error:" + iex.getMessage();
        	log(msg);
        }

        return groupDetailsMap;
    }
    
    /** Executes the LDAP group query using the filters, context and search controls, etc. parameters passed in.
     *  The matching groups located during processing this pages of results are added as new entries to the
     *  groupDetailsMap passed in.
     * 
     * @param filter
     * @param groupDetailsMap
     * @param ctx
     * @param searchControls
     * @param baseDNs
     * @param x
     * @throws NamingException
     */
    private void executeGroupSearch(String filter, Set<Map<String, String>> groupDetailsMap, InitialLdapContext ctx,
        SearchControls searchControls, String[] baseDNs, int x) throws NamingException {
        //execute search based on controls and context passed in.
        NamingEnumeration<SearchResult> answer = ctx.search(baseDNs[x], filter, searchControls);
        boolean ldapApiEnumerationBugEncountered = false;
        while ((!ldapApiEnumerationBugEncountered) && answer.hasMoreElements()) {//BZ:582471- ldap api bug change
            // We use the first match
            SearchResult si = null;
			try {
				si = answer.next();
			} catch (NullPointerException npe) {
				if (enableVerboseDebugging.isSelected()) {
					log(advdb
							+ " NullPtr exception detected. If known LDAP api enum npe ignore: "
							+ npe.getMessage() + ".");
				}
				ldapApiEnumerationBugEncountered = true;
				break;
			}
			
			if (enableVerboseDebugging.isSelected()
					|| enableVerboseGroupParsing.isSelected()) {
				Attributes attributeContainer = si.getAttributes();
				NamingEnumeration<? extends Attribute> attributes = attributeContainer
						.getAll();
				String attributesReturned = " ";
				while (attributes.hasMore()) {
					attributesReturned += attributes.next().getID() + ",";
				}
				String dbugMesg = "\n"
						+ advdb
						+ " Group search LDAP ("
						+ attributeContainer.size()
						+ ") attributes located for group '"
						+ si.getName()
						+ "' are ["
						+ attributesReturned.substring(0,
								attributesReturned.length() - 1) + "].";
				// directly update here to shorten messages for lots of groups
				testResults.setText(testResults.getText() + dbugMesg);
				//This flag can be used in the unlikely case that the UI hangs during a test operation.:
				if(iterativeVerboseLogging.isSelected()){
				  System.out.println(dbugMesg);
				}

				// additionally parse attribute ids and values for illegal ldap
				// characters
				if (enableVerboseGroupParsing.isSelected()) {
					attributes = attributeContainer.getAll();
					String currentAttributeId = "";
					String currentValue = "";
					// spinder: 3/17/11: should we bail on first bad data or
					// display them all?
					while (attributes.hasMore()) {
						boolean badData = false;
						Attribute att = attributes.next();
						currentAttributeId = att.getID();
						if (containsIllegalLdap(currentAttributeId)) {
							log(advdb
									+ " LDAP Group: bad atrribute data detected for group '"
									+ si.getName() + "' for attribute '"
									+ currentAttributeId + "'.");
							badData = true;
						}
						if (att.getAll() != null) {
							NamingEnumeration<?> enumer = att.getAll();
							while (enumer.hasMore()) {
								currentValue = enumer.next() + "";
								if (containsIllegalLdap(currentValue)) {
									log(advdb
											+ " LDAP Group: bad data detected for group '"
											+ si.getName()
											+ "' with attribute '"
											+ currentAttributeId
											+ "' and value:" + currentValue);
									badData = true;
								}
							}
						}
						if (badData) {
							log(advdb
									+ "** LDAP Group: Some bad LDAP data detected for group '"
									+ si.getName() + "'.");
						}
					}
				}
			}
			
            Map<String, String> entry = new HashMap<String, String>();
			// String name = (String) si.getAttributes().get("cn").get();
			Attribute commonNameAttr = si.getAttributes().get("cn");
			if (commonNameAttr != null) {
				String name = (String) commonNameAttr.get();
				name = name.trim();
				Attribute desc = si.getAttributes().get("description");
				String description = desc != null ? (String) desc.get() : "";
				description = description.trim();
				entry.put("id", name);
				entry.put("name", name);
				entry.put("description", description);
				groupDetailsMap.add(entry);
			} else {// unable to retrieve details for specific group.
				log(advdb
						+ " There was an error retrieving 'cn' attribute for group '"
						+ si.getName()
						+ "'. Not adding to returned list of groups. ");
			}
        }
    }
    
    public Map<String, String> findLdapUserDetails(String userName) {
        // Load our LDAP specific properties
    	Properties systemConfig = env;
        HashMap<String, String> userDetails = new HashMap<String, String>();

        // Load the BaseDN
        String baseDN = (String) systemConfig.get(SystemSetting.LDAP_BASE_DN.getInternalName());

        // Load the LoginProperty
        String loginProperty = (String) systemConfig.get(SystemSetting.LDAP_LOGIN_PROPERTY.getInternalName());
        if (loginProperty == null) {
            // Use the default
            loginProperty = "cn";
        }
        // Load any information we may need to bind
        String bindDN = (String) systemConfig.get(SystemSetting.LDAP_BIND_DN.getInternalName());
        String bindPW = (String) systemConfig.get(SystemSetting.LDAP_BIND_PW.getInternalName());

        // Load any search filter
        String groupSearchFilter = (String) systemConfig.get(SystemSetting.LDAP_GROUP_FILTER.getInternalName());
        String groupMemberFilter = (String) systemConfig.get(SystemSetting.LDAP_GROUP_MEMBER.getInternalName());
        String userDn = (String) systemConfig.get(Context.SECURITY_PRINCIPAL);

        String testUserDN = userDn;
        String ldapServer = (String) systemConfig.get(Context.PROVIDER_URL);
        
        Properties env = getProperties(ldapServer);
        
        if (bindDN != null) {
        	env.setProperty(Context.SECURITY_PRINCIPAL, bindDN);
        	env.setProperty(Context.SECURITY_CREDENTIALS, bindPW);
        	env.setProperty(Context.SECURITY_AUTHENTICATION, "simple");
        }

        try {
            InitialLdapContext ctx = new InitialLdapContext(env, null);
            SearchControls searchControls = getSearchControls();

			String filter = String.format("(&(%s)(%s=%s))",
			groupSearchFilter, groupMemberFilter,
			testUserDN);

        	generateUiLoggingForStep4LdapFilter(userName, filter);
            
            // Loop through each configured base DN.  It may be useful
            // in the future to allow for a filter to be configured for
            // each BaseDN, but for now the filter will apply to all.
            String[] baseDNs = baseDN.split(BASEDN_DELIMITER);
            for (int x = 0; x < baseDNs.length; x++) {
                NamingEnumeration<SearchResult> answer = ctx.search(baseDNs[x], filter, searchControls);
                if (!answer.hasMoreElements()) { //BZ:582471- ldap api bug change
                    // Nothing found for this DN, move to the next one if we have one.
                    continue;
                }

                // We use the first match
                SearchResult si = answer.next();
                //generate the DN
                String userDN = null;
                try {
                    userDN = si.getNameInNamespace();
                } catch (UnsupportedOperationException use) {
                    userDN = si.getName();
                    if (userDN.startsWith("\"")) {
                        userDN = userDN.substring(1, userDN.length());
                    }
                    if (userDN.endsWith("\"")) {
                        userDN = userDN.substring(0, userDN.length() - 1);
                    }
                    userDN = userDN + "," + baseDNs[x];
                }
                userDetails.put("dn", userDN);

                // Construct the UserDN
                NamingEnumeration<String> keys = si.getAttributes().getIDs();
                while (keys.hasMore()) {
                    String key = keys.next();
                    Attribute value = si.getAttributes().get(key);
                    if ((value != null) && (value.get() != null)) {
                        userDetails.put(key, value.get().toString());
                    }
                }
//                return userDetails;
            }//end of for loop
			generateUiLoggingStep4Authz(filter);
            return userDetails;
		} catch (Exception ex) {
			generateUiLoggingStep4Exception(ex);
		}
        return userDetails;
    }

    public Set<String> findAvailableGroupsFor(String userName) {
        // Load our LDAP specific properties
     	Properties options = env;
        String groupFilter = options.getProperty(SystemSetting.LDAP_GROUP_FILTER.getInternalName(), "");
        String groupMember = options.getProperty(SystemSetting.LDAP_GROUP_MEMBER.getInternalName(), "");
        String groupUsePosix = options.getProperty(SystemSetting.LDAP_GROUP_USE_POSIX.getInternalName(), "false");
        if (groupUsePosix == null) {
            groupUsePosix = Boolean.toString(false);//default to false
        }
        boolean usePosixGroups = Boolean.valueOf(groupUsePosix);
        String userAttribute = getUserAttribute(options, userName, usePosixGroups);
        Set<String> ldapSet = new HashSet<String>();

        if (userAttribute != null && userAttribute.trim().length() > 0) {
            //TODO: spinder 4/21/10 put in error/debug logging messages for badly formatted filter combinations
            String filter = "";
            //form assumes examples where groupFilter is like 'objectclass=groupOfNames' and groupMember is 'member'
            // to produce ldap filter like (&(objectclass=groupOfNames)(member=cn=Administrator,ou=People,dc=test,dc=com))
            // or like (&(objectclass=groupOfNames)(memberUid=Administrator)) for posixGroups.
            filter = String.format("(&(%s)(%s=%s))", groupFilter, groupMember, encodeForFilter(userAttribute));

            Set<Map<String, String>> matched = buildGroup(options, filter);
//            log.trace("Located '" + matched.size() + "' LDAP groups for user '" + userName
//                + "' using following ldap filter '" + filter + "'.");

            //iterate to extract just the group names.
            for (Map<String, String> match : matched) {
                ldapSet.add(match.get("id"));
            }
        } else {
//            log.debug("Group lookup will not be performed due to no UserDN found for user " + userName);
        }

        return ldapSet;
    }
    
	private void generateUiLoggingStep4Exception(Exception ex) {
		String groupSearchFilter = env
				.getProperty(SystemSetting.LDAP_GROUP_FILTER
						.getInternalName());
		String msg = "STEP-4:FAIL: There was an error searching with the groupFilter supplied: "
				+ groupSearchFilter + "'\n";
		msg += ex.getMessage();
		if (enableVerboseDebugging.isSelected()) {
			msg = appendStacktraceToMsg(msg, ex);
		}
		log(msg);
	}

	private void generateUiLoggingStep4Authz(String filter) {
		Set<Map<String, String>> groups = buildGroup(env, filter);
		String msg = "STEP-4:TESTING: Using Group Search Filter '"
				+ filter + "', " + groups.size()
				+ " ldap group(s) were located.\n";
		if (groups.size() > 0) {
			HashMap<String, String>[] ldapLists = new HashMap[groups
					.size()];
			groups.toArray(ldapLists);
			msg += "STEP-4:PASS: Listing a few of the ldap groups located: \n";
			// iterate over first ten or less to demonstrate retrieve
			for (int i = 0; (i < groups.size() && i < 10); i++) {
				msg += ldapLists[i] + "\n";
			}
		}else{
			msg+="STEP-4:WARN: With current settings, test user is not authorized for any groups. Is this correct?";
		}
		log(msg);
	}

	private void generateUiLoggingForStep4LdapFilter(String userName,
			String filter) {
		String msg = "STEP-4:TESTING: about to do ldap search with filter \n'"
				+ filter
				+ "'\n to locate groups that test user '"+userName+"' IS authorized to access.";
		log(msg);
	}


	// throw the label and fields together, two to a row.
	private HashMap<String, JTextField> loadUiFields(JPanel top,
			String[] componentKeys) {
		HashMap<String, JTextField> mappings = new HashMap<String, JTextField>();
		for (int i = 0; i < componentKeys.length; i++) {
			String firstLabelKey = componentKeys[i];
			String secondLabelKey = componentKeys[++i];
			// locate second key
			JPanel row = new JPanel();
			row.setLayout(new FlowLayout(FlowLayout.LEFT));
			JLabel label1 = new JLabel(firstLabelKey);
			label1.setSize(textBoxWidth, 5);
//			JTextField value1 = new JTextField(textBoxWidth);
			JTextField value1 = null;
			if (firstLabelKey.equalsIgnoreCase("Password:")&&(!showPasswords.isSelected())) {
				value1 = new JPasswordField(textBoxWidth);
			} else {
				value1 = new JTextField(textBoxWidth);
			}
			JLabel label2 = new JLabel(secondLabelKey);
			JTextField value2 = new JTextField(textBoxWidth);
			row.add(label1);
			row.add(value1);
			row.add(Box.createRigidArea(new Dimension(0, 5)));
			row.add(label2);
			row.add(value2);
			mappings.put(firstLabelKey, value1);
			mappings.put(secondLabelKey, value2);
			top.add(row);
		}

		return mappings;
	}

	private Properties getProperties(String contentProvider) {
		Properties env = new Properties();
		env.setProperty(Context.INITIAL_CONTEXT_FACTORY,
				"com.sun.jndi.ldap.LdapCtxFactory");
		env.setProperty(Context.PROVIDER_URL, contentProvider);
		if(!enableLdapReferral.isSelected()){
		  env.setProperty(Context.REFERRAL, "ignore");
		}else{
		  String msg="**---- You have chosen to tell LDAP servers to [FOLLOW] context referrals. Default is [IGNORE] referrals. --**";
		  log(msg);
		  env.setProperty(Context.REFERRAL, "follow");
		}

//		// Setup SSL if requested
//		String protocol = ssl.isSelected()? "ssl":"";
//		if ((protocol != null) && protocol.equals("ssl")) {
//			String ldapSocketFactory = env
//					.getProperty("java.naming.ldap.factory.socket");
//			if (ldapSocketFactory == null) {
//				env.put("java.naming.ldap.factory.socket",
//						UntrustedSSLSocketFactory.class.getName());
//			}
//			env.put(Context.SECURITY_PROTOCOL, "ssl");
//		}

		return env;
	}

	private String delineate() {
		String line = "-";
		for (int i = 0; i < 30; i++) {
			line += "-";
		}
		return line;
	}

	/** Takes care of delineating messages and conditional logging contents passed in.
	 * @param msg
	 */
	private void log(String msg) {
		String message = "\n" + delineate() + "\n";
		message += msg;
		message += "\n" + delineate() + "\n\n";
		//This flag can be used in the unlikely case that the UI hangs during a test operation.:
		if(iterativeVerboseLogging.isSelected()){
		  System.out.println(message);
		}
		testResults.setText(testResults.getText() + message);
	}

	private SearchControls getSearchControls() {
		int scope = SearchControls.SUBTREE_SCOPE;
		int timeLimit = 0;
		long countLimit = 0;
		String[] returnedAttributes = null;
		boolean returnObject = false;
		boolean deference = false;
		SearchControls constraints = new SearchControls(scope, countLimit,
				timeLimit, returnedAttributes, returnObject, deference);
		return constraints;
	}
	
    /** Translate SystemSettings to familiar Properties instance since we're
     *  passing not one but multiple values.
     * 
     * @param systemSettings
     * @return
     */
    private Properties populateProperties(Properties existing) {
        Properties properties = new Properties();
        if(existing!=null){
        	properties = existing;
        }
            for (SystemSetting entry : SystemSetting.values()) {
            	if(entry!=null){
            		switch(entry){
            		case LDAP_BASED_JAAS_PROVIDER: 
            			properties.put(entry.getInternalName(), "");
            			break;
            		}
            	}
            }
        return properties;
    }
    
    /**Build/retrieve the user DN. Not usually a property.
    *
    * @param options
    * @param userName
    * @param usePosixGroups boolean indicating whether we search for groups with posixGroup format
    * @return
    */
   private String getUserAttribute(Properties options, String userName, boolean usePosixGroups) {
       Map<String, String> details = findLdapUserDetails(userName);
       String userAttribute = null;
       if (usePosixGroups) {//return just the username as posixGroup member search uses (&(%s)(memberUid=username))
           userAttribute = userName;
       } else {//this is the default where group search uses (&(%s)(uniqueMember={userDn}))
           userAttribute = details.get("dn");
       }

       return userAttribute;
   }
    
    /** See LDAPStringUtil.encodeForFilter() for original code/source/author/etc.
     * <p>Encode a string so that it can be used in an LDAP search filter.</p> 
     *
     * <p>The following table shows the characters that are encoded and their 
     * encoded version.</p>
     * 
     * <table>
     * <tr><th align="center">Character</th><th>Encoded As</th></tr>
     * <tr><td align="center">*</td><td>\2a</td></tr>
     * <tr><td align="center">(</td><td>\28</td></tr>
     * <tr><td align="center">)</td><td>\29</td></tr>
     * <tr><td align="center">\</td><td>\5c</td></tr>
     * <tr><td align="center"><code>null</code></td><td>\00</td></tr>
     * </table>
     * 
     * <p>In addition to encoding the above characters, any non-ASCII character 
     * (any character with a hex value greater then <code>0x7f</code>) is also 
     * encoded and rewritten as a UTF-8 character or sequence of characters in 
     * hex notation.</p>
     *  
     * @param  filterString a string that is to be encoded
     * @return the encoded version of <code>filterString</code> suitable for use
     *         in a LDAP search filter
     * @see <a href="http://tools.ietf.org/html/rfc4515">RFC 4515</a>
     */
    public static String encodeForFilter(final String filterString) {
        if (filterString != null && filterString.length() > 0) {
            StringBuilder encString = new StringBuilder(filterString.length());
            for (int i = 0; i < filterString.length(); i++) {
                char ch = filterString.charAt(i);
                switch (ch) {
                case '*': // encode a wildcard * character
                    encString.append("\\2a");
                    break;
                case '(': // encode a open parenthesis ( character
                    encString.append("\\28");
                    break;
                case ')': // encode a close parenthesis ) character
                    encString.append("\\29");
                    break;
                case '\\': // encode a backslash \ character
                    encString.append("\\5c");
                    break;
                case '\u0000': // encode a null character
                    encString.append("\\00");
                    break;
                default:
                    if (ch <= 0x7f) { // an ASCII character
                        encString.append(ch);
                    } else if (ch >= 0x80) { // encode to UTF-8
                        try {
                            byte[] utf8bytes = String.valueOf(ch).getBytes("UTF8");
                            for (byte b : utf8bytes) {
                                encString.append(String.format("\\%02x", b));
                            }
                        } catch (UnsupportedEncodingException e) {
                            // ignore
                        }
                    }
                }
            }
            return encString.toString();
        }
        return filterString;
    }
}

//Mock up the upgraded system properties approach to use SystemSetting
enum SystemSetting {
    LDAP_BASED_JAAS_PROVIDER("CAM_JAAS_PROVIDER"),
    LDAP_NAMING_PROVIDER_URL("CAM_LDAP_NAMING_PROVIDER_URL"),
    USE_SSL_FOR_LDAP("CAM_LDAP_PROTOCOL"),
    LDAP_LOGIN_PROPERTY("CAM_LDAP_LOGIN_PROPERTY"),
    LDAP_FILTER("CAM_LDAP_FILTER"),
    LDAP_GROUP_FILTER("CAM_LDAP_GROUP_FILTER"),
    LDAP_GROUP_MEMBER("CAM_LDAP_GROUP_MEMBER"),
    LDAP_GROUP_QUERY_PAGE_SIZE("CAM_LDAP_GROUP_QUERY_PAGE_SIZE"),
    LDAP_BASE_DN("CAM_LDAP_BASE_DN"),
    LDAP_BIND_DN("CAM_LDAP_BIND_DN"),
    LDAP_BIND_PW("CAM_LDAP_BIND_PW"),
    LDAP_NAMING_FACTORY("CAM_LDAP_NAMING_FACTORY_INITIAL"),
    LDAP_GROUP_USE_POSIX("CAM_LDAP_GROUP_USE_POSIX"),
    ;

    private String internalName;

    private SystemSetting(String name) {
        this.internalName = name;
    }

    public String getInternalName() {
        return internalName;
    }

    public static SystemSetting getByInternalName(String internalName) {
        for (SystemSetting p : SystemSetting.values()) {
            if (p.internalName.equals(internalName)) {
                return p;
            }
        }
        return null;
    }
}


