import org.junit.*;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Random;

public class SafeWalkServerTest {
	final String ERR_INVALID_REQUEST = "ERROR: invalid request";
	final String ERR_INVALID_COMMAND = "ERROR: invalid command";
	final boolean TIMEOUT = true;
	private static int port;
	private static String HOST = "127.0.0.1";
	private SafeWalkServer s;

	@Before
	public void setUp() throws IOException {
		s = new SafeWalkServer();
		Thread t = new Thread(s);
		t.start();
		port = s.getLocalPort();
	}

	@After
	public void tearDown() throws IOException {
		if (!s.isClosed()) {
			Client c = new Client(HOST, port, ":SHUTDOWN");
			Thread t = new Thread(c);
			t.start();
		}
	}

	@Test(timeout = 6000)
	public void testInvalidCommand() throws InterruptedException, IOException {

		String cmd = ":INVALID_COMMAND";
		Client c1 = new Client(HOST, port, cmd);
		Thread ct1 = new Thread(c1);
		ct1.start();
		ct1.join();
		assertEquals(ERR_INVALID_COMMAND, c1.getResult());

		cmd = ":SHUTDOWN";
		Client c2 = new Client(HOST, port, cmd);
		Thread ct2 = new Thread(c2);
		ct2.start();
		ct2.join();
	}

	@Test(timeout = 6000)
	public void testPortValidity() {
		String em = "isPortValid() not working correctly";
		assertEquals(em, false, SafeWalkServer.isPortValid("25"));
		assertEquals(em, false, SafeWalkServer.isPortValid("abcd"));
		assertEquals(em, true, SafeWalkServer.isPortValid("2981"));
		assertEquals(em, false, SafeWalkServer.isPortValid("ab23"));
		assertEquals(em, false, SafeWalkServer.isPortValid("11"));
		assertEquals(em, false, SafeWalkServer.isPortValid("122213234"));
	}

	@Test(timeout = 6000)
	public void testReset() throws InterruptedException {

		Client c1 = new Client(HOST, port, "Al,LWSN,PMU");
		Thread tc1 = new Thread(c1);
		tc1.start();
		Client c3 = new Client(HOST, port, "Alicia,LWSN,EE");
		Thread tc3 = new Thread(c3);
		tc3.start();
		Thread.sleep(100);

		Client c2 = new Client(HOST, port, ":RESET");
		Thread tc2 = new Thread(c2);
		tc2.start();

		tc1.join();
		tc2.join();

		assertEquals("ERROR: connection reset", c1.getResult());
		assertEquals("ERROR: connection reset", c3.getResult());
		assertEquals("RESPONSE: success", c2.getResult());
	}

	@Test(timeout = 6000)
	public void testShutdown() throws InterruptedException {

		Client c1 = new Client(HOST, port, "Al,LWSN,PMU");
		Thread tc1 = new Thread(c1);
		tc1.start();
		Thread.sleep(100);

		Client c3 = new Client(HOST, port, "Alicia,LWSN,EE");
		Thread tc3 = new Thread(c3);
		tc3.start();
		Thread.sleep(100);

		Client c2 = new Client(HOST, port, ":SHUTDOWN");
		Thread tc2 = new Thread(c2);
		tc2.start();

		tc1.join();
		tc2.join();

		assertEquals("ERROR: connection reset", c1.getResult());
		assertEquals("ERROR: connection reset", c3.getResult());
		assertEquals("RESPONSE: success", c2.getResult());
		assertEquals(true, s.isClosed());
	}

	@Test(timeout = 6000)
	public void testListPendingRequests() throws InterruptedException,
			IOException {

		Client c1 = new Client(HOST, port, "Al,LWSN,PMU", TIMEOUT);
		Thread ct1 = new Thread(c1);
		ct1.start();

		Thread.sleep(100);

		Client c2 = new Client(HOST, port, ":PENDING_REQUESTS,#,*,*");
		Thread ct2 = new Thread(c2);
		ct2.start();

		ct1.join();
		ct2.join();

		String exp = "RESPONSE: # of pending requests = 1";
		assertEquals(exp, c2.getResult());

		Client c3 = new Client(HOST, port, ":SHUTDOWN");
		Thread ct3 = new Thread(c3);
		ct3.start();
		ct3.join();
	}

	@Test
	public void testListPendingRequestsOrder() throws InterruptedException,
			IOException {

		String cmd = "Danushka,LWSN,PUSH";
		Client c1 = new Client(HOST, port, cmd, TIMEOUT);
		Thread ct1 = new Thread(c1);
		ct1.start();

		Thread.sleep(100);

		cmd = "Dinushi,LWSN,EE";
		Client c2 = new Client(HOST, port, cmd, TIMEOUT);
		Thread ct2 = new Thread(c2);
		ct2.start();

		ct1.join();
		ct2.join();

		cmd = ":PENDING_REQUESTS,*,*,*";
		Client c3 = new Client(HOST, port, cmd);
		Thread ct3 = new Thread(c3);
		ct3.start();
		ct3.join();

		String exp = "[[Danushka, LWSN, PUSH], [Dinushi, LWSN, EE]]";
		assertEquals(exp, c3.getResult());

		cmd = ":SHUTDOWN";
		Client c4 = new Client(HOST, port, ":SHUTDOWN");
		Thread ct4 = new Thread(c4);
		ct4.start();
		ct4.join();
	}

