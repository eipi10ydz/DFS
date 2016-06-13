/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package udtclient;

import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author 杨德中
 */
public class Link implements Runnable
{
    Node node;
    LinkMaintainer lm;
    
    public Link(Node node, LinkMaintainer lm)
    {
        this.node = node;
        this.lm = lm;
    }
    
    @Override
    public void run() 
    {
        while (true) {            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Logger.getLogger(Link.class.getName()).log(Level.SEVERE, null, ex);
            }
            while (!node.messages_from_server.get("Link").isEmpty()) {
                                Map<String, String> pac = node.messages_from_server.get("Link").poll();
				if (pac.get("type").equals("LinkE") && pac.get("type_d").equals("03")) {
					try {
						if (establish_link_s(pac.get("ID"), pac.get("IP"), Integer.parseInt(pac.get("Port")))) {
							lm.link_timers.remove(pac.get("ID"));
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
			}
    
        }
    }
        
    
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
                        System.out.println(str);
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
                                System.out.println(IP_p + ":" + Port_p);
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
    
}
