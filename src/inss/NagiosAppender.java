/*
 * Copyright 1999,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * NagiosAppender: utilizes nsca to push an Alert to Nagios
 */
package inss;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;


/**
 * <p>
 * The NagiosAppender pushes messages into an instance of Nagios via nsca.
 * It was successfully tested against the following:
 * <p>
 * log4j 1.2.9 and nsca version 3
 *
 * <p>
 * This version of the appender includes the following limitations:
 * <br>
 * <li>Encryption is limited to XOR only, so you must have an nsca server
 *     available that is configured with encyption either off or set to XOR.
 *     The remaining dozen or so encryption choices are not available with this
 *     appender (..... yet!!)
 * </li>
 * <li>It does not support bulk transfer, so each message will result
 *     in a socket create/teardown.
 * </li>
 * <li>Layout control is now available, and is optional
 * </li>
 * <br>
 * <pre>
 *      Field length limit (bytes) for host name: 64
 *      Field length limit (bytes) for service name: 128
 *      Field length limit (bytes) for message: 512
 * </pre>
 * <br>
 * <b>Control Between Log4j and Nagios Levels</b>
 * <p>
 * Log4j and Nagios both support the notion of different levels of importance
 * in their messages, but because of the nature of each tool, it is necessary
 * for the user to stipulate which log4j message types should correpond with which
 * nagios message types.  Without overtly specifying the mappings, the appender
 * will not forward the messages.   Here are some sample mappings.  Note
 * that we have commented out the types of log4j messages that we don't want
 * to be forwarded to Nagios, regardless of the default log4j level.
 * <p>
 * <pre>
 *           #log4j.appender.NAGIOS.Log4j_Level_DEBUG=NAGIOS_OK
 *           #log4j.appender.NAGIOS.Log4j_Level_INFO=NAGIOS_UNKNOWN
 *           log4j.appender.NAGIOS.Log4j_Level_WARN=NAGIOS_WARN
 *           log4j.appender.NAGIOS.Log4j_Level_ERROR=NAGIOS_CRITICAL
 *           log4j.appender.NAGIOS.Log4j_Level_FATAL=NAGIOS_CRITICAL
 *
 * </pre>
 *
 * <p>
 * <b>Nagios Service Name:</b>
 * <p>
 * The service name that will be associated with your alert is a string that
 * you control.  You can either set it overtly in your log4j file, or you can
 * set it programmatically via MDC.  There are boolean settings in the log4j
 * file that control which method you prefer.  Here are some sample settings ...
 * <p>
 * <pre>
 *
 *      # You can either set the nagios service name in this file ....
 *      log4j.appender.NAGIOS.ServiceNameDefault=APPLICATION_FOOBAR
 *
 *      # ... or you can override the above and set it programatically, using something like the following ...
 *      #  org.apache.log4j.MDC.put("nagios_service_name", System.getProperty("APPLICATION_FOOBAR","UNKNOWN"));
 *      # ... in which case, all you need to do in this file is specify the key you used
 *      log4j.appender.NAGIOS.useMDCServiceName=true
 *      log4j.appender.NAGIOS.MDCServiceNameKey=nagios_service_name
 *
 * </pre>
 *
 * The MDC setting would be the preferred way to handle the situation where you have
 * multiple servlet applications running in an application container, such as web logic, or jboss.
 * The MDC.put(....) call would need to be made at the beginning of each request, and each
 * subsequent logging record would have the appropriate application name associated with it.  That way
 * you could distinguish records between different applications ... assuming that this means something
 * in your environment.
 *
 * <p>
 * <b>Nagios Host Name:</b>
 * <p>
 * The host name that will be associated with your alert is a string that
 * you control.  You can either set it overtly in your log4j file, or you can
 * have the appender determine it's name programmatically.  Manual control
 * is made available to simplify the case of a multi-homed system.
 * (There are some settings that allow you to control whether you prefer
 * long or short names).
 * <p>
 * <pre>
 *
 *       # with the following parameter, you can control whether your hostname shows up as
 *       # a fully qualified domain name, or just the first portion of the fqdn:
 *       # e.g. www1.amazon.com  vs www1
 *       log4j.appender.NAGIOS.useShortHostName=false
 *
 *       # with the following parameter, you can take all the guess work out of the interface name
 *       # that will be sent upstream via nsca, and just tell us what key in MDC has the appropriate
 *       # name
 *       log4j.appender.NAGIOS.useMDCHostName=true
 *       log4j.appender.NAGIOS.MDCHostNameKey=nagios_host_name
 * </pre>
 *
 * <b>Author : </b><A HREF="mailto:jarlyons@gmail.com">Jar Lyons </A>
 * <p>
 * @author jar
 * @since 1.0
 */
public class NagiosAppender extends AppenderSkeleton {

	private static final int TRANSMITTED_IV_SIZE = 128;	/* Initialization Vector size */

	// The supported encryption methods:
	protected static final int ENCRYPT_NONE 	= Nsca.ENCRYPT_NONE;
	protected static final int ENCRYPT_XOR 	= Nsca.ENCRYPT_XOR;


	/**
	 * Nagios configuration file ... it tells us what encryption model to use
	 */
	private String config_file = null;	/* NSCA config file name: usually something like /usr/local/nagios/nsca-2.4/send_nsca.cfg */
	private int encryption_method = ENCRYPT_NONE;	/* default Encryption Method */
	private String nsca_password = null;	/* NSCA password (optional and only used with XOR) */
	
