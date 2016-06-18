package inss;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * @author amrit
 * Storing all data of an environment.
 * Workflow:
 * 1. Command line (list comp, list task)
 *    define if task, component, server or parameter type.
 *    define component name for parameter commands
 * 2. Header line
 *    store header line
 * 3. Data line
 *    get CC_ALIAS for comp name (servicename, too)
 *    get CC_ALIAS for task as compobject name (base as servicename, too)
 *    get SBLSRVR_NAME for server TODO
 *    get component name from command line as key for map (handle servicename differently)
 * 4. Store data comp object into compMap with compname.tolowercase as key.
 * 5. Field CP_DISP_RUN_STATE is the Nagios description for components.
 */
class EnvironmentManager implements IMakao, Serializable {
   
	/**
	 *
	 */
	private static final long serialVersionUID = 5799945170601505585L;

	
    /** Environemt bean for holding the persistend data for this.  */
    private EnvironmentSmallBean envBean = new EnvironmentSmallBean();
    /**
     *  key = nagios servicename,
     *  value = array
     *  0 = Status 1 = Servicename 2 = description
     */
    private LinkedHashMap<String, String[]> nagiosResult = new LinkedHashMap<String, String[]>();
    /** csv export data in format:
     *  appserver	component	type	field	value
     */

    /** Persistent data like last logfile line or how many alerts already for one service.
     *  Will be stored in .dat file.
     *  Key  for the datamaps is the section name.
     */
//    private DataStorage storage = null;

//    private final String storagePath;
    /** Containing all logfile results, key is path to logfile.  */
    private HashMap<String, HashMap<String, String>> allFileresultsMap = new HashMap<String, HashMap<String, String>>();
    /** Map with results of portchecks. Keys are:
     *  key for containing map = service defined in ini file
     *  Containing map has keys:
	 *	KEYPORTCONNECT
	 *	INISERVICE
     *  */
    private HashMap<String, HashMap<String, String>> allPortcheckresultsMap = new HashMap<String, HashMap<String, String>>();

    private String curHost = null;
//    private int CCALIAS_Pos = 0;
//    private int SVNAME_Pos = 0;


	boolean isDatFileExisting;

    /* 0 = Status 1 = Servicename 2 = description */
    private final static int NAGIOSSTATUS = 0;
    private final static int NAGIOSSERVICENAME = 1;
    private final static int NAGIOSDESCRIPTION = 2;
    /** = "linenumber" */
    private final static String LINENUMBER = "linenumber";
    private final static String SIZE = "size";

    private final static String TRUE = "true";
	private final static String FALSE = "false";

    private static org.apache.log4j.Logger log = Logger.getLogger(EnvironmentManager.class);



	public void initialise(EnvironmentSmallBean envBean) {
		this.envBean = envBean;
	}


   
    
