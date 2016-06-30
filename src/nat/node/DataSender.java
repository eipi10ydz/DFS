package nodetest;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;

/**
 * @author lyx, wzy
 *
 */
class DataSender implements Callable<Boolean> {

	private Node node;
	private String dest;
	private String data;
        private List<String> data_to_send;
        private int cnt;
	private int No1;
	private static AtomicInteger No = new AtomicInteger(0);
	protected static Set<Integer> finished_list = ConcurrentHashMap.<Integer> newKeySet();
	private static final int block = 64 * 1024;

	/**
	 * @param node the node itself
	 * @param destination the ID of destination node
	 * @param data the contents to be sent
	 */
	public DataSender(Node node, String destination, String data) {
		this.node = node;
		this.dest = destination;
		this.data = data;
                this.data_to_send = new ArrayList<>();
                this.cnt = 0;
		No1 = DataSender.No.getAndAdd(1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public Boolean call() {
		Map<String, String> pac = null;
		int i;
		for (i = 0; i * block <= data.length(); i++) {
			data_to_send.add(i,
					data.substring(i * block, ((i + 1) * block > data.length()) ? data.length() : (i + 1) * block));
		}
		cnt = i;
		// Ask for router information
		try {
			while(true)
                        {
                            try 
                            {
                                pac = ask_router_info();
                            } 
                            catch (ExceptionUDT e) 
                            {
                                continue;
                            }
                            if(pac != null)
                                break;
                        }
		} catch (NodeException e) {
			e.printStackTrace();
			return false;// TODO retry?
		} 
//                catch (ExceptionUDT e) {
//			e.printStackTrace();
//			return false;
//		}
		pack_send(pac);
		while (true) {
                    	if (finished_list.contains(No1)) {
                            node.data_resend.remove(No1);
                            return true;
			}
			pac = node.data_resend.remove(No1);
			if (pac != null)
                        {
                            pack_send(pac);
                        }
		}
		// return false;
	}
        /**
         * ask server for route info in order to send packages
         * @return route info from server
         * @throws ExceptionUDT
         * @throws NodeException 
         */
	private Map<String, String> ask_router_info() throws ExceptionUDT, NodeException {
		byte arr[] = new byte[4096];
		String str = null;
		Map<String, String> pac = null;
                SocketUDT server = null;
                try
                {
                    server = new SocketUDT(TypeUDT.STREAM);
                    server.setBlocking(true);
                    server.connect(new InetSocketAddress(node.server_host, node.server_port));
                }
                catch(ExceptionUDT e)
                {
                    System.out.println("cannot connect to server...cannot get route info...");
                    return null;
                }
                pac = new ConcurrentHashMap<>();
		pac.put("ID", node.ID);
		pac.put("ID_target", this.dest);
		pac.put("No", Integer.toString(this.No1));
		pac.put("Cnt", Integer.toString(this.cnt));
		try {
			str = Packer.pack("RoutQ", pac);// send packet RoutQ
		} catch (PackException e) {// just used for debug
			e.printStackTrace();
		}
		server.send(str.getBytes(Charset.forName("ISO-8859-1")));
		str = new String();
		while (true) {
			server.receive(arr);// receive packet RoutD
			str += new String(arr, Charset.forName("ISO-8859-1")).trim();
			Node.empty_arr(4096, arr);
			try {
				pac = Packer.unpack(str);
                                System.out.println(str);
			//	if (!(pac.containsKey("type") && pac.containsKey("type_d") && pac.get("type").equals("RoutD")
			//			&& pac.get("type_d").equals("01"))) {
			//		throw new NodeException("Unexpected packet from the server.");
			//	}
				break;
			} catch (PackException e) {
			}
		}
		return pac;
	}
        /**
         * 
         * @param pac_routd01 route info from server
         * @return 
         */
	private Boolean pack_send(Map<String, String> pac_routd01) {
		String str = null;
		Map<String, String> pac = null;
		List<String> packets = null;
		int packet_cnt = 0;
		String ID_p;
		int rout_cnt = Integer.parseInt(pac_routd01.get("RoutCnt")); // 路径数
		String srout = "Rout";
		String scnt = "Cnt";
		String shop = "Hop_";
                int tempPac = 0;
		for (int i = 1; i <= rout_cnt; i++) {
                        packets = new ArrayList<>();
			int routi = Integer.parseInt(pac_routd01.get(srout + i));// 包数
			int routi_cnt = Integer.parseInt(pac_routd01.get(srout + i + scnt));
			pac = new ConcurrentHashMap<>();
			ID_p = pac_routd01.get(srout + i + shop + 1);
                        if(rout_cnt == 1)
                            pac.put("Len", this.data.length() + "");
//                        if(this.data_to_send.size() <= 10)
//                            pac.put("Len", (this.data.length() + this.data_to_send.size() * 37) + "");
//                        else
//                            pac.put("Len", (this.data.length() + 370 + (this.data_to_send.size() - 10) * 38) + "");
                        else if(i < rout_cnt)
                            pac.put("Len", routi * 64 * 1024 + "");
                        else
                            pac.put("Len", (this.data.length() - (this.data_to_send.size() - 1) * 64 * 1024 + (routi - 1) * 64 * 1024) + "");
/*                        else if(i < rout_cnt)
                        {
                            if(tempPac <= 10)
                            {
                                if((tempPac + routi) <= 10)
                                    pac.put("Len", 65573 * routi + "");
                                else if((tempPac + routi) > 10)
                                    pac.put("Len", (65574 * (tempPac + routi - 10) + 65573 * (10 - tempPac)) + "");
                            }
                            else
                            {
                                pac.put("Len", 65574 * routi + "");
                            }
                            tempPac += routi;
                        }
                        else
                        {
                            if(tempPac <= 10)
                            {
                                pac.put("Len", str)
                            }
                            else
                            {
                                pac.put("Len", 65574 * routi + "");
                            }
                        }*/
                        pac.put("From", node.ID);
			pac.put("To", this.dest);
			pac.put("No", Integer.toString(this.No1));
			pac.put("Cnt", Integer.toString(this.cnt));
			pac.put("NoBeg", Integer.toString(packet_cnt)); // 包的起始编号
			pac.put("HopCnt", pac_routd01.get(srout + i + scnt));// 每个路径的节点数
			pac.put("PackCnt", pac_routd01.get(srout + i));
			for (int j = 1; j <= routi_cnt; j++) {
				pac.put(shop + j, pac_routd01.get(srout + i + shop + j));
			}
			try {
				str = Packer.pack("RoutD", "02", pac);
			} catch (PackException e1) {// just used for debug
				e1.printStackTrace();
			}
			packets.add(str);
			for (int j = 0; j != routi; j++) {
				pac = new ConcurrentHashMap<>();
				pac.put("No", Integer.toString(packet_cnt));
				pac.put("Content", data_to_send.get(packet_cnt));
				packet_cnt++;
				try {
					str = Packer.pack("Data", pac);
				} catch (PackException e1) {// just used for debug
					e1.printStackTrace();
				}
				packets.add(str);
			}
			while (true) {
				try {
                                    DataSender2.Sender(node, ID_p, packets);
                                    System.out.println("send route " + i + " packages...");
                                    break;
				} catch (ExceptionUDT e) {
					// 重发...
				}
			}
		}
		return true;
	}
}