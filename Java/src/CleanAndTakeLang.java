import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class CleanAndTakeLang {
	private static String inputLanguage = "";
	private static LinkedList<File> files = new LinkedList<File>();
	private static String[] nameSpaces = {"Media:", "Special:", "Talk:", "User:", "User_talk:",
		"Project:", "Project_talk:", "File:", "File_talk:", "MediaWiki:", "MediaWiki_talk:",
		"Template:", "Template_talk:", "Help:", "Help_talk:", "Category:", "Category_talk:",
		"Portal:", "Wikipedia:", "Wikipedia_talk:", "P:", "N:"};
	private static String[] blacklist = {"404_error", "Main_Page", "Hypertext_Transfer_Protocol",
		"Favicon.ico", "Search", "index.html", "Wiki"};
	private static String[] imgExt = {"jpg", "gif", "png", "JPG", "PNG", "GIF", "txt", "ico"};
	//               Date           Hour            Title   Views 
	private static HashMap<String, ArrayList<HashMap<String, Integer>>> map = new HashMap<String, ArrayList<HashMap<String, Integer>>>();

	
	public static void main(String[] args) throws IOException {
		inputLanguage = args[0];
		File folder = new File(args[1]);
		if (!folder.exists()) {
			System.out.println("The input folder does not exist");
			System.exit(0);
		}
		if (!folder.isDirectory()) {
			System.out.println("The input folder is not a folder");
			System.exit(0);
		}
		findFiles(folder);
		clean();
		serialize();
	}
	
	private static void findFiles(File folder) throws IOException {
		System.out.println("Finding gzipped files...");
		for (File fileEntry : folder.listFiles()) {
	        if (!fileEntry.isDirectory() && fileEntry.getName().endsWith(".gz")) {
	        	files.add(fileEntry);	        	
	        }
	    }
		if (files.isEmpty()) {
			System.out.println("There were no gzip files found");
			System.exit(0);
		}
		System.out.println("Found " + files.size() + " files");
	}
	
	private static void clean() throws FileNotFoundException, UnsupportedEncodingException, IOException {
		System.out.println("Starting to clean files...");
		int count = 1;
		for (File file : files) {
			System.out.println("Cleaning file number: " + count);
			InputStream fileStream = new FileInputStream(file);
			InputStream gzipStream = null;
			gzipStream = new GZIPInputStream(fileStream);
			Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
			BufferedReader bf = new BufferedReader(decoder);
			String line;
			while (!(line = bf.readLine()).startsWith(inputLanguage + " "));
			System.out.println("Found language: " + inputLanguage);
			long lineNum = 1;
			while (bf.ready()) {
				String[] tokens = line.split(" ");
				String lang = tokens[0];
				String title = tokens[1];
				if (lang.compareTo(inputLanguage) > 0) {
					break;
				} else if (kosherLink(title)) {
					title = cleanAnchors(title);
					title = capitalizeFirst(title);
					int views = Integer.parseInt(tokens[2]);
					String date = getDateFromFile(file.getName());
					int hour = getHourFromFile(file.getName());
					if (map.get(date) == null) {
						map.put(date, initializeArrayList());
					}
					if ((map.get(date)).get(hour).keySet().contains(title)) {
						(map.get(date)).get(hour).put(title, (map.get(date)).get(hour).get(title) + views);
					} else {
						(map.get(date)).get(hour).put(title, views);
					}
				}
					line = bf.readLine();
					lineNum++;
					if (lineNum % 100000 == 0) {
						System.out.println("Have read " + lineNum + " lines of " + inputLanguage);
					}
			}
			fileStream.close();
			gzipStream.close();
			decoder.close();
			bf.close();
		}
		count++;
	}
	
	private static void serialize() throws IOException {
		System.out.println("Starting to serialize map");
		FileOutputStream fos = new FileOutputStream("map.ser");
         ObjectOutputStream oos = new ObjectOutputStream(fos);
         oos.writeObject(map);
         oos.close();
         fos.close();
         System.out.println("Serialized map data is saved in map.ser");
	}
	
	private static boolean kosherLink(String input) {
			for (String title : nameSpaces) {
				if (input.startsWith(title + ":")) {
					return false;
				}
			}
			if (input.length() == 0) {
				return false;
			}
			for (String extension : imgExt) {
				if (input.endsWith("." + extension)) {
					return false;
				}
			}
			for (String bad : blacklist) {
				if (bad.equals(input)) {
					return false;
				}
			}
			return true;
		}
		
	private static String cleanAnchors(String title) {
		int location;
		if ((location = title.indexOf("#")) != -1) {
			return title.substring(0, location);
		}
		return title;
	}
	
	private static String getDateFromFile(String filename) {
		String date = filename.split("-")[1];
		String month = date.substring(4, 6);
		String day = date.substring(6);
		String year = date.substring(0, 4);
		return month + "-" + day + "-" + year;
	}
	
	private static int getHourFromFile(String filename) {
		String hour = filename.split("-")[2];
		return Integer.parseInt(hour.substring(0, 2));
	}
	
	private static ArrayList<HashMap<String, Integer>> initializeArrayList() {
		ArrayList<HashMap<String, Integer>> list = new ArrayList<HashMap<String, Integer>>(24);
		for (int i = 0; i < 24; i++) {
			list.add(new HashMap<String, Integer>());
		}
		return list;
	}
	
	private static String capitalizeFirst(String input) {
		if (input.length() == 0) {
			return input;
		} else if (input.length() == 1) {
			return input.toUpperCase();
		} else {
			return input.substring(0,1).toUpperCase() + input.substring(1);
		}
	}
}
