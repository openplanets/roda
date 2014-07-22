package pt.gov.dgarq.roda.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.Base64;
import com.sun.jersey.multipart.FormDataMultiPart;

import eu.scape_project.model.plan.PlanExecutionState;
import eu.scape_project.model.plan.PlanExecutionState.ExecutionState;
import eu.scape_project.model.plan.PlanExecutionStateCollection;
import eu.scape_project.util.ScapeMarshaller;

public class PlanClient {
  static final private org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PlanClient.class);

  private static final String planResourcePath = "rest/plan";
  private static final String planReservePath = "rest/plan-id/reserve";

  private URL rodacoreURL = null;

  private String username = null;
  private String password = null;

  private URL planResourceURL = null;
  private URL planReserveURL = null;

  /**
   * Constructs a new authenticated {@link PlanClient} for RODA file upload
   * service.
   * 
   * @param rodacoreURL
   *          the {@link URL} to the RODA Core application.
   * @param username
   *          the username to use in the connection to the service
   * @param password
   *          the password to use in the connection to the service
   * 
   * @throws MalformedURLException
   */
  public PlanClient(URL rodacoreURL, String username, String password) throws MalformedURLException {

    this.rodacoreURL = rodacoreURL;
    this.username = username;
    this.password = password;

    logger.debug("PlanClient(rodacoreURL=" + rodacoreURL + ", username=" + username + ", password=***)");

    if (rodacoreURL == null) {
      throw new MalformedURLException("rodacoreURL cannot be null");
    } else {
      if (!rodacoreURL.toString().endsWith("/")) {
        this.rodacoreURL = new URL(rodacoreURL.toString() + "/");
        logger.warn("rodacoreURL doesn't have a trailing '/'. Fixing it to " + rodacoreURL);
      }
    }

    this.planResourceURL = new URL(this.rodacoreURL, planResourcePath);
    this.planReserveURL = new URL(this.rodacoreURL,planReservePath);
    
    logger.debug("planResourceURL=" + planResourceURL + ", username=" + username);
  }

  public String uploadPlan(File planFile) throws MalformedURLException, FileNotFoundException {

    logger.trace("uploadPlan(" + planFile + ")");

    String auth = new String(Base64.encode(username + ":" + password));
    
    Client c = Client.create();
    c.setFollowRedirects(true);

    WebResource planIDReserveResource = c.resource(planReserveURL.toString());
    ClientResponse idReserveResponse = planIDReserveResource.header("Authorization", "Basic " + auth).accept(MediaType.TEXT_PLAIN).get(ClientResponse.class);
    String reserveID = idReserveResponse.getEntity(String.class);
    
    
    URL planURL = new URL(this.rodacoreURL,"rest/plan/"+reserveID);
   
    WebResource putPlanResource = c.resource(planURL.toString());
    InputStream fileInStream = new FileInputStream(planFile);
    String sContentDisposition = "attachment; filename=\"" + planFile.getName()+"\"";
    ClientResponse response = putPlanResource.type(MediaType.APPLICATION_OCTET_STREAM)
    		.header("Authorization", "Basic " + auth)
                            .header("Content-Disposition", sContentDisposition)
                            .put(ClientResponse.class, fileInStream);  
    return reserveID;

  }

public File retrievePlan(String planID) throws MalformedURLException {
	logger.trace("retrievePlan(" + planID + ")");

    Client c = Client.create();
    logger.debug("Activating follow redirects on HTTP client");
    c.setFollowRedirects(true);

    URL planURL = new URL(this.rodacoreURL,"rest/plan/"+planID);
    WebResource r = c.resource(planURL.toString());
    
    String auth = new String(Base64.encode(username + ":" + password));
    
    //ClientResponse response = r.header("Authorization", "Basic " + auth).type(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_XML).get(ClientResponse.class);
    ClientResponse response = r.header("Authorization", "Basic " + auth).accept(MediaType.APPLICATION_XML).get(ClientResponse.class);
    if (response.getStatus() != 200) {
        throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
    }
    File s= response.getEntity(File.class);
    return s;
}
  

public String retrievePlanURL(String planID) throws MalformedURLException {
	logger.trace("retrievePlanURL(" + planID + ")");
	URL planURL = new URL(this.rodacoreURL,"rest/plan/"+planID);
	return planURL.toString();
}
  
