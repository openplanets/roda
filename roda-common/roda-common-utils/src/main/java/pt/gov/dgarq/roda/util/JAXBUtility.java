package pt.gov.dgarq.roda.util;

import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.lang.StringUtils;

/**
 * Utility class for JAXB documents.
 * 
 * @author Rui Castro <rcastro@keep.pt>
 */
public class JAXBUtility {

	/**
	 * Marshal {@link JAXBException} to a {@link String}.
	 * 
	 * @param element
	 *            the {@link JAXBElement} to marshal.
	 * @param isFormatted
	 *            should the output be formatted?
	 * @param isFragment
	 *            is the {@link JAXBElement} a fragment of a bigger document? If
	 *            is <code>false</code> the xml header (&lt;?xml ... ?&gt;) will
	 *            not be generated, if is <code>true</code> it will.
	 * @param schemaLocation
	 *            the schemaLocation attribute value
	 * @param classes
	 *            the XML beans of the schema.
	 * @param <T>
	 *            the type of the {@link JAXBElement} being marshalled.
	 * @return a {@link String} with the content of the marshalled XML.
	 * @throws JAXBException
	 *             if something goes wrong with the marshalling process.
	 */
	public static <T> String marshalToString(JAXBElement<T> element,
			boolean isFormatted, boolean isFragment, String schemaLocation,
			Class<?>... classes) throws JAXBException {

		StringWriter sWriter = new StringWriter();
		marshal(element, isFormatted, isFragment, schemaLocation, sWriter,
				classes);
		return sWriter.toString();
	}

	/**
	 * Marshal {@link JAXBException} to a {@link String}.
	 * 
	 * @param element
	 *            the {@link JAXBElement} to marshal.
	 * @param isFormatted
	 *            should the output be formatted?
	 * @param isFragment
	 *            is the {@link JAXBElement} a fragment of a bigger document? If
	 *            is <code>false</code> the xml header (&lt;?xml ... ?&gt;) will
	 *            not be generated, if is <code>true</code> it will.
	 * @param schemaLocation
	 *            the schemaLocation attribute value
	 * @param writer
	 *            the {@link Writer} to marshal the XML into.
	 * @param classes
	 *            the XML beans of the schema.
	 * @param <T>
	 *            the type of the {@link JAXBElement} being marshalled.
	 * @return a {@link String} with the content of the marshalled XML.
	 * @throws JAXBException
	 *             if something goes wrong with the marshalling process.
	 */
	public static <T> void marshal(JAXBElement<T> element, boolean isFormatted,
			boolean isFragment, String schemaLocation, Writer writer,
			Class<?>... classes) throws JAXBException {

		JAXBContext jc = JAXBContext.newInstance(classes);
		Marshaller marshaller = jc.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, isFormatted);
		marshaller.setProperty(Marshaller.JAXB_FRAGMENT, isFragment);
		if (StringUtils.isNotBlank(schemaLocation)) {
			marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
					schemaLocation);
		}

		marshaller.marshal(element, writer);
	}

	/**
	 * Marshal {@link JAXBException} to a {@link String}.
	 * 
	 * @param element
	 *            the {@link JAXBElement} to marshal.
	 * @param isFormatted
	 *            should the output be formatted?
	 * @param isFragment
	 *            is the {@link JAXBElement} a fragment of a bigger document? If
	 *            is <code>false</code> the xml header (&lt;?xml ... ?&gt;) will
	 *            not be generated, if is <code>true</code> it will.
	 * @param schemaLocation
	 *            the schemaLocation attribute value
	 * @param contextPath
	 *            a colon (:) separated list of package names containing the
	 *            classes that correspond to the XML schema.
	 * @param <T>
	 *            the type of the {@link JAXBElement} being marshalled.
	 * @return a {@link String} with the content of the marshalled XML.
	 * @throws JAXBException
	 *             if something goes wrong with the marshalling process.
	 */
	public static <T> String marshalToString(JAXBElement<T> element,
			boolean isFormatted, boolean isFragment, String schemaLocation,
			String contextPath) throws JAXBException {

		StringWriter sWriter = new StringWriter();
		marshal(element, isFormatted, isFragment, schemaLocation, contextPath,
				sWriter);
		return sWriter.toString();
	}

	/**
	 * Marshal {@link JAXBException} to a {@link String}.
	 * 
	 * @param element
	 *            the {@link JAXBElement} to marshal.
	 * @param isFormatted
	 *            should the output be formatted?
	 * @param isFragment
	 *            is the {@link JAXBElement} a fragment of a bigger document? If
	 *            is <code>false</code> the xml header (&lt;?xml ... ?&gt;) will
	 *            not be generated, if is <code>true</code> it will.
	 * @param schemaLocation
	 *            the schemaLocation attribute value
	 * @param contextPath
	 *            a colon (:) separated list of package names containing the
	 *            classes that correspond to the XML schema.
	 * @param writer
	 *            the {@link Writer} to marshal the XML into.
	 * @param <T>
	 *            the type of the {@link JAXBElement} being marshalled.
	 * @return a {@link String} with the content of the marshalled XML.
	 * @throws JAXBException
	 *             if something goes wrong with the marshalling process.
	 */
	public static <T> void marshal(JAXBElement<T> element, boolean isFormatted,
			boolean isFragment, String schemaLocation, String contextPath,
			Writer writer) throws JAXBException {

		JAXBContext jc = JAXBContext.newInstance(contextPath);
		Marshaller marshaller = jc.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, isFormatted);
		marshaller.setProperty(Marshaller.JAXB_FRAGMENT, isFragment);
		if (StringUtils.isNotBlank(schemaLocation)) {
			marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
					schemaLocation);
		}

		marshaller.marshal(element, writer);
	}

	/**
	 * Unmarshall the XML text read from the {@link InputStream} into a java
	 * object of type &lt;T&gt;.
	 * 
	 * @param clazz
	 *            the {@link Class} of the java object to be created.
	 * @param inputStream
	 *            the {@link InputStream} to read the XML from.
	 * @param <T>
	 *            the type of the {@link JAXBElement} being unmarshalled.
	 * @return the unmarshalled object.
	 * @throws JAXBException
	 *             if something goes wrong with the unmarshalling process.
	 */
	@SuppressWarnings("unchecked")
	public static <T> JAXBElement<T> unmarshal(Class<T> clazz,
			InputStream inputStream) throws JAXBException {
		JAXBContext jc = JAXBContext.newInstance(clazz);
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		return (JAXBElement<T>) unmarshaller.unmarshal(inputStream);

	}

	/**
	 * Unmarshall the XML text read from the {@link InputStream} into a java
	 * object of type &lt;T&gt;.
	 * 
	 * @param contextPath
	 *            a colon (:) separated list of package names containing the
	 *            classes that correspond to the XML schema.
	 * @param inputStream
	 *            the {@link InputStream} to read the XML from.
	 * @param <T>
	 *            the type of the {@link JAXBElement} being unmarshalled.
	 * @return the unmarshalled object.
	 * @throws JAXBException
	 *             if something goes wrong with the unmarshalling process.
	 */
	@SuppressWarnings("unchecked")
	public static <T> JAXBElement<T> unmarshal(String contextPath,
			InputStream inputStream) throws JAXBException {
		JAXBContext jc = JAXBContext.newInstance(contextPath);
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		return (JAXBElement<T>) unmarshaller.unmarshal(inputStream);

	}

}
