package inss;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.*;
import org.apache.log4j.helpers.*;
//import org.apache.log4j.nagios.*;


/**
 * Title:
 * Description:
 * @author jlyons
 * @version 1.0
 */

class NagiosMessenger implements IMakao{

   // private static final Category trace = Category.getInstance(NagiosAppenderTest.class.getName());
	private static final Logger trace = Logger.getLogger(NagiosMessenger.class.getName());

    private static boolean log4jInitialized = false;

    //static {
      // org.apache.log4j.MDC.put("instance", "eCommunicationsObjMgrdeu");
     //  org.apache.log4j.MDC.put("nagios_host_name",    "ubuntu");
       //org.apache.log4j.MDC.put("nagios_service_name", "SERVICE_MDC");
       //org.apache.log4j.MDC.put("nagios_host_name",    "HOST_MDC");
  // }

    public NagiosMessenger(String log4jFile) {
        //final String sFile = "./conf/log4j.xml";
        initLogging(log4jFile);

        if (!log4jInitialized)
            LogLog.error("[NagiosAppender],  Logging initialization error.  Config file=" + log4jFile + " -- Aborting.");
    }
    /*
    public NagiosMessenger() {
        //final String sFile = "./conf/log4j.xml";
        NagiosAppender app = new NagiosAppender();
        app.setHost("nagios");
        app.setName("NAGIOS");
        app.setPort("5667");
        try {
            //org.apache.log4j.MDC;
            log4jInitialized = true;
          } catch (Exception ex) {
            LogLog.error("[NagiosAppender], [initLogging], Error initializing logging!", ex);
          }
        log4jInitialized = true;
        if (!log4jInitialized)
            LogLog.error("[NagiosAppender],  Logging initialization error.  Config file=" + " -- Aborting.");
    }
    */
    public static String sendToNagios(HashMap<String, String[]> nagiosMap, String hostName){
    	String result = null;
    	Set<String> s = nagiosMap.keySet();
    	Iterator<String> it = s.iterator();
    	while (it.hasNext()){
    		String comp = it.next();
    		String[] message = nagiosMap.get(comp);
    		//LogLog.warn("message:" + message[0] + ", " + message[1] + ", " + message[2]);
    	    org.apache.log4j.MDC.put("instance", comp);
    	    org.apache.log4j.MDC.put("nagios_host_name", hostName);

    	    if (NAGOK.equals(message[0])){
    	    	trace.info(message[2]);
    	    } else if (NAGWARNING.equals(message[0])){
    	    	trace.warn(message[2]);
    	    } else if (NAGCRITICAL.equals(message[0])){
    	    	trace.error(message[2]);
    	    } else {
    	    	trace.debug(message[2]);  //TODO: object must be string
    	    }
    	}
    	return result;
    }
    /*
    private void exerciseAllLogLevels()
    {

        trace.debug("10000 Here's a DEBUG message");
        trace.info( "10001 Here's an INFO message ... this message is always associated with 10003, so I can ignore it");
        //trace.warn( "10002 Here's a WARN message ...  this message means our stock price just plummeted");
        //trace.error("10003 Here's an ERROR message ... this message is really bad ... I want to know about it");
        //trace.fatal("10004 Here's a FATAL message ... this message  means our stock just reached a new low, so I can go home early today ...", new Exception("Exception message"));
    }
	*/
    public static void initLogging(final String configFile) {
        if (!log4jInitialized) {
            if (configFile.indexOf("xml") > -1)
            {
              try {
                org.apache.log4j.xml.DOMConfigurator.configure(configFile);
                log4jInitialized = true;
              } catch (Exception ex) {
                LogLog.error("[NagiosAppender], [initLogging], Error initializing logging!", ex);
              }
            } else {
              try {
                org.apache.log4j.PropertyConfigurator.configureAndWatch(configFile);
                log4jInitialized = true;
              } catch (Exception ex) {
                LogLog.error("[NagiosAppender], [initLogging], Error initializing logging!", ex);
              }
            }
        }
    }
    /*
    private static void usage()
    {
       System.out.println("usage: NagiosAppender <configfile>");
    }

    public static class HelloServlet extends HttpServlet {

        public void init()
          throws ServletException
        {
        	// make this the first call in this method
           org.apache.log4j.MDC.put("nagios_service_name", "HelloService");
        }

        public void doGet (HttpServletRequest request,
                           HttpServletResponse response)
          throws ServletException, IOException
        {
           // make this the first call in this method
           org.apache.log4j.MDC.put("nagios_service_name", "HelloService");
        }

        public void doPut (HttpServletRequest request,
                           HttpServletResponse response)
          throws ServletException, IOException
        {
           // make this the first call in this method
           org.apache.log4j.MDC.put("nagios_service_name", "HelloService");

        }
   }
*/
}