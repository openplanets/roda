package eu.scape_project.roda.core.report;

import info.lc.xmlns.premis_v2.EventComplexType;
import info.lc.xmlns.premis_v2.EventIdentifierComplexType;
import info.lc.xmlns.premis_v2.EventOutcomeInformationComplexType;
import info.lc.xmlns.premis_v2.LinkingAgentIdentifierComplexType;
import info.lc.xmlns.premis_v2.LinkingObjectIdentifierComplexType;
import info.lc.xmlns.premis_v2.ObjectFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.util.DateParser;

import pt.gov.dgarq.roda._2008.eadcschema.C;
import pt.gov.dgarq.roda.core.BrowserHelper;
import pt.gov.dgarq.roda.core.common.BrowserException;
import pt.gov.dgarq.roda.core.common.NoSuchRODAObjectException;
import pt.gov.dgarq.roda.core.data.LogEntry;
import pt.gov.dgarq.roda.core.data.SimpleDescriptionObject;
import eu.scape_project.repository.report.AbstractEventCatalog;

/**
 * @author Rui Castro
 */
public class ViewDMDEvent extends AbstractLogEntryEvent {

	/**
	 * The logger.
	 */
	private static Logger logger = Logger.getLogger(ViewDMDEvent.class);

	private BrowserHelper browser = null;
	private SimpleDescriptionObject simpleDO = null;
	private EventComplexType premisEvent = null;

	/**
	 * Constructs a new {@link ViewDMDEvent} for the specified {@link LogEntry}.
	 * 
	 * @param logEntry
	 *            the {@link LogEntry}.
	 * @param browserHelper
	 */
	public ViewDMDEvent(LogEntry logEntry, BrowserHelper browserHelper) {
		super(logEntry);
		this.browser = browserHelper;
	}

	@Override
	public String getType() {
		logger.debug("getType()");
		return "ViewDMD";
	}

	@Override
	public List<String> getPremisRelatedObjects() {
		logger.debug("getPremisRelatedObjects()");

		final List<String> relatedObjects = new ArrayList<String>();

		if (getSimpleDO() != null) {
			try {
				final InputStream inputStream = this.browser
						.getFedoraClientUtility().getDatastream(
								getSimpleDO().getPid(), "EAD-C");

				final JAXBElement<C> eadcElement = AbstractEventCatalog.unmarshal(
						"pt.gov.dgarq.roda._2008.eadcschema", inputStream);

				inputStream.close();

				final String eadcXmlString = AbstractEventCatalog.marshalToString(
						eadcElement, true, true, null, C.class);

				logger.debug("EAD-C XML: " + eadcXmlString);

				relatedObjects.add(eadcXmlString);

			} catch (JAXBException e) {
				logger.error("Couldn't retrieve related objects information - "
						+ e.getMessage(), e);
			} catch (IOException e) {
				logger.error("Couldn't retrieve related objects information - "
						+ e.getMessage(), e);
			}
		} else {
			logger.info("No PREMIS related objects because DO is null");
		}

		return relatedObjects;
	}

	private SimpleDescriptionObject getSimpleDO() {
		logger.debug("getSimpleDO()");

		if (this.simpleDO == null) {

			String doPID = null;
			if (StringUtils.isNotBlank(getLogEntry().getDescription())) {
				doPID = "roda:"
						+ getLogEntry().getDescription().replace(
								"dissemination.browse.", "");
			}
			if (doPID != null) {
				try {

					this.simpleDO = this.browser
							.getSimpleDescriptionObject(doPID);

				} catch (BrowserException e) {
					logger.warn(
							"Couldn't retrieve DO " + doPID + " - "
									+ e.getMessage(), e);
				} catch (NoSuchRODAObjectException e) {
					logger.warn(
							"Couldn't retrieve DO " + doPID + " - "
									+ e.getMessage(), e);
				}
			} else {
				logger.warn("Couldn't retrieve DO, because PID is null");
			}
		}

		return this.simpleDO;
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
			premisEvent.getEventOutcomeInformation().add(outcome);

			// <eventOutcome>
			ObjectFactory premisObjFactory = new ObjectFactory();
			outcome.getContent()
					.add(premisObjFactory
							.createEventOutcome(getSimpleDO() != null ? "success"
									: "failure"));

			// <linkingAgentIdentifier>
			LinkingAgentIdentifierComplexType linkingAgentIdentifier = premisObjFactory
					.createLinkingAgentIdentifierComplexType();
			premisEvent.getLinkingAgentIdentifier().add(linkingAgentIdentifier);
			// <linkingAgentIdentifierType>
			linkingAgentIdentifier.setLinkingAgentIdentifierType("User ID");
			// <linkingAgentIdentifierValue>
			linkingAgentIdentifier.setLinkingAgentIdentifierValue(getLogEntry()
					.getUsername());

			if (getSimpleDO() != null) {
				// <linkingObjectIdentifier>
				LinkingObjectIdentifierComplexType linkingObjectObjectID = new LinkingObjectIdentifierComplexType();
				this.premisEvent.getLinkingObjectIdentifier().add(
						linkingObjectObjectID);
				// <linkingObjectIdentifierType>
				linkingObjectObjectID.setLinkingObjectIdentifierType("DMD ID");
				// <linkingObjectIdentifierValue>
				linkingObjectObjectID
						.setLinkingObjectIdentifierValue(getSimpleDO().getPid());
			}
		}

		return this.premisEvent;
	}

}