	static org.apache.log4j.Logger log = Logger.getLogger(NagiosAppender.class);  //TODO: work without, too

	/**
	 * Nagios host where the nsca server is running
	 */
	private String host        = null;

	/**
	 * Nagios port where the nsca server is running
	 */
	private String port        = null;

	/**
	 * Nagios service name to associate with the messages we forward to nsca server
	 */
	private String service_name= "UNKNOWN_SERVICE";

	/**
	 * Nagios return codes
	 * NOTE: the DO_NOT_SEND message is for appender use only, it is not a nagios type
	 */
	protected static final int DO_NOT_SEND     = Nsca.DO_NOT_SEND;
	protected static final int NAGIOS_OK       = Nsca.NAGIOS_OK;
	protected static final int NAGIOS_WARN     = Nsca.NAGIOS_WARN;
	protected static final int NAGIOS_CRITICAL = Nsca.NAGIOS_CRITICAL;
	protected static final int NAGIOS_UNKNOWN  = Nsca.NAGIOS_UNKNOWN;

	/**
	 * Log4j default type mapping ->  Note that if not overridden, the message type
	 * will not be delivered to nagios.
	 */
	private int Log4j_Level_DEBUG     = DO_NOT_SEND;
	private int Log4j_Level_INFO      = DO_NOT_SEND;
	private int Log4j_Level_WARN      = DO_NOT_SEND;
	private int Log4j_Level_ERROR     = DO_NOT_SEND;
	private int Log4j_Level_FATAL     = DO_NOT_SEND;

	private static final int LEVEL_DEBUG = Level.DEBUG_INT;
	/**
	 * Variable which tells us whether to look for an MDC key to determine the
	 * hostname to associate with the messages we forward to nsca server.
	 */
	private boolean use_MDC_for_hostname = false;

	/**
	 * Variable which tells us whether to look for an MDC key to determine the
	 * service name to associate with the messages we forward to nsca server.
	 */
	private boolean use_MDC_for_servicename = false;

	/**
	 * Variable which tells us the MDC key to use to determine the
	 * service name to associate with the messages we forward to nsca server.
	 */
	private String  MDC_service_name_key = null;
	/**
	 * Variable which tells the MDC key to use to determine the
	 * service name to associate with the messages we forward to nsca server.
	 */
	private boolean  short_hostname = true;
	private String  MDC_hostname_key = null;
	private String  MDC_hostname_value = null;

	private int nsca_version = 3;

	private static Properties Nagios_levels = new Properties();
	static {
		Nagios_levels.setProperty("NAGIOS_OK",     "" + NAGIOS_OK);
		Nagios_levels.setProperty("NAGIOS_WARN",   "" + NAGIOS_WARN);
		Nagios_levels.setProperty("NAGIOS_CRITICAL",  "" + NAGIOS_CRITICAL);
		Nagios_levels.setProperty("NAGIOS_UNKNOWN","" + NAGIOS_UNKNOWN);
	}
	/**
	 * character to separate parts of layout if more than one part is to be used
	 */
	private String layoutPartsDelimiter = "#";

	/**
	 * A flag to indicate configuration status
	 */
/*
	private boolean configured = false;
*/

	/**
	 * Defines how many messages will be buffered until they will be sent to the
	 * nsca server
	 */
//	TODO expose to configuration
	private int buffer_size = 1;

	/**
	 * Stores message-events. When the buffer_size is reached, the buffer will
	 * be flushed and the messages will delivered to nsca server.
	 */
	private ArrayList buffer = new ArrayList();


	/**
	 * A flag to indicate that everything is ready to execute append()-commands
	 */
	private boolean ready = false;

	/**
	 * List of include filters ....
	 */
	private TreeMap includeFilters = null;

	/**
	 * List of exclude filters ....
	 */
	private TreeMap excludeFilters = null;

	/**
	 * boolean attributes enabling filtering by regex settings
	 */
	private boolean includeFilterEnabled = false;
	private boolean excludeFilterEnabled = false;

	/*
	 * Optional message to be sent to Nagios when the appender is instantiated.
	 *
	 */
	private String startupMsg = null;
	private String startupMsgLevel = null;

	/**
	* Store for any initial messages that arrive before we're ready to go ...
	*/
	private TreeMap initialMsgs = new TreeMap();

	/**
	* Underlying object which handles transport
	*/
	private Nsca nagiosClient = new Nsca();

	/**
	 * Constructor for the NagiosAppender object
	 */
	public NagiosAppender() {
		super();
	}

	/**
	 * Constructor for the NagiosAppender object
	 *
	 * @param layout
	 *            Allows you to set your Layout-instance
	 */
	public NagiosAppender(Layout layout) {
		super();
		this.setLayout(layout);
	}

	/**
	 * Sets the Layout attribute of the NagiosAppender object
	 *
	 * @param layout
	 *            The new Layout value
	 */
	public void setLayout(Layout layout) {
		super.setLayout(layout);
	}


	/**
	 * Sets path of nagios config file
	 *
	 * @param value
	 *            The new Url value
	 */
	public void setConfigFile(String value) {

		try
		{
			nagiosClient.setConfigFile(value);
		} catch (Exception e)
		{
			String errorMsg = "NagiosAppender::setConfigFile(), error reading config file: " + e.getMessage();
			log.error(errorMsg);
			errorHandler.error(errorMsg, null, 0);
		}
	}

