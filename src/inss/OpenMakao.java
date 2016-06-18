package inss;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;

import org.apache.log4j.Logger;


public class OpenMakao implements IMakao, IParser {
	static org.apache.log4j.Logger log = Logger.getLogger(OpenMakao.class);  //TODO: work without, too
	final static String lf = System.getProperty("line.separator");
	private static String fileSep = System.getProperty("file.separator");
//	static int task = 0;
//	static String inputFile = null;
	static String outputFile = null;
	final static int DATAFROMOUTPUTFILE = 1;
	final static int DATAFROMSERVERMANAGER = 0;
	final static int ERROR = -1;
	static final String shortVersion = "0.90.01";
	static final String license = "OpenMakao Copyright (C) 2011 International Network Support & Service - Glas" + lf
		+ "This program comes with ABSOLUTELY NO WARRANTY." + lf 
        + "This is free software, and you are welcome to redistribute it under the conditions of the GPL license IF you are respecting human rights." + lf
        + "Otherwise you don't get a license and no permission to use this software." + lf 
        + "Both GPL license and human rights should come along with this software in a package." + lf
        + "If not, see for GPL at http://www.gnu.org/licenses/ and for human rights at http://www.un.org/en/documents/udhr/index.shtml." + lf
        + "Contact author at info@inss.ch or http://inss.ch.";
	final static String version = "OpenMakao version " + shortVersion + lf ;

	/* Constants for parameter parsing */
	private static final int VALFILEPATH = 1;
	private static final int VALFILEINPUT = 2;
	private static final int VALFILEOUTPUT = 3;
	private static final int VALFIELD = 4;
	private static final int VALERRORMSG = 5;
	private static final int VALDOSEND = 6;
	private static final int VALARGUELIST = 7;
	private static final int NONE = 0;

	/* tasks */
	private static final int TASKSENDNAGIOS = 1;
	private static final int TASKGENERATESERVICES = 3;
	private static final int TASKHELP = 4;
	private static final int TASKTESTEN = 6;
	private static final int TASKGENERATEINI = 7;
	private static final int TASKVERSION = 9;
	private static final int TASKHOWTO = 18;

	private static String log4jProps = "log4j.properties";
	private static IniManager iniMan = null;
	/** Defines the debug level.
	 *  1 = normal debug messages.
	 *  2 = more debug messages.
	 *  3 = incls printstacktrace.
	 */
	protected static int debug = 0;
	protected static boolean debugSendNagios = true;

//TODO next: message from task in error
//TODO make non-static
//TODO check if entry for autorestart exists but is not in the environment data.


