package eu.scape_project.roda.core.plan;

import pt.gov.dgarq.roda.core.common.NoSuchReportException;

/**
 * Thrown to indicate that some specified {@link Plan} doesn't exist.
 * 
 * @author Rui Castro
 */
public class NoSuchPlanException extends PlanException {
	private static final long serialVersionUID = 365805872111925463L;

	/**
	 * Constructs a new {@link NoSuchReportException}.
	 */
	public NoSuchPlanException() {
	}

	/**
	 * Constructs a new {@link NoSuchPlanException} with the given message.
	 * 
	 * @param message
	 */
	public NoSuchPlanException(String message) {
		super(message);
	}

	/**
	 * Constructs a new {@link NoSuchPlanException} with the given cause
	 * Exception.
	 * 
	 * @param cause
	 */
	public NoSuchPlanException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a new {@link NoSuchPlanException} with the given message and
	 * cause Exception.
	 * 
	 * @param message
	 * @param cause
	 */
	public NoSuchPlanException(String message, Throwable cause) {
		super(message, cause);
	}

}
