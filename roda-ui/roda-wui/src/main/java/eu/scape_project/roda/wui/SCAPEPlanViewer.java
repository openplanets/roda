package eu.scape_project.roda.wui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import pt.gov.dgarq.roda.common.RodaClientFactory;
import pt.gov.dgarq.roda.core.PlanClient;
import pt.gov.dgarq.roda.util.XsltUtility;

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
			PlanClient client = new PlanClient(rodaCoreURL, "admin", "roda");
			File planXML = client.retrievePlan(planID);
			logger.error(planXML.getPath());
			logger.error("END");
			InputStream planStream = new FileInputStream(planXML);
			InputStream xsltStream = this.getClass().getResourceAsStream("/generatePlanSummary.xsl");
			Map<String,Object> parameters = new HashMap<String,Object>();
			parameters.put("downloadURL", client.retrievePlanURL(planID));
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

}
