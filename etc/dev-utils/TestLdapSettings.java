package com.test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

//import org.rhq.enterprise.server.util.security.UntrustedSSLSocketFactory;

/* Is a development test tool that allows the user to simulate the RHQ server side
 * LDAP calls during auth/authz operations.
 *
 * @author Simeon Pinder
 */
public class TestLdapSettings extends JFrame {
	//shared fields
	private JTextArea testResults;
	private JCheckBox ssl;
	private JTextField testUserNameValue;
	private JTextField testUserPasswordValue;
	private HashMap<String, JTextField> fieldMappings;
	private String[] keys;
	private JCheckBox enableLdapReferral;
	private JCheckBox enableVerboseDebugging;
	private JCheckBox enableVerboseGroupParsing;
    private String advdb = "**Verbose:debug ----";

	private static final long serialVersionUID = 1L;
	int textBoxWidth = 20;

	public static void main(String args[]) {
		new TestLdapSettings();
	}

	// Configure window properties
	private TestLdapSettings() {

		setTitle("Check LDAP Settings: Simulates LDAP checks/queries of RHQ LDAP integration");
		getContentPane().setLayout(new BorderLayout());
		// top panel definition
		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.setBorder(LineBorder.createGrayLineBorder());
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
		enableVerboseGroupParsing= new JCheckBox("more detailed group parsing");
		enableVerboseGroupParsing.setSelected(false);
		//put into row display
		JPanel advancedDebugRegion = new JPanel();
		advancedDebugRegion.setLayout(new FlowLayout(FlowLayout.LEFT));
		LineBorder advancedBorder = new LineBorder(Color.BLACK, 2);
		TitledBorder debugBorder = new TitledBorder(advancedBorder, "Debug:");
		advancedDebugRegion.setBorder(debugBorder);
		advancedDebugRegion.add(enableLdapReferral);
		advancedDebugRegion.add(enableVerboseDebugging);
		advancedDebugRegion.add(enableVerboseGroupParsing);
		top.add(advancedDebugRegion);

		ssl = new JCheckBox("SSL:");
		ssl.setEnabled(false);
		top.add(ssl);
		// test user auth region
		JPanel testUserRegion = new JPanel();
		testUserRegion.setLayout(new FlowLayout(FlowLayout.LEFT));
		LineBorder border = new LineBorder(Color.BLUE, 2);
		TitledBorder tBorder = new TitledBorder(border, "Authentication/Authorization Check Credentials: (insert valid ldap user assigned to group)");
		testUserRegion.setBorder(tBorder);
		JLabel testUserName = new JLabel("Test UserName:");
		testUserNameValue = new JTextField(textBoxWidth);
		JLabel testUserPassword = new JLabel("Test Password:");
		testUserPasswordValue = new JTextField(textBoxWidth);
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
				Properties env;
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
						String filter = String
								.format("(%s)", groupSearchFilter);
						msg = "STEP-3:TESTING: This ldap filter "
								+ filter
								+ " will be used to locate ALL available LDAP groups";
						log(msg);
						try {
							String[] baseDNs = searchBase.split(";");
							for (int x = 0; x < baseDNs.length; x++) {
								if(enableVerboseDebugging.isSelected()){
									log(advdb+" this search was excuted against DN component '"+baseDNs[x]+"'.");
								}
								NamingEnumeration answer = ctx.search(
										baseDNs[x], filter, searchControls);

								if(enableVerboseGroupParsing.isSelected()){//in this mode report initial state of Enumeration results
									log(advdb+" LDAP Group Search/Enumeration found "+((answer.hasMore())? " SOME ":" NO ")+" matching group(s).");
								}

								boolean ldapApiEnumerationBugEncountered = false;
								while ((!ldapApiEnumerationBugEncountered)
										&& answer.hasMoreElements()) {
									// We use the first match
									SearchResult si = null;
									try {
										si = (SearchResult) answer.next();
									} catch (NullPointerException npe) {
										if(enableVerboseDebugging.isSelected()){
											log(advdb+" NullPtr exception detected. If known LDAP api enum npe ignore: "+npe.getMessage()+".");
										}
										ldapApiEnumerationBugEncountered = true;
										break;
									}
									Map<String, String> entry = new HashMap<String, String>();
									if(enableVerboseDebugging.isSelected()||enableVerboseGroupParsing.isSelected()){
										Attributes attributeContainer = si.getAttributes();
										NamingEnumeration<? extends Attribute> attributes = attributeContainer.getAll();
										String attributesReturned = " ";
										while(attributes.hasMore()){
											attributesReturned+=attributes.next().getID()+",";
										}
										String dbugMesg="\n"+advdb+" Group search LDAP ("+attributeContainer.size()+") attributes located for group '"+si.getName()+"' are ["+
										attributesReturned.substring(0, attributesReturned.length()-1)+"].";
										//directly update here to shorten messages for lots of groups
										testResults.setText(testResults.getText() + dbugMesg);

										//additionally parse attribute ids and values for illegal ldap characters
										if(enableVerboseGroupParsing.isSelected()){
											attributes = attributeContainer.getAll();
											String currentAttributeId ="";
											String currentValue ="";
											//spinder: 3/17/11: should we bail on first bad data or display them all?
											while(attributes.hasMore()){
												boolean badData = false;
												Attribute att = attributes.next();
												currentAttributeId =att.getID();
												if(containsIllegalLdap(currentAttributeId)){
													log(advdb+" LDAP Group: bad atrribute data detected for group '"+si.getName()+"' for attribute '"+currentAttributeId+"'.");
													badData=true;
												}
												if(att.getAll()!=null){
													NamingEnumeration<?> enumer = att.getAll();
													while(enumer.hasMore()){
												          currentValue = enumer.next()+"";
														if(containsIllegalLdap(currentValue)){
															log(advdb+" LDAP Group: bad data detected for group '"+si.getName()+"' with attribute '"+currentAttributeId+"' and value:"+currentValue);
															badData=true;
														}
													}
												}
												if(badData){
												 log(advdb+"** LDAP Group: Some bad LDAP data detected for group '"+si.getName()+"'.");
												}
											}
										}
									}

									Attribute commonNameAttr = si.getAttributes()
									.get("cn");
									if(commonNameAttr!=null){
										String name = (String) commonNameAttr.get();
										name = name.trim();
										Attribute desc = si.getAttributes().get(
												"description");
										String description = desc != null ? (String) desc
												.get()
												: "";
										description = description.trim();
										entry.put("id", name);
										entry.put("name", name);
										entry.put("description", description);
										ret.add(entry);
									}else{//unable to retrieve details for specific group.
										log(advdb+" There was an error retrieving 'cn' attribute for group '"+si.getName()+"'. Not adding to returned list of groups. ");
									}
								}
							}
							msg = "STEP-3:TESTING: Using Group Search Filter '" + filter
									+ "', " + ret.size()
									+ " ldap group(s) were located.\n";
							if (ret.size() > 0) {
								HashMap<String, String>[] ldapLists = new HashMap[ret.size()];
								ret.toArray(ldapLists);
								if(enableVerboseGroupParsing.isSelected()){//in this mode go beyond the first ten results.
									msg += "STEP-3:PASS: Listing 'all' of the ldap groups located: \n";
									for (int i = 0; i < ret.size(); i++) {
										msg += ldapLists[i] + "\n";
									}
								}else{//otherwise only show first 10[subset of available groups]
									msg += "STEP-3:PASS: Listing a few(<=10) of the ldap groups located: \n";
									for (int i = 0; (i < ret.size() && i < 10); i++) {
										msg += ldapLists[i] + "\n";
									}
								}
								proceed = true;// then can proceed to next step.
							}
							log(msg);
						} catch (Exception ex) {
							msg = "STEP-3:FAIL: There was an error searching with the groupFilter supplied: "
									+ groupSearchFilter + "'\n";
							msg += ex.getMessage();
							if(enableVerboseDebugging.isSelected()){
								msg = appendStacktraceToMsg(msg, ex);
							}
							log(msg);
							proceed=false;
						}
					} else {
						msg = "STEP-3:FAIL: Group Search Filter: cannot be empty to proceed.";
						log(msg);
						proceed=false;
					}
					// retrieve lists of authorized groups available for the
					if (proceed) {
						// check groupMember
						if (!groupMemberFilter.isEmpty()) {
							Set<Map<String, String>> ret = new HashSet<Map<String, String>>();
							String filter = String.format("(&(%s)(%s=%s))",
									groupSearchFilter, groupMemberFilter,
									userDN);
							msg = "STEP-4:TESTING: about to do ldap search with filter \n'"
									+ filter
									+ "'\n to locate groups that test user IS authorized to access.";
							log(msg);
							try {
								String[] baseDNs = searchBase.split(";");
								for (int x = 0; x < baseDNs.length; x++) {
									NamingEnumeration answer = ctx.search(
											baseDNs[x], filter, searchControls);
									boolean ldapApiEnumerationBugEncountered = false;
									//BZ:582471- ldap api bug change
									while ((!ldapApiEnumerationBugEncountered)
											&& answer.hasMoreElements()) {
										// We use the first match
										SearchResult si = null;
										try {
											si = (SearchResult) answer.next();
										} catch (NullPointerException npe) {
											ldapApiEnumerationBugEncountered = true;
											break;
										}
										Map<String, String> entry = new HashMap<String, String>();
										String name = (String) si
												.getAttributes().get("cn")
												.get();
										name = name.trim();
										Attribute desc = si.getAttributes()
												.get("description");
										String description = desc != null ? (String) desc
												.get()
												: "";
										description = description.trim();
										entry.put("id", name);
										entry.put("name", name);
										entry.put("description", description);
										ret.add(entry);
									}
								}
								msg = "STEP-4:TESTING: Using Group Search Filter '"
										+ filter + "', " + ret.size()
										+ " ldap group(s) were located.\n";
								if (ret.size() > 0) {
									HashMap<String, String>[] ldapLists = new HashMap[ret
											.size()];
									ret.toArray(ldapLists);
									msg += "STEP-4:PASS: Listing a few of the ldap groups located: \n";
									// iterate over first ten or less to demonstrate retrieve
									for (int i = 0; (i < ret.size() && i < 10); i++) {
										msg += ldapLists[i] + "\n";
									}
									proceed = true;// then can proceed to next
													// step.
								}else{
									msg+="STEP-4:WARN: With current settings, test user is not authorized for any groups. Is this correct?";
								}
								log(msg);
							} catch (Exception ex) {
								msg = "STEP-4:FAIL: There was an error searching with the groupFilter supplied: "
										+ groupSearchFilter + "'\n";
								msg += ex.getMessage();
								if(enableVerboseDebugging.isSelected()){
									msg = appendStacktraceToMsg(msg, ex);
								}
								log(msg);
								proceed=false;
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

			private boolean containsIllegalLdap(String currentValue) {
				boolean invalidData = false;
				if((currentValue!=null)&&(!currentValue.trim().isEmpty())){
					//TODO: spinder 3/17/11: need to figure out regex to filter/detect bad data in returned ldap. Giving up for now.
//					String regex = "(?<=(?:[^\\]|^)(\\\\)+|[^\\]|^)[/,+\"><;=#]|(?<=(?:[^\\]|^)(\\\\)+|[^\\]|^)\\(?!\\|[/,+\"><;=#]| $|(?<=^\\) )|^";
//					regex = "(?<=(?:[^\\\\]|^)(\\\\\\\\)+|[^\\\\]|^)[/,+\\\"><;=#]|(?<=(?:[^\\\\]|^)(\\\\\\\\)+|[^\\\\]|^)\\\\(?!\\\\|[/,+\\\"><;=#]| $|(?<=^\\\\) )|^";
//					System.out.println("++++++++ CURR VAL:"+currentValue+":INV-CHeck:"+currentValue.matches(",+\"\\<;\n=/")+":NEWCHECK:"+(currentValue.matches(regex)));
//					if(currentValue.matches(",+\"\\<;\n=/")){
//						invalidData=true;
//					}
//					String badList = ",+\"\\<;\n=";
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
		});
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
			JTextField value1 = new JTextField(textBoxWidth);
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

	private void log(String msg) {
		String message = "\n" + delineate() + "\n";
		message += msg;
		message += "\n" + delineate() + "\n\n";
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
}
