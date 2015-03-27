package ARR233;



import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Thread Safe Session Handler for Assignment 1a and 1b
 * Servlet implementation class SessionHandler
 * Handles retrieving data from cookies and generating the spropriate response
 * 
 * Prints out the session, session table view, cookie retrieved, server view, etc in DEBUG = true
 */
@WebServlet("/SessionHandler")
public class SessionHandler extends HttpServlet {
	/**
	 * Risiliancy to try for
	 */
	public static final byte K = 1;
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Session ID Generator TODO: make this not simple to guess
	 */
	private static final AtomicInteger SESSION_ID_GEN = new AtomicInteger(1);
	/**
	 * Session call ID Generator
	 */
	private static final AtomicInteger SESSION_CALL_GEN = new AtomicInteger(1);
	/**
	 * See Debugging Printouts
	 */
	private final boolean DEBUG = true;
	/**
	 * Html header and body beginning
	 */
	private static final String HTML_HEADER = "<!DOCTYPE html>\n"
											+ "<html lang=\"en-US\">\n"
											+ "<head>\n"
											+ "  <title>ARR233_CS5300_A1_SP2015</title>\n"
											+ "</head>\n"
											+ "<body>";
	/**
	 * Form beginning
	 */
	private static final String FORM_HEADER = "  <form action=\"SessionHandler\" method=\"POST\">";
	/**
	 * form closing tag
	 */
	private static final String FORM_FOOTER = "  </form>";
	/**
	 * html closing tag
	 */
	private static final String HTML_FOOTER = 
											  "</body>"
											+ "</html>";	
	/**
	 *  get and post response of this session handler
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		    throws IOException, ServletException
		    { 
                PrintWriter out = response.getWriter();
		        try{
		        	//Retrieve session table
		        	SessionTable sessions = (SessionTable)getServletContext().getAttribute("sessions");
		        	ViewManager vm = (ViewManager)getServletContext().getAttribute("viewmanager");
		        	
		        	//retrieve cookies
			        Cookie[] cookies = request.getCookies();
			        Cookie c = null;
			        //look for correct cookie
			        for (int i = 0; cookies != null && i < cookies.length && c == null; i++){
			        	
			        	if (cookies[i].getName().equals(SimpleEntry.COOKIE_NAME)){
			        		c = cookies[i];
			        	}
			        }
			        SimpleEntry session = null;
			        String erMsg = null;
			        String sesStateMsg = null;
			        String cVal = null; //cookie value
			        ArrayList<Integer> srvs = new ArrayList<Integer>();
			        int respondingServerID = -1;
			        if (c != null){
			        	System.out.println("found cookie");
			        	//get cookie info
			        	cVal = c.getValue();
			        	try {
			        		//parse JSOn
			        		JsonObject cValAsJson = new JsonParser().parse(cVal).getAsJsonObject();
			        		long cSessionID = Long.parseLong(cValAsJson.get("SID").getAsString());
			        		JsonArray srvsJA = cValAsJson.get("SRVS").getAsJsonArray();
			        		
			        		boolean isLocal = false;
			        		for (JsonElement js : srvsJA){	
			        			int serverID = js.getAsInt();
			        			
			        			if (serverID != vm.localAddress){
			        				srvs.add(js.getAsInt());
			        				// CHeck this - do we add here
			        				//vm.addServer(new SimpleServer(js.getAsInt()));
			        			} else {
			        				isLocal = true;
			        			}
			        		}
			        		//try to get local copy
			        		if (isLocal){ 
			        			session = sessions.get(cSessionID);
			        			respondingServerID = vm.localAddress;
			        		}
			        		//try to get remote copy
			        		if (session == null && srvs.size() > 0){
			        			System.out.println("checking remote servers");
			        			session = SessionFetcher.fetchSession(generateCallID(), cSessionID, srvs, vm, respondingServerID);
			        		}
			        		//no copies available or copy is 'dead'
			        		if (session == null) {
			        			erMsg = "Previous Session not found in System! x_x";
			        			// Reset srvs in the case when old cookie was found from previous server run
			        			srvs = new ArrayList<Integer>();
			        		} else if (session.isRetired()) {
			        			erMsg = "Session was already terminated! x_x";
			        			c = null;
			        		} else if (session.isExpired()) {
			        			erMsg = "Session has expired! x_x";
			        			c = null;
			        		}
			        	} catch (Exception e){
			        		if (DEBUG) {
			        			erMsg = e.getMessage();
			        		} else {
			        			erMsg = "Error Handling Request, Please Contact: [SysAdmin]";
			        		}
			        	}
			        	//if they want to retire it
			        	if (request.getParameter("retire") != null && session != null){
			                session =  new SimpleEntry(session, false);
			        		sessions.put(session);
			        		
			        		srvs = SessionFetcher.writeSession(session, srvs, generateCallID(), vm);

			        		session = null;
			        		sesStateMsg = "Session Terminated Successfully";
			        	//they want to update the message or extend the session
			        	} else if (session != null) { 
			        		Map<String, String[]> requestMap = request.getParameterMap();
			        		//update message
			        		if (requestMap.containsKey("replace")){
			        			session = new SimpleEntry(session, requestMap.get("new_message")[0]);
			        		} else {//extend session
			        			session = new SimpleEntry(session, true);
			        		} 
			        		sessions.put(session);
			        		srvs  = SessionFetcher.writeSession(session, srvs, generateCallID(), vm);
			        		srvs.add(vm.localAddress);
			        		// Ensure we keep K-resiient (1)
			        		srvs = manageSrvs(srvs,vm);
			        	}
			        }
			        //if we need to generate a new session
			        if (session == null){
			        	session = new SimpleEntry(generateSessionID(vm.localAddress));
			        	sessions.put(session);
			        	srvs.add(vm.localAddress);
			        	
			        	// we need this to be 1-resilient - if possible
			        	// at some point maybe K
			        	srvs = manageSrvs(srvs, vm);		        	
			        	
			        	sesStateMsg = sesStateMsg == null ? "New Session Started" : sesStateMsg + "; New Session Started";
			        }
			        //make the new cookie
			        Cookie cookie = session.getAsCookie(srvs);
			        response.addCookie(cookie);
			        response.setContentType("text/html");
			        //general
			        out.println(HTML_HEADER);
			        out.println(session.msg);
			        //form
			        out.println(FORM_HEADER);
			        out.println("    Please include html block elements, images, and formating in message   <br />");
			        out.println("    <input type=\"text\" name=\"new_message\" value=\"<p> </p>\" >\n<br />");
			        out.println("    <input type=\"submit\" value=\"Replace Message\" name=\"replace\" />\n");
			        out.println("    <input type=\"submit\" value=\"Extend Session\" name=\"refresh\" />\n");
			        out.println("    <input type=\"submit\" value=\"Retire Session\" name=\"retire\" />\n");
			        out.println(FORM_FOOTER);
			        //messages
			        if (sesStateMsg != null)
			        	out.println("<p>CURRENT SESSION STATE: "+sesStateMsg+"</p>");
			        if (erMsg != null)
			        	out.println("<p>ERROR MESSAGE: "+erMsg+"</p>");
			        out.println("<h4>Server Executing Request: " + vm.localAddress + " / " + SimpleServer.intToInet(vm.localAddress).getHostName() + "</h4>");
			        if(respondingServerID == -1){
			        	out.println("<h4>Session Data Found: Nowhere, this is a new session</h4>");
			        } else {
			        	out.println("<h4>Session Data Found: " + respondingServerID + " / " + SimpleServer.intToInet(respondingServerID).getHostName() + "</h4>");
			        }
			        //debug
			        if (DEBUG) {
			        	out.println("<h2>Debugging Information</h2>");	
				        for (Integer srv : srvs){
				        	if(srv != 0)
				        		out.println("<h5>Stored in: " + srv + " / " + SimpleServer.intToInet(srv).getHostName() );
				        }
			        	out.println(session.htmlFormattedDebugMessage());
			        	if (cVal != null){
			        		out.println("<p>Retrieved Cookie Value: " + cVal +"</p>");
			        	}
			        	out.println("<p>Set Cookie Value: " + cookie.getValue() +"</p>");
				        out.println("<p>Session Value Retrieved or Created (retrived sessions will show in pre-updated form): </p>");
				        out.println("<h3>Data in session table:</h3>");
				        out.println(sessions.toString().replaceAll("\n", " <br> "));
				        out.println("<h3>View Data:</h3>");
				        out.println(vm.toString().replaceAll("\n", " <br> "));
			        }
			        out.println(HTML_FOOTER);
		        } catch (Exception e){
		        	if (DEBUG)
		        	  out.println(e.getMessage());
		        } finally {
		        	out.close();
		        }
		        
		    }
	/**
	 * Generates a unique session ID by combining the 4 bytes of the serverID and 
	 * A 4 byte session ID. Session ID rolls over. Thread safe
	 * @return
	 */
	private static long generateSessionID(int localServerId) {
		int inServerId = SESSION_ID_GEN.getAndIncrement();
		ByteBuffer hold = ByteBuffer.allocate(8);
		hold.putInt(localServerId);
		hold.putInt(4, inServerId);
		return hold.getLong(0);
	}
	/**
	 * Generate a unique call id. Rolls over. Thread safe
	 * @return
	 */
	public static int generateCallID() {
		return SESSION_CALL_GEN.getAndIncrement();
	}

