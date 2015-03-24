package ARR233;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.ListDomainsResult;

public class Tests extends SessionFetcher{
	static String domain = "Project1bViews";
	
	public static void main(String[] args) throws Exception {
		
		WhatIsInTheDB();
		//TestDeleteDomain();
		//TestSimpleDBHandler();
		//TestMergeDB();
		
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
		
}

