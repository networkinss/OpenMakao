package inss;


import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.ini4j.Config;
import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;

class IniManager implements IMakao{

	private static Logger log = Logger.getLogger(IniManager.class);
	private final static String lf = System.getProperty("line.separator");
//	private final static String[] environmentKeys={"admin","adminpwd","host","gateway","enterprise","appserver","nagiosserver","nagiosport","servermanageroutput", "servermanagerpath","debug"};
	/** Storing values over several executions
	 * for data from EnvironmentManager.  */
//	private LinkedHashMap<String, LinkedHashMap<String, String>> environmentStatusMap = new LinkedHashMap<String, LinkedHashMap<String, String>>();
	//TODO put environmentStatusMap into the bean.
	public boolean isRead = false;
	private boolean allToLowerCase = true;
	private boolean checkLogfile = true;
	private boolean checkPorts = true;

	boolean isDatExisting = true;
	private Ini ini = null;
	/** DataStorage contains also inibean
	 *
	 */
	private DataStorage storage = new DataStorage();
	/** IniBean contains data for ini file. */
	private IniBean inibean = new IniBean();
	/** Component data from stored persistend dat file.	 */
	private EnvironmentSmallBean envBean = new EnvironmentSmallBean();
//	private HashMap<String, ComponentBean> staticCompMap = null;
	private String iniPath = null;
	private boolean useDat = false;
	private boolean useChecks = true;


	/** Constructor
	 * @param iniPath
	 */
	protected IniManager(String path) {
		if(FileManager.checkFile(path)){
			this.iniPath = path;
			/* switch if using dat or ini file */
			useDat = iniPath.substring(iniPath.length() -4, iniPath.length()).equalsIgnoreCase(".dat");
		}else{
			log.error("Cannot read file " + path);
		}
	}
	/** Store all persistent data into a dat file.
	 * @param path
	 * @return
	 */
	boolean storePersistentData(String path){
		boolean ok = false;
		if (null == this.storage){
			this.storage = new DataStorage();
		}
		this.storage.setInibean(this.inibean);
		/* store static data from components like parameter */
		this.storage.setEnvbean(this.envBean);
		if(null == this.storage.getComplIni()){
			this.storage.setComplIni(this.toString());
		}
		if(useDat && path.endsWith(".dat") == false){
			log.warn("Not a dat file used for storing data.");
		}
		ok = FileManager.storeObject(path, this.storage, true);
		log.info("Stored data into file " + FileManager.getPath(path));
		return ok;
	}
	/** Check if Makao storage is from a prior version.
	 * @return
	 */
	private boolean checkSameVersion(){
		boolean isSameVersion = false;
		String oldVersion = this.inibean.getMakaoVersion();
		isSameVersion = oldVersion.equalsIgnoreCase(OpenMakao.shortVersion);
		return isSameVersion;
	}

	//command = pathPrintf + " \"%s\t%s\t%s\t%s\n\" \"" +  nagios.get("host") + "\" \"" + nagios.get("service") + "\" \"" + nagios.get("state") + "\" \"" + nagios.get("description") + "\" " + nsca;
    //	  $echo = `/usr/bin/printf "%s\t%s\t%s\t%s\n" "$host" "$kname" "$status" "$output" | /export/home/nagios/libexec/send_nsca $nagios -c /export/home/nagios/etc/send_nsca.cfg`;


