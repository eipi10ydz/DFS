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
		try {
			SocketUDT sock = null;
			SocketUDT server = new SocketUDT(TypeUDT.STREAM);
			server.setBlocking(true);
			server.bind(new InetSocketAddress(node.IP_local_server, node.Port_local_server));
			server.listen(10);
			while (true) {
				try {
					arr = new byte[1024];
					sock = server.accept();
					sock.receive(arr);
					str = new String(arr, Charset.forName("ISO-8859-1")).trim();
					try {
						pac = Packer.unpack(str);
					} catch (PackException e) {
						throw new LinkException("Unexpected packet from the server.", e);
					}
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
					case ("DataF"):
						tmp = "Data";
						break;
					case ("RoutD"):
						tmp = "Data";
						break;
					case ("ERR"):
						tmp = "ERR";
						break;
					default:
						throw new LinkException("Unexpected packet from the server." + pac.toString());
					}
					node.messages_from_server.get(tmp).add(pac);
				} catch (ExceptionUDT e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (LinkException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					try {
						sock.close();
					} catch (ExceptionUDT e) {
					}
				}
			}
		} catch (ExceptionUDT e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