	/**
	 * Test a request with an invalid FROM.
	 **/
	@Test
	public void testInvalidFrom() throws InterruptedException, IOException {

		String cmd = "Danushka,FROM,PUSH";
		Client c1 = new Client(HOST, port, cmd);
		Thread ct1 = new Thread(c1);
		ct1.start();
		ct1.join();

		assertEquals(ERR_INVALID_REQUEST, c1.getResult());

		cmd = ":SHUTDOWN";
		Client c2 = new Client(HOST, port, cmd);
		Thread ct2 = new Thread(c2);
		ct2.start();

		ct2.join();

	}

	/**
	 * Test a request with an invalid TO.
	 **/
	@Test
	public void testInvalidTo() throws InterruptedException, IOException {

		String cmd = "Danushka,LWSN,TO";
		Client c1 = new Client(HOST, port, cmd);
		Thread ct1 = new Thread(c1);
		ct1.start();
		ct1.join();

		assertEquals(ERR_INVALID_REQUEST, c1.getResult());

		cmd = ":SHUTDOWN";
		Client c2 = new Client(HOST, port, cmd);
		Thread ct2 = new Thread(c2);
		ct2.start();

		ct2.join();

	}

	/**
	 * Test sending a request with an invalid delimiter.
	 **/
	@Test
	public void testInvalidRequest1() throws InterruptedException, IOException {

		String cmd = "Danushka:LWSN:TO";
		Client c1 = new Client(HOST, port, cmd);
		Thread ct1 = new Thread(c1);
		ct1.start();
		ct1.join();

		assertEquals(ERR_INVALID_REQUEST, c1.getResult());

		cmd = ":SHUTDOWN";
		Client c2 = new Client(HOST, port, cmd);
		Thread ct2 = new Thread(c2);
		ct2.start();

		ct2.join();

	}

	/**
	 * Test sending a request with an invalid number of fields.
	 **/
	@Test
	public void testInvalidRequest2() throws InterruptedException, IOException {

		String cmd = "Danushka,LWSN";
		Client c1 = new Client(HOST, port, cmd);
		Thread ct1 = new Thread(c1);
		ct1.start();
		ct1.join();

		assertEquals(ERR_INVALID_REQUEST, c1.getResult());

		cmd = ":SHUTDOWN";
		Client c2 = new Client(HOST, port, cmd);
		Thread ct2 = new Thread(c2);
		ct2.start();

		ct2.join();

	}

	/**
	 * Test sending a request with FROM = *.
	 **/
	@Test
	public void testFromStar() throws InterruptedException, IOException {

		String cmd = "Danushka,*,PUSH";
		Client c1 = new Client(HOST, port, cmd);
		Thread ct1 = new Thread(c1);
		ct1.start();
		ct1.join();

		assertEquals(ERR_INVALID_REQUEST, c1.getResult());

		cmd = ":SHUTDOWN";
		Client c2 = new Client(HOST, port, cmd);
		Thread ct2 = new Thread(c2);
		ct2.start();

		ct2.join();

	}

	/**
	 * Test sending a request with FROM = TO.
	 **/
	@Test
	public void testToEqualsFrom() throws InterruptedException, IOException {

		String cmd = "Danushka,PUSH,PUSH";
		Client c1 = new Client(HOST, port, cmd);
		Thread ct1 = new Thread(c1);
		ct1.start();
		ct1.join();

		assertEquals(ERR_INVALID_REQUEST, c1.getResult());

		cmd = ":SHUTDOWN";
		Client c2 = new Client(HOST, port, cmd);
		Thread ct2 = new Thread(c2);
		ct2.start();

		ct2.join();

	}

	/**
	 * Test a scenario where there is an exact match.
	 **/
	@Test
	public void testExactMatch() throws InterruptedException, IOException {

		String cmd = "Danushka,LWSN,PUSH";
		Client c1 = new Client(HOST, port, cmd);
		Thread ct1 = new Thread(c1);
		ct1.start();

		cmd = "Dinushi,LWSN,PUSH";
		Client c2 = new Client(HOST, port, cmd);
		Thread ct2 = new Thread(c2);
		ct2.start();

		ct1.join();
		ct2.join();

		String exp = "RESPONSE: Dinushi,LWSN,PUSH";
		assertEquals(exp, c1.getResult());
		exp = "RESPONSE: Danushka,LWSN,PUSH";
		assertEquals(exp, c2.getResult());

		cmd = ":SHUTDOWN";
		Client c3 = new Client(HOST, port, cmd);
		Thread ct3 = new Thread(c3);
		ct3.start();

		ct3.join();

	}

