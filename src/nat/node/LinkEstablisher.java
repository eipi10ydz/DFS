/**
 * 
 */
package data_transferor;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;

/**
 * @author wzy
 *
 */
class LinkEstablisher {

	protected Node node;

	/**
	 * @param node
	 */
	protected LinkEstablisher(Node node) {
		this.node = node;
	}

	/**
	 * @param ID_p
	 * @return
	 * @throws NodeException
	 * @throws PackException
	 * @throws ExceptionUDT
	 */
	protected SocketUDT establish_link_m(String ID_p) throws NodeException, ExceptionUDT, PackException {
		if (node.ID.equals(ID_p) || !node.nodeIDs.contains(ID_p))
			throw new IllegalArgumentException("the ID of peer node is not in the node list.");
		byte arr[] = new byte[1024];
		String str = new String();
		Map<String, String> pac;
		String IP_p = new String();
		int Port_p = 0;
		SocketUDT sock = new SocketUDT(TypeUDT.STREAM);
		sock.setBlocking(true);
		try {
			// direct connect
			sock.connect(new InetSocketAddress(node.node_IPs.get(ID_p), 2333));
			pac = new ConcurrentHashMap<String, String>();
			pac.put("ID", node.ID);
			try {
				str = Packer.pack("LinkE", "04", pac);
			} catch (PackException e) {// just used for debug
				e.printStackTrace();
			}
			sock.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																	// packet
																	// LinkE04
			sock.receive(arr);// receive packet LinkE04
			str = new String(arr, Charset.forName("ISO-8859-1")).trim();
			node.empty_arr(str.length(), arr);
			pac = Packer.unpack(str);
			if (!(pac.containsKey("type") && pac.containsKey("type_d") && pac.containsKey("ID")
					&& pac.get("type").equals("LinkE") && pac.get("type_d").equals("04")
					&& pac.get("type_d").equals(ID_p))) {
				throw new NodeException("Error on the peer side.");
			}
			InetSocketAddress local_address = sock.getLocalSocketAddress();
			sock.close();
			SocketUDT socket = new SocketUDT(TypeUDT.STREAM);
			socket.setBlocking(true);
			socket.bind(local_address);
			socket.setSoTimeout(1000);
			sock = socket.accept();
			sock.receive(arr);// receive packet LinkE04
			str = new String(arr, Charset.forName("ISO-8859-1")).trim();
			node.empty_arr(str.length(), arr);
			pac = Packer.unpack(str);
			if (!(pac.containsKey("type") && pac.containsKey("type_d") && pac.containsKey("ID")
					&& pac.get("type").equals("LinkE") && pac.get("type_d").equals("04")
					&& pac.get("type_d").equals(ID_p))) {
				try {
					sock.close();
				} catch (ExceptionUDT e1) {
					e1.printStackTrace();
				}
				throw new NodeException("Unexpected packet from the peer node.");
			}
			sock.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																	// packet
																	// LinkE04
			tell_server(ID_p, true);
			return sock;
		} catch (ExceptionUDT e) {
			try {
				sock.close();
			} catch (ExceptionUDT e1) {
			}
			e.printStackTrace();
		}
		// traverse
		SocketUDT server = new SocketUDT(TypeUDT.STREAM);
		server.setBlocking(true);
		sock = new SocketUDT(TypeUDT.STREAM);
		sock.setBlocking(true);
		try {
			server.connect(new InetSocketAddress(node.server_host, node.server_port));
			pac = new ConcurrentHashMap<String, String>();
			pac.put("ID", node.ID);
			pac.put("ID_target", ID_p);
			try {
				str = Packer.pack("LinkE", "01", pac);
			} catch (PackException e1) {// just used for debug
				e1.printStackTrace();
			}
			server.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																		// packet
																		// LinkE01
			server.receive(arr);// receive packet LinkE03
			str = new String(arr, Charset.forName("ISO-8859-1")).trim();
			node.empty_arr(str.length(), arr);
			pac = Packer.unpack(str);
			InetSocketAddress local_address = server.getLocalSocketAddress();
			server.close();
			if (pac.get("type").equals("ERR")) {// another Node is trying to
												// connect with it.
				throw new NodeException("Application denied by server.");
			} else if (!pac.get("ID").equals(ID_p)) {// Error on the server
														// side.
				throw new NodeException("Error on the server side.");
			}
			IP_p = pac.get("IP");
			Port_p = Integer.parseInt(pac.get("Port"));
			sock.bind(local_address);
			sock.setRendezvous(true);
			try {
				sock.connect(new InetSocketAddress(IP_p, Port_p));
				pac = new ConcurrentHashMap<String, String>();
				pac.put("ID", node.ID);
				try {
					str = Packer.pack("LinkE", "04", pac);
				} catch (PackException e) {// just used for debug
					e.printStackTrace();
				}
				sock.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																		// packet
																		// LinkE04
				sock.receive(arr);// receive packet LinkE04
				str = new String(arr, Charset.forName("ISO-8859-1")).trim();
				node.empty_arr(str.length(), arr);
				pac = Packer.unpack(str);
				if (!(pac.containsKey("type") && pac.containsKey("type_d") && pac.containsKey("ID")
						&& pac.get("type").equals("LinkE") && pac.get("type_d").equals("04")
						&& pac.get("type_d").equals(ID_p))) {
					try {
						sock.close();
					} catch (ExceptionUDT e1) {
						e1.printStackTrace();
					}
					throw new NodeException("Unexpected packet from the peer node.");
				}
			} catch (ExceptionUDT e) {
				e.printStackTrace();
				tell_server(ID_p, false);
				return null;
			}
			tell_server(ID_p, true);
		} catch (ExceptionUDT e) {
			try {
				sock.close();
			} catch (ExceptionUDT e1) {
				e1.printStackTrace();
			}
			throw e;
		} catch (PackException e) {
			throw e;
		} finally {
			try {
				server.close();
			} catch (ExceptionUDT e) {
				e.printStackTrace();
			}
		}
		return sock;
	}

