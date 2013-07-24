package eu.scape_project.repository.report;


/**
 * This exception is thrown to indicate that some error occurred inside the
 * EventCatalog.
 * 
 * @author Rui Castro
 */
public class EventCatalogException extends Exception {
	private static final long serialVersionUID = 1213620181254838354L;

	/**
	 * Constructs an empty {@link EventCatalogException}.
	 */
	public EventCatalogException() {
	}

	/**
	 * Constructs a {@link EventCatalogException} with the specified error
	 * message.
	 * 
	 * @param message
	 *            the error message.
	 */
	public EventCatalogException(String message) {
		super(message);
	}

	/**
	 * Constructs a {@link EventCatalogException} with the specified cause
	 * exception.
	 * 
	 * @param cause
	 *            the cause exception.
	 */
	public EventCatalogException(Throwable cause) {
		super(cause);
	}

	/**
	 * Constructs a {@link EventCatalogException} with the specified message and
	 * cause exception.
	 * 
	 * @param message
	 *            the error message.
	 * @param cause
	 *            the cause exception.
	 */
	public EventCatalogException(String message, Throwable cause) {
		super(message, cause);
	}

}
