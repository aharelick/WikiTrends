import java.io.*;
import java.util.*;
import java.util.Map.Entry;


public class ReadAndPrintSerialized {
	static Map map;
	static Set set;
	public static void main(String[] args) throws ClassNotFoundException, IOException {
		if (args.length < 2) {
			System.out.println("usage: java Read....Serialized filename set/map");
			System.exit(0);
		}
		File file = new File(args[0]);
		if (!file.exists()) {
			System.out.println("File does not exist");
			System.exit(0);
		}
		if (args[1].equals("set")) {
			deserializeSet(file);
			printSet();
		} else if (args[1].equals("map")) {
			deserializeMap(file);
			printMap();
		} else {
			System.out.println("usage: java Read....Serialized filename set/map");
			System.exit(0);
		}
	}
	
	private static void deserializeSet(File file) throws IOException, ClassNotFoundException {
		System.out.println("Starting to deserialize " + file.getName());
		FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);
        set = (HashSet) ois.readObject();
        ois.close();
        fis.close();
		System.out.println("Deserialized set data is stored");
	}
	
	private static void deserializeMap(File file) throws IOException, ClassNotFoundException {
		System.out.println("Starting to deserialize " + file.getName());
		FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);
        map = (HashMap) ois.readObject();
        ois.close();
        fis.close();
		System.out.println("Deserialized map data is stored");
	}
	
	private static void printSet() {
		System.out.println("Printing...");
		Iterator i = set.iterator();
		while (i.hasNext()) {
			System.out.println(i.next().toString());
		}
	}
	
	private static void printMap() {
		System.out.println("Printing...");
		Iterator i = map.keySet().iterator();
		int count = 0;
		while (i.hasNext()) {
			Object o = i.next();
			System.out.println(o.toString() + " : " + map.get(o));
			count++;
		}
		System.out.println(count + " lines");
	}
	
	
}
