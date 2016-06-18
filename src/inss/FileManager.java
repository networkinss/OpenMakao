package inss;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;


class FileManager {
	//private static String header = null;
	//private static boolean debug = false;
	private static org.apache.log4j.Logger log = Logger.getLogger(FileManager.class);
	final static String lf = System.getProperty("line.separator");
	final static String fileSep = System.getProperty("file.separator");

	
	/**
	 * simple constructor
	 */
	FileManager(){

	}
	

	
	
	
	
	
	
	


	
	
	
	
	

	/** Export to a file
	 * It's just storing the content of the string into the given file
	 *  overwriting existing
	 * @param pathFile
	 * @param content
	 * @return
	 */
	public static boolean storeContent(String pathFile, String content) {
		boolean ok = false;
		BufferedWriter writer;
		if( null == content ) return false;
		try {
			writer = new BufferedWriter(new FileWriter(pathFile));
			writer.write(content);
			writer.newLine();
			writer.flush();
			writer.close();
			ok = true;
			log.debug("Stored content into file: " + pathFile);
		} catch (IOException e) {
			log.error("storeContent: ", e);
			ok = false;
		}
		return ok;
	}
	
	/**
	 * @param path
	 * @param storage
	 * @return
	 */
	static boolean storeObject(String path, Object storage, boolean zipIt) {
//		Store store = new Store(dataMap);
		boolean result = false;
		try {
//			ByteArrayOutputStream out = new ByteArrayOutputStream();
//			ObjectOutputStream    oos  = new ObjectOutputStream( out );
//			oos.writeObject(storage);
//			oos.close();
			ObjectOutputStream oos = null;

			/* write data */
			BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(path));
			if(zipIt){
				OutputStream zipout = new GZIPOutputStream( fos );
				oos = new ObjectOutputStream(zipout);
			}else{
				oos = new ObjectOutputStream(fos);
			}
//			fos.write(out.toByteArray());
			oos.writeObject(storage);
			oos.flush();
			oos.close();
			fos.flush();
			fos.close();
			oos = null;

			result = true;
		} catch (IOException ioe) {
			log.error(ioe);
			if(OpenMakao.debug >= 3){
				ioe.printStackTrace();
			}
			result = false;
		} catch (Exception e) {
			log.error(e);
			if(OpenMakao.debug >= 3){
				e.printStackTrace();
			}
			result = false;
		}
		log.debug("Stored object into file: " + path);
		return result;

	}
	/** Reading a zipped DataStorage object from filesystem.
	 *  Returns null if not readable.
	 * @param path
	 * @return null if error
	 */
	static DataStorage loadZipDataStorage(String path) {
		DataStorage storage = null;
		try {
			ByteArrayInputStream bais = loadZipObject(path);
			if(null == bais) return null;
			storage = (DataStorage) new ObjectInputStream( bais ).readObject();
		} catch(Exception e) {
			log.error(e);
			return null;
		}
		return storage;
	}
	
	/** Get bytestream from object in file system.
	 * @param path
	 * @return null if error
	 */
	private static ByteArrayInputStream loadZipObject(String path) {
		ByteArrayInputStream bais = null;
		InputStream zipout = null;
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		ByteArrayOutputStream bos = null;
		try {
			boolean ok = checkFile(path);
			if (ok == false) {
				log.debug("File not readable: " + getPath(path));
				return null;
			}
			fis = new FileInputStream(path);
			 bis = new BufferedInputStream(fis);
			/* handle zip */
			zipout = new GZIPInputStream( bis );
			bos = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int c = 0;
			while((c = zipout.read(buf)) != -1) {
				bos.write(buf,0,c);
			}
			bais = new ByteArrayInputStream( bos.toByteArray());
			
		}
		catch(IOException io) {
			log.error(io);
//			io.printStackTrace();
			return null;
		} catch(Exception e) {
			log.error(e);
			return null;
		}finally{
			try{
				bais.close();
				bos.close();
				zipout.close();
				bis.close();
				fis.close();
			}catch(IOException e){
				log.error(e);
			}
		}
		return bais;
	}
	/** Check if file
	 * a. exists
	 * b. is a file and not a directory
	 * c. is readable
	 * @param path
	 * @param f
	 * @return true or false
	 */
	public static boolean checkFile(String path) {
		boolean ok = true;
		File f = null;
		if (null == path) {
			System.out.println("No path defined.");  //no log
			return false;
		} else {
			f = new File(path);
		}
		if (!(f.exists() && f.canRead() )) {
//			log.warn("Could not read file: " + f.getAbsolutePath());
			return false;
		}
		if(f.isDirectory()) return false;
		return ok;
	}
	/**
	 * @param pathFile
	 * @return
	 */
	public static boolean deleteFile(String pathFile) {
		boolean ok = false;
		File f;
		try {
			f = new File(pathFile);
			ok = f.delete();
		} catch (Exception e) {
			log.error("storeContent: ", e);
			ok = false;
		}
		return ok;
	}
	/** Simple check if a file exists and is not a folder.
	 * @param pathFile
	 * @param showAbsolute
	 * @return
	 */
	public static boolean existFile(String pathFile, boolean showAbsolute){

		File f = new File(pathFile);
		if (showAbsolute){
			System.out.println(f.getAbsolutePath());
		}
		if(f.exists() && f.isFile()) return true;
		return false;
	}
	public static boolean existFolder(String pathFile){
		File f = new File(pathFile);
		if(f.exists() && f.isDirectory()) return true;
		return false;
	}
	public static boolean createFolder(String folderpath) {
		boolean ok = false;
		File f = new File(folderpath);
		if(f.exists() && f.isDirectory()) return false;
		try {
			ok = f.mkdir();
		}catch(Exception e) {
			log.error(e);
		}
		return ok;
	}
	public static File getTheFile(String inipath) {
		File f = null;
		if (existFile(inipath, false)) {
			f = new File(inipath);
		}else {
			log.error("File " + inipath + " does not exist.");
		}
		return f;
	}

	/** Simply returns absolute path of file.
	 * @param pathFile
	 * @return
	 */
	public static String getPath(String pathFile){
		return new File(pathFile).getAbsolutePath();
	}
	/**
	 * @param pathFile
	 * @return size of file in bytes.
	 */
	public static long getSize(String pathFile) {
		long size = 0;
		if(checkFile(pathFile)) {
			File f = new File(pathFile);
			size = f.length();
		}else return 0;
		return size;
	}
	/** Scans a file for tokens defined in toFind
	 *  the file is scanned to the end, and counts error found.
	 *  Last error token and line are in the result array.
	 * @param String pathFile
	 * @param String[] toFind
	 * @param String[] exceptions
	 * @param String lineNumber
	 * @return String[] = {lineNumber, line, nr error lines}
	 */
	public static String[] scanFile(String pathFile, String[] toFind, String[] exceptions, String lineNumber, String size){
		String[] result = new String[4];
		result[0] = lineNumber;				//last line number (file position)
		result[1] = "line undefined";		//String of line from last error
		result[2] = "";						//Number of errors found in scanloop
		result[3] = "error";				//status of scan process: ok=success, error=failed to scan
		int pos = new Integer(lineNumber).intValue();
		long lastSize = new Long(size).longValue();
		long currentSize = getSize(pathFile);
		/* assume new file */
		if(lastSize > currentSize) {
			pos = 0;
		}
		int count = 0;
		//convert all to lower case
		for(int i=0;i<toFind.length;i++){
			toFind[i] = toFind[i].toLowerCase();
		}
		if(null == exceptions) exceptions = new String[] {};
		for(int i=0;i<exceptions.length;i++){
			exceptions[i] = exceptions[i].toLowerCase();
		}
		FileReader f;
		LineNumberReader reader;
		try{
			f = new FileReader(pathFile);
			reader = new LineNumberReader(f);
//			if (reader.getLineNumber() < pos){
//				log.debug("Linenumber too high: " + pos);
//				pos = 0;
//			}
			reader.setLineNumber(0);
			int countLines = 0;
			while(reader.ready()){
				String origLine = reader.readLine();
				countLines++;
				if(countLines <= pos) continue;
				String line = origLine.toLowerCase();
				int i = 0;
				for (i=0;i<toFind.length;i++){
					if(line.indexOf(toFind[i]) >= 0){			//found token
						boolean isException = false;
						for(int n = 0;n < exceptions.length;n++) {
							if(line.indexOf(exceptions[n]) >= 0 && "".equals(exceptions[n]) == false) {
								isException = true;
								break;
							}
						}
						if(isException == false) {
							result[1] = toFind[i] + " at line " + reader.getLineNumber() + ": " + origLine;
							count++;
							break;
						}
					}
				}
			}
			result[0] = Integer.valueOf(reader.getLineNumber()).toString();
			result[3] = "ok"; //scan successfull
			reader.close();
			f.close();
		}catch(FileNotFoundException fnfe){
			result[1] = "Didn't find file: " + pathFile;
			log.error(result[1]);
			return result;
		}catch(IOException ioe){
			result[1] = ioe.toString();
			log.error(ioe);
			return result;
		}catch(Exception e){
			result[1] = e.toString();
			log.error(e);
			return result;
		}
		result[2] = Integer.valueOf(count).toString();
		return result;
	}
}

