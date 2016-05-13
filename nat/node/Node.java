import java.io.File;
import java.io.InputStream;
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
import java.util.concurrent.atomic.AtomicInteger;
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
 * @author wzy
 *
 */
public class Node {

	protected String ID;
	protected Set<String> nodeIDs;
	protected Thread node_thread;

	protected InetAddress IP_local;
	protected InetAddress IP_local_nat;
	protected int Port_local_nat;

	protected Queue<String> node_inserted_lm;
	protected Queue<String> node_deleted_lm;
	protected Thread link_maintainer_thread;

	protected Map<String, SocketUDT> links_p;
	protected Map<String, Thread> links_p_t;

	protected Queue<String> node_inserted_rout;
	protected Queue<String> node_deleted_rout;

	protected SocketUDT server;
	protected String server_host;
	protected int server_port;
	protected InputStream in_s;
	protected OutputStream out_s;
	protected ReentrantLock server_link_lock;
	protected AtomicInteger server_link_count;
	protected Thread server_link_thread;
	protected Map<String, Queue<Map<String, String>>> messages_from_server;

	/**
	 * @param server_host
	 * @param server_port
	 * @throws ExceptionUDT 
	 */
	public Node(String server_host, int server_port) throws ExceptionUDT {
		nodeIDs = ConcurrentHashMap.<String> newKeySet();
		node_inserted_lm = new ConcurrentLinkedQueue<>();
		node_deleted_lm = new ConcurrentLinkedQueue<>();
		links_p = new ConcurrentHashMap<>();
		links_p_t = new ConcurrentHashMap<>();
		node_inserted_rout = new ConcurrentLinkedQueue<>();
		node_deleted_rout = new ConcurrentLinkedQueue<>();
		messages_from_server = new ConcurrentHashMap<>();
		messages_from_server.put("Node", new ConcurrentLinkedQueue<Map<String, String>>());
		messages_from_server.put("Link", new ConcurrentLinkedQueue<Map<String, String>>());
		messages_from_server.put("ERR", new ConcurrentLinkedQueue<Map<String, String>>());
		this.server_host = server_host;
		this.server_port = server_port;
		server_link_lock = new ReentrantLock();
		server_link_count = new AtomicInteger(0);
		try {
			IP_local = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			IP_local = null;
			e.printStackTrace();
		}
		byte arr[] = new byte[1024];
		String str;
		Map<String, String> pac;
		try {
			server = new SocketUDT(TypeUDT.STREAM);
			server.setBlocking(true);
			server.connect(new InetSocketAddress(server_host, server_port));
			server.setSoTimeout(10000);
			IP_local_nat = server.getLocalInetAddress();
			Port_local_nat = server.getLocalInetPort();
			in_s = new NetInputStreamUDT(server);// currently not in use
			out_s = new NetOutputStreamUDT(server);// currently not in use
			server.receive(arr);
			str = new String(arr, Charset.forName("ISO-8859-1")).trim();
			pac = Packer.unpack(str);
			ID = pac.get("ID");
		} catch (ExceptionUDT e) {
			// TODO Auto-generated catch block
			throw e;
		}
		// TODO receive NodeID table
		link_maintainer_thread = new Thread(new LinkMaintainer(this));
		link_maintainer_thread.start();
		node_thread = new Thread(new NodeThread(this));
		node_thread.start();
		server_link_thread = new Thread(new ServerLink(this));
		server_link_thread.start();
	}

	/**
	 * @param file
	 * @param ID_p
	 * @return
	 */
	public boolean transfer_file(File file, String ID_p) {
		return false;

	}
}
