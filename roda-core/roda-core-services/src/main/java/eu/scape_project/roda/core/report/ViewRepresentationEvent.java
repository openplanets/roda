package eu.scape_project.roda.core.report;

import info.lc.xmlns.premis_v2.EventComplexType;
import info.lc.xmlns.premis_v2.EventIdentifierComplexType;
import info.lc.xmlns.premis_v2.EventOutcomeInformationComplexType;
import info.lc.xmlns.premis_v2.LinkingAgentIdentifierComplexType;
import info.lc.xmlns.premis_v2.LinkingObjectIdentifierComplexType;
import info.lc.xmlns.premis_v2.ObjectComplexType;
import info.lc.xmlns.premis_v2.ObjectFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.w3c.util.DateParser;

import pt.gov.dgarq.roda.core.BrowserHelper;
import pt.gov.dgarq.roda.core.common.BrowserException;
import pt.gov.dgarq.roda.core.common.NoSuchRODAObjectException;
import pt.gov.dgarq.roda.core.data.LogEntry;
import pt.gov.dgarq.roda.core.data.RepresentationPreservationObject;
import eu.scape_project.repository.report.AbstractEventCatalog;

/**
 * @author Rui Castro
 */
public class ViewRepresentationEvent extends AbstractLogEntryEvent {

	/**
	 * The logger.
	 */
	private static Logger logger = Logger
			.getLogger(ViewRepresentationEvent.class);

	private BrowserHelper browser = null;
	private EventComplexType premisEvent = null;

	/**
	 * Constructs a new {@link ViewRepresentationEvent} for the specified
	 * {@link LogEntry}.
	 * 
	 * @param logEntry
	 * @param browserHelper
	 */
	public ViewRepresentationEvent(LogEntry logEntry,
			BrowserHelper browserHelper) {
		super(logEntry);
		this.browser = browserHelper;
	}

	@Override
	public String getType() {
		logger.debug("getType()");
		return "ViewRepresentation";
	}

	@Override
	public List<String> getPremisRelatedObjects() {
		logger.debug("getPremisRelatedObjects()");
		final List<String> relatedObjects = new ArrayList<String>();

		if (getRelatedObjectPID() != null) {
			try {

				RepresentationPreservationObject rpo = this.browser
						.getROPreservationObject(getRelatedObjectPID());

				final InputStream inputStream = this.browser
						.getFedoraClientUtility().getDatastream(rpo.getPid(),
								"PREMIS");

				JAXBElement<ObjectComplexType> objectElement = AbstractEventCatalog
						.unmarshal(ObjectComplexType.class, inputStream);

				inputStream.close();

				relatedObjects
						.add(AbstractEventCatalog
								.marshalToString(
										objectElement,
										true,
										true,
										"info:lc/xmlns/premis-v2 http://www.loc.gov/standards/premis/v2/premis.xsd",
										ObjectComplexType.class));

			} catch (JAXBException e) {
				logger.error("Couldn't retrieve related objects information - "
						+ e.getMessage(), e);
			} catch (IOException e) {
				logger.error("Couldn't retrieve related objects information - "
						+ e.getMessage(), e);
			} catch (BrowserException e) {
				logger.error("Couldn't retrieve related objects information - "
						+ e.getMessage(), e);
			} catch (NoSuchRODAObjectException e) {
				logger.error("Couldn't retrieve related objects information - "
						+ e.getMessage(), e);
			}
		} else {
			logger.info("No PREMIS related objects because RO PID is null");
		}

		return relatedObjects;
	}

	@Override
	protected EventComplexType getPremisEventObject() {
		logger.debug("getPremisEventObject()");

		if (this.premisEvent == null) {

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
			EventOutcomeInformationComplexType outcome = new EventOutcomeInformationComplexType();
			this.premisEvent.getEventOutcomeInformation().add(outcome);

			// <eventOutcome>
			ObjectFactory premisObjFactory = new ObjectFactory();
			outcome.getContent().add(
					premisObjFactory.createEventOutcome("success"));

			// <linkingAgentIdentifier>
			LinkingAgentIdentifierComplexType linkingAgentIdentifier = premisObjFactory
					.createLinkingAgentIdentifierComplexType();
			this.premisEvent.getLinkingAgentIdentifier().add(
					linkingAgentIdentifier);
			// <linkingAgentIdentifierType>
			linkingAgentIdentifier.setLinkingAgentIdentifierType("User ID");
			// <linkingAgentIdentifierValue>
			linkingAgentIdentifier.setLinkingAgentIdentifierValue(getLogEntry()
					.getUsername());

			if (getRelatedObjectPID() != null) {
				// <linkingObjectIdentifier>
				LinkingObjectIdentifierComplexType linkingObjectObjectID = new LinkingObjectIdentifierComplexType();
				this.premisEvent.getLinkingObjectIdentifier().add(
						linkingObjectObjectID);
				// <linkingObjectIdentifierType>
				linkingObjectObjectID
						.setLinkingObjectIdentifierType("Representation ID");
				// <linkingObjectIdentifierValue>
				linkingObjectObjectID
						.setLinkingObjectIdentifierValue(getRelatedObjectPID());
			}
		}

		return this.premisEvent;
	}
}