	/** Filling all classfields with values from ini file.
	 * @return if ini had been read
	 */
	boolean initialize(boolean getPersistent){
		if(null == this.iniPath) return false;
		try {
			/* dat file */
			if(useDat){
				this.storage = FileManager.loadZipDataStorage(this.iniPath);
				if(null != storage){		//TODO: extract method.
					this.inibean = this.storage.getInibean();
					/* get static data for components like DfltTasks and MaxTasks */
					this.envBean = this.storage.getEnvbean();
					log.info("Loaded data from file " + FileManager.getPath(this.iniPath));
					if(checkSameVersion() == false){
						log.warn("Data are stored with version " + this.inibean.getMakaoVersion() + ", while you are running " + OpenMakao.shortVersion + "."
								+ lf + "To avoid problems you should recreate dat file.");
					}
					this.isRead = true;
				}else{
					log.error("Failed to read content of dat file: " + iniPath);

				}
			/* ini file */
			}else{
				this.ini = new Ini();
				Config con = new Config();
				con.setStrictOperator(false);
				con.setEmptyOption(true);
				con.setComment(true);
				con.setEmptySection(true);
				con.setEscape(false);
				con.setLowerCaseOption(false);
				con.setMultiOption(true);
//					con.setMultiSection(true);
//					con.setPathSeparator(System.getProperty("file.separator"));
				this.ini.setConfig(con);
				this.ini.load(FileManager.getTheFile(this.iniPath));
				if(getPersistent){
					String datFile = this.iniPath.substring(0, this.iniPath.length() - 4) + ".dat";
					this.storage = FileManager.loadZipDataStorage(datFile);		//TODO make this.storage local
					if(null != storage){
						this.inibean = this.storage.getInibean();
						this.envBean = this.storage.getEnvbean();
						log.info("Loaded data from file " + FileManager.getPath(datFile));
						if(checkSameVersion() == false){
							log.warn("Data are stored with version " + this.inibean.getMakaoVersion() + ", while you are running " + OpenMakao.shortVersion + "."
									+ lf + "To avoid problems you should recreate dat file.");
						}
					}else {
						log.debug("No .dat file available (" + FileManager.getPath(datFile));
						this.isDatExisting = false;
					}
				}
				this.isRead = true;
			}

		} catch (InvalidFileFormatException e) {
			this.isRead = false;
			e.printStackTrace();
		} catch (IOException e) {
			this.isRead = false;
			e.printStackTrace();
		}
		if(this.isRead == false) return false;
		this.inibean.setMakaoVersion(OpenMakao.shortVersion);
		/* if data coming from dat file all ok */
		if(this.useDat) return true;

		this.ini  = this.correctIni();

		inibean.setEnvMap( getASectionMap(INIENV) );
		if(useChecks == false) return true;

		/* must have data of checks before any other check */
		inibean.setCheckMap( this.getBoolSectionMap(INICHECKS) );



//		{
//			LinkedHashMap<String, String[]> map = this.createSummaryMapFromIni();
//			if(null != map && map.size() >= 1 ){
//				inibean.setSumMap(map);
//				this.checkSummary = true;
//			}else{
//				this.checkSummary = false;
//			}
//		}
		if(checkThat(INILOGFILE)) {
			inibean.setAllFileScanMap( this.getLogfileMapMap(INILOGFILE, INISERVICE, INILOGPATH) );
			this.checkLogfile = true;
		}else{
			this.checkLogfile = false;
		}
		if(checkThat(INIPORTCHECK)) {
			inibean.setAllPortcheckMap( this.getPortcheckMapMap(INIPORTCHECK, INISERVICE) );
			this.checkPorts = true;
		}else{
			this.checkPorts = false;
		}
		/* restart of components */
		/* other configurations */
		inibean.setServiceNames( this.getAsList(INISERVICENAMES) );
		inibean.setServicetemplate( this.getAsList(INISERVICETEMPLATE) );
//		log.debug("All sections: " + lf + this.getAllSections());
	    return this.isRead;
	}
	private LinkedHashMap<String, Boolean> getBoolSectionMap(String sectionName) {
		Section section = ini.get(sectionName);
		LinkedHashMap<String, Boolean> map = new LinkedHashMap<String, Boolean>();
		if(null != section) {
			Set<String> set = section.keySet();
			Iterator<String> it = set.iterator();
			while(it.hasNext()) {
				String key = it.next();
				String value = section.get(key);
				Boolean val = Boolean.valueOf(value);
				map.put(key, val);
			}
		}
		return map;
	}
		/** Check the ini entries.
		 * @return
		 * TODO
		 */
		public boolean checkIniConfiguration() {
			log.debug("Starting now configuration check.");
			int foundFlaws = 0;
		
			if(checkEnvironment() == false ) {
				foundFlaws++;
			}
	
			if(useDat){
				log.debug("Ending now configuration check for dat file.");
				if (foundFlaws == 0) return true;
				else return false;
			}
			/* check section names */
			for( String sectionName : ini.keySet() ) {
				boolean found = false;
				for (int i = 0; i < INISECTIONS.length;i++) {
					if(INISECTIONS[i].equalsIgnoreCase(sectionName)) {
						found = true;
					}
				}
				if(found == false ) {
					log.error(sectionName + " is not a valid section name.");
					foundFlaws++;
				}
			}
			/* check logfile section */
			Section section = ini.get(INILOGFILE);
			if(null != section) {
				int entries = section.length(INISERVICE);  // key in ini for bundle of lines
				int pathEntries = section.length(INILOGPATH);
				if(entries != pathEntries) {
					String er = "Every logfile entry needs values for both service (Nagios servicename) and path for the logfile." ;
					if(checkThat(INILOGFILE)) {
						foundFlaws++;
						er = er + lf  + "Please correct logfile section in ini, task is skipped.";
						log.error(er);
						this.checkLogfile = false;
					}else {
						log.info(er + lf + "Logfile section is not enabled, this is no error");
					}
				}
				if(this.checkEqualNumbers(section, INISERVICE) == false){
					foundFlaws++;
				}
			}
			/* check portcheck section */
			Section portsection = ini.get(INIPORTCHECK);
			if(null != portsection) {
				int entries = portsection.length(INISERVICE);  // key in ini for bundle of lines
	//			int hostEntries = portsection.length(INIHOST);
				int portEntries = portsection.length(INIPORTNR);
				if( entries != portEntries) {
					String er = "Every portcheck entry needs values for service (Nagios servicename), hostname and port for each portcheck." ;
					if(checkThat(INIPORTCHECK)) {
						foundFlaws++;
						er = er + lf  + "Please correct portcheck section in ini, task is skipped.";
						log.error(er);
						this.checkPorts = false;
					}else {
						log.info(er + lf + "Portcheck section is not enabled, this is no error");
					}
				}
	//			if(this.checkEqualNumbers(portsection, INISERVICE) == false){
	//				foundFlaws++;
	//			}
			}
			

			/* end section checks */
			if(foundFlaws == 0) {
				log.debug("Configuration is valid.");
			}else {
				if(foundFlaws == 1){
					log.error("Ini file has an invalid entry.");
				}else {
					log.error("Ini file has " + foundFlaws + " invalid entries.");
				}
			}
			return (foundFlaws == 0);
		}
	/** Avoid to get null
	 * @param sectionName
	 * @return
	 */
	private LinkedHashMap<String, String> getASectionMap(String sectionName) {
		Section section = ini.get(sectionName);
		LinkedHashMap<String, String> map = null;
		if(null != section) {
			map = new LinkedHashMap<String, String>(section);
		}else {
			map =  new LinkedHashMap<String, String>();
		}
		return map;
	}

