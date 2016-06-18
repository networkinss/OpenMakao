package inss;

import java.io.Serializable;

/**  Persistant data.
 * @author amrit
 *
 */
class DataStorage implements Serializable{

	private static final long serialVersionUID = 8994334133654854875L;
	/** Storing status data.
	 */
//	private LinkedHashMap<String, LinkedHashMap<String, String>> statusMap = null;

//	private LinkedHashMap<String, LinkedHashMap<String, String>> sectionMaps = new LinkedHashMap<String, LinkedHashMap<String, String>>();
//	Set<Map.Entry<String, Section>> set;
	private IniBean inibean = null;
	private EnvironmentSmallBean envbean = null;
	private String complIni = null;
//	private HashMap<String, ComponentBean> compStaticData = null;
	/** Constructor.
	 *
	 */
	DataStorage(){
//		statusMap = new LinkedHashMap<String, LinkedHashMap<String, String>>();
	}

	public IniBean getInibean() {
		return inibean;
	}

	public void setInibean(IniBean bean) {
		this.inibean = bean;
	}

	public String getComplIni() {
		return complIni;
	}

	public void setComplIni(String complIni) {
		this.complIni = complIni;
	}

	public EnvironmentSmallBean getEnvbean() {
		return envbean;
	}

	public void setEnvbean(EnvironmentSmallBean envbean) {
		this.envbean = envbean;
	}
	public String toString(){
		StringBuilder build = new StringBuilder();
//		build.append("statusMap: ").append(null == this.statusMap ? "null" : this.statusMap).append(FileManager.lf);
		build.append("iniBean: ").append(null == this.inibean ? "null" : this.inibean).append(FileManager.lf);
		build.append("envbean: ").append(null == this.envbean ? "null" : this.envbean).append(FileManager.lf);
		build.append("complIni length: ").append(null == this.complIni ? "null" : this.complIni.length()).append(FileManager.lf);
		return build.toString();
	}

}
