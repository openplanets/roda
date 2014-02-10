package eu.scape_project.roda.core.plan;

/**
 * Thrown to indicate that some error occured in the Plan.
 * 
 * @author Rui Castro
 */
public class PlanAlreadyExistsException extends Exception {
	private static final long serialVersionUID = -6011197545774739453L;

	/**
	 * Constructs a new {@link LoggerException}.
	 */
	public PlanAlreadyExistsException() {
	}

	/**
	 * Constructs a new {@link LoggerException} with the given error message.
	 * 
	 * @param message
	 *            the error message.
	 */
	public PlanAlreadyExistsException(String message) {
		super(message);
	}

	/**
	 * Constructs a new {@link LoggerException} with the given cause exception.
	 * 
	 * @param cause
	 *            the cause exception.
	 */
	public PlanAlreadyExistsException(Throwable cause) {
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
	public PlanAlreadyExistsException(String message, Throwable cause) {
		super(message, cause);
	}

}
