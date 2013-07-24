package eu.scape_project.roda.core.report;

import info.lc.xmlns.premis_v2.AgentComplexType;
import info.lc.xmlns.premis_v2.AgentIdentifierComplexType;
import info.lc.xmlns.premis_v2.EventComplexType;
import info.lc.xmlns.premis_v2.ExtensionComplexType;
import info.lc.xmlns.premis_v2.ObjectFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.util.DateParser;
import org.w3c.util.InvalidDateException;

import pt.gov.dgarq.roda.core.common.RODAServiceException;
import pt.gov.dgarq.roda.core.data.LogEntry;
import pt.gov.dgarq.roda.core.data.LogEntryParameter;
import pt.gov.dgarq.roda.core.data.User;
import pt.gov.dgarq.roda.core.services.UserBrowser;
import eu.scape_project.repository.agentdetails.AgentDetails;
import eu.scape_project.repository.report.AbstractEvent;
import eu.scape_project.repository.report.AbstractEventCatalog;

/**
 * This is the base class for events based on {@link LogEntry}.
 * 
 * @author Rui Castro
 */
public abstract class AbstractLogEntryEvent extends AbstractEvent {

	/**
	 * The logger.
	 */
	private static Logger logger = Logger
			.getLogger(AbstractLogEntryEvent.class);

	private Map<String, User> userCache;

	private LogEntry logEntry = null;

	private boolean lookedForRelatedObject = false;
	private String relatedObjectPID = null;

	private String premisEventXmlString = null;

	/**
	 * Constructs a new {@link AbstractLogEntryEvent} for the specified
	 * {@link LogEntry}.
	 * 
	 * @param logEntry
	 *            the {@link LogEntry}.
	 */
	public AbstractLogEntryEvent(LogEntry logEntry) {
		this.logEntry = logEntry;
		this.userCache = new HashMap<String, User>();
	}

	/**
	 * Returns the inner {@link LogEntry}.
	 * 
	 * @return the inner {@link LogEntry}.
	 */
	public LogEntry getLogEntry() {
		return logEntry;
	}

	@Override
	public String getIdentifier() {
		logger.debug("getIdentifier()");
		return getType() + ":" + this.logEntry.getId();
	}

	@Override
	public Date getDatetime() {
		logger.debug("getDatetime()");
		try {

			return DateParser.parse(this.logEntry.getDatetime());

		} catch (InvalidDateException e) {
			logger.error("Couldn't parse date '" + this.logEntry.getDatetime()
					+ "' - " + e.getMessage(), e);
			return null;
		}
	}

	@Override
	public String getPremisEvent() {
		logger.debug("getPremisEvent()");
		try {

			JAXBElement<EventComplexType> eventElement = new ObjectFactory()
					.createEvent(getPremisEventObject());

			this.premisEventXmlString = AbstractEventCatalog
					.marshalToString(
							eventElement,
							true,
							true,
							"info:lc/xmlns/premis-v2 http://www.loc.gov/standards/premis/v2/premis.xsd",
							EventComplexType.class);

		} catch (JAXBException e) {
			logger.error("Could not marshal PREMIS event element into text - "
					+ e.getMessage(), e);
			this.premisEventXmlString = null;
		}

		return this.premisEventXmlString;
	}

