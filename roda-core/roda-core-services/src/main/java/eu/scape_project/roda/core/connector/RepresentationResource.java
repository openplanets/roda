package eu.scape_project.roda.core.connector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import pt.gov.dgarq.roda.core.BrowserHelper;
import pt.gov.dgarq.roda.core.EditorHelper;
import pt.gov.dgarq.roda.core.IngestHelper;
import pt.gov.dgarq.roda.core.common.NoSuchRODAObjectException;
import pt.gov.dgarq.roda.core.common.RODAServiceException;
import pt.gov.dgarq.roda.core.data.DescriptionObject;
import pt.gov.dgarq.roda.core.data.RepresentationObject;
import pt.gov.dgarq.roda.core.fedora.FedoraClientException;
import eu.scape_project.model.Representation;
import eu.scape_project.roda.core.connector.utils.DataModelUtils;
import eu.scape_project.roda.core.connector.utils.HelperUtils;
import eu.scape_project.roda.core.connector.utils.IngestionUtils;
import eu.scape_project.roda.core.connector.utils.UploadException;
import eu.scape_project.roda.core.connector.utils.Uploader;
import eu.scape_project.roda.core.connector.utils.Utils;
import eu.scape_project.util.ScapeMarshaller;

@Path("representation")
public class RepresentationResource {
static final private Logger logger = Logger.getLogger(RepresentationResource.class);
	
