package eu.scape_project.roda.report;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.openarchives.oai._2_0.oai_dc.ElementType;
import org.openarchives.oai._2_0.oai_dc.OaiDcType;
import org.openarchives.oai._2_0.oai_dc.ObjectFactory;
import org.w3c.util.DateParser;

public abstract class AbstractEvent implements Event {

	/**
	 * The logger.
	 */
	private static Logger logger = Logger.getLogger(AbstractEvent.class);

	@Override
	public String getOaiDcEvent() {
		logger.debug("getOaiDcEvent()");
		OaiDcType oaiDcType = new OaiDcType();

		ElementType identifier = new ElementType();
		identifier.setValue(getIdentifier());

		ObjectFactory objFactory = new ObjectFactory();

		oaiDcType.getTitleOrCreatorOrSubject().add(
				objFactory.createIdentifier(identifier));

		ElementType type = new ElementType();
		type.setValue(getType());

		oaiDcType.getTitleOrCreatorOrSubject().add(objFactory.createType(type));

		ElementType date = new ElementType();
		date.setValue(DateParser.getIsoDateNoMillis(getDatetime()));

		oaiDcType.getTitleOrCreatorOrSubject().add(objFactory.createDate(date));

		try {

			return EventCatalog
					.marshalToString(
							objFactory.createDc(oaiDcType),
							true,
							true,
							"http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd",
							OaiDcType.class);

		} catch (JAXBException e) {
			logger.error(
					"Could not marshal oai_dc element into text - "
							+ e.getMessage(), e);
			return null;
		}

	}

}
