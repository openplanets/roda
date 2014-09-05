package eu.scape_project.roda.wui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import pt.gov.dgarq.roda.common.RodaClientFactory;
import pt.gov.dgarq.roda.core.PlanClient;
import pt.gov.dgarq.roda.util.XsltUtility;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * Servlet implementation class SCAPEPlanViewer
 */
public class SCAPEPlanViewer extends HttpServlet {
	private static final long serialVersionUID = 1L;
	static final private Logger logger = Logger.getLogger(SCAPEPlanViewer.class);
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try{
			String planID = request.getParameter("planID");
			logger.error("PLAN VIEW: "+planID);
			URL rodaCoreURL = RodaClientFactory.getRodaCoreUrl();
			// FIXME fix hardcoded username/password
			PlanClient client = new PlanClient(rodaCoreURL, "admin", "roda");
			File planXML = client.retrievePlan(planID);
			logger.error(planXML.getPath());
			logger.error("END");			
			
			// generate svg for workflow
			String workflow = getWorkflowFileAsString(retrieveDocumentFromPlan(planXML));
			Client restClient = Client.create();
		    logger.debug("Activating follow redirects on HTTP client");
		    restClient.setFollowRedirects(true);
		    // FIXME hardcoded url
		    URL t2flow2svgURL = new URL("http://localhost:8180/t2flow2svg/");
		    WebResource webResource = restClient.resource(t2flow2svgURL.toString());
		    ClientResponse t2flow2svgResponse = webResource.type("application/vnd.taverna.t2flow+xml").accept("image/svg+xml").post(ClientResponse.class, workflow);
		    if (t2flow2svgResponse.getStatus() != 200) {
		        throw new RuntimeException("Failed : HTTP error code : " + t2flow2svgResponse.getStatus());
		    }
		    // FIXME get a proper way to pass the svg to the xslt
//		    File s= t2flow2svgResponse.getEntity(File.class);
//		    String tempDir = System.getProperty("java.io.tmpdir");
//		    File tempFile = new File(tempDir,planXML.getName()+".svg");
//		    s.renameTo(tempFile);
		    
			InputStream planStream = new FileInputStream(planXML);
			InputStream xsltStream = this.getClass().getResourceAsStream("/generatePlanSummary.xsl");
			Map<String,Object> parameters = new HashMap<String,Object>();
			parameters.put("downloadURL", client.retrievePlanURL(planID));
			parameters.put("workflowSVG", t2flow2svgResponse.getEntity(String.class).replaceFirst("(?s).+<svg ", "<svg "));
			XsltUtility.applyTransformation(xsltStream, parameters, planStream, response.getOutputStream());
		}catch(Exception e){
			logger.error(e.getMessage(),e);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}
	
	private Document retrieveDocumentFromPlan(File plan){
		final DocumentBuilderFactory factory = DocumentBuilderFactory
				.newInstance();
		factory.setNamespaceAware(true);

		Document workflow = null;
		
		try {

			final DocumentBuilder builder = factory.newDocumentBuilder();
			workflow = builder.parse(plan);

		} catch (ParserConfigurationException e) {
			logger.error("Error creating Document - " + e.getMessage());
		} catch (SAXException e) {
			logger.error("Error parsing plan XML file - " + e.getMessage());
		} catch (IOException e) {
			logger.error("Error reading plan XML file - " + e.getMessage());
		}
		
		return workflow;
	}
	
	private String getWorkflowFileAsString(final Document plan) {

		final XPathFactory xPathfactory = XPathFactory.newInstance();
		final XPath xpath = xPathfactory.newXPath();
		
		String workflow="";

		StringWriter writer = new StringWriter();
		try {

			xpath.setNamespaceContext(new PlanNamespaceContext());
			final XPathExpression expr = xpath.compile("/plato:plans/plato:plan/plato:preservationActionPlan/plato:executablePlan[@type='t2flow']/t2flow:workflow");
			final Element elementWorkflow = (Element) expr.evaluate(plan,
					XPathConstants.NODE);

			// Prepare the DOM document for writing
			final Source source = new DOMSource(elementWorkflow);
			
			final Result result = new StreamResult(writer);

			// Write the DOM document
			final Transformer xformer = TransformerFactory.newInstance()
					.newTransformer();
			xformer.transform(source, result);

			workflow = writer.getBuffer().toString();

		} catch (XPathExpressionException e) {
			logger.error("Error compiling XPATH expression or evaluating the plan - "
					+ e.getMessage());
		
		} catch (TransformerFactoryConfigurationError e) {
			logger.error("Error writing workflow file - " + e.getMessage());
			
		} catch (TransformerException e) {
			logger.error("Error writing workflow file - " + e.getMessage());

		} finally {

			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					logger.error("Error closing output writer - "
							+ e.getMessage());
				}
			}

		}
		
		return workflow;

	}
	
	public static void main(String[] args) throws MalformedURLException {
		File plan = new File("/home/hsilva/Desktop/Downloads/d8395fa1-8e43-416b-9e59-f8db9ca93df9.xml");
//		File plan = new File("/WDdata/Git/roda-openplanets/roda-client/src/test/resources/plan.xml");
		
		SCAPEPlanViewer scapePlanViewer = new SCAPEPlanViewer();
		
		Document retrieveDocumentFromPlan = scapePlanViewer.retrieveDocumentFromPlan(plan);
		
		String workflowFileAsString = scapePlanViewer.getWorkflowFileAsString(retrieveDocumentFromPlan);
		
//		System.err.println(workflowFileAsString);
		
		Client c = Client.create();
	    logger.debug("Activating follow redirects on HTTP client");
	    c.setFollowRedirects(true);
	    // FIXME hardcoded url
	    URL t2flow2svg = new URL("http://localhost:8180/t2flow2svg/");
	    WebResource r = c.resource(t2flow2svg.toString());
	    ClientResponse response2 = r.type("application/vnd.taverna.t2flow+xml").accept("image/svg+xml").post(ClientResponse.class, workflowFileAsString);
	    if (response2.getStatus() != 200) {
	        throw new RuntimeException("Failed : HTTP error code : " + response2.getStatus());
	    }
	    
	    System.out.println(response2.getEntity(String.class).replaceFirst("(?s).+<svg ", "<svg "));
	    
//	    File s= response2.getEntity(File.class);
//	    String tempDir = System.getProperty("java.io.tmpdir");
//	    File tempFile = new File(tempDir,"d8.svg");
//	    s.renameTo(tempFile);
		
	}
	
	class PlanNamespaceContext implements NamespaceContext {

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

}
