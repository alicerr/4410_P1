package ARR233;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

public abstract class SessionFetcher {
	public static final byte READ = 0, WRITE = 1, RETURNING_PACKET = 2, STORED_SESSION= 3;
	public static final short portProj1bRPC = 5300, MAX_BYTES_FOR_UDP = 512;
	public static final byte CALL_ID_OFFSET = 0, OPERATION_OFFSET = 4,  MESSAGE_OFFSET = 5;
	public static final byte FAILED_TO_RETRIEVE = -1;
	public static final byte SESSION_NOT_FOUND = -2;
	public static final byte NEWER_VERSION_IN_TABLE = -3;
	public static final float FACTOR_TO_CHECK = 1.5f;

	public static SimpleEntry fetchSession(int callID, long sessionID, InetAddress[] destAddrs){
			ByteBuffer request = ByteBuffer.allocate(13);
			request.putInt(CALL_ID_OFFSET, callID);
			request.put(OPERATION_OFFSET, READ);
			request.putLong(MESSAGE_OFFSET, sessionID);
			byte[] requestMessage = request.array();
			DatagramSocket rpcSocket;				
			SimpleEntry sessionFetched = null;
			try {
				rpcSocket = new DatagramSocket();
				
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
					      
					    } while( ByteBuffer.wrap(recvPkt.getData()).getInt(CALL_ID_OFFSET) == callID);
					  } catch(SocketTimeoutException store) {
					    // timeout 
					    
					  } catch(IOException ioe) {
					    // other error 
					    
					  }

				if (recvPkt != null && (recvPkt.getData()[OPERATION_OFFSET] == RETURNING_PACKET))
					try {
						sessionFetched = new SimpleEntry(sessionID, ByteBuffer.wrap(recvPkt.getData()));
					} catch (Exception e){
						//TODO
					}			
				rpcSocket.close();
			} catch (SocketException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch(IOException ioe) {
			    // other error 
			    
			}

			return sessionFetched;
	}
	
	public static List<InetAddress> writeSession(SimpleEntry session, InetAddress[] destAddrs, int callID, ViewManager vm){
		ByteBuffer request = ByteBuffer.allocate(MAX_BYTES_FOR_UDP);
		request.putInt(CALL_ID_OFFSET, callID);
		request.put(OPERATION_OFFSET, WRITE);
		session.fillBufferForUDP(request);		
		byte[] requestMessage = request.array();
		
		Enumeration<SimpleServer> servers = vm.getServers();
		List<InetAddress> tryThisRound = new ArrayList<InetAddress>();
		
		List<InetAddress> stored = new ArrayList<InetAddress>();
		int k = 2;
		int numServersToTryPerRound =  (int) ((k - stored.size() -1)* FACTOR_TO_CHECK + .999);
		for (int i = 0; tryThisRound.size() < numServersToTryPerRound  && i < destAddrs.length; i++){
			if (vm.getStatus(destAddrs[i]) == SimpleServer.status_state.UP)
				tryThisRound.add(destAddrs[i]);
		}
		
		DatagramSocket rpcSocket;
		
		
		try {
			rpcSocket = new DatagramSocket();
			do{
				
				numServersToTryPerRound =  (int) ((k - stored.size() -1 )* FACTOR_TO_CHECK + .999);
				while (tryThisRound.size() < numServersToTryPerRound  && servers.hasMoreElements()){
					
					SimpleServer nextServer = servers.nextElement();
					if (nextServer.status == SimpleServer.status_state.UP && !contains(destAddrs, nextServer.serverID))
						tryThisRound.add(nextServer.serverID);
				}
				for( InetAddress destAddr : tryThisRound ) {
					    DatagramPacket sendPkt = new DatagramPacket(requestMessage, requestMessage.length, destAddr, portProj1bRPC);
					    rpcSocket.send(sendPkt);
				}
				byte [] inBuf = new byte[512];
				DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
				
				
				try {
					do {
					      recvPkt.setLength(inBuf.length);
					      rpcSocket.receive(recvPkt);
					      ByteBuffer response = ByteBuffer.wrap(recvPkt.getData());
					      if (response.getInt(CALL_ID_OFFSET) == callID){
					    	  InetAddress recievedIP = recvPkt.getAddress();
					    	  vm.addServer(new SimpleServer(recievedIP));
					    	  byte operationPerformed = response.get(OPERATION_OFFSET);
					    	  if (response.getInt(CALL_ID_OFFSET) == callID && operationPerformed == NEWER_VERSION_IN_TABLE || operationPerformed == STORED_SESSION){
					    		  stored.add(recievedIP);
					    	  	  tryThisRound.remove(recievedIP);
					    	  }
					      }
					    } while( stored.size() < k - 1 );
					  } catch(SocketTimeoutException store) {
					    for (InetAddress failure : tryThisRound) 
					    	vm.addServer(new SimpleServer(failure, new Date().getTime(), SimpleServer.status_state.DOWN));
					  } catch(IOException ioe) {
					    // TODO 
					    
					  }
			} while (stored.size() < k - 1 && servers.hasMoreElements());
			rpcSocket.close();
	
				
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch(IOException ioe) {
		    // other error 
		    
		}

		
		
	return stored;
	
	}

	private static boolean contains(InetAddress[] destAddrs,
			InetAddress serverID) {
		// TODO Auto-generated method stub
		for (int i = 0; i < destAddrs.length; i++)
			if (destAddrs[i].equals(serverID))
					return true;
			
		return false;
	}
}
