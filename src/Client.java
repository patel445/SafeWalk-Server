import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Client class for SafeWalkServer
 */
public class Client implements Runnable {
	private final String host;
	private final int port;
	private final String message;
	private boolean timeout;
	private String result;

	/**
	 * Used to create Client socket without timeout
	 */
	public Client(String host, int port, String msg) {
		this.host = host;
		this.port = port;
		this.message = msg;
	}

	/**
	 * Necessary for JUnit testing (and otherwise) Used to create Client socket
	 * with timeout
	 */
	public Client(String host, int port, String msg, boolean value) {
		this.host = host;
		this.port = port;
		this.message = msg;
		timeout = value;
	}

	public String getResult() {
		return result;
	}

	public void run() {
		// try with resource block is used here
		try (Socket s = new Socket(host, port);
				PrintWriter out = new PrintWriter(s.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(
						s.getInputStream()));) {
			if (timeout)
				s.setSoTimeout(1000);
			out.println(message);
			result = in.readLine();
		} catch (SocketTimeoutException e) {
			/*
			 * During testing the following message might get printed. It does
			 * not always mean that there is an error in the program
			 */
			System.out.println("Connection timed out");
		} catch (IOException e) {
			System.err.println(e);
		}
	}

	public static void main(String[] args) {
		Client c = new Client(args[0], Integer.parseInt(args[1]), args[2]);
		Thread t = new Thread(c);
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(c.getResult());
	}
}