	/**to get a map with maps of one section where the service name is the key and nagios service name.
	 *  Every entry (several lines) must contain a line with service= to have a key.
	 *  Seems its only useful for logfile section.
	 * @param sectionName name of section in ini file
	 * @param mainKey	look for main key (how many enries)
	 * @param minorKey  look for minor key and take that if more minor then major keys.
	 * @param dfltService name of service if not defined with key "service".
	 * @return
	 */
	private LinkedHashMap<String, HashMap<String, String>> getLogfileMapMap(String sectionName, String mainKey, String minorKey) {
		LinkedHashMap<String, HashMap<String, String>> result = new LinkedHashMap<String, HashMap<String, String>>();
		Section section = ini.get(sectionName);
		if(null == section) return result;
		int entries = section.length(mainKey);  // key in ini for bundle of lines
		int pathEntries = section.length(minorKey);
		if(entries != pathEntries) {
			log.error("No valid values for logfile section.");
			return result;
		}
		/* one run per bundle of lines (defined by service name). */
		for (int i = 0; i < entries; i++) {
			HashMap<String, String> map = new HashMap<String, String>();
			String service = null;
			try {
				service = section.get(INISERVICE, i);
				String path = section.get(INILOGPATH, i);
				String errorline = section.get(INILOGERRORTOKEN, i);
				String exceptionline = section.get(INILOGEXCEPTIONS, i);
				if(null == service || "".equals(service)) {
					log.error("No value defined for service in section logfile, please correct ini file.");
					continue;
				}
				if(null == path || "".equals(path)) {
					log.error("No value defined for path in logfile section.");
					continue;
				}
				map.put(INISERVICE, service);
				map.put(INILOGPATH, path);
				map.put(INILOGERRORTOKEN, errorline);
				map.put(INILOGEXCEPTIONS, exceptionline);
			}catch (Exception e) {
				log.warn("Error while processing service names from " + sectionName + ", bundle nr " + i + ", service = " + service + ".");
			}
			if(null == service) {
				log.error("No service defined for logfile.");
				service = "Unknown";
			}
			result.put(service, map);
		}
		return result;
	}
	/** Map with maps for each portcheck.
	 * @param sectionName
	 * @param mainKey
	 * @param minorKey
	 * @return
	 */
	private LinkedHashMap<String, HashMap<String, String>> getPortcheckMapMap(String sectionName, String mainKey) {
		LinkedHashMap<String, HashMap<String, String>> result = new LinkedHashMap<String, HashMap<String, String>>();
		Section section = ini.get(sectionName);
		if(null == section) return result;
		int entries = section.length(mainKey);  // key in ini for bundle of lines
		/* one run per bundle of lines (defined by service name). */
		for (int i = 0; i < entries; i++) {
			HashMap<String, String> map = new HashMap<String, String>();
			String service = null;
			try {
				service = section.get(INISERVICE, i);
//				String host = section.get(INIHOST, i);
				String errorline = section.get(INIPORTNR, i);
				if(null == service || "".equals(service)) {
					log.error("No value defined for service in section portcheck, please correct ini file.");
					continue;
				}
//				if(null == host || "".equals(host)) {
//					log.error("No value defined for host in portcheck section.");
//					continue;
//				}
				map.put(INISERVICE, service);
//				map.put(INIHOST, host);
				map.put(INIPORTNR, errorline);
			}catch (Exception e) {
				log.warn("Error while processing service names from " + sectionName + ", bundle nr " + i + ", service = " + service + ".");
			}
			if(null == service) {
				log.error("No service defined for logfile.");
				service = "Unknown";
			}
			result.put(service, map);
		}
		return result;
	}
	/** Get the summary section as map with service
	 *    as key and array as values
	 *    array: 0=service, 1=type
	 * @return empty map if no values are defined
	 */
//	private LinkedHashMap<String, String[]> createSummaryMapFromIni() {
//		LinkedHashMap<String, String[]> result = new LinkedHashMap<String, String[]>();
//		final int NRFIELDS = 3;
//		final int FIELDNRSERVICE = 0;
//		final int FIELDNRTYPE = 1;
//		/* add all from section tasks */
//		Section sectionSum = ini.get(INISUMMARY);
//		int nr = 0;
//		if(null != sectionSum) {
//			nr = sectionSum.length(INISUMSECTION);
//		}else{
//			return result;
//		}
//		for (int i = 0; i < nr; i++) {
//			String sumSection = sectionSum.get(INISUMSECTION, i);//sectionTasksnr.get(INITASKSNRTASK, i);
//			if("".equals(sumSection)) {
//				log.warn("Summary " + i + " is not correct defined, summary section is not valid.");
//				continue;
//			}
//			String[] sumNr = new String[NRFIELDS];
//			sumNr[FIELDNRSERVICE] = getIniString(sectionSum, INISUMSERVICE, i);
//			sumNr[FIELDNRTYPE] = getIniString(sectionSum, INISUMTYPE, i).toLowerCase();
//			if( null != sumNr[FIELDNRSERVICE] && null != sumNr[FIELDNRTYPE] ){
//				result.put(sumSection.toLowerCase(), sumNr);
//			}
//		}
//		return result;
//	}
	
