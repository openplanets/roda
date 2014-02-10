package eu.scape_project.roda.core.connector.utils;

import pt.gov.dgarq.roda.core.common.LoggerException;

/**
 * Thrown to indicate that some error occured in the Plan.
 * 
 * @author Rui Castro
 */
public class DataConnectorException extends Exception {
	private static final long serialVersionUID = -6011197545774739453L;

	/**
	 * Constructs a new {@link LoggerException}.
	 */
	public DataConnectorException() {
	}

	/**
	 * Constructs a new {@link LoggerException} with the given error message.
	 * 
	 * @param message
	 *            the error message.
	 */
	public DataConnectorException(String message) {
		super(message);
	}

	/**
	 * Constructs a new {@link LoggerException} with the given cause exception.
	 * 
	 * @param cause
	 *            the cause exception.
	 */
	public DataConnectorException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new {@link LoggerException} with the given error message and
	 * cause exception.
	 * 
	 * @param message
	 *            the error message.
	 * @param cause
	 *            the cause exception.
	 */
	public DataConnectorException(String message, Throwable cause) {
		super(message, cause);
	}

}
