package ARR233;



import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Application Lifecycle Listener implementation class SessionTableHandler
 *
 */
@WebListener
public class SessionTableHandler implements ServletContextListener {


	    /**
	     * the thread pool that may be needed when I schedule more tasks & make this into a real server
	     */
	    private ScheduledThreadPoolExecutor executor = null;
	    private SessionServerThread listener = null;
		private final boolean[] keepListenerAlive = {true};
		/* (non-Javadoc)
		 * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
		 */
		@Override
		public void contextInitialized(ServletContextEvent sce) {
			executor = new ScheduledThreadPoolExecutor(3);
			SessionTable sessions = new SessionTable();
			System.out.println(sessions);
			ViewManager vm = new ViewManager();
			System.out.println(vm);
			sce.getServletContext().setAttribute("sessions", sessions);

			try {
				listener = new SessionServerThread(sessions, vm, keepListenerAlive);
				listener.start(); //TODO is one thread enough
				
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			//Check for sessions expired by 2 days once a day
			try {
	           executor.scheduleAtFixedRate(sessions, 
	                    1, 1, TimeUnit.DAYS);
	        } catch(Exception e) {
	           e.printStackTrace();
	        }
	        
		}
		/* (non-Javadoc)
		 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
		 */
		@Override
		public void contextDestroyed(ServletContextEvent arg0) {
			 executor.shutdown();
			 keepListenerAlive[0] = false;
			 	
			 
		}
	
}
