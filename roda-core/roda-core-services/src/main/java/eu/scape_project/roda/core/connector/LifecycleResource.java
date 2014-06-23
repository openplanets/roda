package eu.scape_project.roda.core.connector;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

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
import pt.gov.dgarq.roda.core.common.NoSuchRODAObjectException;
import pt.gov.dgarq.roda.core.data.DescriptionObject;
import eu.scape_project.model.LifecycleState;
import eu.scape_project.model.LifecycleState.State;
import eu.scape_project.roda.core.connector.utils.HelperUtils;
import eu.scape_project.roda.core.connector.utils.IngestionUtils;
import eu.scape_project.roda.core.connector.utils.Utils;
import eu.scape_project.util.ScapeMarshaller;

@Path("lifecycle")
public class LifecycleResource {
	static final private Logger logger = Logger.getLogger(LifecycleResource.class);
	
	
	@GET
	@Path("status/{statusID}")
	@Produces(MediaType.APPLICATION_XML)
	public Response getLifecycleStatusFromStatusId(@Context HttpServletRequest req, @PathParam("statusID") String statusID) {
		logger.debug("getLifecycleStatusFromStatusId(statusID='"+statusID+"')");
		Response r = null;
		try{
			BrowserHelper browser = HelperUtils.getBrowserHelper(req,getClass());
			if(browser!=null){
				LifecycleState lcs = null;
				if(!IngestionUtils.getInstance().statusIdExists(statusID)){
					r = Response.status(Status.NOT_FOUND).build();
				}else{
					String entityId = IngestionUtils.getInstance().getIntellectualEntityFromStatusId(statusID).getIdentifier().getValue();
					
					DescriptionObject descriptionObject = Utils.findById(entityId, browser);
					
					State descriptionObjectState;
					
					if(descriptionObject==null){
						descriptionObjectState = State.INGESTING;
					}else{
						descriptionObjectState = State.INGESTED;
					}
					lcs = new LifecycleState("", descriptionObjectState);
					
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					ScapeMarshaller.newInstance().serialize(lcs, bos);
					
					logger.debug("OUTPUT:");
					String output = bos.toString("UTF-8");
					r = Response.ok().entity(output).header("Content-Type", MediaType.TEXT_XML).build();
				}
			}else{
				logger.error("Unable to get helpers");
				r = Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to get helpers").type(MediaType.TEXT_PLAIN).build();
			}
		}catch(Exception e){
			logger.error("Error while gettinh LifeCycleState: "+e.getMessage(),e);
			r = Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error while getting LifeCycleState").type(MediaType.TEXT_PLAIN).build();
		}
		return r;

	}
	
	
	@GET
	@Path("{entityID}")
	@Produces(MediaType.APPLICATION_XML)
	public Response getLifecycleStatus(@Context HttpServletRequest req, @PathParam("entityID") String entityID) {
		logger.debug("getLifecycleStatus(entityID='"+entityID+"')");
		try {
			BrowserHelper browser = HelperUtils.getBrowserHelper(req,getClass());
			if(browser!=null){
				State descriptionObjectState;
				if(IngestionUtils.getInstance().getStatusIDFromEntityID(entityID)!=null){
					descriptionObjectState = State.INGESTING;
				}else{
					DescriptionObject o = Utils.findById(entityID, browser);
					if(o==null){
						descriptionObjectState = State.INGEST_FAILED;
					}else{
						descriptionObjectState = State.INGESTED;
					}
					
				}
				LifecycleState lcs = new LifecycleState("", descriptionObjectState);
	
				
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ScapeMarshaller.newInstance().serialize(lcs, bos);
				return Response.ok().entity(bos.toString("UTF-8")).header("Content-Type", MediaType.TEXT_XML).build();
			}else{
				logger.error("Unable to get helpers");
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to get helpers").type(MediaType.TEXT_PLAIN).build();
			}
		} catch (JAXBException je) {
			logger.error("Error serializing entity " + entityID + " - " + je.getMessage(), je);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error serializing entity " + entityID + " - " + je.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (UnsupportedEncodingException uee) {
			logger.error("Error creating string from stream - " + uee.getMessage(), uee);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error creating string from stream - " + uee.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (NoSuchRODAObjectException e) {
			logger.error("Error serializing entity " + entityID + " - " + e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error serializing entity " + entityID + " - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}
	}
}
