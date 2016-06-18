package inss;

import java.util.HashMap;

/** Parse arguments and return task and parameter values.
 * @author amrit
 *
 */
class ArgueParser implements IParser{

	private static int task = 0;

	private static HashMap<Integer, String> values = new HashMap<Integer, String>();
	private static HashMap<String, Integer[]> actionMap = new HashMap<String, Integer[]>();

	private static boolean ok = true;
	private static String errorMessage = "";


	static String getValue(int key){
		return values.get(key);
	}
	static boolean getBooleanValue(int key){
		String val = values.get(key);
		return Boolean.valueOf(val);
	}
	static void setValue(int key, String value){
		values.put(key, value);
	}
	static void setAction(String paramKey, Integer[] actions){
		if(actionMap.containsKey(paramKey)){
			System.out.print("actionMap contains key " + paramKey + " already.");
		}
		actionMap.put(paramKey, actions);
	}
	static int getTask(){
		return task;
	}
	static boolean parseArguments(String[] args) {
		final int FIELDTASK = 0;
		final int FIELDONE = 1;
		final int FIELDTWO = 2;
//		final int FIELDTHREE = 3;
		final int FIELDTYPE = 4;

		final int NONE = 0;

		final String TRUE = "true";
		final String FALSE = "false";

		/* key = argument
		   * value = action[]
		   * action[0] = TASK
		   * action[1] = which value to fill
		   * action[2] = which second value to fill
		   * action[3] = which third value to fill //not used till now
		   * action[4] = where to get value: 0=false, 1=true, 2=next arg, 3=two next arg
		   *
		   * */

	  String taskString = "";

//	  StringBuilder allOther = new StringBuilder();

	  /* PARSE PARAMETER */
	  for(int i=0;i<args.length;i++) {
//		  if(args[i].startsWith("$")) {
//			  allOther.append(args[i]).append(SEPSECTION);
//			  continue;
//		  }
		  /* ger arrays of actions and fields */
		  Integer[] in = actionMap.get(args[i]);
		  if(null == in) {
			  System.out.println("Unknown parameter: " + args[i]);
			  continue;
		  }
		  /* TASK DEFINITION */
		  if(in[FIELDTASK] > 0 && task == NONE) {
			  task = in[FIELDTASK];
			  taskString = args[i];
		  }else if(in[FIELDTASK] > 0){ //if task already defined
//			  if(task > maxindex) task = TASKUNKNOWN;
			  errorMessage = "Parameter " + args[i] + " not valid for the task " + taskString + ".";
			  ok = false;
			  return ok;
		  }
		  /* which value */
		  if(in[FIELDTYPE] > NONE) {							// if any
			  /* what value */
			  if(in[FIELDTYPE] == TYPEFALSE) {					//false
				  values.put(in[FIELDONE], FALSE) ;					//which value
			  }else if(in[FIELDTYPE] == TYPETRUE) {				//true
				  values.put(in[FIELDONE], TRUE);
			  }else if(in[FIELDTYPE] == TYPELIST){
				  /* put all following parameter into a comma seperated string */
				  StringBuilder build = new StringBuilder();
				  int n = 0;
				  for (n = i + 1; n < args.length; n++){
					  build.append(args[n]).append(",");
				  }
				  if( build.indexOf(",") > 0 ){
					  build.deleteCharAt(build.lastIndexOf(","));
				  }
				  values.put(in[FIELDONE], build.toString());
				  i = n;
			  } else if(in[FIELDTYPE] == TYPENEXTARG || in[FIELDTYPE] == TYPEOPTNEXTARG
					  || in[FIELDTYPE] == TYPETWONEXTARG || in[FIELDTYPE] == TYPEOPTTWONEXTARG) {		//value from next argument
				  /* CHECK FIRST */
				  i++;
				  if(i < args.length) {
					  String nextArg = args[i];
					  if(actionMap.containsKey(nextArg) == false){
						  if(UNDEFINED.equals(values.get(in[FIELDONE]))) { 
							  values.put(in[FIELDONE],  nextArg);
						  }else {
							  errorMessage = "Parameter for task " + taskString + " already defined.";
							  ok = false;
							  return ok;
						  }
					  }
				  } else if(in[FIELDTYPE] == TYPEOPTNEXTARG) {
					  	i--;
						continue;
				  } else {
					  errorMessage = "Missing value for task " + taskString + ".";
					  ok = false;
			  	}
				  //question if to check here, check for mandatory parameters will be later.
//				  else {
//					  if(in[FIELDTYPE] == TYPENEXTARG) {		//if next parameter is not optional
//						  System.out.println("Missing value for parameter " + args[i] + ".");
//						  values[in[FIELDONE]] = ERROR;
//						  error = true;
//					  }
//				  }
				  /* CHECK SECOND */
				  if(in[FIELDTYPE] == TYPETWONEXTARG || in[FIELDTYPE] == TYPEOPTTWONEXTARG){  		//second is optional
					  i++;
					  if(i < args.length) {
						  if(args[ i ].startsWith("-") == false){
//							  System.out.println(values);
//							  System.out.println(in[FIELDTWO]);
//							  System.out.println(values.get(in[FIELDTWO]));
							  if(UNDEFINED.equals(values.get(in[FIELDTWO]))) {   //TODO check this check.
//								  System.out.println(values);
								values.put(in[FIELDTWO], args[ i ]);
//								System.out.println(values);
							  }else {
								  errorMessage = "WARNING: parameter for task " + taskString + " defined twice.";
								  ok = false;
								  return ok;
							  }
						  }
					  }else if (in[FIELDTYPE] == TYPETWONEXTARG){
						  errorMessage = "Missing second value for " + taskString + ".";
						  ok = false;
						  return ok;
					  }
				  }
			  }
		  }
	  }
//	  values[VALTASKNR] = new String(new Integer(task).toString());
//	  if(task == 0) {
//		  System.out.println("No task defined.");
//		  error = true;
//	  }else { //check for parameter -p if valid or not.
//		  List<Integer> passTasks = Arrays.asList(new Integer[]{TASKENCRYPTFILE,TASKDECRYPTFILE, TASKENCRYPTSTRING, TASKDECRYPTSTRING, TASKPRINTINI});
//		  List<Integer> nodelTasks = Arrays.asList(new Integer[]{TASKREADINI});
//		  List<Integer> fileTasks = Arrays.asList(new Integer[]{TASKEXECUTION, TASKENCRYPTFILE, TASKDECRYPTFILE, TASKPRINTINI, TASKREADINI, TASKSAMPLE, TASKCKSUM, TASKCHECKINI});
//		  List<Integer> targetTasks = Arrays.asList(new Integer[]{TASKENCRYPTFILE, TASKDECRYPTFILE, TASKREADINI, TASKSAMPLE});
//		  if(null != values[VALPASS] && passTasks.contains(new Integer(task)) == false ) {
//			  System.out.println("Parameter -p is not valid for task " + taskList.get(task) + ".");
//			  error = true;
//		  }
//		  if(FALSE.equals(values[VALDELETEINI]) && nodelTasks.contains(new Integer(task)) == false ) {
//			  System.out.println("Parameter -nodelete is not valid for task " + taskList.get(task) + ".");
////			  error = true;
//		  }
//		  if(null != values[VALFILE] && fileTasks.contains(new Integer(task)) == false ) {
//			  System.out.println("Parameter -file is not valid for task " + taskList.get(task) + ".");
//			  error = true;
//		  }
//		  if(null != values[VALOPTTARGET] && targetTasks.contains(new Integer(task)) == false ) {
//			  System.out.println("Parameter -target is not valid for task " + taskList.get(task) + ".");
//			  error = true;
//		  }
//	  }
	  return ok;
	}
//	public static boolean isOk() {
//		return ok;
//	}
	public static String getErrorMessage() {
		return errorMessage;
	}

}
