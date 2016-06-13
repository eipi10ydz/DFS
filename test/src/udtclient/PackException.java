package udtclient;

/**
 * @author 李亞希
 *
 */
public class PackException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8289398842164621023L;

	/**
	 * 
	 */
	public PackException() {
	}

	/**
	 * @param gripe
	 */
	public PackException(String gripe) {
		super(gripe);
	}
}
