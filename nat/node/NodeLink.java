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
		byte[] arr = new byte[1024];
		try {
			while (!Thread.currentThread().isInterrupted()) {
				Thread.sleep(1);
				socket.receive(arr);
				// TODO
			}
		} catch (InterruptedException e) {
		} catch (ExceptionUDT e) {
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
