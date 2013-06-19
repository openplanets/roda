package eu.scape_project.roda.report;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.w3c.util.DateParser;

import pt.gov.dgarq.roda.core.BrowserHelper;
import pt.gov.dgarq.roda.core.common.BrowserException;
import pt.gov.dgarq.roda.core.common.LoggerException;
import pt.gov.dgarq.roda.core.common.NoSuchRODAObjectException;
import pt.gov.dgarq.roda.core.common.NoSuchSIPException;
import pt.gov.dgarq.roda.core.common.RODAServiceException;
import pt.gov.dgarq.roda.core.common.UserManagementException;
import pt.gov.dgarq.roda.core.data.LogEntry;
import pt.gov.dgarq.roda.core.data.SIPState;
import pt.gov.dgarq.roda.core.data.SimpleEventPreservationObject;
import pt.gov.dgarq.roda.core.data.adapter.ContentAdapter;
import pt.gov.dgarq.roda.core.data.adapter.filter.Filter;
import pt.gov.dgarq.roda.core.data.adapter.filter.FilterParameter;
import pt.gov.dgarq.roda.core.data.adapter.filter.LikeFilterParameter;
import pt.gov.dgarq.roda.core.data.adapter.filter.RangeFilterParameter;
import pt.gov.dgarq.roda.core.data.adapter.filter.SimpleFilterParameter;
import pt.gov.dgarq.roda.core.data.adapter.sublist.Sublist;
import pt.gov.dgarq.roda.core.fedora.FedoraClientException;
import pt.gov.dgarq.roda.core.fedora.FedoraClientUtility;
import pt.gov.dgarq.roda.core.ingest.IngestManager;
import pt.gov.dgarq.roda.core.ingest.IngestRegistryException;
import pt.gov.dgarq.roda.core.logger.LoggerManager;
import pt.gov.dgarq.roda.core.services.UserBrowser;
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
public class EventCatalog extends AbstractCatalog {

	/**
	 * The logger.
	 */
	private static Logger logger = Logger.getLogger(EventCatalog.class);

	private ServletContext context = null;

	private Configuration configuration = null;

	private Map<String, BrowserHelperCache> userBrowsers = null;

	/**
	 * maximum number of entries to return for ListRecords, ListIdentifiers and
	 * ListSets (loaded from properties).
	 */
	private int maxListSize;

	private IngestManager ingestManager = null;

	private String softwareName = null;
	private String softwareVersion = null;

	/**
	 * Maximum browser cache duration in nanoseconds. Default value is 5 minutes
	 * (3e+11 nanoseconds).
	 */
	private long cacheMaxDuration = (long) Math.pow(3, 11);

	private Properties properties;

	private LoggerManager loggerManager = null;

	/**
	 * Constructs a new {@link EventCatalog}.
	 * 
	 * @param properties
	 *            the properties
	 * @param context
	 *            the {@link ServletContext}
	 * @throws OAIInternalServerError
	 */
	public EventCatalog(Properties properties, ServletContext context)
			throws OAIInternalServerError {
		logger.debug("EventCatalog(properties=" + properties + ", context"
				+ context + ")");

		this.properties = properties;
		this.context = context;

		this.maxListSize = NumberUtils.toInt(
				properties.getProperty("EventCatalog.maxListSize"), 100);
		logger.info("maxListSize=" + this.maxListSize);

		this.softwareName = properties.getProperty("EventCatalog.softwareName",
				"RODA");
		logger.info("softwareName=" + this.softwareName);

		this.softwareVersion = properties
				.getProperty("EventCatalog.softwareVersion");
		logger.info("softwareVersion=" + this.softwareVersion);

		try {

			this.ingestManager = IngestManager.getDefaultIngestManager();

		} catch (IngestRegistryException e) {
			logger.error("Error initializing EventCatalog - " + e.getMessage(),
					e);
			throw new OAIInternalServerError(
					"Error initializing EventCatalog - " + e.getMessage());
		}

		try {

			String propertiesFile = context.getInitParameter("properties");
			logger.debug("ServletContext.getInitParameter('properties') => "
					+ propertiesFile);
			this.configuration = new PropertiesConfiguration(propertiesFile);

		} catch (ConfigurationException e) {
			logger.error("Couldn't load configuration from properties file - "
					+ e.getMessage(), e);
		}

		this.userBrowsers = new HashMap<String, BrowserHelperCache>();
	}

