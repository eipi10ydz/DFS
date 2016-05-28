package data_transferor;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Map;

import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;

/**
 * 
 */

/**
 * @author wzy
 *
 */
class ServerLink implements Runnable {

	private Node node;

	/**
	 * @param node
	 */
	public ServerLink(Node node) {
		this.node = node;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		byte[] arr = new byte[1024];
		String str = new String();
		String tmp;
		Map<String, String> pac;
		SocketUDT server;
		try {
			server = new SocketUDT(TypeUDT.STREAM);
			server.setBlocking(true);
			server.bind(new InetSocketAddress(node.IP_local_server, node.Port_local_server));
			server.listen(10);
			SocketUDT server_cnnt = null;
			while (true) {
				try {
					arr = new byte[1024];
					server_cnnt = server.accept();
					server_cnnt.setSoTimeout(100);
					server_cnnt.receive(arr);
					str = new String(arr, Charset.forName("ISO-8859-1")).trim();
					pac = Packer.unpack(str);
					tmp = null;
					switch (pac.get("type")) {
					case ("NodeI"):
					case ("NodeD"):
					case ("NodeT"):
						tmp = "Node";
						break;
					case ("LinkE"):
					case ("LinkC"):
						tmp = "Link";
						break;
					case ("ERR"):
						tmp = "ERR";
						break;
					default:
						throw new NodeException("Unknown type" + pac.toString());
					}
					node.messages_from_server.get(tmp).add(pac);
					server_cnnt.close();
				} catch (ExceptionUDT e) {
					switch (e.getError().getCode()) {
					case (2001):
						break;
					case (6002):
						break;
					case (6003):
						break;
					default:
						e.printStackTrace();
					}
				} catch (NodeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					try {
						server_cnnt.close();
					} catch (ExceptionUDT e) {
					}
				}
			}
		} catch (ExceptionUDT e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
