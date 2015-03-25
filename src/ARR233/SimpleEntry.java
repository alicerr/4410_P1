package ARR233;



import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.Cookie;

import com.google.gson.Gson;
import  com.google.common.base.Ascii;
/**
 * @author Alice
 *Immutable simple entry class
 *Assumed to be unique by: Session, Version
 */
public final class SimpleEntry implements Comparable<SimpleEntry>{
	public static final byte SESSION_OFFSET = SessionFetcher.MESSAGE_OFFSET;
	public static final byte EXP_OFFSET = SESSION_OFFSET + 8;
	public static final byte VN_OFFSET = EXP_OFFSET + 8;
	public static final short MSG_LENGTH_OFFSET = VN_OFFSET + 4; 
	public static final byte MSG_OFFSET = MSG_LENGTH_OFFSET+ 2;
	public final static int TTL = 1000 * 60 * 60 * 6;
	public static final short MAX_MSG_SIZE_ASCII  = (short)(512-MSG_OFFSET);
	/**
	 * 
	 */
	public static final String COOKIE_NAME = "CS5300_A1_ARR233_SP2015";
	/**
	 * Session ID, assumed to be unique to servlet per session
	 */
	public final long sid;
	/**
	 * Message this session has stored @ this version
	 */
	public final String msg;
	/**
	 * Version number 
	 */
	public final int vn;


	//set session timeout for 6 hours from now every time a cookie is created
	/**
	 * Expiration date
	 */
	public final long exp;
	
	/**
	 * Method to create a new session
	 * @param sessionID the session ID assumed to be unique to servlet
	 */
	public SimpleEntry(long sessionID){
		this.sid = sessionID;
		this.vn = 0;
		this.msg = "Hello World";
		this.exp = System.currentTimeMillis() + TTL;
	}
	/**
	 * Update a session with a new message (and extend the expiration time). Will not work if the session
	 * is expired or retired 
	 * @param session the session to update
	 * @param message the message to update with
	 */
	public SimpleEntry(SimpleEntry session, String message){
		sid = session.sid;
		vn = newVersionNumber(session, true);
		exp = !session.isExpired() && ! session.isRetired() ? System.currentTimeMillis() + TTL : session.exp;
		if (!session.isExpired() && ! session.isRetired()){
			msg = sanitizeSessionMessage(message);
		} else {
			msg = session.msg;
		}
	}
	/**
	 * TODO: not getting message properly ('hello world' message)
	 * Conversion problems
	 * @param recvPkt
	 */
	public SimpleEntry(ByteBuffer recvPkt){
		this.sid = recvPkt.getLong(SESSION_OFFSET);
		this.vn = recvPkt.getInt(VN_OFFSET);
		this.exp = recvPkt.getLong(EXP_OFFSET);
		short message_length = recvPkt.getShort(MSG_LENGTH_OFFSET);
		String s = "";
		for (int i = MSG_OFFSET; i < MSG_OFFSET + message_length ; i ++){
			s += (char)(int)recvPkt.get(i);
		}
		
		this.msg = s;
	}
	/**
	 * Constructor for renewing or retiring a session
	 * @param session session to act on
	 * @param keepAlive if set to false this will retire the session, otherwise the session will renew
	 */
	public SimpleEntry(SimpleEntry session, boolean keepAlive){
		sid = session.sid;
		vn = newVersionNumber(session, keepAlive);
		msg = session.msg;
		exp = keepAlive ? System.currentTimeMillis() + TTL : session.exp;
	}
    /**
     * Update a version number
     * TODO: make unique per server
     * @param session session to update
     * @param keepAlive if false then retire session (set version to -1)
     * @return new version number (-1 if session is retired, expired, or version was above MAX_INT)
     */
    private int newVersionNumber(SimpleEntry session, boolean keepAlive){
    	if (session != null && session.vn < Integer.MAX_VALUE && !session.isRetired() && !session.isExpired() && keepAlive)
			return session.vn + 1;
		else 
			return -1;
    }