	@Override
	public void close() {
		logger.debug("close()");
		ingestManager = null;
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

		} catch (RODAServiceException e) {
			logger.error("Couldn't list records - " + e.getMessage(), e);
			throw new OAIInternalServerError("Couldn't list records - "
					+ e.getMessage());
		} catch (FedoraClientException e) {
			logger.error("Couldn't list records - " + e.getMessage(), e);
			throw new OAIInternalServerError("Couldn't list records - "
					+ e.getMessage());
		} catch (MalformedURLException e) {
			logger.error("Couldn't list records - " + e.getMessage(), e);
			throw new OAIInternalServerError("Couldn't list records - "
					+ e.getMessage());
		} catch (LoggerException e) {
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

				} catch (RODAServiceException e) {
					logger.error("Couln't list records - " + e.getMessage(), e);
					throw new OAIInternalServerError("Couln't list records - "
							+ e.getMessage());
				} catch (FedoraClientException e) {
					logger.error("Couln't list records - " + e.getMessage(), e);
					throw new OAIInternalServerError("Couln't list records - "
							+ e.getMessage());
				} catch (MalformedURLException e) {
					logger.error("Couln't list records - " + e.getMessage(), e);
					throw new OAIInternalServerError("Couln't list records - "
							+ e.getMessage());
				} catch (LoggerException e) {
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

		} catch (RODAServiceException e) {
			logger.error("Couln't list identifiers - " + e.getMessage(), e);
			throw new OAIInternalServerError("Couln't list identifiers - "
					+ e.getMessage());
		} catch (FedoraClientException e) {
			logger.error("Couln't list identifiers - " + e.getMessage(), e);
			throw new OAIInternalServerError("Couln't list identifiers - "
					+ e.getMessage());
		} catch (MalformedURLException e) {
			logger.error("Couln't list identifiers - " + e.getMessage(), e);
			throw new OAIInternalServerError("Couln't list identifiers - "
					+ e.getMessage());
		} catch (LoggerException e) {
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

				} catch (RODAServiceException e) {
					logger.error(
							"Couln't list identifiers - " + e.getMessage(), e);
					throw new OAIInternalServerError(
							"Couln't list identifiers - " + e.getMessage());
				} catch (FedoraClientException e) {
					logger.error(
							"Couln't list identifiers - " + e.getMessage(), e);
					throw new OAIInternalServerError(
							"Couln't list identifiers - " + e.getMessage());
				} catch (MalformedURLException e) {
					logger.error(
							"Couln't list identifiers - " + e.getMessage(), e);
					throw new OAIInternalServerError(
							"Couln't list identifiers - " + e.getMessage());
				} catch (LoggerException e) {
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
				"Ingest finished", "View descriptive metadata", "View representation",
				"Download representation", "Plan executed on an object");
		
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
			throws CannotDisseminateFormatException, RODAServiceException,
			FedoraClientException, MalformedURLException, LoggerException {

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

					List<IngestStartedEvent> ingestStartedEvents = getIngestStartedEvents(
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

					List<IngestFinishedEvent> ingestFinishedEvents = getIngestFinishedEvents(
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

					List<ViewDMDEvent> viewDMDEvents = getViewDMDEvents(from,
							until, metadataPrefix, viewDMDIndex, spaceLeft);

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

					List<ViewRepresentationEvent> viewRepresentationEvents = getViewRepresentationEvents(
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

					List<DownloadRepresentationEvent> downloadRepresentationEvents = getDownloadRepresentationEvents(
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

					List<PlanExecutedEvent> planExecutedEvents = getPlanExecutedEvents(
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
			OAIInternalServerError, RODAServiceException,
			FedoraClientException, MalformedURLException, LoggerException {

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

				List<IngestStartedEvent> ingestStartedEvents = getIngestStartedEvents(
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

		// IngestFinishedEvent

		int ingestFinishedCount = getIngestFinishedEventCount(from, until,
				metadataPrefix);
		logger.debug(String.format("Found %d IngestFinished events",
				ingestFinishedCount));
		totalCount += ingestFinishedCount;
		if (ingestFinishedCount > 0) {
			if (ingestFinishedIndex >= 0 && spaceLeft > 0) {

				List<IngestFinishedEvent> ingestFinishedEvents = getIngestFinishedEvents(
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

				List<ViewDMDEvent> viewDMDEvents = getViewDMDEvents(from,
						until, metadataPrefix, viewDMDIndex, spaceLeft);

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

				List<ViewRepresentationEvent> viewRepresentationEvents = getViewRepresentationEvents(
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

				List<DownloadRepresentationEvent> downloadRepresentationEvents = getDownloadRepresentationEvents(
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

				List<PlanExecutedEvent> planExecutedEvents = getPlanExecutedEvents(
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

			} catch (NoSuchSIPException e) {
				logger.error("Couldn't retrieve event '" + identifier + "' - "
						+ e.getMessage());
				throw new IdDoesNotExistException(identifier);
			} catch (IngestRegistryException e) {
				logger.error("Couldn't retrieve event '" + identifier + "' - "
						+ e.getMessage());
				throw new OAIInternalServerError("Couldn't retrieve event '"
						+ identifier + "' - " + e.getMessage());
			} catch (FedoraClientException e) {
				logger.error("Couldn't retrieve event '" + identifier + "' - "
						+ e.getMessage());
				throw new OAIInternalServerError("Couldn't retrieve event '"
						+ identifier + "' - " + e.getMessage());
			} catch (MalformedURLException e) {
				logger.error("Couldn't retrieve event '" + identifier + "' - "
						+ e.getMessage());
				throw new OAIInternalServerError("Couldn't retrieve event '"
						+ identifier + "' - " + e.getMessage());
			} catch (BrowserException e) {
				logger.error("Couldn't retrieve event '" + identifier + "' - "
						+ e.getMessage());
				throw new OAIInternalServerError("Couldn't retrieve event '"
						+ identifier + "' - " + e.getMessage());
			} catch (NoSuchRODAObjectException e) {
				logger.error("Couldn't retrieve event '" + identifier + "' - "
						+ e.getMessage());
				throw new OAIInternalServerError("Couldn't retrieve event '"
						+ identifier + "' - " + e.getMessage());
			} catch (LoggerException e) {
				logger.error("Couldn't retrieve event '" + identifier + "' - "
						+ e.getMessage());
				throw new OAIInternalServerError("Couldn't retrieve event '"
						+ identifier + "' - " + e.getMessage());
			}

		}
	}

	private Event getIngestStartedEvent(String sipID)
			throws NoSuchSIPException, IngestRegistryException {
		logger.debug(String.format("getIngestStartedEvent(sipID=%s)", sipID));

		SIPState sipState = this.ingestManager.getSIP(sipID);
		return new IngestStartedEvent(sipState);
	}

	private int getIngestStartedEventCount(String from, String until,
			String metadataPrefix) throws RODAServiceException {

		logger.debug(String
				.format("getIngestStartedEventCount(from=%s, until=%s, metadataPrefix=%s)",
						from, until, metadataPrefix));

		Filter filter = null;
		if (StringUtils.isNotBlank(from) || StringUtils.isNotBlank(until)) {

			FilterParameter filterParamFromUntil = new RangeFilterParameter(
					"datetime", from, until);
			filter = new Filter(filterParamFromUntil);
		}

		return this.ingestManager.getSIPsCount(filter);
	}

	private List<IngestStartedEvent> getIngestStartedEvents(String from,
			String until, String metadataPrefix, int startIndex, int maxResults)
			throws RODAServiceException {

		logger.debug(String.format(
				"getIngestStartedEvents(from=%s, until=%s, metadataPrefix=%s, "
						+ "startIndex=%s, maxResults=%s)", from, until,
				metadataPrefix, startIndex, maxResults));

		Filter filter = null;
		if (StringUtils.isNotBlank(from) || StringUtils.isNotBlank(until)) {

			FilterParameter filterParamFromUntil = new RangeFilterParameter(
					"datetime", from, until);
			filter = new Filter(filterParamFromUntil);
		}

		ContentAdapter cAdapter = new ContentAdapter(filter, null, new Sublist(
				startIndex, maxResults));
		List<SIPState> sips = this.ingestManager.getSIPs(cAdapter);

		List<IngestStartedEvent> events = new ArrayList<IngestStartedEvent>(
				sips.size());
		for (SIPState sip : sips) {
			events.add(new IngestStartedEvent(sip));
		}

		return events;
	}

	private Event getIngestFinishedEvent(String sipID)
			throws NoSuchSIPException, IngestRegistryException,
			FedoraClientException, MalformedURLException {
		logger.debug(String.format("getIngestFinishedEvent(sipID=%s)", sipID));

		SIPState sipState = this.ingestManager.getSIP(sipID);
		if (sipState.isComplete()) {
			return new IngestFinishedEvent(sipState, getBrowserHelper());
		} else {
			throw new NoSuchSIPException();
		}
	}

	private int getIngestFinishedEventCount(String from, String until,
			String metadataPrefix) throws RODAServiceException {

		logger.debug(String
				.format("getIngestFinishedEventCount(from=%s, until=%s, metadataPrefix=%s)",
						from, until, metadataPrefix));

		FilterParameter filterParamFromUntil = new RangeFilterParameter(
				"datetime", from, until);
		FilterParameter filterParamCompleted = new SimpleFilterParameter(
				"completed", "true");
		Filter filter = new Filter(new FilterParameter[] {
				filterParamFromUntil, filterParamCompleted });

		return this.ingestManager.getSIPsCount(filter);
	}

	private List<IngestFinishedEvent> getIngestFinishedEvents(String from,
			String until, String metadataPrefix, int startIndex, int maxResults)
			throws RODAServiceException, FedoraClientException,
			MalformedURLException {

		logger.debug(String.format(
				"getIngestFinishedEvents(from=%s, until=%s, metadataPrefix=%s, "
						+ "startIndex=%s, maxResults=%s)", from, until,
				metadataPrefix, startIndex, maxResults));

		FilterParameter filterParamFromUntil = new RangeFilterParameter(
				"datetime", from, until);
		FilterParameter filterParamCompleted = new SimpleFilterParameter(
				"completed", "true");
		Filter filter = new Filter(new FilterParameter[] {
				filterParamFromUntil, filterParamCompleted });

		ContentAdapter cAdapter = new ContentAdapter(filter, null, new Sublist(
				startIndex, maxResults));
		List<SIPState> sips = this.ingestManager.getSIPs(cAdapter);

		List<IngestFinishedEvent> events = new ArrayList<IngestFinishedEvent>(
				sips.size());
		for (SIPState sip : sips) {
			events.add(new IngestFinishedEvent(sip, getBrowserHelper()));
		}

		return events;

	}

	private int getViewDMDEventCount(String from, String until,
			String metadataPrefix) throws LoggerException {

		logger.debug(String.format(
				"getViewDMDEventCount(from=%s, until=%s, metadataPrefix=%s)",
				from, until, metadataPrefix));

		// TODO this can be improved by filtering system users:
		// roda-ingest-task, roda-preservation-task, etc
		Filter filter = new Filter();
		filter.add(new RangeFilterParameter("datetime", from, until));
		filter.add(new SimpleFilterParameter("action", "RODAWUI.pageHit"));
		filter.add(new LikeFilterParameter("description",
				"dissemination.browse.%"));

		return getLoggerManager().getLogEntriesCount(filter);
	}

	private Event getViewDMDEvent(String eventID) throws LoggerException,
			FedoraClientException, MalformedURLException,
			NoSuchRODAObjectException {
		logger.debug(String.format("getViewDMDEvent(eventID=%s)", eventID));

		return new ViewDMDEvent(getLogEntry(eventID), getBrowserHelper());
	}

	private List<ViewDMDEvent> getViewDMDEvents(String from, String until,
			String metadataPrefix, int startIndex, int maxResults)
			throws LoggerException, FedoraClientException,
			MalformedURLException {

		logger.debug(String.format(
				"getViewDMDEvents(from=%s, until=%s, metadataPrefix=%s, "
						+ "startIndex=%s, maxResults=%s)", from, until,
				metadataPrefix, startIndex, maxResults));

		// TODO this can be improved by filtering system users:
		// roda-ingest-task, roda-preservation-task, etc
		Filter filter = new Filter();
		if (StringUtils.isNotBlank(from) || StringUtils.isNotBlank(until)) {

			filter.add(new RangeFilterParameter("datetime", from, until));
		}
		filter.add(new SimpleFilterParameter("action", "RODAWUI.pageHit"));
		filter.add(new LikeFilterParameter("description",
				"dissemination.browse.%"));
		ContentAdapter cAdapter = new ContentAdapter(filter, null, new Sublist(
				startIndex, maxResults));

		List<LogEntry> logEntries = getLoggerManager().getLogEntries(cAdapter);

		List<ViewDMDEvent> events = new ArrayList<ViewDMDEvent>();
		for (LogEntry logEntry : logEntries) {

			events.add(new ViewDMDEvent(logEntry, getBrowserHelper()));

		}

		return events;
	}

	private int getViewRepresentationEventCount(String from, String until,
			String metadataPrefix) throws LoggerException {

		logger.debug(String
				.format("getViewRepresentationEventCount(from=%s, until=%s, metadataPrefix=%s)",
						from, until, metadataPrefix));

		// TODO this can be improved by filtering system users:
		// roda-ingest-task, roda-preservation-task, etc
		Filter filter = new Filter();
		filter.add(new RangeFilterParameter("datetime", from, until));
		filter.add(new LikeFilterParameter("action", "disseminator.hit.%"));

		return getLoggerManager().getLogEntriesCount(filter);
	}

	private Event getViewRepresentationEvent(String eventID)
			throws LoggerException, FedoraClientException,
			MalformedURLException, NoSuchRODAObjectException {
		logger.debug(String.format("getViewRepresentationEvent(eventID=%s)",
				eventID));
		return new ViewRepresentationEvent(getLogEntry(eventID),
				getBrowserHelper());
	}

	private List<ViewRepresentationEvent> getViewRepresentationEvents(
			String from, String until, String metadataPrefix, int startIndex,
			int maxResults) throws LoggerException, FedoraClientException,
			MalformedURLException {

		logger.debug(String
				.format("getViewRepresentationEvents(from=%s, until=%s, metadataPrefix=%s)",
						from, until, metadataPrefix, startIndex, maxResults));

		// TODO this can be improved by filtering system users:
		// roda-ingest-task, roda-preservation-task, etc
		Filter filter = new Filter();
		if (StringUtils.isNotBlank(from) || StringUtils.isNotBlank(until)) {
			filter.add(new RangeFilterParameter("datetime", from, until));
		}
		filter.add(new LikeFilterParameter("action", "disseminator.hit.%"));
		ContentAdapter cAdapter = new ContentAdapter(filter, null, new Sublist(
				startIndex, maxResults));

		List<LogEntry> logEntries = getLoggerManager().getLogEntries(cAdapter);

		List<ViewRepresentationEvent> events = new ArrayList<ViewRepresentationEvent>();
		for (LogEntry logEntry : logEntries) {
			events.add(new ViewRepresentationEvent(logEntry, getBrowserHelper()));
		}

		return events;
	}

	private int getDownloadRepresentationEventCount(String from, String until,
			String metadataPrefix) throws LoggerException {

		logger.debug(String
				.format("getDownloadRepresentationEventCount(from=%s, until=%s, metadataPrefix=%s)",
						from, until, metadataPrefix));

		// TODO this can be improved by filtering system users:
		// roda-ingest-task, roda-preservation-task, etc
		Filter filter = new Filter();
		filter.add(new RangeFilterParameter("datetime", from, until));
		filter.add(new SimpleFilterParameter("action",
				"disseminator.hit.AIPDownload"));

		return getLoggerManager().getLogEntriesCount(filter);
	}

	private Event getDownloadRepresentationEvent(String eventID)
			throws LoggerException, FedoraClientException,
			MalformedURLException, NoSuchRODAObjectException {
		logger.debug(String.format(
				"getDownloadRepresentationEvent(eventID=%s)", eventID));
		return new DownloadRepresentationEvent(getLogEntry(eventID),
				getBrowserHelper());
	}

	private List<DownloadRepresentationEvent> getDownloadRepresentationEvents(
			String from, String until, String metadataPrefix, int startIndex,
			int maxResults) throws LoggerException, FedoraClientException,
			MalformedURLException {

		logger.debug(String.format(
				"getDownloadRepresentationEvents(from=%s, until=%s, metadataPrefix=%s, "
						+ "startIndex=%s, maxResults=%s)", from, until,
				metadataPrefix, startIndex, maxResults));

		// TODO this can be improved by filtering system users:
		// roda-ingest-task, roda-preservation-task, etc
		Filter filter = new Filter();
		if (StringUtils.isNotBlank(from) || StringUtils.isNotBlank(until)) {
			filter.add(new RangeFilterParameter("datetime", from, until));
		}
		filter.add(new SimpleFilterParameter("action",
				"disseminator.hit.AIPDownload"));
		ContentAdapter cAdapter = new ContentAdapter(filter, null, new Sublist(
				startIndex, maxResults));

		List<LogEntry> logEntries = getLoggerManager().getLogEntries(cAdapter);

		List<DownloadRepresentationEvent> events = new ArrayList<DownloadRepresentationEvent>();
		for (LogEntry logEntry : logEntries) {
			events.add(new DownloadRepresentationEvent(logEntry,
					getBrowserHelper()));
		}

		return events;
	}

	private Event getPlanExecutedEvent(String eventPID)
			throws BrowserException, NoSuchRODAObjectException,
			FedoraClientException, MalformedURLException {
		logger.debug(String.format("getPlanExecutedEvent(eventPID=%s)",
				eventPID));

		SimpleEventPreservationObject simpleEPO = getBrowserHelper()
				.getSimpleEventPreservationObject(eventPID);
		return new PlanExecutedEvent(simpleEPO, getBrowserHelper());
	}

	private int getPlanExecutedEventCount(String from, String until,
			String metadataPrefix) throws BrowserException,
			FedoraClientException, MalformedURLException {

		logger.debug(String
				.format("getPlanExecutedEventCount(from=%s, until=%s, metadataPrefix=%s",
						from, until, metadataPrefix));

		FilterParameter filterParamFromUntil = new RangeFilterParameter(
				"datetime", from, until);
		FilterParameter filterParamLabelMigration = new SimpleFilterParameter(
				"label", "migration");
		Filter filter = new Filter(new FilterParameter[] {
				filterParamFromUntil, filterParamLabelMigration });

		return getBrowserHelper().getSimpleEventPreservationObjectCount(filter);
	}

	private List<PlanExecutedEvent> getPlanExecutedEvents(String from,
			String until, String metadataPrefix, int startIndex, int maxResults)
			throws BrowserException, FedoraClientException,
			MalformedURLException {

		logger.debug(String.format(
				"getPlanExecutedEvents(from=%s, until=%s, metadataPrefix=%s, "
						+ "startIndex=%s, maxResults=%s)", from, until,
				metadataPrefix, startIndex, maxResults));

		FilterParameter filterParamFromUntil = new RangeFilterParameter(
				"datetime", from, until);
		FilterParameter filterParamLabelMigration = new SimpleFilterParameter(
				"label", "migration");
		Filter filter = new Filter(new FilterParameter[] {
				filterParamFromUntil, filterParamLabelMigration });

		ContentAdapter cAdapter = new ContentAdapter(filter, null, new Sublist(
				startIndex, maxResults));
		List<SimpleEventPreservationObject> premisMigrationEvents = getBrowserHelper()
				.getSimpleEventPreservationObjects(cAdapter);

		List<PlanExecutedEvent> events = new ArrayList<PlanExecutedEvent>(
				premisMigrationEvents.size());
		for (SimpleEventPreservationObject migrationEvent : premisMigrationEvents) {
			events.add(new PlanExecutedEvent(migrationEvent, getBrowserHelper()));
		}

		return events;
	}

	private LogEntry getLogEntry(String logEntryID) throws LoggerException,
			FedoraClientException, MalformedURLException,
			NoSuchRODAObjectException {
		logger.debug(String.format("getLogEntry(logEntryID=%s)", logEntryID));

		Filter filter = new Filter();
		filter.add(new SimpleFilterParameter("id", logEntryID));
		ContentAdapter cAdapter = new ContentAdapter(filter, null, new Sublist(
				0, 1));

		List<LogEntry> logEntries = getLoggerManager().getLogEntries(cAdapter);

		if (logEntries != null && logEntries.size() > 0) {
			return logEntries.get(0);
		} else {
			throw new NoSuchRODAObjectException();
		}
	}

	private BrowserHelper getBrowserHelper() throws FedoraClientException,
			MalformedURLException {

		// pt.gov.dgarq.roda.core.data.User user =
		// (pt.gov.dgarq.roda.core.data.User) this.context
		// .getAttribute("EventCatalog.user");
		// String userPassword = (String)
		// this.context.getAttribute("EventCatalog.password");

		pt.gov.dgarq.roda.core.data.User user = null;
		try {
			UserBrowser userBrowser = new UserBrowser();
			user = userBrowser.getUser(this.properties
					.getProperty("adminUsername"));
		} catch (UserManagementException e) {
			logger.error("Couln't get admin user - " + e.getMessage(), e);
		} catch (RODAServiceException e) {
			logger.error("Couln't get admin user - " + e.getMessage(), e);
		}
		String userPassword = this.properties.getProperty("adminPassword");

		BrowserHelperCache browser = this.userBrowsers.get(user.getName());

		if (browser == null
				|| (System.nanoTime() - browser.creationTime) > this.cacheMaxDuration) {

			String fedoraURL = this.configuration.getString("fedoraURL");
			String fedoraGSearchURL = this.configuration
					.getString("fedoraGSearchURL");
			FedoraClientUtility fedoraClient = new FedoraClientUtility(
					fedoraURL, fedoraGSearchURL, user, userPassword);

			this.userBrowsers.put(user.getName(), new BrowserHelperCache(
					fedoraClient, this.configuration));
		}

		return this.userBrowsers.get(user.getName());
	}

	/**
	 * @return the {@link LoggerManager}.
	 * @throws LoggerException
	 */
	protected LoggerManager getLoggerManager() throws LoggerException {
		if (loggerManager == null) {
			loggerManager = LoggerManager.getDefaultLoggerManager();
		}
		return loggerManager;
	}

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

	class BrowserHelperCache extends BrowserHelper {

		long creationTime;

		public BrowserHelperCache(FedoraClientUtility fedoraClient,
				Configuration configuration) {
			super(fedoraClient, configuration);
			this.creationTime = System.nanoTime();
		}

	}

	class ViewDMDListResult {
		List<ViewDMDEvent> events = new ArrayList<ViewDMDEvent>();;
		int invalidEvents = 0;
	}
}
