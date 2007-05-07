import java.io.*;
import java.util.*;
import java.text.*;


class DialogMapElement {
	String from = null;
	String to = null;
	
	DialogMapElement (String from, String to) {
		this.from = from;
		this.to = to;
	}
}

class Operator {
	
	IRC irc;
	String opName;
	
	Operator (String opName, IRC irc) {
		this.opName = opName;
		this.irc = irc;
		irc.sendMessage (this.opName, "Welcome, "+opName+", my master :P");			
		System.out.println ("Operator login.");
	}
	
	public void message (String szoveg) {
		irc.sendMessage (this.opName, szoveg);
	}
}

class SendMessageThread extends Thread {
	
	IRC irc;
	String cimzett;
	String reply;
	
	SendMessageThread (IRC irc, String cimzett, String reply) {
		this.irc = irc;
		this.cimzett = cimzett;
		this.reply = reply;
	}
	
	public void run () {
		// valaszolunk
		try {
			sleep (this.reply.length()*190);
		} catch (InterruptedException ex) {
			System.out.println ("SendMessageThread break error.");
		}
		this.irc.sendMessage (this.cimzett, this.reply);				
	}
}

public class Halacska extends Thread {

	static String SERVER = new String ("irc.extra.hu");	
	final static String VERZIO = new String ("1.08");
	final static String NICK = new String ("abbalany");
	final static String IRCNAME = new String ("abba");
	final static int    PORT = 6667;
	final static String CHANNEL = new String ("#robochn").toUpperCase();
	final static String MEGAHALPATH = new String ("/home/ktamas/megahalirc/megahal");
	final static String WELCOME = new String ("tud valaki segiteni nekem, hm, drog-ugyben?...");
	final static String PASSWORD = new String ("robotrip");
	final static String HEADER = "MEGAHAL iRC WRappER  version "+VERZIO+" (dh@squidcode.com)";
	static Operator operator = null;
	static Vector channels = new Vector ();
	static Vector maps = new Vector ();
	
	public static String convertMessage (String szoveg) {	
		char [] oldConv = {'á','Á','é','É','í','Í','ó','Ó','ö','Ö','õ','Õ','ú','Ú','ü','Ü','û','Û'};
		char [] newConv = {'a','A','e','E','i','I','o','O','o','O','o','O','u','U','u','U','u','U'};
		String message = new String (szoveg);
		// konvertaljuk az uzenetet
		for (int i=0;i<oldConv.length;i++) {
			message = message.replace (oldConv [i], newConv [i]);
		}
		return message;
	}
	
	public static String nickFilter (String szoveg, String nick) {
		int eloFordul = szoveg.toUpperCase().indexOf (nick.toUpperCase());
		int nickHossz = nick.length ();
		if (eloFordul != -1) {
			String ujSzoveg = new String (szoveg.substring(0,eloFordul)).trim ();
			ujSzoveg = ujSzoveg+" "+szoveg.substring (eloFordul+nickHossz).trim ();
			return ujSzoveg;
		} else {
			return szoveg;
		}
	}
	
