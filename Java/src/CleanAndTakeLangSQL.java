import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

public class CleanAndTakeLangSQL {
	// sqlite connections and statements
	private static Connection c = null;
    private static Statement stmt = null;
	
	private static String inputLanguage = ""; 
	private static LinkedList<File> files = new LinkedList<File>(); // files found in input folder
	// can't allow these at the front of a title
	private static final String[] nameSpaces = {"Media:", "Special:", "Talk:", "User:", "User_talk:",
		"Project:", "Project_talk:", "File:", "File_talk:", "MediaWiki:", "MediaWiki_talk:",
		"Template:", "Template_talk:", "Help:", "Help_talk:", "Category:", "Category_talk:",
		"Portal:", "Wikipedia:", "Wikipedia_talk:", "P:", "N:"};
	// can't allow these at all
	private static final String[] blacklist = {"404_error", "Main_Page", "Hypertext_Transfer_Protocol",
		"Favicon.ico", "Search", "index.html", "Wiki"};
	// can't allow these at the end of a title
	private static final String[] imgExt = {".jpg", ".gif", ".png", ".JPG", ".PNG", ".GIF", ".txt", ".ico"};
	private static String tableName = null;
	// map titles to summed views
	private static HashMap<String, Integer> map = new HashMap<String, Integer>();
	// tracks if we have already inserted that link into today's table in the db
	private static HashSet<String> insertedLinks = new HashSet<String>();
	
	public static void main(String args[]) throws FileNotFoundException, UnsupportedEncodingException, IOException, SQLException {
		if (args.length != 3) {
			System.out.println("Args: language folder_with_gzips db_name");
			System.exit(0);
		}
		inputLanguage = args[0];
		File folder = new File(args[1]);
		// brackets because sqlite doesn't like starting with a number
		tableName = "[" + args[1] + "]";
		if (!folder.exists()) {
			System.out.println("The input folder does not exist");
			System.exit(0);
		}
		if (!folder.isDirectory()) {
			System.out.println("The input folder is not a folder");
			System.exit(0);
		}
		findFiles(folder);
		openConnection(args[2]);
		createTableCols();
		clean();
		insertedLinks.clear();
		stmt.close();
		c.close();
	}

	/**
	 * Uses the sqlite3 jdbc jar to connect/create a database. It also sets a global variable
	 * that will be used for statements throughout the program.
	 * 
	 * @param db - the name of the database to open a connection with (either create or open)
	 */
	private static void openConnection(String db) {
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:" + db);
			System.out.println("Opened " + db + "successfully");
			stmt = c.createStatement(); // used for all sqlite statements
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
	}

