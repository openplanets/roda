package eu.scape_project.roda.report;

import org.apache.log4j.Logger;

import pt.gov.dgarq.roda.core.BrowserHelper;
import pt.gov.dgarq.roda.core.data.LogEntry;

/**
 * @author Rui Castro
 */
public class DownloadRepresentationEvent extends ViewRepresentationEvent {

	/**
	 * The logger.
	 */
	private static Logger logger = Logger
			.getLogger(ViewRepresentationEvent.class);

	/**
	 * Constructs a new {@link DownloadRepresentationEvent} for the specified
	 * {@link LogEntry}.
	 * 
	 * @param logEntry
	 * @param browserHelper
	 */
	public DownloadRepresentationEvent(LogEntry logEntry,
			BrowserHelper browserHelper) {
		super(logEntry, browserHelper);
	}

	@Override
	public String getType() {
		logger.debug("getType()");
		return "DownloadRepresentation";
	}

}