	/**
	 * Post calls the standard get response
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	/**
	 * Manages the server put in the cookie
	 * @param srvs 
	 * @param vm
	 * @return
	 */
	public static ArrayList<Integer> manageSrvs(ArrayList<Integer> srvs, ViewManager vm) {
		if(srvs.size() == (K+1)){ // WE have the right amount of servers for our cookie
			return srvs;
		}
		
		if(vm.size() > K && vm.hasUpServers()) { // by default K is set to 1
			SimpleServer backup = null;
			do { // get a server that is not local
				backup = vm.getAServer();
			} while(backup.serverID == vm.localAddress && backup.status == SimpleServer.status_state.UP);
			
			srvs.add(backup.serverID);
		} else { // enter null server
			System.out.println("Added null server to cookie: srvs size: " + srvs.size() + " VM size: " + vm.size());
			srvs.add(0);
		} 
		if(srvs.size() > (K+1)) { // several were returned from writeSessions, but the cookie only needs one + local
			int srvsNeeded = K+1;
			ArrayList<Integer> newSrvs = new ArrayList<Integer>();
			newSrvs.add(vm.localAddress);
			for(int srv:srvs) {
				//add non local addresses until
				if(srv != vm.localAddress) {
					newSrvs.add(srv);
				} 
				if(newSrvs.size() == srvsNeeded){
					break;
				}
			}
			return newSrvs;
		}
		return srvs;
	}

}
