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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import org.apache.log4j.Logger;

//import org.apache.log4j.helpers.LogLog;

/**
 * Java native equivalent to nsca_send.c
 *
 * It's main purpose is to manage encryption (if needed), and the protocol over the wire to the nsca server.  It has
 * no dependencies on log4j, so can be used independent of it (log4j).
 *
 * Example usage:
 *
 * <p>
 * <pre>
 *		try
 *		{
 *			Nsca nsca = new Nsca();
 *			nsca.setConfigFile("conf/nsca_send_clear.cfg");
 *			nsca.send_nsca("localhost", "5667", "production", "MyApplication", "something bad just happened", 1, 0);
 *			System.out.println("send complete!!");
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
 **/
public class Nsca {

	private static final int TRANSMITTED_IV_SIZE = 128;	/* Initialization Vector size */

	// The supported encryption methods:
	protected static final int ENCRYPT_NONE = 0;
	protected static final int ENCRYPT_XOR = 1;

	/**
	 * Nagios configuration file ... it tells us what encryption model to use
	 */
	private String config_file = null;	/* NSCA config file name: usually something like /usr/local/nagios/nsca-2.4/send_nsca.cfg */
	private int encryption_method = ENCRYPT_NONE;	/* default Encryption Method */
	private String nsca_password = null;	/* NSCA password (optional and only used with XOR) */

	/**
	 * Nagios return codes
	 * NOTE: the DO_NOT_SEND message is for appender use only, it is not a nagios type
	 */
	public static final int DO_NOT_SEND     = -1;
	public static final int NAGIOS_OK       = 0;
	public static final int NAGIOS_WARN     = 1;
	public static final int NAGIOS_CRITICAL = 2;
	public static final int NAGIOS_UNKNOWN  = 3;

	private int nsca_version = 3;
	
//	static org.apache.log4j.Logger log = Logger.getLogger(Nsca.class);

	/**
	 * A flag to indicate configuration status
	 */
	private boolean configured = false;

	/**
	 * A flag to indicate that everything is ready to execute append()-commands
	 */
	private boolean ready = false;

	/**
	 * Constructor
	 */
	public Nsca() {
		super();
	}

	/**
	 * Sets path of nagios config file
	 *
	 * @param value
	 *            The new Url value
	 */
	public void setConfigFile(String value) throws Exception {
		if (value == null) { return; }

		value = value.trim();

		if (value.length() == 0) { return; }

		config_file = value;

		configure();
	}

	/**
	 * Sets the encryption method (instead of reading it from the config file)
	 *
	 * @param value
	 *            The new encryption method (0=None, 1=XOR)
	 */
	public void setEncryptionMethod(int value) {
		if (value < 0) { return; }

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
	 * Returns true, when we are ready to write
	 * messages to the nsca server, else false.
	 *
	 * @return Description of the Returned Value
	 */
	public boolean ready() throws Exception {
		if (ready) { return true; }

		if (!configured) {
			if (!configure()) { return false; }
		}

		ready = true;

		return ready;
	}

	/**
	 * Internal method. Configures for appending...
	 *
	 * @return Boolean specifying whether configuration succeeded
	 */
	protected boolean configure() throws Exception {
		BufferedReader in;
		try {
			if (configured) { return true; }

			if (config_file == null) {
				// No config-file specified - assumes the default encryption method and no password.
				return true;
			}
			in = new BufferedReader(new FileReader(config_file));

		} catch (FileNotFoundException e) {
			return false;
		} catch (Exception e) {
			throw new Exception("send_nsca.configure() - File open error: " + config_file);
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
			throw new Exception("send_nsca.configure() - I/O error: " + config_file);
		}

		try {
			in.close();
		} catch (IOException e) {
			throw new Exception("send_nsca.configure() - Cannot close file: " + config_file);
		}

		configured = true;

		return true;
	}

	/**
	 * Encrypts the send buffer according the nsca encryption method
	 *
	 * @param buffer
	 *            Buffer to be encrypted
	 * @param iv
	 *            Encryption Initialization Vector
	 * @throws
	 *            Exception if encryption scheme is not supported
	 */
	public void encrypt_buffer(int encryption_method, byte[] buffer, byte[] server_iv) throws Exception
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
			throw new Exception("NagiosAppender::encrypt_buffer(): unsupported encryption method: " + encryption_method);
		}
	}

	/**
	* This is the call that will push the alert out over the wire to the nagios server
	*
	* @param host host where nagios server is running
	* @param reportingHost host that is where the alert originated
	* @param reportingService name of the nagios servie the alert is for
	* @param message string message associated with the alert
	* @param return_code one of NAGIOS_UNKNOWN, NAGIOS_OK, NAGIOS_WARN, or NAGIOS_CRITICAl
	*/
	public void send_nsca(String host,
				String port,
				String reportingHost,
				String reportingService,
				String message,
				int return_code /* ,
				int encryption_method */) throws Exception
	{
		if (null == message) return;
		System.out.println("Sending for host " + reportingHost + " the service " + reportingService);
		Socket s = null;
		try
		{
			while( true )
			{
				int portAsInt = 0;
				try
				{
					portAsInt = Integer.parseInt(port);
				} catch (NumberFormatException  ee)
				{
					throw new Exception("NscaSend.send_nsca(): port was malformed: " + port);
				}
				s = new Socket( host, portAsInt );
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

			// Set up the host that the push is initiated from.
			//
			byte[] host_name = new byte[64];
			temp = (null == reportingHost) ? "UNKNOWN" : reportingHost;
			System.arraycopy(temp.getBytes(),0,host_name,0,temp.getBytes().length);

			// Set up our service name.
			//
			byte[] service_name = new byte[128];
			temp = (null == reportingService) ? "UNKNOWN" : reportingService;
			System.arraycopy(temp.getBytes(),0,service_name,0,temp.getBytes().length);

			// Set up the free text message.
			//
			byte[] plugin_output = new byte[512];

			// nsca doesn't handle line feeds very well ..... let's remove them
			message.replaceAll("\n", "<linefeed>"); //TODO check assignment

			if ((null != message) && (message.getBytes().length <= 512))
			{
				System.arraycopy(message.getBytes(),0,plugin_output,0,message.getBytes().length);
			}
			else if (null != message)
			{
				System.arraycopy(message.getBytes(),0,plugin_output,0,plugin_output.length);
			}
			else
			{
				System.arraycopy("<null>".getBytes(),0,plugin_output,0,plugin_output.length);
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
			encrypt_buffer(encryption_method, alert, received_iv);
			// write to the socket
			System.out.println("Write now to socket; " + reportingHost + " the service " + reportingService);
			out.write(alert,0,alert.length);
			out.flush();

			// clean up
			out.close();
			in.close();
			s.close();

		}
		catch (Exception e)
		{
			if (null != s)
			{
				try {s.close(); } catch (Exception ee) {}
			}
			throw e;
		}
	}

//	public static void main(String[] args)
//	{
//		System.out.println("creating instance of Nsca ...");
//		try
//		{
//			Nsca nsca = new Nsca();
//			nsca.setConfigFile("conf/nsca_send_clear.cfg");
//			nsca.send_nsca("localhost", "5667", "production", "MyApplication", "something bad just happened", 1, 0);
//			System.out.println("send complete!!");
//		} catch (Exception e)
//		{
//			System.out.println("exception thrown ... " + e.getMessage());
//		}
//	}
}
