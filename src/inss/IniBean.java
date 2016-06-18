package inss;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * @author amrit
 *
 */
class IniBean implements Serializable{
	/** version of bean, if new bean, regenerate dat file	 */  //TODO
	private static final long serialVersionUID = 2453323152901242453L;
	private String makaoVersion = "";
	private LinkedHashMap<String, String> envMap = new LinkedHashMap<String, String>();
	private LinkedHashMap<String, Boolean> checkMap = new LinkedHashMap<String, Boolean>();
	/**
	 * 
	 */
	private LinkedHashMap<String, String[]> sumMap = new LinkedHashMap<String, String[]>();
//	private LinkedHashMap<String, String> fileScanMap = new LinkedHashMap<String, String>();
	/** map of all files to scan */
	private LinkedHashMap<String, HashMap<String, String>> allFileScanMap = new LinkedHashMap<String, HashMap<String, String>>();
	/** map of all ports to check */
	private LinkedHashMap<String, HashMap<String, String>> allPortcheckMap = new LinkedHashMap<String, HashMap<String, String>>();
	private LinkedHashMap<String, String> addStatusCompMap = new LinkedHashMap<String, String>();
	private LinkedHashMap<String, String> addStatusCompRunMap = new LinkedHashMap<String, String>();
	private LinkedHashMap<String, String> addStatusTasksMap = new LinkedHashMap<String, String>();
	private LinkedHashSet<String> addStatusTasksRunningList = new LinkedHashSet<String>();
	private LinkedHashSet<String> componentList = new LinkedHashSet<String>();
	private LinkedHashSet<String> componentRunList = new LinkedHashSet<String>();
//	private LinkedList<String> taskList = new LinkedList<String>();
	/**
	 * 	final int NRFIELDS = 6;
		final int FIELDNRCRITICAL = 0;
		final int FIELDNRWARNING = 1;
		final int FIELDNRALERTS = 2;
		final int FIELDMINNRWARNING = 3;
		final int FIELDMINNRCRITICAL = 4;
		final int FIELDTIMEOUT = 5;
	 */
	private LinkedHashMap<String, Integer[]> taskMap = new LinkedHashMap<String, Integer[]>();
	private LinkedHashSet<String> restartList = new LinkedHashSet<String>();
	private LinkedHashSet<String> serviceNames = new LinkedHashSet<String>();
	private LinkedHashSet<String> servicetemplate = new LinkedHashSet<String>();
	/** Persistent data like linenumber from logfile and other stuff.	 **/
	private LinkedHashMap<String, LinkedHashMap<String, String>> environmentStatusMap = new LinkedHashMap<String, LinkedHashMap<String, String>>();

