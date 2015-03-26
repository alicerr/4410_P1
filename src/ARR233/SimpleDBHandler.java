package ARR233;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.BatchPutAttributesRequest;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DeleteDomainRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.ReplaceableItem;
import com.amazonaws.services.simpledb.model.SelectRequest;

public class SimpleDBHandler {
	private AmazonSimpleDB db;
	
	public SimpleDBHandler(String credentialsFile, boolean isAliceComputer) {
		this.connectToAccount(credentialsFile, isAliceComputer);
	}
	public boolean dbNotNull(){ return db != null; }
	public boolean connectToAccount(String credentialsFile, boolean isAliceComputer){
		boolean success = false;
		System.out.println(credentialsFile);
		try {
			InputStream is = isAliceComputer? new FileInputStream(credentialsFile) : SimpleDBHandler.class.getResourceAsStream(credentialsFile);
			System.out.println(is);
			db = new AmazonSimpleDBClient(new PropertiesCredentials(is));
		} catch (IOException e) {
			e.printStackTrace();
			success = false;
			System.out.println("Could not read keys from file");
		} catch (NullPointerException e) {
			success = false;
			e.printStackTrace();
			System.out.println("Could not find credentials file");
		}
		success = true;
		
		return success;
	}
	
	public boolean domainExists(String domain) {
		List<String> domains = db.listDomains().getDomainNames();
		return domains.contains(domain);
	}
	
	public void domainExistsOrCreate(String domain) {
		if(!domainExists(domain)) {
			createDomain(domain);
			System.out.println("New SimpleDB Domain Created");
		}
	}

	private List<SimpleServer> getAllViewData(String domain) {
		List<SimpleServer> serverList = new ArrayList<SimpleServer>();
		String selectExpression = "select * from " + domain;
		SelectRequest sr = new SelectRequest(selectExpression);
		for (Item item : db.select(sr).getItems()) {
			for(Attribute attribute : item.getAttributes()) {
				serverList.add(new SimpleServer(attribute.getValue()));
			}
		}
		return serverList;
	}
	
	public ViewManager getDBViews(String domain) {
		List<SimpleServer> servers = this.getAllViewData(domain);
		ViewManager vm = new ViewManager();
		for(SimpleServer s : servers) {
			vm.addServer(s);
		}
		return vm;
	}
	
	public void addServerToDBViews(String domain, SimpleServer ss) {
		List<ReplaceableAttribute> data = new ArrayList<ReplaceableAttribute>();
		data.add(new ReplaceableAttribute().withName("ServerView").withValue(ss.toString()).withReplace(true));
		//db.batchPutAttributes(new BatchPutAttributesRequest(domain, data));
		db.putAttributes(new PutAttributesRequest().withDomainName(domain).withItemName(Integer.toString(ss.serverID)).withAttributes(data));
		
	}

	public void createDomain(String domain) {
		CreateDomainRequest request = new CreateDomainRequest(domain);
		System.out.println(db);
		db.createDomain(request);
	}
	
	public void deleteDomain(String domain) {
		db.deleteDomain(new DeleteDomainRequest(domain));
	}

	public void updateDBViews(String domain, ViewManager vm) {
		List<ReplaceableItem> data = new ArrayList<ReplaceableItem>();
		Enumeration<SimpleServer> serverEnum = vm.getServers();
		while(serverEnum.hasMoreElements()) {
			SimpleServer server = serverEnum.nextElement();
			data.add(new ReplaceableItem().withName(Integer.toString(server.serverID)).withAttributes(
					new ReplaceableAttribute().withName("ServerView").withValue(server.toString()).withReplace(true)));
		}
		db.batchPutAttributes(new BatchPutAttributesRequest(domain, data));
	}
}
