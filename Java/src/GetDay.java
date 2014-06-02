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
	private static String monthURL;
	private static String month;
	private static String day;
	private static String year;
	private static ArrayList<String> toDownload = new ArrayList<String>();
	private static HashMap<String, String> hashes = new HashMap<String,String>();

	
	public static void main(String[] args) {
		String[] inputSplit = args[0].split("-");
		month = inputSplit[0];
		day = inputSplit[1];
		year = inputSplit[2];
		parseHTMl(month, day, year);
		directoryCreate();
		downloadmd5();
		readInmd5();
		downloadstats();
		cleanup();
	}
	
	private static void parseHTMl(String month, String day, String year) {
		monthURL = BASEURL + year + "/" + year + "-" + month + "/";
		Document doc = null;
		try {
			doc = Jsoup.connect(monthURL).get();
		} catch (IOException e) {
			System.err.println("There was an error getting HTML from " + monthURL);
			e.printStackTrace();
		}
		Elements links = doc.select("li > a");
		for (Element link : links) {
			String linkHref = link.attr("href");
			String date = linkHref.split("-")[1];
			if (linkHref.startsWith("page") && date.endsWith(day)) {
				toDownload.add(linkHref);
			}
		}
	}
	
	private static void downloadmd5() {
		String link = "md5sums.txt";
		File outputFile = new File(month + "-" + day + "-" + year + "/" + link);
		System.out.println("downloading md5 sums");
		download(link, outputFile);
	}
	
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
					continue;
				} else {
					System.out.println("redownloading file " + (i + 1) + " of " + toDownload.size() + ": " + link);
					download(link, outputFile);
				}
			}
		}
	}	
	
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
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
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
			System.out.println("directory exists, will fill the rest of directory");
		}
	}
	
	private static void cleanup() {
		toDownload.clear();
		hashes.clear();
	}
}
