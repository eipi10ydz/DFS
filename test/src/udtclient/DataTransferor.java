package udtclient;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */

/**
 * @author wzy
 *
 */
public class DataTransferor implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3263064232605461413L;
	private Node node;
	private String peer;
	protected AtomicReference<String> data;

	/**
	 * @param node
	 * @param peer
	 */
	public DataTransferor(Node node, String peer) {
		if (node == null || peer == null)
			throw new IllegalArgumentException("Illegal argument.");
		this.node = node;
		this.peer = node.UName_ID.get(peer);
		this.data.set(new String());
//		node.data_arrived.put(this.peer, this);
	}

	/**
	 * @param in
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void readObject(ObjectInputStream in) throws IllegalArgumentException, IOException, ClassNotFoundException {
		node.transfer_message((String) in.readObject(), peer);
	}

	/**
	 * @param out
	 * @throws IOException
	 */
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeBytes(data.getAndSet(new String()));
	}

	/**
	 * @throws ObjectStreamException
	 */
	private void readObjectNoData() throws ObjectStreamException {
	}
}
