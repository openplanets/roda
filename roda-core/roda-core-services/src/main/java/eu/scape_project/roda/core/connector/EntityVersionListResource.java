package eu.scape_project.roda.core.connector;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;

import pt.gov.dgarq.roda.core.BrowserHelper;
import eu.scape_project.model.VersionList;
import eu.scape_project.roda.core.connector.utils.HelperUtils;
import eu.scape_project.roda.core.connector.utils.Utils;
import eu.scape_project.util.ScapeMarshaller;

@Path("entity-version-list")
public class EntityVersionListResource {
	static final private Logger logger = Logger.getLogger(EntityResource.class);
	
	@GET
	@Path("{entityID}")
	@Produces(MediaType.APPLICATION_XML)
	public Response getIntellectualEntityVersionsList(@Context HttpServletRequest req, @PathParam("entityID") String entityID) {
		Response r = null;
		try{
			BrowserHelper browser = HelperUtils.getBrowserHelper(req, getClass());
			if(browser!=null){
				List<String> versions = Utils.getVersions(entityID,browser);
				VersionList list = new VersionList(entityID, versions);
				
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ScapeMarshaller.newInstance().serialize(list, bos);
				
				logger.debug("OUTPUT:");
				String output = bos.toString("UTF-8");
				logger.debug(output);
				r = Response.status(Status.OK).entity(output).header("Content-Type", MediaType.TEXT_XML).build();
			}else{
				logger.error("Unable to get helpers");
				r = Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to get helpers").type(MediaType.TEXT_PLAIN).build();
			}
		}catch(JAXBException je){
			logger.error("Error while getting versions list - " + je.getMessage(), je);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while getting versions list - " + je.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (UnsupportedEncodingException uee) {
			logger.error("Error while getting versions list - " + uee.getMessage(), uee);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while getting versions list - " + uee.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}
		return r;
	}
}
