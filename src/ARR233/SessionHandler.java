package ARR233;



import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
	/**
	 * Serial Version UID
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Session ID Generator TODO: make this not simple to guess
	 */
	private static final AtomicInteger SESSION_ID_GEN = new AtomicInteger(0);
	/**
	 * Session call ID Generator
	 */
	private static final AtomicInteger SESSION_CALL_GEN = new AtomicInteger(0);
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
			        if (c != null){
			        	cVal = c.getValue();
			        	try {
			        		JsonObject cValAsJson = new JsonParser().parse(cVal).getAsJsonObject();
			        		long cSessionID = Long.parseLong(cValAsJson.get("SID").getAsString(), 36);
			        		session = sessions.get(cSessionID);
			        		if (session == null){
			        			erMsg = "Previous Session not found in table! x_x";
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
			        	
			        	if (request.getParameter("retire") != null && session != null){
			                session =  new SimpleEntry(session, false);
			        		sessions.put(session);
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
			        	}
			        }
			      
			        if (session == null){
			        	session = new SimpleEntry(generateSessionID());
			        	sessions.put(session);
			        	sesStateMsg = sesStateMsg == null ? "New Session Started" : sesStateMsg + "; New Session Started";
			        }
			        Cookie cookie = session.getAsCookie();
			        response.addCookie(cookie);
			        response.setContentType("text/html");

			        out.println(HTML_HEADER);
			        out.println(session.msg);
			        out.println(FORM_HEADER);
			        out.println("    Please include html block elements, images, and formating in message   <br />");
			        out.println("    <input type=\"text\" name=\"new_message\" value=\"<p> </p>\" >\n<br />");
			        out.println("    <input type=\"submit\" value=\"Replace Message\" name=\"replace\" />\n");
			        out.println("    <input type=\"submit\" value=\"Extend Session\" name=\"refresh\" />\n");
			        out.println("    <input type=\"submit\" value=\"Retire Session\" name=\"retire\" />\n");
			        out.println(FORM_FOOTER);
			        if (sesStateMsg != null)
			        	out.println("<p>CURRENT SESSION STATE: "+sesStateMsg+"</p>");
			        if (erMsg != null)
			        	out.println("<p>ERROR MESSAGE: "+erMsg+"</p>");
			        if (DEBUG) {
			        	out.println("<h2>Debugging Information</h2>");
			        	if (cVal != null){
			        		out.println("<p>Retrieved Cookie Value: " + cVal +"</p>");
			        	}
			        	out.println("<p>Session Value Retrieved or Created (retrived sessions will show in pre-updated form): </p>");
			        	out.println(session.htmlFormattedDebugMessage());
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
	 * @return
	 */
	private static int generateSessionID() {
		return SESSION_ID_GEN.getAndIncrement();
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
