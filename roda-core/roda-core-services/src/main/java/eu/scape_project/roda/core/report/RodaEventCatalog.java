package eu.scape_project.roda.core.report;

import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
import org.apache.log4j.Logger;

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
import ORG.oclc.oai.server.verb.IdDoesNotExistException;
import ORG.oclc.oai.server.verb.OAIInternalServerError;
import eu.scape_project.repository.report.AbstractEventCatalog;
import eu.scape_project.repository.report.Event;
import eu.scape_project.repository.report.EventCatalogException;

/**
 * The RODA Report API Catalog.
 * 
 * {@link AbstractCatalog} implementation.
 * 
 * @author Rui Castro
 * @see AbstractCatalog
 */
@SuppressWarnings("rawtypes")
public class RodaEventCatalog extends AbstractEventCatalog {

	/**
	 * The logger.
	 */
	private static Logger logger = Logger.getLogger(RodaEventCatalog.class);

	private ServletContext context = null;

	private Configuration configuration = null;

	private Map<String, BrowserHelperCache> userBrowsers = null;

	private IngestManager ingestManager = null;

	/**
	 * Maximum browser cache duration in nanoseconds. Default value is 5 minutes
	 * (3e+11 nanoseconds).
	 */
	private long cacheMaxDuration = (long) Math.pow(3, 11);

	private Properties properties;

	private LoggerManager loggerManager = null;