	public void setIncludeFilterEnabled(String value)
	{
		this.includeFilterEnabled = Boolean.valueOf(value).booleanValue();
	}
	public void setExcludeFilterEnabled(String value)
	{
		this.excludeFilterEnabled = Boolean.valueOf(value).booleanValue();
	}

	/**
	 * Sets the encryption method (instead of reading it from the config file)
	 *
	 * @param value
	 *            The new encryption method (0=None, 1=XOR)
	 */
	public void setEncryptionMethod(int value) {
		if (value < 0 || value > 26) {
			System.out.println("Invalid encryption method value: " + value);
			return; }

		encryption_method = value;
	}

	/**
	 * Sets the NSCA password (instead of reading it from the config file)
	 *
	 * @param value
	 *            The new NSCA password
	 */
	public void setPassword(String value) {
		if (value == null) { return; }

		value = value.trim();

		if (value.length() == 0) { return; }

		nsca_password = value;
	}

	/**
	 * Sets the host name where nsca server is running
	 *
	 * @param value
	 *            The new hostname value
	 */
	public void setHost(String value) {
		if (value == null) { return; }

		value = value.trim();

		if (value.length() == 0) { return; }

		host = value;

		// this will send any startup msg, if needed
		//nagiosClient.ready();
	}

	/**
	 * Sets the port where the nsca server is running
	 *
	 * @param value
	 *            The new port value
	 */
	public void setPort(String value) {

		if (value == null) { return; }

		value = value.trim();

		if (value.length() == 0) { return; }

		port = value;

		// this will send any startup msg, if needed
		//ready();
	}

	/**
	 * Sets the service name for nagios
	 *
	 * @param value
	 *            The new service_name value
	 */
	public void setServiceNameDefault(String value) {

		if (value == null) { return; }

		value = value.trim();

		if (value.length() == 0) { return; }

		service_name = value;
	}

	/**
	 * Sets the MDC key to be used for determining hostname for nagios
	 *
	 * @param value
	 */
	public void setUseMDCHostName(boolean value) {
		use_MDC_for_hostname = value;
	}

	/**
	 * Sets the MDC key to be used for determining servicename for nagios
	 *
	 * @param value
	 */
	public void setUseMDCServiceName(boolean value) {
		use_MDC_for_servicename = value;
	}

	/**
	 * Sets the service name for nagios
	 *
	 * @param value
	 *            The new service_name value
	 */
	public void setMDCServiceNameKey(String value) {

		if (value == null) { return; }

		value = value.trim();

		if (value.length() == 0) { return; }

		MDC_service_name_key = value;
	}

	/**
	 * Sets the MDC key for the hostname
	 *
	 * @param value
	 *            The new service_name value
	 */
	public void setMDCHostNameKey(String value) {

		if (value == null) { return; }

		value = value.trim();

		if (value.length() == 0) { return; }

		MDC_hostname_key = value;

		// since we can't control the order of attribute calls from the framework, we'll make this
		// defensive call ... in case setInitializeMDCHostNameValue() was called before this method
		if (null != MDC_hostname_value)
		{
			setInitializeMDCHostNameValue(MDC_hostname_value);
		}
	}

	/**
	 * Initialializes the MDC value for hostname
	 *
	 * @param value
	 *            The initial value
	 */
	public void setInitializeMDCHostNameValue(String value) {

		if (value == null) { return; }

		value = value.trim();

		if (value.length() == 0) { return; }

		if (null != MDC_hostname_key)
		{
			MDC.put(MDC_hostname_key, value);
		} else {
			MDC_hostname_value = value;
		}
	}

	/**
	 * Sets the MDC key for the canonical hostname, which is determined programmatically
	 *
	 * @param value
	 *            The key
	 */
	public void setMDCCanonicalHostNameKey(String value) {

		if (value == null) { return; }

		value = value.trim();

		if (value.length() == 0) { return; }

		try
		{
			InetAddress address = InetAddress.getLocalHost();
			String nagios_canonical_hostname = "nagios_canonical_hostname";
			if (null == MDC.get(nagios_canonical_hostname))
				MDC.put(value, address.getCanonicalHostName());
		} catch (Exception e) {
			log.error(e);
		}
	}

	/**
	 * Name of the properties file containing a list of include and exclude filters
	 * to be applied against the message text before passing along ...
	 *
	 * @param filterFile
	 */
	public void setPatternFilterFile(String filterFile)
	{
		try
		{

			includeFilters = new TreeMap();
			excludeFilters = new TreeMap();
			BufferedReader in = new BufferedReader(new FileReader(filterFile));
			String nextLine = in.readLine();
			while (null != nextLine)
			{
				if (nextLine.toLowerCase().startsWith("includefilter="))
				{
					String regex = nextLine.substring(nextLine.indexOf('=') + 1, nextLine.length());
					if ((null != regex) && (regex.trim().length() > 0))
							includeFilters.put(regex, Pattern.compile(regex));
				}
				if (nextLine.toLowerCase().startsWith("excludefilter="))
				{
					String regex = nextLine.substring(nextLine.indexOf('=') + 1, nextLine.length());
					if ((null != regex) && (regex.trim().length() > 0))
							excludeFilters.put(regex, Pattern.compile(regex));
				}
				nextLine = in.readLine();
			}

			in.close();
		} catch (Exception e)
		{
			String errorMsg = "NagiosAppender::setPatternFilterFile() - File open error: " + filterFile;
			log.error(errorMsg, e);
			errorHandler.error(errorMsg, e, 0);
		}
	}

