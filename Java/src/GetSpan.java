import java.text.ParseException;

import java.text.SimpleDateFormat;
import java.util.*;


public class GetSpan {
	
	public static void main(String... strings) {
		if (strings.length !=2 && strings.length!= 3) {
			System.out.println("Command line arg options:\n1. 'MM-DD-YYYY MM-DD-YYYY' where the second date "
					+ "is later (both inclusive)\n2. 'MM-DD-YYYY # +/-' where the 3rd arg is either + or -."
					+ " You will get data for the inputted number of days with input date being the first or last.");
		} else if (strings.length == 2) {
			startEndDate(strings[0], strings[1]);
		} else {
			givenLength(strings[0], Integer.parseInt(strings[1]), strings[2]);
		}
	}
	
	private static void givenLength(String date, int length, String sign) {
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
		int increment = 0;
		if (sign.equals("+")) {
			increment = 1;
		} else {
			increment = -1;
		}
		Calendar c = Calendar.getInstance();
		for (int i = 0; i < length; i++) {
			try {
				c.setTime(sdf.parse(date));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			String[] args = {date};
			GetDay.main(args);
			c.add(Calendar.DATE, increment);
			date = sdf.format(c.getTime());
		}
	}
	
	private static void startEndDate(String startDate, String endDate) {
		SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy");
		Calendar c1 = Calendar.getInstance();
		Calendar c2 = Calendar.getInstance();
		try {
			c1.setTime(sdf.parse(startDate));
			c2.setTime(sdf.parse(endDate));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		if (c2.getTimeInMillis() - c1.getTimeInMillis() < 0) {
			System.out.println("2nd input must be >= first input");
			System.exit(0);
		}
		while (c1.getTimeInMillis() <= c2.getTimeInMillis()) {
			String[] args = {startDate};
			GetDay.main(args);
			c1.add(Calendar.DATE, 1);
			startDate = sdf.format(c1.getTime());
		}

	}
}
