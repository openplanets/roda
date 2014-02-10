package eu.scape_project.roda.core.connector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import pt.gov.dgarq.roda.core.BrowserHelper;
import pt.gov.dgarq.roda.core.EditorHelper;
import pt.gov.dgarq.roda.core.IngestHelper;
import pt.gov.dgarq.roda.core.common.BrowserException;
import pt.gov.dgarq.roda.core.common.EditorException;
import pt.gov.dgarq.roda.core.common.InvalidDescriptionLevel;
import pt.gov.dgarq.roda.core.common.NoSuchRODAObjectException;
import pt.gov.dgarq.roda.core.common.RODAServiceException;
import pt.gov.dgarq.roda.core.data.DescriptionObject;
import pt.gov.dgarq.roda.core.data.RepresentationFile;
import pt.gov.dgarq.roda.core.data.RepresentationObject;
import pt.gov.dgarq.roda.core.fedora.FedoraClientException;
import scape.dc.ElementContainer;
import scape.mix20.Mix;
import eu.scape_project.roda.core.connector.utils.DataConnectorException;
import eu.scape_project.roda.core.connector.utils.DataModelUtils;
import eu.scape_project.roda.core.connector.utils.HelperUtils;
import eu.scape_project.roda.core.connector.utils.IngestionUtils;
import eu.scape_project.roda.core.connector.utils.Utils;

@Path("metadata")
public class MetadataResource {
	static final private Logger logger = Logger.getLogger(MetadataResource.class);
	
