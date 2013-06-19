package eu.scape_project.roda.report;

import info.lc.xmlns.premis_v2.AgentComplexType;
import info.lc.xmlns.premis_v2.EventComplexType;
import info.lc.xmlns.premis_v2.EventIdentifierComplexType;
import info.lc.xmlns.premis_v2.EventOutcomeDetailComplexType;
import info.lc.xmlns.premis_v2.EventOutcomeInformationComplexType;
import info.lc.xmlns.premis_v2.ExtensionComplexType;
import info.lc.xmlns.premis_v2.LinkingObjectIdentifierComplexType;
import info.lc.xmlns.premis_v2.ObjectComplexType;
import info.lc.xmlns.premis_v2.ObjectFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.util.DateParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import pt.gov.dgarq.roda.core.BrowserHelper;
import pt.gov.dgarq.roda.core.data.SimpleEventPreservationObject;

/**
 * @author Rui Castro
 */
public class PlanExecutedEvent extends AbstractEvent {
	private static Logger logger = Logger.getLogger(PlanExecutedEvent.class);

	private SimpleEventPreservationObject simplePremisMigrationEvent;
	private BrowserHelper browserHelper;

	private EventComplexType premisEvent;
	private String premisEventXmlString;

	private String premisAgentXmlString;

	/**
	 * Constructs a new {@link PlanExecutedEvent} with the given migration
	 * preservation event.
	 * 
	 * @param simpleMigrationEvent
	 *            the PREMIS migration event.
	 * @param browserHelper
	 *            the {@link BrowserHelper}.
	 */
	public PlanExecutedEvent(
			SimpleEventPreservationObject simpleMigrationEvent,
			BrowserHelper browserHelper) {
		if (simpleMigrationEvent == null || browserHelper == null) {
			throw new NullPointerException(
					"premisMigrationEvent and browserHelper parameters cannot be null");
		}
		this.simplePremisMigrationEvent = simpleMigrationEvent;
		this.browserHelper = browserHelper;
	}

	@Override
	public String getType() {
		logger.debug("getType()");
		return "PlanExecuted";
	}

	@Override
	public String getIdentifier() {
		logger.debug("getIdentifier()");
		return getType() + ":" + this.simplePremisMigrationEvent.getPid();
	}

	@Override
	public Date getDatetime() {
		logger.debug("getDatetime()");
		return this.simplePremisMigrationEvent.getCreatedDate();
	}

	@Override
	public String getPremisEvent() {
		logger.debug("getPremisEvent()");

		try {

			JAXBElement<EventComplexType> eventElement = new ObjectFactory()
					.createEvent(getPremisPlanExecutedEvent());

			this.premisEventXmlString = EventCatalog
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
		} catch (IOException e) {
			logger.error("Could not marshal PREMIS event element into text - "
					+ e.getMessage(), e);
			this.premisEventXmlString = null;
		}

		return this.premisEventXmlString;
	}

