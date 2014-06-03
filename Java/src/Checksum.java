import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Checksum {
	final private char[] hexArray = "0123456789ABCDEF".toCharArray(); // used in hex conversion
	private File file; // file to hash
	private String hash; // hash to check against

	public Checksum(File file, String hash) {
		this.file = file;
		this.hash = hash;
	}
	
	/**
	 * Creates an md5 instance and calls the hashing function
	 * @return Whether the given hash and the file digest are the same value
	 */
	public boolean check() {
		MessageDigest md = null;
		try {
			// using md5
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		String digest = null;
		try {
			// hashing the file
			digest = getDigest(new FileInputStream(file), md, 2048);
		} catch (NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
		}
		// was the original input the correct hash?
		return hash.equals(digest.toLowerCase()); 
	}
	
	/**
	 * Actually hashes the file and creates a byte array of the output.
	 * @return A hex string converted from the byte array.
	 */
	private String getDigest(InputStream is, MessageDigest md, int byteArraySize)
			throws NoSuchAlgorithmException, IOException {

		md.reset();
		byte[] bytes = new byte[byteArraySize];
		int numBytes;
		while ((numBytes = is.read(bytes)) != -1) {
			md.update(bytes, 0, numBytes);
		}
		byte[] digest = md.digest();
		String result = new String(bytesToHex(digest));
		return result;
	}
	
	/**
	 * Taken from http://stackoverflow.com/a/9855338
	 * Converts a byte array to hex formatted string.
	 * @return Hex digest string
	 */
	private String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
}