	/**
	 */
	public static void main(String[] args) {

		boolean ok = false;
		/* find log4j propery file */
		if (! FileManager.existFile(log4jProps, (debug > 0))){
			if ( FileManager.existFile(".." + fileSep + log4jProps, (debug > 0))){
				log4jProps = ".." + fileSep + log4jProps;
			} else if(FileManager.existFile("." + fileSep + "conf" + fileSep + log4jProps, (debug > 0))){
				log4jProps = "." + fileSep + "conf" + fileSep + log4jProps;
			} else {
				System.out.println("No log4j property file found: " + log4jProps);
				return;
			}
		}
		NagiosMessenger.initLogging(log4jProps);
		if( debug > 0 ){
			log.debug("Log4jproperty: " + log4jProps);
		}

		if (ArgueParser.getTask() > 0){
			log.error("Task already running, exit this one.");
			return;
		}
		/* prepare parameter parsing */
		ArgueParser.setValue(VALFILEPATH, UNDEFINED);
		ArgueParser.setValue(VALFILEINPUT, UNDEFINED);
		ArgueParser.setValue(VALFILEOUTPUT, UNDEFINED);
		ArgueParser.setValue(VALFIELD, UNDEFINED);
		ArgueParser.setValue(VALERRORMSG, UNDEFINED);
		ArgueParser.setValue(VALDOSEND, "true");
		/* key = argument
		   * value = action[]
		   * action[0] = TASK
		   * action[1] = which value to fill
		   * action[2] = which second value to fill
		   * action[3] = which third value to fill //not used till now
		   * action[4] = where to get value: 0=false, 1=true, 2=next arg, 3=two next arg
		   *
		   * */
		ArgueParser.setAction("-help", new Integer[]{TASKHELP, NONE, NONE, NONE, NONE});
		ArgueParser.setAction("-h", new Integer[]{TASKHELP, NONE, NONE, NONE, NONE});
		ArgueParser.setAction("-n", new Integer[]{TASKSENDNAGIOS, VALFILEPATH, NONE, NONE, TYPENEXTARG});
		ArgueParser.setAction("-nosend", new Integer[]{NONE, VALDOSEND, NONE, NONE, TYPEFALSE});
		ArgueParser.setAction("-ini", new Integer[]{TASKGENERATEINI, NONE, NONE, NONE, NONE});
		ArgueParser.setAction("-i", new Integer[]{TASKGENERATEINI, NONE, NONE, NONE, NONE});
		ArgueParser.setAction("-howto", new Integer[]{TASKHOWTO, NONE, NONE, NONE, NONE});
		ArgueParser.setAction("-version", new Integer[]{TASKVERSION, NONE, NONE, NONE, NONE});
		ArgueParser.setAction("-v", new Integer[]{TASKVERSION, NONE, NONE, NONE, NONE});
		ArgueParser.setAction("-s", new Integer[]{TASKGENERATESERVICES, VALARGUELIST, NONE, NONE, TYPELIST});
		ArgueParser.setAction("-services", new Integer[]{TASKGENERATESERVICES, VALARGUELIST, NONE, NONE, TYPELIST});
		ArgueParser.setAction("-test", new Integer[]{TASKTESTEN, VALFILEPATH, NONE, NONE, TYPEOPTNEXTARG});
		//TODO work second argument as argulist

		ok = ArgueParser.parseArguments(args);
		if(ok == false){
			log.error(ArgueParser.getErrorMessage());
			return;
		}
		debugSendNagios = ArgueParser.getBooleanValue(VALDOSEND);

		/*************************/
		/* do all the task to do */
		/*************************/
		ok = false;
		switch (ArgueParser.getTask()){
			case TASKSENDNAGIOS: {
				log.info(version);
				AnalyseData an = new AnalyseData(debug);
				HashMap<String, String[]> nagiosMap = an.analyseData(ArgueParser.getValue(VALFILEPATH));
				HashMap<String, String> envMap = an.getEnvironment();

				debugSendNagios = an.isSendNagios();
				if(null != nagiosMap && null != envMap) {
					ok = sendNscaNagios(nagiosMap, envMap);
				}else {
					log.error("Analysing data failed.");
				}
				break;
			}
			
			case TASKGENERATESERVICES: {
				String[] a = ArgueParser.getValue(VALARGUELIST).split(",");
				LinkedHashSet<String> iniList = new LinkedHashSet<String>(Arrays.asList(a));
				String services = Util.generateServices(iniList);
				if(null != services) {
					ok = FileManager.storeContent("generatedservices.txt", services);
					System.out.println("Output stored in generatedservices.txt.");
					ok = true;
				}else {
					System.out.println("No section with [servicetemplate] found in ini files.");
					ok = false;
				}
				break;
			}
			case TASKHELP: {
				printHelp();
				ok = true;
				break;
			}
			case NONE: {
				printHelp();
				ok = true;
				break;
			}
			case TASKHOWTO: {
				printHowto();
				ok = true;
				break;
			}
			case TASKGENERATEINI: {
				String ini = "sample.ini";
				String content = buildIni();
				ok = FileManager.storeContent(ini, content);
				if ( ok ) {
					System.out.println("Saved new file at: " + FileManager.lf + FileManager.getPath(ini));
				}
				break;
			}
			case TASKVERSION:{
				System.out.println(version);
				System.out.println(license);
				ok = true;
				break;
			}
			default: {
				if(args.length > 0){
					log.error("Unknown argument: " + args[0]);
				}
			}
		}
		if (ok){
			Integer[] nolog = {TASKHELP, TASKHOWTO, TASKVERSION, NONE};
			if(Arrays.asList(nolog).contains(Integer.valueOf(ArgueParser.getTask())) == false){
//			if (ArgueParser.getTask() != TASKHELP && ArgueParser.getTask() != TASKHOWTO && ArgueParser.getTask() != TASKVERSION && ArgueParser.getTask() != NONE){
				log.info("Task completed successfully." + lf);
			}
		} else {
			log.error("Task not completed, error occured." + lf);
		}
//		if(null != inputFile  && debug == false && inputFile.startsWith("temp")){
//			FileManager.deleteFile(inputFile);
//		}
//		task = 0;
	}

	/**
	 * @param iniMan
	 * @param env
	 */
	private static boolean sendNscaNagios(HashMap<String, String[]> nagiosMap, HashMap<String, String> envMap) {
		NscaSend nsca = new NscaSend();
		String conf = "." + fileSep + "conf" + fileSep + "send_nsca.cfg";
		generateNSCAcfg(conf);
		nsca.setNscaConfigFile(conf);
		nsca.setNagiosHost(envMap.get(INIENVNAGIOSSERVER));
		nsca.setNagiosPort(envMap.get(INIENVNAGIOSPORT));
		nsca.setNagiosVirtualHost(envMap.get(INIENVHOST));
		//nsca.setNagiosServiceName("ClientAdmin");
		nsca.setUseShortHostname();
		log.debug("Start sending Nagios messages with nsca.");
		nsca.sendMap(nagiosMap);
		return nsca.allOK;
	}

