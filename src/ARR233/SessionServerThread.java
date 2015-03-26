package ARR233;

/*
 * Listener class
 */
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
 
public class SessionServerThread extends Thread {
	private Thread t;
    protected DatagramSocket socket = null;
    private final SessionTable sessions;
    private volatile boolean[] keepGoing;
    private final ViewManager vm;

 
    public SessionServerThread(SessionTable sessions, ViewManager vm, boolean[] keepGoing) throws IOException {
        super("SessionServerThread");
        this.sessions = sessions;
        socket = new DatagramSocket(SessionFetcher.portProj1bRPC);
        this.vm = vm;
        System.out.println("session server thread initialized");
        this.keepGoing = keepGoing; //stops the thread
    }
 
    public void run() {
        while (keepGoing[0] == true) {
            try {
                byte[] buf = new byte[512];
                // receive request
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                vm.addServer(new SimpleServer(packet.getAddress()));
                
                ByteBuffer response = ByteBuffer.allocate(512);
                ByteBuffer data = ByteBuffer.wrap(packet.getData());
                //callid
                int callID = data.getInt(SessionFetcher.CALL_ID_OFFSET);
                response.putInt(SessionFetcher.CALL_ID_OFFSET, callID);
                
                byte opCode = data.get(SessionFetcher.OPERATION_OFFSET);
                //ops
                if (opCode == SessionFetcher.WRITE){
                	SimpleEntry session = new SimpleEntry(data); 
                	boolean success = sessions.put(session);
                	if (success){
                		response.put(SessionFetcher.OPERATION_OFFSET, SessionFetcher.STORED_SESSION);
                	} else {
                		//this is the only error anticipated atm
                		response.put(SessionFetcher.OPERATION_OFFSET, SessionFetcher.NEWER_VERSION_IN_TABLE);
                	}
                	
                } else if (opCode == SessionFetcher.READ) {
                	
                	long sessionID = data.getLong(SessionFetcher.MESSAGE_OFFSET);
                	
                	SimpleEntry session = sessions.get(sessionID);
                	
                	if (session != null){
                		response.put(SessionFetcher.OPERATION_OFFSET, SessionFetcher.FOUND_SESSION);
                		session.fillBufferForUDP(response);
                	} else {
                		response.put(SessionFetcher.OPERATION_OFFSET, SessionFetcher.SESSION_NOT_FOUND);
                		response.putLong(SessionFetcher.MESSAGE_OFFSET, sessionID);
                	}
                	
                } else if (opCode == SessionFetcher.MERGE_VIEWS){
                    //count num gained
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
    
    public void start() {
        if (t == null)
        {
           t = new Thread (this);
           t.start ();
        }
    }
    
    public void kill(){
    	keepGoing[0] = false;
    }
 
    
}