	/**
	 * Gets the Port attribute of the NagiosAppender object
	 *
	 * @return The Port value
	 */
	public String getPort() {
		return port;
	}

	/**
	 * Gets the Host attribute of the NagiosAppender object
	 *
	 * @return The Host value
	 */
	public String getHost() {
		return host;
	}

	/**
	 * Sets the hostname_style attribute of the NagiosAppender object
	 *
	 * @return The value indicating the style of hostname: long vs short

	 */
	public void setUseShortHostName(boolean value) {
		short_hostname = value;
	}

	/**
	 * Gets the ServiceNameDefault attribute of the NagiosAppender object
	 *
	 * @return The Port value
	 */
	public String getServiceNameDefault() {
		return service_name;
	}

	/**
	 * Gets the ConfigFile attribute of the NagiosAppender object
	 *
	 * @return The ConfigFile value
	 */
	public String getConfigFile() {
		return config_file;
	}

	/**
	 * Gets the EncryptionMethod attribute of the NagiosAppender object
	 *
	 * @return The EncryptionMethod value
	 */
	public int getEncryptionMethod() {
		return encryption_method;
	}

	/**
	 * Gets the Password attribute of the NagiosAppender object
	 *
	 * @return The Password value
	 */
	public String getPassword() {
		return nsca_password;
	}


	/**
	 * Implemented to return "true" .
	 */
	public boolean requiresLayout() {
		return true;
	}

	/**
	 * Sets the nagios return code that you want mapped to DEBUG within log4j
	 *
	 * @param value
	 *            The new return code value for DEBUG
	 */
	public void setLog4j_Level_DEBUG(String value) {
		if (value == null) { return; }

		value = value.trim();

		if (value.length() == 0) { return; }

		switch ((new Integer((String)Nagios_levels.get(value))).intValue()) {
		case NAGIOS_OK:		Log4j_Level_DEBUG = NAGIOS_OK; break;
		case NAGIOS_WARN:	Log4j_Level_DEBUG = NAGIOS_WARN; break;
		case NAGIOS_CRITICAL:	Log4j_Level_DEBUG = NAGIOS_CRITICAL; break;
		case NAGIOS_UNKNOWN:	Log4j_Level_DEBUG = NAGIOS_UNKNOWN; break;
		default:		Log4j_Level_DEBUG = NAGIOS_UNKNOWN; break;
		}

	}

	/**
	 * Gets the nagios return code that is mapped to DEBUG within log4j
	 */
	public String getLog4j_Level_DEBUG() {
		return "" + Log4j_Level_DEBUG;
	}

	/**
	 * Sets the nagios return code that you want mapped to INFO within log4j
	 *
	 * @param value
	 *            The new return code value for INFO
	 */
	public void setLog4j_Level_INFO(String value) {
		if (value == null) { return; }

		value = value.trim();

		if (value.length() == 0) { return; }

		switch ((new Integer((String)Nagios_levels.get(value))).intValue()) {
		case NAGIOS_OK:		Log4j_Level_INFO = NAGIOS_OK; break;
		case NAGIOS_WARN:	Log4j_Level_INFO = NAGIOS_WARN; break;
		case NAGIOS_CRITICAL:	Log4j_Level_INFO = NAGIOS_CRITICAL; break;
		case NAGIOS_UNKNOWN:	Log4j_Level_INFO = NAGIOS_UNKNOWN; break;
		default:		Log4j_Level_INFO = NAGIOS_UNKNOWN; break;
		}
	}

	/**
	 * Gets the nagios return code that is mapped to INFO within log4j
	 */
	public String getLog4j_Level_INFO() {
		return "" + Log4j_Level_INFO;
	}

	/**
	 * Sets the nagios return code that you want mapped to WARN within log4j
	 *
	 * @param value
	 *            The new return code value for WARN
	 */
	public void setLog4j_Level_WARN(String value) {
		if (value == null) { return; }

		value = value.trim();

		if (value.length() == 0) { return; }

		switch ((new Integer((String)Nagios_levels.get(value))).intValue()) {
		case NAGIOS_OK:		Log4j_Level_WARN = NAGIOS_OK; break;
		case NAGIOS_WARN:	Log4j_Level_WARN = NAGIOS_WARN; break;
		case NAGIOS_CRITICAL:	Log4j_Level_WARN = NAGIOS_CRITICAL; break;
		case NAGIOS_UNKNOWN:	Log4j_Level_WARN = NAGIOS_UNKNOWN; break;
		default:		Log4j_Level_WARN = NAGIOS_UNKNOWN; break;
		}
	}

	/**
	 * Gets the nagios return code that is mapped to WARN within log4j
	 */
	public String getLog4j_Level_WARN() {
		return "" + Log4j_Level_WARN;
	}

	/**
	 * Sets the nagios return code that you want mapped to ERROR within log4j
	 *
	 * @param value
	 *            The new return code value for ERROR
	 */
	public void setLog4j_Level_ERROR(String value) {
		if (value == null) { return; }

		value = value.trim();

		if (value.length() == 0) { return; }

		switch ((new Integer((String)Nagios_levels.get(value))).intValue()) {
		case NAGIOS_OK:		Log4j_Level_ERROR = NAGIOS_OK; break;
		case NAGIOS_WARN:	Log4j_Level_ERROR = NAGIOS_WARN; break;
		case NAGIOS_CRITICAL:	Log4j_Level_ERROR = NAGIOS_CRITICAL; break;
		case NAGIOS_UNKNOWN:	Log4j_Level_ERROR = NAGIOS_UNKNOWN; break;
		default:		Log4j_Level_ERROR = NAGIOS_UNKNOWN; break;
		}
	}

