package ARR233;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.BatchPutAttributesRequest;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ListDomainsResult;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.ReplaceableItem;
import com.amazonaws.services.simpledb.model.SelectRequest;

public class SimpleDBHandler {
	private AmazonSimpleDB db;
	
	public SimpleDBHandler(String credentialsFile) {
		this.connectToAccount(credentialsFile);
	}
	
	public boolean connectToAccount(String credentialsFile){
		boolean success = false;
		try {
			InputStream is = RPCClients.class.getResourceAsStream(credentialsFile);
			db = new AmazonSimpleDBClient(new PropertiesCredentials(is));
		} catch (IOException e) {
			success = false;
			System.out.println("Could not read keys from file");
		} catch (NullPointerException e) {
			success = false;
			System.out.println("Could not find credentials file");
		}
		success = true;
		
		return success;
	}
	
	public boolean domainExists(String domain) {
		List<String> domains = db.listDomains().getDomainNames();
		return domains.contains(domain);
	}

	public List<SimpleServer> getAllViewData(String domain) {
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
	
	public void addViewToData(String domain, SimpleServer ss) {
		List<ReplaceableAttribute> data = new ArrayList<ReplaceableAttribute>();
		data.add(new ReplaceableAttribute().withName("ServerView").withValue(ss.toString()).withReplace(true));
		//db.batchPutAttributes(new BatchPutAttributesRequest(domain, data));
		db.putAttributes(new PutAttributesRequest().withDomainName(domain).withItemName(Integer.toString(ss.serverID)).withAttributes(data));
		
	}

	public void createDomain(String domain) {
		db.createDomain(new CreateDomainRequest(domain));
	}
}
