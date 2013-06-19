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
import info.lc.xmlns.premis_v2.ObjectComplexType;
import info.lc.xmlns.premis_v2.ObjectFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.w3c.util.DateParser;

import pt.gov.dgarq.roda.core.BrowserHelper;
import pt.gov.dgarq.roda.core.common.BrowserException;
import pt.gov.dgarq.roda.core.common.NoSuchRODAObjectException;
import pt.gov.dgarq.roda.core.data.EventPreservationObject;
import pt.gov.dgarq.roda.core.data.RepresentationObject;
import pt.gov.dgarq.roda.core.data.RepresentationPreservationObject;
import pt.gov.dgarq.roda.core.data.SIPState;
import pt.gov.dgarq.roda.core.data.SIPStateTransition;

public class IngestFinishedEvent extends AbstractEvent {

	/**
	 * The logger.
	 */
	private static Logger logger = Logger.getLogger(IngestFinishedEvent.class);

	private BrowserHelper browser;
	private SIPState sipState;

	private EventComplexType premisIngestEvent = null;
	private String premisEventXmlString = null;
	private String premisAgentXmlString = null;

	private AgentComplexType premisIngestFailedAgent = null;

	/**
	 * Constructs a new {@link IngestFinishedEvent} for the specified
	 * {@link SIPState}.
	 * 
	 * @param sipState
	 *            the {@link SIPState}.
	 * @param browser
	 *            the {@link BrowserHelper}.
	 */
	public IngestFinishedEvent(SIPState sipState, BrowserHelper browser) {
		this.browser = browser;
		this.sipState = sipState;
	}

	public SIPState getSipState() {
		return sipState;
	}

	@Override
	public String getType() {
		return "IngestFinished";
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

		if (this.premisEventXmlString == null) {

			JAXBElement<EventComplexType> eventElement = null;

			if (ingestWasSuccessful()) {

				// Ingest was successful. Get PREMIS event with type
				// 'ingestion'.

				try {

					eventElement = new ObjectFactory()
							.createEvent(getPremisIngestEvent());

				} catch (BrowserException e) {
					logger.error(
							"Couldn't retrieve ingest event information - "
									+ e.getMessage(), e);
					// throw new OAIInternalServerError(
					// "Couldn't retrieve ingest event information - "
					// + e.getMessage());
				} catch (NoSuchRODAObjectException e) {
					logger.error(
							"Couldn't retrieve ingest event information - "
									+ e.getMessage(), e);
					// throw new OAIInternalServerError(
					// "Couldn't retrieve ingest event information - "
					// + e.getMessage());
				} catch (IOException e) {
					logger.error(
							"Couldn't retrieve ingest event information - "
									+ e.getMessage(), e);
					// throw new OAIInternalServerError(
					// "Couldn't retrieve ingest event information - "
					// + e.getMessage());
				} catch (JAXBException e) {
					logger.error(
							"Could not unmarshal PREMIS event from Fedora datastream - "
									+ e.getMessage(), e);
				}

			} else {
				// Ingest failed. Construct a PREMIS event

				eventElement = new ObjectFactory()
						.createEvent(getIngestFailedEvent());
			}

			try {

				this.premisEventXmlString = EventCatalog
						.marshalToString(
								eventElement,
								true,
								true,
								"info:lc/xmlns/premis-v2 http://www.loc.gov/standards/premis/v2/premis.xsd",
								EventComplexType.class);

			} catch (JAXBException e) {
				logger.error(
						"Could not marshal PREMIS event element into text - "
								+ e.getMessage(), e);
				this.premisEventXmlString = null;
			}

		}

		return this.premisEventXmlString;
	}

	@Override
	public String getPremisAgent() {
		logger.debug("getPremisAgent()");

		if (this.premisAgentXmlString == null) {

			JAXBElement<AgentComplexType> agentElement = null;

			if (ingestWasSuccessful()) {

				// Ingest was successful. Get PREMIS agent that is linked to
				// ingest event.

				try {

					final EventComplexType ingestEvent = getPremisIngestEvent();
					final List<LinkingAgentIdentifierComplexType> agentIdentifiers = ingestEvent
							.getLinkingAgentIdentifier();

					if (agentIdentifiers != null && agentIdentifiers.size() > 0) {

						final String agentPid = agentIdentifiers.get(0)
								.getLinkingAgentIdentifierValue();

						final InputStream inputStream = this.browser
								.getFedoraClientUtility().getDatastream(
										agentPid, "PREMIS");

						agentElement = EventCatalog.unmarshal(
								AgentComplexType.class, inputStream);
						inputStream.close();

					} else {
						// no agent
					}

				} catch (BrowserException e) {
					logger.error(
							"Couldn't retrieve agent information - "
									+ e.getMessage(), e);
					// throw new OAIInternalServerError(
					// "Couldn't retrieve ingest event information - "
					// + e.getMessage());
				} catch (NoSuchRODAObjectException e) {
					logger.error(
							"Couldn't retrieve agent information - "
									+ e.getMessage(), e);
					// throw new OAIInternalServerError(
					// "Couldn't retrieve ingest event information - "
					// + e.getMessage());
				} catch (IOException e) {
					logger.error(
							"Couldn't retrieve agent information - "
									+ e.getMessage(), e);
					// throw new OAIInternalServerError(
					// "Couldn't retrieve ingest event information - "
					// + e.getMessage());
				} catch (JAXBException e) {
					logger.error(
							"Could not unmarshal PREMIS agent from Fedora datastream - "
									+ e.getMessage(), e);
				}

			} else {
				// Ingest failed. Construct a PREMIS agent

				agentElement = new ObjectFactory()
						.createAgent(getIngestFailedAgent());
			}

			try {
				this.premisAgentXmlString = EventCatalog
						.marshalToString(
								agentElement,
								true,
								true,
								"info:lc/xmlns/premis-v2 http://www.loc.gov/standards/premis/v2/premis.xsd",
								AgentComplexType.class);
			} catch (JAXBException e) {
				logger.error(
						"Could not marshal PREMIS agent element into text - "
								+ e.getMessage(), e);
				this.premisAgentXmlString = null;
			}

		}

		return this.premisAgentXmlString;

	}

