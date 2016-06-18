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
	private int No1;
	private static AtomicInteger No = new AtomicInteger(0);
	protected static Set<Integer> finished_list = ConcurrentHashMap.<Integer> newKeySet();;
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
		No1 = DataSender.No.getAndAdd(1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public Boolean call() {// 每调一次一个线程
		byte arr[] = new byte[4096];
		String str = null;
		Map<String, String> pac, pac_routd01 = null;
		List<String> data_to_send = new ArrayList<>(); // 切块后的数据
		List<String> packets = new ArrayList<>();
		int cnt; // 包的总数目
		int packet_cnt = 0;
		String ID_p;
		int i;
		for (i = 0; i * block <= data.length(); i++) {
			data_to_send.add(i,
					data.substring(i * block, ((i + 1) * block > data.length()) ? data.length() : (i + 1) * block));
		}
		cnt = i;
		// Ask for route information
		try {
			SocketUDT server = new SocketUDT(TypeUDT.STREAM);
			server.setBlocking(true);
			server.connect(new InetSocketAddress(node.server_host, node.server_port));
			pac = new ConcurrentHashMap<>();
			pac.put("ID", node.ID);
			pac.put("ID_target", dest);
			pac.put("No", Integer.toString(No1));
			pac.put("Cnt", Integer.toString(cnt));
			try {
				str = Packer.pack("RoutQ", pac);
			} catch (PackException e) {// just used for debug
				e.printStackTrace();
			}
                        server.send(str.getBytes(Charset.forName("ISO-8859-1")));
			while (true) 
                        {
				server.receive(arr);
				str = new String(arr, Charset.forName("ISO-8859-1")).trim();
				node.empty_arr(str.length(), arr);
				pac_routd01 = Packer.unpack(str);
                                System.out.println(str);
				break;
			}
		} catch (PackException e) {
			e.printStackTrace();
			return false;
		} catch (ExceptionUDT e) {
			e.printStackTrace();
			return false;
		}
		int rout_cnt = Integer.parseInt(pac_routd01.get("RoutCnt")); // 路径数
		String srout = "Rout";
		String scnt = "Cnt";
		String shop = "Hop_";
		for (i = 1; i <= rout_cnt; i++) {
			int routi = Integer.parseInt(pac_routd01.get(srout+i));// 包数
			int routi_cnt = Integer.parseInt(pac_routd01.get(srout+i+scnt));
			pac = new ConcurrentHashMap<>();
			ID_p = pac_routd01.get(srout+i+shop+i);
			pac.put("From", node.ID);
			pac.put("To", dest);
			pac.put("No", Integer.toString(No1));
                        pac.put("Cnt", cnt + "");
			pac.put("NoBeg", Integer.toString(packet_cnt)); // 包的起始编号
			pac.put("HopCnt", pac_routd01.get(srout+i+scnt));// 每个路径的节点数
			pac.put("PackCnt", pac_routd01.get(srout+i));
			for (int j = 1; j <= routi_cnt; j++) {
				pac.put(shop+j, pac_routd01.get(srout+i+shop+j));
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
                        while (true) 
                        {
                            try 
                            {
                                DataSender2.Sender(node, ID_p, packets);
                                break;
                            } 
                            catch (ExceptionUDT e) 
                            {
                                //重发...
                            }
                        }
                }
		while (true) {
			if (finished_list.contains(No1))
				return true;
		}
		// return false;
	}
}