	/** Reads data from dat file and exports into an ini file
	 *  with same name just ending with .ini instead of .dat.
	 * @param datapath
	 * @return outputpath
	 */
	private static String exportIni(String datapath){
			boolean result = false;
			iniMan = new IniManager(datapath);
			result = iniMan.initialize(false);
			if(iniMan.isRead == false || result == false) {
				log.error("Reading ini file failed.");
				return null;
			}
			result = iniMan.checkIniConfiguration();
			String outputPath = null;
			if(datapath.substring(datapath.length() -4, datapath.length()).equals(".dat")){
				outputPath =  datapath.substring(0,datapath.length() -4) + "_export.ini";
			}else{
				outputPath = datapath + "_export.ini";
			}
			result = result && iniMan.writeIniFromDat(outputPath);
			if(result) return outputPath;
			return null;
	}
	

	private static String buildIni(){
		StringBuilder buf = new StringBuilder();
		buf.append("# please replace every value in tags: <value>.").append(lf);
		buf.append("[environment]").append(lf);
		buf.append("host = <hostname of server>                               # mandatory: must be the same name as used in Nagios server.").append(lf);
		buf.append("nagiosserver = <hostname nagios server>                   # mandatory: hostname or ip adress of NSCA (Nagios) server").append(lf);
		buf.append("nagiosport = <nagios port>                                # mandatory: Port where Nagios NSCA is listening. Default is 5667.").append(lf);
//		buf.append("sendnagios = true                                         # Set to false if you want to get only console output without sending to Nagios.").append(lf);
		buf.append(lf).append("# Enable/disable each section. Insert name of ini section to activate action.").append(lf);
		buf.append("# If [checks] is deleted or empty, ALL other (check) sections will be active (if defined).").append(lf);
		buf.append("# If exists and not empty, sections will be enabled only if set to true. If not listed, sections are disabled.").append(lf);
		buf.append("[checks]").append(lf);
		buf.append("portcheck = true").append(lf);
		buf.append("logfile = true").append(lf);
		buf.append(lf);
		buf.append(lf).append("# Checks if a port is open.").append(lf);
		buf.append("# Put one line for service and portnumber for each portcheck. Host is as defined in environment section.").append(lf);
		buf.append("[portcheck]").append(lf);
		buf.append("service = <Nagiosservice1>                                # Name of Nagios service, must be unique.").append(lf);
		buf.append("portnr = <portnumber>                                     # port where the application listens-").append(lf);
		buf.append(lf);
		buf.append(lf).append("# Scans the logfile for defined string tokens.").append(lf);
		buf.append("# Put one line for service, path, errortokens and exceptiontokens for each logfile.").append(lf);
		buf.append("[logfile]").append(lf);
		buf.append("service = <Nagiosservice1>                                # Name of Nagios service, must be unique.").append(lf);
		buf.append("path = <logfile1>                                         # path to logfile.").append(lf);
		buf.append("errortokens = <token1>, <token2>, <token3>                # define with comma seperated the tokens that are indicating an error.").append(lf);
		buf.append("exceptiontokens = <exception1>,<exception2>               # define the exceptions that deny an error even if defined in errortokens.").append(lf);
		buf.append(lf);
		buf.append("service = <Nagiosservice2>                                # Another Nagios service. Name must be unique.").append(lf);
		buf.append("path = <logfile2>                                         # Another logfile ").append(lf);
		buf.append("errortokens = ").append(lf);
		buf.append("exceptiontokens = ").append(lf);
		
		buf.append(lf).append("# Here you can add additional non-OpenMakao Nagios service names to be included in the Nagios service cfg.").append(lf);
		buf.append("# After using you can delete this section.").append(lf);
		buf.append("[servicenames]").append(lf);
		buf.append("anyothernagiosservicename").append(lf);
		
		buf.append(lf).append("# this is template to generate the service entries for nagios. Use  -s <pathini> to generate nagios services with values from this ini file.").append(lf);
		buf.append("# After using you can delete this section.").append(lf);
		buf.append("[servicetemplate]").append(lf);
		buf.append("define service{").append(lf);
		buf.append("        host_name               <hostname will be replaced by Makao>").append(lf);
		buf.append("        use			generic_passive").append(lf);
		buf.append("        service_description     check_passive").append(lf);
		buf.append("        check_command           check_passive").append(lf);
		buf.append("        active_checks_enabled   0").append(lf);
		buf.append("        passive_checks_enabled  1").append(lf);
		buf.append("        check_freshness         1").append(lf);
		buf.append("        freshness_threshold     1200").append(lf);
		buf.append("        max_check_attempts      5").append(lf);
		buf.append("        normal_check_interval   5").append(lf);
		buf.append("        retry_check_interval    3").append(lf);
		buf.append("        check_period            24x7").append(lf);
		buf.append("        notification_interval   30").append(lf);
		buf.append("        notification_period     24x7").append(lf);
		buf.append("        notification_options    w,c,r,u").append(lf);
		buf.append("        contact_groups          <please fill out this>").append(lf);
		buf.append("        }").append(lf);
		return buf.toString();

	}
	
