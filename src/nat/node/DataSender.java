package data_transferor;

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
	 * @param node
	 * @param destination
	 * @param data
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
		while (true) {
			try {
				ask_router_info();
				break;
			} catch (NodeException e) {
				e.printStackTrace();
				return false;// TODO retry?
			} catch (ExceptionUDT e) {
				e.printStackTrace();
				return false;
			} catch (LinkException e) {
				// retry
			}
		}
		pack_send(pac);
		while (true) {
			if (finished_list.contains(No1)) {
				node.data_resend.remove(No1);
				return true;
			}
			pac = node.data_resend.remove(No1);
			if (pac != null)
				pack_send(pac);
		}
		// return false;
	}

	private Map<String, String> ask_router_info() throws ExceptionUDT, NodeException, LinkException {
		byte arr[] = new byte[4096];
		String str = null;
		Map<String, String> pac = null;
		SocketUDT server = new SocketUDT(TypeUDT.STREAM);
		server.setBlocking(true);
		server.connect(new InetSocketAddress(node.server_host, node.server_port));
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
			} catch (PackException e) {
				throw new LinkException("Unexpected packet from the server." + pac.toString());
			}
			if (!(pac.containsKey("type") && pac.containsKey("type_d"))) {
				throw new LinkException("Unexpected packet from the server." + pac.toString());
			}
			if ((pac.get("type").equals("ERR") && pac.get("type_d").equals("02"))) {
				throw new NodeException("Routing failed.");
			}
			if (!(pac.get("type").equals("RoutD") && pac.get("type_d").equals("01"))) {
				throw new LinkException("Unexpected packet from the server." + pac.toString());
			}
			break;
		}
		return pac;
	}

	private Boolean pack_send(Map<String, String> pac_routd01) {
		String str = null;
		Map<String, String> pac = null;
		List<String> packets = new ArrayList<>();
		int packet_cnt = 0;
		String ID_p;
		int rout_cnt = Integer.parseInt(pac_routd01.get("RoutCnt")); // 路径数
		String srout = new String("Rout");
		String scnt = new String("Cnt");
		String shop = new String("Hop_");
		for (int i = 1; i <= rout_cnt; i++) {
			int routi = Integer.parseInt(pac_routd01.get(srout + i));// 包数
			int routi_cnt = Integer.parseInt(pac_routd01.get(srout + i + scnt));
			pac = new ConcurrentHashMap<>();
			ID_p = pac_routd01.get(srout + i + shop + 1);
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
				pac.put("No", Integer.toString(No1));
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
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					Thread.currentThread().interrupt();
				}
				try {
					DataSender2.Sender(node, ID_p, packets);
					break;
				} catch (LinkException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NodeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExceptionUDT e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return true;
	}
}