	/**
	 * Uses the statement global variable to execute the SQL code. Created so I didn't
	 * have to have a separate try/catch every time I wanted to run SQL.
	 * 
	 * @param sql - the actual code to be executed as a String
	 */
	private static void executeSQL(String sql) {
		try {
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
	}

	/**
	 * Creates a table to populate with the gzips for the day. 
	 */
	private static void createTableCols() {
		/* The table name is of the format [MM-dd-yyyy]. The link column is the primary key
		* meaning it is unique and used as the identifier for the row. It of course cannot
		* be null.
		*/
		String sql = "CREATE TABLE " + tableName + " (link TEXT PRIMARY KEY NOT NULL);";
		executeSQL(sql);
		System.out.println("Table " + tableName + " created successfully with primary key 'link'");
	}

	/**
	 * Alters the table by adding the next column.
	 * @param col - zero indexed column number from [0,23]
	 * @return    - the name of the column (ex. hour00, hour15) to be used when inserting/updating 
	 */
	private static String addCol(int col) {
		String colName = ""; 
		// everything is 2 digits so if the col # is only one then we need another zero
		if (col < 10) {
			colName = "hour0" + col; 
		} else {
			colName = "hour" + col;
		}
		String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + colName + " INTEGER;";
		executeSQL(sql);
		return colName;
	}
	
	/**
	 * Using a command line argument the method goes into a directory and gathers all
	 * files with the extension .gz and stores their file names. This assumes that 
	 * all the files with a .gz extensions in the folder are for the current day
	 *  (this can be done by downloading with the GetDay class).
	 * 
	 * @param folder - the directory which will be opened and searched for gzips
	 */
	private static void findFiles(File folder) {
		System.out.println("Finding gzipped files in " + folder.getName() + " ...");
		for (File fileEntry : folder.listFiles()) {
	        if (!fileEntry.isDirectory() && fileEntry.getName().endsWith(".gz")) {
	        	files.add(fileEntry);	        	
	        }
	    }
		// currently this class only works if all 24 files are present for the day
		if (files.size() != 24) {
			System.out.println("There less than 24 gzip files found");
			System.exit(0);
		}
		System.out.println("Found " + files.size() + " files, good to go");
	}
	
	/**
	 * Reads each file in the folder, checks for validity, and strips the unnecessary lines. 
	 * Fixes the case of the title of the article and puts it into a map with it's view count.
	 * 
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	private static void clean() throws FileNotFoundException, UnsupportedEncodingException, IOException {
		int count = 1; // the current file of 24 that is being cleaned
		for (File file : files) {
			System.out.println("Cleaning file number: " + count + " (" + file.getName() + ")");
			// the classes to read a gzip
			InputStream fileStream = new FileInputStream(file);
			InputStream gzipStream = null;
			gzipStream = new GZIPInputStream(fileStream);
			Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
			BufferedReader bf = new BufferedReader(decoder);
			String line;
			// skip all of the lines before the requested language
			while (!(line = bf.readLine()).startsWith(inputLanguage + " "));
			System.out.print("Reading lines .");
			long lineNum = 1;
			while (bf.ready()) {
				String[] tokens = line.split(" ");
				String lang = tokens[0]; // language
				String title = tokens[1]; // article title
				/* if the line language is lexicographically greater than 
				   the input language we can stop */
				if (lang.compareTo(inputLanguage) > 0) {
					break;
					// makes sure there's nothing wrong with the link
				} else if (kosherLink(title)) {
					title = cleanAnchors(title); // get rid of hashes
					title = capitalizeFirst(title); // fix the case
					int views = Integer.parseInt(tokens[2]);
					// discard anything with <= 14 views
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
					// a way to see progress by printing periods
					if (lineNum % 100000 == 0) {
						System.out.print(" .");
					}
			}
			System.out.println();
			mapToSQL(count - 1);
			count++;
			fileStream.close();
			gzipStream.close();
			decoder.close();
			bf.close();
			// only want to collect/store data for an hour at a time
			map.clear();
		}
	}
	
	/**
	 * Convert a hashmap that maps titles to views to a sqlite database with a table for each
	 * day and a column for each hour.
	 * 
	 * @param hour - zero indexed hour number from [0,23]
	 */
	private static void mapToSQL(int hour) {
		int count = 0;
		String colName = addCol(hour);
		System.out.print("Starting map to sql .");
		// use a single transaction for each hour increase speed
		executeSQL("BEGIN TRANSACTION;");
		for (Entry<String, Integer> e : map.entrySet()) {
			// have to escape single quotes
			String key = escapeSingleQuotes(e.getKey());
			// if we already have a row in the table for that article
			if (insertedLinks.contains(key)) {
				executeSQL("UPDATE " + tableName + " SET " + colName + "=" + e.getValue() + " WHERE link='" + key + "';");
			} else {
				executeSQL("INSERT INTO " + tableName + "(link, " + colName + ") VALUES('" + key + "', " + e.getValue() + ");");
				insertedLinks.add(key);
			}
			count++;
			// a way to see progress by printing periods
			if (count % 4600 == 0) {
				System.out.print(" .");
			}
		}
		executeSQL("COMMIT;");
		System.out.println();
	}
	
	/**
	 * Escape single quotes in a string by replacing each instance of a single
	 * quote with two. ' -> '', they're -> they''re
	 * 
	 * @param input - the string to perform the operation on
	 * @return      - the string after escaping
	 */
	private static String escapeSingleQuotes(String input) {
		return input.replaceAll("'", "''");
	}
	
	/**
	 * Determine if the string is allowed.
	 * 
	 * @param input - the string to check
	 * @return      - boolean, true if everything checks out, else false
	 */
	private static boolean kosherLink(String input) {
		// the article name can't start with a namespace
		for (String title : nameSpaces) {
			if (input.startsWith(title)) {
				return false;
			}
		}
		// it can't be an empty string
		if (input.length() == 0) {
			return false;
		}
		// it can't be just an image
		for (String extension : imgExt) {
			if (input.endsWith(extension)) {
				return false;
			}
		}
		// and finally it cannot be on the blacklist
		for (String bad : blacklist) {
			if (bad.equals(input)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Remove any anchors and whatever follows. Anchors denote a subsection 
	 * of a wikipedia page and each subsection view should just be added to the
	 * parent page it exists on.
	 * 
	 * @param title - the string to clean the anchors off of
	 * @return      - the string after cleaning
	 */
	private static String cleanAnchors(String title) {
		int location;
		if ((location = title.indexOf("#")) != -1) {
			return title.substring(0, location);
		}
		return title;
	}
	
	/**
	 * Wikipedia automatically capitalizes the first letter of the first word 
	 * on every link. Therefore there may be instances of google, and Google; so,
	 * we make everything the same case format so we can sum together
	 * views that share the same title.
	 * @param input
	 * @return
	 */
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