	@GET
	@Path("{entityID}/{representationID}")
	@Produces(MediaType.APPLICATION_XML)
	public Response getRepresentation(@Context HttpServletRequest req, @PathParam("entityID") String entityID, @PathParam("representationID") String representationID) {
		logger.debug("getRepresentation(entityID='"+entityID+"', representationID='"+representationID+"')");
		String coreURL = req.getRequestURL().substring(0,req.getRequestURL().indexOf("/rest/")); 
		Response r = null;
		try {
			BrowserHelper browser = HelperUtils.getBrowserHelper(req,getClass());
			EditorHelper editor = HelperUtils.getEditorHelper(req,getClass());
			if(browser!=null && editor!=null){
				DescriptionObject o = Utils.findById(entityID, browser); 
				if(o==null){
					throw new NoSuchRODAObjectException("No Representation with ID/PID can be found");
				}
				RepresentationObject[] ros = browser.getDORepresentations(o.getPid());
				logger.debug("Representations found:"+ros.length);
				if(ros.length>0){
					for(RepresentationObject ro : ros){
						if(ro.getId().equalsIgnoreCase(representationID)){
							Representation rep = DataModelUtils.getInstance(browser,editor).representationObjectToRepresentation(browser, ro,coreURL);
							ByteArrayOutputStream bos = new ByteArrayOutputStream();
							ScapeMarshaller.newInstance().serialize(rep, bos);
							r =  Response.ok().entity(bos.toString("UTF-8")).header("Content-Type", MediaType.TEXT_XML).build();
						}
					}
				}
				if(r==null){
					r = Response.status(Response.Status.NOT_FOUND).entity("Not found representation" + representationID+ " for entity " +entityID).type(MediaType.TEXT_PLAIN).build();
				}
			}else{
				logger.error("Unable to get helpers");
				r = Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to get helpers").type(MediaType.TEXT_PLAIN).build();
			}
			return r;
		} catch(RODAServiceException rse){
			logger.error("Couldn't retrieve entity - " + rse.getMessage(), rse);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Couldn't retrieve entity - " + rse.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (NoSuchRODAObjectException nsroe) {
			logger.error(nsroe.getMessage(), nsroe);
			return Response.status(Response.Status.NOT_FOUND).entity(nsroe.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (JAXBException je) {
			logger.error("Error serializing Representation " + representationID + " - " + je.getMessage(), je);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error serializing Representation " + representationID + " - " + je.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (UnsupportedEncodingException uee) {
			logger.error("Error creating string from stream - " + uee.getMessage(), uee);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error creating string from stream - " + uee.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (URISyntaxException use) {
			logger.error("Error creating URI - " + use.getMessage(), use);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error creating URI - " + use.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} 
	}
	
	@PUT
	@Path("{entityID}/{representationID}")
	@Consumes("application/xml")
	public Response updateRepresentation(@Context HttpServletRequest req,@PathParam("entityID") String entityID, @PathParam("representationID") String representationID, byte[] binaryRepresentation) {
		logger.debug("updateRepresentation(entityID='"+entityID+"', representationID='"+representationID+"'");
		Response r = null;
		try {
			
			BrowserHelper browser = HelperUtils.getBrowserHelper(req, getClass());
			EditorHelper editor = HelperUtils.getEditorHelper(req, getClass());
			IngestHelper ingest = HelperUtils.getIngestHelper(req, getClass());
			Uploader uploader = HelperUtils.getUploader(req, getClass());
			
			logger.debug("Representation deserialization...");
			Representation newRepresentation = ScapeMarshaller.newInstance().deserialize(Representation.class, new ByteArrayInputStream(binaryRepresentation));

			
			
			if(browser!=null){
				logger.debug("Find representation:"+entityID);
				DescriptionObject o = Utils.findById(entityID, browser);
				logger.debug("DO PID:"+o.getPid());
				logger.debug("Getting DO representations...");
				RepresentationObject[] ros = browser.getDORepresentations(o.getPid());
				logger.debug("Number of representations:"+ros.length);
				for(RepresentationObject ro : ros){
					logger.debug("RO ID:"+ro.getId());
					if(ro.getId().equalsIgnoreCase(representationID)){
						logger.debug("MATCH!!!");
						logger.debug("Representation to RepresentationObject");
						RepresentationObject newRepresentationObject = DataModelUtils.getInstance(browser, editor).representationToRepresentationObject(newRepresentation);
						logger.debug("Updating RepresentationObject");
						IngestionUtils.getInstance().updateRepresentation(o.getPid(),ro,newRepresentationObject,ingest,uploader,newRepresentation.getFiles());

						r = Response.status(201).build();
						break;
					}
				}
				if(r==null){
					r = Response.status(Response.Status.NOT_FOUND).entity("Not found representation" + representationID+ " for entity " +entityID).type(MediaType.TEXT_PLAIN).build();
				}
			}else{
				logger.error("Unable to get helpers");
				r = Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to get helpers").type(MediaType.TEXT_PLAIN).build();
			}
			return r;
		} catch(RODAServiceException rse){
			logger.error("Couldn't retrieve entity - " + rse.getMessage(), rse);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Couldn't retrieve entity - " + rse.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (NoSuchRODAObjectException nsroe) {
			logger.error("RepresentationObject " + representationID + " doesn't exist - " + nsroe.getMessage(), nsroe);
			return Response.status(Response.Status.NOT_FOUND).entity("RepresentationObject " + representationID + " doesn't exist - " + nsroe.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (JAXBException je) {
			logger.error("Error serializing Representation " + representationID + " - " + je.getMessage(), je);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error serializing Representation " + representationID + " - " + je.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (UnsupportedEncodingException uee) {
			logger.error("Error creating string from stream - " + uee.getMessage(), uee);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error creating string from stream - " + uee.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (ConfigurationException ce) {
			logger.error("Error creating helper - " + ce.getMessage(), ce);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error creating helper - " + ce.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (FedoraClientException fce) {
			logger.error("Error creating helper - " + fce.getMessage(), fce);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error creating helper - " + fce.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (MalformedURLException mue) {
			logger.error("Error creating helper - " + mue.getMessage(), mue);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error creating helper - " + mue.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (UploadException ue) {
			logger.error("Error updating representation - " + ue.getMessage(), ue);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error updating representation - " + ue.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (IOException ioe) {
			logger.error("Error updating representation - " + ioe.getMessage(), ioe);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error updating representation - " + ioe.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}
		
	}
}
