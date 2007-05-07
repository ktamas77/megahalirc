import java.io.*;

public class Megahal {
	
		Process halProc;
		PrintWriter out = null;
		BufferedReader in = null;
		String [] halParam = new String [4];

		Megahal (String halPath) {
			halParam [0] = new String (halPath);
			halParam [1] = new String ("--no-prompt");
			halParam [2] = new String ("--no-wrap");
			halParam [3] = new String ("--no-banner");
			System.out.print ("Launching MEGAHAL... ");
			try {
				this.halProc = Runtime.getRuntime().exec(halParam);
				this.out = new PrintWriter (this.halProc.getOutputStream (), true);
				this.in = new BufferedReader (new InputStreamReader (this.halProc.getInputStream()));
			} catch (IOException ex) {
				System.out.println ("failed (Reason: "+ex+").");
				System.exit (0);
			}
			// dev -> null :P
			this.readLine ();
			System.out.println ("done.");
		}
		
		public String readLine () {
			String sor = null;
			try {
				sor = this.in.readLine ();
			} catch (IOException ex) {
				System.out.println ("Megahal I/O Error: "+ex);
			}
			return sor;
		}
		
		public String writeLine (String szoveg) {
			out.println (szoveg);
			out.println ();
			return this.readLine ();
		}
		
		public void exit () {
			System.out.print ("Stopping MEGAHAL...");
			try {
				this.out.println ("#QUIT");
				this.out.println ();
				this.out.close ();
				this.in.close ();
				System.out.println ("done.");
			} catch (IOException ex) {
				System.out.println ("failed.");
			}
		}
	
}