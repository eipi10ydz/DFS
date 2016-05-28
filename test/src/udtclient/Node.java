package udtclient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;
import com.barchart.udt.net.NetInputStreamUDT;
import com.barchart.udt.net.NetOutputStreamUDT;

/**
 * 
 */

/**
 * @author wzy, ydz
 *
 */
public class Node {

	protected String ID;
	protected String user_name;
	protected Set<String> nodeIDs;
	protected Map<String, String> UName_ID;
	protected Thread node_thread;

	protected InetAddress IP_local;

	protected Queue<String> node_inserted_lm;
	protected Queue<String> node_deleted_lm;
	protected Thread link_maintainer_thread;

	protected Map<String, SocketUDT> links_p;
	protected Map<String, Thread> links_p_t;
	protected Map<String, ReentrantLock> links_p_l;

	protected NodeRouter router;
	protected Queue<String> node_inserted_rout;
	protected Queue<String> node_deleted_rout;

	protected InetAddress IP_local_server;
	protected int Port_local_server;
	protected String server_host;
	protected int server_port;
	protected InputStream in_s;
	protected OutputStream out_s;
	protected ReentrantLock server_link_lock;
	protected Thread server_link_thread;
	protected Map<String, Queue<Map<String, String>>> messages_from_server;

	protected Map<String, DataTransferor> data_arrived;
	protected ExecutorService data_to_send;

	/**
	 * @param user_name
	 * @param server_host
	 * @param server_port
	 * @throws ExceptionUDT
	 * @throws NodeException
	 */
	public Node(String user_name, String server_host, int server_port) throws ExceptionUDT, NodeException, PackException {
		this.user_name = user_name;
		nodeIDs = ConcurrentHashMap.<String> newKeySet();
		UName_ID = new ConcurrentHashMap<>();
		node_inserted_lm = new ConcurrentLinkedQueue<>();
		node_deleted_lm = new ConcurrentLinkedQueue<>();
		links_p = new ConcurrentHashMap<>();
		links_p_t = new ConcurrentHashMap<>();
		links_p_l = new ConcurrentHashMap<>();
		router = new NodeRouter(this);
		node_inserted_rout = new ConcurrentLinkedQueue<>();
		node_deleted_rout = new ConcurrentLinkedQueue<>();
		messages_from_server = new ConcurrentHashMap<>();
		messages_from_server.put("Node", new ConcurrentLinkedQueue<Map<String, String>>());
		messages_from_server.put("Link", new ConcurrentLinkedQueue<Map<String, String>>());
		messages_from_server.put("ERR", new ConcurrentLinkedQueue<Map<String, String>>());
		this.server_host = server_host;
		this.server_port = server_port;
		server_link_lock = new ReentrantLock();
		data_arrived = new ConcurrentHashMap<>();
		data_to_send = Executors.newCachedThreadPool();
		try {
			IP_local = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			IP_local = null;
			e.printStackTrace();
		}
		byte arr[] = new byte[1024];
		String str = null;
		Map<String, String> pac;
		SocketUDT server = new SocketUDT(TypeUDT.STREAM);
		server.setBlocking(true);
		server.connect(new InetSocketAddress(this.server_host, this.server_port));
		IP_local_server = server.getLocalInetAddress();
		Port_local_server = server.getLocalInetPort();
		in_s = new NetInputStreamUDT(server);// currently not in use
		out_s = new NetOutputStreamUDT(server);// currently not in use
		pac = new ConcurrentHashMap<String, String>();
		pac.put("Insertion", "Insertion");
		try {
			str = Packer.pack("NodeI", "00", pac);
		} catch (NodeException e) {// just used for debug
			e.printStackTrace();
		}
		server.send(str.getBytes(Charset.forName("ISO-8859-1")));// Insertion
		server.receive(arr);
		str = new String(arr, Charset.forName("ISO-8859-1")).trim();
		empty_arr(str.length(), arr);
		pac = Packer.unpack(str);
		ID = pac.get("ID");
		pac = new ConcurrentHashMap<String, String>();
		pac.put("UName", this.user_name);
		try {
			str = Packer.pack("NodeI", "03", pac);
		} catch (NodeException e) {// just used for debug
			e.printStackTrace();
		}
		server.send(str.getBytes(Charset.forName("ISO-8859-1")));// send user
																	// name
		server.receive(arr);// receive node and user name table
		str = new String(arr, Charset.forName("ISO-8859-1")).trim();
		pac = Packer.Check_table(str);
		int cnt = Integer.parseInt(pac.get("cnt"));
		String s1 = new String("UName_");
		String s2 = new String("ID_");
		for (int i = 1; i <= cnt; i++) {
			nodeIDs.add(pac.get(s2 + i));
			UName_ID.put(pac.get(s1 + i), pac.get(s2 + i));
		}
                System.out.println("local:" + this.IP_local_server.toString() + ":" + this.Port_local_server);
		link_maintainer_thread = new Thread(new LinkMaintainer(this));
		link_maintainer_thread.start();
		node_thread = new Thread(new NodeThread(this));
		node_thread.start();
		server_link_thread = new Thread(new ServerLink(this));
		server_link_thread.start();
	}

	/**
	 * @param length
	 * @param arr
	 */
	protected void empty_arr(int length, byte arr[]) {
		for (int i = 0; i < length; i++) {
			arr[i] = ' ';
		}
	}

	/**
	 * @param str
	 * @param ID_p
	 * @return result
	 */
	public Future<Boolean> transfer_message(String str, String ID_p) {
		if (str == null || str.isEmpty()) {
			throw new IllegalArgumentException("Empty String.");
		}
		if (nodeIDs.contains(ID_p)) {// Launch a new task and hold its result
			Future<Boolean> result;
			result = data_to_send.submit(new DataSender(this, ID_p, str));
			return result;
		} else {
			throw new IllegalArgumentException("The destination isn't exist.");
		}
	}

	/**
	 * @param obj
	 * @param peer
	 * @return result
	 * @throws IOException
	 */
	public Future<Boolean> transfer_message(Object obj, String peer) throws IOException {
		if (obj == null) {
			throw new IllegalArgumentException("Null Object.");
		}
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		ObjectOutputStream so = new ObjectOutputStream(bo);
		so.writeObject(obj);
		so.flush();
		String str = bo.toString();
		so.close();
		bo.close();
		String ID_p = UName_ID.get("peer");
		if (ID_p != null && nodeIDs.contains(ID_p)) {// Launch a new task and
														// hold its result
			Future<Boolean> result;
			result = data_to_send.submit(new DataSender(this, ID_p, str));
			return result;
		} else {
			throw new IllegalArgumentException("The destination dose not exist.");
		}
	}
}