	@Override
	public List<String> getPremisRelatedObjects() {
		logger.debug("getPremisRelatedObjects()");
		if (ingestWasSuccessful()) {

			try {

				final EventComplexType ingestEvent = getPremisIngestEvent();

				final List<LinkingObjectIdentifierComplexType> objectIdentifiers = ingestEvent
						.getLinkingObjectIdentifier();

				final List<String> relatedObjects = new ArrayList<String>();

				if (objectIdentifiers != null) {

					logger.debug("Found " + objectIdentifiers.size()
							+ " related objects");

					for (LinkingObjectIdentifierComplexType objectIdentifier : objectIdentifiers) {

						final String objectPid = objectIdentifier
								.getLinkingObjectIdentifierValue();

						logger.debug(String
								.format("Related object: identifierType=%s, identifierValue=%s, role=%s",
										objectIdentifier
												.getLinkingObjectIdentifierType(),
										objectIdentifier
												.getLinkingObjectIdentifierValue(),
										objectIdentifier.getLinkingObjectRole()));

						final InputStream inputStream = this.browser
								.getFedoraClientUtility().getDatastream(
										objectPid, "PREMIS");

						JAXBElement<ObjectComplexType> objectElement = EventCatalog
								.unmarshal(ObjectComplexType.class, inputStream);

						inputStream.close();

						relatedObjects
								.add(EventCatalog
										.marshalToString(
												objectElement,
												true,
												true,
												"info:lc/xmlns/premis-v2 http://www.loc.gov/standards/premis/v2/premis.xsd",
												ObjectComplexType.class));
					}

				}

				return relatedObjects;

			} catch (BrowserException e) {
				logger.error("Couldn't retrieve related objects information - "
						+ e.getMessage(), e);
				// throw new OAIInternalServerError(
				// "Couldn't retrieve ingest event information - "
				// + e.getMessage());
				return null;
			} catch (NoSuchRODAObjectException e) {
				logger.error("Couldn't retrieve related objects information - "
						+ e.getMessage(), e);
				// throw new OAIInternalServerError(
				// "Couldn't retrieve ingest event information - "
				// + e.getMessage());
				return null;
			} catch (IOException e) {
				logger.error("Couldn't retrieve related objects information - "
						+ e.getMessage(), e);
				// throw new OAIInternalServerError(
				// "Couldn't retrieve ingest event information - "
				// + e.getMessage());
				return null;
			} catch (JAXBException e) {
				logger.error(
						"Could not marshal PREMIS event element into text - "
								+ e.getMessage(), e);
				return null;
			}

		} else {
			return null;
		}
	}

