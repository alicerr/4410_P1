package ARR233;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;

public class SimpleServer implements Comparable<SimpleServer>{
	public final int serverID;
	public enum status_state { UP, DOWN };
	public final long time_observed;
	public final status_state status;
	public SimpleServer(InetAddress serverID){
		time_observed = new Date().getTime();
		this.serverID = inetToInt(serverID);
		status = status_state.UP;
		//                int serverID = ByteBuffer.wrap(packet.getAddress().getAddress()).getInt();
	}

	public SimpleServer(InetAddress serverID, long time_observed, status_state status){
		this.time_observed = time_observed;
		this.serverID = inetToInt(serverID);
		this.status = status;
	}
	
	public static int inetToInt(InetAddress inetAddress){
		return ByteBuffer.wrap(inetAddress.getAddress()).getInt();
	}
	public static InetAddress intToInet(int address) throws UnknownHostException{
		return InetAddress.getByAddress(ByteBuffer.allocate(4).putInt(address).array());
	}
	@Override
	public int compareTo(SimpleServer o) {
		int comp = Integer.compare(this.serverID, o.serverID);
		if (comp == 0)
			return Long.compare(time_observed, o.time_observed);
		else 
			return comp;
	}
	public String toString(){
		
		return "<" + serverID + (isUp() ? "Up" : "Down" ) + ", " + new Date(time_observed).toString() + ">"; 
	}

	private boolean isUp() {
		// TODO Auto-generated method stub
		return status == status_state.UP;
	}
	@Override
	public int hashCode() {
		return(serverID);
	}
	public int key(){
		return serverID;
	}
	public InetAddress serverAddress(){
		try {
			return intToInet(this.serverID);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
