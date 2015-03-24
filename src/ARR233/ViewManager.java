package ARR233;
/*
 * Takes care of views
 */
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ViewManager  implements Runnable {

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
	}
	/**
	 * Add a new server. will ad if not locol, or outdated
	 * @param newServer
	 * @return
	 */
	public boolean addServer(SimpleServer newServer){
		//TOD avoid adding local address
		if (newServer.serverID == localAddress){
			return false;
		}
		SimpleServer oldServer = servers.putIfAbsent(newServer.serverID, newServer);
		boolean success = oldServer == null;
		if (success){
			size.incrementAndGet();
			serverList.add(newServer.serverID);
			if (newServer.status == SimpleServer.status_state.UP){
				runningServers.incrementAndGet();
			}
		} else {
			boolean outdated = false;
			while (!outdated && !success){
				
				outdated = oldServer.time_observed >= newServer.time_observed;
				success = servers.replace(newServer.serverID, newServer, oldServer);
				oldServer = success || outdated ? oldServer : servers.get(newServer.serverID);
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
	public int size() {
		return size.get();
	}
	public Enumeration<SimpleServer> getServers() {
		return servers.elements();
	}
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
	public SimpleServer.status_state getStatus(Integer integer) {
		if (servers.containsKey(integer)){
			return servers.get(integer).status;
		} else {
			addServer(new SimpleServer(integer));
			return SimpleServer.status_state.UP;
		}
	}
	 
	public ByteBuffer getServerSet(ByteBuffer b){
		Enumeration<SimpleServer> f = servers.elements();
		short start = SessionFetcher.MESSAGE_OFFSET + 2; //start at index 2
		while(f.hasMoreElements() && start < SessionFetcher.MAX_BYTES_FOR_UDP){
			byte[] serverAsByteArray = f.nextElement().simpleServerByteArray();
			for (byte t : serverAsByteArray){
				b.put(start++, t);
			}
		}
		b.putShort(SessionFetcher.MESSAGE_OFFSET, start);
		return b;
		
	}
	@Override
	public void run() {
		
		
	}
	public boolean hasUpServers() {
		return runningServers.get() > 0;
	}
	public SimpleServer getAServer(){
		int i = serverList.remove();
		serverList.add(i); //move i to tail
		return servers.get(i);
		
	}
	

}
