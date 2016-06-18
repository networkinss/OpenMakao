package inss;

import java.util.LinkedHashSet;

class LinkedSetStr extends LinkedHashSet<String> {

	/** Class for overwriting toString() method,
	 * without leading and trailing [ ] .
	 *  Only for String types.
	 */
	private static final long serialVersionUID = 3150907968440436802L;

	public LinkedSetStr() {
		super();
	}



	//	public LinkedListStr(Collection arg0) {
//		super(arg0);
//		// TODO Auto-generated constructor stub
//	}
	@Override public String toString(){
		StringBuilder build = new StringBuilder(super.toString());
		if(build.length() > 1 ){
			build.deleteCharAt(0);
			build.deleteCharAt(build.length() -1 );
		}
		return build.toString();
	}

}
