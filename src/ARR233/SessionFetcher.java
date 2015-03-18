package ARR233;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public abstract class SessionFetcher {
	public static final byte READ = 0, WRITE = 1; 
	public static final short portProj1bRPC = 5300;
	public static final byte CALL_ID_OFFSET = 0, OPERATION_OFFSET = 4,  MESSAGE_OFFSET = 5;
	public static void fetchSession(int callID, long sessionID, InetAddress[] destAddrs){
			ByteBuffer request = ByteBuffer.allocate(13);
			request.putInt(callID, CALL_ID_OFFSET);
			request.put(READ, OPERATION_OFFSET);
			request.putLong(sessionID, int(MESSAGE_OFFSET));
			byte[] requestMessage = request.array();
			for( InetAddress destAddr : destAddrs ) {
				    DatagramPacket sendPkt = new DatagramPacket(requestMessage, requestMessage.length, destAddr, portProj1bRPC);
				    rpcSocket.send(sendPkt);
			}
			byte [] inBuf = new byte[512];
			DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
			try {
				do {
				      recvPkt.setLength(inBuf.length);
				      rpcSocket.receive(recvPkt);
				      byte[] recMsg = getPacket().getData();
				    } while( ByteBuffer.wrap(recMsg) );
				  } catch(SocketTimeoutException store) {
				    // timeout 
				    recvPkt = null;
				  } catch(IOException ioe) {
				    // other error 
				    ...
				  }
				  return recvPkt;
				  
		
	}
}
