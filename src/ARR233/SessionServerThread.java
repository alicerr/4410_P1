package ARR233;

/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.*;
 
public class SessionServerThread extends Thread {
 
    protected DatagramSocket socket = null;
    private final SessionTable sessions;
    private final boolean[] keepGoing;
    private final ViewManager vm;

 
    public SessionServerThread(SessionTable sessions, ViewManager vm, boolean[] keepGoing) throws IOException {
        super("SessionServerThread");
        this.sessions = sessions;
        socket = new DatagramSocket(5300);
        this.vm = vm;
        System.out.println("session server thread initialized");
        this.keepGoing = keepGoing;

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
                int callID = data.getInt(SessionFetcher.CALL_ID_OFFSET);
                response.putInt(SessionFetcher.CALL_ID_OFFSET, callID);
                byte opCode = data.get(SessionFetcher.OPERATION_OFFSET);
                if (opCode == SessionFetcher.WRITE){
                	SimpleEntry session = new SimpleEntry(data); 
                	boolean success = sessions.put(session);
                	if (success){
                		response.put(SessionFetcher.OPERATION_OFFSET, SessionFetcher.STORED_SESSION);
                	} else {
                		response.put(SessionFetcher.OPERATION_OFFSET, SessionFetcher.NEWER_VERSION_IN_TABLE);
                	}
                	
                } else if (opCode == SessionFetcher.READ) {
                	long sessionID = data.getLong(SessionFetcher.MESSAGE_OFFSET);
                	SimpleEntry session = sessions.get(sessionID);
                	if (session != null){
                		response.put(SessionFetcher.OPERATION_OFFSET, SessionFetcher.RETURNING_PACKET);
                		session.fillBufferForUDP(response);
                	} else {
                		response.put(SessionFetcher.OPERATION_OFFSET, SessionFetcher.SESSION_NOT_FOUND);
                		response.putLong(SessionFetcher.MESSAGE_OFFSET, sessionID);
                	}
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
 
    
}


