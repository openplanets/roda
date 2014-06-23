package eu.scape_project.roda.core.connector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
import pt.gov.dgarq.roda.core.common.BrowserException;
import pt.gov.dgarq.roda.core.common.EditorException;
import pt.gov.dgarq.roda.core.common.NoSuchRODAObjectException;
import pt.gov.dgarq.roda.core.common.RODAServiceException;
import pt.gov.dgarq.roda.core.data.DescriptionObject;
import pt.gov.dgarq.roda.core.fedora.FedoraClientException;
import pt.gov.dgarq.roda.core.metadata.eadc.EadCMetadataException;
import eu.scape_project.model.Identifier;
import eu.scape_project.model.IntellectualEntity;
import eu.scape_project.model.LifecycleState;
import eu.scape_project.model.Representation;
import eu.scape_project.roda.core.connector.utils.DataConnectorException;
import eu.scape_project.roda.core.connector.utils.DataModelUtils;
import eu.scape_project.roda.core.connector.utils.HelperUtils;
import eu.scape_project.roda.core.connector.utils.IngestionUtils;
import eu.scape_project.roda.core.connector.utils.Uploader;
import eu.scape_project.roda.core.connector.utils.Utils;
import eu.scape_project.util.ScapeMarshaller;

@Path("entity")
public class EntityResource {
	static final private Logger logger = Logger.getLogger(EntityResource.class);
	
