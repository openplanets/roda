package eu.scape_project.repository.report;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.w3c.util.DateParser;

import ORG.oclc.oai.server.catalog.AbstractCatalog;
import ORG.oclc.oai.server.verb.BadArgumentException;
import ORG.oclc.oai.server.verb.BadResumptionTokenException;
import ORG.oclc.oai.server.verb.CannotDisseminateFormatException;
import ORG.oclc.oai.server.verb.IdDoesNotExistException;
import ORG.oclc.oai.server.verb.NoItemsMatchException;
import ORG.oclc.oai.server.verb.NoMetadataFormatsException;
import ORG.oclc.oai.server.verb.NoSetHierarchyException;
import ORG.oclc.oai.server.verb.OAIInternalServerError;
import ORG.oclc.oai.util.OAIUtil;

/**
 * The RODA Report API Catalog.
 * 
 * {@link AbstractCatalog} implementation.
 * 
 * @author Rui Castro
 * @see AbstractCatalog
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractEventCatalog extends AbstractCatalog {

	/**
	 * The logger.
	 */
	private static Logger logger = Logger.getLogger(AbstractEventCatalog.class);

	/**
	 * maximum number of entries to return for ListRecords, ListIdentifiers and
	 * ListSets (loaded from properties).
	 */
	private int maxListSize;

	private String softwareName = null;
	private String softwareVersion = null;

	/**
	 * Constructs a new {@link AbstractEventCatalog}.
	 * 
	 * @param properties
	 *            the properties
	 * @param context
	 *            the {@link ServletContext}
	 * @throws OAIInternalServerError
	 */
	public AbstractEventCatalog(Properties properties, ServletContext context)
			throws OAIInternalServerError {
		logger.debug("AbstractReportCatalog(properties=" + properties
				+ ", context" + context + ")");

		this.maxListSize = NumberUtils.toInt(
				properties.getProperty("EventCatalog.maxListSize"), 100);
		logger.info("maxListSize=" + this.maxListSize);

		this.softwareName = properties.getProperty("EventCatalog.softwareName",
				"RODA");
		logger.info("softwareName=" + this.softwareName);

		this.softwareVersion = properties
				.getProperty("EventCatalog.softwareVersion");
		logger.info("softwareVersion=" + this.softwareVersion);

	}

	@Override
	public void close() {
		logger.debug("close()");
	}

	@Override
	public String getDescriptions() {
		String software = "<software name='" + this.softwareName + "' ";
		if (StringUtils.isBlank(this.softwareVersion)) {
			software += "/>";
		} else {
			software += "version='" + this.softwareVersion + "' />";
		}
		return software;
	}

	@Override
	public String getRecord(String identifier, String metadataPrefix)
			throws IdDoesNotExistException, CannotDisseminateFormatException,
			OAIInternalServerError {
		logger.info(String.format(
				"getRecord(identifier=%s, metadataPrefix=%s)", identifier,
				metadataPrefix));

		String schemaURL = getCrosswalks().getSchemaURL(metadataPrefix);
		if (schemaURL == null) {
			throw new CannotDisseminateFormatException(metadataPrefix);
		}

		return constructRecord(getEvent(identifier), metadataPrefix, schemaURL);
	}

	@Override
	public Vector getSchemaLocations(String identifier)
			throws IdDoesNotExistException, NoMetadataFormatsException,
			OAIInternalServerError {
		logger.info(String.format("getSchemaLocations(identifier=%s)",
				identifier));

		return getRecordFactory().getSchemaLocations(getEvent(identifier));
	}

	@Override
	public Map listRecords(String from, String until, String set,
			String metadataPrefix) throws BadArgumentException,
			CannotDisseminateFormatException, NoItemsMatchException,
			NoSetHierarchyException, OAIInternalServerError {

		logger.info(String.format(
				"listRecords(from=%s, until=%s, set=%s, metadataPrefix=%s)",
				from, until, set, metadataPrefix));

		from = "0001-01-01T00:00:00Z".equals(from) ? null : from;
		until = "9999-12-31T23:59:59Z".equals(until) ? null : until;
		set = StringUtils.isBlank(set) ? null : set;

		Integer ingestStartedIndex = null;
		Integer ingestFinishedIndex = null;
		Integer viewDMDIndex = null;
		Integer viewRepresentationIndex = null;
		Integer downloadRepresentationIndex = null;
		Integer planExecutedIndex = null;

		if ("IngestStarted".equals(set)) {
			ingestStartedIndex = 0;
		} else if ("IngestFinished".equals(set)) {
			ingestFinishedIndex = 0;
		} else if ("ViewDMD".equals(set)) {
			viewDMDIndex = 0;
		} else if ("ViewRepresentation".equals(set)) {
			viewRepresentationIndex = 0;
		} else if ("DownloadRepresentation".equals(set)) {
			downloadRepresentationIndex = 0;
		} else if ("PlanExecuted".equals(set)) {
			planExecutedIndex = 0;
		} else {

			logger.debug("No set (event type) selected. Listing all event types.");

			ingestStartedIndex = 0;
			ingestFinishedIndex = 0;
			viewDMDIndex = 0;
			viewRepresentationIndex = 0;
			downloadRepresentationIndex = 0;
			planExecutedIndex = 0;
		}

		try {

			return listRecords(from, until, metadataPrefix, ingestStartedIndex,
					ingestFinishedIndex, viewDMDIndex, viewRepresentationIndex,
					downloadRepresentationIndex, planExecutedIndex);

		} catch (EventCatalogException e) {
			logger.error("Couldn't list records - " + e.getMessage(), e);
			throw new OAIInternalServerError("Couldn't list records - "
					+ e.getMessage());
		}
	}

	@Override
	public Map listRecords(String resumptionToken)
			throws BadResumptionTokenException, OAIInternalServerError {

		logger.info(String.format("listRecords(resumptionToken=%s)",
				resumptionToken));

		if (StringUtils.isBlank(resumptionToken)) {
			logger.error("resumptionToken ('" + resumptionToken + "') is blank");
			throw new BadResumptionTokenException();
		} else {

			String[] tokenParts = resumptionToken.split(";", -1);

			logger.debug("resumptionToken parts are "
					+ Arrays.asList(tokenParts));

			// Validate token parts
			if (tokenParts.length != 9) {
				logger.error("resumptionToken ('" + resumptionToken
						+ "') has a wrong format.");
				throw new BadResumptionTokenException();
			} else {

				Integer ingestStartedIndex = NumberUtils
						.isNumber(tokenParts[3]) ? NumberUtils
						.toInt(tokenParts[3]) : null;
				Integer ingestFinishedIndex = NumberUtils
						.isNumber(tokenParts[4]) ? NumberUtils
						.toInt(tokenParts[4]) : null;
				Integer viewDMDIndex = NumberUtils.isNumber(tokenParts[5]) ? NumberUtils
						.toInt(tokenParts[5]) : null;
				Integer viewRepresentationIndex = NumberUtils
						.isNumber(tokenParts[6]) ? NumberUtils
						.toInt(tokenParts[6]) : null;
				Integer downloadRepresentationIndex = NumberUtils
						.isNumber(tokenParts[7]) ? NumberUtils
						.toInt(tokenParts[7]) : null;
				Integer planExecutedIndex = NumberUtils.isNumber(tokenParts[8]) ? NumberUtils
						.toInt(tokenParts[8]) : null;
				// int ingestStartedIndex = NumberUtils.toInt(tokenParts[3],
				// -1);
				// int ingestFinishedIndex = NumberUtils.toInt(tokenParts[4],
				// -1);
				// int viewDMDIndex = NumberUtils.toInt(tokenParts[5], -1);
				// int viewRepresentationIndex =
				// NumberUtils.toInt(tokenParts[6],
				// -1);
				// int downloadRepresentationIndex = NumberUtils.toInt(
				// tokenParts[7], -1);
				// int planExecutedIndex = NumberUtils.toInt(tokenParts[8], -1);

				try {

					return listRecords(tokenParts[0], tokenParts[1],
							tokenParts[2], ingestStartedIndex,
							ingestFinishedIndex, viewDMDIndex,
							viewRepresentationIndex,
							downloadRepresentationIndex, planExecutedIndex);

				} catch (CannotDisseminateFormatException e) {

					logger.error(
							"listRecords threw an exception - "
									+ e.getMessage(), e);
					throw new BadResumptionTokenException();

				} catch (EventCatalogException e) {
					logger.error("Couln't list records - " + e.getMessage(), e);
					throw new OAIInternalServerError("Couln't list records - "
							+ e.getMessage());
				}

			}
		}
	}

	@Override
	public Map listIdentifiers(String from, String until, String set,
			String metadataPrefix) throws BadArgumentException,
			CannotDisseminateFormatException, NoItemsMatchException,
			NoSetHierarchyException, OAIInternalServerError {

		logger.info(String
				.format("listIdentifiers(from=%s, until=%s, set=%s, metadataPrefix=%s)",
						from, until, set, metadataPrefix));

		from = "0001-01-01T00:00:00Z".equals(from) ? null : from;
		until = "9999-12-31T23:59:59Z".equals(until) ? null : until;
		set = StringUtils.isBlank(set) ? null : set;

		int ingestStartedIndex = -1;
		int ingestFinishedIndex = -1;
		int viewDMDIndex = -1;
		int viewRepresentationIndex = -1;
		int downloadRepresentationIndex = -1;
		int planExecutedIndex = -1;

		if ("IngestStarted".equals(set)) {
			ingestStartedIndex = 0;
		} else if ("IngestFinished".equals(set)) {
			ingestFinishedIndex = 0;
		} else if ("ViewDMD".equals(set)) {
			viewDMDIndex = 0;
		} else if ("ViewRepresentation".equals(set)) {
			viewRepresentationIndex = 0;
		} else if ("DownloadRepresentation".equals(set)) {
			downloadRepresentationIndex = 0;
		} else if ("PlanExecuted".equals(set)) {
			planExecutedIndex = 0;
		} else {

			logger.debug("No set (event type) selected. Listing all event types.");

			ingestStartedIndex = 0;
			ingestFinishedIndex = 0;
			viewDMDIndex = 0;
			viewRepresentationIndex = 0;
			downloadRepresentationIndex = 0;
			planExecutedIndex = 0;
		}

		try {

			return listIdentifiers(from, until, metadataPrefix,
					ingestStartedIndex, ingestFinishedIndex, viewDMDIndex,
					viewRepresentationIndex, downloadRepresentationIndex,
					planExecutedIndex);

		} catch (EventCatalogException e) {
			logger.error("Couln't list identifiers - " + e.getMessage(), e);
			throw new OAIInternalServerError("Couln't list identifiers - "
					+ e.getMessage());
		}

	}

	@Override
	public Map listIdentifiers(String resumptionToken)
			throws BadResumptionTokenException, OAIInternalServerError {

		logger.info(String.format("listIdentifiers(resumptionToken=%s)",
				resumptionToken));

		if (StringUtils.isBlank(resumptionToken)) {
			logger.error("resumptionToken ('" + resumptionToken + "') is blank");
			throw new BadResumptionTokenException();
		} else {

			String[] tokenParts = resumptionToken.split(";");

			// Validate token parts
			if (tokenParts.length != 9) {
				logger.error("resumptionToken ('" + resumptionToken
						+ "') has a wrong format");
				throw new BadResumptionTokenException();
			} else {

				int ingestStartedIndex = NumberUtils.toInt(tokenParts[3], -1);
				int ingestFinishedIndex = NumberUtils.toInt(tokenParts[4], -1);
				int viewDMDIndex = NumberUtils.toInt(tokenParts[5], -1);
				int viewRepresentationIndex = NumberUtils.toInt(tokenParts[6],
						-1);
				int downloadRepresentationIndex = NumberUtils.toInt(
						tokenParts[7], -1);
				int planExecutedIndex = NumberUtils.toInt(tokenParts[8], -1);

				try {

					return listIdentifiers(tokenParts[0], tokenParts[1],
							tokenParts[2], ingestStartedIndex,
							ingestFinishedIndex, viewDMDIndex,
							viewRepresentationIndex,
							downloadRepresentationIndex, planExecutedIndex);

				} catch (CannotDisseminateFormatException e) {

					logger.error(
							"listIdentifiers threw an exception - "
									+ e.getMessage(), e);
					throw new BadResumptionTokenException();

				} catch (EventCatalogException e) {
					logger.error(
							"Couln't list identifiers - " + e.getMessage(), e);
					throw new OAIInternalServerError(
							"Couln't list identifiers - " + e.getMessage());
				}

			}
		}

	}

	@Override
	public Map listSets() throws NoSetHierarchyException,
			OAIInternalServerError {
		logger.info("listSets()");

		Map<String, Object> listSetsMap = new HashMap<String, Object>();

		List<String> setSpecs = Arrays.asList("IngestStarted",
				"IngestFinished", "ViewDMD", "ViewRepresentation",
				"DownloadRepresentation", "PlanExecuted");
		List<String> setNames = Arrays.asList("Ingest started",
				"Ingest finished", "View descriptive metadata",
				"View representation", "Download representation",
				"Plan executed on an object");

		List<String> sets = new ArrayList<String>();
		for (int i = 0; i < setSpecs.size(); i++) {
			sets.add(getSetXML(setSpecs.get(i), setNames.get(i), null));
		}
		listSetsMap.put("sets", sets.iterator());
		return listSetsMap;
	}

	/**
	 * Extract &lt;set&gt; XML string from setItem object
	 * 
	 * @param setItem
	 *            individual set instance in native format
	 * @return an XML String containing the XML &lt;set&gt; content
	 */
	public String getSetXML(String setSpec, String setName,
			String setDescription) throws IllegalArgumentException {
		StringBuffer sb = new StringBuffer();
		sb.append("<set>");
		sb.append("<setSpec>");
		sb.append(OAIUtil.xmlEncode(setSpec));
		sb.append("</setSpec>");
		sb.append("<setName>");
		sb.append(OAIUtil.xmlEncode(setName));
		sb.append("</setName>");
		if (setDescription != null) {
			sb.append("<setDescription>");
			sb.append(OAIUtil.xmlEncode(setDescription));
			sb.append("</setDescription>");
		}
		sb.append("</set>");
		return sb.toString();
	}

	@Override
	public Map listSets(String resumptionToken)
			throws BadResumptionTokenException, OAIInternalServerError {
		logger.info(String.format("listSets(resumptionToken=%s)",
				resumptionToken));
		throw new BadResumptionTokenException();
	}

	private Map listRecords(String from, String until, String metadataPrefix,
			Integer ingestStartedIndex, Integer ingestFinishedIndex,
			Integer viewDMDIndex, Integer viewRepresentationIndex,
			Integer downloadRepresentationIndex, Integer planExecutedIndex)
			throws CannotDisseminateFormatException, EventCatalogException {

		logger.debug(String
				.format("listRecords(from=%s, until=%s, metadataPrefix=%s, "
						+ "ingestStartedIndex=%s, ingestFinishedIndex=%s, "
						+ "viewDMDIndex=%s, viewRepresentationIndex=%s, "
						+ "downloadRepresentationIndex=%s, planExecutedIndex=%s)",
						from, until, metadataPrefix, ingestStartedIndex,
						ingestFinishedIndex, viewDMDIndex,
						viewRepresentationIndex, downloadRepresentationIndex,
						planExecutedIndex));

		String schemaURL = getCrosswalks().getSchemaURL(metadataPrefix);
		if (schemaURL == null) {
			throw new CannotDisseminateFormatException(metadataPrefix);
		}

		List<Event> events = new ArrayList<Event>();
		int spaceLeft = this.maxListSize;
		int totalCount = 0;

		// IngestStartedEvent

		if (ingestStartedIndex != null) {
			int ingestStartedCount = getIngestStartedEventCount(from, until,
					metadataPrefix);
			logger.debug(String.format("Found %d IngestStarted events",
					ingestStartedCount));
			totalCount += ingestStartedCount;
			if (ingestStartedCount > 0) {
				if (ingestStartedIndex >= 0 && spaceLeft > 0) {

					List<Event> ingestStartedEvents = getIngestStartedEvents(
							from, until, metadataPrefix, ingestStartedIndex,
							spaceLeft);

					events.addAll(ingestStartedEvents);

					logger.debug(String.format("Added %d IngestStarted events",
							ingestStartedEvents.size()));

					spaceLeft = this.maxListSize - events.size();

					if (ingestStartedIndex + ingestStartedEvents.size() < ingestStartedCount) {
						ingestStartedIndex += ingestStartedEvents.size();
					} else {
						ingestStartedIndex = -ingestStartedCount;
					}
				}
			} else {
				ingestStartedIndex = -1;
			}
		}

		// IngestFinishedEvent

		if (ingestFinishedIndex != null) {
			int ingestFinishedCount = getIngestFinishedEventCount(from, until,
					metadataPrefix);
			logger.debug(String.format("Found %d IngestFinished events",
					ingestFinishedCount));
			totalCount += ingestFinishedCount;
			if (ingestFinishedCount > 0) {
				if (ingestFinishedIndex >= 0 && spaceLeft > 0) {

					List<Event> ingestFinishedEvents = getIngestFinishedEvents(
							from, until, metadataPrefix, ingestFinishedIndex,
							spaceLeft);

					events.addAll(ingestFinishedEvents);

					logger.debug(String.format(
							"Added %d IngestFinished events",
							ingestFinishedEvents.size()));

					spaceLeft = this.maxListSize - events.size();

					if (ingestFinishedIndex + ingestFinishedEvents.size() < ingestFinishedCount) {
						ingestFinishedIndex += ingestFinishedEvents.size();
					} else {
						ingestFinishedIndex = -ingestFinishedCount;
					}
				}
			} else {
				ingestFinishedIndex = -1;
			}
		}

		// ViewDMDEvent

		if (viewDMDIndex != null) {
			int viewDMDCount = getViewDMDEventCount(from, until, metadataPrefix);
			logger.debug(String.format("Found %d ViewDMD events", viewDMDCount));
			totalCount += viewDMDCount;
			if (viewDMDCount > 0) {
				if (viewDMDIndex >= 0 && spaceLeft > 0) {

					List<Event> viewDMDEvents = getViewDMDEvents(from, until,
							metadataPrefix, viewDMDIndex, spaceLeft);

					events.addAll(viewDMDEvents);

					logger.debug(String.format("Added %d ViewDMD events",
							viewDMDEvents.size()));

					spaceLeft = this.maxListSize - events.size();

					if (viewDMDIndex + viewDMDEvents.size() < viewDMDCount) {
						viewDMDIndex += viewDMDEvents.size();
					} else {
						viewDMDIndex = -viewDMDCount;
					}
				}
			} else {
				viewDMDIndex = -1;
			}
		}

		// ViewRepresentationEvent

		if (viewRepresentationIndex != null) {
			int viewRepresentationCount = getViewRepresentationEventCount(from,
					until, metadataPrefix);
			logger.debug(String.format("Found %d ViewRepresentation events",
					viewRepresentationCount));
			totalCount += viewRepresentationCount;
			if (viewRepresentationCount > 0) {
				if (viewRepresentationIndex >= 0 && spaceLeft > 0) {

					List<Event> viewRepresentationEvents = getViewRepresentationEvents(
							from, until, metadataPrefix,
							viewRepresentationIndex, spaceLeft);

					events.addAll(viewRepresentationEvents);

					logger.debug(String.format(
							"Added %d ViewRepresentation events",
							viewRepresentationEvents.size()));

					spaceLeft = this.maxListSize - events.size();

					if (viewRepresentationIndex
							+ viewRepresentationEvents.size() < viewRepresentationCount) {
						viewRepresentationIndex += viewRepresentationEvents
								.size();
					} else {
						viewRepresentationIndex = -viewRepresentationCount;
					}
				}
			} else {
				viewRepresentationIndex = -1;
			}
		}

		// DownloadRepresentationEvent

		if (downloadRepresentationIndex != null) {
			int downloadRepresentationCount = getDownloadRepresentationEventCount(
					from, until, metadataPrefix);
			logger.debug(String.format(
					"Found %d DownloadRepresentation events",
					downloadRepresentationCount));
			totalCount += downloadRepresentationCount;
			if (downloadRepresentationCount > 0) {
				if (downloadRepresentationIndex >= 0 && spaceLeft > 0) {

					List<Event> downloadRepresentationEvents = getDownloadRepresentationEvents(
							from, until, metadataPrefix,
							downloadRepresentationIndex, spaceLeft);

					events.addAll(downloadRepresentationEvents);

					logger.debug(String.format(
							"Added %d DownloadRepresentation events",
							downloadRepresentationEvents.size()));

					spaceLeft = this.maxListSize - events.size();

					if (downloadRepresentationIndex
							+ downloadRepresentationEvents.size() < downloadRepresentationCount) {
						downloadRepresentationIndex += downloadRepresentationEvents
								.size();
					} else {
						downloadRepresentationIndex = -downloadRepresentationCount;
					}
				}
			} else {
				downloadRepresentationIndex = -1;
			}
		}

		// PlanExecutedEvent

		if (planExecutedIndex != null) {
			int planExecutedCount = getPlanExecutedEventCount(from, until,
					metadataPrefix);
			logger.debug(String.format("Found %d PlanExecuted events",
					planExecutedCount));
			totalCount += planExecutedCount;
			if (planExecutedCount > 0) {
				if (planExecutedIndex >= 0 && spaceLeft > 0) {

					List<Event> planExecutedEvents = getPlanExecutedEvents(
							from, until, metadataPrefix, planExecutedIndex,
							spaceLeft);

					events.addAll(planExecutedEvents);

					logger.debug(String.format("Added %d PlanExecuted events",
							planExecutedEvents.size()));

					spaceLeft = this.maxListSize - events.size();

					if (planExecutedIndex + planExecutedEvents.size() < planExecutedCount) {
						planExecutedIndex += planExecutedEvents.size();
					} else {
						planExecutedIndex = -planExecutedCount;
					}
				}
			} else {
				planExecutedIndex = -1;
			}
		}

		Map<String, Object> listRecordsMap = new HashMap<String, Object>();
		ArrayList<String> records = new ArrayList<String>();

		for (Event event : events) {
			records.add(constructRecord(event, metadataPrefix, schemaURL));
		}

		String resumptionToken = null;
		if (ingestStartedIndex != null && ingestStartedIndex >= 0
				|| ingestFinishedIndex != null && ingestFinishedIndex >= 0
				|| viewDMDIndex != null && viewDMDIndex >= 0
				|| viewRepresentationIndex != null
				&& viewRepresentationIndex >= 0
				|| downloadRepresentationIndex != null
				&& downloadRepresentationIndex >= 0
				|| planExecutedIndex != null && planExecutedIndex >= 0) {

			resumptionToken = String.format("%s;%s;%s;%s;%s;%s;%s;%s;%s",
					from == null ? "" : from, until == null ? "" : until,
					metadataPrefix, ingestStartedIndex == null ? ""
							: ingestStartedIndex,
					ingestFinishedIndex == null ? "" : ingestFinishedIndex,
					viewDMDIndex == null ? "" : viewDMDIndex,
					viewRepresentationIndex == null ? ""
							: viewRepresentationIndex,
					downloadRepresentationIndex == null ? ""
							: downloadRepresentationIndex,
					planExecutedIndex == null ? "" : planExecutedIndex);
		}

		listRecordsMap.put("resumptionMap",
				getResumptionMap(resumptionToken, totalCount, 0));

		// listRecordsMap.put("metadataPrefix", metadataPrefix);
		// listRecordsMap.put("headers", headers.iterator());
		// listRecordsMap.put("identifiers", identifiers.iterator());
		listRecordsMap.put("records", records.iterator());

		return listRecordsMap;

	}

	private Map listIdentifiers(String from, String until,
			String metadataPrefix, int ingestStartedIndex,
			int ingestFinishedIndex, int viewDMDIndex,
			int viewRepresentationIndex, int downloadRepresentationIndex,
			int planExecutedIndex) throws CannotDisseminateFormatException,
			OAIInternalServerError, EventCatalogException {

		logger.debug(String
				.format("listIdentifiers(from=%s, until=%s, metadataPrefix=%s, "
						+ "ingestStartedIndex=%s, ingestFinishedIndex=%s, "
						+ "viewDMDIndex=%s, viewRepresentationIndex=%s, "
						+ "downloadRepresentationIndex=%s, planExecutedIndex=%s)",
						from, until, metadataPrefix, ingestStartedIndex,
						ingestFinishedIndex, viewDMDIndex,
						viewRepresentationIndex, downloadRepresentationIndex,
						planExecutedIndex));

		String schemaURL = getCrosswalks().getSchemaURL(metadataPrefix);
		if (schemaURL == null) {
			throw new CannotDisseminateFormatException(metadataPrefix);
		}

		List<Event> events = new ArrayList<Event>();
		int spaceLeft = this.maxListSize;
		int totalCount = 0;

		// IngestStartedEvent

		int ingestStartedCount = getIngestStartedEventCount(from, until,
				metadataPrefix);
		logger.debug(String.format("Found %d IngestStarted events",
				ingestStartedCount));
		totalCount += ingestStartedCount;
		if (ingestStartedCount > 0) {
			if (ingestStartedIndex >= 0 && spaceLeft > 0) {

				List<Event> ingestStartedEvents = getIngestStartedEvents(from,
						until, metadataPrefix, ingestStartedIndex, spaceLeft);

				events.addAll(ingestStartedEvents);

				logger.debug(String.format("Added %d IngestStarted events",
						ingestStartedEvents.size()));

				spaceLeft = this.maxListSize - events.size();

				if (ingestStartedIndex + ingestStartedEvents.size() < ingestStartedCount) {
					ingestStartedIndex += ingestStartedEvents.size();
				} else {
					ingestStartedIndex = -ingestStartedCount;
				}
			}
		} else {
			ingestStartedIndex = -1;
		}

		// IngestFinishedEvent

		int ingestFinishedCount = getIngestFinishedEventCount(from, until,
				metadataPrefix);
		logger.debug(String.format("Found %d IngestFinished events",
				ingestFinishedCount));
		totalCount += ingestFinishedCount;
		if (ingestFinishedCount > 0) {
			if (ingestFinishedIndex >= 0 && spaceLeft > 0) {

				List<Event> ingestFinishedEvents = getIngestFinishedEvents(
						from, until, metadataPrefix, ingestFinishedIndex,
						spaceLeft);

				events.addAll(ingestFinishedEvents);

				logger.debug(String.format("Added %d IngestFinished events",
						ingestFinishedEvents.size()));

				spaceLeft = this.maxListSize - events.size();

				if (ingestFinishedIndex + ingestFinishedEvents.size() < ingestFinishedCount) {
					ingestFinishedIndex += ingestFinishedEvents.size();
				} else {
					ingestFinishedIndex = -ingestFinishedCount;
				}
			}
		} else {
			ingestFinishedIndex = -1;
		}

		// ViewDMDEvent

		int viewDMDCount = getViewDMDEventCount(from, until, metadataPrefix);
		logger.debug(String.format("Found %d ViewDMD events", viewDMDCount));
		totalCount += viewDMDCount;
		if (viewDMDCount > 0) {
			if (viewDMDIndex >= 0 && spaceLeft > 0) {

				List<Event> viewDMDEvents = getViewDMDEvents(from, until,
						metadataPrefix, viewDMDIndex, spaceLeft);

				events.addAll(viewDMDEvents);

				logger.debug(String.format("Added %d ViewDMD events",
						viewDMDEvents.size()));

				spaceLeft = this.maxListSize - events.size();

				if (viewDMDIndex + viewDMDEvents.size() < viewDMDCount) {
					viewDMDIndex += viewDMDEvents.size();
				} else {
					viewDMDIndex = -viewDMDCount;
				}
			}
		} else {
			viewDMDIndex = -1;
		}

		// ViewRepresentationEvent

		int viewRepresentationCount = getViewRepresentationEventCount(from,
				until, metadataPrefix);
		logger.debug(String.format("Found %d ViewRepresentation events",
				viewRepresentationCount));
		totalCount += viewRepresentationCount;
		if (viewRepresentationCount > 0) {
			if (viewRepresentationIndex >= 0 && spaceLeft > 0) {

				List<Event> viewRepresentationEvents = getViewRepresentationEvents(
						from, until, metadataPrefix, viewRepresentationIndex,
						spaceLeft);

				events.addAll(viewRepresentationEvents);

				logger.debug(String.format(
						"Added %d ViewRepresentation events",
						viewRepresentationEvents.size()));

				spaceLeft = this.maxListSize - events.size();

				if (viewRepresentationIndex + viewRepresentationEvents.size() < viewRepresentationCount) {
					viewRepresentationIndex += viewRepresentationEvents.size();
				} else {
					viewRepresentationIndex = -viewRepresentationCount;
				}
			}
		} else {
			viewRepresentationIndex = -1;
		}

		// DownloadRepresentationEvent

		int downloadRepresentationCount = getDownloadRepresentationEventCount(
				from, until, metadataPrefix);
		logger.debug(String.format("Found %d DownloadRepresentation events",
				downloadRepresentationCount));
		totalCount += downloadRepresentationCount;
		if (downloadRepresentationCount > 0) {
			if (downloadRepresentationIndex >= 0 && spaceLeft > 0) {

				List<Event> downloadRepresentationEvents = getDownloadRepresentationEvents(
						from, until, metadataPrefix,
						downloadRepresentationIndex, spaceLeft);

				events.addAll(downloadRepresentationEvents);

				logger.debug(String.format(
						"Added %d DownloadRepresentation events",
						downloadRepresentationEvents.size()));

				spaceLeft = this.maxListSize - events.size();

				if (downloadRepresentationIndex
						+ downloadRepresentationEvents.size() < downloadRepresentationCount) {
					downloadRepresentationIndex += downloadRepresentationEvents
							.size();
				} else {
					downloadRepresentationIndex = -downloadRepresentationCount;
				}
			}
		} else {
			downloadRepresentationIndex = -1;
		}

		// PlanExecutedEvent

		int planExecutedCount = getPlanExecutedEventCount(from, until,
				metadataPrefix);
		logger.debug(String.format("Found %d PlanExecuted events",
				planExecutedCount));
		totalCount += planExecutedCount;
		if (planExecutedCount > 0) {
			if (planExecutedIndex >= 0 && spaceLeft > 0) {

				List<Event> planExecutedEvents = getPlanExecutedEvents(from,
						until, metadataPrefix, planExecutedIndex, spaceLeft);

				events.addAll(planExecutedEvents);

				logger.debug(String.format("Added %d PlanExecuted events",
						planExecutedEvents.size()));

				spaceLeft = this.maxListSize - events.size();

				if (planExecutedIndex + planExecutedEvents.size() < planExecutedCount) {
					planExecutedIndex += planExecutedEvents.size();
				} else {
					planExecutedIndex = -planExecutedCount;
				}
			}
		} else {
			planExecutedIndex = -1;
		}

		Map<String, Object> listIdentifiersMap = new HashMap<String, Object>();
		ArrayList<String> headers = new ArrayList<String>();
		ArrayList<String> identifiers = new ArrayList<String>();

		for (Event event : events) {

			String[] header = getRecordFactory().createHeader(event);
			headers.add(header[0]);
			identifiers.add(header[1]);

		}

		String resumptionToken = null;
		if (ingestStartedIndex >= 0 || ingestFinishedIndex >= 0
				|| viewDMDIndex >= 0 || viewRepresentationIndex >= 0
				|| downloadRepresentationIndex >= 0 || planExecutedIndex >= 0) {

			resumptionToken = String.format("%s;%s;%s;%s;%s;%s;%s;%s;%s",
					from == null ? "" : from, until == null ? "" : until,
					metadataPrefix, ingestStartedIndex, ingestFinishedIndex,
					viewDMDIndex, viewRepresentationIndex,
					downloadRepresentationIndex, planExecutedIndex);
		}

		listIdentifiersMap.put("resumptionMap",
				getResumptionMap(resumptionToken, totalCount, 0));

		listIdentifiersMap.put("headers", headers.iterator());
		listIdentifiersMap.put("identifiers", identifiers.iterator());

		return listIdentifiersMap;
	}

	private String constructRecord(Event event, String metadataPrefix,
			String schemaURL) throws IllegalArgumentException,
			CannotDisseminateFormatException {
		logger.debug(String.format(
				"constructRecord(event=%s, metadataPrefix=%s, schemaURL=%s)",
				event, metadataPrefix, schemaURL));

		List<String> abouts = new ArrayList<String>();
		if ("oai_dc".equalsIgnoreCase(metadataPrefix)) {
			// Nothing to add on about
		}
		if ("premis-event-v2".equalsIgnoreCase(metadataPrefix)) {
			String agent = event.getPremisAgent();
			if (agent != null) {
				abouts.add(event.getPremisAgent());
			}
		}
		if ("premis-full-v2".equalsIgnoreCase(metadataPrefix)) {

			String agent = event.getPremisAgent();
			if (agent != null) {
				abouts.add(agent);
			}

			List<String> relatedObjects = event.getPremisRelatedObjects();
			if (relatedObjects != null) {
				for (String relatedObj : relatedObjects) {
					if (relatedObj != null) {
						abouts.add(relatedObj);
					}
				}
			}
		}

		return getRecordFactory().create(event, schemaURL, metadataPrefix,
				event.getIdentifier(),
				DateParser.getIsoDate(event.getDatetime()),
				Arrays.asList(event.getType()).iterator(), abouts.iterator(),
				false);
	}

	private Event getEvent(String identifier) throws IdDoesNotExistException,
			OAIInternalServerError {
		logger.debug(String.format("getEvent(identifier=%s)", identifier));

		if (StringUtils.isBlank(identifier)) {
			throw new IdDoesNotExistException(identifier);
		}

		int separatorIndex = identifier.indexOf(":");

		if (separatorIndex < 1) {
			logger.error(String
					.format("identifier '%s' is not valid. It should be in the form of <Type>:<InnerID>",
							identifier));
			throw new IdDoesNotExistException(identifier);
		} else {

			String eventType = identifier.substring(0, separatorIndex);
			String innerID = identifier.substring(separatorIndex + 1);

			if (StringUtils.isBlank(eventType) || StringUtils.isBlank(innerID)) {

				logger.error(String
						.format("identifier '%s' is not valid. It should be in the form of <Type>:<InnerID>",
								identifier));

				throw new IdDoesNotExistException(identifier);
			}

			Event event = null;

			try {

				if ("IngestStarted".equals(eventType)) {
					event = getIngestStartedEvent(innerID);
				} else if ("IngestFinished".equals(eventType)) {
					event = getIngestFinishedEvent(innerID);
				} else if ("ViewDMD	".equals(eventType)) {
					event = getViewDMDEvent(innerID);
				} else if ("ViewRepresentation".equals(eventType)) {
					event = getViewRepresentationEvent(innerID);
				} else if ("DownloadRepresentation".equals(eventType)) {
					event = getDownloadRepresentationEvent(innerID);
				} else if ("PlanExecuted".equals(eventType)) {
					event = getPlanExecutedEvent(innerID);
				} else {
					throw new IdDoesNotExistException(identifier);
				}

				return event;

			} catch (EventCatalogException e) {
				logger.error("Couldn't retrieve event '" + identifier + "' - "
						+ e.getMessage());
				throw new IdDoesNotExistException(identifier);
			}

		}
	}

	protected abstract Event getIngestStartedEvent(String innerID)
			throws IdDoesNotExistException, EventCatalogException;

	protected abstract int getIngestStartedEventCount(String from,
			String until, String metadataPrefix) throws EventCatalogException;

	protected abstract List<Event> getIngestStartedEvents(String from,
			String until, String metadataPrefix, int startIndex, int maxResults)
			throws EventCatalogException;

	protected abstract Event getIngestFinishedEvent(String innerID)
			throws IdDoesNotExistException, EventCatalogException;

	protected abstract int getIngestFinishedEventCount(String from,
			String until, String metadataPrefix) throws EventCatalogException;

	protected abstract List<Event> getIngestFinishedEvents(String from,
			String until, String metadataPrefix, int startIndex, int maxResults)
			throws EventCatalogException;

	protected abstract int getViewDMDEventCount(String from, String until,
			String metadataPrefix) throws EventCatalogException;

	protected abstract Event getViewDMDEvent(String innerID)
			throws IdDoesNotExistException, EventCatalogException;

	protected abstract List<Event> getViewDMDEvents(String from, String until,
			String metadataPrefix, int startIndex, int maxResults)
			throws EventCatalogException;

	protected abstract int getViewRepresentationEventCount(String from,
			String until, String metadataPrefix) throws EventCatalogException;

	protected abstract Event getViewRepresentationEvent(String eventID)
			throws IdDoesNotExistException, EventCatalogException;

	protected abstract List<Event> getViewRepresentationEvents(String from,
			String until, String metadataPrefix, int startIndex, int maxResults)
			throws EventCatalogException;

	protected abstract int getDownloadRepresentationEventCount(String from,
			String until, String metadataPrefix) throws EventCatalogException;

	protected abstract Event getDownloadRepresentationEvent(String innerID)
			throws IdDoesNotExistException, EventCatalogException;

	protected abstract List<Event> getDownloadRepresentationEvents(String from,
			String until, String metadataPrefix, int startIndex, int maxResults)
			throws EventCatalogException;

	protected abstract Event getPlanExecutedEvent(String innerID)
			throws IdDoesNotExistException, EventCatalogException;

	protected abstract int getPlanExecutedEventCount(String from, String until,
			String metadataPrefix) throws EventCatalogException;

	protected abstract List<Event> getPlanExecutedEvents(String from,
			String until, String metadataPrefix, int startIndex, int maxResults)
			throws EventCatalogException;

	public static <T> String marshalToString(JAXBElement<T> element,
			boolean isFormatted, boolean isFragment, String schemaLocation,
			Class<?>... classes) throws JAXBException {

		JAXBContext jc = JAXBContext.newInstance(classes);
		Marshaller marshaller = jc.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, isFormatted);
		marshaller.setProperty(Marshaller.JAXB_FRAGMENT, isFragment);
		if (StringUtils.isNotBlank(schemaLocation)) {
			marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
					schemaLocation);
		}

		StringWriter sWriter = new StringWriter();
		marshaller.marshal(element, sWriter);
		return sWriter.toString();
	}

	public static <T> String marshalToString(JAXBElement<T> element,
			boolean isFormatted, boolean isFragment, String schemaLocation,
			String contextPath) throws JAXBException {

		JAXBContext jc = JAXBContext.newInstance(contextPath);
		Marshaller marshaller = jc.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, isFormatted);
		marshaller.setProperty(Marshaller.JAXB_FRAGMENT, isFragment);
		if (StringUtils.isNotBlank(schemaLocation)) {
			marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
					schemaLocation);
		}

		StringWriter sWriter = new StringWriter();
		marshaller.marshal(element, sWriter);
		return sWriter.toString();
	}

	@SuppressWarnings("unchecked")
	public static <T> JAXBElement<T> unmarshal(Class<T> clazz,
			InputStream inputStream) throws JAXBException {
		JAXBContext jc = JAXBContext.newInstance(clazz);
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		return (JAXBElement<T>) unmarshaller.unmarshal(inputStream);

	}

	@SuppressWarnings("unchecked")
	public static <T> JAXBElement<T> unmarshal(String contextPath,
			InputStream inputStream) throws JAXBException {
		JAXBContext jc = JAXBContext.newInstance(contextPath);
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		return (JAXBElement<T>) unmarshaller.unmarshal(inputStream);

	}

}