	/** Scans all logs defined
	 * @param allFilesMap
	 */
	protected void scanLogs(LinkedHashMap<String, HashMap<String, String>> allFilesMap) {
		Set<String> keyService = allFilesMap.keySet();
		Iterator<String> it = keyService.iterator();
//		String linenumber = "0";
		while(it.hasNext()) {
			String service = it.next();
			String id = service + LINENUMBER;
			String linenumber = this.getDataFromStorage(INILOGFILE, id);
			if(null == linenumber) {
				linenumber = "0";
			}
			String size = this.getDataFromStorage(INILOGFILE, service + SIZE);
			if(null == size ) {
				size = "0";
			}
			HashMap<String, String> map = allFilesMap.get(service);
			String[] exceptions = {};
			String[] errorToken = {};
			String error = map.get(INILOGERRORTOKEN);
			if(null != error) {
				errorToken = error.split(",");
			}
			String except = map.get(INILOGEXCEPTIONS);
			if(null != except) {
				exceptions = except.split(",");
			}

			String path = map.get(INILOGPATH);
			log.debug("Going to scan: " + path);
	    	if(null == service || "".equals(service)) {
	    		log.error("No service name defined for logfile " + path + ", please correct ini file.");
	    		continue;
	    	}
	    	String[] values = FileManager.scanFile(path, errorToken, exceptions, linenumber, size );
	    	final String ERRORRES = "error";
	    	HashMap<String, String> logfileResult = new HashMap<String, String>();
	    	if(null == values || ERRORRES.equals(values[3])) {
	    		log.error("Failed to scan file: " + path);
	    		log.error(values[1]);  			//error message
	    		logfileResult.put(KEYLOGLASTLINENR, ERRORRES);
		    	logfileResult.put(KEYLOGLASTERRORLINE, ERRORRES);
		    	logfileResult.put(KEYLOGERRORNR, ERRORRES);

	    	}else {
	    		Integer linenr = Integer.parseInt(linenumber);
	    		Integer lastline = Integer.parseInt(values[0]);
	    		int diff = lastline - linenr;
	    		log.info(diff + " new lines scanned.");
		    	logfileResult.put(KEYLOGLASTLINENR, values[0]);
		    	logfileResult.put(KEYLOGLASTERRORLINE, values[1]);
		    	logfileResult.put(KEYLOGERRORNR, values[2]);
	    	}
	    	if(null == values[0] ) values[0] = "0";
	    	this.allFileresultsMap.put(service, logfileResult);
	    	this.setDataForStorage(INILOGFILE, id, values[0]);
	    	long curSize = FileManager.getSize(path);
	    	this.setDataForStorage(INILOGFILE, service + SIZE, Long.valueOf(curSize).toString());
		}
	}
	/**
	 * @param allPortsMap
	 */
	public void checkPortConnection(LinkedHashMap<String, HashMap<String, String>> allPortsMap) {
		Set<String> keyService = allPortsMap.keySet();
//		Iterator<String> it = keyService.iterator();
		for (String service : keyService) {
			try{
//				String service = it.next();
				HashMap<String, String> map = allPortsMap.get(service);
//				String host = map.get(INIHOST);
//				if(null == host){
//					log.error("No value defined for host of service " + service);
//					continue;
//				}
				String port = map.get(INIPORTNR);
				if(null == port){
					log.error("No value defined for port of service " + service);
					continue;
				}
	//			String path = map.get(INILOGPATH);
		    	if(null == service || "".equals(service)) {
		    		log.error("No service name defined for portcheck " + service + ", please correct ini file.");
		    		continue;
		    	}
		    	HashMap<String, String> result = new HashMap<String, String>();
		    	/* now test the connection */
		    	TCPClient client = new TCPClient();
		    	int thePort = Integer.parseInt(port);
		    	if(null == this.curHost){
		    		log.error("Host is not defined for port check.");
		    		return;
		    	}
		    	Boolean getConnect = Boolean.valueOf( client.ConnectTo(this.curHost, thePort));
//		    	String id = service + host + port;
		    	result.put(KEYPORTCONNECT, getConnect.toString());
		    	result.put(INIPORTNR, port);
		    	this.allPortcheckresultsMap.put(service, result);
			}catch(Exception e){
				log.error(e);
			}
		}

	}
    /**
     * @return
     */
    public boolean convertLogScanNagios() {
    	boolean ok = true;
    	try {
    		Iterator<String> it = this.allFileresultsMap.keySet().iterator();
    		while(it.hasNext()) {
    			String service = it.next();
    			HashMap<String, String> logfileResult = this.allFileresultsMap.get(service);
		    	if(null == logfileResult) continue;
		    	/* 0 = Status 1 = Servicename 2 = description */
		    	String[] nagiosMessage = new String[3];
//		    	String service = logfileResult.get(iNILOGSERVICE);
		    	if(null == service) {
		    		log.error("No service defined for logfile.");
		    		continue;
		    	}
		    	String errnr = logfileResult.get(KEYLOGERRORNR);
		    	if(null == errnr ) errnr = "0";

		//    	int errcount = new Integer(errnr).intValue();
		    	if("error".equals(errnr)) {
		    		nagiosMessage[NAGIOSSTATUS] = NAGUNKNOWN;
		    		nagiosMessage[NAGIOSDESCRIPTION] = "Failed to scan logfile.";
		    	}else if(new Integer(errnr).intValue() > 0) {
		    		nagiosMessage[NAGIOSSTATUS] = NAGCRITICAL;
		    		nagiosMessage[NAGIOSDESCRIPTION] = errnr + " errors found. Last found " + logfileResult.get(KEYLOGLASTERRORLINE);
		    	}else {
		    		nagiosMessage[NAGIOSSTATUS] = NAGOK;
		    		nagiosMessage[NAGIOSDESCRIPTION] = "No new errors found in file.";
		    	}
		    	nagiosMessage[NAGIOSSERVICENAME] = service;
		    	this.nagiosResult.put(service, nagiosMessage);
    		}
    	}catch(Exception e) {
    		e.printStackTrace();
            log.error(e);
            ok = false;
    	}
    	return ok;
    }
    /**
     * @return
     */
    public boolean convertPortcheckNagios() {
    	boolean ok = true;
    	try {
    		Iterator<String> it = this.allPortcheckresultsMap.keySet().iterator();
    		while(it.hasNext()) {
    			String service = it.next();
    			HashMap<String, String> checkResultMap = this.allPortcheckresultsMap.get(service);
		    	if(null == checkResultMap) continue;
		    	/* 0 = Status 1 = Servicename 2 = description */
		    	String[] nagiosMessage = new String[3];
		    	String isOpen = checkResultMap.get(KEYPORTCONNECT);
		    	String portnr = checkResultMap.get(INIPORTNR);
		//    	int errcount = new Integer(errnr).intValue();
		    	if(TRUE.equals(isOpen)) {
		    		nagiosMessage[NAGIOSSTATUS] = NAGOK;
		    		nagiosMessage[NAGIOSDESCRIPTION] = "Port " + portnr + " is open.";
		    	}else if(FALSE.equals(isOpen)) {
		    		nagiosMessage[NAGIOSSTATUS] = NAGCRITICAL;
		    		nagiosMessage[NAGIOSDESCRIPTION] = "Connect to port " + portnr + " failed.";
		    	}else {
		    		nagiosMessage[NAGIOSSTATUS] = NAGUNKNOWN;
		    		nagiosMessage[NAGIOSDESCRIPTION] = "Status unknown: " + isOpen;
		    	}
		    	nagiosMessage[NAGIOSSERVICENAME] = service;
		    	this.nagiosResult.put(service, nagiosMessage);
    		}
    	}catch(Exception e) {
    		if(OpenMakao.debug >= 3){
    			e.printStackTrace();
    		}
            log.error(e);
            ok = false;
    	}
    	return ok;
    }

  

