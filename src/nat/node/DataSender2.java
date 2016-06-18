package data_transferor;

import java.nio.charset.Charset;
import java.util.List;
import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author lyx
 *
 */
public class DataSender2 {

	/**
	 * @param node
	 * @param ID_p
	 * @param packets
	 * @throws NodeException
	 * @throws LinkException
	 * @throws ExceptionUDT
	 */
	static public void Sender(Node node, String ID_p, List<String> packets)
			throws NodeException, LinkException, ExceptionUDT {
		SocketUDT sock = null;
		try {
			sock = node.link_establisher.establish_link_m(ID_p); // Establish a
																	// link with
																	// peer
		} catch (NodeException | LinkException e) {
			Logger.getLogger(DataSender2.class.getName()).log(Level.SEVERE, null, e);
			throw e;
		}
		for (String str : packets) {
			try {
				sock.send(str.getBytes(Charset.forName("ISO-8859-1")));
			} catch (ExceptionUDT e) {
				Logger.getLogger(DataSender2.class.getName()).log(Level.SEVERE, null, e);
				throw e;
			}
			try {
				Thread.sleep(700);
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
			}
		}
		try {
			InfoSender.Info_send(node, node.ID, ID_p, sock);// Update router
															// info
		} catch (ExceptionUDT e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}