	/** Eliminate all trailing comments in lines of ini
	 *
	 */
	private Ini correctIni() {
		Ini tempo = new Ini();
		Iterator<String> it = ini.keySet().iterator();
		while(it.hasNext()) {
			String keysec = it.next();
			Section section = ini.get(keysec);
			Section temposec = tempo.add(keysec);
			Iterator<String> itsec = section.keySet().iterator();
			/* Iterate the section */
			while(itsec.hasNext()) {
				String key = itsec.next();
				String value = null;
				int ent = section.length(key);
				for (int i = 0; i < ent; i++) {
					value = section.get(key, i);
					if(null == value || "".equals(value)) {
						key = this.removeComm(key);
					}else {
						value = this.removeComm(value);
					}
					temposec.add(key, value);
				}
			}
			tempo.put(keysec, temposec);
		}
		return tempo;
	}

	/** Removes the comment from the line.
	 * @param value
	 * @return
	 */
	private String removeComm(String value) {
		if(null == value) return "";
		int pos1, pos2 = 0;
		pos1 = value.indexOf(cm);
		if(pos1 >= 0) {
			value = value.substring(0, pos1);
		}
		pos2 = value.indexOf(";");
		if(pos2 >= 0) {
			value = value.substring(0, pos2);
		}
		value = value.trim();
		return value;
	}
	/**
	 * @param sect
	 * @param secname
	 * @param nr
	 * @return
	 */
	private Integer getIniInt(Section sect, String secname, int nr, int defaultNr) {
		Integer result = Integer.valueOf(defaultNr);
		try {
			if(sect.containsKey(secname)) {
				String value = sect.get(secname, nr);
				if(null != value && "".equals(value) == false) {
					result = new Integer(value);
				}
			}
		}catch(Exception e) {
			if(OpenMakao.debug >= 3){
				e.printStackTrace();
			}
//			log.debug(e);
			log.warn("Missing value in " + sect.getName() + " section, parameter: " + secname + ".");
		}
		return result;
	}




