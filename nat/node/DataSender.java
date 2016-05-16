import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import com.barchart.udt.ExceptionUDT;

/**
 * 
 */

/**
 * @author wzy
 *
 */
class DataSender implements Callable<Boolean> {

	private Node node;
	private String dest;
	private String data;

	/**
	 * @param node
	 * @param destination
	 * @param data
	 */
	public DataSender(Node node, String destination, String data) {
		this.node = node;
		this.dest = destination;
		this.data = data;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public Boolean call() {
		String next_hop;
		String str = null;
		Map<String, String> pac = new ConcurrentHashMap<>();
		pac.put("From", node.ID);
		pac.put("To", dest);
		pac.put("Content", data);
		try {
			str = Packer.pack("Data", pac);
		} catch (NodeException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		while (true) {
			node.router.lock.lock();
			next_hop = node.router.find_next_hop(dest);
			node.router.lock.unlock();
			node.links_p_l.get(next_hop);// TODO
			try {
				node.links_p.get(next_hop).send(str.getBytes(Charset.forName("ISO-8859-1")));
			} catch (ExceptionUDT e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
			return true;
		}
		return false;
	}

}
