import java.io.*;
import java.net.URLEncoder;
import java.util.*;

public class MturkFormat {
	private static File file = null;
	private static File outputFile = null;
	private static int numLines = 0;
	private static int numHeadings = 0;
	private static LinkedList<String> toOutput = new LinkedList<String>();


	public static void main(String[] args) {
		if (args.length != 4) {
			System.out.println("command line args: file_name outputFile_name #ofLinesFromFile #ofHeadings");
			System.exit(0);
		}
		file = new File(args[0]);
		if (!file.exists()) {
			System.out.println("Data file does not exist");
			System.exit(0);
		}
		outputFile = new File (args[1]);
		if (outputFile.exists()) {
			System.out.println("Output file already exists");
			System.exit(0);
		}
		numLines = Integer.parseInt(args[2]);
		numHeadings = Integer.parseInt(args[3]);
		readIn();
		printOut();
	}
	
	private static void readIn() {
		BufferedReader r = null;
		try {
			r = new BufferedReader(new FileReader(file));
			while (r.ready() && numLines > 0) {
				String line = r.readLine();
				String title = line.split(" +")[1];
				toOutput.add(title);
				numLines--;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void printOut() {
		BufferedWriter w = null;
		try {
			w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile),"UTF-8"));
			for (int i = 1; i <= numHeadings; i++) {
				String num = i + "";
				if (i < 10) {
					num = "0" + num;
				}
				w.append("Encoded" + num);
			    w.append(',');
			    w.append("Decoded" + num);
			    w.append(',');
			    w.append("WikiURL" + num);
			    w.append(',');
			    w.append("NewsURL" + num);
			    if (i != numHeadings) {
				    w.append(',');
			    }
			}
		    w.append('\n');
		    int counter = 0;
			for (String title : toOutput) {
				String decodedTitle = (java.net.URLDecoder.decode(title, "UTF-8")).replaceAll("_", " ");
				w.append(title);
			    w.append(',');
			    w.append(decodedTitle);
			    w.append(',');
			    w.append("http://en.wikipedia.org/wiki/" + title);
			    w.append(',');
			    w.append("https://www.google.com/search?q=" + URLEncoder.encode(decodedTitle, "UTF-8") + "&tbm=nws");
			    counter += 4;
			    if (counter % (numHeadings * 4) == 0) {
				    w.append('\n');
			    } else {
				    w.append(',');
			    }
			}
			w.flush();
		    w.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
