package data_transferor;

import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

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
		while (true) {
			while (!node.messages_from_server.get("Node").isEmpty()) {
				Map<String, String> pac = node.messages_from_server.get("Node").poll();
				if (pac.get("type").equals("NodeI")) {
					if (pac.get("type_d").equals("02")) {
						node_inserted(pac.get("ID"), pac.get("UName"), pac.get("LIP"));
					} else {
						// TODO Something wrong
					}
				} else if (pac.get("type").equals("NodeD")) {
					node_deleted(pac.get("ID"));
				} else {
					// TODO Something wrong
				}
			}
			while (!node.messages_from_server.get("Data").isEmpty()) {
				Map<String, String> pac = node.messages_from_server.get("Data").poll();
				if (pac.get("type").equals("DataF")) {
					DataSender.finished_list.add(Integer.parseInt(pac.get("No")));
				} else {
					// TODO Something wrong
				}
			}
		}
	}

	private void node_inserted(String nodeID, String UName, String LIP) {
		node.nodeIDs.add(nodeID);
		node.UName_ID.put(UName, nodeID);
		node.node_IPs.put(nodeID, LIP);
		node.link_establish_locks.put(nodeID, new ReentrantLock());
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
