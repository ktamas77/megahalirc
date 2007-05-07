import java.net.*;
import java.io.*;

public class IRC {
	
	public final static int MSG = -1;
	public final static int KICK = -2;
	public final static int NONE = 0;
	
	Socket ircSocket = null;
	PrintWriter out = null;
	BufferedReader in = null;
	boolean connect = false;
	int mode = 0;
	IRCMessage ircMessage = null;
	String nick = null;
	String kickChannel = null;
	
	public class IRCMessage {
		String from = null;
		String to = null;
		String message = new String ();
		
		IRCMessage (String parseLine) {
			// parseoljunk...
			int poz = 0;
			int poz2 = 0;
			String sor = new String (parseLine).substring (1);
			poz = sor.indexOf ("!");
			this.from = new String (sor.substring (0,poz));
			poz = sor.indexOf ("PRIVMSG");
			String nev = new String (sor.substring (poz+8));
			poz2 = nev.indexOf (" :");
			this.to = new String (nev.substring (0,poz2));
			this.message = sor.substring (poz+10+this.to.length());
		}
		
		public String getFrom () { return this.from; } 
		public String getTo () { return this.to; } 
		public String getMessage () { return this.message; }
	}
	
	IRC (String nick, String server, int port, String ircName) {
		
		this.nick = nick;
		
		// kapcsolodunk az irc szerverhez
		try {
			System.out.print ("Connecting to "+server+":"+port+"... ");
			this.ircSocket = new Socket (InetAddress.getByName (server), port);
			this.out = new PrintWriter (this.ircSocket.getOutputStream (), true);
			this.in = new BufferedReader (new InputStreamReader (this.ircSocket.getInputStream()));
		} catch (java.io.IOException ex) {
			System.out.println ("connect failed.");
			System.exit (0);
		}
		
		// sikeres kapcsolodas eseten
		System.out.println ("connected!");
		this.connect = true;		
		
		// Login
		System.out.print ("Logging in as "+this.nick+"... ");
		this.out.println ("USER "+this.nick+" "+this.nick+" "+this.nick+" :"+this.nick);
		this.out.println ("NICK "+this.nick);

		// megvarjuk a motd-ot		
		try {
			String sor = this.in.readLine ();
			while (sor.indexOf ("376") == -1) sor = this.in.readLine ();
		} catch (java.io.IOException ex) { 
			System.out.println ("login failed.");
			System.exit (0);
		}
		
		System.out.println ("done.");
		
	}
	
	public String readLine () throws java.io.IOException {
		return this.in.readLine ();
	}
	
	public boolean join (String channelName) {
		System.out.print ("Joining "+channelName+"... ");
		this.out.println ("JOIN "+channelName);
		try {
			if (new String (this.in.readLine()).indexOf (" 474 ") == -1) {
				System.out.println ("done.");
				return true;
			} else {
				System.out.println ("failed.");
				return false;
			}
		} catch (IOException ex) {
			System.out.println ("failed. Reason: "+ex);
			return false;
		}
	}
	
	public String parseLine () {
		this.mode = 0;
		String sor = null;
		try {
			sor = this.in.readLine ();
			
			if (sor != null) {
		
				// PONG
				if (sor.startsWith ("PING")) {
					String serverName = sor.substring (6);
					this.out.println ("PONG "+serverName);
					//System.out.println ("PONG "+serverName);
				}
				
				// PRIVMSG
				if (sor.indexOf ("PRIVMSG") != -1) {
					this.mode = MSG;
					this.ircMessage = new IRCMessage (sor);
				}
				
				if ((sor.indexOf (" KICK ") != -1) && (sor.indexOf (this.nick) > 2)) {
					int poz = sor.indexOf ("#");
					this.kickChannel = sor.substring (poz);
					poz = kickChannel.indexOf (" ");
					this.kickChannel = kickChannel.substring (0,poz).toUpperCase();
					this.mode = KICK;
				}
			}
			
		} catch (java.io.IOException ex) {
			System.out.println ("iRC I/O error: "+ex);
		}
		
		return sor;
	}
	
	public void setNick (String szoveg) {
		this.nick = szoveg;
	}
	
	public String getNick () {
		return this.nick;
	}
	
	public void writeLine (String szoveg) {
		this.out.println (szoveg);
	}
	
	public void sendMessage (String cimzett, String szoveg) {
		this.out.println ("PRIVMSG "+cimzett+" :"+szoveg);
	}
	
	public void close () {
		// logout
		System.out.print ("Closing connection... ");
		try {
			this.out.close ();
			this.in.close ();
			this.ircSocket.close ();
			this.connect = false;
		} catch (java.io.IOException ex) { 
			System.out.println ("failed.");
			System.exit (1);
		}
		System.out.println ("done.");
	}
	
	public boolean connected () {
		return this.connect;
	}
}

