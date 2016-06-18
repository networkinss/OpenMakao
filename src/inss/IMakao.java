package inss;

interface IMakao {


	/** = "0" */
	public final static String NAGOK = "0";
	/** = "1" */
	public final static String NAGWARNING = "1";
	/** = "2" */
	public final static String NAGCRITICAL = "2";
	/** = "3" */
	public final static String NAGUNKNOWN = "3";
    public final static String KEYLOGERRORNR = "keylogerrornr";
    public final static String KEYLOGLASTERRORLINE = "keyloglasterror";
    public final static String KEYLOGLASTLINENR = "keyloglastlinenr";

    public final static String KEYPORTCONNECT = "keyportconnect";
//    public final static String KEYLOGSERVICENAME = "nagiosservice";


	public final static String INIENV = "environment";
	public final static String INICHECKS = "checks";
	public final static String INISUMMARY = "summary";
	/** = logfile ini section for logfile definitions	 */
	public final static String INILOGFILE = "logfile";
	public final static String INIPORTCHECK = "portcheck";
	public final static String INISERVICENAMES = "servicenames";
	public final static String INISERVICETEMPLATE = "servicetemplate";
	/** array of all possible ini sections */
	public final static String[] INISECTIONS = {INIENV, INICHECKS, INISUMMARY, INIPORTCHECK, INILOGFILE, INISERVICENAMES,  INISERVICETEMPLATE};
	/* section parameter for name of Nagios service */
	/** entry for nagios service = "service" */
	public final static String INISERVICE = "service";
	/** = "port"	 */
	public final static String INIPORTNR = "portnr";
//	public final static String INIHOST = "hostname";
	/** = logpath  filesystem path to logfile to scan */
	public final static String INILOGPATH = "path";
	/** = errortoken    the key used in ini file, seperator is "," */
	public final static String INILOGERRORTOKEN = "errortokens";
	/** = exceptions    the key used in ini file, seperator is "," */
	public final static String INILOGEXCEPTIONS = "exceptiontokens";
	/** array of logfile keys */
	public final static String INIENVADMIN = "admin";
	public final static String INIENVADMINPWD = "adminpwd";
	public final static String INIENVHOST = "host";
	public final static String INIENVNAGIOSSERVER = "nagiosserver";
	public final static String INIENVNAGIOSPORT = "nagiosport";
	public final static String INIENVDEBUG = "debug";
	public final static String INIENVSENDNAGIOS = "sendnagios";
	/** = "servermanageroutput" */
	public final static String INIENVMANDATORY[] = {INIENVADMIN, INIENVADMINPWD,INIENVHOST,INIENVNAGIOSSERVER,INIENVNAGIOSPORT};
	public final static String INIENVALL[] = {INIENVADMIN, INIENVADMINPWD,INIENVHOST,INIENVNAGIOSPORT, INIENVDEBUG, INIENVSENDNAGIOS};


	/** = "section" */
	public final static String INISUMSECTION = "section";
	public final static String INISUMSERVICE = "service";
	public final static String INISUMTYPE = "type";

	/* */
	final static String cm = "#";


}