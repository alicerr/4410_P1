package ARR233;

import java.util.Enumeration;
import java.util.Random;

public class GossipService extends Thread {
	public static final int GOSSIP_SECS = 20000;
	private ViewManager vm;
	
	public GossipService(ViewManager vim) {
		vm = vim;
	}
	
	@Override
	public void run() {
		Random generator = new Random();
		
		while(true) {
			
			// pick a random server to gossip with (including the DB)
			int numServers = vm.size();
			// numServers + 1 -> 0-(size-1) are servers, size = databaseGossip
			int serverToGossip = generator.nextInt(numServers+1);
			
			if(serverToGossip == numServers) { // lets chat with the DB
				SessionFetcher.sessionMergerDB(vm);
				System.out.println("Gossiping with database");
				
				
			} else { // pick a random server
				Enumeration<SimpleServer> serverEnum = vm.getServers();
				SimpleServer theLuckyOne = null;
				int i = 0;
				while(serverEnum.hasMoreElements()) {
					theLuckyOne = serverEnum.nextElement();
					// Check that we are at the 'randomly' chosen server AND it is believed to be UP
					if(i >= serverToGossip && theLuckyOne.status == SimpleServer.status_state.UP){
						break;
					}
					i++;
				}
				// I have a server that is believed to be up Or the last server, which could be down
				System.out.println("Gossiping with " + theLuckyOne);
				SessionFetcher.sessionMerger(vm, theLuckyOne);
			}
			
			
			try {
				Thread.sleep( (GOSSIP_SECS/2) + generator.nextInt( GOSSIP_SECS));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	

}
