package ARR233;



import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A session table using a concurrency hashmap that implements read and write locks (based on my understanding of the docs)
 * Should be initialized by a listener that will clear the table after a given time of sessions expired 
 * after some amount of time
 * 
 * @author Alice/Spencer (arr233)
 *
 */
public class SessionTable implements Runnable {
	/**
	 * the table to hold the sessions
	 */
	private ConcurrentHashMap<Long, SimpleEntry> table = new ConcurrentHashMap<Long, SimpleEntry>();
    /**
     * Retrieve a session by ID
     * @param id The session ID to be retrieved
     * @return the session if found, otherwise null
     */
    public SimpleEntry get(long id){
    	return table.get(id);
    }
    /**
     * Store or update a session
     * @param session to be stored
     * @return
     */
    public boolean put(SimpleEntry session){
    	SimpleEntry oldEntry = table.putIfAbsent(session.sid, session);
    	boolean success = oldEntry == null;
    	if (!success){
    		boolean outdated = false;
    		//we will try to replace the session until we are outdated or successful
    		while (!outdated && !success){
    			//you can't un-retire (-1) a session, or replace it with an version that isn't newer
    			outdated = (oldEntry.vn >= session.vn || oldEntry.vn == -1) && session.vn > -1;  
    			if (!outdated){
    				success = table.replace(session.sid, oldEntry, session);
    				if (!success){
    					oldEntry = table.get(session.sid);
    				}
    			}
    		}		
    	}
    	return success;
    }
    /**
     * A runnable service to be called by a listener removing entries that are more than two days past expiration
     * Does not remove retired sessions until the above condition is met 
     * 
     * @return number of sessions removed
     */
    public long removeOldEntries(){
    	long removed = 0;
    	Enumeration<SimpleEntry> sessions = table.elements();
    	
       	while (sessions.hasMoreElements()){
       		try{
       			SimpleEntry session = sessions.nextElement();
       			if (session.isExpired()){
       				table.remove(session.sid);
       				removed++;
       			}
       		} catch (Exception e) {
       			e.printStackTrace();
       		}
    	}
       	return removed;
    
    }

	/**
	 * runnable holder of removal service
	 */
	@Override
	public void run() {
		long removed = removeOldEntries();
		System.out.println("Table Update complete, # sessions removed: " + removed);
	}
	/**
	 * Prints session table, used for debugging
	 */
	public String toString(){
		String s = "Session Table Contents:\n";
		Enumeration<SimpleEntry> e = table.elements();
		while (e.hasMoreElements()){
			s += e.nextElement().toString() + "\n";
		}
		return s;
	}


    
    
    

}
