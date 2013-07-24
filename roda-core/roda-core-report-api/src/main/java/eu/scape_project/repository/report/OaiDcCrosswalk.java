package eu.scape_project.repository.report;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;

/**
 * Converts a native item into an OAI_DC XML metadata.
 * 
 * @author Rui Castro
 * 
 * @see Crosswalk
 */
public class OaiDcCrosswalk extends Crosswalk {

	/**
	 * The logger.
	 */
	private static Logger logger = Logger.getLogger(OaiDcCrosswalk.class);

	/**
	 * Constructs a new {@link OaiDcCrosswalk}.
	 * 
	 * @param schemaLocation
	 *            the schema location
	 * @see Crosswalk#Crosswalk(String)
	 */
	public OaiDcCrosswalk(String schemaLocation) {
		super(schemaLocation);
		logger.debug(String.format("OaiDcCrosswalk(schemaLocation=%s)",
				schemaLocation));
	}

	/**
	 * Constructs a new {@link OaiDcCrosswalk}.
	 * 
	 * @param properties
	 *            the properties
	 * @see Crosswalk#Crosswalk(String)
	 */

	public OaiDcCrosswalk(Properties properties) {
		super("http://www.openarchives.org/OAI/2.0/oai_dc/ "
				+ "http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
		logger.debug(String.format("OaiDcCrosswalk(properties=%s)", properties));
	}

	@Override
	public boolean isAvailableFor(Object nativeItem) {
		logger.debug(String.format("isAvailableFor(nativeItem=%s)",
				StringUtils.abbreviate(String.valueOf(nativeItem), 20)));

		return (nativeItem != null && nativeItem instanceof Event);
	}

	@Override
	public String createMetadata(Object nativeItem)
			throws CannotDisseminateFormatException {
		logger.debug(String.format("createMetadata(nativeItem=%s)",
				StringUtils.abbreviate(String.valueOf(nativeItem), 20)));

		String metadata = null;

		if (nativeItem != null && nativeItem instanceof Event) {
			Event event = (Event) nativeItem;
			metadata = event.getOaiDcEvent();
		} else {
			// what should we do?????
		}

		return metadata;
	}

}