	/**
	 * Test a scenario where the second request has * as TO..
	 **/
	@Test
	public void testAnyMatch() throws InterruptedException, IOException {

		String cmd = "Danushka,LWSN,PUSH";
		Client c1 = new Client(HOST, port, cmd);
		Thread ct1 = new Thread(c1);
		ct1.start();

		Thread.sleep(100);

		cmd = "Dinushi,LWSN,*";
		Client c2 = new Client(HOST, port, cmd);
		Thread ct2 = new Thread(c2);
		ct2.start();

		ct1.join();
		ct2.join();

		String exp = "RESPONSE: Dinushi,LWSN,*";
		assertEquals(exp, c1.getResult());
		exp = "RESPONSE: Danushka,LWSN,PUSH";
		assertEquals(exp, c2.getResult());

		cmd = ":SHUTDOWN";
		Client c3 = new Client(HOST, port, cmd);
		Thread ct3 = new Thread(c3);
		ct3.start();

		ct3.join();

	}

	/**
	 * Same as "testAnyMatch" but with a different order of requests.
	 **/
	@Test
	public void testAnyMatch2() throws InterruptedException, IOException {

		String cmd = "Danushka,LWSN,*";
		Client c1 = new Client(HOST, port, cmd);
		Thread ct1 = new Thread(c1);
		ct1.start();

		Thread.sleep(100);

		cmd = "Dinushi,LWSN,PUSH";
		Client c2 = new Client(HOST, port, cmd);
		Thread ct2 = new Thread(c2);
		ct2.start();

		ct1.join();
		ct2.join();

		String exp = "RESPONSE: Dinushi,LWSN,PUSH";
		assertEquals(exp, c1.getResult());
		exp = "RESPONSE: Danushka,LWSN,*";
		assertEquals(exp, c2.getResult());

		cmd = ":SHUTDOWN";
		Client c3 = new Client(HOST, port, cmd);
		Thread ct3 = new Thread(c3);
		ct3.start();

		ct3.join();

	}

	/**
	 * Test first-come-first-serve.
	 **/
	@Test
	public void testFCFS() throws InterruptedException, IOException {

		String cmd = "Dihein,LWSN,PUSH";
		Client c1 = new Client(HOST, port, cmd);
		Thread ct1 = new Thread(c1);
		ct1.start();

		Thread.sleep(100);

		cmd = "Danushka,LWSN,EE";
		Client c2 = new Client(HOST, port, cmd, TIMEOUT);
		Thread ct2 = new Thread(c2);
		ct2.start();

		Thread.sleep(100);

		cmd = "Dinushi,LWSN,*";
		Client c3 = new Client(HOST, port, cmd);
		Thread ct3 = new Thread(c3);
		ct3.start();

		ct1.join();
		ct2.join();
		ct3.join();

		String exp = "RESPONSE: Dinushi,LWSN,*";
		assertEquals(exp, c1.getResult());
		exp = "RESPONSE: Dihein,LWSN,PUSH";
		assertEquals(exp, c3.getResult());

		cmd = "Dinushi,LWSN,*";
		Client c4 = new Client(HOST, port, cmd);
		Thread ct4 = new Thread(c4);
		ct4.start();
		ct4.join();

		exp = "RESPONSE: Danushka,LWSN,EE";
		assertEquals(exp, c4.getResult());

		cmd = ":SHUTDOWN";
		Client c5 = new Client(HOST, port, cmd);
		Thread ct5 = new Thread(c5);
		ct5.start();

		ct5.join();

	}

	/**
	 * Test a scenario where there are two requests with TO = * but FROM is the
	 * same.
	 **/
	@Test
	public void testToBothStar() throws InterruptedException, IOException {

		String cmd = "Danushka,LWSN,*";
		Client c1 = new Client(HOST, port, cmd, TIMEOUT);
		Thread ct1 = new Thread(c1);
		ct1.start();

		Thread.sleep(100);

		cmd = "Dinushi,LWSN,*";
		Client c2 = new Client(HOST, port, cmd, TIMEOUT);
		Thread ct2 = new Thread(c2);
		ct2.start();

		Thread.sleep(100);

		cmd = ":PENDING_REQUESTS,*,*,*";
		Client c3 = new Client(HOST, port, cmd);
		Thread ct3 = new Thread(c3);
		ct3.start();
		ct3.join();
		ct1.join();
		ct2.join();

		String exp = "[]";
		assertEquals(exp, c3.getResult());

		cmd = ":SHUTDOWN";
		Client c4 = new Client(HOST, port, cmd);
		Thread ct4 = new Thread(c4);
		ct4.start();
		ct4.join();

	}

}
