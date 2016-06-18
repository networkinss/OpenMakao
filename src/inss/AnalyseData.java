package inss;

import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

class AnalyseData implements IMakao{

	/**
	 *
	 */
	private IniManager iniMan;
	private int debug;
	private boolean sendNagios = true;

	private static org.apache.log4j.Logger log = Logger.getLogger(AnalyseData.class);  

	/* Constructors */
	protected AnalyseData(int debugLevel){
		this.debug = debugLevel;
	}

	/**
	 * @return null if ini was not initialised.
	 */
	HashMap<String, String> getEnvironment(){
		if(null == this.iniMan) return null;
		HashMap<String, String> result = iniMan.getEnvironmentMap();
		return result;
	}

	/** Main method to analyse and convert into Nagios status
	 * @param iniPath
	 * @param delimiter
	 * @return
	 * TODO: refactor to extract methods
	 * TODO: no entry if list param for comp xyz but no rows returned.
	 */
	HashMap<String, String[]> analyseData(String iniPath){
		boolean ok = false;
		String tempfile = null;
		iniMan = new IniManager(iniPath);
		ok = iniMan.initialize(true);
		if(this.debug >= 3){
			log.debug(iniMan.getBean().toString());
		}
		if(iniMan.isRead == false ) return null;
		if (!ok ){
			log.error("Reading ini file failed.");
		}
		/* get new debug level from ini */
		this.debug = getIntFromString(iniMan.getEnvValue(INIENVDEBUG));
		OpenMakao.debug = this.debug;
		/* check ini configuration */
		boolean check = iniMan.checkIniConfiguration();
		if(check == false) {
			log.error("Inifile " + iniPath + " is not correctly configured.");
		}
		if(null != this.iniMan.getEnvValue(INIENVSENDNAGIOS)) {
			this.sendNagios = Boolean.valueOf(this.iniMan.getEnvValue(INIENVSENDNAGIOS)).booleanValue();
		}
		log.info("Debug level: " + debug);
		log.info("Will send to Nagios: " + sendNagios);
		if(debug == 0) {
			org.apache.log4j.Logger.getRootLogger().setLevel(Level.INFO);
		}else if(debug > 0) {
			org.apache.log4j.Logger.getRootLogger().setLevel(Level.DEBUG);
		}

		//TODO inputfile
		EnvironmentManager env = new EnvironmentManager();
		env.initialise(iniMan.getEnvBean());
		env.isDatFileExisting = iniMan.isDatExisting;
		env.setCurHost(iniMan.getEnvValue(INIENVHOST));
		int refreshCounter = env.manageCountRefresh(); //TODO
		log.debug("Refresh: " + refreshCounter + " times data read.");
//		if(this.debug >=4 ) {
//			log.debug("All components: " + env.getAllComponentKeys());
//			log.debug("All from datafile: " + iniMan.getBean().getComponentList());
//		}
		
		/* logfile scan */
		if(iniMan.checkFileScan()) {
			log.debug("Start analysing logfiles.");
//			HashMap<String, String> fileScanMap = ini.getFileScanMap();
			LinkedHashMap<String, HashMap<String, String>> allFilesMap = iniMan.getAllFileScanMap();
//			dubidu(env, allFilesMap);
			env.scanLogs(allFilesMap);
			env.convertLogScanNagios();
			log.debug("End analysing logfiles.");
		}
		/* port checks */
		if(iniMan.checkPorts()){
			log.debug("Starting portcheck.");
			LinkedHashMap<String, HashMap<String, String>> allPortsMap = iniMan.getAllPortcheckMap();
			env.checkPortConnection(allPortsMap);
			env.convertPortcheckNagios();
			log.debug("End portcheck.");
		}

		if(null != tempfile ) {
			FileManager.deleteFile(tempfile);
		}
		/* define all persistent data and store into environment bean */
//		env.lastAction(iniMan.getTaskMap().keySet());
		/* transfer all persistent status data to iniManager */
		iniMan.setEnvBean(env.getEnvBean());
		/* store data to filesystem.
		 * If called with an ini, it stores data into dat file with same name as ini,
		 *  just ending with .dat.
		 * If called with a dat file, it stores all data there. */
		String storePath = null;
		if( iniPath.substring(iniPath.length() -4, iniPath.length()).equalsIgnoreCase(".ini") ){
			storePath = iniPath.substring(0, iniPath.length() -4) + ".dat";
		}else {
			storePath = iniPath;
		}
		iniMan.setEnvBean(env.getEnvBean());
		ok = iniMan.storePersistentData(storePath);
		if(ok == false){
			log.error("Could not store persistent data into file " + storePath);
		}

		return env.getNagiosResult();
	}

	

	
	

	
	/**
	 *
	 */
	//TODO merge
	private int getIntFromString(String d) {
		int de = 0;
		if (null != d ){
			try {
				de = new Integer(d).intValue();
			}catch(Exception e){
				log.warn("Invalid value=" + d + ", only numbers are allowed.");
			}
		}
		return de;
	}
  

	public int getDebug() {
		return debug;
	}
	public void setDebug(int debug) {
		this.debug = debug;
	}
	public boolean isSendNagios() {
		return sendNagios;
	}
	public void setSendNagios(boolean sendNagios) {
		this.sendNagios = sendNagios;
	}
	public IniManager getIniMan() {
		return iniMan;
	}
	public void setIniMan(IniManager iniMan) {
		this.iniMan = iniMan;
	}
}
