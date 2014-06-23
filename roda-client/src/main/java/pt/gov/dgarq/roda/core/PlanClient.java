package pt.gov.dgarq.roda.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.ws.rs.core.MediaType;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.Base64;
import com.sun.jersey.multipart.FormDataMultiPart;

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

    logger.info("POST " + planFile + " to " + planResourceURL.toString() + " successful");

    return reserveID;

  }

public File retrievePlan(String planID) throws MalformedURLException {
	logger.trace("retrievePlan(" + planID + ")");

    Client c = Client.create();
    logger.debug("Activating follow redirects on HTTP client");
    c.setFollowRedirects(true);

    logger.error("RODA CORE:"+this.rodacoreURL.toString());
    URL planURL = new URL(this.rodacoreURL,"rest/plan/"+planID);
    logger.error("PLAN URL: "+planURL.toString());
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
		PlanClient pc = new PlanClient(new URL("http://localhost:8080/roda-core/"), "admin", "roda");
		String planID = pc.uploadPlan(new File("/home/sleroux/Desktop/PreservationPlanReport/testplan.xml"));
		File f = pc.retrievePlan(planID);
		logger.error(f.getPath());
	}catch(Exception e){
		e.printStackTrace();
	}
}

}
