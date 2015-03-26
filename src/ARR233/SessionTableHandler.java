package ARR233;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
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
			SessionTable sessions = new SessionTable();
			ViewManager vm = null; 
			InetAddress IP;
			try {
				String ipString = getIpAddress();
				IP = InetAddress.getByName(ipString);
				vm = new ViewManager(SimpleServer.inetToInt(IP));
				// Connect to SimpleDB and get all Views. Merge them. 
				SessionFetcher.sessionMergerDB(vm);
				System.out.println("IP of my system is := "+IP.getHostAddress());
			} catch (UnknownHostException e2) {
				e2.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			//get this from here later
			sce.getServletContext().setAttribute("sessions", sessions);
			sce.getServletContext().setAttribute("viewmanager", vm);
			//start  tasks
			try {
				System.out.println("starting listener");
				listener = new SessionServerThread(sessions, vm, keepListenerAlive);
				listener.start(); 
				System.out.println("Set listener");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			//Check for sessions expired every 5 minutes
			try {
			   System.out.println("Session Cleanup Processes Starting");
	           executor.scheduleAtFixedRate(sessions, 
	                    5, 5, TimeUnit.MINUTES);
	           //merge handler
	           System.out.println("Session Cleanup Processes Started");
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
			 listener.kill();
		}
		/**
		 * Get remote IP address of local host
		 * @return the IP as a string
		 * @throws MalformedURLException
		 * @throws IOException
		 */
		public static String getIpAddress() throws MalformedURLException, IOException {
			URL myIP = new URL("http://myip.dnsomatic.com/");
			BufferedReader in = new BufferedReader(new InputStreamReader(myIP.openStream()));
			return in.readLine();
		}
		
		
	
}
