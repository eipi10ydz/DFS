/***************************************************************************
 *                                                                         *
 *                               Request.java                              *
 *                            -------------------                          *
 *   date                 : 02.09.2004, 13:48                              *
 *   copyright            : (C) 2004-2008 Distributed and                  *
 *                              Mobile Systems Group                       *
 *                              Lehrstuhl fuer Praktische Informatik       *
 *                              Universitaet Bamberg                       *
 *                              http://www.uni-bamberg.de/pi/              *
 *   email                : sven.kaffille@uni-bamberg.de                   *
 *                          karsten.loesing@uni-bamberg.de                 *
 *                                                                         *
 *                                                                         *
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   A copy of the license can be found in the license.txt file supplied   *
 *   with this software or at: http://www.gnu.org/copyleft/gpl.html        *
 *                                                                         *
 ***************************************************************************/

package nodetest;

import java.io.Serializable;

/**
 * <p>
 * This class represents a request for the invocation of a method on
 * a {@link Chord node}. <code>Request</code>s are sent by a
 * {@link SocketProxy} to the {@link SocketEndpoint} of the node the
 * {@link SocketProxy} represents.
 * </p>
 * <p>
 * Results of a method invocation are sent back to the {@link SocketProxy}
 * by {@link SocketEndpoint} with help of a {@link Response} message.
 * with help of
 * </p>
 *
 * @author sven
 * @version 1.0.5
 */
public final class Request implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -1295124240351172262L;

	/**
	 * Constant holding the value that indicates that the {@link Request} that
	 * caused this response has been executed successfully.
	 */
	public static final int REQUEST_SUCCESSFUL = 1;

	/**
	 * Constant holding the value that indicates that the {@link Request} that
	 * caused this response failed.
	 */
	public static final int REQUEST_FAILED = 0;

	/**
	 * The type of this request, request or response.
	 */
	private int type;

	/**
	 * The method to invoke. Must be one of the constants defined in
	 * {@link MethodConstants} .
	 *
	 */
	private int methodIdentifier = -1;

	/**
	 * Status of the request {@link #REQUEST_FAILED} or {@link #REQUEST_SUCCESSFUL}.
	 */
	private int status = REQUEST_SUCCESSFUL;

	/**
	 * The parameters for the request. Must match the parameters for the method identified by   {@link #type}   in types and order.
	 */
	private Serializable[] parameters = null;

	/**
	 * Identifier used to identify this request. This identifier must be the value of the   {@link Response#getInReplyTo()}   field of a   {@link Response}   send for this request.
	 */
	private String replyTo;
	
	private String shard_hash;
	
	private byte[] shard;

	/**
	 * Creates a new instance of Request
	 *
	 * @param type1
	 *            The type of this request. One of the method identifiers from
	 *            {@link MethodConstants}.
	 * @param replyTo1
	 *            Identifier used to identify this request. This identifier must
	 *            be the value of the {@link Response#getInReplyTo()} field of a
	 *            {@link Response} send for this request.
	 */
	public Request(int type1, int method, String replyTo1) {
		this.type = type1;
		this.methodIdentifier = method;
		this.replyTo = replyTo1;
	}

	/**
	 * Get the type of this request.
	 *
	 * @return The type of this request, request or response.
	 */
	public int getRequestType() {
		return this.type;
	}

	/**
	 * Get the type of this request.
	 *
	 * @return The type of this request. One of the method identifiers from
	 *         {@link MethodConstants}.
	 */
	public int getRequestMethod() {
		return this.methodIdentifier;
	}

	/**
	 * Set the parameters for this request.
	 *
	 * @param parameters1
	 *            The parameters for the request. Must match the parameters for
	 *            the method identified by {@link #type} in types and order.
	 */
	public void setParameters(Serializable[] parameters1) {
		this.parameters = parameters1;
	}
	
	public void setShardhash(String hash){
		this.shard_hash = hash;
	}
	
	public void setShard(byte[] shard){
		this.shard = shard;
	}

	/**
	 * Get the parameters that shall be passed to the method that is requested
	 * by this.
	 *
	 * @return The parameters for the request. Must match the parameters for the
	 *         method identified by {@link #type} in types and order.
	 */
	public Serializable[] getParameters() {
		return this.parameters;
	}

	/**
	 * Get the value of the identifier for this request.
	 *
	 * @return Identifier used to identify this request. This identifier must be
	 *         the value of the {@link Response#getInReplyTo()} field of a
	 *         {@link Response} send for this request.
	 */
	public String getReplyTo() {
		return this.replyTo;
	}
	
	public String getShardhash(){
		return this.shard_hash;
	}
	
	public byte[] getShard(){
		return this.shard;
	}

	@Override
	public String toString() {
		return super.toString();
	}

}