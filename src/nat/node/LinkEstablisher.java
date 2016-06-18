/**
 * 
 */
package data_transferor;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;

/**
 * @author wzy, ydz
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
	 * @throws LinkException
	 * @throws NodeException
	 */
	protected SocketUDT establish_link_m(String ID_p) throws LinkException, NodeException {
		if (node.ID.equals(ID_p) || !node.nodeIDs.contains(ID_p))
			throw new IllegalArgumentException("the ID of peer node is not in the node list.");
		node.link_establish_locks.get(ID_p).lock();
		try {
			byte arr[] = new byte[1024];
			String str = new String();
			Map<String, String> pac;
			String IP_p = new String();
			int Port_p = 0;
			// direct connect
			SocketUDT sock = null;
			try {
				sock = new SocketUDT(TypeUDT.STREAM);
				sock.setBlocking(true);
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
				Node.empty_arr(str.length(), arr);
				try {
					pac = Packer.unpack(str);
				} catch (PackException e) {
					throw new LinkException("Unexpected packet from the peer node.", e);
				}
				if (!(pac.containsKey("type") && pac.containsKey("type_d") && pac.containsKey("ID")
						&& pac.get("type").equals("LinkE") && pac.get("type_d").equals("04")
						&& pac.get("ID").equals(ID_p))) {
					throw new LinkException("Unexpected packet from the peer node." + pac.toString());
				}
				InetSocketAddress local_address = sock.getLocalSocketAddress();
				sock.close();
				SocketUDT socket = new SocketUDT(TypeUDT.STREAM);
				socket.setBlocking(true);
				socket.bind(local_address);
				socket.setSoTimeout(1000);
				socket.listen(5);
				sock = socket.accept();
				sock.receive(arr);// receive packet LinkE04
				str = new String(arr, Charset.forName("ISO-8859-1")).trim();
				Node.empty_arr(str.length(), arr);
				try {
					pac = Packer.unpack(str);
				} catch (PackException e) {
					throw new LinkException("Unexpected packet from the peer node.", e);
				}
				if (!(pac.containsKey("type") && pac.containsKey("type_d") && pac.containsKey("ID")
						&& pac.get("type").equals("LinkE") && pac.get("type_d").equals("04")
						&& pac.get("ID").equals(ID_p))) {
					throw new LinkException("Unexpected packet from the peer node." + pac.toString());
				}
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
				while (true) {
					try {
						tell_server(ID_p, true);
						break;
					} catch (ExceptionUDT e) {
						// throw new NodeException("Failed to inform the server
						// of the result.");
					}
				}
				try {
					Thread.sleep(700);// tricky
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return sock;
			} catch (LinkException | ExceptionUDT e) {
				try {
					sock.close();
				} catch (ExceptionUDT e1) {
				}
				e.printStackTrace();
			}
			// direct connect from the peer
			SocketUDT server = null;
			try {
				server = new SocketUDT(TypeUDT.STREAM);
				server.setBlocking(true);
				server.connect(new InetSocketAddress(node.server_host, node.server_port));
				pac = new ConcurrentHashMap<String, String>();
				pac.put("ID", node.ID);
				pac.put("ID_target", ID_p);
				try {
					str = Packer.pack("LinkE", "06", pac);
				} catch (PackException e1) {// just used for debug
					e1.printStackTrace();
				}
				server.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																			// packet
																			// LinkE06
				Timer timer = new Timer(10000);// TODO 10s - may be too short?
				timer.start();
				while ((sock = node.link_establish_socks.remove(ID_p)) == null) {
					if (timer.isExpired())// timeout
					{
						throw new LinkException("No response from the peer node.");
					}
				}
				// similar with establish_link_s1
				InetSocketAddress remote_address = sock.getRemoteSocketAddress();
				InetSocketAddress local = sock.getLocalSocketAddress();
				pac = new ConcurrentHashMap<String, String>();
				pac.put("ID", node.ID);
				try {
					str = Packer.pack("LinkE", "08", pac);
				} catch (PackException e) {// just used for debug
					e.printStackTrace();
				}
				sock.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																		// packet
																		// LinkE08
				sock.close();
				sock = new SocketUDT(TypeUDT.STREAM);
				sock.setBlocking(true);
				sock.bind(local);
				sock.connect(remote_address);
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
				Node.empty_arr(str.length(), arr);
				try {
					pac = Packer.unpack(str);
				} catch (PackException e) {
					throw new LinkException("Unexpected packet from the peer node.", e);
				}
				if (!(pac.containsKey("type") && pac.containsKey("type_d") && pac.containsKey("ID")
						&& pac.get("type").equals("LinkE") && pac.get("type_d").equals("04")
						&& pac.get("ID").equals(ID_p))) {
					throw new LinkException("Unexpected packet from the peer node." + pac.toString());
				}
				while (true) {
					try {
						tell_server(ID_p, true);
						break;
					} catch (ExceptionUDT e) {
						// throw new NodeException("Failed to inform the server
						// of the result.");
					}
				}
				try {
					Thread.sleep(700);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				return sock;
			} catch (LinkException | ExceptionUDT e) {
				try {
					sock.close();
				} catch (ExceptionUDT e1) {
				}
				e.printStackTrace();
			} finally {
				try {
					server.close();
				} catch (ExceptionUDT e) {
				}
			}
			// traverse
			try {
				server = new SocketUDT(TypeUDT.STREAM);
				server.setBlocking(true);
				sock = new SocketUDT(TypeUDT.STREAM);
				sock.setBlocking(true);
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
				Node.empty_arr(str.length(), arr);
				try {
					pac = Packer.unpack(str);
				} catch (PackException e) {
					throw new LinkException("Unexpected packet from the server.", e);
				}
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
					if (sock.isConnected()) {
						sock.send(" ".getBytes());
						System.out.println("connect success");
					}
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
					Node.empty_arr(str.length(), arr);
					try {
						pac = Packer.unpack(str);
					} catch (PackException e) {
						throw new LinkException("Unexpected packet from the peer node.", e);
					}
					if (!(pac.containsKey("type") && pac.containsKey("type_d") && pac.containsKey("ID")
							&& pac.get("type").equals("LinkE") && pac.get("type_d").equals("04")
							&& pac.get("ID").equals(ID_p))) {
						throw new LinkException("Unexpected packet from the peer node." + pac.toString());
					}
				} catch (ExceptionUDT | LinkException e) {
					try {
						sock.close();
					} catch (ExceptionUDT e1) {
					}
					Logger.getLogger(LinkEstablisher.class.getName()).log(Level.FINER, "Failed to connect.", e);
					while (true) {
						try {
							tell_server(ID_p, false);
							break;
						} catch (ExceptionUDT e1) {
							// throw new NodeException("Failed to inform the
							// server of the result.");
						}
					}
					return null;
				}
			} catch (ExceptionUDT e) {
				throw new LinkException(e);
			} catch (LinkException e) {
				throw e;
			} finally {
				try {
					sock.close();
				} catch (ExceptionUDT e) {
				}
			}
			while (true) {
				try {
					tell_server(ID_p, true);
					break;
				} catch (ExceptionUDT e) {
					// throw new NodeException("Failed to inform the server of
					// the result.");
				}
			}
			try {
				Thread.sleep(700);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return sock;
		} finally

		{
			node.link_establish_locks.get(ID_p).unlock();
		}
	}

	/**
	 * @param ID_p
	 * @param sock
	 * @return
	 * @throws LinkException
	 */
	protected boolean establish_link_s1(String ID_p, SocketUDT sock) throws LinkException {
		if (node.ID.equals(ID_p))
			throw new IllegalArgumentException("the ID of peer node is not in the node list.");
		byte arr[] = new byte[1024];
		String str = new String();
		Map<String, String> pac;
		InetSocketAddress remote_address = null;
		InetSocketAddress local = null;
		// direct connect
		try {
			remote_address = sock.getRemoteSocketAddress();
			local = sock.getLocalSocketAddress();
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
			sock.close();
			sock = new SocketUDT(TypeUDT.STREAM);
			sock.setBlocking(true);
			sock.bind(local);
			sock.connect(remote_address);
			sock.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																	// packet
																	// LinkE04
			sock.receive(arr);// receive packet LinkE04
			str = new String(arr, Charset.forName("ISO-8859-1")).trim();
			Node.empty_arr(str.length(), arr);
			try {
				pac = Packer.unpack(str);
			} catch (PackException e) {
				throw new LinkException("Unexpected packet from the peer node.", e);
			}
			if (!(pac.containsKey("type") && pac.containsKey("type_d") && pac.containsKey("ID")
					&& pac.get("type").equals("LinkE") && pac.get("type_d").equals("04")
					&& pac.get("ID").equals(ID_p))) {
				try {
					sock.close();
				} catch (ExceptionUDT e1) {
					e1.printStackTrace();
				}
				throw new LinkException("Unexpected packet from the peer node." + pac.toString());
			}
		} catch (LinkException e) {
			throw e;
		} catch (ExceptionUDT e) {
			throw new LinkException(e);
		} finally {
			try {
				sock.close();
			} catch (ExceptionUDT e) {
			}
		}
		while (true) {
			try {
				tell_server(ID_p, true);
				break;
			} catch (ExceptionUDT e) {
				// throw new NodeException("Failed to inform the server of the
				// result.");
			}
		}
		Thread t = new Thread(new NodeLink(ID_p, sock, node));
		t.start();
		return true;
	}

	/**
	 * @param ID_p
	 * @param IP_p
	 * @param Port_p
	 * @return
	 * @throws LinkException
	 */
	protected boolean establish_link_s2(String ID_p, String IP_p, int Port_p) throws LinkException {
		if (node.ID.equals(ID_p) || (IP_p == null) || (IP_p.length() == 0))
			throw new IllegalArgumentException("the ID of peer node is incorrect.");
		if (!node.nodeIDs.contains(ID_p)) {
			node.nodeIDs.add(ID_p);
		}
		byte arr[] = new byte[1024];
		String str = null;
		Map<String, String> pac;
		SocketUDT server = null, sock = null;
		// traverse
		try {
			server = new SocketUDT(TypeUDT.STREAM);
			server.setBlocking(true);
			sock = new SocketUDT(TypeUDT.STREAM);
			sock.setBlocking(true);
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
			Node.empty_arr(str.length(), arr);
			try {
				pac = Packer.unpack(str);
			} catch (PackException e) {
				throw new LinkException("Unexpected packet from the server.", e);
			}
			InetSocketAddress local_address = server.getLocalSocketAddress();
			server.close();
			sock.bind(local_address);
			sock.setRendezvous(true);
			try {
				sock.connect(new InetSocketAddress(IP_p, Port_p));
				if (sock.isConnected()) {
					sock.send(" ".getBytes());
					System.out.println("connect success");
				}
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
				Node.empty_arr(str.length(), arr);
				try {
					pac = Packer.unpack(str);
				} catch (PackException e) {
					throw new LinkException("Unexpected packet from the peer node.", e);
				}
				if (!(pac.containsKey("type") && pac.containsKey("type_d") && pac.containsKey("ID")
						&& pac.get("type").equals("LinkE") && pac.get("type_d").equals("04")
						&& pac.get("ID").equals(ID_p))) {
					throw new LinkException("Unexpected packet from the peer node." + pac.toString());
				}
			} catch (ExceptionUDT | LinkException e) {
				try {
					sock.close();
				} catch (ExceptionUDT e1) {
				}
				Logger.getLogger(LinkEstablisher.class.getName()).log(Level.FINER, "Failed to connect.", e);
				while (true) {
					try {
						tell_server(ID_p, false);
						break;
					} catch (ExceptionUDT e1) {
						// throw new NodeException("Failed to inform the server
						// of the result.");
					}
				}
				return false;
			}
		} catch (LinkException e) {
			throw e;
		} catch (ExceptionUDT e) {
			throw new LinkException(e);
		} finally {
			try {
				server.close();
			} catch (ExceptionUDT e) {
			}
		}
		while (true) {
			try {
				tell_server(ID_p, true);
				break;
			} catch (ExceptionUDT e) {
				// throw new NodeException("Failed to inform the server of the
				// result.");
			}
		}
		Thread t = new Thread(new NodeLink(ID_p, sock, node));
		t.start();
		return true;
	}

	/**
	 * @param ID_p
	 * @return
	 * @throws LinkException
	 */
	public boolean establish_link_s3(String ID_p) throws LinkException {
		byte arr[] = new byte[1024];
		String str = null;
		Map<String, String> pac = null;
		SocketUDT sock = null;
		// direct connect from the peer
		try {
			sock = new SocketUDT(TypeUDT.STREAM);
			sock.setBlocking(true);
			sock.connect(new InetSocketAddress(node.node_IPs.get(ID_p), 2333));
			pac = new ConcurrentHashMap<String, String>();
			pac.put("ID", node.ID);
			try {
				str = Packer.pack("LinkE", "08", pac);
			} catch (PackException e) {// just used for debug
				e.printStackTrace();
			}
			sock.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																	// packet
																	// LinkE08
			sock.receive(arr);// receive packet LinkE08
			str = new String(arr, Charset.forName("ISO-8859-1")).trim();
			Node.empty_arr(str.length(), arr);
			try {
				pac = Packer.unpack(str);
			} catch (PackException e1) {
				throw new LinkException("Unexpected packet from the peer node.", e1);
			}
			if (!(pac.containsKey("type") && pac.containsKey("type_d") && pac.containsKey("ID")
					&& pac.get("type").equals("LinkE") && pac.get("type_d").equals("08")
					&& pac.get("ID").equals(ID_p))) {
				throw new LinkException("Unexpected packet from the peer node." + pac.toString());
			}
			InetSocketAddress local_address = sock.getLocalSocketAddress();
			sock.close();
			SocketUDT socket = new SocketUDT(TypeUDT.STREAM);
			socket.setBlocking(true);
			socket.bind(local_address);
			socket.setSoTimeout(1000);
			socket.listen(5);
			sock = socket.accept();
			sock.receive(arr);// receive packet LinkE04
			str = new String(arr, Charset.forName("ISO-8859-1")).trim();
			Node.empty_arr(str.length(), arr);
			try {
				pac = Packer.unpack(str);
			} catch (PackException e1) {
				throw new LinkException("Unexpected packet from the peer node.", e1);
			}
			if (!(pac.containsKey("type") && pac.containsKey("type_d") && pac.containsKey("ID")
					&& pac.get("type").equals("LinkE") && pac.get("type_d").equals("04")
					&& pac.get("ID").equals(ID_p))) {
				throw new LinkException("Unexpected packet from the peer node." + pac.toString());
			}
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
		} catch (ExceptionUDT e) {
			throw new LinkException(e);
		} catch (LinkException e) {
			throw e;
		} finally {
			try {
				sock.close();
			} catch (ExceptionUDT e) {
			}
		}
		while (true) {
			try {
				tell_server(ID_p, true);
				break;
			} catch (ExceptionUDT e) {
				// throw new NodeException("Failed to inform the server of the
				// result.");
			}
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
		String str = null;
		Map<String, String> pac = null;
		SocketUDT server = null;
		try {
			server = new SocketUDT(TypeUDT.STREAM);
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
			server.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																		// packet
																		// LinkC
		} catch (ExceptionUDT e) {
			throw e;
		} finally {
			try {
				server.close();
			} catch (ExceptionUDT e) {
			}
		}
		return;
	}

}