	public LinkedHashMap<String, String> getAddStatusCompMap() {
		return addStatusCompMap;
	}
	public void setAddStatusCompMap(LinkedHashMap<String, String> addStatusCompMap) {
		this.addStatusCompMap = addStatusCompMap;
	}
	public LinkedHashMap<String, String> getAddStatusCompRunMap() {
		return addStatusCompRunMap;
	}
	public void setAddStatusCompRunMap(
			LinkedHashMap<String, String> addStatusCompRunMap) {
		this.addStatusCompRunMap = addStatusCompRunMap;
	}
	public LinkedHashMap<String, String> getAddStatusTasksMap() {
		return addStatusTasksMap;
	}
	public void setAddStatusTasksMap(LinkedHashMap<String, String> addStatusTasksMap) {
		this.addStatusTasksMap = addStatusTasksMap;
	}
	public LinkedHashSet<String> getAddStatusTasksRunningList() {
		return addStatusTasksRunningList;
	}
	public void setAddStatusTasksRunningList(LinkedHashSet<String> addStatusTasksRunningList ) {
		this.addStatusTasksRunningList = addStatusTasksRunningList;
	}
	public LinkedHashMap<String, HashMap<String, String>> getAllFileScanMap() {
		return allFileScanMap;
	}
	public void setAllFileScanMap(
			LinkedHashMap<String, HashMap<String, String>> allFileScanMap) {
		this.allFileScanMap = allFileScanMap;
	}
	public LinkedHashMap<String, HashMap<String, String>> getAllPortcheckMap() {
		return allPortcheckMap;
	}
	public void setAllPortcheckMap(
			LinkedHashMap<String, HashMap<String, String>> allPortcheckMap) {
		this.allPortcheckMap = allPortcheckMap;
	}
	public LinkedHashMap<String, Boolean> getCheckMap() {
		return checkMap;
	}
	public void setCheckMap(LinkedHashMap<String, Boolean> checkMap) {
		this.checkMap = checkMap;
	}
	public LinkedHashSet<String> getComponentList() {
		return componentList;
	}
	public void setComponentList(LinkedHashSet<String> componentList) {
		this.componentList = componentList;
	}
//	public void addComponentList(LinkedHashSet<String> componentList) {
//		this.componentList.addAll(componentList);
//	}
	public LinkedHashSet<String> getComponentRunList() {
		return componentRunList;
	}
	public void setComponentRunList(LinkedHashSet<String> componentRunList) {
		this.componentRunList = componentRunList;
	}
//	public void addComponentRunList(LinkedHashSet<String> componentRunList) {
//		this.componentRunList.addAll(componentRunList);
//	}
	public LinkedHashMap<String, String> getEnvMap() {
		return envMap;
	}
	public void setEnvMap(LinkedHashMap<String, String> envMap) {
		this.envMap = envMap;
	}
	public LinkedHashSet<String> getRestartList() {
		return restartList;
	}
	public void setRestartList(LinkedHashSet<String> restartList) {
		this.restartList = restartList;
	}
	public LinkedHashSet<String> getServiceNames() {
		return serviceNames;
	}
	public void setServiceNames(LinkedHashSet<String> serviceNames) {
		this.serviceNames = serviceNames;
	}
	public LinkedHashSet<String> getServicetemplate() {
		return servicetemplate;
	}
	public void setServicetemplate(LinkedHashSet<String> servicetemplate) {
		this.servicetemplate = servicetemplate;
	}
	public String toString(){
		StringBuilder build = new StringBuilder();
		build.append("envMap: ").append(this.envMap.size()).append(FileManager.lf);
		build.append("checkMap: ").append(this.checkMap.size()).append(FileManager.lf);
		build.append("sumMap: ").append(this.sumMap.size()).append(FileManager.lf);
		build.append("allFileScanMap: ").append(this.allFileScanMap.size()).append(FileManager.lf);
		build.append("allPortcheckMap: ").append(this.allPortcheckMap.size()).append(FileManager.lf);
		build.append("addStatusCompMap: ").append(this.addStatusCompMap.size()).append(FileManager.lf);
		build.append("addStatusCompRunMap: ").append(this.addStatusCompRunMap.size()).append(FileManager.lf);
		build.append("addStatusTasksMap: ").append(this.addStatusTasksMap.size()).append(FileManager.lf);
		build.append("addStatusTasksRunningList: ").append(this.addStatusTasksRunningList.size()).append(FileManager.lf);
		build.append("componentList: ").append(this.componentList.size()).append(FileManager.lf);
		build.append("componentRunList: ").append(this.componentRunList.size()).append(FileManager.lf);
		build.append("taskMap: ").append(this.taskMap.size()).append(FileManager.lf);
		build.append("restartList: ").append(this.restartList.size()).append(FileManager.lf);
		build.append("serviceNames: ").append(this.serviceNames.size()).append(FileManager.lf);
		build.append("servicetemplate: ").append(this.servicetemplate.size()).append(FileManager.lf);
		build.append("makaVersion: ").append(this.makaoVersion).append(FileManager.lf);
		return build.toString();
	}
	public LinkedHashMap<String, LinkedHashMap<String, String>> getEnvironmentStatusMap() {
		return environmentStatusMap;
	}
	public void setEnvironmentStatusMap(
			LinkedHashMap<String, LinkedHashMap<String, String>> environmentStatusMap) {
		this.environmentStatusMap = environmentStatusMap;
	}
	public LinkedHashMap<String, Integer[]> getTaskMap() {
		return this.taskMap;
	}
	public void setTaskMap(LinkedHashMap<String, Integer[]> taskMap) {
		this.taskMap = taskMap;
	}
	public String getMakaoVersion() {
		return makaoVersion;
	}
	public void setMakaoVersion(String makaoVersion) {
		this.makaoVersion = makaoVersion;
	}
	public LinkedHashMap<String, String[]> getSumMap() {
		return sumMap;
	}
	public void setSumMap(LinkedHashMap<String, String[]> sumMap) {
		this.sumMap = sumMap;
	}
}
