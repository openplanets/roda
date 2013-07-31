package eu.scape_project.roda.core.plan;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

/**
 * The plan XML {@link NamespaceContext}.
 * 
 * @author Rui Castro
 */
public class PlanNamespaceContext implements NamespaceContext {

	@Override
	public Iterator getPrefixes(final String namespaceURI) {
		return null;
	}

	@Override
	public String getPrefix(final String namespaceURI) {
		return null;
	}

	@Override
	public String getNamespaceURI(final String prefix) {
		String uri;
		if ("plato".equals(prefix)) {
			uri = "http://ifs.tuwien.ac.at/dp/plato";
		} else if ("t2flow".equals(prefix)) {
			uri = "http://taverna.sf.net/2008/xml/t2flow";
		} else {
			uri = null;
		}
		return uri;
	}
}
