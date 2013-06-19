package eu.scape_project.roda.report;

import info.lc.xmlns.premis_v2.AgentComplexType;
import info.lc.xmlns.premis_v2.AgentIdentifierComplexType;
import info.lc.xmlns.premis_v2.EventComplexType;
import info.lc.xmlns.premis_v2.EventIdentifierComplexType;
import info.lc.xmlns.premis_v2.EventOutcomeDetailComplexType;
import info.lc.xmlns.premis_v2.EventOutcomeInformationComplexType;
import info.lc.xmlns.premis_v2.ExtensionComplexType;
import info.lc.xmlns.premis_v2.LinkingAgentIdentifierComplexType;
import info.lc.xmlns.premis_v2.LinkingObjectIdentifierComplexType;
import info.lc.xmlns.premis_v2.ObjectFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.w3c.util.DateParser;

import pt.gov.dgarq.roda.core.common.RODAServiceException;
import pt.gov.dgarq.roda.core.data.SIPState;
import pt.gov.dgarq.roda.core.data.SIPStateTransition;
import pt.gov.dgarq.roda.core.data.User;
import pt.gov.dgarq.roda.core.services.UserBrowser;
import eu.scape_project.repository.agentdetails.AgentDetails;

public class IngestStartedEvent extends AbstractEvent {

	/**
	 * The logger.
	 */
	private static Logger logger = Logger.getLogger(IngestStartedEvent.class);

	private SIPState sipState;
	private Map<String, User> userCache;

	/**
	 * Constructs a new {@link IngestStartedEvent} for the specified
	 * {@link SIPState}.
	 * 
	 * @param sipState
	 *            the {@link SIPState}.
	 */
	public IngestStartedEvent(SIPState sipState) {
		this.sipState = sipState;
		this.userCache = new HashMap<String, User>();
	}

	public SIPState getSipState() {
		return sipState;
	}

	@Override
	public String getType() {
		logger.debug("getType()");
		return "IngestStarted";
	}

	@Override
	public String getIdentifier() {
		logger.debug("getIdentifier()");
		return getType() + ":" + sipState.getId();
	}

	@Override
	public Date getDatetime() {
		logger.debug("getDatetime()");
		return sipState.getDatetime();
	}

	@Override
	public String getPremisEvent() {
		logger.debug("getPremisEvent()");

		ObjectFactory premisObjFactory = new ObjectFactory();

		EventComplexType premisEvent = new EventComplexType();

		// <eventIdentifier>
		premisEvent.setEventIdentifier(new EventIdentifierComplexType());
		// <eventIdentifierType>
		premisEvent.getEventIdentifier().setEventIdentifierType("Event ID");
		// <eventIdentifierValue>
		premisEvent.getEventIdentifier().setEventIdentifierValue(
				getIdentifier());

		// <eventType>
		premisEvent.setEventType(getType());
		// <eventDateTime>
		premisEvent.setEventDateTime(DateParser.getIsoDate(getDatetime()));

		// <eventOutcomeInformation>
		EventOutcomeInformationComplexType outcome = new EventOutcomeInformationComplexType();
		premisEvent.getEventOutcomeInformation().add(outcome);

		SIPStateTransition sipStateTransition = null;
		if (sipState.getStateTransitions().length > 0) {
			sipStateTransition = sipState.getStateTransitions()[0];

			// <eventOutcome>
			outcome.getContent().add(
					premisObjFactory.createEventOutcome(sipStateTransition
							.isSuccess() ? "success" : "failure"));

			// <eventOutcomeDetail>
			EventOutcomeDetailComplexType outcomeDetail = premisObjFactory
					.createEventOutcomeDetailComplexType();
			outcome.getContent().add(
					premisObjFactory.createEventOutcomeDetail(outcomeDetail));

			ExtensionComplexType outcomeDetailExtension = premisObjFactory
					.createExtensionComplexType();
			outcomeDetailExtension.getAny().add(
					new JAXBElement<String>(new QName(
							"http://www.w3.org/1999/xhtml", "p"), String.class,
							sipStateTransition.getDescription()));

			// <eventOutcomeDetailExtension>
			outcomeDetail.getEventOutcomeDetailExtension().add(
					outcomeDetailExtension);

		} else {

			outcome.getContent().add(
					premisObjFactory.createEventOutcome("failure"));

		}

		// <linkingAgentIdentifier>
		LinkingAgentIdentifierComplexType linkingAgentIdentifier = premisObjFactory
				.createLinkingAgentIdentifierComplexType();
		premisEvent.getLinkingAgentIdentifier().add(linkingAgentIdentifier);
		// <linkingAgentIdentifierType>
		linkingAgentIdentifier.setLinkingAgentIdentifierType("User ID");
		// <linkingAgentIdentifierValue>
		linkingAgentIdentifier.setLinkingAgentIdentifierValue(getSipState()
				.getUsername());

		// <linkingObjectIdentifier>
		LinkingObjectIdentifierComplexType linkingObjectIdentifier = new LinkingObjectIdentifierComplexType();
		premisEvent.getLinkingObjectIdentifier().add(linkingObjectIdentifier);
		// <linkingObjectIdentifierType>
		linkingObjectIdentifier.setLinkingObjectIdentifierType("SIP ID");
		// <linkingObjectIdentifierValue>
		linkingObjectIdentifier.setLinkingObjectIdentifierValue(getSipState()
				.getId());

		try {

			return EventCatalog
					.marshalToString(
							premisObjFactory.createEvent(premisEvent),
							true,
							true,
							"info:lc/xmlns/premis-v2 http://www.loc.gov/standards/premis/v2/premis.xsd",
							EventComplexType.class);

		} catch (JAXBException e) {
			logger.error("Could not marshal PREMIS event element into text - "
					+ e.getMessage(), e);
			return null;
		}
	}

	@Override
	public String getPremisAgent() {
		logger.debug("getPremisAgent()");
		AgentComplexType agent = new AgentComplexType();

		AgentIdentifierComplexType agentIdentifier = new AgentIdentifierComplexType();
		agentIdentifier.setAgentIdentifierType("User ID");
		agentIdentifier.setAgentIdentifierValue(this.getSipState()
				.getUsername());

		agent.getAgentIdentifier().add(agentIdentifier);
		agent.setAgentType("User");

		AgentDetails agentDetails = new AgentDetails();

		try {

			User user = this.userCache.get(sipState.getUsername());
			if (user == null) {
				UserBrowser userBrowser = new UserBrowser();
				user = userBrowser.getUser(sipState.getUsername());
				this.userCache.put(sipState.getUsername(), user);
			}

			String[] groups = user.getAllGroups();

			if (groups != null && groups.length > 0) {

				agentDetails
						.setUser(new eu.scape_project.repository.agentdetails.User());

				for (String group : groups) {
					agentDetails.getUser().getRole().add(group);
				}
			}

			// if (StringUtils.isNotEmpty(user.getCountryName())) {
			// agentDetails.getUser().setLanguage(user.getCountryName());
			// }

		} catch (RODAServiceException e) {
			logger.error("Couln't get user " + sipState.getUsername() + " - "
					+ e.getMessage(), e);
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
		eu.scape_project.repository.agentdetails.ObjectFactory agentDetailsFactory = new eu.scape_project.repository.agentdetails.ObjectFactory();
		extension.getAny().add(
				agentDetailsFactory.createAgentDetails(agentDetails));
		agent.getAgentExtension().add(extension);

		try {

			return EventCatalog
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

	@Override
	public List<String> getPremisRelatedObjects() {
		logger.debug("getPremisRelatedObjects()");
		return null;
	}
}
