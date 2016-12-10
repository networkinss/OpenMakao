# OpenMakao
OpenMakao - Logfile monitoring plugin for Nagios.
Short how to configure and run OpenMakao
OpenMakao version 0.90.01

More detailed documentation at ./docs/OpenMakao.pdf in this package.

Here the very quickone to get started:
1. Generate a sample ini file with parameter -i: "java -jaropenmakao.jar -i".
2. Fill out in environment section the mandatory Nagios parameter in sample.ini.
3. Setting the parameter "sendnagios = false" in environment section you can run OpenMakao even without Nagios.
4. Adjust the ini file according the needs and delete what you dont need.
5. Generate all Nagios services with service parameter: "java -jaropenmakao.jar -s samplehostname.ini".
6. Add services to Nagios configuration and restart it.
7. Run (schedule) OpenMakao with Nagios parameter -n: "java -jar openmakao.jar -n samplehostname.ini".

Requirements:
   1. Nagios server.
   2. NSCA configured at Nagios (passive checks).
   3. Java JRE 1.5 or higher.
   4. Common Nagios knowledge.
Implementation:
   1. Pluginfolder shall contain at least: a)openmakao.jar b) log4j-1.2.15.jar  c) log4j.properties
   2. In general you start OpenMakao with: java -jar openmakao.jar <parameter>
   3. Generate a sample ini file with: java -jar openmakao.jar -ini. You need one ini for each server.
   4. Rename created sample.ini into e.g. hostname.ini.
   5. Edit first section [environment] in ini file.
   6. Edit portcheck section.
       Every portcheck needs two lines: service and port number. Service is the name of the Nagios service.
   7. Edit logfile section if you want to monitor logfiles. Each file you want to monitor needs 
       to have four lines: service, path, errortokens and exceptiontokens.
       service = is the name of the Nagios service (casesensitive).
       path = the file path.
       Errortokens = and exceptiontokens = have a comma seperated list of tokens to scan for.
       Errortokens define the errors, exceptiontokens define where to skip found error.
Run
   1. Create a scheduled task to start OpenMakao regularly.
   2. Execute it with parameter -n inipath, e.g. "java -jaropenmakao.jar -n windows.ini".
   3. Makao will create a logfile in same directory (makaoplugin.log).
      You can edit the format of the log to your needs in the log4j.properties file. Look log4j documentation for details.
      The log4j.properties is configured to create a limited size of logfiles.
   4. Mako will create a subdirectory "conf" containing the file send_nsca.cfg