    /** Get how many times the data had been read and increase with +1.
     * @return
     */
    int manageCountRefresh(){
    	final String keyCounter = "countrefresh";
    	String val = this.getDataFromStorage(keyCounter, "number");
    	if(null == val) val = "0";
    	Integer result = Integer.parseInt(val);
    	/* increase counter */
    	val = Integer.valueOf(result.intValue() + 1).toString();
    	this.setDataForStorage(keyCounter, "number", val);
    	return result.intValue();
    }
    
    
    /** Get data from filesystem.
     * @param sectionName
     * @param dataKey
     * @return can be null
     */
    private String getDataFromStorage(String sectionName, String dataKey) {
      	String result = null;
      	if(null == this.envBean.getPersistentDataMap() ) return null;
    	LinkedHashMap<String, String> statusMap = this.envBean.getPersistentDataMap().get(sectionName);
    	if(null != statusMap) {
    		result = statusMap.get(dataKey);
    	}
    	return result;
    }
    /**
     * @param sectionName   as key for storage map for data map.
     * @param dataKey		as key for the data map.
     * @param dataValue		value for data map.
     * @return
     */
    private void setDataForStorage(String sectionName, String dataKey, String dataValue) {
//    	if(null == this.storage) {
//    		this.storage = FileManager.loadObject(this.storagePath, true);
//    	}
    	LinkedHashMap<String, String> dataMap = this.envBean.getPersistentDataMap().get(sectionName);
//    	if(null != this.storage) {
//    		dataMap = storage.getDataMap(sectionName);
//    	}
    	if(null == dataMap) {
    		dataMap = new LinkedHashMap<String, String>();
    	}
    	dataMap.put(dataKey, dataValue);
    	this.envBean.getPersistentDataMap().put(sectionName, dataMap);
    	return ;
    }


    

    /**
     * @param compName
     * @return
     */
//    public String getAllParameterComponent(String compName){
//        ComponentBean comp = this.compMap.get(compName);
//        if (null == comp){
//            return "Component not defined.";
//        }
//        return comp.getAllParameterAlias();
//    }



    public LinkedHashMap<String, String[]> getNagiosResult() {
        return nagiosResult;
    }
    public void setNagiosResult(LinkedHashMap<String, String[]> nagiosResult) {
        this.nagiosResult = nagiosResult;
    }
	public String getCurHost() {
		return curHost;
	}
	public void setCurHost(String curHost) {
		this.curHost = curHost;
	}

//	public HashMap<String, ComponentBean> getCompMap() {
//		return compMap;
//	}

//	public void setCompMap(HashMap<String, ComponentBean> compMap) {
//		this.compMap = compMap;
//	}

	public EnvironmentSmallBean getEnvBean() {
		return envBean;
	}



}
