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
	private static Connection c = null;
    private static Statement stmt = null;
    private static HashMap<String, Integer> trendValues = new HashMap<String, Integer>();
    private static final String[] hours = {"00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10",
    	"11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23"};
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
	
	private static void calcDay(String day, int sign) {
		long startTime = System.nanoTime();
		ResultSet rs = null;
		try {
			//for (String hour: hours) {
				//rs = stmt.executeQuery("SELECT " + hour + " from [" + day + "] WHERE hour" + hour + " IS NOT NULL;");
				rs = stmt.executeQuery("SELECT * from [" + day + "];");
				while (rs.next()) {
					int count = 0;
					String link = rs.getString(1);
					for (int i = 2; i <= 25; i++) {
						count+=rs.getInt(i);
					}
					count*=sign;
					//String link = rs.getString(1);
					//int count = rs.getInt(2) * sign;
					Integer temp;
					if ((temp = trendValues.get(link)) != null) {
						trendValues.put(link, temp + count);
					} else {
						trendValues.put(link, count);
					}
				}
				rs.close();
			//}
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		long endTime = System.nanoTime();
		System.out.println("Done with " + day + " - Duration: " + (endTime - startTime)/1E9 + "s");
	}
	
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
	
	private static SortedSet<Map.Entry<String, Integer>> entriesSortedByValues() {
	    SortedSet<Entry<String, Integer>> sortedEntries = new TreeSet<Entry<String, Integer>>(
	        new Comparator<Entry<String, Integer>>() {
	            @Override
	            public int compare(Entry<String, Integer> e1, Entry<String, Integer> e2) {
	                return e2.getValue().compareTo(e1.getValue());
	            }
	        }
	    );
	    sortedEntries.addAll(trendValues.entrySet());
	    System.out.println("Sorted values");
	    return sortedEntries;
	}
	
	private static void printSorted(SortedSet<Entry<String, Integer>> set) {
		System.out.println("Start printing to " + outputFile.toString() + " . . .");
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
		System.out.println("Done printing");
	}

	
}

