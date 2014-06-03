import java.text.ParseException;

import java.text.SimpleDateFormat;
import java.util.*;


public class GetSpan {
	private static String commandLine = "Command line arg options:\n1. 'MM-DD-YYYY MM-DD-YYYY' where the second date "
			+ "is later (both inclusive)\n2. 'MM-DD-YYYY # +/-' where the 3rd arg is either + or -."
			+ " You will get data for the inputted number of days with input date being the first or last.";
	public static void main(String... strings) {
		// There are two input formats, anything else should be rejected.
		if (strings.length !=2 && strings.length!= 3) {
			System.out.println(commandLine);
		} else if (strings.length == 2) {
			// format for two input dates
			startEndDate(strings[0], strings[1]);
		} else {
			// format for one input date, a number of days, and a sign (for before/after)
			givenLength(strings[0], Integer.parseInt(strings[1]), strings[2]);
		}
	}
	
	/**
	 * Depending on the input it calls GetDay the appropriate amount of times
	 * either going forward or backward by one day each time.
	 * 
	 * @param date - the date to either start or end the data collection at
	 * @param length - the number of days data that will be downloaded
	 * @param sign  - positive means the date is the start point of the data
	 * 				- negative means the date is the end point of the data
	 */
	private static void givenLength(String date, int length, String sign) {
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
		int increment = 0;
		// are we going forwards or backwards
		if (sign.equals("+")) {
			increment = 1;
		} else if (sign.equals("-")) {
			increment = -1;
		} else {
			System.out.println(commandLine);
			System.exit(0);
		}
		Calendar c = Calendar.getInstance();
		for (int i = 0; i < length; i++) {
			try {
				c.setTime(sdf.parse(date));
			} catch (ParseException e) {
				System.out.println(commandLine);
				e.printStackTrace();
				System.exit(0);
			}
			String[] args = {date};
			GetDay.main(args); // Get this days data
			c.add(Calendar.DATE, increment); // increment the date
			date = sdf.format(c.getTime());
		}
	}
	
	/**
	 * This method begins downloading files for the start date and increments
	 * by one until it gets to the end date.
	 * Data is downloaded for the end date (inclusive on both sides).
	 * 
	 * @param startDate - The day we start collecting data.
	 * @param endDate - The last day data is collected.
	 */
	private static void startEndDate(String startDate, String endDate) {
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
		Calendar c1 = Calendar.getInstance();
		Calendar c2 = Calendar.getInstance();
		try {
			c1.setTime(sdf.parse(startDate));
			c2.setTime(sdf.parse(endDate));
		} catch (ParseException e) {
			System.out.println(commandLine);
			e.printStackTrace();
			System.exit(0);
		}
		if (c2.getTimeInMillis() - c1.getTimeInMillis() < 0) {
			System.out.println("2nd input must be >= first input");
			System.exit(0);
		}
		// don't go past the end date
		while (c1.getTimeInMillis() <= c2.getTimeInMillis()) {
			String[] args = {startDate};
			GetDay.main(args);
			c1.add(Calendar.DATE, 1);
			startDate = sdf.format(c1.getTime());
		}

	}
}
