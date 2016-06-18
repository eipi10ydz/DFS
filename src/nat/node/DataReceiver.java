package data_transferor;

import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author 杨德中
 */
public class DataReceiver implements Runnable {
	protected String from;
	protected int cnt;
	protected int No;
	protected String res;
	protected Map<Integer, String> pack;
	protected Timer timer;
	protected Node node;

	/**
	 * @param from
	 * @param cnt
	 * @param No
	 * @param node
	 */
	public DataReceiver(String from, int cnt, int No, Node node) {
		this.from = from;
		this.cnt = cnt;
		this.No = No;
		this.res = "";
		this.pack = new ConcurrentHashMap<>();
		this.timer = new Timer(300000); // 5分钟?
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while (this.pack.size() < this.cnt && (!this.timer.isExpired()))
			;
		if (this.pack.size() < this.cnt) {
			Map<String, String> info = new HashMap<>();
			info.put("ID", from);
			info.put("ID_target", node.ID);
			info.put("No", No + "");
			SocketUDT sock;
			try {
				sock = new SocketUDT(TypeUDT.STREAM);
				sock.connect(new InetSocketAddress(node.server_host, node.server_port));
				sock.send(Packer.pack("DataR", info).getBytes(Charset.forName("ISO-8859-1")));
			} catch (ExceptionUDT | PackException ex) {
				Logger.getLogger(DataReceiver.class.getName()).log(Level.SEVERE, null, ex);
			}
			return;
		}
		for (int i = 0; i < this.cnt; ++i) {
			res = res + this.pack.get(i);
		}
		ObjectInputStream oin = null;
		Object obj = null;
		try {
			// 得到完整内容
			oin = new ObjectInputStream(new ByteArrayInputStream(res.getBytes(Charset.forName("ISO-8859-1"))));
			obj = oin.readObject();
		} catch (IOException ex) {
			// Logger.getLogger(DataReceiver.class.getName()).log(Level.SEVERE,
			// null, ex);
			System.out.println("get object failed...");
		} catch (ClassNotFoundException ex) {
			Logger.getLogger(DataReceiver.class.getName()).log(Level.SEVERE, null, ex);
		}
		System.out.println(obj.toString());
	}

}
