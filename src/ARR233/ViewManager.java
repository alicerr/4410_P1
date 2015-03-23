package ARR233;

import java.net.InetAddress;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

public class ViewManager {

	private ConcurrentHashMap<InetAddress, SimpleServer> servers = new ConcurrentHashMap<InetAddress, SimpleServer>();
	int size = 0;

	public boolean addServer(SimpleServer newServer){
		//TOD avoid adding local address
		SimpleServer oldServer = servers.putIfAbsent(newServer.serverID, newServer);
		boolean success = oldServer == null;
		if (success){
			size++;
		} else {
			boolean outdated = false;
			while (!outdated && !success){
				
				outdated = oldServer.time_observed >= newServer.time_observed;
				success = servers.replace(newServer.serverID, newServer, oldServer);
				oldServer = success || outdated ? oldServer : servers.get(newServer.serverID);
			}
			
				
		}
		return success;
	}
	public int size() {
		return size();
	}
	public Enumeration<SimpleServer> getServers() {
		return servers.elements();
	}
	public int merge(ViewManager o){
		int addedOrUpdated = 0;
		for (SimpleServer so : o.servers.values()){
			addedOrUpdated += addServer(so) ? 1 : 0;
		}
		return addedOrUpdated;
	}
	public SimpleServer.status_state getStatus(InetAddress inetAddress) {
		if (servers.containsKey(inetAddress)){
			return servers.get(inetAddress).status;
		} else {
			addServer(new SimpleServer(inetAddress));
			return SimpleServer.status_state.UP;
		}
	}

}