	/**
	 * @param ID_p
	 * @param sock
	 * @return
	 * @throws ExceptionUDT
	 * @throws NodeException
	 */
	protected boolean establish_link_s1(String ID_p, SocketUDT sock) throws ExceptionUDT, NodeException {
		if (node.ID.equals(ID_p))
			throw new IllegalArgumentException("the ID of peer node is not in the node list.");
		byte arr[] = new byte[1024];
		String str = new String();
		Map<String, String> pac;
		InetSocketAddress remote_address = sock.getRemoteSocketAddress();
		if (!node.node_IPs.get(ID_p).equals(remote_address.getAddress().getHostAddress())) {
			try {
				sock.close();
			} catch (ExceptionUDT e) {
				e.printStackTrace();
			}
			return false;
		}
		// direct connect
		pac = new ConcurrentHashMap<String, String>();
		pac.put("ID", node.ID);
		try {
			str = Packer.pack("LinkE", "04", pac);
		} catch (PackException e) {// just used for debug
			e.printStackTrace();
		}
		try {
			sock.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																	// packet
																	// LinkE04
			sock.close();
			sock = new SocketUDT(TypeUDT.STREAM);
			sock.setBlocking(true);
			sock.connect(remote_address);
			sock.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																	// packet
																	// LinkE04
			sock.receive(arr);// receive packet LinkE04
			str = new String(arr, Charset.forName("ISO-8859-1")).trim();
			node.empty_arr(str.length(), arr);
			pac = Packer.unpack(str);
			if (!(pac.containsKey("type") && pac.containsKey("type_d") && pac.containsKey("ID")
					&& pac.get("type").equals("LinkE") && pac.get("type_d").equals("04")
					&& pac.get("type_d").equals(ID_p))) {
				try {
					sock.close();
				} catch (ExceptionUDT e1) {
					e1.printStackTrace();
				}
				throw new NodeException("Unexpected packet from the peer node.");
			}
		} catch (ExceptionUDT e) {
			e.printStackTrace();
			try {
				sock.close();
			} catch (ExceptionUDT e1) {
				e1.printStackTrace();
			}
			tell_server(ID_p, false);
			return false;
		} catch (PackException e) {
			e.printStackTrace();
			try {
				sock.close();
			} catch (ExceptionUDT e1) {
				e1.printStackTrace();
			}
			tell_server(ID_p, false);
			return false;
		}
		tell_server(ID_p, true);
		Thread t = new Thread(new NodeLink(ID_p, sock, node));
		t.start();
		return true;
	}