	/**
	 * @param section
	 * @param mainKey
	 * @return
	 */
	private boolean checkEqualNumbers(Section section, String mainKey) {
		boolean ok = true;
		if(null == section || null == mainKey ) return false;
		int numbers = section.length(mainKey);
		Iterator<String> it = section.keySet().iterator();
		while(it.hasNext()) {
			String key = it.next();
			int l = section.length(key);
			if(l != numbers && l != 0) {
				log.warn("[" + section.getName() + "] section has invalid number of entries for " + key + ".");
				ok = false;
			}
		}
		return ok;
	}


	
	
	public String getInputfileName(){
		if (this.isRead == false) return null;
		return this.getEnvValue("enterprise") + "_" + this.getEnvValue("appserver") + "_input.dat";
	}
	public String getOutputfileName(){
		if (this.isRead == false) return null;
		return this.getEnvValue("enterprise") + "_" + this.getEnvValue("appserver") + "_output.dat";
	}
	/**
	 * @return if filescan entries exist && if check is enabled
	 */
	public boolean checkFileScan() {
		/* check if and what defined in check section */
		boolean ok =  checkThat(INILOGFILE) && this.checkLogfile;
		if(ok) {
			if(null == inibean.getAllFileScanMap() || inibean.getAllFileScanMap().size() < 1) {
				log.warn("Not enough values defined for logfile section, will skip task.");
				return false;
			}
//			for (int i = 0;i < LOGFILEMANDATORY.length;i++) {
//				if(! this.fileScanMap.containsKey(LOGFILEMANDATORY[i])) {
//					log.warn("Missing value for logfile section: " + LOGFILEMANDATORY[i]);
//					return false;
//				}
//				if(null == this.fileScanMap.get(LOGFILEMANDATORY[i]) || "".equals(this.fileScanMap.get(LOGFILEMANDATORY[i]))) {
//					log.warn("Missing value for logfile section: " + LOGFILEMANDATORY[i]);
//					return false;
//				}
//			}
		}
		return ok;
	}
	/** Check if all mandatory keys for environment exist.
	 * @return
	 */
	private boolean checkEnvironment() {
		boolean ok = true;
		if(null == inibean.getEnvMap() || inibean.getEnvMap().size() < INIENVMANDATORY.length) return false;
		for (int i = 0;i < INIENVMANDATORY.length;i++) {
			ok = ok && inibean.getEnvMap().containsKey(INIENVMANDATORY[i]);
			if(ok && null != inibean.getEnvMap().get(INIENVMANDATORY[i]) && "".equals(inibean.getEnvMap().get(INIENVMANDATORY[i])) == false) {
				ok = ok && true;
			}else {
				ok = false;
				log.error("Missing values for " + INIENVMANDATORY[i] + " in ini file.");
				break;
			}
		}
		List<String> allEntries = Arrays.asList(INIENVALL);
		for ( String entry : inibean.getEnvMap().keySet() ){
			if ( allEntries.contains( entry ) == false ){
				ok = false;
				log.error("Unknown entry: " + entry + " in environment section. Please check ini file.");
			}
		}
		return ok;
	}
	public boolean checkPorts() {
		boolean check =  true;
		if( checkThat(INIPORTCHECK) ) {
			/* check minimum of mandatory entries */
			check = null != inibean.getAllPortcheckMap() && inibean.getAllPortcheckMap().size() >= 1 && this.checkPorts;
			if( check == false) {
				log.warn("Section " + INIPORTCHECK + " is marked to be check, but does not have entries.");
			}
		}else check = false;
		return check;
	}