	public static void main (String args []) {
		if (args.length >= 1) {
			SERVER = args [0];
		}
		channels.addElement (CHANNEL);
		int saveCounter = 0;		
		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
				
		System.out.println ("\n\n"+HEADER+"\n");
		Megahal hal = new Megahal (MEGAHALPATH);		
		IRC irc = new IRC (NICK, SERVER, PORT, IRCNAME);
		
		for (int i=0;i<channels.size();i++) {
			String csatorna = (String)channels.elementAt (i);
			if (! irc.join (csatorna)) channels.removeElement (csatorna);
		}
		//irc.sendMessage (CHANNEL, WELCOME);
		
		while (irc.connected ()) {
			String sor = irc.parseLine ();
			if (irc.mode == irc.KICK) {
				System.out.println ("KICKED from "+irc.kickChannel+"!");
				channels.removeElement (irc.kickChannel);
				try {
					sleep (3000);
				} catch (InterruptedException ex) {
					System.out.println ("Break error.");
				}
				if (irc.join (irc.kickChannel)) {
					channels.addElement (irc.kickChannel);
				}
			} else
			if (irc.mode == irc.MSG) {
				String from = irc.ircMessage.getFrom ();				
				String message = irc.ircMessage.getMessage ();
				String to = irc.ircMessage.getTo ();				
				
				// --- ctcp emulation 
				boolean ctcpEmu = false;
				char marker = '\u0001';										
				if (message.indexOf (marker+"VERSION"+marker) == 0) {
					System.out.println ("["+from+"] CTCP: Version");
					irc.writeLine ("NOTICE "+from+" :"+marker+"VERSION mIRC32 v5.91 K.Mardam-Bey"+marker);
					ctcpEmu = true;
				}
				if (message.indexOf (marker+"PING") == 0) {
					System.out.println ("["+from+"] CTCP: Ping");
					irc.writeLine ("NOTICE "+from+" :"+message);
					ctcpEmu = true;
				}
				if (message.indexOf (marker+"TIME"+marker) == 0) {
					System.out.println ("["+from+"] CTCP: Time");
					Date date = new Date ();				
					SimpleDateFormat formatter = new SimpleDateFormat ("EEE MMM dd hh:mm:ss yyyy");
					irc.writeLine ("NOTICE "+from+" :"+marker+formatter.format (date)+marker);
					ctcpEmu = true;
				}
				if (message.indexOf (marker+"FINGER"+marker) == 0) {
					System.out.println ("["+from+"] CTCP: Finger");
					irc.writeLine ("NOTICE "+from+" :"+marker+"FINGER "+IRCNAME+marker);
					ctcpEmu = true;
				}
				
				// DCC Emulation
				boolean dccEmu = false;
				if (message.indexOf ("DCC SEND") > -1) {
					System.out.println ("["+from+"] DCC: Send");
					dccEmu = true;
				}
				if (message.indexOf ("DCC CHAT") > -1) {
					System.out.println ("["+from+"] DCC: Chat");
					dccEmu = true;
				}
				
				// --- operator/reply processing ---
				if ((! ctcpEmu) && (! dccEmu)) {
					if ((operator != null) && (to.equals (irc.getNick())) && (message.startsWith ("."))) {
						String parancs = message.substring (1);
						String p = parancs.toUpperCase ();
						// nick csere
						if (p.startsWith ("NICK ")) {
								// -- ide egy ellenorzes ha  nickcsere telleg megvolt-e, jo lenne	
								irc.setNick (parancs.substring (5));
								operator.message ("Nick change -> "+irc.getNick ());
								irc.writeLine (parancs);							
						}
						// plusz csatorna
						if (p.startsWith ("JOIN #")) {
								String csatorna = parancs.substring (5);
								if (irc.join (csatorna.toLowerCase())) {
									operator.message (csatorna.toUpperCase ()+" joined.");
									channels.addElement (csatorna.toUpperCase());
								} else {
									operator.message ("Can't join to "+csatorna.toUpperCase ()+".");
								}
						}
						// csatornabol kilepes
						if (p.startsWith ("PART #")) {
								String csatorna = parancs.substring (5);
								if (channels.removeElement (csatorna.toUpperCase())) {
									irc.writeLine (parancs);							
									operator.message (csatorna.toUpperCase()+" leaved.");
								} else {
									operator.message (irc.getNick()+" isn't at "+csatorna.toUpperCase()+".");
								}
						}
						// dialogus mapping
						if (p.startsWith ("MAP")) {
							try {
								String mapFrom = parancs.substring (4);
								int poz = mapFrom.indexOf (" ");
								mapFrom = mapFrom.substring (0, poz).toUpperCase();
								String mapTo = parancs.substring (5+poz).toUpperCase();
								if ((mapFrom.length () > 1) && (mapTo.length () > 1) && (mapTo.startsWith ("#"))) {
									DialogMapElement tempMap = new DialogMapElement (mapFrom, mapTo);
									maps.addElement (tempMap);
									operator.message ("Map ("+mapFrom+" -> "+mapTo+") added.");
								} else {
									operator.message ("Map what? :P");
								}
							} catch (StringIndexOutOfBoundsException ex) {
								operator.message ("Type .HELP and read the correct syntax.");
							}
						}
						if (p.startsWith ("CATCH ")) {
							try {
								String csatorna = parancs.substring (6);
								int poz = csatorna.indexOf (" ");
								csatorna = csatorna.substring (0, poz).toUpperCase();
								String mit = parancs.substring (7+poz);
								Vector users = new Vector ();							
								if ((csatorna.length () > 1) && (mit.length () > 0) && (csatorna.startsWith ("#"))) {
									mit = convertMessage (mit);
									int szam = 0;
									try {
										boolean csatlakozas = false;
										// ha nem vagyunk rajta a csatornan, csatlakozunk
										if (! channels.contains (csatorna.toUpperCase())) {
											if (irc.join (csatorna.toLowerCase())) {
												operator.message (csatorna.toUpperCase ()+" joined.");
											    csatlakozas = true;
											} else {
												operator.message ("Can't join to "+csatorna.toUpperCase ());
											}
										}
										try {									
											System.out.print ("Sending WHO... ");
											irc.writeLine ("who "+csatorna);									
											System.out.println ("done.");
										} catch (Exception ex) {
											System.out.println ("failed : "+ex);
										}
										// ha csak ideiglenesen mentunk a csatira, most elhagyjuk
										if (csatlakozas) {
											try {
												System.out.print ("Sending PART... ");
												irc.writeLine ("part "+csatorna);										
												operator.message (csatorna.toUpperCase()+" leaved.");
												System.out.println ("done.");
											} catch (Exception ex) {
												System.out.println ("Can't leave "+csatorna);
											}
										}
										String ircSor = new String (irc.readLine());
										//System.out.println ("TEST: "+ircSor);
										while ((ircSor.indexOf ("End of WHO") == -1) && (szam < 20)) {
											//System.out.println ("TEST: "+ircSor);
											if ((ircSor.indexOf ("@") == -1) && (ircSor.indexOf ("352 ") > 0)) {
												StringTokenizer st = new StringTokenizer (ircSor," ");
												int tokenSzam = 0;
												while (st.hasMoreTokens ()) {
													if (tokenSzam == 7) {
														String felhasznalo = new String (st.nextToken());
														if (! felhasznalo.toUpperCase().equals (irc.getNick().toUpperCase())) {
															//operator.message ("Found user: "+felhasznalo);
															users.addElement (felhasznalo);
															szam++;
														}
													}
													st.nextToken();
													tokenSzam++;
												}
											}
											ircSor = new String (irc.readLine());
										}
									} catch (java.io.IOException ex) {
										operator.message ("Catch I/O Error: "+ex);
									}
									operator.message ("Found "+szam+" user(s).");								
									for (int i=0;i<users.size();i++) {
										String kinek = new String ((String)users.elementAt (i));
										operator.message ("User: "+kinek);
										SendMessageThread rs = new SendMessageThread (irc, kinek, mit);
										rs.start ();
									}
									operator.message ("Catch done.");
								} else {
									operator.message ("Catch how? :P");
								}
							} catch (StringIndexOutOfBoundsException ex) {
								operator.message ("Type .HELP and read the correct syntax.");
							}
						}
						// dialogus de-mapping
						if (p.startsWith ("REMOVEMAP ")) {
							try {
								String mapFrom = parancs.substring (10);
								int mapFromInt = -1;
								mapFromInt = Integer.parseInt (mapFrom);
								try {
									maps.removeElementAt (mapFromInt);
									operator.message ("Map #"+mapFromInt+" removed.");
								} catch (ArrayIndexOutOfBoundsException ex) {
									operator.message ("There isn't map #"+mapFromInt+" in my list.");
								}
							} catch (Exception ex) {
								operator.message ("Type .HELP and read the correct syntax.");
							}
						}
						// say
						if (p.startsWith ("SAY")) {
							try {
								String kinek = parancs.substring (4);
								int poz = kinek.indexOf (" ");
								kinek = kinek.substring (0, poz).toUpperCase();
								String mit = parancs.substring (5+poz);
								if ((kinek.length () > 1) && (mit.length () > 0)) {
									mit = convertMessage (mit);
									operator.message (kinek+": "+mit);
									irc.sendMessage (kinek, mit);
								} else {
									operator.message ("Say what? :P");
								}
							} catch (StringIndexOutOfBoundsException ex) {
								operator.message ("Type .HELP and read the correct syntax.");
							}
						}
						if (p.startsWith ("QUIT")) {
							operator.message ("Bye-bye.");
							irc.close ();
						}
						// info
						if (p.startsWith ("INFO")) {
								operator.message (HEADER);
								operator.message ("Name    : "+irc.getNick());
								operator.message ("Channels: "+channels.size());
								for (int i=0;i<channels.size();i++) {
									operator.message (" Channel: "+(String)channels.elementAt (i));
								}
								operator.message ("Maps    : "+maps.size());
									for (int i=0;i<maps.size();i++) {
										String mapSzoveg = new String ();
										DialogMapElement tempMap = (DialogMapElement)maps.elementAt (i);
										mapSzoveg = "("+tempMap.from+" -> "+tempMap.to+")";
										operator.message ("     Map: #"+i+" "+mapSzoveg);
										
									}
	
						}
						// help
						if (p.startsWith ("HELP")) {
							operator.message (HEADER);
							operator.message ("SAY <nick/channel> <message>          send a message to nick/channel");
							operator.message ("JOIN <channel>                        join a channel");
							operator.message ("PART <channel>                        part a channel");												
							operator.message ("NICK <nick>                           change nick");
							operator.message ("CATCH <channel> <message>             talks to everybody on a channel");
							operator.message ("MAP <nick> <channel>                  map a nick dialog to a channel");
							operator.message ("REMOVEMAP <map-number>                remove a map");
							operator.message ("INFO                                  view status");												
							operator.message ("HELP                                  this text");
							operator.message ("QUIT                                  shut down this stuff");						
						}
					} else {
						// operator login?
						if ((message.equals (PASSWORD)) && (to.equals(irc.getNick())) && (operator == null)) {
							operator = new Operator (from, irc);
						} else {
							// send reply
							
							// kiszurjuk az ekezetes betuket
							message = convertMessage (message);
							// kivesszuk a sajat nick-jet is
							String oldMessage = new String (message);
							message = nickFilter (message, irc.getNick ());
	
							String reply = new String (hal.writeLine (message));						
							if (message.length () != oldMessage.length ()) reply = new String (from+", "+reply);
							
							String cimzett = new String ();
							if (to.startsWith ("#")) {
								cimzett = to;
							} else {
								cimzett = from;
							}
		
							// --- display ---
							System.out.println ("("+from+" -> "+to+"): "+message);
							// --- map-szures ---
							for (int i=0;i<maps.size();i++) {
								DialogMapElement tempMap = (DialogMapElement)maps.elementAt (i);
								if (tempMap.from.toUpperCase().equals (from.toUpperCase())) {
									irc.sendMessage (tempMap.to, "("+from+" -> "+to+"): "+message);
								}
							}
		
							
							SendMessageThread rs = new SendMessageThread (irc, cimzett, reply);
							rs.start ();
							
							// --- display ---
							System.out.println ("("+irc.getNick()+" -> "+cimzett+"): "+reply);
							// --- map-szures ---
							for (int i=0;i<maps.size();i++) {
								DialogMapElement tempMap = (DialogMapElement)maps.elementAt (i);
								if (tempMap.from.toUpperCase().equals (cimzett.toUpperCase())) {
									irc.sendMessage (tempMap.to, "("+irc.getNick()+" -> "+cimzett+"): "+reply);
								}
							}
						
							// save
							saveCounter++;
							if (saveCounter >= 10) {
								System.out.print ("Saving the brain... ");
								hal.writeLine ("#SAVE");
								System.out.println ("done.");
								saveCounter = 0;
							}
						}
					}
				}
			} else {
			  //System.out.println ("DEBUG: "+sor);							
			}
		}
		
		hal.exit ();
	}
	
}