	/**
	 * Gets the nagios return code that is mapped to ERROR within log4j
	 */
	public String getLog4j_Level_ERROR() {
		return "" + Log4j_Level_ERROR;
	}

	/**
	 * Sets the nagios return code that you want mapped to FATAL within log4j
	 *
	 * @param value
	 *            The new return code value for FATAL
	 */
	public void setLog4j_Level_FATAL(String value) {
		if (value == null) { return; }

		value = value.trim();

		if (value.length() == 0) { return; }

		switch ((new Integer((String)Nagios_levels.get(value))).intValue()) {
		case NAGIOS_OK:		Log4j_Level_FATAL = NAGIOS_OK; break;
		case NAGIOS_WARN:	Log4j_Level_FATAL = NAGIOS_WARN; break;
		case NAGIOS_CRITICAL:	Log4j_Level_FATAL = NAGIOS_CRITICAL; break;
		case NAGIOS_UNKNOWN:	Log4j_Level_FATAL = NAGIOS_UNKNOWN; break;
		default:		Log4j_Level_FATAL = NAGIOS_UNKNOWN; break;
		}
	}

	/**
	 * Gets the nagios return code that is mapped to FATAL within log4j
	 */
	public String getLog4j_Level_FATAL() {
		return "" + Log4j_Level_FATAL;
	}

	public void setSendStartupMessageOK(String msg)
	{
		setSendStartupMessage("" + NAGIOS_OK, msg);
	}
	public void setSendStartupMessageUNKNOWN(String msg)
	{
		setSendStartupMessage("" + NAGIOS_UNKNOWN, msg);
	}
	public void setSendStartupMessageWARN(String msg)
	{
		setSendStartupMessage("" + NAGIOS_WARN, msg);
	}
	public void setSendStartupMessageCRITICAL(String msg)
	{
		setSendStartupMessage("" + NAGIOS_CRITICAL, msg);
	}
	private void setSendStartupMessage(String nagiosLevel, String msg)
	{
		this.startupMsg = msg;
		this.startupMsgLevel = nagiosLevel;
	}
	private void sendMessage(String nagiosLevel, String msg)
	{
		String myClassName = this.getClass().getName();
		LoggingEvent event = new LoggingEvent(myClassName, Logger.getInstance(this.getClass()), Priority.INFO, msg, null);

		try
		{
			if (nagiosClient.ready())
				send_nsca(event, stringToNagiosLevel(nagiosLevel));
		} catch (Exception e)
		{
			String errorMsg = "NagiosAppender::sendMessaage(), Not ready to append: " + e.getMessage();
			log.error(errorMsg);
			errorHandler.error(errorMsg, null, 0);
		}
	}

	private int stringToNagiosLevel(String levelAsString)
	{
		try
		{
			int nagiosLevel = (new Integer(levelAsString)).intValue();
			switch (nagiosLevel)
			{
				case NAGIOS_UNKNOWN :
				case NAGIOS_OK:
				case NAGIOS_WARN:
				case NAGIOS_CRITICAL:
					return nagiosLevel;
				default:
					return DO_NOT_SEND;
			}
		} catch (NumberFormatException e) {}

		return DO_NOT_SEND;

	}


	/**
	 * Delivers outstanding messages to nsca server.
	 *
	 * @param event
	 *            Description of Parameter
	 */
	public void append(LoggingEvent event) {

		// throw the msg away if the user hasn't explicitly mapped it to a typed Nagios code
		if (event.getLevel().equals(Level.DEBUG) && Log4j_Level_DEBUG == DO_NOT_SEND) return;
		if (event.getLevel().equals(Level.INFO ) && Log4j_Level_INFO  == DO_NOT_SEND) return;
		if (event.getLevel().equals(Level.WARN ) && Log4j_Level_WARN  == DO_NOT_SEND) return;
		if (event.getLevel().equals(Level.ERROR) && Log4j_Level_ERROR == DO_NOT_SEND) return;
		if (event.getLevel().equals(Level.FATAL) && Log4j_Level_FATAL == DO_NOT_SEND) return;

		// ignore custom log levels ..... including TRACE
		switch (event.getLevel().toInt())
		{
			case Level.DEBUG_INT:
			case Level.INFO_INT:
			case Level.WARN_INT:
			case Level.ERROR_INT:
			case Level.FATAL_INT:
				break;
			default:
				return;
		}

		// Are include filters on .... and did we fail?  If so, don't send the message
		if (this.includeFilterEnabled && failedRegexIncludeFilter(event))
		{
			return;
		}

		// Are exclude filters on .... and did we fail?  If so, don't send the message
		if (this.excludeFilterEnabled && failedRegexExcludeFilter(event))
		{
			return;
		}


		try {
			if (!nagiosClient.ready()) {
				if (!nagiosClient.ready()) {
					String errorMsg = "NagiosAppender::append(), Not ready to append !";
					log.error(errorMsg);
					errorHandler.error(errorMsg, null, 0);
					return;
				}
			}

			buffer.add(event);

			if (buffer_size > 1) {
				// MDC problem fix for buffer_size > 1 from Patrick Carlos
				// --- Taken from AsyncAppender ---
				// Set the NDC and thread name for the calling thread as these
				// LoggingEvent fields were not set at event creation time.
				event.getNDC();
				event.getThreadName();
				// Get a copy of this thread's MDC.
				event.getMDCCopy();
				// make sure to also remember locationinfo in the event
				event.getLocationInformation();
			}

			if (buffer.size() >= buffer_size) {
				flush_buffer();
			}
		} catch (Exception e) {
			String errorMsg = "NagiosAppender::append(), ";
			log.error(errorMsg, e);
			errorHandler.error(errorMsg, e, 0);
		}
	}

