package data_transferor;
import java.nio.charset.Charset;
import java.util.Map;

import com.barchart.udt.ExceptionUDT;

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
		Map<String, String> pac;
		while (true) {
			if (node.server_link_count.get() == 0) {// inaccurate: When no
													// thread wants to use
													// node.server
				node.server_link_lock.lock();
				try {
					node.server.setSoTimeout(1);
					node.server.receive(arr);
					str = new String(arr, Charset.forName("ISO-8859-1")).trim();
					pac = Packer.unpack(str);
					String tmp = null;
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
				} catch (ExceptionUDT e) {
					switch (e.getError().getCode()) {
					case (2001):
						// TODO try to reestablish
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
						node.server.setSoTimeout(10000);
					} catch (ExceptionUDT e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					node.server_link_lock.unlock();
				}
			}

			// TODO
		}
	}

}
