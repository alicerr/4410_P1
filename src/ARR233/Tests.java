package ARR233;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
/**
 * A testing run for the gossip prrotocol
 * @author Alice/Spencer
 *
 */
public class Tests extends SessionFetcher{
	static String domain = "Project1bViews";
	private static String ipAddr = "";
	
	public static void main(String[] args) throws Exception {
		
		int numServersToTryPerRound =  (int) ((SessionHandler.K - 0)* 1.5 + .999);
		System.out.println(numServersToTryPerRound);
		
		ipAddr = getMyIP();
		System.out.println(ipAddr);
		//WhatIsInTheDB();
		//TestDeleteDomain();
		//TestSimpleDBHandler();
		//TestMergeDB();
		// GOSSIP TESTS can't be run together. The socket is not closed by the test befoer
		//   which causes the 2nd test to error out.
		//TestGossip(false);
		//TestGossip(true);
		
	}
	/**
	 * Try to delete a domain
	 */
	private static void TestDeleteDomain() {
		SimpleDBHandler dbhandle = new SimpleDBHandler("AwsCredentials.properties",false);
		dbhandle.deleteDomain(domain);
		System.out.println("Deleted domain");
	}
	/**
	 * Setup the DB
	 * @return the handler for the DB
	 */
	private static SimpleDBHandler setUpDBandDomain() {
		SimpleDBHandler dbhandle = new SimpleDBHandler("AwsCredentials.properties",false);
		dbhandle.createDomain(domain);
		/*if(dbhandle.domainExists(domain)) {
			System.out.println(domain + " already existed.");
		} else {
			dbhandle.createDomain(domain);
			System.out.println("Created domain " + domain);
		}*/
		
		return dbhandle;
	}
	/**
	 * Print out the contents of the DB
	 */
	private static void WhatIsInTheDB() {
		SimpleDBHandler dbhandle = setUpDBandDomain();
		
		ViewManager vm = dbhandle.getDBViews(domain);
		System.out.println("Database contents and size: " + vm.size());
		printVM(vm);
		
	}
	/**
	 * Create some server objects to test with
	 * @return
	 * @throws UnknownHostException
	 */
	private static List<SimpleServer> createTestServerObjects() throws UnknownHostException {
		List<SimpleServer> servers = new ArrayList<SimpleServer>();
		servers.add(new SimpleServer(InetAddress.getByName("cornell.edu")));
		servers.add(new SimpleServer(InetAddress.getByName("marconysa.org"))); 
		servers.add(new SimpleServer(InetAddress.getByName("google.com")));
		return servers;
	}
	/**
	 * tests the number of servers in the DB
	 * @throws UnknownHostException
	 */
	private static void TestSimpleDBHandler() throws UnknownHostException {
		System.out.println("Testing DBHandler...");
		SimpleDBHandler dbhandle = setUpDBandDomain();
		
		List<SimpleServer> servers = createTestServerObjects();
		
		for (SimpleServer s : servers) {
			dbhandle.addServerToDBViews(domain,s);
		}
		
		ViewManager vm = dbhandle.getDBViews(domain);
		int numServers = vm.size();
		System.out.println("Number of Servers: " + numServers);
		printVM(vm);
	}
	/**
	 * Test a merge of two DBs
	 * @throws UnknownHostException
	 */
	private static void TestMergeDB() throws UnknownHostException {
		System.out.println("Testing MergeDB...");
		SimpleDBHandler dbhandle = setUpDBandDomain();
			
		ViewManager dbvm = dbhandle.getDBViews(domain);
		
		int serverID = SimpleServer.inetToInt(InetAddress.getLocalHost());
		System.out.println("My serverID: " + serverID);
		ViewManager localvm = new ViewManager(serverID);
		int numServers = localvm.size();
		System.out.println("Servers in local VM pre-merge: " + numServers);
		printVM(localvm);
		
		localvm.merge(dbvm);
		
		dbhandle.updateDBViews(domain, localvm);
		
		dbvm = dbhandle.getDBViews(domain);
		
		numServers = localvm.size();
		System.out.println("Servers in local VM post-merge: " + numServers);
		printVM(localvm);
		numServers = dbvm.size();
		System.out.println("Servers on simpleDB post-merge: " + numServers);
		printVM(dbvm);
	}
	/**
	 * Print a view manager to console
	 * @param vm
	 */
	private static void printVM(ViewManager vm) {
		Enumeration<SimpleServer> serverEnum = vm.getServers();
		while(serverEnum.hasMoreElements()) {
			System.out.println((SimpleServer)serverEnum.nextElement());
		}
	}
	/**
	 * Test the gossip protocol
	 * @param running
	 * @throws IOException
	 */
	private static void TestGossip(boolean running) throws IOException {
		System.out.println("Testing gossip with running server set to: " + running);
		SessionTable st = new SessionTable();
		//other vm
		// These are hardcoded - not sure how to get the sending address properly
		ViewManager myVM = new ViewManager(2130706433); // real serverID for localhost on my machine
		myVM.addServer(new SimpleServer(2130706435)); // Made up server (not valid) just here for gossip
		System.out.println("myVM");
		printVM(myVM);
		ViewManager otherRunningVM = new ViewManager(SimpleServer.inetToInt(InetAddress.getByName("google.com")));
		otherRunningVM.addServer(new SimpleServer(InetAddress.getByName("cornell.edu")));
		System.out.println("otherRunningVM");
		printVM(otherRunningVM);
		
		boolean[] go = {true};
		SessionServerThread thread = new SessionServerThread(st, otherRunningVM, go);
		if(running)
			thread.start();		
		
		SessionFetcher.sessionMerger(myVM);
		
		System.out.println("myVM");
		printVM(myVM);
		System.out.println("otherRunningVM");
		printVM(otherRunningVM);
		
		if(running)
			thread.kill();
		
		System.out.println("========================================================\n"
				+ "PLEASE STOP THIS PROCESS.... THREAD IS NOT KILLING ITSELF");
	}
	/**
	 * Get the remote IP of a server this is running on 
	 * @return
	 * @throws Exception
	 */
	private static String getMyIP() throws Exception {
		URL myIP = new URL("http://myip.dnsomatic.com/");
		BufferedReader in = new BufferedReader(new InputStreamReader(myIP.openStream()));
		return in.readLine();
	}

		
}