    /**
     * Determine if this session has been explicitly logged out of
     * @return this session has been explicitly logged out
     */
    public boolean isRetired() {
    	return vn < 0;
    }
    /**
     * Determine if the session has expired
     * @return session has expired
     */
    public boolean isExpired(){
    	return new Date().getTime() > exp;
    }
	/**
	 * A method to clean the session message
	 * @param sessionMessage
	 * @return
	 */
	private String sanitizeSessionMessage(String sessionMessage) {
		String message = "";
		for (int i = 0; i < sessionMessage.length(); i++){
			if (inRange(sessionMessage.charAt(i))){
				message += (char)(int)sessionMessage.charAt(i);
			}
		}
		message =  org.owasp.html.Sanitizers.BLOCKS.and(
				org.owasp.html.Sanitizers.FORMATTING
		).sanitize(message);
		if (message.length() > MAX_MSG_SIZE_ASCII){
			message = message.substring(0, MAX_MSG_SIZE_ASCII);
		}

		return message;
	}
    /**
     * Human readable session state, to discriminate between active sessions, sessions that were terminated, 
     * and sessions that expired
     * @return Human readable session state
     */
    public String sessionStateHR(){
    	if (isRetired() && isExpired()) {
    		return "Session exprired and retired";
    	} else if (isRetired()){
    		return "Session Explicitly Terminated";
    	} else if (isExpired()) {
    		return "Session Expired";
    	} else {
    		return "Session Active";
    	}
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString(){
    	return "SID: " + sid + ", VN: " + vn + ", EXP: " + new Date(exp)+ ", MSG: " + msg; 
    }
    /**
     * A HTML formatted version of toString for debugging
     * @return A HTML formatted version of toString for debugging
     */
    public String htmlFormattedDebugMessage(){
    	return "<p>" + "SID: " + sid + ", VN: " + vn + ", EXP: " + new Date(exp) + " MSG(Below):</p>" + msg;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object object){
    	if (object == null || !(object instanceof SimpleEntry)){
    		return false;
    	} else {
    		return compareTo((SimpleEntry)object) == 0;
    	}
    }
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(SimpleEntry simpleEntry) {
		int sid_cmp = Long.compare(sid, simpleEntry.sid);
		if (sid_cmp != 0)
			return sid_cmp;
		else 
			return Integer.compare(vn, simpleEntry.vn);
		
	}

	/**
	 * Cookie Maker
	 * @return JSON Cookie of this session, including Session ID in base 36, Version number in base 36 and MetaData in its value
	 */
	public Cookie getAsCookie(ArrayList<Integer> srvs){
		
		String s = "{ SRVS: "+ "[";
		for (Integer srv: srvs){
			s += srv + ", ";
		}
		s = (s.lastIndexOf(',') > 0 ? s.substring(0,s.lastIndexOf(',')) : s)+ "], SID: " + sid + ", VN: " + vn + " }";
		
		
		
		System.out.println(s);
		Cookie cookie = new Cookie(COOKIE_NAME, s);
		int expiry = (int) ((exp - System.currentTimeMillis() + 999)/1000);
		
		if (isRetired() || expiry < 0){
			expiry = 0;
			cookie.setValue(sessionStateHR());
		}
		cookie.setMaxAge(expiry);
		return cookie;
	}
	

	/**
	 * TODO not perserving message
	 * @param buffer
	 */
	public void fillBufferForUDP(ByteBuffer buffer){
		buffer.putLong(SESSION_OFFSET, sid);
		buffer.putLong(EXP_OFFSET, exp);
		buffer.putInt(VN_OFFSET, vn);
		
		char[] m = msg.toCharArray();
		buffer.putShort(MSG_LENGTH_OFFSET, (short) m.length);
		for (int i = 0; i < m.length + 0; i++)
		{
			buffer.put(MSG_OFFSET + i, (byte)m[i]);
		}
	}
	/**
	 * TODO is valid ascii?
	 * @param c
	 * @return
	 */
	public boolean inRange(char c){
		return (int)c >= 1 && (int)c <= 255;
	}
	
	
	
	
}