	@GET
	@Path("{entityID}/{metadataID}")
	public Response getEntityMetadataRecord(@Context HttpServletRequest req, @PathParam("entityID") String entityID,@PathParam("metadataID") String metadataID) {
		Response r=null;
		try {
			BrowserHelper browser = HelperUtils.getBrowserHelper(req,getClass());
			EditorHelper editor = HelperUtils.getEditorHelper(req,getClass());
			if(browser!=null && editor!=null){
				DescriptionObject o = Utils.findById(entityID, browser);
				Object metadata = DataModelUtils.getInstance(browser, editor).extractMetadataFromDescriptionObject(o,metadataID);
				if(metadata instanceof ElementContainer){
					ByteArrayOutputStream bos = new ByteArrayOutputStream();
					ElementContainer dc = (ElementContainer)metadata;
					JAXBContext jc = JAXBContext.newInstance(ElementContainer.class);
					Marshaller m = jc.createMarshaller();
					m.marshal(dc, bos);
					String output = bos.toString("UTF-8");
					return Response.status(Status.OK).entity(output).header("Content-Type", MediaType.TEXT_XML).build();
				}else{
					return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to generate metadata").build();
				}
			}else{
				logger.error("Unable to get helpers");
				r = Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to get helpers").type(MediaType.TEXT_PLAIN).build();
			}
		} catch (JAXBException je) {
			logger.error("Error while generating output XML: " + je.getMessage(), je);
			r = Response.status(Response.Status.NOT_FOUND).entity("Error while generating output XML: " + je.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (UnsupportedEncodingException uee) {
			logger.error("Error while generating output XML: " + uee.getMessage(), uee);
			r = Response.status(Response.Status.NOT_FOUND).entity("Error while generating output XML: " + uee.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}
		
		return r;
	}
	
	@GET
	@Path("{entityID}/{representationID}/{metadataID}")
	public Response getRepresentationMetadataRecord(@Context HttpServletRequest req, @PathParam("entityID") String entityID,@PathParam("representationID") String representationID,@PathParam("metadataID") String metadataID) {
		Response r=null;
		try {
			BrowserHelper browser = HelperUtils.getBrowserHelper(req,getClass());
			EditorHelper editor = HelperUtils.getEditorHelper(req,getClass());
			if(browser!=null && editor!=null){
				DescriptionObject o = Utils.findById(entityID, browser);
				RepresentationObject representation = null;
				for(RepresentationObject ro : browser.getDORepresentations(o.getPid())){
					if(ro.getId().equalsIgnoreCase(representationID)){
						representation = ro;
						break;
					}	
				}
				if(representation==null){
					logger.error("Couldn't retrieve representation with id "+representationID+" for entity "+entityID);
					r = Response.status(Response.Status.NOT_FOUND).entity("Couldn't retrieve representation with id "+representationID+" for entity "+entityID).type(MediaType.TEXT_PLAIN).build();
				}else{
					Object metadata = DataModelUtils.getInstance(browser, editor).extractMetadataFromRepresentationObject(browser,representation,metadataID);
					String output = Utils.metadataToXML(metadata);
					if(output!=null){
						return Response.status(Status.OK).entity(output).header("Content-Type", MediaType.TEXT_XML).build();
					}else{
						return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to generate metadata").build();
					}
				}
			}else{
				logger.error("Unable to get helpers");
				r = Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to get helpers").type(MediaType.TEXT_PLAIN).build();
			}
		} catch(RODAServiceException rse){
			logger.error("Couldn't retrieve entity - " + rse.getMessage(), rse);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Couldn't retrieve entity - " + rse.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (NoSuchRODAObjectException nsroe) {
			logger.error("Entity " + entityID + " doesn't exist - " + nsroe.getMessage(), nsroe);
			r = Response.status(Response.Status.NOT_FOUND).entity("Entity " + entityID + " doesn't exist - " + nsroe.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} 
		return r;
	}
	
	
	@GET
	@Path("{entityID}/{representationID}/{fileID}/{metadataID}")
	public Response getFileMetadataRecord(@Context HttpServletRequest req, @PathParam("entityID") String entityID,@PathParam("representationID") String representationID,@PathParam("fileID") String fileID,@PathParam("metadataID") String metadataID) {
		Response r=null;
		try {
			BrowserHelper browser = HelperUtils.getBrowserHelper(req,getClass());
			EditorHelper editor = HelperUtils.getEditorHelper(req,getClass());
			if(browser!=null && editor!=null){
				DescriptionObject o = Utils.findById(entityID, browser);
				RepresentationFile file = null;
				for(RepresentationObject ro : browser.getDORepresentations(o.getPid())){
					if(ro.getId().equalsIgnoreCase(representationID)){
						for(RepresentationFile rf : ro.getPartFiles()){
							if(rf.getId().equalsIgnoreCase(fileID)){
								file = rf;
								break;
							}
						}
						if(file!=null){
							break;
						}
					}	
				}
			
				if(file==null){
					logger.error("Couldn't retrieve representation file with id "+metadataID+" for representation "+representationID +" of entity "+entityID);
					r = Response.status(Response.Status.NOT_FOUND).entity("Couldn't retrieve representation file with id "+metadataID+" for representation "+representationID +" of entity "+entityID).type(MediaType.TEXT_PLAIN).build();
				}{
					Object metadata = DataModelUtils.getInstance(browser, editor).extractMetadataFromRepresentationFile(file,metadataID);
					if(metadata instanceof Mix){
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						Mix mix = (Mix)metadata;
						JAXBContext jc = JAXBContext.newInstance(Mix.class);
						Marshaller m = jc.createMarshaller();
						m.marshal(mix, bos);
						String output = bos.toString("UTF-8");
						return Response.status(Status.OK).entity(output).header("Content-Type", MediaType.TEXT_XML).build();
					}else{
						return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to generate metadata").build();
					}
				}
			}else{
				logger.error("Unable to get helpers");
				r = Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to get helpers").type(MediaType.TEXT_PLAIN).build();
			}
		} catch(RODAServiceException rse){
			logger.error("Couldn't retrieve entity - " + rse.getMessage(), rse);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Couldn't retrieve entity - " + rse.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (NoSuchRODAObjectException nsroe) {
			logger.error("Entity " + entityID + " doesn't exist - " + nsroe.getMessage(), nsroe);
			r = Response.status(Response.Status.NOT_FOUND).entity("Entity " + entityID + " doesn't exist - " + nsroe.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (JAXBException je) {
			logger.error("Error while generating output XML: " + je.getMessage(), je);
			r = Response.status(Response.Status.NOT_FOUND).entity("Error while generating output XML: " + je.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (UnsupportedEncodingException uee) {
			logger.error("Error while generating output XML: " + uee.getMessage(), uee);
			r = Response.status(Response.Status.NOT_FOUND).entity("Error while generating output XML: " + uee.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}
		return r;
	}
	

	
	@PUT
	@Path("{entityID}/{metadataID}")
	public Response updateIntellectualEntityMetadata(@Context HttpServletRequest req, @PathParam("entityID") String entityID,@PathParam("metadataID") String metadataID, byte[] binaryMetadata) {
		logger.debug("updateIntellectualEntity(entityID='"+entityID+"')");
		Response r = null;
		try {
			BrowserHelper browser = HelperUtils.getBrowserHelper(req,getClass());
			EditorHelper editor = HelperUtils.getEditorHelper(req,getClass());
			IngestHelper ingest = HelperUtils.getIngestHelper(req, getClass());
			Object metadata = Utils.createMetadata(binaryMetadata);
			DescriptionObject descriptionObject = Utils.findById(entityID, browser);
			descriptionObject = browser.getDescriptionObject(descriptionObject.getPid());
			if(metadata!=null){
				if(descriptionObject!=null){
					logger.debug("COUNTRY:"+descriptionObject.getCountryCode());
					
					IngestionUtils.getInstance().updateDescriptionObjectMetadata(descriptionObject,metadata,editor,browser,ingest);
					r = Response.ok().build();
				}else{
					logger.error("DescriptionObject with ID='"+entityID+"' not found");
					r = Response.status(Response.Status.NOT_FOUND).entity("DescriptionObject with ID='"+entityID+"' not found").type(MediaType.TEXT_PLAIN).build();
				}

			}else{
				logger.error("Unable to deserialize metadata");
				r = Response.status(Response.Status.BAD_REQUEST).entity("Unable to deserialize metadata").type(MediaType.TEXT_PLAIN).build();
			}
			
		} catch (DataConnectorException e) {
			logger.error("Error while updating IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while updating IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (ConfigurationException e) {
			logger.error("Error while updating IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while updating IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (FedoraClientException e) {
			logger.error("Error while updating IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while updating IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (IOException e) {
			logger.error("Error while updating IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while updating IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (EditorException e) {
			logger.error("Error while updating IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while updating IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (InvalidDescriptionLevel e) {
			logger.error("Error while updating IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while updating IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (BrowserException e) {
			logger.error("Error while updating IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while updating IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} catch (NoSuchRODAObjectException e) {
			logger.error("Error while updating IntellectualEntity - " + e.getMessage(), e);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while updating IntellectualEntity - " + e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}

		return r;
		
		
	}
}
