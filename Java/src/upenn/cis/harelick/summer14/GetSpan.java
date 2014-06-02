package upenn.cis.harelick.summer14;
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
		Calendar c = Calendar.getInstance();
		for (int i = 0; i < length; i++) {
			
			try {
				c.setTime(sdf.parse(date));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			c.add(Calendar.DATE, 1);
			date = sdf.format(c.getTime());
		}
	}
	
	private static void startEndDate(String startDate, String endDate) {
		
	}
}
