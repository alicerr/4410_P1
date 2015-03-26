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
 * Thread Safe Session Handler for Assignment 1a 5300
 * Servlet implementation class SessionHandler
 */
/**
 * @author Alice (arr233)
 *
 */
@WebServlet("/SessionHandler")
public class SessionHandler extends HttpServlet {
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
	 *  @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
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
			        //int i = 1;
			        SimpleEntry session = null;
			        String erMsg = null;
			        String sesStateMsg = null;
			        //System.out.println(i++ + "1");
			        String cVal = null; //cookie value
			        ArrayList<Integer> srvs = new ArrayList<Integer>();
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
			        				vm.addServer(new SimpleServer(js.getAsInt()));
			        			} else {
			        				isLocal = true;
			        			}
			        		}
			        		
			        		if (isLocal){ //if found locally
			        			//System.out.println("looking in local table for cookies");
			        			session = sessions.get(cSessionID);
			        			//System.out.println("I found this in local table: " + session);
			        		}
			        		if (session == null && srvs.size() > 0){
			        			//System.out.println("checking remote servers");
			        			session = SessionFetcher.fetchSession(generateCallID(), cSessionID, srvs, vm);
			        		} 
			        		if (session == null) {
			        			erMsg = "Previous Session not found in System! x_x";
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
				        //System.out.println(i++ + "2");
			        	if (request.getParameter("retire") != null && session != null){
			                session =  new SimpleEntry(session, false);
			        		sessions.put(session);
			        		
			        		srvs = SessionFetcher.writeSession(session, srvs, generateCallID(), vm);

			        		session = null;
			        		sesStateMsg = "Session Terminated Successfully";
			        	} else if (session != null) { 
			        		Map<String, String[]> requestMap = request.getParameterMap();
			        		
			        		if (requestMap.containsKey("replace")){
			        			session = new SimpleEntry(session, requestMap.get("new_message")[0]);
			        		} else {
			        			session = new SimpleEntry(session, true);
			        		} 
			        		sessions.put(session);
			        		srvs = SessionFetcher.writeSession(session, srvs, generateCallID(), vm);
			        		srvs.add(vm.localAddress);
			        	}
			        } // end not null cookie
			        //else {
			        	//System.out.println("no cookie found");
			        //}

			       // System.out.println(i++ + "after cookie check");
			        if (session == null){
			        	session = new SimpleEntry(generateSessionID(vm.localAddress));
			        	//System.out.println("made new session");
			        	sessions.put(session);
			        	//System.out.println("stored session");
			        	//srvs = SessionFetcher.writeSession(session, srvs, generateCallID(), vm);
			        	srvs.add(vm.localAddress);
			        	sesStateMsg = sesStateMsg == null ? "New Session Started" : sesStateMsg + "; New Session Started";
			        }
			        //System.out.println(i++ + "about to make cookie");
			        
			        Cookie cookie = session.getAsCookie(srvs);
			        response.addCookie(cookie);
			        response.setContentType("text/html");
			        //System.out.println(i++ + "made cookie");
			        //out.println(cookie);
			        out.println(HTML_HEADER);
			        out.println(session.msg);
			        out.println(FORM_HEADER);
			        //out.println("    Please include html block elements, images, and formating in message   <br />");
			        out.println("    <input type=\"text\" name=\"new_message\" value=\"\" >\n<br />");
			        out.println("    <input type=\"submit\" value=\"Replace Message\" name=\"replace\" />\n");
			        out.println("    <input type=\"submit\" value=\"Extend Session\" name=\"refresh\" />\n");
			        out.println("    <input type=\"submit\" value=\"Retire Session\" name=\"retire\" />\n");
			        out.println(vm.localAddress);
			        out.println(FORM_FOOTER);
			        if (sesStateMsg != null)
			        	out.println("<p>CURRENT SESSION STATE: "+sesStateMsg+"</p>");
			        if (erMsg != null)
			        	out.println("<p>ERROR MESSAGE: "+erMsg+"</p>");
			        if (DEBUG) {
			        	
			        	out.println("<h2>Debugging Information</h2>");
			        	out.println("<h4>Current Address: " + vm.localAddress + "</h4>");
				        out.println(session.htmlFormattedDebugMessage());
			        	if (cVal != null){
			        		out.println("<p>Retrieved Cookie Value: " + cVal +"</p>");
			        	}
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
	 * 
	 */
	public SimpleEntry doGetDatagram(long sessionID){         
	        	//Retrieve session table
	   SessionTable sessions = (SessionTable)getServletContext().getAttribute("sessions");
	   return sessions.get(sessionID);	        
	}
	/**
	 * 
	 */
	public boolean doPutDatagram(SimpleEntry session){      
	   //Retrieve session table
	   SessionTable sessions = (SessionTable)getServletContext().getAttribute("sessions");
	   return sessions.put(session);	        
	}
	/**
	 * @return
	 */
	private static long generateSessionID(int localServerId) {
		int inServerId = SESSION_ID_GEN.getAndIncrement();
		ByteBuffer hold = ByteBuffer.allocate(8);
		hold.putInt(localServerId);
		hold.putInt(4, inServerId);
		return hold.getLong(0);
	}
	public static int generateCallID() {
		return SESSION_CALL_GEN.getAndIncrement();
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
