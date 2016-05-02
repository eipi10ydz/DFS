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
			try {
				node.server.receive(arr);
			} catch (ExceptionUDT e) {
				// TODO try to reestablish
				e.printStackTrace();
			}
			// TODO
		}
	}

}
