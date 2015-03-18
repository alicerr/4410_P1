package ARR233;



import java.nio.ByteBuffer;
import java.util.Date;

import javax.servlet.http.Cookie;

/**
 * @author Alice
 *Immutable simple entry class
 *Assumed to be unique by: Session, Version
 */
public class SimpleEntry implements Comparable<SimpleEntry>{
	public static final byte EXP_OFFSET = SessionFetcher.MESSAGE_OFFSET;
	public static final byte VN_OFFSET = SessionFetcher.MESSAGE_OFFSET + 8;
	public static final byte MSG_OFFSET = SessionFetcher.MESSAGE_OFFSET + 12;
	public static final byte MAX_MSG_SIZE_UTF_8  = (64 - MSG_OFFSET) / 8;
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
	/**
	 * placeholder for metadata, not sure if this will be handled implicitly by version number or need to be stored.
	 */
	private final Byte metadata = null;
	//set session timeout for 6 hours from now every time a cookie is created
	/**
	 * Expiration date
	 */
	private final Date exp;
	/**
	 * Method to create a new session
	 * @param sessionID the session ID assumed to be unique to servlet
	 */
	public SimpleEntry(long sessionID){
		this.sid = sessionID;
		this.vn = 0;
		this.msg = "Hello World";
		this.exp = newExp(null);
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
		exp = newExp(session);
		if (!session.isExpired() && ! session.isRetired()){
			msg = sanitizeSessionMessage(message);
		} else {
			msg = session.msg;
		}
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
		exp = newExp(session);
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
     * create an updated sxpiration date, if the session passed in is not null, retired, or expired
     * @param prevSession
     * @return date to expire at
     */
    private Date newExp(SimpleEntry prevSession){
    	if (prevSession == null || !(prevSession.isExpired() || prevSession.isRetired()))
    		return new Date(new Date().getTime() + 6 * 60 * 60 * 1000 );
    	else 
    		return prevSession.getExp();
    }
	/**
	 * Get date this session expires at
	 * @return date this session expires at
	 */
	public Date getExp() {
		return new Date(exp.getTime());
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
    	return new Date().after(exp);
    }
	/**
	 * A method to clean the session message
	 * @param sessionMessage
	 * @return
	 */
	private String sanitizeSessionMessage(String sessionMessage) {
		String message =  org.owasp.html.Sanitizers.BLOCKS.and(
				org.owasp.html.Sanitizers.FORMATTING
		).sanitize(sessionMessage);
		if (message.length() > MAX_MSG_SIZE_UTF_8){
			message = message.substring(0, MAX_MSG_SIZE_UTF_8 + 1);
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
    	return "SID: " + sid + ", VN: " + vn + ", EXP: " + exp.toString() + ", MSG: " + msg; 
    }
    /**
     * A HTML formatted version of toString for debugging
     * @return A HTML formatted version of toString for debugging
     */
    public String htmlFormattedDebugMessage(){
    	return "<p>" + "SID: " + sid + ", VN: " + vn + ", EXP: " + exp.toString() + " MSG(Below):</p>" + msg;
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
	public Cookie getAsCookie(){
		String value = "{\"SID\":\""+Long.toString(sid,36)+"\", \"VN\":\""+Integer.toString(vn, 36)+"\", \"MetaData\":\"null\"}";
		Cookie cookie = new Cookie(COOKIE_NAME, value);
		int expiry = (int) ((exp.getTime() - new Date().getTime() + 999)/1000);
		if (isRetired() || expiry < 0){
			expiry = 0;
			cookie.setValue(sessionStateHR());
		}
		cookie.setMaxAge(expiry);
		return cookie;
	}
	
	public byte[] fillBufferForUDP(ByteBuffer buffer){
		buffer.putLong(SessionFetcher.MESSAGE_OFFSET, exp.getTime());
		buffer.putInt(SessionFetcher.MESSAGE_OFFSET + 8, vn);
		buffer.putInt(SessionFetcher.MESSAGE_OFFSET + 12, message)
	}
	
	
}
