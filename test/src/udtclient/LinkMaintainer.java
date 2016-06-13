package udtclient;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 */

/**
 * @author wzy
 *
 */
class LinkMaintainer implements Runnable {

	private Node node;

	protected Map<String, Timer> link_timers;

	/**
	 * @param node
	 */
	public LinkMaintainer(Node node) {
		this.node = node;
		link_timers = new ConcurrentHashMap<>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		establish_links();
		while (true) {
                    try {
                        // check link timers
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(LinkMaintainer.class.getName()).log(Level.SEVERE, null, ex);
                    }
			try {
				for (Map.Entry<String, Timer> entry : link_timers.entrySet()) {
					String nodeID = entry.getKey();
					Timer timer = entry.getValue();
					if (!node.nodeIDs.contains(nodeID)) {// the peer node has
															// dropped
						link_timers.remove(nodeID);
					} else if (timer.isExpired()) {
						try {
							if (establish_link_m(nodeID)) {
								link_timers.remove(nodeID);
							} else {
								timer.postpone(20000 * timer.getCnt());
								timer.start();
							}
						} catch (PackException e) {
							timer.postpone(20000 * timer.getCnt());
							timer.start();
							e.printStackTrace();
						} catch (ExceptionUDT e) {
							timer.postpone(20000 * timer.getCnt());
							timer.start();
							e.printStackTrace();
						} catch (NodeException e) {
							timer.postpone(20000 * timer.getCnt());
							timer.start();
							throw e;
						}
					}
				}
			} catch (NodeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// check if new node inserted
			while (!node.node_inserted_lm.isEmpty())
				new_link_timer(node.node_inserted_lm.poll());
			// check if node deleted
			while (!node.node_deleted_lm.isEmpty()) {
				String nodeID = node.node_deleted_lm.poll();
				if (node.links_p.containsKey(nodeID)) {
					node.links_p_t.get(nodeID).interrupt();
					node.links_p_t.remove(nodeID);
					node.links_p_l.remove(nodeID);
					node.links_p.remove(nodeID);
				} else
					link_timers.remove(nodeID);
			}
			// find out link broken
			node.links_p.forEach((nodeID, socket) -> {
				if (socket.isClosed()) {
					String str = new String();
					Map<String, String> pac;
					node.links_p.remove(nodeID);
					node.links_p_t.remove(nodeID);
					node.links_p_l.remove(nodeID);
					pac = new ConcurrentHashMap<String, String>();
					pac.put("ID", nodeID);
					pac.put("Connectivity", "false");
					try {
						str = Packer.pack("LinkC", pac);
					} catch (PackException e) {// just used for debug
						e.printStackTrace();
					}
					SocketUDT server = null;
					try {
						server = new SocketUDT(TypeUDT.STREAM);
						server.setBlocking(true);
						server.connect(new InetSocketAddress(node.server_host, node.server_port));
						try {
							server.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																						// packetLinkC:false
						} catch (ExceptionUDT e1) {
							e1.printStackTrace();
						} finally {
							try {
								server.close();
							} catch (ExceptionUDT e1) {
								e1.printStackTrace();
							}
						}
					} catch (ExceptionUDT e) {
						e.printStackTrace();
					}
					new_link_timer(nodeID);
				}
			});
			// check if other Nodes try to establish links with this
/*			while (!node.messages_from_server.get("Link").isEmpty()) {
				Map<String, String> pac = node.messages_from_server.get("Link").poll();
				if (pac.get("type").equals("LinkE") && pac.get("type_d").equals("03")) {
					try {
						if (establish_link_s(pac.get("ID"), pac.get("IP"), Integer.parseInt(pac.get("Port")))) {
							link_timers.remove(pac.get("ID"));
						}
					} catch (ExceptionUDT e) {
						e.printStackTrace();
					} catch (NodeException e) {
						e.printStackTrace();
					} catch (PackException e) {
						e.printStackTrace();
					}
				} else {
					// TODO Something wrong
				}
			}*/
		}

	}

	/**
	 * Try to establish links with every Node in the NodeID table.
	 */
	private void establish_links() {
		node.nodeIDs.forEach(nodeID -> {
			try {
				if (!establish_link_m(nodeID)) {
					new_link_timer(nodeID);
				}
			} catch (PackException e) {
				new_link_timer(nodeID);
				e.printStackTrace();
			} catch (ExceptionUDT e) {
				new_link_timer(nodeID);
				e.printStackTrace();
			} catch (NodeException e) {
				new_link_timer(nodeID);
				e.printStackTrace();
			}
		});
		return;
	}

	/**
	 * @param ID_p
	 * @return
	 * @throws NodeException
	 * @throws PackException
	 * @throws ExceptionUDT
	 */
	private boolean establish_link_m(String ID_p) throws NodeException, PackException, ExceptionUDT {
		if (node.ID.equals(ID_p) || !node.nodeIDs.contains(ID_p) || node.links_p.containsKey(ID_p))
			return false;// TODO Something wrong
		byte arr[] = new byte[1024];
		String str = new String();
		Map<String, String> pac;
		String IP_p = new String();
		int Port_p = 0;
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
                                System.out.println("connect to:" + IP_p + ":" + Port_p);
				System.out.println(sock.getLocalInetAddress().toString() + ":" + sock.getLocalInetPort());
                                sock.connect(new InetSocketAddress(IP_p, Port_p));
                                if(sock.isConnected())
                                {
                                    sock.send("".getBytes());
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
				// TODO
			} catch (ExceptionUDT e) {
				e.printStackTrace();
				server = new SocketUDT(TypeUDT.STREAM);
				server.setBlocking(true);
				server.connect(new InetSocketAddress(node.server_host, node.server_port));
				pac = new ConcurrentHashMap<String, String>();
				pac.put("ID", node.ID);
				pac.put("ID_target", ID_p);
				pac.put("Connectivity", "false");
				try {
					str = Packer.pack("LinkC", pac);
				} catch (PackException e1) {// just used for debug
					e1.printStackTrace();
				}
				server.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																			// packet
																			// LinkC:false
				try {
					server.close();
				} catch (ExceptionUDT e1) {
					e1.printStackTrace();
				}
				return false;
			}
			server = new SocketUDT(TypeUDT.STREAM);
			server.setBlocking(true);
			server.connect(new InetSocketAddress(node.server_host, node.server_port));
			pac = new ConcurrentHashMap<String, String>();
			pac.put("ID", node.ID);
			pac.put("ID_target", ID_p);
			pac.put("Connectivity", "true");
			try {
				str = Packer.pack("LinkC", pac);
			} catch (PackException e) {// just used for debug
				e.printStackTrace();
			}
			server.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																		// packet
																		// LinkC:true
			try {
				server.close();
			} catch (ExceptionUDT e1) {
				e1.printStackTrace();
			}
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
		node.links_p.put(ID_p, sock);
		node.links_p_t.put(ID_p, new Thread(new NodeLink(ID_p, sock, node)));
		node.links_p_t.get(ID_p).start();
		node.links_p_l.put(ID_p, new ReentrantLock());
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
	private boolean establish_link_s(String ID_p, String IP_p, int Port_p)
			throws ExceptionUDT, NodeException, PackException {
		if (node.ID.equals(ID_p) || (IP_p == null) || (IP_p.length() == 0))
			return false;// TODO Something wrong
		if (!node.nodeIDs.contains(ID_p)) {
			node.nodeIDs.add(ID_p);
		}
		if (node.links_p.containsKey(ID_p)) {
			node.links_p_t.get(ID_p).interrupt();
			node.links_p_t.remove(ID_p);
			node.links_p_l.remove(ID_p);
			node.links_p.remove(ID_p);
		}
		byte arr[] = new byte[1024];
		String str = new String();
		Map<String, String> pac;
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
//			if (!pac.get("ID").equals(ID_p)) {// Error on the server side.
//				throw new NodeException("Error on the server side.");
//			}
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
				// TODO
			} catch (ExceptionUDT e) {
				e.printStackTrace();
				server = new SocketUDT(TypeUDT.STREAM);
				server.setBlocking(true);
				server.connect(new InetSocketAddress(node.server_host, node.server_port));
				pac = new ConcurrentHashMap<String, String>();
				pac.put("ID", node.ID);
				pac.put("ID_target", ID_p);
				pac.put("Connectivity", "false");
				try {
					str = Packer.pack("LinkC", pac);
				} catch (PackException e1) {// just used for debug
					e1.printStackTrace();
				}
				server.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																			// packet
																			// LinkC:false
				try {
					server.close();
				} catch (ExceptionUDT e1) {
					e1.printStackTrace();
				}
				return false;
			}
			server = new SocketUDT(TypeUDT.STREAM);
			server.setBlocking(true);
			server.connect(new InetSocketAddress(node.server_host, node.server_port));
			pac = new ConcurrentHashMap<String, String>();
			pac.put("ID", node.ID);
			pac.put("ID_target", ID_p);
			pac.put("Connectivity", "true");
			try {
				str = Packer.pack("LinkC", pac);
			} catch (PackException e) {// just used for debug
				e.printStackTrace();
			}
			server.send(str.getBytes(Charset.forName("ISO-8859-1")));// send
																		// packet
																		// LinkC:true
			try {
				server.close();
			} catch (ExceptionUDT e1) {
				e1.printStackTrace();
			}
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
		node.links_p.put(ID_p, sock);
		node.links_p_t.put(ID_p, new Thread(new NodeLink(ID_p, sock, node)));
		node.links_p_t.get(ID_p).start();
		node.links_p_l.put(ID_p, new ReentrantLock());
		return true;

	}

	/**
	 * @param nodeID
	 */
	private void new_link_timer(String nodeID) {
		if (nodeID == null)
			return;
		if (!node.nodeIDs.contains(nodeID))
			return;
		Timer timer = new Timer(10000, 3600000);
		timer.start();
		link_timers.put(nodeID, timer);
		return;
	}

}