public static void main(String[] args) {
	try{
		/*PlanClient pc = new PlanClient(new URL("http://roda.scape.keep.pt/roda-core/"), "admin", "QzbUmO+5");
		System.out.println("Uploading plan...");
		String planID = pc.uploadPlan(new File("/home/sleroux/Desktop/PreservationPlanReport/testplan.xml"));
		System.out.println("PlanID: "+planID);
		*/
		  String xml = "<ns10:plan-execution-state ns10:timeStamp='2014-06-24T10:12:41.777+01:00' ns10:state='EXECUTION_IN_PROGRESS' xmlns:port='http://ns.taverna.org.uk/2010/port/' xmlns:ts-rest='http://ns.taverna.org.uk/2010/xml/server/rest/' xmlns:ts-soap='http://ns.taverna.org.uk/2010/xml/server/soap/' xmlns:scape='http://www.scape-project.eu/api/execution' xmlns:ts='http://ns.taverna.org.uk/2010/xml/server/' xmlns:admin='http://ns.taverna.org.uk/2010/xml/server/admin/' xmlns:xlink='http://www.w3.org/1999/xlink' xmlns:feed='http://ns.taverna.org.uk/2010/xml/server/feed/' xmlns:plato='http://ifs.tuwien.ac.at/dp/plato' xmlns:ns10='http://scape-project.eu/model'></ns10:plan-execution-state>";
	    //String xml = "<scape:plan-execution-state xmlns:mets='http://www.loc.gov/METS/' xmlns:xlink='http://www.w3.org/1999/xlink' xmlns:scape='http://scape-project.eu/model' xmlns:dc='http://purl.org/dc/elements/1.1/' xmlns:premis='info:lc/xmlns/premis-v2' xmlns:textmd='info:lc/xmlns/textmd-v3' xmlns:fits='http://hul.harvard.edu/ois/xml/ns/fits/fits_output' xmlns:ns9='http://www.loc.gov/mix/v20' xmlns:gbs='http://books.google.com/gbs' xmlns:vmd='http://www.loc.gov/videoMD/' xmlns:ns12='http://www.loc.gov/audioMD/' xmlns:marc='http://www.loc.gov/MARC21/slim' timestamp='2014-07-03T14:46:43.815+01:00' state='EXECUTION_SUCCESS'/>";
	    InputStream is = new ByteArrayInputStream(xml.getBytes());

		PlanExecutionState pes = ScapeMarshaller.newInstance().deserialize(PlanExecutionState.class, is);
		System.out.println("STATE:"+pes.getState());
		System.out.println("DATE:"+pes.getTimeStamp());
		
		PlanExecutionState pes2 = new PlanExecutionState();
		pes2.setState(ExecutionState.EXECUTION_SUCCESS);
		pes2.setTimeStamp(new Date());
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		ScapeMarshaller.newInstance().serialize(pes2, os);
		System.out.println(new String(os.toByteArray()));
		/*
		pes.setState(ExecutionState.EXECUTION_IN_PROGRESS);
		pes.setTimeStamp(new Date());
		System.out.println("Adding state: "+pes.getState());
		pc.addExecutionState(planID,pes);
		System.out.println("Getting execution states for plan "+planID);
		PlanExecutionStateCollection pesc = pc.getPlanExecutionStates(planID);
		for(PlanExecutionState p : pesc.executionStates){
			System.out.println("Timestamp: "+p.getTimeStamp()+ " - State: "+p.getState());
		}
		Thread.sleep(1000);
		pes.setState(ExecutionState.EXECUTION_SUCCESS);
		pes.setTimeStamp(new Date());
		System.out.println("Adding state: "+pes.getState());
		pc.addExecutionState(planID,pes);
		System.out.println("Getting execution states for plan "+planID);
		pesc = pc.getPlanExecutionStates(planID);
		for(PlanExecutionState p : pesc.executionStates){
			System.out.println("Timestamp: "+p.getTimeStamp()+ " - State: "+p.getState());
		}
		*/
	}catch(Exception e){
		e.printStackTrace();
	}
}

private PlanExecutionStateCollection getPlanExecutionStates(String planID) {
	try{
	logger.trace("getPlanExecutionStates(" + planID + ")");

    Client c = Client.create();
    logger.debug("Activating follow redirects on HTTP client");
    c.setFollowRedirects(true);

    URL planURL = new URL(this.rodacoreURL,"rest/plan-execution-state/"+planID);
    WebResource r = c.resource(planURL.toString());
    
    String auth = new String(Base64.encode(username + ":" + password));
    
    //ClientResponse response = r.header("Authorization", "Basic " + auth).type(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_XML).get(ClientResponse.class);
    ClientResponse response = r.header("Authorization", "Basic " + auth).accept(MediaType.APPLICATION_XML).get(ClientResponse.class);
    if (response.getStatus() != 200) {
        throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
    }
    PlanExecutionStateCollection  pesc = response.getEntity(PlanExecutionStateCollection.class);
    return pesc;
	}catch(Exception e){
		e.printStackTrace();	
	}
	return null;
}

private void addExecutionState(String planID, PlanExecutionState pes) {	
	try{
	    String auth = new String(Base64.encode(username + ":" + password));
	    
	    Client c = Client.create();
	    c.setFollowRedirects(true);
	    
	    
	    URL addExecutionURL = new URL(this.rodacoreURL,"rest/plan-execution-state/"+planID);
	    WebResource putPlanResource = c.resource(addExecutionURL.toString());
	
	    ClientResponse response = putPlanResource.type(MediaType.TEXT_XML)
	    		.header("Authorization", "Basic " + auth)
	                            .post(ClientResponse.class, pes);  
	}catch(Exception e){
		e.printStackTrace();
	}
	
}

}
