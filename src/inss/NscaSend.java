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
package inss;

//import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * This class is a thin wrapper around the NagiosAppender class.  As such, it is dependent on the log4j framework.
 *
 * The main difference between this class, and the lower level class Nsca.java (which has no dependencies on log4j)
 * is that here you can use dynamic fields (properties) in the messages.
 *
 * <p>
 * Example usage:
 *
 * <pre>
 *		try
 *		{
 *			System.out.println("creating instance of NscaSend ...");
 *			NscaSend nscaSend = new NscaSend();
 *			nscaSend.setNscaConfigFile("conf/nsca_send_clear.cfg");
 *			nscaSend.setNagiosHost("localhost");
 *			nscaSend.setNagiosPort("5667");
 *			nscaSend.setNagiosVirtualHost("production");
 *			nscaSend.setNagiosServiceName("MyApplication");
 *			nscaSend.setUseShortHostname();
 *			nscaSend.send(NagiosAppender.NAGIOS_CRITICAL,"server: [HOSTNAME]: something bad just happened ....");
 *
 *			Properties customProps = new Properties();
 *			customProps.put("FOO", "foo");
 *			customProps.put("BAR", "bar");
 *			nscaSend.setMessageProperties(customProps);
 *			nscaSend.send(NagiosAppender.NAGIOS_CRITICAL,"server: [HOSTNAME]: checking custom properties .... do you see foobar?: [FOO][BAR]");
 *
 *		} catch (Exception e)
 *		{
 *			System.out.println("exception thrown ... " + e.getMessage());
 *		}
 *
 * </pre>
 *
 *
 * @author <a href="mailto:jarlyons@gmail.com">Jar Lyons</a>
 *
 *
 * @author <a href="mailto:jarlyons@gmail.com">Jar Lyons</a>
 *
 **/
public class NscaSend implements IMakao
{
	private  NagiosAppender nagiosAppender = null;
	private String nagiosHost = null;
	private String nagiosPort = null;
	private String nscaConfigFile = null;

	private boolean useShortHostname = false;
	private String virtualHostname = null;
	private String nagiosServiceName = "NOT_SET";
	private static org.apache.log4j.Logger log = Logger.getLogger(NscaSend.class);
	public boolean allOK = true;
	/**
	* list of properties which can be used to dynamically replace message parameters
	* NOTE: HOSTNAME will be automagically added if it's not set by you ....
	*
	* example message using template variables:
	*
	*    "source:[HOSTNAME]: something bad happened ...."
	*/
	private Properties props = new Properties();

