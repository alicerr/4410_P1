package ARR233;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class Tests extends SessionFetcher{
	static String domain = "Project1bViews";
	
	public static void main(String[] args) throws Exception {
		
		//WhatIsInTheDB();
		TestDeleteDomain();
		//TestSimpleDBHandler();
		//TestMergeDB();
		// GOSSIP TESTS can't be run together. The socket is not closed by the test befoer
		//   which causes the 2nd test to error out.
		//TestGossip(false);
		//TestGossip(true);
		
	}

	private static void TestDeleteDomain() {
		SimpleDBHandler dbhandle = new SimpleDBHandler("AwsCredentials.properties");
		dbhandle.deleteDomain(domain);
		System.out.println("Deleted domain");
	}

	private static SimpleDBHandler setUpDBandDomain() {
		SimpleDBHandler dbhandle = new SimpleDBHandler("AwsCredentials.properties");
		if(dbhandle.domainExists(domain)) {
			System.out.println(domain + " already existed.");
		} else {
			dbhandle.createDomain(domain);
			System.out.println("Created domain " + domain);
		}
		
		return dbhandle;
	}
	
	private static void WhatIsInTheDB() {
		SimpleDBHandler dbhandle = setUpDBandDomain();
		
		ViewManager vm = dbhandle.getDBViews(domain);
		System.out.println("Database contents and size: " + vm.size());
		printVM(vm);
		
	}
	
	private static List<SimpleServer> createTestServerObjects() throws UnknownHostException {
		List<SimpleServer> servers = new ArrayList<SimpleServer>();
		servers.add(new SimpleServer(InetAddress.getByName("cornell.edu")));
		servers.add(new SimpleServer(InetAddress.getByName("marconysa.org"))); 
		servers.add(new SimpleServer(InetAddress.getByName("google.com")));
		return servers;
	}

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
	
	private static void printVM(ViewManager vm) {
		Enumeration<SimpleServer> serverEnum = vm.getServers();
		while(serverEnum.hasMoreElements()) {
			System.out.println((SimpleServer)serverEnum.nextElement());
		}
	}
	
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
		
}

