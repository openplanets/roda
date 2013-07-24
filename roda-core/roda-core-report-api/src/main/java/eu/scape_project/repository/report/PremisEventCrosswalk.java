package eu.scape_project.repository.report;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import ORG.oclc.oai.server.crosswalk.Crosswalk;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;

/**
 * Converts a native item into an PREMIS v2 Event XML metadata.
 * 
 * @author Rui Castro
 * 
 * @see Crosswalk
 */
public class PremisEventCrosswalk extends Crosswalk {

	/**
	 * The logger.
	 */
	private static Logger logger = Logger.getLogger(PremisEventCrosswalk.class);

	/**
	 * Constructs a new {@link PremisEventCrosswalk}.
	 * 
	 * @param schemaLocation
	 *            the schema location
	 * @see Crosswalk#Crosswalk(String)
	 */
	public PremisEventCrosswalk(String schemaLocation) {
		super(schemaLocation);
		logger.debug(String.format("PremisEventCrosswalk(schemaLocation=%s)",
				schemaLocation));
	}

	/**
	 * Constructs a new {@link PremisEventCrosswalk}.
	 * 
	 * @param properties
	 *            the properties
	 * @see Crosswalk#Crosswalk(String)
	 */
	public PremisEventCrosswalk(Properties properties) {
		super(
				"info:lc/xmlns/premis-v2 http://www.loc.gov/standards/premis/v2/premis.xsd");
		logger.debug(String.format("PremisEventCrosswalk(properties=%s)",
				properties));
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
			metadata = event.getPremisEvent();
		} else {
			// what should we do?????
		}

		return metadata;
	}

}
