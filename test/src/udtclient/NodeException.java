package udtclient;

/**
 * @author wzy
 *
 */
public class NodeException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3283896622988011291L;

	/**
	 * 
	 */
	public NodeException() {
		super();
	}

	/**
	 * @param gripe
	 */
	public NodeException(String gripe) {
		super(gripe);
	}
}
