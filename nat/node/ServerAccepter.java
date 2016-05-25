/**
 * 
 */
package data_transferor;

import java.net.InetSocketAddress;

import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;

/**
 * @author lenovo
 *
 */
class ServerAccepter implements Runnable {

	private Node node;
	private SocketUDT server1;

	/**
	 * @param node
	 * @throws IllegalArgumentException
	 * @throws ExceptionUDT
	 */
	public ServerAccepter(Node node) throws ExceptionUDT, IllegalArgumentException {
		this.node = node;
		server1 = new SocketUDT(TypeUDT.STREAM);
		server1.bind(new InetSocketAddress(this.node.IP_local_server, this.node.Port_local_server));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			try {
				server1.accept();
			} catch (ExceptionUDT e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