	/**
	 * @param file path of send_nsca.cfg
	 */
	private static void generateNSCAcfg(String file) {
		if(FileManager.existFolder("conf") == false) {
			FileManager.createFolder("conf");
		}
		if(FileManager.existFile(file, false) == false) {
			FileManager.storeContent(file, "encryption_method=0");
		}
	}

	/** help message.
	 *  TODO: check functions.
	 */
	private static void printHelp(){
		System.out.println(version);
		System.out.println("Usage parameter:");
		System.out.println("-h                                 prints this help message.");
		System.out.println("-howto                             prints a short how to configure Makao.");
		System.out.println("-n inipath                         send Nagios data according ini file in path.");
		System.out.println("-i                                 generates a sample ini file name sample.ini.");
		System.out.println("-s inipath inipath inipath ...     generates Nagios services from defined [servicetemplate] from inifiles in inipaths.");
		System.out.println("-version                           prints Makao version.");
	}
	private static void printHowto() {
		System.out.println("Short how to configure and run OpenMakao");
		System.out.println(version);
		System.out.println("More detailed documentation at ./docs/OpenMakao.pdf in this package." );
		System.out.println();
		System.out.println("Here the very quickone to get started:");
		System.out.println("1. Generate a sample ini file with parameter -i: \"java -jaropenmakao.jar -i\".");
		System.out.println("2. Fill out in environment section the mandatory Nagios parameter in sample.ini.");
		System.out.println("3. Setting the parameter \"sendnagios = false\" in environment section you can run OpenMakao even without Nagios.");
		System.out.println("4. Adjust the ini file according the needs and delete what you dont need.");
		System.out.println("5. Generate all Nagios services with service parameter: \"java -jaropenmakao.jar -s samplehostname.ini\".");
		System.out.println("6. Add services to Nagios configuration and restart it.");
		System.out.println("7. Run (schedule) OpenMakao with Nagios parameter -n: \"java -jar openmakao.jar -n samplehostname.ini\".");
		System.out.println();
		System.out.println("Requirements:");
		System.out.println("   1. Nagios server.");
		System.out.println("   2. NSCA configured at Nagios (passive checks).");
		System.out.println("   3. Java JRE 1.5 or higher.");
		System.out.println("   4. Common Nagios knowledge.");
		System.out.println("Implementation:");
		System.out.println("   1. Pluginfolder shall contain at least: a)openmakao.jar b) log4j-1.2.15.jar  c) log4j.properties");
		System.out.println("   2. In general you start OpenMakao with: java -jar openmakao.jar <parameter>");
		System.out.println("   3. Generate a sample ini file with: java -jar openmakao.jar -ini. You need one ini for each server.");
		System.out.println("   4. Rename created sample.ini into e.g. hostname.ini.");
		System.out.println("   5. Edit first section [environment] in ini file.");
		System.out.println("   6. Edit portcheck section.");
		System.out.println("       Every portcheck needs two lines: service and port number. Service is the name of the Nagios service.");
		System.out.println("   7. Edit logfile section if you want to monitor logfiles. Each file you want to monitor needs ");
		System.out.println("       to have four lines: service, path, errortokens and exceptiontokens.");
		System.out.println("       service = is the name of the Nagios service (casesensitive).");
		System.out.println("       path = the file path.");
		System.out.println("       Errortokens = and exceptiontokens = have a comma seperated list of tokens to scan for.");
		System.out.println("       Errortokens define the errors, exceptiontokens define where to skip found error.");
		System.out.println("Run");
		System.out.println("   1. Create a scheduled task to start OpenMakao regularly.");
		System.out.println("   2. Execute it with parameter -n inipath, e.g. \"java -jaropenmakao.jar -n windows.ini\".");
		System.out.println("   3. Makao will create a logfile in same directory (makaoplugin.log).");
		System.out.println("      You can edit the format of the log to your needs in the log4j.properties file. Look log4j documentation for details.");
		System.out.println("      The log4j.properties is configured to create a limited size of logfiles.");
		System.out.println("   4. Mako will create a subdirectory \"conf\" containing the file send_nsca.cfg");

	}
}
