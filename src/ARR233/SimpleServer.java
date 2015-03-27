package ARR233;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
/**
 * A immutable minimal server class
 * @author Alice
 *
 */
public class SimpleServer implements Comparable<SimpleServer>{
	/**
	 * The sever IP (IP represented as an integer
	 */
	public final int serverID;
	/**
	 * the state of the server (UP or DOWN)
	 * @author Alice
	 *
	 */
	public enum status_state { UP, DOWN };
	/**
	 * The last time the server was observed
	 */
	public final long time_observed;
	/**
	 * The status of this server
	 */
	public final status_state status;
	/**
	 * Init from packet with start index
	 * @param b
	 * @param start
	 */
	public SimpleServer(ByteBuffer b, int start){
		serverID = b.getInt(start);
		time_observed = b.getLong(start + 4);
		status = b.get(start + 12) == (byte) 1? status_state.UP : status_state.DOWN;
	}
	/**
	 * Init form Inet, update time
	 * @param serverID
	 */
	public SimpleServer(InetAddress serverID){
		time_observed = System.currentTimeMillis();
		this.serverID = inetToInt(serverID);
		status = status_state.UP;
	}
	/**
	 * Inet from data
	 * @param serverID
	 * @param time_observed
	 * @param status
	 */
	public SimpleServer(InetAddress serverID, long time_observed, status_state status){
		this.time_observed = time_observed;
		this.serverID = inetToInt(serverID);
		this.status = status;
	}
	
	/**
	 * Init form int, update time
	 * @param serverID
	 */
	public SimpleServer(int serverID) {
		this.serverID = serverID;
		this.time_observed = System.currentTimeMillis();
		this.status = status_state.UP;
	}
	/**
	 * Init for int, status, update time
	 * @param serverID
	 * @param status
	 */
	public SimpleServer(int serverID, status_state status) {
		this.serverID = serverID;
		this.time_observed = System.currentTimeMillis();
		this.status = status;
	}
	
	/**
	 * Init with the output of another servers toString
	 * @param serverData the toString output from a server
	 */
	public SimpleServer(String serverData) {
		//<SERVERID, Up/Down, EEE MMM d HH:mm:ss zzz yyyy>
		String data = serverData.substring(1, serverData.length()-1);
		//SERVERID, Up/Down, EEE MMM d HH:mm:ss zzz yyyy		
		String[] parts = data.split(",");
		this.serverID = Integer.parseInt(parts[0]);
		String upDown = parts[1];
		if(upDown.equals("Up")){
			this.status = status_state.UP;
		} else {
			this.status = status_state.DOWN;
		}
		DateFormat format = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy", Locale.ENGLISH);
		Date date = null;
		try {
			date = format.parse(parts[2]);
		} catch (ParseException e) {
			// This shouldn't happen because we control how this string is formed
		}
		this.time_observed = date.getTime();
	}
	/**
	 * Converts an IPV4 inet address to an int
	 * @param inetAddress
	 * @return
	 */
	public static int inetToInt(InetAddress inetAddress){
		return ByteBuffer.wrap(inetAddress.getAddress()).getInt();
	}
	/**
	 * Most recent observation comes first, if ID matches
	 * @param address
	 * @return
	 * @throws UnknownHostException
	 */
	public static InetAddress intToInet(int address) throws UnknownHostException{
		return InetAddress.getByAddress(ByteBuffer.allocate(4).putInt(address).array());
	}
	/**
	 * Comparison method based on ID and time last observed
	 */
	public int compareTo(SimpleServer o) {
		int comp = Integer.compare(this.serverID, o.serverID);
		if (comp == 0)
			return Long.compare(time_observed, o.time_observed);
		else 
			return comp;
	}
	/**
	 * String <ServerID, status, time observed in Date format>
	 */
	public String toString(){
		return "<" + serverID + "," + (isUp() ? "Up" : "Down" ) + "," + new Date(time_observed).toString() + ">"; 
	}
	/**
	 * Status checker
	 * @return
	 */
	private boolean isUp() {
		// TODO Auto-generated method stub
		return status == status_state.UP;
	}
	/**
	 * Inet address of this server
	 * @return
	 */
	public InetAddress serverAddress(){
		try {
			return intToInet(this.serverID);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * Represents this server as 13 bytes
	 * @return
	 */
	public byte[] simpleServerByteArray(){
		ByteBuffer b = ByteBuffer.allocate(13);
		b.putInt(serverID);
		b.putLong(4, time_observed);
		b.put(12, status == status_state.UP ? (byte) 1 : (byte) 0);
		return b.array();
		
	}
}