	/**
	 * Delivers outstanding messages to nsca server.  Ignores mappings from Log4j <-> Nagios
	 *
	 * @param event
	 *
	 */
	public boolean send(String event, int return_code) {
		boolean sentOK = false;
		try {
			if (!nagiosClient.ready()) {
				if (!nagiosClient.ready()) {
					String errorMsg = "NagiosAppender::append(), Not ready to append !";
					log.error(errorMsg);
					errorHandler.error(errorMsg, null, 0);
					return false;
				}
			}

			sentOK = send_nsca(event, return_code);
		} catch (Exception e) {
			String errorMsg = "NagiosAppender::append(), ";
			log.error(errorMsg, e);
			errorHandler.error(errorMsg, e, 0);
		}
		return sentOK;
	}

	/**
	 * Interface requirement.
	 */
	public void close()
	{
		// nothing to do
	}

	/**
	 * Returns true, when the NagiosAppender is ready to write
	 * messages to the nsca server, else false.
	 *
	 * @return Description of the Returned Value
	 */
/*
	public boolean ready() {
		if (ready) { return true; }

		if (!configured) {
			if (!configure()) { return false; }
		}

		if (null == this.host) { return false; }
		if (null != this.port)
		{
			try { new Integer(this.port); } catch (Exception e)  { return false; }
		} else {
			return false;
		}

		ready = true;

		//Default Message-Layout
		if (layout == null) {
			layout = new PatternLayout("%m");
		}

		return ready;
	}
*/

	/**
	 * Internal method. Configures for appending...
	 *
	 * @return Boolean specifying whether configuration succeeded
	 */
/*
	protected boolean configure() {
		BufferedReader in;
		try {
			if (configured) { return true; }

			if (config_file == null) {
				// No config-file specified - assumes the default encryption method and no password.
				return true;
			}
			in = new BufferedReader(new FileReader(config_file));

		} catch (FileNotFoundException e) {
			String errorMsg = "NagiosAppender::configure() - File not found: " + config_file;
			LogLog.error(errorMsg, e);
			errorHandler.error(errorMsg, e, 0);
			return false;
		} catch (Exception e) {
			String errorMsg = "NagiosAppender::configure() - File open error: " + config_file;
			LogLog.error(errorMsg, e);
			errorHandler.error(errorMsg, e, 0);
			return false;
		}

		// Parse the file
		String input;
		Pattern p = Pattern.compile("^(\\w+)\\s*=\\s*(\\w+)$");
		Matcher m = p.matcher("fred");
		try {
			while ((input = in.readLine()) != null) {
				m.reset(input);
				if (m.find() && m.groupCount() == 2) {
					if (m.group(1).compareTo("encryption_method") == 0)
						encryption_method = Integer.valueOf(m.group(2)).intValue();
					if (m.group(1).compareTo("password") == 0)
						nsca_password = m.group(2);
				}
			}
		} catch (EOFException e) {
			// Normal EOF - do nothing
		} catch (IOException e) {
			String errorMsg = "NagiosAppender::configure() - I/O error: " + config_file;
			LogLog.error(errorMsg, e);
			errorHandler.error(errorMsg, e, 0);
		}

		try {
			in.close();
		} catch (IOException e) {
			String errorMsg = "NagiosAppender::configure() - Cannot close file: " + config_file;
			LogLog.error(errorMsg, e);
			errorHandler.error(errorMsg, e, 0);
		}

		configured = true;

		return true;
	}
*/

	/**
	 * character to separate parts of layout if more than one part is to be used
	 * @return layoutPartsDelimiter
	 */
	public String getLayoutPartsDelimiter() {
		return layoutPartsDelimiter;
	}

	/**
	 * character to separate parts of layout if more than one part is to be used
	 * @param c
	 */
	public void setLayoutPartsDelimiter(String c) {
		layoutPartsDelimiter = c;
	}


	/**
	 *  Flushes the buffer.
	 */
	public void flush_buffer() {
		try {
			int size = buffer.size();

			if (size < 1) { return; }

			for (int i = 0; i < size; i++) {
				LoggingEvent event = (LoggingEvent) buffer.get(i);
				send_nsca(event);
			}

			buffer.clear();

		} catch (Exception e) {
			String errorMsg = "NagiosAppender::flush_buffer(), : " + e.getMessage();
			log.error(errorMsg, e);
			errorHandler.error(errorMsg, e, 0);
			buffer.clear();
			try {
			} catch (Exception ex) {
				/* empty */
			}
			return;
		}
	}

    /** Called by log4j once all it's done configuring our appender from the config file
	*/
	public void activateOptions()
	{
		if ((null != this.startupMsgLevel) && (null != this.startupMsg))
		{
			sendMessage(this.startupMsgLevel, this.startupMsg);
		}
		this.startupMsgLevel = "" + Level.OFF;
		this.startupMsg = null;
	}


