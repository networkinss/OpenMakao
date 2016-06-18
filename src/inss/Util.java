package inss;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.log4j.Logger;

class Util implements IMakao{
	private static org.apache.log4j.Logger log = Logger.getLogger(Util.class);
	private static String lf = System.getProperty("line.separator");
	//private static String fs = System.getProperty("file.separator");


	
	
	/**
	 * @param pathFile Liste der pfade zu den inidateien
	 * @return
	 */
	static String generateServices(LinkedHashSet<String> iniListe){
		boolean ok = false;
		//StringBuilder hosts = new StringBuilder();
		HashMap<String, String> serviceMap = new HashMap<String, String>();
		Iterator<String> iniit = iniListe.iterator();
		LinkedHashSet<String> template = null;
		/* look into all provided ini files */
		while (iniit.hasNext()){
			String pathFile = iniit.next();
			log.debug("Analysing " + pathFile + ".");
			IniManager ini = new IniManager(pathFile);
			ini.setAllToLowerCase(false);
			ok = ini.initialize(false);
			if (ok == false){
				log.error("Could not analyse " + pathFile + ".");
				continue;
			}
			String host = ini.getEnvValue(INIENVHOST);
			LinkedHashSet<String> services =new LinkedHashSet<String>();
//			services.addAll(ini.getComponentRunList());
	
			/* add logfiles */
			if( ini.checkFileScan() ){
				LinkedList<String> logfiles = new LinkedList<String>(ini.getAllFileScanMap().keySet());
				services.addAll(logfiles);
			}
			/* add portchecks */
			if( ini.checkPorts() ){
				LinkedList<String> logfiles = new LinkedList<String>( ini.getAllPortcheckMap().keySet() );
				services.addAll(logfiles);
			}
			/* finally add also free defined names for Nagios services */
			LinkedHashSet<String> serviceNames = ini.getServiceNames();
			for(String serviceToAdd : serviceNames){
				services.add(serviceToAdd);
			}
			/* now add services to all other */
			Iterator<String> serviceit = services.iterator();
			while (serviceit.hasNext()){
				String key = serviceit.next();
				if (serviceMap.containsKey(key)){
					String hosts = serviceMap.get(key) + "," + host;
					serviceMap.put(key, hosts);
				} else {
					serviceMap.put(key, host);
				}
			}
			if (null == template || template.size() == 0){
				template = ini.getServicetemplate();
			}
		}
		if (null == template || template.size() == 0){
			log.error("No servicetemplate had been defined in any of given ini files.");
			return null;
		}
		/* after reading ini files, now put together the services */
		Set<String> keys = serviceMap.keySet();
		Iterator<String> itservice = keys.iterator();
		StringBuilder result = new StringBuilder();
		final String hostname = "host_name";
		final String servicedesc = "service_description";
		while (itservice.hasNext()){
			String servicename = itservice.next();
//			String hostlist = ; //comma seperated list of hosts
			String[] tempHosts = serviceMap.get(servicename).split(",");
			LinkedSetStr nodouble = new LinkedSetStr();
			for ( String host : tempHosts ){
				nodouble.add(host);
			}
			String hostlist = nodouble.toString();
			Iterator<String> itsercon = template.iterator();
			while (itsercon.hasNext()){
				String line = itsercon.next();
				int pos1 = line.indexOf(hostname);
				int pos2 = line.indexOf(servicedesc);
				if (pos1 >= 0){
					line = line.substring(0,pos1 + hostname.length()) + "\t\t" + hostlist;
				}
				if (pos2 >= 0 ){
					line = line.substring(0,pos2 + servicedesc.length()) + "\t" + servicename;
				}
				if((line.indexOf('{') >= 0 || line.indexOf('}') >= 0) == false) {
					result.append("\t");
				}
				result.append(line + lf);
			}
		}
		//FileManager.storeContent("generatedservices.txt", result.toString());
		return result.toString();
	}
	
	
	
}