	/**
	 * @param ID_p
	 * @param IP_p
	 * @param Port_p
	 * @return
	 * @throws ExceptionUDT
	 * @throws NodeException
	 * @throws PackException
	 */
	protected boolean establish_link_s2(String ID_p, String IP_p, int Port_p)
			throws NodeException, ExceptionUDT, PackException {
		if (node.ID.equals(ID_p) || (IP_p == null) || (IP_p.length() == 0))
			throw new IllegalArgumentException("the ID of peer node is not in the node list.");
		if (!node.nodeIDs.contains(ID_p)) {
			node.nodeIDs.add(ID_p);
		}
		byte arr[] = new byte[1024];
		String str = new String();
		Map<String, String> pac;
		// traverse
		SocketUDT server = new SocketUDT(TypeUDT.STREAM);
		server.setBlocking(true);
		SocketUDT sock = new SocketUDT(TypeUDT.STREAM);
		sock.setBlocking(true);
		try {
			server.connect(new InetSocketAddress(node.server_host, node.server_port));
			pac = new ConcurrentHashMap<String, String>();
			pac.put("ID", node.ID);
			pac.put("ID_target", ID_p);
			try {
				str = Packer.pack("LinkE", "02", pac);
			} catch (PackException e1) {// just used for debug
				e1.printStackTrace();
			}
			server.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																		// packet
																		// LinkE02
			server.receive(arr);// receive packet LinkE05
			str = new String(arr, Charset.forName("ISO-8859-1")).trim();
			node.empty_arr(str.length(), arr);
			pac = Packer.unpack(str);
			InetSocketAddress local_address = server.getLocalSocketAddress();
			server.close();
			if (!pac.get("ID").equals(ID_p)) {// Error on the server side.
				throw new NodeException("Error on the server side.");
			}
			sock.bind(local_address);
			sock.setRendezvous(true);
			try {
				sock.connect(new InetSocketAddress(IP_p, Port_p));
				pac = new ConcurrentHashMap<String, String>();
				pac.put("ID", node.ID);
				try {
					str = Packer.pack("LinkE", "04", pac);
				} catch (PackException e) {// just used for debug
					e.printStackTrace();
				}
				sock.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																		// packet
																		// LinkE04
				sock.receive(arr);// receive packet LinkE04
				str = new String(arr, Charset.forName("ISO-8859-1")).trim();
				node.empty_arr(str.length(), arr);
				pac = Packer.unpack(str);
				if (!(pac.containsKey("type") && pac.containsKey("type_d") && pac.containsKey("ID")
						&& pac.get("type").equals("LinkE") && pac.get("type_d").equals("04")
						&& pac.get("type_d").equals(ID_p))) {
					try {
						sock.close();
					} catch (ExceptionUDT e1) {
						e1.printStackTrace();
					}
					throw new NodeException("Unexpected packet from the peer node.");
				}
			} catch (ExceptionUDT e) {
				e.printStackTrace();
				tell_server(ID_p, false);
				return false;
			}
			tell_server(ID_p, true);
		} catch (ExceptionUDT e) {
			try {
				server.close();
			} catch (ExceptionUDT e1) {
				e1.printStackTrace();
			}
			throw e;
		} catch (PackException e) {
			try {
				server.close();
			} catch (ExceptionUDT e1) {
				e1.printStackTrace();
			}
			throw e;
		}
		Thread t = new Thread(new NodeLink(ID_p, sock, node));
		t.start();
		return true;
	}

	/**
	 * @param ID_p
	 * @param result
	 * @throws ExceptionUDT
	 */
	protected void tell_server(String ID_p, Boolean result) throws ExceptionUDT {
		String str = new String();
		Map<String, String> pac;
		SocketUDT server = new SocketUDT(TypeUDT.STREAM);
		server.setBlocking(true);
		server.connect(new InetSocketAddress(node.server_host, node.server_port));
		pac = new ConcurrentHashMap<String, String>();
		pac.put("ID", node.ID);
		pac.put("ID_target", ID_p);
		pac.put("Connectivity", result == true ? "true" : "false");
		try {
			str = Packer.pack("LinkC", pac);
		} catch (PackException e1) {// just used for debug
			e1.printStackTrace();
		}
		try {
			server.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																		// packet
																		// LinkC
		} catch (ExceptionUDT e) {
			throw e;
		} finally {
			try {
				server.close();
			} catch (ExceptionUDT e) {
				e.printStackTrace();
			}
		}
		return;
	}

}
