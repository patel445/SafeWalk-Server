import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Iterator;

// you are a server socket and dont need to create a server object 
public class SafeWalkServer extends ServerSocket implements Runnable {
	// ServerSocket socket1; // object socket1 of server socket
	ArrayList<Request> requestlist = new ArrayList<Request>();
	String[] locations = { "PMU", "LWSN", "EE", "PUSH", "CL50", "*" };
	String[] tasks = { "#", "*" };
	final String INV_REQ = "ERROR: invalid request";
	final String INV_COM = "ERROR: invalid command";

	/**
	 * Construct the server, and create a server socket, bound to the specified
	 * port.
	 * 
	 * @throws IOException
	 *             IO error when opening the socket.
	 */
	public SafeWalkServer(int port) throws IOException {
		super(port);
		// this.port = port; // set port to argument value
		// if the port is not < 1025
		// or
		// greater than 65535 then
		// proceed
		// socket1 = new ServerSocket(port);// creates the server socket
		// with
		// port bounded
		// socket1.setReuseAddress(true); // this is so the socket still
		setReuseAddress(true); // binds even if connection is in timeout
		// state
	}

	/**
	 * Construct the server, and create a server socket, bound to a port that is
	 * automatically allocated.
	 * 
	 * @throws IOException
	 *             IO error when opening the socket.
	 */
	public SafeWalkServer() throws IOException {
		super(0);
		// port = 0; // declare an initial value of port
		// socket1 = new ServerSocket(port);// creates the server socket with
		// port
		// bounded
		// port = getLocalPort();// this is the method from
		// ServerSocket
		// class that returns port number in
		// which the socket is listening
		System.out.println("Port not specified. Using free port "
				+ getLocalPort());
		setReuseAddress(true);

	}

	/**
	 * Start a loop to accept incoming connections.
	 */
	public void run() {
		while (!isClosed()) {
			try {
				Socket client = accept(); // socket called client is =
				new Request(client).run();

			} catch (IOException e) {

			}
		}
	}

	public static boolean matching(Request a, Request b) {
		return (a.info[1].equals(b.info[1])) && (a.info[2].equals(b.info[2]))
				|| (a.info[1].equals(b.info[1]))
				&& ((a.info[2].equals("*") || b.info[2].equals("*")));
	}

	/**
	 * Return true if the port entered by the user is valid. Else return false.
	 * Return false if you get a NumberFormatException while parsing the
	 * parameter port Call this method from main() before creating
	 * SafeWalkServer object Note that you do not have to check for validity of
	 * automatically assigned port
	 */
	public static boolean isPortValid(String port) {
		try {
			int a = Integer.parseInt(port); // parses the port into an integer
			// and the
			// constructor checks if port is valid
			// number
			if (!(a < 1025 || a > 65536)) {
				return true;
			}
		} catch (NumberFormatException e) {
			return false; // false otherwise
		}
		return false; // return true if valid
	}

	public static void main(String[] args) {

		// call the isPortValid method to check for validity

		if (args.length == 0)
			try {
				SafeWalkServer a = new SafeWalkServer();
				a.run();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		else {
			if (isPortValid(args[0]) == false) {
				System.out.println("Inavalid port no.");
				return;
			} else {
				try {
					int str = Integer.parseInt(args[0]);
					SafeWalkServer a = new SafeWalkServer(str);
					a.run();
				} catch (IOException e) {
					System.out.println("Port already in use");
				}
			}
		}
	}

	class Request implements Runnable {
		// add to string method

		Socket ss;
		BufferedReader reader;
		PrintWriter writer;
		boolean iscommand;
		String command; // tells which of the command
		String[] info;

		Request(Socket ss) throws IOException, IllegalArgumentException {

			this.ss = ss;
			this.reader = new BufferedReader(new InputStreamReader(
					this.ss.getInputStream()));
			this.writer = new PrintWriter(this.ss.getOutputStream(), true);

		}

		public void reset(Request object) {
			for (Request s : requestlist) {
				s.writer.println("ERROR: connection reset");
				s.close();

			}
			requestlist.clear();
			object.writer.println("RESPONSE: success");
			object.close();
		}

		public void close() {
			// three sets for reader writer socket
			try {
				reader.close();
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			try {
				ss.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		public String toString() {
			return Arrays.toString(info);

		}

		public void run() {
			String line = "";
			try {
				line = reader.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			info = line.split(",");

			if (info[0].charAt(0) != ':') {
				iscommand = false;
				if (info.length != 3 || info[1].equals(info[2])
						|| !Arrays.asList(locations).contains(info[1])
						|| !Arrays.asList(locations).contains(info[2])
						|| info[1].equals("*")) {
					this.writer.println(INV_REQ);
					return;
				}

			} else {
				if (info.length == 4
						&& (!Arrays.asList(tasks).contains(info[1])
								|| !Arrays.asList(locations).contains(info[2]) || !Arrays
								.asList(locations).contains(info[3]))) {
					// needs to be fixed
					this.writer.println(INV_COM);
					return;
				} else {
					iscommand = true;
				}
			}
			command = line;
			if (this.iscommand) {
				if (this.command.equals(":RESET")) {
					// do things for reset
					reset(this);

				} else if (this.command.equals(":SHUTDOWN")) {
					// do things for shutdown
					reset(this);
					close();
					try {
						SafeWalkServer.this.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return;

				} else if (this.command.startsWith(":PENDING_REQUESTS,")) {

					if (this.command.equals(":PENDING_REQUESTS,#,*,*")) {
						this.writer
								.println("RESPONSE: # of pending requests = "
										+ requestlist.size());
					} else if (this.info[1].equals("#") && info[3].equals("*")) {
						int counter = 0;
						for (Request r : requestlist) {
							if (r.info[1].equals(this.info[2])) {
								counter++;
							}
						}
						this.writer
								.printf("RESPONSE: # of pending requests from %s = %d%n",
										this.info[2], counter);
					} else if (this.info[1].equals("#")
							&& this.info[2].equals("*")) {
						int counter = 0;
						for (Request r : requestlist) {
							if (r.info[2].equals(this.info[3])) {
								counter++;
							}
						}
						this.writer.printf(
								"RESPONSE: # of pending requests to %s = %d%n",
								this.info[3], counter);

					} else if (this.command.equals(":PENDING_REQUESTS,*,*,*")) {
						String output = requestlist.toString();
						this.writer.println(output);
					}
					this.close();

				} else {
					this.writer.println(INV_COM);
					this.close();
				}
			} else if (!this.iscommand) {

				Iterator<Request> itr = requestlist.iterator();
				while (itr.hasNext()) {
					Request r = itr.next();
					if (matching(this, r)) {

						this.writer.println("RESPONSE: " + r.info[0] + ","
								+ r.info[1] + "," + r.info[2]);
						r.writer.println("RESPONSE: " + this.info[0] + ","
								+ this.info[1] + "," + this.info[2]);
						r.close();
						this.close();
						itr.remove();
						return;
					}
				}

				requestlist.add(this);
				// System.out.println(requestlist.size());
			}

		}

	}
}