	/**
	 * Encrypts the send buffer according the nsca encryption method
	 *
	 * @param buffer
	 *            Buffer to be encrypted
	 * @param iv
	 *            Encryption Initialization Vector
	 */
	public void encrypt_buffer(byte[] buffer, byte[] server_iv)
	{

		switch (encryption_method) {

		case ENCRYPT_NONE:
			break;

		case ENCRYPT_XOR:
			/* rotate over IV we received from the server... */
			for (int y=0,x=0; y < buffer.length; y++,x++) {

				/* keep rotating over IV */
				if (x >= TRANSMITTED_IV_SIZE)
					x = 0;

				buffer[y] ^= server_iv[x];
			}

			/* rotate over password... */
			if (nsca_password != null) {
				byte[] password = nsca_password.getBytes();

				for (int y=0,x=0; y < buffer.length; y++,x++) {

					/* keep rotating over password */
					if (x >= password.length)
						x = 0;

					buffer[y] ^= password[x];
				}
			}
			break;

		default:
			String errorMsg = "NagiosAppender::encrypt_buffer(): unsupported encryption method: " + encryption_method;
		log.error(errorMsg);
		errorHandler.error(errorMsg, null, 0);
		;break;
		}
	}

	private void send_nsca(LoggingEvent event)
	{
		send_nsca(event, getReturnCode(event));
	}

	private void send_nsca(LoggingEvent event, int return_code)
	{

		// let's make the Pattern an option, not a requirement ....
		if (null != this.getLayout())
			send_nsca("" + ((PatternLayout) this.getLayout()).format(event), return_code);
		else
			send_nsca("" + event.getMessage(), return_code);
	}

	/**
	 * This is the meat of the appender ... it makes no attempt to wait if the connection
	 * can't be established
	 */
	private boolean send_nsca(String message, int return_code)
	{
		if (null == message) return false;
		boolean sentOK = false;
		try
		{

			nagiosClient.send_nsca(getHost(), getPort(), getHostName(), getServiceName(), message, return_code/*, encryption_method */);
			sentOK = true;
/*

			while( true )
			{
				s = new Socket( getHost(), (new Integer(getPort())).intValue() );
				if (null != s)
					break;
			}

			OutputStream out = s.getOutputStream();
			DataInputStream in   = new DataInputStream(s.getInputStream());
			byte[] received_iv = new byte[128];
			in.readFully(received_iv, 0, 128); // Read the encryption Initialization Vector
			int time = in.readInt();           // Read the server time stamp

			// local variable used for populating byte arrays.
			String temp;

			// Set up the return code that will be sent to nagios.
			//
			// NOTE: nagios codes and log4j message types do not map 1 to 1.  The mappings
			//       can be managed via the appender configuration parameters to suit your
			//       needs.
			//
			//int return_code  = getReturnCode(event);

			// Set up the host that the push is initiated from.
			//
			byte[] host_name = new byte[64];
			temp = this.getHostName();
			System.arraycopy(temp.getBytes(),0,host_name,0,temp.getBytes().length);

			// Set up our service name.
			//
			// if the application has set a service name via MDC, that will override anything
			// configured via log4j

			byte[] service_name = new byte[128];
			temp = this.getServiceName();
			System.arraycopy(temp.getBytes(),0,service_name,0,temp.getBytes().length);

			// Set up the free text message.
			//
			byte[] plugin_output = new byte[512];

			// nsca doesn't handle line feeds very well ..... let's remove them
			message.replaceAll("\n", "<linefeed>");

			if (message.getBytes().length <= 512)
			{
				System.arraycopy(message.getBytes(),0,plugin_output,0,message.getBytes().length);
			}
			else
			{
				System.arraycopy(message.getBytes(),0,plugin_output,0,plugin_output.length);
			}

			// alert is made up of 4 ints, followed by 3 strings
			//
			int alert_size = 4 + 4 + 4 + 4 + host_name.length + service_name.length + plugin_output.length;
			byte[] alert = new byte[alert_size];

			// 1st int
			alert[0] = (byte)((nsca_version >> 8) & 0xff);
			alert[1] = (byte) (nsca_version       & 0xff);

			// 2nd int
			// we calculate the crc with zeroes in the crc field ...
			alert[4] = (byte)((0            >> 24) & 0xff);
			alert[5] = (byte)((0            >> 16) & 0xff);
			alert[6] = (byte)((0            >> 8 ) & 0xff);
			alert[7] = (byte) (0                   & 0xff);

			// 3rd int (we're echo'ing back the time we read from the server)
			alert[8] = (byte)((time         >> 24) & 0xff);
			alert[9] = (byte)((time         >> 16) & 0xff);
			alert[10]= (byte)((time         >> 8 ) & 0xff);
			alert[11]= (byte) (time                & 0xff);

			// 4th int (this is the code associated with the alert)
			alert[12]= (byte)((return_code  >> 8) & 0xff);
			alert[13]= (byte) (return_code        & 0xff);

			int offset = 14;
			// 1st of 3 strings
			System.arraycopy(host_name,0,alert,offset,host_name.length);
			offset += host_name.length;

			// 2nd of 3 strings
			System.arraycopy(service_name,0,alert,offset,service_name.length);
			offset += service_name.length;

			// 3rd of 3 strings
			System.arraycopy(plugin_output,0,alert,offset,plugin_output.length);
			offset += plugin_output.length;

			// now we can calculate the crc
			CRC32 crc = new CRC32();
			crc.update(alert);
			long crc_value = crc.getValue();

			// now that we've calculated the crc, fill it in
			alert[4] = (byte)((crc_value    >> 24) & 0xff);
			alert[5] = (byte)((crc_value    >> 16) & 0xff);
			alert[6] = (byte)((crc_value    >> 8 ) & 0xff);
			alert[7] = (byte) (crc_value           & 0xff);

			// encrypt the buffer
			encrypt_buffer(alert, received_iv);
			// write to the socket
			out.write(alert,0,alert.length);
			out.flush();

			// clean up
			out.close();
			in.close();
			s.close();

*/

		}
		catch (Exception e)
		{
			String errorMsg = "NagiosAppender::send_nsca(), Exception thrown trying to deliver log4j record to Nagios: " +
			"nagios server= " + getHost() + ", nagios port = " + getPort() + ": " + e.getMessage();
			log.error(errorMsg, e);
			errorHandler.error(errorMsg, e, 0);
		}
		return sentOK;
	}