	private LinkedHashSet<String> getAsList(String section) {
		LinkedHashSet<String> liste = null;
		if(this.ini.containsKey(section)) {
			liste = new LinkedHashSet<String>(ini.get(section).keySet());
		}else {
			liste = new LinkedHashSet<String>();
		}
		return liste;
	}

	/** if checks is defined and has data, check if section is enabled.
	 * @return true if no checks or checks has no data. False if section has data but this section not.
	 */
	private boolean checkThat(String section) {
		if(null != inibean.getCheckMap() && inibean.getCheckMap().size() > 0) {
			Boolean oko = inibean.getCheckMap().get(section);
			if (null == oko) oko = Boolean.valueOf(false);  //if no entry it is disabled, if not other defined.
			if(oko.booleanValue() == false) {
				log.info("Section " + section + " disabled in [checks], skipping.");
			}
			return oko.booleanValue();
		}
		return true;
	}
	
	protected boolean writeIniFromDat(String path) {
		boolean ok = false;
		String content = this.storage.getComplIni();
		if( null == content ) return false;
		ok = FileManager.storeContent(path, content);
		return ok;
	}

	/**
	 * @return
	 */
	public String toString() {
		StringBuilder build = new StringBuilder();
		try {
//			ini.store(FileManager.getTheFile(path));
			LinkedList<String> keyList = null;
			if(null == ini){
				log.error("Could not retrieve data from ini file, maybe you used the data file.");
				return "";
			}else{
				keyList = new LinkedList<String>(ini.keySet());
			}
			Iterator<String> it = keyList.iterator();
			/* section loop */
			while(it.hasNext()) {
				String key = it.next();
				Section section = ini.get(key);
				Set<String> aset = section.keySet();
				Iterator<String> itsec = aset.iterator();
				build.append("[").append(section.getName()).append("]").append(lf);
				int maxlines = 0;
				/* check max entries loop */
				while(itsec.hasNext()) {
					String keysec = itsec.next();
//					String value = "";
					int len = section.length(keysec);
					if(len > maxlines) {
						maxlines = len;
					}
				}
				/* loop the values */
				for (int i = 0; i < maxlines ; i++) {
					itsec = aset.iterator();
					while(itsec.hasNext()) {
						String keysec = itsec.next();
						String value = "";
						if(section.length(keysec) > i) {
							value = section.get(keysec, i);
							if(null == value) {
								value = "";
							}else {
								value = " = " + value;
							}
						}
//						log.debug("keysec = " + keysec + ", value = " + value);
						build.append(keysec).append(value).append(lf);
					}
					build.append(lf);
				}
				build.append(lf);
			}
//			log.debug("CONTENT:" + lf + build.toString());
		} catch (Exception e) {
			if(OpenMakao.debug >= 3){
				e.printStackTrace();
			}else{
				log.error(e);
			}
		}
		return build.toString();
	}


