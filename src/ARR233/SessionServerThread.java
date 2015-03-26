package ARR233;

/*
 * Listener class
 */
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
 /**
  * Listener class 
  * @author Alice/Spencer
  *
  */
public class SessionServerThread extends Thread {
	/**
	 * the thread running the listener
	 */
	private Thread t;
	/**
	 * Socket to listen on
	 */
    private DatagramSocket socket = null;
    /**
     * Handle to session table
     */
    private final SessionTable sessions;
    /**
     * killing signal
     */
    private volatile boolean[] keepGoing;
    /**
     * Handle to view manager
     */
    private final ViewManager vm;

    /**
     * 
     * @param sessions handle to session table
     * @param vm handle to view manager
     * @param keepGoing handle to killing signal
     * @throws IOException
     */
    public SessionServerThread(SessionTable sessions, ViewManager vm, boolean[] keepGoing) throws IOException {
        super("SessionServerThread");
        this.sessions = sessions;
        socket = new DatagramSocket(SessionFetcher.portProj1bRPC);
        this.vm = vm;
        System.out.println("session server thread initialized");
        this.keepGoing = keepGoing; //stops the thread
    }
	 /**
	  * Start listening
	  */	
    public void run() {
        while (keepGoing[0] == true) {
            try {
                byte[] buf = new byte[512];
                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                vm.addServer(new SimpleServer(packet.getAddress()));
                //build response
                ByteBuffer response = ByteBuffer.allocate(512);
                ByteBuffer data = ByteBuffer.wrap(packet.getData());
                //callid
                int callID = data.getInt(SessionFetcher.CALL_ID_OFFSET);
                response.putInt(SessionFetcher.CALL_ID_OFFSET, callID);
                
                byte opCode = data.get(SessionFetcher.OPERATION_OFFSET);
                //op write
                if (opCode == SessionFetcher.WRITE){
                	SimpleEntry session = new SimpleEntry(data); 
                	System.out.println("Stored " +  (session == null ? "null" : session.toString().substring(1,20))  + " from " + packet.getAddress());
                	boolean success = sessions.put(session);
                	if (success){
                		response.put(SessionFetcher.OPERATION_OFFSET, SessionFetcher.STORED_SESSION);
                	} else {
                		//this is the only condition with a false response
                		response.put(SessionFetcher.OPERATION_OFFSET, SessionFetcher.NEWER_VERSION_IN_TABLE);
                	}
                } else if (opCode == SessionFetcher.READ) {
                	
                	long sessionID = data.getLong(SessionFetcher.MESSAGE_OFFSET);
                	SimpleEntry session = sessions.get(sessionID);
                	System.out.println("Sent in " + (session == null ? "null" : session.toString().substring(1,20))  + " to " + packet.getAddress());
                	if (session != null){
                		response.put(SessionFetcher.OPERATION_OFFSET, SessionFetcher.FOUND_SESSION);
                		session.fillBufferForUDP(response);
                	} else {
                		response.put(SessionFetcher.OPERATION_OFFSET, SessionFetcher.SESSION_NOT_FOUND);
                		response.putLong(SessionFetcher.MESSAGE_OFFSET, sessionID);
                	}
                	
                } else if (opCode == SessionFetcher.MERGE_VIEWS){
                    //count number of server IDs gained
                	int i = vm.merge(data);
                	System.out.println("merged in " + i + " servers from " + packet.getAddress());
                	response.put(SessionFetcher.OPERATION_OFFSET, SessionFetcher.MERGE_VIEW_RESPONSE);
                	//send merged view
                	vm.getServerSet(response);
                }
                
                
        // send the response to the client at "address" and "port"
                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                packet = new DatagramPacket(response.array(), 512, address, port);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket.close();
    }
    /**
     * Start running
     */
    public void start() {
        if (t == null)
        {
           t = new Thread (this);
           t.start();
        }
    }
    /**
     * kill
     */
    public void kill(){
    	keepGoing[0] = false;
    }
}


