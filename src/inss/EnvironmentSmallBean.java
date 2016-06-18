package inss;

import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * @author amrit
 *
 */
class EnvironmentSmallBean implements Serializable {

	private static final long serialVersionUID = -8537367877482599765L;
	/**
	 * 
	 */
	private LinkedHashMap<String, LinkedHashMap<String, String>> persistentDataMap = new LinkedHashMap<String, LinkedHashMap<String, String>>();

	/** Used to set single keys, too.
	 * @return
	 */
	public LinkedHashMap<String, LinkedHashMap<String, String>> getPersistentDataMap() {
		return persistentDataMap;
	}

	public void setPersistentDataMap(
			LinkedHashMap<String, LinkedHashMap<String, String>> persistentDataMap) {
		if(null != persistentDataMap){
			this.persistentDataMap = persistentDataMap;
		}
	}


	public String toString(){
		StringBuilder build = new StringBuilder();
		build.append("persistentDataMap size: ").append(null == this.persistentDataMap ? "null" : this.persistentDataMap.size()).append(FileManager.lf);
		return build.toString();
	}
}