	public NscaSend()
	{
	}
	/**
	* This is the path to the config file that you would normally use with nsca_send
	*/
	public void setNscaConfigFile(String nscaConfigFile)
	{
		this.nscaConfigFile = nscaConfigFile;
	}
	/**
	* Host of the NSCA server
	*/
	public void setNagiosHost(String host)
	{
		this.nagiosHost = host;
	}
	/**
	* Port of the NSCA server
	*/
	public void setNagiosPort(String port)
	{
		this.nagiosPort = port;
	}
	/**
	* @see http://www.novell.com/communities/node/4131/application-monitoring-made-easy-java-applications-using-nagios
	*/
	public void setNagiosVirtualHost(String virtualHostname)
	{
		this.virtualHostname = virtualHostname;
	}
	/**
	* This is the SERVICE name that must be configured in Nagios
	*/
	public void setNagiosServiceName(String serviceName)
	{
		this.nagiosServiceName = serviceName;
	}
	/**
	* Use this method to note whether you want a fqn or short hostname
	*/
	public void setUseShortHostname()
	{
		this.useShortHostname = true;
	}
	/**
	* Properties to use with template based messages
	*/
	public void setMessageProperties(Properties customProps)
	{
		this.props = customProps;
	}
	private void init()
	{
		synchronized (this)
		{
			if (null != this.nagiosAppender) return;

			this.nagiosAppender = new NagiosAppender();

			// Set up hard-coded replacement pairs
			if ((null != this.props) && null == this.props.getProperty("HOSTNAME"))
			{
				props.put("HOSTNAME", nagiosAppender.getMyHostName(this.useShortHostname));
			}

			this.nagiosAppender.setHost(this.nagiosHost);
			this.nagiosAppender.setPort(this.nagiosPort);
			if (this.useShortHostname)
				this.nagiosAppender.setUseShortHostName(true);
			else
				this.nagiosAppender.setUseShortHostName(false);
			this.nagiosAppender.setServiceNameDefault(this.nagiosServiceName);

			if (null != this.virtualHostname)
			{
				this.nagiosAppender.setUseMDCHostName(true);
				this.nagiosAppender.setMDCHostNameKey("NagiosAlertHostname");
				this.nagiosAppender.setInitializeMDCHostNameValue(this.virtualHostname);
			}

			this.nagiosAppender.setConfigFile(this.nscaConfigFile);
			this.nagiosAppender.activateOptions();
		}
	}
	/**
	* Method which will deliver the message to the Nagios nsca server
	* getting all messages from a map.
	*
	* @param level level corresponding to one of: NagiosLevel.OK, NagiosLevel.WARN, NagiosLevel.CRITICAL,
	* 		or NagiosLevel.UNKNOWN
	* @param message message which will be passed to Nagios.  Bracketed terms (e.g. '[TESTING123]' will
	*		be dynamically replaced per the custom properties supplied in the setMessageProperties(..) method
	*
	*/
	public void sendMap(HashMap<String, String[]> nagiosMap )
	{
		if (null == this.nagiosAppender) {
			init();
		}
		String key = null;
		int counttries = 0;
		int countsent = 0;
		int total = 0;
		try
		{
			this.allOK = true;
			Set<String> keySet = nagiosMap.keySet();
			Iterator<String> it = keySet.iterator();
			total = keySet.size();
			log.info("Will send for host " + this.virtualHostname + " to nagios " + this.nagiosHost + " at port " + this.nagiosPort);
			while(it.hasNext() && counttries >= 3 == false){
				key = it.next();
				String[] result = nagiosMap.get(key);
				String service = result[1];
				if (result.length > 3 || result.length < 3){
					log.error("Internal error: result array index not correct: " + result.length);
					this.allOK = false;
					return;
				}
				int level = this.getNagiosInt(result[0]);
				if ((level < NagiosAppender.NAGIOS_OK) || (level > NagiosAppender.NAGIOS_UNKNOWN)) {
					log.error("Internal error: Nagios code not in range.");
					this.allOK = false;
					return;
				}
				InputStream input = new ReplacementInputStream(new ByteArrayInputStream(result[2].getBytes()), props);
				// Read one buffer at a time
				int count = 0;
				byte[] buffer = new byte[1024];
				while ((count = input.read(buffer)) > -1) { }
				this.setNagiosServiceName(service);
				this.nagiosAppender.setServiceNameDefault(this.nagiosServiceName);
//				log.debug("To send: " + service);
				boolean isSent = false;
				if(OpenMakao.debugSendNagios){
					isSent = nagiosAppender.send(new String(buffer), level);
				}
				if(isSent) {
					log.info("Message sent. Service: " + service + ", status: " + result[0] + ", description: " + result[2]);
					countsent++;
				}else {
					if(OpenMakao.debugSendNagios){
						log.error("No message sent due to error.");
						counttries++;
						this.allOK = false;
					}else{
						log.debug("Not sent (sendnagios parameter) Service: " + service + ", status: " + result[0] + ", description: " + result[2]);
					}
				}
				if( counttries >= 3 ) {
					log.error("Stop now trying to send messages due to more than 3 fails.");
					this.allOK = false;
					break;
				}
			}

		}
		catch (Exception e) {
			log.error("Service " + key + ": " + e);
			e.printStackTrace();
			this.allOK = false;
		}
		log.info("Counted " + countsent + " of " + total +  " messages successfully sent and " + counttries + " failures.");
	}
	/** translater from nagiosMap to nscaSend int value
	 *  TODO harmonize with nscaSend values
	 * @param code
	 * @return
	 */
	private int getNagiosInt(String code){  //TODO harmonize
		int result = -1;
		if (code.equals(NAGOK)) result = NagiosAppender.NAGIOS_OK;
		else if (code.equals(NAGWARNING)) result = NagiosAppender.NAGIOS_WARN;
		else if (code.equals(NAGCRITICAL)) result = NagiosAppender.NAGIOS_CRITICAL;
		else if (code.equals(NAGUNKNOWN)) result = NagiosAppender.NAGIOS_UNKNOWN;
		else {
			log.error("Internal error: result code not in range.");
		}
		return result;
	}
	/**
	* Method which will deliver the message to the Nagios nsca server
	*
	* @param level level corresponding to one of: NagiosLevel.OK, NagiosLevel.WARN, NagiosLevel.CRITICAL,
	* 		or NagiosLevel.UNKNOWN
	* @param message message which will be passed to Nagios.  Bracketed terms (e.g. '[TESTING123]' will
	*		be dynamically replaced per the custom properties supplied in the setMessageProperties(..) method
	*
	*/
	public void send(int level, String message)
	{

		if ((level < NagiosAppender.NAGIOS_OK) || (level > NagiosAppender.NAGIOS_UNKNOWN)) return;

		if (null == this.nagiosAppender)
			init();

		try
		{

			InputStream input = new ReplacementInputStream(new ByteArrayInputStream(message.getBytes()), props);

			// Read one buffer at a time
			int count;
			byte[] buffer = new byte[1024];
			while ((count = input.read(buffer)) > -1) { }
			nagiosAppender.send(new String(buffer), level);
		}
		catch (IOException e) { }

	}
	public static void testMessage(String[] args)
	{
		System.out.println("creating instance of NscaSend ...");
		NscaSend nscaSend = new NscaSend();
		String conf = "." + FileManager.fileSep + "conf" + FileManager.fileSep + "send_nsca.cfg";
		nscaSend.setNscaConfigFile(conf);
		nscaSend.setNagiosHost("192.168.99.129");
		nscaSend.setNagiosPort("5667");
		nscaSend.setNagiosVirtualHost("windowsone");
		nscaSend.setNagiosServiceName("ClientAdmin");
		nscaSend.setUseShortHostname();
		nscaSend.send(NagiosAppender.NAGIOS_OK,"server: [HOSTNAME]: something bad just happened ....");

		Properties customProps = new Properties();
		customProps.put("FOO", "foo");
		customProps.put("BAR", "bar");
		customProps.put("HOSTNAME", "wiedenn");
		nscaSend.setMessageProperties(customProps);
		nscaSend.send(NagiosAppender.NAGIOS_OK,"server: [HOSTNAME]: checking custom properties .... do you see foobar?: [FOO][BAR]");
	}


