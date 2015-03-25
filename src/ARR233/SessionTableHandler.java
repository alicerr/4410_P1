package ARR233;



import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Application Lifecycle Listener implementation class SessionTableHandler
 * Sets up all needed vars
 *
 */
@WebListener
public class SessionTableHandler implements ServletContextListener {
	

	    /**
	     * the thread pool to remove old sessions
	     */
	    private ScheduledThreadPoolExecutor executor = null;
	    /**
	     * pool to merge old sessions
	     */
	    private ScheduledThreadPoolExecutor merger = null;
	    
	    /**
	     *  thread to listen to the port for new request
	     */
	    private SessionServerThread listener = null;
	    /**
	     *  Runnable service to take care of gossiping
	     */
	    private GossipService gossiper = null;
	    /**
	     * Shutdown initiator for ^
	     */
		private final boolean[] keepListenerAlive = {true};
		/* (non-Javadoc)
		 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
		 */
		@Override
		public void contextInitialized(ServletContextEvent sce) {
			executor = new ScheduledThreadPoolExecutor(1);
			merger = new ScheduledThreadPoolExecutor(2);
			SessionTable sessions = new SessionTable();
			System.out.println(sessions);
			ViewManager vm = null; 
			InetAddress IP;
			SimpleDBHandler dbhandle;
			try {
				IP = InetAddress.getLocalHost();
				vm = new ViewManager(SimpleServer.inetToInt(IP));
				//TODO get amavon IP

				// Connect to SimpleDB and get all Views. Merge them. 
				//SessionFetcher.sessionMergerDB(vm);
				
				System.out.println("IP of my system is := "+IP.getHostAddress());
				
			} catch (UnknownHostException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			
			
			//get this from here later
			sce.getServletContext().setAttribute("sessions", sessions);
			sce.getServletContext().setAttribute("viewmanager", vm);
			//start tasks
			try {
				System.out.println("starting listener");
				listener = new SessionServerThread(sessions, vm, keepListenerAlive);
				listener.start(); 
				System.out.println("Set listener");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			//Check for sessions expired  once a day
			try {
			   System.out.println("starting processes");
	           executor.scheduleAtFixedRate(sessions, 
	                    30, 30, TimeUnit.MINUTES);
	           //merge handler
	           merger.scheduleAtFixedRate(vm, 30, 30, TimeUnit.MINUTES);
	           System.out.println("processes started");
	        } catch(Exception e) {
	           e.printStackTrace();
	        }
			
			System.out.println("Starting Gossip Service");
			gossiper = new GossipService(vm);
			gossiper.start();
			System.out.println("Gossip Service Started");
	        
		}
		/* (non-Javadoc)
		 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
		 */
		@Override
		public void contextDestroyed(ServletContextEvent arg0) {
			 executor.shutdown();
			 merger.shutdown();
			 keepListenerAlive[0] = false;
			 	
			 
		}
		
		
	
}
