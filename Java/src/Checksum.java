import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Checksum {
	final private char[] hexArray = "0123456789ABCDEF".toCharArray();
	private File file;
	private String hash;

	public Checksum(File file, String hash) {
		this.file = file;
		this.hash = hash;
	}
	
	public boolean check() {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		String digest = null;
		try {
			digest = getDigest(new FileInputStream(file), md, 2048);
		} catch (NoSuchAlgorithmException | IOException e) {
			e.printStackTrace();
		}
		return hash.equals(digest.toLowerCase()); 
	}

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