	@Override
	public String getPremisAgent() {
		logger.debug("getPremisAgent()");
		if (this.premisAgentXmlString == null) {

			JAXBElement<AgentComplexType> agentElement = null;

			try {

				final InputStream inputStream = this.browserHelper
						.getFedoraClientUtility().getDatastream(
								this.simplePremisMigrationEvent.getAgentPID(),
								"PREMIS");

				agentElement = EventCatalog.unmarshal(AgentComplexType.class,
						inputStream);
				inputStream.close();

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
		try {

			final EventComplexType planExecutedEvent = getPremisPlanExecutedEvent();

			final List<LinkingObjectIdentifierComplexType> objectIdentifiers = planExecutedEvent
					.getLinkingObjectIdentifier();

			final List<String> relatedObjects = new ArrayList<String>();

			if (objectIdentifiers != null) {

				logger.debug("Found " + objectIdentifiers.size()
						+ " related objects");

				for (LinkingObjectIdentifierComplexType objectIdentifier : objectIdentifiers) {

					if ("Object ID".equalsIgnoreCase(objectIdentifier
							.getLinkingObjectIdentifierType())) {

						final String objectPid = objectIdentifier
								.getLinkingObjectIdentifierValue();

						logger.debug("Found related object " + objectPid);

						logger.debug(String
								.format("Related object: identifierType=%s, identifierValue=%s, role=%s",
										objectIdentifier
												.getLinkingObjectIdentifierType(),
										objectIdentifier
												.getLinkingObjectIdentifierValue(),
										objectIdentifier.getLinkingObjectRole()));

						final InputStream inputStream = this.browserHelper
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

			} else {
				// No linking object identifiers
			}

			return relatedObjects;

		} catch (IOException e) {
			logger.error(
					"Couldn't retrieve related objects information - "
							+ e.getMessage(), e);
			// throw new OAIInternalServerError(
			// "Couldn't retrieve ingest event information - "
			// + e.getMessage());
			return null;
		} catch (JAXBException e) {
			logger.error("Could not marshal PREMIS event element into text - "
					+ e.getMessage(), e);
			return null;
		}
	}

	private EventComplexType getPremisPlanExecutedEvent() throws IOException,
			JAXBException {
		logger.debug("getPremisPlanExecutedEvent()");

		if (this.premisEvent == null) {

			// Get migration event from Fedora
			InputStream inputStream = this.browserHelper
					.getFedoraClientUtility().getDatastream(
							this.simplePremisMigrationEvent.getPid(), "PREMIS");

			EventComplexType migrationEvent = EventCatalog.unmarshal(
					EventComplexType.class, inputStream).getValue();
			inputStream.close();

			this.premisEvent = new EventComplexType();

			// <eventIdentifier>
			this.premisEvent
					.setEventIdentifier(new EventIdentifierComplexType());
			// <eventIdentifierType>
			this.premisEvent.getEventIdentifier().setEventIdentifierType(
					"Event ID");
			// <eventIdentifierValue>
			this.premisEvent.getEventIdentifier().setEventIdentifierValue(
					getIdentifier());

			// <eventType>
			this.premisEvent.setEventType(getType());
			// <eventDateTime>
			this.premisEvent.setEventDateTime(DateParser
					.getIsoDate(getDatetime()));

			// <eventOutcomeInformation>
			this.premisEvent.getEventOutcomeInformation().addAll(
					migrationEvent.getEventOutcomeInformation());

			// debugPlanExecutionDetails(migrationEvent.getEventOutcomeInformation());

			// <linkingAgentIdentifier>
			this.premisEvent.getLinkingAgentIdentifier().addAll(
					migrationEvent.getLinkingAgentIdentifier());

			// <linkingObjectIdentifier>
			LinkingObjectIdentifierComplexType linkingObjectPlanID = new LinkingObjectIdentifierComplexType();
			this.premisEvent.getLinkingObjectIdentifier().add(
					linkingObjectPlanID);
			// <linkingObjectIdentifierType>
			linkingObjectPlanID.setLinkingObjectIdentifierType("Plan ID");
			// <linkingObjectIdentifierValue>
			linkingObjectPlanID
					.setLinkingObjectIdentifierValue(this.simplePremisMigrationEvent
							.getAgentPID());

			// <linkingObjectIdentifier>
			LinkingObjectIdentifierComplexType linkingObjectObjectID = new LinkingObjectIdentifierComplexType();
			this.premisEvent.getLinkingObjectIdentifier().add(
					linkingObjectObjectID);
			// <linkingObjectIdentifierType>
			linkingObjectObjectID.setLinkingObjectIdentifierType("Object ID");
			// <linkingObjectIdentifierValue>
			linkingObjectObjectID
					.setLinkingObjectIdentifierValue(this.simplePremisMigrationEvent
							.getTargetPID());
		}

		return this.premisEvent;
	}

	private void debugPlanExecutionDetails(
			List<EventOutcomeInformationComplexType> eventOutcomeInformation) {
		try {

			if (eventOutcomeInformation != null
					&& eventOutcomeInformation.size() > 0) {

				EventOutcomeInformationComplexType outcomeInfo = eventOutcomeInformation
						.get(0);

				logger.debug("eventOutcomeInformation[0]: " + outcomeInfo);

				String eventOutcome = getEventOutcome(outcomeInfo);

				logger.debug("eventOutcomeInformation[0].eventOutcome: "
						+ eventOutcome);

				EventOutcomeDetailComplexType eventOutcomeDetail = getEventOutcomeDetail(outcomeInfo);

				logger.debug("eventOutcomeInformation[0].eventOutcomeDetail: "
						+ eventOutcomeDetail);

				if (eventOutcomeDetail != null) {

					List<ExtensionComplexType> detailExtensions = eventOutcomeDetail
							.getEventOutcomeDetailExtension();

					if (detailExtensions != null && detailExtensions.size() > 0) {

						List<Object> extension = detailExtensions.get(0)
								.getAny();

						if (extension != null && extension.size() > 0) {

							Object object = extension.get(0);

							logger.debug("eventOutcomeInformation[0].eventOutcomeDetail[0].eventOutcomeDetailExtension[0]: "
									+ object);

							if (object != null && object instanceof Node) {
								Node nodeP = (Node) object;

								if (nodeP.getFirstChild() != null) {

									logger.debug("nodeP.getFirstChild().getTextContent() => "
											+ nodeP.getFirstChild()
													.getTextContent());

									Map<String, String> planExecutionDetailProperties = getPlanExecutionDetailProperties(nodeP
											.getFirstChild().getTextContent());

								} else {
									logger.debug("extension Node first child is null");
								}

							} else {

								logger.debug("eventOutcomeInformation[0].eventOutcomeDetail[0].eventOutcomeDetailExtension[0] is null or isn't a Node");
							}

						} else {
							logger.debug("eventOutcomeInformation[0].eventOutcomeDetail[0].eventOutcomeDetailExtension has no sub-elements");
						}

					} else {
						logger.debug("eventOutcomeInformation[0].eventOutcomeDetail[0].eventOutcomeDetailExtension has no sub-elements");
					}

				}

			} else {
				logger.debug("eventOutcomeInformation has no sub-elements");
			}

		} catch (DOMException e) {
			logger.error(
					"Couldn't parse planExecutionDetails from event outcome details - "
							+ e.getMessage(), e);
		}
	}

	private Map<String, String> getPlanExecutionDetailProperties(
			String planExecutionDetailsText) {

		Map<String, String> properties = new HashMap<String, String>();

		try {

			Document planExecutionDetails = loadXMLFromString(planExecutionDetailsText);

			NodeList elementsQA = planExecutionDetails
					.getElementsByTagName("qa");

			logger.debug("<planExecutionDetails> has " + elementsQA.getLength()
					+ " elements");

			for (int i = 0; i < elementsQA.getLength(); i++) {
				Node elementQA = elementsQA.item(i);
				Node property = elementQA.getAttributes().getNamedItem(
						"property");
				String value = elementQA.getTextContent();

				properties.put(property.getNodeValue(), value);

				logger.debug(String.format("QA property %s=%s",
						property.getNodeValue(), value));
			}

		} catch (SAXException e) {
			logger.error(
					"Couldn't parse planExecutionDetails from event outcome details - "
							+ e.getMessage(), e);
		} catch (DOMException e) {
			logger.error(
					"Couldn't parse planExecutionDetails from event outcome details - "
							+ e.getMessage(), e);
		} catch (ParserConfigurationException e) {
			logger.error(
					"Couldn't parse planExecutionDetails from event outcome details - "
							+ e.getMessage(), e);
		} catch (IOException e) {
			logger.error(
					"Couldn't parse planExecutionDetails from event outcome details - "
							+ e.getMessage(), e);
		}

		return properties;
	}

	private Document loadXMLFromString(String xml)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(xml));
		return builder.parse(is);
	}

	private String getEventOutcome(
			EventOutcomeInformationComplexType outcomeInfo) {
		logger.debug(String.format("getEventOutcome(outcomeInfo=%s)",
				outcomeInfo));

		String outcome = null;

		if (outcomeInfo.getContent() != null) {
			for (JAXBElement<?> content : outcomeInfo.getContent()) {

				if ("eventOutcome".equals(content.getName().getLocalPart())) {
					outcome = (String) content.getValue();
				} else {
					logger.debug("Ignoring content value with name "
							+ content.getName());
				}
			}
		}

		return outcome;
	}

	private EventOutcomeDetailComplexType getEventOutcomeDetail(
			EventOutcomeInformationComplexType outcomeInfo) {
		logger.debug(String.format("getEventOutcomeDetail(outcomeInfo=%s)",
				outcomeInfo));

		EventOutcomeDetailComplexType outcomeDetail = null;

		if (outcomeInfo.getContent() != null) {
			for (JAXBElement<?> content : outcomeInfo.getContent()) {
				if ("eventOutcomeDetail".equals(content.getName()
						.getLocalPart())) {
					outcomeDetail = (EventOutcomeDetailComplexType) content
							.getValue();
				} else {
					logger.debug("Ignoring content value with name "
							+ content.getName());
				}
			}
		}

		return outcomeDetail;
	}

}