	public void setEnvMap(LinkedHashMap<String, String> envMap) {
		inibean.setEnvMap(envMap);
	}
	public LinkedHashSet<String> getComponentList() {
		return inibean.getComponentList();
	}
	public void setComponentList(LinkedHashSet<String> componentList) {
		inibean.setComponentList( componentList );
	}




	
	
	/**
	 * @return
	 */
	public LinkedHashMap<String, LinkedHashMap<String, String>> getAllSectionsMap(){
		LinkedHashMap<String, LinkedHashMap<String, String>> result = new LinkedHashMap<String, LinkedHashMap<String, String>>();
		Set<Map.Entry<String, Section>> set = this.ini.entrySet();
		for(Map.Entry<String, Section> key : set){
			LinkedHashMap<String, String> kindOfSection = new LinkedHashMap<String, String>();
			String sectionName = key.getKey();
			Section section = key.getValue();
			Set<Map.Entry<String, String>> sectionSet = section.entrySet();
			for(Map.Entry<String, String> sectionKey : sectionSet){
				String lastKey = sectionKey.getKey();
				String lastValue = sectionKey.getValue();
				kindOfSection.put(lastKey, lastValue);
			}
			result.put(sectionName, kindOfSection);
		}
		return result;
	}
	

	public LinkedHashSet<String> getComponentRunList() {
		return inibean.getComponentRunList();
	}
	public void setComponentRunList(LinkedHashSet<String> componentRunList) {
		inibean.setComponentRunList( componentRunList );
	}

	/**
	 * @param key
	 * @return null if not existing.
	 */
	public String getEnvValue(String key){
		if (null == inibean.getEnvMap()){
			log.error("Error trying get env value: ini file was not read." );
			return null;
		}
		String value = inibean.getEnvMap().get(key);
		return value;
	}
	public LinkedHashMap<String, String> getEnvironmentMap(){
		if (null == inibean.getEnvMap()){
			log.error("Error trying get env map: ini data missing." );
			return null;
		}
		LinkedHashMap<String, String> mapResult = this.inibean.getEnvMap();
		return mapResult;
	}
	public HashMap<String, Integer[]> getTaskMap(){
		return inibean.getTaskMap();
	}

