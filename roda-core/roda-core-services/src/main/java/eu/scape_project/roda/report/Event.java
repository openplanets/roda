package eu.scape_project.roda.report;

import java.util.Date;
import java.util.List;

/**
 * This interface must be implemented by all classes that represent events in
 * the Report API.
 * 
 * @author Rui Castro
 */
public interface Event {

	/**
	 * Returns the type of the event.
	 * 
	 * @return a {@link String} with the type of the event.
	 */
	String getType();

	/**
	 * Returns the record identifier.
	 * 
	 * @return the record identifier.
	 */
	String getIdentifier();

	/**
	 * Returns the record datetime.
	 * 
	 * @return the record datetime.
	 */
	Date getDatetime();

	/**
	 * Returns the event in OAI_DC format.
	 * 
	 * @return a {@link String} with an XML representation of the OAI_DC event.
	 */
	String getOaiDcEvent();

	/**
	 * Returns the event in PREMIS format.
	 * 
	 * @return a {@link String} with an XML representation of the PREMIS event.
	 */
	String getPremisEvent();

	/**
	 * Returns the agent information in PREMIS format.
	 * 
	 * @return a {@link String} with an XML representation of a PREMIS agent.
	 */
	String getPremisAgent();

	/**
	 * Returns PREMIS objects related with the event.
	 * 
	 * @return a {@link List} with an XML representations of a PREMIS objects
	 *         related with the event.
	 */
	List<String> getPremisRelatedObjects();

}
