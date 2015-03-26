package ARR233;
/*
 * Takes care of views
 */
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;

import ARR233.SimpleServer.status_state;
public class ViewManager  {

	private ConcurrentHashMap<Integer, SimpleServer> servers = new ConcurrentHashMap<Integer, SimpleServer>();
	public final int localAddress;
	/**
	 * If there are no servers up we can probably stop trying
	 */
	private final AtomicInteger runningServers = new AtomicInteger(0);
	private final AtomicInteger size = new AtomicInteger(0);
	/**
	 * used to select a 'random' server
	 */
	private final ConcurrentLinkedQueue<Integer> serverList = new ConcurrentLinkedQueue<Integer>();
	/**
	 * needs to know local address
	 * @param loc
	 */
	public ViewManager(int loc){
		localAddress = loc;
		// The first server in a new servers view manager should be itself.
		this.addServer(new SimpleServer(loc));
	}
	
	/**
	 * This should only ever be used by the simpleDB when creating it's own view.
	 */
	public ViewManager() {
		localAddress = 0;
	}
	/**
	 * Add a new server. will ad if not locol, or outdated
	 * @param newServer
	 * @return
	 */
	public boolean addServer(SimpleServer newServer){
		SimpleServer oldServer = servers.putIfAbsent(newServer.serverID, newServer);
		boolean success = oldServer == null;
		//if someone has declared this server dead then declare it back alive
		if (localAddress == newServer.serverID && newServer.status == status_state.DOWN){
			newServer = new SimpleServer(localAddress);
		}
		if (success){
			size.incrementAndGet();
			serverList.add(newServer.serverID);
			if (newServer.status == SimpleServer.status_state.UP){
				runningServers.incrementAndGet();
			}
		} else {
			boolean outdated = false;
			while (!outdated && !success){	
				//TODO test loop
				//has the server we are trying to insert been outdated?
				outdated = oldServer.time_observed >= newServer.time_observed;
				if(!outdated) { 
					// We need to replace
					success = servers.replace(oldServer.serverID, oldServer, newServer);
				}
				//if replacment wasn't successful because someone else has updated this 
				//before we read it well read the new value & try again
				oldServer = success || outdated ? oldServer : servers.get(newServer.serverID);
				//if replacment was successful
				if (success){
					if (oldServer.status == SimpleServer.status_state.UP && newServer.status == SimpleServer.status_state.DOWN){
						runningServers.decrementAndGet();
					} else if (oldServer.status == SimpleServer.status_state.DOWN && newServer.status == SimpleServer.status_state.UP){
						runningServers.incrementAndGet();
					}
				}
			}
		}
		return success;
	}
	/**
	 * Number of servers
	 * @return
	 */
	public int size() {
		return size.get();
	}
	/**
	 * Server enumeration
	 * @return
	 */
	public Enumeration<SimpleServer> getServers() {
		return servers.elements();
	}
	/**
	 * Merge in another view
	 * @param o the other view
	 * @return the number of servers retained from the passed in view
	 */
	public int merge(ByteBuffer o){
		short length = o.getShort(SessionFetcher.MESSAGE_OFFSET);
		short start = 2 + SessionFetcher.MESSAGE_OFFSET;
		int addedOrUpdated = 0;
		while (start < length){
			SimpleServer server = new SimpleServer(o, start);
			addedOrUpdated += addServer(server)? 1 : 0;
			start += 13;
		}
		return addedOrUpdated;
	}
	
	/**
	 * This should only be called when merging with the simpleDB
	 * @param dbvm - vm from DB
	 * @return
	 */
	public int merge(ViewManager dbvm) {
		int addedOrUpdated = 0;
		Enumeration<SimpleServer> serverEnum = dbvm.getServers();
		while(serverEnum.hasMoreElements()) {
			SimpleServer server = serverEnum.nextElement();
			addedOrUpdated += addServer(server)? 1 : 0;
		}
		return addedOrUpdated;
	}
	/**
	 * Just get the status of a specific server
	 * @param integer serverID
	 * @return
	 */
	public SimpleServer.status_state getStatus(Integer integer) {
		if (servers.containsKey(integer)){
			return servers.get(integer).status;
		} else {
			addServer(new SimpleServer(integer));
			return SimpleServer.status_state.UP;
		}
	}
	/**
	 * Load the servers (up to 38) onto a UDP packet for shipping
	 * @param b
	 * @return
	 */
	public ByteBuffer getServerSet(ByteBuffer b){
		Enumeration<SimpleServer> f = servers.elements();
		short start = SessionFetcher.MESSAGE_OFFSET + 2; //start at index 2
		//add all up elements
		while(f.hasMoreElements() && start < SessionFetcher.MAX_BYTES_FOR_UDP){
			SimpleServer s = f.nextElement();
			if (s.status == SimpleServer.status_state.UP){
				byte[] serverAsByteArray = s.simpleServerByteArray();
				for (byte t : serverAsByteArray){
					b.put(start++, t);
				}
			}
		}
		f = servers.elements();
		//add all down servers
		while(f.hasMoreElements() && start < SessionFetcher.MAX_BYTES_FOR_UDP){
			SimpleServer s = f.nextElement();
			if (s.status == SimpleServer.status_state.DOWN){
				byte[] serverAsByteArray = s.simpleServerByteArray();
				for (byte t : serverAsByteArray){
					b.put(start++, t);
				}
			}
		}
		b.putShort(SessionFetcher.MESSAGE_OFFSET, start);
		return b;
		
	}
	/**
	 * Are there any 'UP' servers? Server is assumed to know that it is up.
	 * @return
	 */
	public boolean hasUpServers() {
		return runningServers.get() > 1;
	}
	public SimpleServer getAServer(){
		int i = serverList.remove();
		serverList.add(i); //move i to tail
		return servers.get(i);
		
	}
	/**
	 * 
	 */
	public String toString(){
		String s = "Size: " + size.toString() +", Active size: " + runningServers.toString() + "\n";
		Enumeration<SimpleServer> e = getServers();
		while (e.hasMoreElements())
			s += e.nextElement().toString() + "\n";
		return s;
	}
	

}
