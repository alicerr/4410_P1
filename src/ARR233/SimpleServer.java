package ARR233;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;

public class SimpleServer implements Comparable<SimpleServer>{
	public final InetAddress serverID;
	public enum status_state { UP, DOWN };
	public final long time_observed;
	public final status_state status;
	public SimpleServer(InetAddress serverID){
		time_observed = new Date().getTime();
		this.serverID = serverID;
		status = status_state.UP;
		//                int serverID = ByteBuffer.wrap(packet.getAddress().getAddress()).getInt();
	}

	public SimpleServer(InetAddress serverID, long time_observed, status_state status){
		this.time_observed = time_observed;
		this.serverID = serverID;
		this.status = status;
	}

	@Override
	public int compareTo(SimpleServer o) {
		int comp = Arrays.toString(serverID.getAddress()).compareTo(Arrays.toString(o.serverID.getAddress()));
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
		return(Arrays.toString(this.serverID.getAddress()).hashCode());
	}
	public String key(){
		return Arrays.toString(serverID.getAddress());
	}
	
}
