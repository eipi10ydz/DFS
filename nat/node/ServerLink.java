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
		while (true) {
			if (node.server_link_count.get() == 0) {// inaccurate: When no
													// thread wants to use
													// node.server_link
				node.server_link_lock.lock();
				try {
					node.server.setSoTimeout(1);
					node.server.receive(arr);
					// TODO
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
