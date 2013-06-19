package eu.scape_project.roda.report;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.util.DateParser;

import ORG.oclc.oai.server.catalog.RecordFactory;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;

@SuppressWarnings("rawtypes")
public class EventRecordFactory extends RecordFactory {

	/**
	 * The logger.
	 */
	private static Logger logger = Logger.getLogger(EventRecordFactory.class);

	/**
	 * Constructs a new {@link EventRecordFactory}.
	 * 
	 * @param properties
	 *            the properties
	 * @see RecordFactory#RecordFactory(Properties)
	 */
	public EventRecordFactory(Properties properties) {
		super(properties);
		logger.debug(String.format("EventRecordFactory(properties=%s)",
				properties));
	}

	/**
	 * Constructs a new {@link EventRecordFactory}.
	 * 
	 * @param crosswalkMap
	 *            the crosswalk map
	 * @see RecordFactory#RecordFactory(HashMap)
	 */
	public EventRecordFactory(HashMap crosswalkMap) {
		super(crosswalkMap);
		logger.debug(String.format("EventRecordFactory(crosswalkMap=%s)",
				crosswalkMap));
	}

	@Override
	public String fromOAIIdentifier(String identifier) {
		logger.debug(String.format("fromOAIIdentifier(identifier=%s)",
				identifier));
		return null;
	}

	@Override
	public Iterator getAbouts(Object nativeItem) {
		logger.debug(String.format("getAbouts(nativeItem=%s)",
				StringUtils.abbreviate(String.valueOf(nativeItem), 20)));
		return null;
	}

	@Override
	public String getDatestamp(Object nativeItem) {
		logger.debug(String.format("getDatestamp(nativeItem=%s)",
				StringUtils.abbreviate(String.valueOf(nativeItem), 20)));
		return DateParser
				.getIsoDateNoMillis(((Event) nativeItem).getDatetime());
	}

	@Override
	public String getOAIIdentifier(Object nativeItem) {
		logger.debug(String.format("getOAIIdentifier(nativeItem=%s)",
				StringUtils.abbreviate(String.valueOf(nativeItem), 20)));
		return ((Event) nativeItem).getIdentifier();
	}

	@Override
	public Iterator getSetSpecs(Object nativeItem)
			throws IllegalArgumentException {
		logger.debug(String.format("getSetSpecs(nativeItem=%s)",
				StringUtils.abbreviate(String.valueOf(nativeItem), 20)));
		return Arrays.asList(((Event) nativeItem).getType()).iterator();
	}

	@Override
	public boolean isDeleted(Object nativeItem) {
		logger.debug(String.format("isDeleted(nativeItem=%s)",
				StringUtils.abbreviate(String.valueOf(nativeItem), 20)));
		return false;
	}

	@Override
	public String quickCreate(Object nativeItem, String schemaURL,
			String metadataPrefix) throws IllegalArgumentException,
			CannotDisseminateFormatException {
		logger.debug(String.format(
				"quickCreate(nativeItem=%s, schemaURL=%s, metadataPrefix=%s)",
				StringUtils.abbreviate(String.valueOf(nativeItem), 20),
				schemaURL, metadataPrefix));
		return null;
	}

}