	/**
	 * This stream allows you to replace substrings, on-the-fly, when
	 * reading an input stream. It is geared toward text-based
	 * replacement. To keep things simple, the consutructor expects
	 * a Properties object which stores the replace/replacement
	 * value (string) pairs, where the key is the value to be
	 * replaced. The 'start' and 'stop' bytes are set to '[' and ']'
	 * by default by can be changed using the alternate constructor.
	 * Note that the characters are handled as integers to support
	 * unicode streams and that the delimiters are removed by the
	 * filter during replacement. Non matching delimiter pairs are
	 * ignored. Also, no attempt was made to handle delimiter mismatch
	 * problems. The algorithm will use the first start and nearest
	 * subsequent stop character to delimit a pattern. There is no
	 * restriction on using the same start and stop delimiter. Of
	 * course, the right choice of delimiters at design time is the
	 * most useful approach to avoiding later dificulties.
	**/
	private class ReplacementInputStream extends FilterInputStream
	{
		protected Properties properties;
		protected int start = '[';
		protected int stop = ']';
		protected final int escape = '`';

		protected boolean escaping = false;
		protected boolean encodeQuotes = true;

		private transient String key = "";

		/**
		* Construct a new filter stream which acts on the specified input
		* stream and replaces speficied properties. The properties argument
		* may be set to null to allow pass-trhough during testing.
		* @param input The input stream to filter
		* @param properties The replace/replacement pairs, excluding delimiters
		**/
		public ReplacementInputStream(InputStream input, Properties properties)
		{
			this(input, properties, '[', ']');
		}

		/**
		* Construct a new filter stream which acts on the specified input
		* stream and replaces speficied properties. The properties argument
		* may be set to null to allow pass-trhough during testing.
		* @param input The input stream to filter
		* @param properties The replace/replacement pairs, excluding delimiters
		* @param start The start/begin delimiter character
		* @param stop The stop/end delimiter character
		**/
		public ReplacementInputStream(InputStream input, Properties properties, int start, int stop)
		{
			super(input);
			this.properties = properties;
			this.start = start;
			this.stop = stop;
		}

		/**
		* Useful support method for popping the front character off the stack.
		**/
		private int popKey()
		{
			int chr = key.charAt(0);
			key = key.substring(1);
			return chr;
		}

		/**
		* Standard interface for reading a byte stream.
		* @see InputStream
		**/
		public int read() throws IOException
		{
			int chr;

			// No properties, pass everything directly
			if (properties == null)
				return in.read();

			// If the key stack has data, return that first
			if (key.length() > 0)
				return popKey();

			do
			{
				if ((chr = in.read()) == escape)
				{
					escaping = !escaping;

					if (escaping)
						continue;
				}

				if (chr != start || escaping)
				{
					escaping = false;
					return chr;
				}

				escaping = false;

				int more;
				while ((more = in.read()) != stop)
					key += (char)more;

				if (key.length() > 0)
					key = encodeQuotes(properties.getProperty(key, ""));
			}
			while (key.length() == 0);

			return popKey();
		}

		/**
		* Standard interface for reading a byte stream using a buffer.
		* @see InputStream
		**/
		public int read(byte[] buffer, int off, int len) throws IOException
		{
			// Same code as InputStream, implemented locally to restore
			// default behavior which may be modified by other streams.
			if (len <= 0)
				return 0;
			int chr = read();
			if (chr == -1)
				return -1;
			buffer[off] = (byte)chr;
			int count = 1;
			try
			{
				while (count < len)
				{
					chr = read();
					if (chr == -1)
						break;
					if (buffer != null)
						buffer[off + count] = (byte)chr;
					count++;
				}
			}
			catch (IOException e)
			{
			}
			return count;
		}

		protected String encodeQuotes(String val)
		{
			if (!encodeQuotes)
				return val;

			StringBuffer buf = new StringBuffer(val.length());
			for (int i = 0; i < val.length(); ++i)
			{
				char ch = val.charAt(i);
				if (ch == '"')
					buf.append("&quot");
				else
					buf.append(ch);
			}

			return buf.toString();
		}
	}

}