	@GET
	@Path("{entityID}/{versionID}")
	@Produces(MediaType.APPLICATION_XML)
	public Response getIntellectualEntity(@Context HttpServletRequest req, @PathParam("entityID") String entityID,@PathParam("versionID") String versionID, @QueryParam("useReferences")@DefaultValue("no")String useReferences) {
		logger.debug("getIntellectualEntity(entityID='"+entityID+"', useReferences='"+useReferences+"')");
		try {
			BrowserHelper browser = HelperUtils.getBrowserHelper(req,getClass());
			EditorHelper editor = HelperUtils.getEditorHelper(req,getClass());
			if(browser!=null && editor!=null){
				boolean useRef = false;
				if(useReferences!=null && useReferences.equalsIgnoreCase("yes")){
					useRef = true;
				}
	
				int version = Integer.parseInt(versionID);
				String coreURL = req.getRequestURL().substring(0,req.getRequestURL().indexOf("/rest/")); 

				
				DescriptionObject descriptionObject = Utils.findById(entityID,version, browser);
				Object descriptive = null;
				try{
					descriptive = Utils.getDatastreamObject(descriptionObject.getPid(), version,"DESCRIPTIVE",  browser);
				}catch(Exception e){
					logger.debug("No DESCRIPTIVE datastream associated to "+descriptionObject.getPid());
				}
				LifecycleState lifeCycleState = Utils.getLifecycleState(descriptionObject);
				List<Representation> representations = Utils.getRepresentations(descriptionObject.getPid(),version,browser,coreURL);
				
				IntellectualEntity intellectualEntity = null;
				if(descriptive!=null){
					if(lifeCycleState!=null){
						if(representations!=null){
							intellectualEntity = new IntellectualEntity.Builder().identifier(
							    	new Identifier(entityID))
							    	.descriptive(descriptive)
							    	.representations(representations)
							    	.lifecycleState(lifeCycleState)
							    	.build();
						}else{
							intellectualEntity = new IntellectualEntity.Builder().identifier(
							    	new Identifier(entityID))
							    	.descriptive(descriptive)
							    	.lifecycleState(lifeCycleState)
							    	.build();
						}
					}else{
						if(representations!=null){
							intellectualEntity = new IntellectualEntity.Builder().identifier(
							    	new Identifier(entityID))
							    	.descriptive(descriptive)
							    	.representations(representations)
							    	.build();
						}else{
							intellectualEntity = new IntellectualEntity.Builder().identifier(
							    	new Identifier(entityID))
							    	.descriptive(descriptive)
							    	.build();
						}
					}
					 
				}else{
					intellectualEntity = DataModelUtils.getInstance(browser,editor).descriptionObjectToIntellectualEntity(browser, descriptionObject,req);
				}

					
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ScapeMarshaller.newInstance().serialize(intellectualEntity, bos,useRef);
				
				logger.debug("OUTPUT:");
				String output = bos.toString("UTF-8");
				logger.debug(output);
				return Response.status(Status.OK).entity(output).header("Content-Type", MediaType.TEXT_XML).build();
			}else{
				logger.error("Unable to get helpers");
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to get helpers").type(MediaType.TEXT_PLAIN).build();
			}
			
		} catch(RODAServiceException rse){
			logger.error("Couldn't retrieve entity - " + rse.getMessage(), rse);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Couldn't retrieve entity - " + rse.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (NoSuchRODAObjectException nsroe) {
			logger.error("Entity " + entityID + " doesn't exist - " + nsroe.getMessage(), nsroe);
			return Response.status(Response.Status.NOT_FOUND).entity("Entity " + entityID + " doesn't exist - " + nsroe.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (JAXBException je) {
			logger.error("Error serializing entity " + entityID + " - " + je.getMessage(), je);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error serializing entity " + entityID + " - " + je.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (UnsupportedEncodingException uee) {
			logger.error("Error creating string from stream - " + uee.getMessage(), uee);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error creating string from stream - " + uee.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (URISyntaxException use) {
			logger.error("Error creating URI - " + use.getMessage(), use);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error creating URI - " + use.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (MalformedURLException e) {
			logger.error("Couldn't retrieve entity (MalformedURLException) - " + e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Couldn't retrieve entity (MalformedURLException) - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (IOException ioe) {
			logger.error("Couldn't retrieve entity (ConfigurationException) - " + ioe.getMessage(), ioe);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Couldn't retrieve entity (ConfigurationException) - " + ioe.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (EadCMetadataException eme) {
			logger.error("Error while getting entities:"+eme.getMessage(),eme);
			return Response.status(Status.NOT_FOUND).entity("Error while getting entities:"+eme.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}catch(NumberFormatException nfe){
			logger.error("Couldn't retrieve entity - " + nfe.getMessage(), nfe);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Couldn't retrieve entity - " + nfe.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}
	}
	
	
	@GET
	@Path("{entityID}")
	@Produces(MediaType.APPLICATION_XML)
	public Response getIntellectualEntity(@Context HttpServletRequest req, @PathParam("entityID") String entityID, @QueryParam("useReferences")@DefaultValue("no")String useReferences) {
		logger.debug("getIntellectualEntity(entityID='"+entityID+"', useReferences='"+useReferences+"')");
		return getIntellectualEntity(req, entityID, "-1", useReferences);
	}
	
	@PUT
	@Path("{entityID}")
	@Consumes("application/xml")
	public Response updateIntellectualEntity(@Context HttpServletRequest req,@PathParam("entityID") String entityID,byte[] binaryEntity) throws RODAServiceException, URISyntaxException {
		return updateIntellectualEntity(req, entityID, null, binaryEntity);
		
	}
	@PUT
	@Path("{entityID}/{planID}")
	@Consumes("application/xml")
	public Response updateIntellectualEntity(@Context HttpServletRequest req,@PathParam("entityID") String entityID,@PathParam("planID") String planID,byte[] binaryEntity) throws RODAServiceException, URISyntaxException {
		logger.debug("updateIntellectualEntity(entityID='"+entityID+"')");
		Response r = null;
		try {
			BrowserHelper browser = HelperUtils.getBrowserHelper(req,getClass());
			EditorHelper editor = HelperUtils.getEditorHelper(req,getClass());
			IngestHelper ingest = HelperUtils.getIngestHelper(req, getClass());
			Uploader uploader = HelperUtils.getUploader(req, getClass());
			IntellectualEntity entity = ScapeMarshaller.newInstance().deserialize(IntellectualEntity.class, new ByteArrayInputStream(binaryEntity));
			DescriptionObject doUpdated = IngestionUtils.getInstance().update(entityID,entity, editor,browser,uploader,ingest,planID);
			if(doUpdated==null){
				logger.error("doUpdated is null...");
			}
			IntellectualEntity updatedIntellectualEntity = DataModelUtils.getInstance(browser,editor).descriptionObjectToIntellectualEntity(HelperUtils.getBrowserHelper(req, getClass()),doUpdated,req);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ScapeMarshaller.newInstance().serialize(updatedIntellectualEntity, bos);
			r = Response.status(201).entity(bos.toString("UTF-8")).header("Content-Type", MediaType.TEXT_XML).build();
		} catch (DataConnectorException e) {
			logger.error("Error while updating IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while updating IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (BrowserException e) {
			logger.error("Error while updating IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while updating IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (ConfigurationException e) {
			logger.error("Error while updating IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while updating IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (FedoraClientException e) {
			logger.error("Error while updating IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while updating IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (EadCMetadataException e) {
			logger.error("Error while updating IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while updating IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (NoSuchRODAObjectException e) {
			logger.error("Error while updating IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while updating IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (IOException e) {
			logger.error("Error while updating IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while updating IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (JAXBException e) {
			logger.error("Error while updating IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while updating IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}

		return r;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.TEXT_PLAIN)
	public Response ingestIntellectualEntity(@Context HttpServletRequest req,byte[] binaryEntity) {
		return ingestIntellectualEntity(req, null,binaryEntity);
	}
	
	@POST
	@Path("{planID}")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.TEXT_PLAIN)
	public Response ingestIntellectualEntity(@Context HttpServletRequest req,@PathParam("planID") String planID,byte[] binaryEntity) {

		Response r = Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build();
		try {
			BrowserHelper browser = HelperUtils.getBrowserHelper(req,getClass());
			EditorHelper editor = HelperUtils.getEditorHelper(req,getClass());
			IngestHelper ingest = HelperUtils.getIngestHelper(req, getClass());
			Uploader uploader = HelperUtils.getUploader(req, getClass());
			IntellectualEntity entity = ScapeMarshaller.newInstance().deserialize(IntellectualEntity.class, new ByteArrayInputStream(binaryEntity));
			logger.debug("IntellectualEntity ID:"+entity.getIdentifier().getValue());
			String descriptionObjectId;
			descriptionObjectId = IngestionUtils.getInstance().ingest(entity, editor,ingest,browser,uploader,planID);
			logger.debug("INGESTED ENTITY PID:"+descriptionObjectId);
			if(descriptionObjectId!=null){
				r = Response.status(Status.CREATED).entity(descriptionObjectId).header("Content-Type", MediaType.TEXT_PLAIN).build();
			}
		} catch (DataConnectorException e) {
			logger.error("Error while ingesting IntellectualEntity:"+e.getMessage(),e);
		} catch (JAXBException e) {
			logger.error("Error while deserializing POST content");
		} catch (ConfigurationException e) {
			logger.error("Error while ingesting IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while ingesting IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (FedoraClientException e) {
			logger.error("Error while ingesting IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while ingesting IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (EditorException e) {
			logger.error("Error while ingesting IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while ingesting IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (MalformedURLException e) {
			logger.error("Error while ingesting IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while ingesting IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}
		return r;
		
	}
	
	
}
