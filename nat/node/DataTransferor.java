ÁºÌÎimport java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

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
	private static final long serialVersionUID = 4536285731163136597L;
	private Node node;
	private String peer;
	protected String data;

	/**
	 * @param node
	 * @param peer
	 */
	public DataRransferor(Node node, String peer) {
		if (node == null || peer == null)
			throw new IllegalArgumentException("Illegal argument.");
		this.node = node;
		this.peer = peer;// TODO
		node.data_arrived.put(peer, this);
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
		out.writeBytes(data);
	}

	/**
	 * @throws ObjectStreamException
	 */
	private void readObjectNoData() throws ObjectStreamException {
	}
}
