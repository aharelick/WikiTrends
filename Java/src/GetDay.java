import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class GetDay {
	private final static String BASEURL = "http://dumps.wikimedia.org/other/pagecounts-raw/";
	private static String monthURL; // the base + YYYY/YYYY-MM/
	private static String month; // MM
	private static String day; // DD
	private static String year; // YYYY
	private static ArrayList<String> toDownload = new ArrayList<String>();
	private static HashMap<String, String> hashes = new HashMap<String,String>();

	
	public static void main(String[] args) {
		String[] inputSplit = args[0].split("-");
		month = inputSplit[0];
		day = inputSplit[1];
		year = inputSplit[2];
		System.out.println("Requested Date: " + args[0]);
		parseHTMl(month, day, year);
		directoryCreate();
		downloadmd5();
		readInmd5();
		downloadstats();
		cleanup();
	}
	/**
	 * Creates the URL to access the files for that month. Grabs all of the HTML.
	 * Parse through it to find the files from the requested day and adds them
	 * to the list.
	 * @param month
	 * @param day
	 * @param year
	 */
	private static void parseHTMl(String month, String day, String year) {
		monthURL = BASEURL + year + "/" + year + "-" + month + "/";
		Document doc = null;
		try {
			doc = Jsoup.connect(monthURL).get(); // pulls html
		} catch (IOException e) {
			System.err.println("There was an error getting HTML from " + monthURL);
			e.printStackTrace();
		}
		Elements links = doc.select("li > a"); // selects the list items in a link tag
		for (Element link : links) {
			String linkHref = link.attr("href");
			String date = linkHref.split("-")[1];
			// makes sure to only take the count files that are for the input date
			if (linkHref.startsWith("page") && date.endsWith(day)) {
				toDownload.add(linkHref);
			}
		}
	}
	
	/**
	 * Creates the url to download the md5 sums for the appropriate month.
	 * Calls the function to download them.
	 */
	private static void downloadmd5() {
		String link = "md5sums.txt";
		File outputFile = new File(month + "-" + day + "-" + year + "/" + link);
		System.out.println("downloading md5 sums");
		download(link, outputFile);
	}
	
	/**
	 * Once the hashes are downloaded they need to be read into the program.
	 * Each line is split into the file name and it's respective hash.
	 * These strings are put into a map for easy access.
	 */
	private static void readInmd5() {
		System.out.println("reading in hashes");
		BufferedReader bf = null;
		try {
			bf = new BufferedReader(new FileReader
					(new File(month + "-" + day + "-" + year + "/" + "md5sums.txt")));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			while (bf.ready()) {
				String line = bf.readLine();
				String hash = line.split(" +")[0];
				String filename = line.split(" +")[1];
				// makes sure to only take the hashes that are for the input date
				if (filename.startsWith("page") && filename.split("-")[1].endsWith(day)) {
					hashes.put(filename, hash);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			bf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(hashes.size() + " hashes stored");
	}
	
	/**
	 * Go through the list of files to download. If the file is already in the directory
	 * then we want to check it's validity so we use the checksum. If it's not correct
	 * the file is redownloaded. If it never existed to begin with, it gets downloaded.
	 */
	private static void downloadstats() {
		for (int i = 0; i < toDownload.size(); i++) {
			String link = toDownload.get(i);
			File outputFile = new File(month + "-" + day + "-" + year + "/" + link);
			if (!outputFile.exists()) {
				System.out.println("downloading file " + (i + 1) + " of " + toDownload.size() + ": " + link);
				download(link, outputFile);
			} else {
				Checksum checksum = new Checksum(outputFile, hashes.get(link));
				if (checksum.check()) {
					System.out.println("checksum passed for " + (i + 1) + " of " + toDownload.size() + ": " + link);
					continue; // we don't want to download it's already there
				} else {
					System.out.println("redownloading file " + (i + 1) + " of " + toDownload.size() + ": " + link);
					download(link, outputFile);
				}
			}
		}
	}	
	
	/**
	 * The actual method to open a stream and download the file.
	 * @param link - the url to download from
	 * @param outputFile - where to put the downloaded data
	 */
	private static void download(String link, File outputFile) {
		URL website = null;
		try {
			website = new URL(monthURL + link);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		ReadableByteChannel rbc = null;
		try {
			rbc = Channels.newChannel(website.openStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(outputFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		try {
			// Long.MAX_VALUE is the number of bytes read
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Creates the folder to place the data for the requested day.
	 * The folder is of the date format MM-DD-YYYY.
	 * It does not replace if a directory is already there.
	 */
	private static void directoryCreate() {
		File dir = new File(month + "-" + day + "-" + year);
		if (!dir.exists()) {
		    System.out.println("creating directory: " + dir.toString());
		    if (dir.mkdir()) {    
		    	 System.out.println("created directory: " + dir.toString());  
		    } else {
		    	System.out.println("couldn't create directory");
		    	System.exit(0);
		    }
		} else {
			System.out.println("directory (" + dir.toString() + ") exists, will validate checksums and fills the rest of directory");
		}
	}
	
	/**
	 * A small method to reset the class.
	 * Ensures no data rolls over when GetDay is used by GetSpan.
	 */
	private static void cleanup() {
		toDownload.clear();
		hashes.clear();
		System.out.println("-------------------------------");
	}
}