	private EventComplexType getPremisIngestEvent() throws BrowserException,
			NoSuchRODAObjectException, IOException, JAXBException {
		logger.debug("getPremisIngestEvent()");

		if (this.premisIngestEvent == null) {

			logger.debug("SIP " + getSipState().getId() + " contained DO "
					+ getSipState().getIngestedPID());

			RepresentationObject originalRO = this.browser
					.getDOOriginalRepresentation(getSipState().getIngestedPID());

			logger.debug("Original representation for DO "
					+ getSipState().getIngestedPID() + " is "
					+ originalRO.getPid());

			RepresentationPreservationObject rpoOriginal = this.browser
					.getROPreservationObject(originalRO.getPid());

			logger.debug("RepresentationPreservationObject for RO "
					+ originalRO.getPid() + " is " + rpoOriginal.getPid());

			List<EventPreservationObject> events = this.browser
					.getPreservationEventsPerformedOn(rpoOriginal.getPid());

			logger.debug("Found " + events.size() + " events performed on RPO "
					+ rpoOriginal.getPid());

			EventPreservationObject ingestEventPO = null;
			for (EventPreservationObject event : events) {

				logger.debug(String.format(
						"EventPreservationObject %s has type %s",
						event.getPid(), event.getEventType()));

				if (EventPreservationObject.PRESERVATION_EVENT_TYPE_INGESTION
						.equalsIgnoreCase(event.getEventType())) {
					ingestEventPO = event;
					logger.info("EventPreservationObject 'ingestion' found: "
							+ ingestEventPO.getPid());
				}
			}

			if (ingestEventPO == null) {
				throw new NoSuchRODAObjectException("couln't find ingest event");
			}

			InputStream inputStream = this.browser.getFedoraClientUtility()
					.getDatastream(ingestEventPO.getPid(), "PREMIS");

			this.premisIngestEvent = EventCatalog.unmarshal(
					EventComplexType.class, inputStream).getValue();
			inputStream.close();

			// <eventIdentifier>
			this.premisIngestEvent
					.setEventIdentifier(new EventIdentifierComplexType());
			// <eventIdentifierType>
			this.premisIngestEvent.getEventIdentifier().setEventIdentifierType(
					"Event ID");
			// <eventIdentifierValue>
			this.premisIngestEvent.getEventIdentifier()
					.setEventIdentifierValue(getIdentifier());

			// <eventType>
			this.premisIngestEvent.setEventType(getType());

			// <eventDateTime>

			// <eventOutcomeInformation>
			// <eventOutcome>
			// <eventOutcomeDetail>
			// <eventOutcomeDetailExtension>

			// <linkingAgentIdentifier>
			// <linkingAgentIdentifierType>
			// <linkingAgentIdentifierValue>

			// <linkingObjectIdentifier>
			// <linkingObjectIdentifierType>
			// <linkingObjectIdentifierValue>

			// <linkingObjectIdentifier>
			LinkingObjectIdentifierComplexType linkingObjectIdentifier = new LinkingObjectIdentifierComplexType();
			this.premisIngestEvent.getLinkingObjectIdentifier().add(
					linkingObjectIdentifier);
			// <linkingObjectIdentifierType>
			linkingObjectIdentifier.setLinkingObjectIdentifierType("SIP ID");
			// <linkingObjectIdentifierValue>
			linkingObjectIdentifier
					.setLinkingObjectIdentifierValue(getSipState().getId());

		}

		return this.premisIngestEvent;
	}

	private EventComplexType getIngestFailedEvent() {
		logger.debug("getIngestFailedEvent()");

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

		// <eventOutcomeInformation>
		EventOutcomeInformationComplexType outcome = new EventOutcomeInformationComplexType();
		premisEvent.getEventOutcomeInformation().add(outcome);

		SIPStateTransition sipStateTransition = null;
		if (sipState.getStateTransitions().length > 0) {
			sipStateTransition = sipState.getStateTransitions()[sipState
					.getStateTransitions().length - 1];

			// <eventDateTime>
			premisEvent.setEventDateTime(DateParser
					.getIsoDate(sipStateTransition.getDatetime()));

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

		AgentComplexType agent = getIngestFailedAgent();
		AgentIdentifierComplexType agentIdentifier = agent.getAgentIdentifier()
				.get(0);

		// <linkingAgentIdentifier>
		LinkingAgentIdentifierComplexType linkingAgentIdentifier = premisObjFactory
				.createLinkingAgentIdentifierComplexType();
		premisEvent.getLinkingAgentIdentifier().add(linkingAgentIdentifier);
		// <linkingAgentIdentifierType>
		linkingAgentIdentifier.setLinkingAgentIdentifierType(agentIdentifier
				.getAgentIdentifierType());
		// <linkingAgentIdentifierValue>
		linkingAgentIdentifier.setLinkingAgentIdentifierValue(agentIdentifier
				.getAgentIdentifierValue());

		// <linkingObjectIdentifier>
		LinkingObjectIdentifierComplexType linkingObjectIdentifier = new LinkingObjectIdentifierComplexType();
		premisEvent.getLinkingObjectIdentifier().add(linkingObjectIdentifier);
		// <linkingObjectIdentifierType>
		linkingObjectIdentifier.setLinkingObjectIdentifierType("SIP ID");
		// <linkingObjectIdentifierValue>
		linkingObjectIdentifier.setLinkingObjectIdentifierValue(getSipState()
				.getId());

		return premisEvent;
	}

	private AgentComplexType getIngestFailedAgent() {
		logger.debug("getIngestFailedAgent()");

		if (this.premisIngestFailedAgent == null) {

			SIPStateTransition[] stateTransitions = getSipState()
					.getStateTransitions();

			if (stateTransitions != null && stateTransitions.length > 0) {

				SIPStateTransition transition = stateTransitions[stateTransitions.length - 1];

				this.premisIngestFailedAgent = new AgentComplexType();

				AgentIdentifierComplexType agentIdentifier = new AgentIdentifierComplexType();
				agentIdentifier.setAgentIdentifierType("Ingest Task ID");
				agentIdentifier.setAgentIdentifierValue(transition.getTaskID());

				this.premisIngestFailedAgent.getAgentIdentifier().add(
						agentIdentifier);
				this.premisIngestFailedAgent
						.setAgentType("software:ingest_task");
			}
		}

		return this.premisIngestFailedAgent;
	}

	private boolean ingestWasSuccessful() {
		logger.debug("ingestWasSuccessful()");
		return !"QUARANTINE".equals(getSipState().getState());
	}

}
