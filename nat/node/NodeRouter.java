package data_transferor;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 
 */

/**
 * @author wzy
 *
 */
class NodeRouter {

	private Node node;
	private Map<String, ArrayList<String>> routing_table;
	protected ReentrantLock lock;

	/**
	 * @param node
	 */
	public NodeRouter(Node node) {
		this.node = node;
		this.routing_table = new ConcurrentHashMap<>();
		this.lock = new ReentrantLock();
	}

	/**
	 * @param dest
	 * @return next_hop
	 */
	protected String find_next_hop(String dest) {
		ArrayList<String> rout = routing_table.get(dest);
		for (int i = 0; i < rout.size(); i++) {
			if (!node.links_p_l.get(rout.get(i)).isLocked()) {
				return rout.get(i);
			}
		}
		return null;
	}

}
