package ARR233;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;


/**
 * Handles UDP packet requests, recieving genrated responses, updating the view with these
 * @author Alice/Spencer
 *
 */
public abstract class SessionFetcher {
	/**
	 * Operation codes for the 
	 */
	public static final byte READ = 0, 
							 WRITE = 1, 
							 FOUND_SESSION = 2, 
							 STORED_SESSION= 3, 
							 MERGE_VIEWS = 4,
							 MERGE_VIEW_RESPONSE = 5,
							 SESSION_NOT_FOUND = -2,
							 NEWER_VERSION_IN_TABLE = -3;
	
	public static final short portProj1bRPC = 5300, MAX_BYTES_FOR_UDP = 512;
	/**
	 * (pre-message) Packet offsets (messaage offsets in SimpleEntry, SimpleServer)
	 */
	public static final byte CALL_ID_OFFSET = 0, 
			                 OPERATION_OFFSET = 4,  
			                 MESSAGE_OFFSET = 5;

	/**
	 * Given a need for X write responses we send out FACTOR TO CHECK * X requests, rounded up
	 */
	public static final float FACTOR_TO_CHECK = 1.5f;
	public static final short DATAGRAM_TIMEOUT = 5000;
	private static final String DB_DOMAIN = "Project1bViews";
	/**
	 * Return a session, if found at another server
	 * @param callID 
	 * @param sessionID
	 * @param destAddrs -- addresses to check
	 * @param vm
	 * @return SimpleEntry session or null if not retrieved
	 */
	public static SimpleEntry fetchSession(int callID, long sessionID, List<Integer> destAddrs, ViewManager vm){
			//build packet
			ByteBuffer request = ByteBuffer.allocate(13);
			request.putInt(CALL_ID_OFFSET, callID);
			request.put(OPERATION_OFFSET, READ);
			request.putLong(MESSAGE_OFFSET, sessionID); //session id is the message
			byte[] requestMessage = request.array();
			//send requests
			DatagramSocket rpcSocket;				
			SimpleEntry sessionFetched = null;
			try {
 				rpcSocket = new DatagramSocket();
 				rpcSocket.setSoTimeout(DATAGRAM_TIMEOUT);
 				//try all dest addresses
 				for(Integer destAddr : destAddrs ) {
 					if (destAddr.intValue() != vm.localAddress){
 						InetAddress destAddrInet = SimpleServer.intToInet(destAddr);
 					    DatagramPacket sendPkt = new DatagramPacket(requestMessage, requestMessage.length, destAddrInet, portProj1bRPC);
 					    rpcSocket.send(sendPkt);
 					}
 				}
				//collect response
				//collect first response
 				byte [] inBuf = new byte[512];
 				DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
 				ByteBuffer data = null;
 				try {
 					do {
 					      recvPkt.setLength(inBuf.length);
 					      rpcSocket.receive(recvPkt);
 					      vm.addServer(new SimpleServer(recvPkt.getAddress()));
 					      
 					      data = ByteBuffer.wrap(recvPkt.getData());
 					      if (data.getInt(CALL_ID_OFFSET) == callID)
 					    	  destAddrs.remove(recvPkt.getAddress()); //this server is alive at least 
 					    } while( !(data.getInt(CALL_ID_OFFSET) == callID && data.get(OPERATION_OFFSET) == FOUND_SESSION));
 					  } catch(SocketTimeoutException store) {
						  //down timedout servers
						 //down timedout servers
 					    for (Integer i : destAddrs)
 					    	vm.addServer(new SimpleServer(i,SimpleServer.status_state.DOWN));
 					  } catch(IOException ioe) {
 						  ioe.printStackTrace();
 					  }
 				//if response
 				if (recvPkt != null && (recvPkt.getData()[OPERATION_OFFSET] == FOUND_SESSION))
 					try {
 						sessionFetched = new SimpleEntry(ByteBuffer.wrap(recvPkt.getData()));
 					} catch (Exception e){
 						e.printStackTrace();
 					}			
 				rpcSocket.close();
 			} catch (SocketException e1) {
 				e1.printStackTrace();
 			} catch(IOException ioe) {
 			    ioe.printStackTrace();
 			}
 			return sessionFetched;
	}
	/**
	 * Write a session to remote servers
	 * @param session
	 * @param destAddrs -- preferred addresses (probably places where the session was already written)
	 * @param callID
	 * @param vm
	 * @return ArrrayList of the serverIDs the session was stored in
	 * 
	 */
	public static ArrayList<Integer> writeSession(SimpleEntry session, ArrayList<Integer> destAddrs, int callID, ViewManager vm) {
		ByteBuffer request = ByteBuffer.allocate(MAX_BYTES_FOR_UDP);
		request.putInt(CALL_ID_OFFSET, callID);
		request.put(OPERATION_OFFSET, WRITE);
		session.fillBufferForUDP(request);		
		byte[] requestMessage = request.array();
		/**
		 * for later
		 */
		Enumeration<SimpleServer> servers = vm.getServers();
		/**
		 * repopulate every round
		 */
		List<InetAddress> tryThisRound = new ArrayList<InetAddress>();
		//what weve stored in
		List<InetAddress> stored = new ArrayList<InetAddress>();
		//determine how many servers to check at once
		int numServersToTryPerRound =  (int) ((SessionHandler.K - stored.size())* FACTOR_TO_CHECK + .999);
		//add preffered addresses
		for (int i = 0; i < destAddrs.size(); i++){
			//try if up, if not self
			if (destAddrs.get(i).intValue() != vm.localAddress && vm.getStatus(destAddrs.get(i)) == SimpleServer.status_state.UP ){
				try {
					tryThisRound.add(SimpleServer.intToInet(destAddrs.get(i)));
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
			}
		}
		
		DatagramSocket rpcSocket;
		//send
		//send
 		try {
 			rpcSocket = new DatagramSocket();
 			rpcSocket.setSoTimeout(DATAGRAM_TIMEOUT);
			
 			do{
				
 				numServersToTryPerRound =  (int) ((SessionHandler.K - stored.size())* FACTOR_TO_CHECK + .999);
				//repopulae tryThisRound if needed
				//repopulate servers to tryThisRound if needed
 				while (tryThisRound.size() < numServersToTryPerRound  && servers.hasMoreElements()){
 					
 					SimpleServer nextServer = servers.nextElement();
 					InetAddress nextInetAddress = nextServer.serverAddress();
 					if (nextServer.serverID != vm.localAddress && nextServer.status == SimpleServer.status_state.UP && !destAddrs.contains(SimpleServer.inetToInt(nextInetAddress)))
 						tryThisRound.add(nextInetAddress);
 				}
 				//send packets
 				for( InetAddress destAddr : tryThisRound ) {
 					    DatagramPacket sendPkt = new DatagramPacket(requestMessage, requestMessage.length, destAddr, portProj1bRPC);
 					    rpcSocket.send(sendPkt);
 					    System.out.println("sent packet");
 				}
 				//recieve responses
 				byte [] inBuf = new byte[512];
 				DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
 				
 				
 				try {
 					do {
 					      recvPkt.setLength(inBuf.length);
 					      rpcSocket.receive(recvPkt);
 					      ByteBuffer response = ByteBuffer.wrap(recvPkt.getData());
 					      //check callID
 					      if (response.getInt(CALL_ID_OFFSET) == callID){
 					    	  InetAddress recievedIP = recvPkt.getAddress();
 					    	  vm.addServer(new SimpleServer(recievedIP));
 				    		  //server is working
 				    	  	  tryThisRound.remove(recievedIP);
 					    	  byte operationPerformed = response.get(OPERATION_OFFSET);
 					    	  //either of these is a success
 					    	  if (operationPerformed == NEWER_VERSION_IN_TABLE || operationPerformed == STORED_SESSION){
 					    		  stored.add(recievedIP);
 					    	  }
 					    	  System.out.println("Operation completed: " + operationPerformed);
 					      }
 					    } while( stored.size() < SessionHandler.K && tryThisRound.size() > 0); 
 					  } catch(SocketTimeoutException store) {
 					    for (InetAddress failure : tryThisRound) {
 					    	vm.addServer(new SimpleServer(failure, new Date().getTime(), SimpleServer.status_state.DOWN));

 	 						System.out.println("Server timeout:" + failure.getHostName());
 					    }
 					   tryThisRound = new ArrayList<InetAddress>();
 					  } catch(IOException ioe) {
 						  	ioe.printStackTrace();
 						  	tryThisRound = new ArrayList<InetAddress>();
 					  }
 			} while (stored.size() < SessionHandler.K && servers.hasMoreElements());
 			rpcSocket.close();
 		} catch (SocketException e1) {
 			e1.printStackTrace();
 		} catch(IOException ioe) {
 		    ioe.printStackTrace(); 
 		}
 	//convert stored to ints	
 	ArrayList<Integer> storedInSrvId = new ArrayList<Integer>();
 	for (InetAddress i : stored)
 		storedInSrvId.add(SimpleServer.inetToInt(i));
 	return storedInSrvId;
 	
	}
	
	/**
	 * This is the gossip protocol for the database
	 * @param vm the vm to update with the database vm
	 */
	public static void sessionMergerDB(ViewManager vm) {
		SimpleDBHandler dbhandle = new SimpleDBHandler("AwsCredentials.properties", false);
		if (!dbhandle.dbNotNull())
			dbhandle = new SimpleDBHandler("C:\\Users\\Alice\\Google Drive\\New folder\\Assignment1A\\AwsCredentials.properties", true);
		dbhandle.createDomain(DB_DOMAIN);
		ViewManager dbvm = dbhandle.getDBViews(DB_DOMAIN);
		vm.merge(dbvm);
		dbhandle.updateDBViews(DB_DOMAIN,vm);
	}
	
	/**
	 * This is a wrapper for sessionMerger for testing 
	 * @param vm view manager to gossip with
	 */
	public static void sessionMerger(ViewManager vm) {
		sessionMerger(vm,null);
	}
	
	/**
	 * Mergers sessions by intiating a UDP packet. Method is limited to 38 servers
	 * @param vm
	 * @param s server to gossip with
	 */
	public static void sessionMerger(ViewManager vm, SimpleServer s){
		if (!vm.hasUpServers()) return;
		// If null is passed in, we find one or just return, else if s is a real server we try to gossip with it
		while ((s == null || 
				s.status == SimpleServer.status_state.DOWN || 
				vm.localAddress == s.serverID) 
				&& vm.hasUpServers()){ 
			s = vm.getAServer();
		}
		//if there are no servers to gossip with
		if (s == null){
			return;
		}
		int callID = SessionHandler.generateCallID();
		ByteBuffer request = ByteBuffer.allocate(MAX_BYTES_FOR_UDP);
		request.putInt(CALL_ID_OFFSET, callID);
		request.put(OPERATION_OFFSET, MERGE_VIEWS);
		vm.getServerSet(request);		
		byte[] requestMessage = request.array();
		DatagramSocket rpcSocket;
		boolean firstIOE = false;
		do{
			try {
				rpcSocket = new DatagramSocket();
				rpcSocket.setSoTimeout(DATAGRAM_TIMEOUT);
				//fill and send socket
				InetAddress destAddrInet = SimpleServer.intToInet(s.serverID);
				DatagramPacket sendPkt = new DatagramPacket(requestMessage, requestMessage.length, destAddrInet, portProj1bRPC);
				rpcSocket.send(sendPkt);
			
				byte [] inBuf = new byte[512];
				DatagramPacket recvPkt = new DatagramPacket(inBuf, inBuf.length);
				try {
					do {
						  //get merged views from other server
					      recvPkt.setLength(inBuf.length);
					      rpcSocket.receive(recvPkt);
					      
					    } while(ByteBuffer.wrap(recvPkt.getData()).getInt(CALL_ID_OFFSET) != callID);
				  } catch(SocketTimeoutException store) {
					System.out.println("Server timeout: " + s);
				    vm.addServer(new SimpleServer(s.serverID, SimpleServer.status_state.DOWN));
				    
				  } catch(IOException ioe) {
				    ioe.printStackTrace();
				  }
				
				if (recvPkt != null && (recvPkt.getData()[OPERATION_OFFSET] == MERGE_VIEW_RESPONSE)){
					int merged = vm.merge(ByteBuffer.wrap(recvPkt.getData()));
					System.out.println("Sessions merged: " + merged);
				}
				rpcSocket.close();
			} catch (SocketException e1) {
				e1.printStackTrace();
			} catch(IOException ioe) {
			    // other error 
				ioe.printStackTrace();
				if (!firstIOE) firstIOE = true;
				else firstIOE = false;
			}
		} while (firstIOE);
	}
			
}
