package data_transferor;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.Future;

import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;

/**
 * 
 */

/**
 * @author wzy
 *
 */
class NodeLink implements Runnable {

	private Node node;
	private String nodeID;
	private SocketUDT socket;

	/**
	 * @param nodeID_peer
	 * @param socket
	 * @param node
	 */
	public NodeLink(String nodeID_peer, SocketUDT socket, Node node) {
		this.nodeID = nodeID_peer;
		this.socket = socket;
		this.node = node;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		String str = new String();
		byte[] arr = new byte[1024];
		Map<String, String> pac;
		try {
			while (!Thread.currentThread().isInterrupted()) {
				Thread.sleep(1);
				try {
					socket.receive(arr);
					str += new String(arr, Charset.forName("ISO-8859-1")).trim();
					node.empty_arr(str.length(), arr);
					pac = Packer.unpack(str);
					if (pac.get("To").equals(node.ID)) {
						final String content = pac.get("Content");
						node.data_arrived.get(pac.get("From")).data.getAndUpdate((String data) -> data + content);
					} else {// Launch a new task and hold its result in
							// node.send_results
						Future<Boolean> result;
						result = node.data_to_send.submit(new DataSender(node, pac.get("To"), pac.get("Content")));
					}
					str = new String();
				} catch (ExceptionUDT e) {
					switch (e.getError().getCode()) {
					case (6002):
					case (6003):
						break;
					default:
						e.printStackTrace();
						throw e;
					}
				} catch (NodeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (InterruptedException e) {
		} catch (ExceptionUDT e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				socket.close();
			} catch (ExceptionUDT e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
