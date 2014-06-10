import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

public class CleanAndTakeLangSQL {
	private static Connection c = null;
    private static Statement stmt = null;
	
	private static String inputLanguage = "";
	private static LinkedList<File> files = new LinkedList<File>();
	private static String[] nameSpaces = {"Media:", "Special:", "Talk:", "User:", "User_talk:",
		"Project:", "Project_talk:", "File:", "File_talk:", "MediaWiki:", "MediaWiki_talk:",
		"Template:", "Template_talk:", "Help:", "Help_talk:", "Category:", "Category_talk:",
		"Portal:", "Wikipedia:", "Wikipedia_talk:", "P:", "N:"};
	private static String[] blacklist = {"404_error", "Main_Page", "Hypertext_Transfer_Protocol",
		"Favicon.ico", "Search", "index.html", "Wiki"};
	private static String[] imgExt = {"jpg", "gif", "png", "JPG", "PNG", "GIF", "txt", "ico"};
	private static String tableName = null;
	// just maps titles to summed views
	private static HashMap<String, Integer> map = new HashMap<String, Integer>();
	private static HashSet<String> insertedLinks = new HashSet<String>();
	private static int startFile = 1;

	
	public static void main(String args[]) throws FileNotFoundException, UnsupportedEncodingException, IOException, SQLException {
		inputLanguage = args[0];
		File folder = new File(args[1]);
		tableName = "[" + args[1] + "]";
		//startFile = Integer.parseInt(args[2]);
		if (!folder.exists()) {
			System.out.println("The input folder does not exist");
			System.exit(0);
		}
		if (!folder.isDirectory()) {
			System.out.println("The input folder is not a folder");
			System.exit(0);
		}
		findFiles(folder);
		openConnection();
		createTableCols();
		clean();
		insertedLinks.clear();
		stmt.close();
		c.close();
	}

	private static void openConnection() {
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:data.db");
			System.out.println("Opened data.db successfully");
			stmt = c.createStatement();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
	}

	private static void executeSQL(String sql) {
		//System.out.println(sql);
		try {
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
	}
	
	private static boolean keyExists(String sql) {
		//System.out.println(sql);
		ResultSet rs = null;
		try {
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				return (rs.getInt(1) == 1);
			}
			rs.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		return false;
	}

	private static void createTableCols() {
		//String sql = "CREATE TABLE " + tableName + " (Id INTEGER PRIMARY KEY AUTOINCREMENT, link TEXT NOT NULL);";
		String sql = "CREATE TABLE " + tableName + " (link TEXT PRIMARY KEY NOT NULL);";
		executeSQL(sql);
		//sql = "CREATE UNIQUE INDEX name ON " + tableName + "(link);";
		//executeSQL(sql);
		System.out.println("Table " + tableName + " created successfully with primary key 'link'");
		//System.out.println("Index created successfully on link");
	}

	private static String addCol(int col) {
		String colName = ""; 
		if (col < 10) {
			colName = "hour0" + col; 
		} else {
			colName = "hour" + col;
		}
		String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + colName + " INTEGER;";
		executeSQL(sql);
		System.out.println("Column " + colName + " was created successfully" );
		return colName;
	}
	
	private static void findFiles(File folder) {
		System.out.println("Finding gzipped files in " + folder.getName() + " ...");
		for (File fileEntry : folder.listFiles()) {
	        if (!fileEntry.isDirectory() && fileEntry.getName().endsWith(".gz")) {
	        	files.add(fileEntry);	        	
	        }
	    }
		if (files.size() != 24) {
			System.out.println("There less than 24 gzip files found");
			System.exit(0);
		}
		System.out.println("Found " + files.size() + " files, good to go");
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
					if (views > 14) {
						if (map.keySet().contains(title)) {
							map.put(title, (map.get(title) + views));
						} else {
							map.put(title, views);
						}
					}
				}
					line = bf.readLine();
					lineNum++;
					if (lineNum % 100000 == 0) {
						System.out.print(" .");
					}
			}
			System.out.println("\nFinished mapping");
			long a = System.nanoTime();
			mapToSQL(count - 1);
			long b = System.nanoTime();
			System.out.println("Duration: " + ((b - a)/1E9) + "s");
			count++;
			fileStream.close();
			gzipStream.close();
			decoder.close();
			bf.close();
			map.clear();
			System.out.println("Storage cleared, onto the next one");
		}
	}
	
	private static void mapToSQL(int hour) {
		int count = 0;
		String colName = addCol(hour);
		System.out.print("Starting map to sql .");
		executeSQL("BEGIN TRANSACTION;");
		for (Entry<String, Integer> e : map.entrySet()) {
			String key = escapeSingleQuotes(e.getKey());
			//if (keyExists("SELECT EXISTS(SELECT 1 FROM " + tableName + " WHERE link='" + key + "' LIMIT 1);")) {
			if (insertedLinks.contains(key)) {
				executeSQL("UPDATE " + tableName + " SET " + colName + "=" + e.getValue() + " WHERE link='" + key + "';");
			} else {
				executeSQL("INSERT INTO " + tableName + "(link, " + colName + ") VALUES('" + key + "', " + e.getValue() + ");");
				insertedLinks.add(key);
			}
			count++;
			if (count % 4600 == 0) {
				System.out.print(" .");
			}
		}
		executeSQL("COMMIT;");
		System.out.println("\nSQL column successfully filled");
	}
	
	private static String escapeSingleQuotes(String input) {
		return input.replaceAll("'", "''");
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