	public LinkedHashSet<String> getServiceNames() {
		return inibean.getServiceNames();
	}
	public LinkedHashSet<String> getServicetemplate() {
		return inibean.getServicetemplate();
	}
//	public String getData(String mapKey, String dataKey) {
//		LinkedHashMap<String, String> resultMap = this.environmentStatusMap.get(mapKey);
//		if(null == resultMap) {
//			return null;
//		}
//		String result = resultMap.get(dataKey);
//		return result;
//	}

//	public LinkedHashMap<String, String> getDataMap(String mapKey) {
//		return this.environmentStatusMap.get(mapKey);
//	}
//
//	public void setDataMap(String key, LinkedHashMap<String, String> data) {
//		this.environmentStatusMap.put(key, data);
//	}

//	public LinkedList<String> getTaskList() {
//		LinkedList<String> result = new LinkedList<String>();
//		Iterator<String> it = this.taskList.keySet().iterator();
//		while(it.hasNext()) {
//			String task = it.next();
//			if(INITASKSNRCRITICAL.equals(task)  ||
//					INITASKSNRWARNING.equals(task)) continue;
//			result.add(task);
//		}
//		return result;
//	}
//	public LinkedList<String> getTaskList() {
//		return taskList;
//	}
//	public void setTaskList(LinkedList<String> taskList) {
//		this.taskList = taskList;
//	}
//	public HashMap<String, String> getFileScanMap() {
//		return fileScanMap;
//	}
//
//	public void setFileScanMap(LinkedHashMap<String, String> fileScanMap) {
//		this.fileScanMap = fileScanMap;
//	}
	public boolean isRead() {
		return isRead;
	}
	public Ini getIni() {
		return ini;
	}
	public LinkedHashMap<String, String> getAddStatusCompMap() {
		return inibean.getAddStatusCompMap();
	}
	public void setAddStatusCompMap(LinkedHashMap<String, String> addStatusCompMap) {
		inibean.setAddStatusCompMap( addStatusCompMap );
	}
	public LinkedHashMap<String, String> getAddStatusCompRunMap() {
		return inibean.getAddStatusCompRunMap();
	}
	public void setAddStatusCompRunMap(
			LinkedHashMap<String, String> addStatusCompRunMap) {
		inibean.setAddStatusCompRunMap( addStatusCompRunMap );
	}
	public LinkedHashMap<String, String> getAddStatusTasksMap() {
		return inibean.getAddStatusTasksMap();
	}
	public void setAddStatusTasksMap(LinkedHashMap<String, String> addStatusTasksMap) {
		inibean.setAddStatusTasksMap( addStatusTasksMap );
	}
	public boolean isAllToLowerCase() {
		return allToLowerCase;
	}
	public void setAllToLowerCase(boolean allToLowerCase) {
		this.allToLowerCase = allToLowerCase;
	}


	public LinkedHashSet<String> getAddStatusTasksRunningList() {
		return inibean.getAddStatusTasksRunningList();
	}


	public LinkedHashMap<String, HashMap<String, String>> getAllFileScanMap() {
		return inibean.getAllFileScanMap();
	}


//	public void setAllFileScanMap(
//			LinkedHashMap<String, HashMap<String, String>> allFileScanMap) {
//		this.allFileScanMap = allFileScanMap;
//	}


	public LinkedHashSet<String> getRestartList() {
		return inibean.getRestartList();
	}


	public void setRestartList(LinkedHashSet<String> restartList) {
		inibean.setRestartList( restartList );
	}


	public LinkedHashMap<String, HashMap<String, String>> getAllPortcheckMap() {
		return inibean.getAllPortcheckMap();
	}


	public void setIni(Ini ini) {
		this.ini = ini;
	}


	public IniBean getBean() {
		return inibean;
	}


	public void setBean(IniBean bean) {
		this.inibean = bean;
	}




//	public LinkedHashMap<String, LinkedHashMap<String, String>> getEnvironmentStatusMap() {
//		return bean.getEnvironmentStatusMap();
//	}
//
//
//	public void setEnvironmentStatusMap(
//		LinkedHashMap<String, LinkedHashMap<String, String>> environmentStatusMap) {
//		bean.setEnvironmentStatusMap( environmentStatusMap );
//	}
	/** Persistent data for EnvironmentManager.
	 * @return
	 */
	public EnvironmentSmallBean getEnvBean() {
		return envBean;
	}
	public void setEnvBean(EnvironmentSmallBean envBean) {
		this.envBean = envBean;
	}
	public boolean isUseChecks() {
		return useChecks;
	}
	public void setUseChecks(boolean useChecks) {
		this.useChecks = useChecks;
	}

}