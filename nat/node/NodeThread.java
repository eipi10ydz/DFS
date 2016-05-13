import java.util.Map;

/**
 * 
 */

/**
 * @author wzy
 *
 */
class NodeThread implements Runnable {

	private Node node;

	/**
	 * @param node
	 */
	public NodeThread(Node node) {
		this.node = node;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while (true) {
			while (!node.messages_from_server.get("Node").isEmpty()) {
				Map<String, String> link_establish = node.messages_from_server.get("Node").poll();
				if (link_establish.get("type").equals("NodeI")) {
					if (link_establish.get("type_d").equals("02")) {
						node_inserted(link_establish.get("ID"));
					} else {
						// TODO Something wrong
					}
				} else if (link_establish.get("type").equals("NodeD")) {
					node_deleted(link_establish.get("ID"));
				} else {
					// TODO Something wrong
				}
			}
		}
	}

	private void node_inserted(String nodeID) {
		node.nodeIDs.add(nodeID);
		node.node_inserted_lm.add(nodeID);
		node.node_inserted_rout.add(nodeID);
		return;
	}

	private void node_deleted(String nodeID) {
		node.nodeIDs.remove(nodeID);
		node.node_deleted_lm.add(nodeID);
		node.node_deleted_rout.add(nodeID);
		return;
	}
}