	@Override
	public String getPremisAgent() {
		logger.debug("getPremisAgent()");
		AgentComplexType agent = new AgentComplexType();

		AgentIdentifierComplexType agentIdentifier = new AgentIdentifierComplexType();
		agentIdentifier.setAgentIdentifierType("User ID");
		agentIdentifier.setAgentIdentifierValue(this.logEntry.getUsername());

		agent.getAgentIdentifier().add(agentIdentifier);
		agent.setAgentType("User");

		eu.scape_project.repository.agentdetails.ObjectFactory agentDetailsFactory = new eu.scape_project.repository.agentdetails.ObjectFactory();
		AgentDetails agentDetails = new AgentDetails();

		try {

			User user = this.userCache.get(this.logEntry.getUsername());
			if (user == null) {
				UserBrowser userBrowser = new UserBrowser();
				user = userBrowser.getUser(this.logEntry.getUsername());
				this.userCache.put(this.logEntry.getUsername(), user);
			}

			String[] groups = user.getAllGroups();

			if (groups != null && groups.length > 0) {

				agentDetails.setUser(agentDetailsFactory.createUser());

				for (String group : groups) {
					agentDetails.getUser().getRole().add(group);
				}
			}

			agentDetails.setEndpoint(agentDetailsFactory.createEndpoint());

			// client address
			if (this.logEntry.getParameters() != null) {
				for (LogEntryParameter logParam : this.logEntry.getParameters()) {
					if ("address".equals(logParam.getName())) {
						agentDetails.getEndpoint().setIpHash(
								String.valueOf(logParam.getValue().hashCode()));
					}
				}
			}

			// TODO implement a client for this service http://freegeoip.net/
			// agentDetails.getEndpoint().setGeoIP(agentDetailsFactory.createGeoIP());

			// if (StringUtils.isNotEmpty(user.getCountryName())) {
			// agentDetails.getUser().setLanguage(user.getCountryName());
			// }

		} catch (RODAServiceException e) {
			logger.error("Couln't get user " + this.logEntry.getUsername()
					+ " - " + e.getMessage(), e);
		}

		// TODO for all roles ...
		// agentDetails.getUser().getRole().add(...);
		// agentDetails.getUser().setLanguage(null);

		// agentDetails.getEndpoint().setIpHash(null);
		// agentDetails.getEndpoint().setNetworkHash(null);
		// agentDetails.getEndpoint().setSessionID(null);
		// agentDetails.getEndpoint().getGeoIP().setCountryCode(null);
		// agentDetails.getEndpoint().getGeoIP().setCountryName(null);
		// agentDetails.getEndpoint().getGeoIP().setRegionName(null);
		// agentDetails.getEndpoint().getGeoIP().setCityName(null);
		// agentDetails.getEndpoint().getGeoIP().setZipCode(null);
		// agentDetails.getEndpoint().getGeoIP().setLatitude(null);
		// agentDetails.getEndpoint().getGeoIP().setLongitude(null);
		// agentDetails.getEndpoint().getGeoIP().setTimeZone(null);

		// agentDetails.getUserAgent().setUserAgentID(null);
		// TODO for all languages...
		// agentDetails.getUserAgent().getLanguages().getLanguage().add(null);
		// TODO for all plugins...
		// agentDetails.getUserAgent().getPlugins().getPlugin().add(null);

		// agentDetails.getUserAgent().setOs(null);

		// agentDetails.getUserAgent().getDevice().setScreenWidth(null);
		// agentDetails.getUserAgent().getDevice().setScreenHeight(null);
		// agentDetails.getUserAgent().getDevice().setColorspace(null);

		ExtensionComplexType extension = new ExtensionComplexType();
		extension.getAny().add(
				agentDetailsFactory.createAgentDetails(agentDetails));
		agent.getAgentExtension().add(extension);

		try {

			return AbstractEventCatalog
					.marshalToString(
							new info.lc.xmlns.premis_v2.ObjectFactory()
									.createAgent(agent),
							true,
							true,
							"info:lc/xmlns/premis-v2 http://www.loc.gov/standards/premis/v2/premis.xsd",
							AgentComplexType.class, AgentDetails.class);

		} catch (JAXBException e) {
			logger.error(
					"Could not marshal agent element into text - "
							+ e.getMessage(), e);
			return null;
		}
	}

	protected String getRelatedObjectPID() {
		logger.debug("getRelatedObjectPID()");

		if (!this.lookedForRelatedObject) {

			this.relatedObjectPID = getLogEntry().getRelatedObjectPID();
			logger.debug("Related representation object PID is "
					+ this.relatedObjectPID);

			if (StringUtils.isBlank(this.relatedObjectPID)) {
				// Related object PID is not filled. Let's try to get it from
				// the
				// parameters

				logger.debug("Searching for related representation object PID in LogEntry parameters");

				if (getLogEntry().getParameters() != null) {

					for (LogEntryParameter logParam : getLogEntry()
							.getParameters()) {
						if ("pid".equals(logParam.getName())) {
							this.relatedObjectPID = logParam.getValue();
							logger.debug("Found related representation object PID: "
									+ this.relatedObjectPID);
						}
					}
				} else {
					logger.debug("LogEntry has no parameters.");
				}
			}

			this.lookedForRelatedObject = true;
		}

		return this.relatedObjectPID;
	}

	/**
	 * Returns the event PREMIS object.
	 * 
	 * @return an {@link EventComplexType} with the PREMIS event.
	 */
	protected abstract EventComplexType getPremisEventObject();

}
