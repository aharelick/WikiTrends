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
	// just maps titles to summed views
	private static HashMap<String, Integer> map2 = new HashMap<String, Integer>();
	// list of files that have been included in the sum
	private static HashSet<String> readFiles = new HashSet<String>();
	private static String folderName = null;
	private static File resultsFolder = null;


	
	public static void main(String[] args) throws IOException, ClassNotFoundException {
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
//		if (checkForStoredFiles()) {
//			deserialize();
//		}
		folderName = folder.toString();
		resultsFolder = new File(folderName + "-results");
		if (resultsFolder.mkdir()) {
			System.out.println(resultsFolder.toString() + " directory created");
		} else {
			System.out.println(resultsFolder.toString() + " directory couldn't be created");
			System.exit(0);
		}
		findFiles(folder);
		clean();
		serializeList("readFiles-" + folderName + ".ser");
	}
	
	private static boolean checkForStoredFiles() throws IOException {
		File mapSer = new File("map.ser");
		File listSer = new File("read.ser");
		boolean exists = (mapSer.exists() && listSer.exists());
		if (!exists) {
			System.out.println("Either/Both map.ser and list.ser are missing.");
			System.out.println("Is this your first file? (y/n)");
			System.out.print("> ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String response = null;
			if ((response = br.readLine()).equals("y")) {
				System.out.println("Good to go");
				return false;
			} else if (response.equals("n")) {
				System.out.println("if this isn't your first file, you should have an existing map and list");
				System.exit(0);
			} else {
				System.out.println("you must enter y or n, quitting");
				System.exit(0);
			}
		}
		return true;
	}
	
	private static void findFiles(File folder) throws IOException {
		System.out.println("Finding gzipped files in " + folder.getName() + " ...");
		for (File fileEntry : folder.listFiles()) {
	        if (!fileEntry.isDirectory() && fileEntry.getName().endsWith(".gz") /* && !readFiles.contains(fileEntry.getName()) */) {
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
			System.out.println("Cleaning file number: " + count + " (" + file.getName() + ")");
			InputStream fileStream = new FileInputStream(file);
			InputStream gzipStream = null;
			gzipStream = new GZIPInputStream(fileStream);
			Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
			BufferedReader bf = new BufferedReader(decoder);
			String line;
			while (!(line = bf.readLine()).startsWith(inputLanguage + " "));
			System.out.println("Found language: " + inputLanguage);
			System.out.print("Reading lines .");
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
					if (views > 4) {
						if (map2.keySet().contains(title)) {
							map2.put(title, (map2.get(title) + views));
						} else {
							map2.put(title, views);
						}
					}
				}
					line = bf.readLine();
					lineNum++;
					if (lineNum % 100000 == 0) {
						System.out.print(" .");
					}
			}
			System.out.println();
			if ((count % 4 == 0) || count == files.size()) {
				serializeMap("map-" + folderName + "-" + (int)(Math.ceil(count / 4.0)) + ".ser" );
			}
			count++;
			readFiles.add(file.getName());
			fileStream.close();
			gzipStream.close();
			decoder.close();
			bf.close();
		}
	}
	/*
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
			count++;
		}
	}
	*/
	private static void serializeMap(String input) throws IOException {
		System.out.println("Starting to serialize map");
		FileOutputStream fos1 = new FileOutputStream(resultsFolder.toString() + "/" + input);
		ObjectOutputStream oos1 = new ObjectOutputStream(fos1);
		oos1.writeObject(map2);
		oos1.close();
		fos1.close();
		System.out.println("Serialized map data is saved in " + resultsFolder.toString() + "/" + input);
		map2.clear();
		System.out.println("map cleared");
	}
	
	private static void serializeList(String input) throws IOException {
		System.out.println("Starting to serialize readFiles list");
		FileOutputStream fos2 = new FileOutputStream(resultsFolder.toString() + "/" + input);
		ObjectOutputStream oos2 = new ObjectOutputStream(fos2);
		oos2.writeObject(readFiles);
		oos2.close();
		fos2.close();
		System.out.println("Serialized list data is saved in " + resultsFolder.toString() + "/" +  input);
	}
	
	@SuppressWarnings("unchecked")
	private static void deserialize() throws IOException, ClassNotFoundException {
		System.out.println("Starting to deserialize map");
		FileInputStream fis1 = new FileInputStream("map.ser");
        ObjectInputStream ois1 = new ObjectInputStream(fis1);
        map2 = (HashMap<String, Integer>) ois1.readObject();
        ois1.close();
        fis1.close();
		System.out.println("Deserialized map data is stored");
		System.out.println("Starting to deserialize readFiles list");
		FileInputStream fis2 = new FileInputStream("read.ser");
        ObjectInputStream ois2 = new ObjectInputStream(fis2);
        readFiles = (HashSet<String>) ois2.readObject();
        ois2.close();
        fis2.close();
		System.out.println("Deserialized list data is stored");
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
