import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class SumSpansDiffSort {
	// sqlite connections and statements
	private static Connection c = null;
    private static Statement stmt = null;
    // maps each title to its trend value
    private static HashMap<String, Integer> trendValues = new HashMap<String, Integer>();
    // the text file that the results will be printed to
    private static File outputFile;

	public static void main(String args[]) throws FileNotFoundException, UnsupportedEncodingException, IOException, SQLException {
		if (args.length != 6) {
			System.out.println("Args: db_name outputFile.txt span_1_start span_1_end span_2_start span_2_end");
			System.exit(0);
		}
		outputFile = new File(args[1]);
		if (outputFile.exists()) {
			System.out.println("The output file already exists, please choose another");
			System.exit(0);
		}
		openConnection(args[0]);
		roll(args[2], args[3], -1);
		roll(args[4], args[5], 1);
		stmt.close();
		c.close();
		printSorted(entriesSortedByValues());
	}
	
	/**
	 * Uses the sqlite3 jdbc jar to connect to a database. It also sets a global variable
	 * that will be used for statements throughout the program.
	 * 
	 * @param db - the name of the database to open a connection with
	 */
	private static void openConnection(String db) {
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:" + db);
			System.out.println("Opened " + db + " successfully");
			stmt = c.createStatement();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
	}
	
	/**
	 * Queries the database for all the data in a certain table. Then it goes across
	 * every row and sums the integers. It also applies the correct multiplier (1 or -1)
	 * depending on whether the current date is in the first or second span.
	 * 
	 * @param day  - the day we want data for (the table name)
	 * @param sign - whether we should multiply the sum by -1 or 1 
	 */
	private static void calcDay(String day, int sign) {
		long startTime = System.nanoTime();
		ResultSet rs = null;
		try {
			rs = stmt.executeQuery("SELECT * from [" + day + "];");
			// iterating down the table (looking at each row)
			while (rs.next()) {
				int count = 0;
				String link = rs.getString(1); // article title
				// iterating across the columns (left -> right)
				for (int i = 2; i <= 25; i++) {
					count += rs.getInt(i);
				}
				count *= sign;
				Integer temp;
				if ((temp = trendValues.get(link)) != null) {
					trendValues.put(link, temp + count);
				} else {
					trendValues.put(link, count);
				}
			}
			rs.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		long endTime = System.nanoTime();
		System.out.println("Done with " + day + " - Duration: "
				+ (endTime - startTime) / 1E9 + "s");
	}
	
	/**
	 * This method iterates through a calendar and calls calcDay for each date. 
	 * It begins at the start date and goes through each day until it reaches the 
	 * end date (the end date is included).
	 * 
	 * @param startDate - the date to start the roll
	 * @param endDate   - the last day counted
	 * @param sign      - whether we should multiply the sum by -1 or 1 (first or second span?)
	 */
	private static void roll(String startDate, String endDate, int sign) {
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
		Calendar c1 = Calendar.getInstance();
		Calendar c2 = Calendar.getInstance();
		String currDate = startDate;
		try {
			c1.setTime(sdf.parse(startDate));
			c2.setTime(sdf.parse(endDate));
		} catch (ParseException e) {
			System.out.println("start: " + startDate + ", end: " + endDate);
			e.printStackTrace();
			System.exit(0);
		}
		if (c2.getTimeInMillis() - c1.getTimeInMillis() < 0) {
			System.out.println("2nd date must be >= first date");
			System.exit(0);
		}
		// don't go past the end date
		while (c1.getTimeInMillis() <= c2.getTimeInMillis()) {
			calcDay(currDate, sign);
			c1.add(Calendar.DATE, 1);
			currDate = sdf.format(c1.getTime());
		}
	}
	
	/**
	 * Converts a global variable hashmap which is unordered by definition 
	 * into a sorted set of entries. Each entry is the article title and 
	 * it's trend value. The entries are sorted from greatest to least trend value.
	 * 
	 * @return - the sorted set of entries (the entries originally come from the hashmap).
	 */
	private static SortedSet<Map.Entry<String, Integer>> entriesSortedByValues() {
	    SortedSet<Entry<String, Integer>> sortedEntries = new TreeSet<Entry<String, Integer>>(
	        new Comparator<Entry<String, Integer>>() {
	            @Override
	            public int compare(Entry<String, Integer> e1, Entry<String, Integer> e2) {
	            	// e2 before e1 because we want greatest to least
	                return e2.getValue().compareTo(e1.getValue());
	            }
	        }
	    );
	    sortedEntries.addAll(trendValues.entrySet());
	    System.out.println("Sorted values");
	    return sortedEntries;
	}
	
	/**
	 * Iterates through a set of entries and prints every line in the format:
	 * TrendValue     ArticleTitle
	 * There are 5 spaces, NOT A TAB, between the two fields.
	 *  
	 * @param set - the sorted set of entries that we want to print
	 */
	private static void printSorted(SortedSet<Entry<String, Integer>> set) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(outputFile, "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		for (Entry<String, Integer> e: set) {
			writer.println(e.getValue() + "     " + e.getKey());
		}
		writer.close();
		System.out.println("Done printing to " + outputFile.toString());
	}

	
}