	private int getReturnCode(LoggingEvent event)
	{
		int return_code = NAGIOS_UNKNOWN;
		if (event.getLevel().equals(Level.ERROR))
		{
			return_code = Log4j_Level_ERROR;
		} else if (event.getLevel().equals(Level.WARN))
		{
			return_code = Log4j_Level_WARN;
		} else if (event.getLevel().equals(Level.FATAL))
		{
			return_code = Log4j_Level_FATAL;
		} else if (event.getLevel().equals(Level.INFO))
		{
			return_code = Log4j_Level_INFO;
		} else if (event.getLevel().equals(Level.DEBUG))
		{
			return_code = Log4j_Level_DEBUG;
		}
		return return_code;
	}

	protected String getMyHostName(boolean short_name_only)
	{
		String hostname = null;
		try
		{
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {return null;}

		if (short_name_only)
		{
			if (hostname.indexOf(".") > 0)
				hostname = hostname.substring(0,hostname.indexOf("."));
		}
		return hostname;

	}

	private boolean failedRegexIncludeFilter(LoggingEvent event )
	{
		if (null == this.includeFilters) return false;
		Iterator it = this.includeFilters.keySet().iterator();
		while (it.hasNext())
		{
			Matcher m = ((Pattern) this.includeFilters.get((String) it.next())).matcher((String) event.getMessage());
			if (m.matches())
				return false;
		}
		return true;
	}

	private boolean failedRegexExcludeFilter(LoggingEvent event )
	{
		if (null == this.excludeFilters) return false;
		Iterator it = this.excludeFilters.keySet().iterator();
		while (it.hasNext())
		{
			Matcher m = ((Pattern) this.excludeFilters.get((String) it.next())).matcher((String) event.getMessage());
			if (m.matches())
				return true;
		}
		return false;
	}

	/**
	 * Returns the value of HOST that the message will be associated with.
	 * The value is obtained either from an MDC setting (configurable), or from
	 * the name of the machine itself.   The name of the machine can either be
	 * fully qualified, or short (configurable).
	 *
	 * If the application is misconfigured, it will return "NOT_SET"
	 *
	 * @return value for HOST field in nagios message
	 *
	 */
	protected String getHostName()
	{
		String hostname = null;
		if (use_MDC_for_hostname)
		{
			hostname = (String) MDC.get(MDC_hostname_key);
			if ((null == hostname) || (hostname.length() == 0))
			{
				String errorMsg = "MDC lookup for hostname failed ... key = " + MDC_hostname_key;
				Exception e = new Exception(errorMsg);
				log.error(errorMsg, e);
				errorHandler.error(errorMsg, e, 0);
				hostname = "NOT_SET";
			}
		}
		else
		{
			hostname = getMyHostName(short_hostname);
			if ((null == hostname) || (hostname.length() == 0))
			{
				String errorMsg = "hostname was not set in log4j config file";
				Exception e = new Exception(errorMsg);
				log.error(errorMsg, e);
				errorHandler.error(errorMsg, e, 0);
				hostname = "NOT_SET";
			}
		}
		return hostname;
	}

	/**
	 * Returns the value of SERVICE that the message will be associated with.
	 * The value is obtained either directly from configuration, or via an
	 * MDC lookup (configurable).
	 *
	 * If the application is misconfigured, it will return "NOT_SET"
	 *
	 * @return value for SERVICE field in nagios message
	 *
	 */
	protected String getServiceName()
	{
		String serviceName = null;
		if (use_MDC_for_servicename)
		{
			serviceName = (String) MDC.get(MDC_service_name_key);
			if ((null == serviceName) || (serviceName.length() == 0))
			{
				String errorMsg = "MDC lookup for servicename failed ... key = " + MDC_service_name_key;
				Exception e = new Exception(errorMsg);
				log.error(errorMsg, e);
				errorHandler.error(errorMsg, e, 0);
				serviceName = "NOT_SET";
			}
		}
		else
		{
			serviceName = getServiceNameDefault();
			if ((null == serviceName) || (serviceName.length() == 0))
			{
				String errorMsg = "servicename was not set in log4j config file";
				Exception e = new Exception(errorMsg);
				log.error(errorMsg, e);
				errorHandler.error(errorMsg, e, 0);
				serviceName = "NOT_SET";
			}
		}
		return serviceName;
	}
}