	/**
	 * Constructs a new {@link RodaEventCatalog}.
	 * 
	 * @param properties
	 *            the properties
	 * @param context
	 *            the {@link ServletContext}
	 * @throws OAIInternalServerError
	 */
	public RodaEventCatalog(Properties properties, ServletContext context)
			throws OAIInternalServerError {
		super(properties, context);

		logger.debug("RodaReportCatalog(properties=" + properties + ", context"
				+ context + ")");

		this.properties = properties;
		this.context = context;

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

	public void close() {
		logger.debug("close()");
		ingestManager = null;
	}

	protected Event getIngestStartedEvent(String innerID)
			throws IdDoesNotExistException, EventCatalogException {
		logger.debug(String
				.format("getIngestStartedEvent(innerID=%s)", innerID));

		try {

			SIPState sipState = this.ingestManager.getSIP(innerID);
			return new IngestStartedEvent(sipState);

		} catch (NoSuchSIPException e) {
			logger.debug("Error retrieving SIP '" + innerID + "' - "
					+ e.getMessage());
			throw new IdDoesNotExistException(innerID);
		} catch (IngestRegistryException e) {
			logger.debug("Error retrieving SIP '" + innerID + "' - "
					+ e.getMessage());
			throw new EventCatalogException("Error retrieving SIP '" + innerID
					+ "' - " + e.getMessage());
		}
	}

	protected int getIngestStartedEventCount(String from, String until,
			String metadataPrefix) throws EventCatalogException {

		logger.debug(String
				.format("getIngestStartedEventCount(from=%s, until=%s, metadataPrefix=%s)",
						from, until, metadataPrefix));

		Filter filter = null;
		if (StringUtils.isNotBlank(from) || StringUtils.isNotBlank(until)) {

			FilterParameter filterParamFromUntil = new RangeFilterParameter(
					"datetime", from, until);
			filter = new Filter(filterParamFromUntil);
		}

		try {

			return this.ingestManager.getSIPsCount(filter);

		} catch (IngestRegistryException e) {
			logger.debug("Error retrieving SIP count - " + e.getMessage());
			throw new EventCatalogException("Error retrieving SIP count - "
					+ e.getMessage());
		}
	}

	protected List<Event> getIngestStartedEvents(String from, String until,
			String metadataPrefix, int startIndex, int maxResults)
			throws EventCatalogException {

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

		try {

			List<SIPState> sips = this.ingestManager.getSIPs(cAdapter);

			List<Event> events = new ArrayList<Event>(sips.size());
			for (SIPState sip : sips) {
				events.add(new IngestStartedEvent(sip));
			}

			return events;

		} catch (IngestRegistryException e) {
			logger.debug("Error retrieving SIPs - " + e.getMessage());
			throw new EventCatalogException("Error retrieving SIPs - "
					+ e.getMessage());
		}
	}

	protected Event getIngestFinishedEvent(String innerID)
			throws IdDoesNotExistException, EventCatalogException {
		logger.debug(String.format("getIngestFinishedEvent(innerID=%s)",
				innerID));

		try {

			SIPState sipState = this.ingestManager.getSIP(innerID);

			if (sipState.isComplete()) {
				return new IngestFinishedEvent(sipState, getBrowserHelper());
			} else {
				throw new IdDoesNotExistException(innerID);
			}

		} catch (NoSuchSIPException e) {
			logger.debug("Error retrieving Event for SIP '" + innerID + "' - "
					+ e.getMessage());
			throw new IdDoesNotExistException(innerID);
		} catch (IngestRegistryException e) {
			logger.debug("Error retrieving Event for SIP '" + innerID + "' - "
					+ e.getMessage());
			throw new EventCatalogException("Error retrieving Event for SIP '"
					+ innerID + "' - " + e.getMessage());
		} catch (FedoraClientException e) {
			logger.debug("Error retrieving Event for SIP '" + innerID + "' - "
					+ e.getMessage());
			throw new EventCatalogException("Error retrieving Event for SIP '"
					+ innerID + "' - " + e.getMessage());
		} catch (MalformedURLException e) {
			logger.debug("Error retrieving Event for SIP '" + innerID + "' - "
					+ e.getMessage());
			throw new EventCatalogException("Error retrieving Event for SIP '"
					+ innerID + "' - " + e.getMessage());
		}
	}

	protected int getIngestFinishedEventCount(String from, String until,
			String metadataPrefix) throws EventCatalogException {

		logger.debug(String
				.format("getIngestFinishedEventCount(from=%s, until=%s, metadataPrefix=%s)",
						from, until, metadataPrefix));

		FilterParameter filterParamFromUntil = new RangeFilterParameter(
				"datetime", from, until);
		FilterParameter filterParamCompleted = new SimpleFilterParameter(
				"completed", "true");
		Filter filter = new Filter(new FilterParameter[] {
				filterParamFromUntil, filterParamCompleted });

		try {

			return this.ingestManager.getSIPsCount(filter);

		} catch (IngestRegistryException e) {
			logger.debug("Error retrieving SIP count - " + e.getMessage());
			throw new EventCatalogException("Error retrieving SIP count - "
					+ e.getMessage());
		}
	}

	protected List<Event> getIngestFinishedEvents(String from, String until,
			String metadataPrefix, int startIndex, int maxResults)
			throws EventCatalogException {

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
		try {
			List<SIPState> sips = this.ingestManager.getSIPs(cAdapter);

			List<Event> events = new ArrayList<Event>(sips.size());
			for (SIPState sip : sips) {
				events.add(new IngestFinishedEvent(sip, getBrowserHelper()));
			}

			return events;

		} catch (FedoraClientException e) {
			logger.debug("Error retrieving SIPs - " + e.getMessage());
			throw new EventCatalogException("Error retrieving SIPs - "
					+ e.getMessage());
		} catch (MalformedURLException e) {
			logger.debug("Error retrieving SIPs - " + e.getMessage());
			throw new EventCatalogException("Error retrieving SIPs - "
					+ e.getMessage());
		} catch (IngestRegistryException e) {
			logger.debug("Error retrieving SIPs - " + e.getMessage());
			throw new EventCatalogException("Error retrieving SIPs - "
					+ e.getMessage());
		}

	}

	protected int getViewDMDEventCount(String from, String until,
			String metadataPrefix) throws EventCatalogException {

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

		try {

			return getLoggerManager().getLogEntriesCount(filter);

		} catch (LoggerException e) {
			logger.debug("Error retrieving LogEntries count - "
					+ e.getMessage());
			throw new EventCatalogException(
					"Error retrieving LogEntries count - " + e.getMessage());
		}
	}

	protected Event getViewDMDEvent(String innerID)
			throws IdDoesNotExistException, EventCatalogException {
		logger.debug(String.format("getViewDMDEvent(innerID=%s)", innerID));

		try {

			return new ViewDMDEvent(getLogEntry(innerID), getBrowserHelper());

		} catch (NoSuchRODAObjectException e) {
			logger.debug("Error retrieving Event for LogEntry '" + innerID
					+ "' - " + e.getMessage());
			throw new IdDoesNotExistException(innerID);
		} catch (LoggerException e) {
			logger.debug("Error retrieving Event for LogEntry '" + innerID
					+ "' - " + e.getMessage());
			throw new EventCatalogException(
					"Error retrieving Event for LogEntry '" + innerID + "' - "
							+ e.getMessage());
		} catch (FedoraClientException e) {
			logger.debug("Error retrieving Event for LogEntry '" + innerID
					+ "' - " + e.getMessage());
			throw new EventCatalogException(
					"Error retrieving Event for LogEntry '" + innerID + "' - "
							+ e.getMessage());
		} catch (MalformedURLException e) {
			logger.debug("Error retrieving Event for LogEntry '" + innerID
					+ "' - " + e.getMessage());
			throw new EventCatalogException(
					"Error retrieving Event for LogEntry '" + innerID + "' - "
							+ e.getMessage());
		}
	}

	protected List<Event> getViewDMDEvents(String from, String until,
			String metadataPrefix, int startIndex, int maxResults)
			throws EventCatalogException {

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

		try {

			List<LogEntry> logEntries = getLoggerManager().getLogEntries(
					cAdapter);

			List<Event> events = new ArrayList<Event>();
			for (LogEntry logEntry : logEntries) {
				events.add(new ViewDMDEvent(logEntry, getBrowserHelper()));
			}

			return events;

		} catch (FedoraClientException e) {
			logger.debug("Error retrieving LogEntries - " + e.getMessage());
			throw new EventCatalogException("Error retrieving LogEntries - "
					+ e.getMessage());
		} catch (MalformedURLException e) {
			logger.debug("Error retrieving LogEntries - " + e.getMessage());
			throw new EventCatalogException("Error retrieving LogEntries - "
					+ e.getMessage());
		} catch (LoggerException e) {
			logger.debug("Error retrieving LogEntries - " + e.getMessage());
			throw new EventCatalogException("Error retrieving LogEntries - "
					+ e.getMessage());
		}
	}

	protected int getViewRepresentationEventCount(String from, String until,
			String metadataPrefix) throws EventCatalogException {

		logger.debug(String
				.format("getViewRepresentationEventCount(from=%s, until=%s, metadataPrefix=%s)",
						from, until, metadataPrefix));

		// TODO this can be improved by filtering system users:
		// roda-ingest-task, roda-preservation-task, etc
		Filter filter = new Filter();
		filter.add(new RangeFilterParameter("datetime", from, until));
		filter.add(new LikeFilterParameter("action", "disseminator.hit.%"));

		try {

			return getLoggerManager().getLogEntriesCount(filter);

		} catch (LoggerException e) {
			logger.debug("Error retrieving LogEntries - " + e.getMessage());
			throw new EventCatalogException("Error retrieving LogEntries - "
					+ e.getMessage());
		}
	}

	protected Event getViewRepresentationEvent(String innerID)
			throws IdDoesNotExistException, EventCatalogException {
		logger.debug(String.format("getViewRepresentationEvent(innerID=%s)",
				innerID));
		try {

			return new ViewRepresentationEvent(getLogEntry(innerID),
					getBrowserHelper());

		} catch (NoSuchRODAObjectException e) {
			logger.debug("Error retrieving Event for LogEntry '" + innerID
					+ "' - " + e.getMessage());
			throw new IdDoesNotExistException(innerID);
		} catch (LoggerException e) {
			logger.debug("Error retrieving Event for LogEntry '" + innerID
					+ "' - " + e.getMessage());
			throw new EventCatalogException(
					"Error retrieving Event for LogEntry '" + innerID + "' - "
							+ e.getMessage());
		} catch (FedoraClientException e) {
			logger.debug("Error retrieving Event for LogEntry '" + innerID
					+ "' - " + e.getMessage());
			throw new EventCatalogException(
					"Error retrieving Event for LogEntry '" + innerID + "' - "
							+ e.getMessage());
		} catch (MalformedURLException e) {
			logger.debug("Error retrieving Event for LogEntry '" + innerID
					+ "' - " + e.getMessage());
			throw new EventCatalogException(
					"Error retrieving Event for LogEntry '" + innerID + "' - "
							+ e.getMessage());
		}
	}

	protected List<Event> getViewRepresentationEvents(String from,
			String until, String metadataPrefix, int startIndex, int maxResults)
			throws EventCatalogException {

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

		try {

			List<LogEntry> logEntries = getLoggerManager().getLogEntries(
					cAdapter);

			List<Event> events = new ArrayList<Event>();
			for (LogEntry logEntry : logEntries) {
				events.add(new ViewRepresentationEvent(logEntry,
						getBrowserHelper()));
			}

			return events;

		} catch (FedoraClientException e) {
			logger.debug("Error retrieving LogEntries - " + e.getMessage());
			throw new EventCatalogException("Error retrieving LogEntries - "
					+ e.getMessage());
		} catch (MalformedURLException e) {
			logger.debug("Error retrieving LogEntries - " + e.getMessage());
			throw new EventCatalogException("Error retrieving LogEntries - "
					+ e.getMessage());
		} catch (LoggerException e) {
			logger.debug("Error retrieving LogEntries - " + e.getMessage());
			throw new EventCatalogException("Error retrieving LogEntries - "
					+ e.getMessage());
		}
	}

	protected int getDownloadRepresentationEventCount(String from,
			String until, String metadataPrefix) throws EventCatalogException {

		logger.debug(String
				.format("getDownloadRepresentationEventCount(from=%s, until=%s, metadataPrefix=%s)",
						from, until, metadataPrefix));

		// TODO this can be improved by filtering system users:
		// roda-ingest-task, roda-preservation-task, etc
		Filter filter = new Filter();
		filter.add(new RangeFilterParameter("datetime", from, until));
		filter.add(new SimpleFilterParameter("action",
				"disseminator.hit.AIPDownload"));

		try {

			return getLoggerManager().getLogEntriesCount(filter);

		} catch (LoggerException e) {
			logger.debug("Error retrieving LogEntries - " + e.getMessage());
			throw new EventCatalogException("Error retrieving LogEntries - "
					+ e.getMessage());
		}
	}

	protected Event getDownloadRepresentationEvent(String innerID)
			throws IdDoesNotExistException, EventCatalogException {
		logger.debug(String.format(
				"getDownloadRepresentationEvent(innerID=%s)", innerID));
		try {

			return new DownloadRepresentationEvent(getLogEntry(innerID),
					getBrowserHelper());

		} catch (NoSuchRODAObjectException e) {
			logger.debug("Error retrieving Event for LogEntry '" + innerID
					+ "' - " + e.getMessage());
			throw new IdDoesNotExistException(innerID);
		} catch (LoggerException e) {
			logger.debug("Error retrieving Event for LogEntry '" + innerID
					+ "' - " + e.getMessage());
			throw new EventCatalogException(
					"Error retrieving Event for LogEntry '" + innerID + "' - "
							+ e.getMessage());
		} catch (FedoraClientException e) {
			logger.debug("Error retrieving Event for LogEntry '" + innerID
					+ "' - " + e.getMessage());
			throw new EventCatalogException(
					"Error retrieving Event for LogEntry '" + innerID + "' - "
							+ e.getMessage());
		} catch (MalformedURLException e) {
			logger.debug("Error retrieving Event for LogEntry '" + innerID
					+ "' - " + e.getMessage());
			throw new EventCatalogException(
					"Error retrieving Event for LogEntry '" + innerID + "' - "
							+ e.getMessage());
		}
	}

	protected List<Event> getDownloadRepresentationEvents(String from,
			String until, String metadataPrefix, int startIndex, int maxResults)
			throws EventCatalogException {

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

		try {

			List<LogEntry> logEntries = getLoggerManager().getLogEntries(
					cAdapter);

			List<Event> events = new ArrayList<Event>();
			for (LogEntry logEntry : logEntries) {
				events.add(new DownloadRepresentationEvent(logEntry,
						getBrowserHelper()));
			}

			return events;

		} catch (FedoraClientException e) {
			logger.debug("Error retrieving LogEntries - " + e.getMessage());
			throw new EventCatalogException("Error retrieving LogEntries - "
					+ e.getMessage());
		} catch (MalformedURLException e) {
			logger.debug("Error retrieving LogEntries - " + e.getMessage());
			throw new EventCatalogException("Error retrieving LogEntries - "
					+ e.getMessage());
		} catch (LoggerException e) {
			logger.debug("Error retrieving LogEntries - " + e.getMessage());
			throw new EventCatalogException("Error retrieving LogEntries - "
					+ e.getMessage());
		}
	}

	protected Event getPlanExecutedEvent(String innerID)
			throws IdDoesNotExistException, EventCatalogException {
		logger.debug(String.format("getPlanExecutedEvent(innerID=%s)", innerID));

		try {

			SimpleEventPreservationObject simpleEPO = getBrowserHelper()
					.getSimpleEventPreservationObject(innerID);
			return new PlanExecutedEvent(simpleEPO, getBrowserHelper());

		} catch (NoSuchRODAObjectException e) {
			logger.debug("Error retrieving Event for SimpleEventPreservationObject '"
					+ innerID + "' - " + e.getMessage());
			throw new IdDoesNotExistException(innerID);
		} catch (FedoraClientException e) {
			logger.debug("Error retrieving Event for SimpleEventPreservationObject '"
					+ innerID + "' - " + e.getMessage());
			throw new EventCatalogException(
					"Error retrieving Event for SimpleEventPreservationObject '"
							+ innerID + "' - " + e.getMessage());
		} catch (MalformedURLException e) {
			logger.debug("Error retrieving Event for SimpleEventPreservationObject '"
					+ innerID + "' - " + e.getMessage());
			throw new EventCatalogException(
					"Error retrieving Event for SimpleEventPreservationObject '"
							+ innerID + "' - " + e.getMessage());
		} catch (BrowserException e) {
			logger.debug("Error retrieving Event for SimpleEventPreservationObject '"
					+ innerID + "' - " + e.getMessage());
			throw new EventCatalogException(
					"Error retrieving Event for SimpleEventPreservationObject '"
							+ innerID + "' - " + e.getMessage());
		}
	}

	protected int getPlanExecutedEventCount(String from, String until,
			String metadataPrefix) throws EventCatalogException {

		logger.debug(String
				.format("getPlanExecutedEventCount(from=%s, until=%s, metadataPrefix=%s",
						from, until, metadataPrefix));

		FilterParameter filterParamFromUntil = new RangeFilterParameter(
				"datetime", from, until);
		FilterParameter filterParamLabelMigration = new SimpleFilterParameter(
				"label", "migration");
		Filter filter = new Filter(new FilterParameter[] {
				filterParamFromUntil, filterParamLabelMigration });

		try {

			return getBrowserHelper().getSimpleEventPreservationObjectCount(
					filter);

		} catch (BrowserException e) {
			logger.debug("Error retrieving SimpleEventPreservationObjects - "
					+ e.getMessage());
			throw new EventCatalogException(
					"Error retrieving SimpleEventPreservationObjects - "
							+ e.getMessage());
		} catch (FedoraClientException e) {
			logger.debug("Error retrieving SimpleEventPreservationObjects - "
					+ e.getMessage());
			throw new EventCatalogException(
					"Error retrieving SimpleEventPreservationObjects - "
							+ e.getMessage());
		} catch (MalformedURLException e) {
			logger.debug("Error retrieving SimpleEventPreservationObjects - "
					+ e.getMessage());
			throw new EventCatalogException(
					"Error retrieving SimpleEventPreservationObjects - "
							+ e.getMessage());
		}
	}

	protected List<Event> getPlanExecutedEvents(String from, String until,
			String metadataPrefix, int startIndex, int maxResults)
			throws EventCatalogException {

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
		try {

			List<SimpleEventPreservationObject> premisMigrationEvents = getBrowserHelper()
					.getSimpleEventPreservationObjects(cAdapter);

			List<Event> events = new ArrayList<Event>(
					premisMigrationEvents.size());
			for (SimpleEventPreservationObject migrationEvent : premisMigrationEvents) {
				events.add(new PlanExecutedEvent(migrationEvent,
						getBrowserHelper()));
			}

			return events;

		} catch (FedoraClientException e) {
			logger.debug("Error retrieving SimpleEventPreservationObjects - "
					+ e.getMessage());
			throw new EventCatalogException(
					"Error retrieving SimpleEventPreservationObjects - "
							+ e.getMessage());
		} catch (MalformedURLException e) {
			logger.debug("Error retrieving SimpleEventPreservationObjects - "
					+ e.getMessage());
			throw new EventCatalogException(
					"Error retrieving SimpleEventPreservationObjects - "
							+ e.getMessage());
		} catch (BrowserException e) {
			logger.debug("Error retrieving SimpleEventPreservationObjects - "
					+ e.getMessage());
			throw new EventCatalogException(
					"Error retrieving SimpleEventPreservationObjects - "
							+ e.getMessage());
		}
	}

	protected LogEntry getLogEntry(String logEntryID) throws LoggerException,
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

	protected BrowserHelper getBrowserHelper() throws FedoraClientException,
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
