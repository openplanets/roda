package eu.scape_project.roda.report;

import java.util.Properties;

import org.apache.log4j.Logger;

import ORG.oclc.oai.server.crosswalk.Crosswalk;

/**
 * Converts a native item into an PREMIS v2 Event XML metadata.
 * 
 * @author Rui Castro
 * 
 * @see Crosswalk
 */
public class PremisFullCrosswalk extends PremisEventCrosswalk {

	/**
	 * The logger.
	 */
	private static Logger logger = Logger.getLogger(PremisFullCrosswalk.class);

	/**
	 * Constructs a new {@link PremisEventCrosswalk}.
	 * 
	 * @param schemaLocation
	 *            the schema location
	 * @see Crosswalk#Crosswalk(String)
	 */
	public PremisFullCrosswalk(String schemaLocation) {
		super(schemaLocation);
		logger.debug(String.format("PremisFullCrosswalk(schemaLocation=%s)",
				schemaLocation));
	}

	/**
	 * Constructs a new {@link PremisEventCrosswalk}.
	 * 
	 * @param properties
	 *            the properties
	 * @see Crosswalk#Crosswalk(String)
	 */
	public PremisFullCrosswalk(Properties properties) {
		super(
				"info:lc/xmlns/premis-v2 http://www.loc.gov/standards/premis/v2/premis.xsd");
		logger.debug(String.format("PremisFullCrosswalk(properties=%s)",
				properties));
	}

}
