package ARR233;

import java.util.Enumeration;
import java.util.Random;
/**
 * Gossip protocol service runner for innitiating node
 * @author Alice/Spencer
 *
 */
public class GossipService extends Thread {
	/**
	 * Milliseconds to wait between gossip rounds. Artificially high here
	 */
	public static final int GOSSIP_SECS = 30000;
	/**
	 * A link to the view manager for the owning server
	 */
	private ViewManager vm;
	/**
	 * Pass in the owning view manager
	 * @param vim
	 */
	public GossipService(ViewManager vim) {
		vm = vim;
	}
	/**
	 * Run the thread
	 */
	@Override
	public void run() {
		Random generator = new Random();
		
		// Start with a sleep
		try {
			Thread.sleep( (GOSSIP_SECS/2) + generator.nextInt( GOSSIP_SECS));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		while(true) {
			
			// pick a random server to gossip with (including the DB)
			int numServers = vm.runningServerSize();
			
			// numServers + 1 -> 0-(size-1) are servers, size = databaseGossip
			int serverToGossip = generator.nextInt(numServers+1);
			
			if(serverToGossip == numServers) { // lets chat with the DB
				SessionFetcher.sessionMergerDB(vm);
				System.out.println("Gossiping with database");
				
				
			} else { // pick a random server
				SimpleServer theLuckyOne = null;
				Enumeration<SimpleServer> serverEnum = vm.getServers();
				
				int i = 0;
				while(serverEnum.hasMoreElements()) {
					theLuckyOne = serverEnum.nextElement();
					// Check that we are at the 'randomly' chosen server AND it is believed to be UP
					if(theLuckyOne.status == SimpleServer.status_state.UP){
						i++;
						if(i==serverToGossip){
							break;
						}
					}
				}
				// I have a server that is believed to be up Or the last server, which could be down
				System.out.println("Gossiping with " + theLuckyOne);
				SessionFetcher.sessionMerger(vm, theLuckyOne);
				//System.out.println(vm);
			}
			
			
			try {
				Thread.sleep( (GOSSIP_SECS/2) + generator.nextInt( GOSSIP_SECS));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	

}
