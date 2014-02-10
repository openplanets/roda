package eu.scape_project.roda.core.connector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;

import pt.gov.dgarq.roda.core.BrowserHelper;
import pt.gov.dgarq.roda.core.EditorHelper;
import pt.gov.dgarq.roda.core.common.BrowserException;
import pt.gov.dgarq.roda.core.common.InvalidDescriptionLevel;
import pt.gov.dgarq.roda.core.common.NoSuchRODAObjectException;
import pt.gov.dgarq.roda.core.common.RODAServiceException;
import pt.gov.dgarq.roda.core.data.DescriptionObject;
import pt.gov.dgarq.roda.core.metadata.eadc.EadCMetadataException;
import eu.scape_project.model.IntellectualEntity;
import eu.scape_project.model.IntellectualEntityCollection;
import eu.scape_project.roda.core.connector.utils.DataModelUtils;
import eu.scape_project.roda.core.connector.utils.HelperUtils;
import eu.scape_project.roda.core.connector.utils.Utils;
import eu.scape_project.util.ScapeMarshaller;


@Path("entity-list")
public class EntityListResource {
	static final private Logger logger = Logger.getLogger(EntityListResource.class);
	
	@POST
	@Consumes("text/uri-list")
	@Produces(MediaType.APPLICATION_XML)
	public Response getIntellectualEntities(@Context HttpServletRequest req, String uris) {
		logger.debug("getIntellectualEntities("+uris+")");
		Response r = null;	
			
		BrowserHelper browser = HelperUtils.getBrowserHelper(req,getClass());;
		EditorHelper editor = HelperUtils.getEditorHelper(req, getClass());


		if(browser!=null && editor!=null){
			List<IntellectualEntity> entities = new ArrayList<IntellectualEntity>();
			try{
				for(String uri : uris.split("\n")){
					DescriptionObject o = Utils.findById(uri, browser);
					if(o!=null){
						IntellectualEntity entity = DataModelUtils.getInstance(browser,editor).descriptionObjectToIntellectualEntity(browser, o,req);
						entities.add(entity);
					}else{
						throw new NoSuchRODAObjectException("URI "+uri+" is not recognized");
					}
				}
				IntellectualEntityCollection collection = new IntellectualEntityCollection(entities);
				
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ScapeMarshaller.newInstance().serialize(collection, bos);
				r = Response.ok().entity(bos.toString("UTF-8")).header("Content-Type", MediaType.TEXT_XML).build();
			}catch(BrowserException be){
				logger.error("Error while getting entities:"+be.getMessage(),be);
				r = Response.status(Status.NOT_FOUND).entity("Error while getting entities:"+be.getMessage()).type(MediaType.TEXT_PLAIN).build();
			} catch (InvalidDescriptionLevel idl) {
				logger.error("Error while getting entities:"+idl.getMessage(),idl);
				r = Response.status(Status.NOT_FOUND).entity("Error while getting entities:"+idl.getMessage()).type(MediaType.TEXT_PLAIN).build();
			} catch (NoSuchRODAObjectException nsroe) {
				logger.error("Error while getting entities:"+nsroe.getMessage(),nsroe);
				r = Response.status(Status.NOT_FOUND).entity("Error while getting entities:"+nsroe.getMessage()).type(MediaType.TEXT_PLAIN).build();
			} catch (RODAServiceException rse) {
				logger.error("Error while getting entities:"+rse.getMessage(),rse);
				r = Response.status(Status.NOT_FOUND).entity("Error while getting entities:"+rse.getMessage()).type(MediaType.TEXT_PLAIN).build();
			} catch (URISyntaxException use) {
				logger.error("Error while getting entities:"+use.getMessage(),use);
				r = Response.status(Status.NOT_FOUND).entity("Error while getting entities:"+use.getMessage()).type(MediaType.TEXT_PLAIN).build();
			} catch (JAXBException je) {
				logger.error("Error while getting entities:"+je.getMessage(),je);
				r = Response.status(Status.NOT_FOUND).entity("Error while getting entities:"+je.getMessage()).type(MediaType.TEXT_PLAIN).build();
			} catch (UnsupportedEncodingException uee) {
				logger.error("Error while getting entities:"+uee.getMessage(),uee);
				r = Response.status(Status.NOT_FOUND).entity("Error while getting entities:"+uee.getMessage()).type(MediaType.TEXT_PLAIN).build();
			} catch (IOException ioe) {
				logger.error("Error while getting entities:"+ioe.getMessage(),ioe);
				r = Response.status(Status.NOT_FOUND).entity("Error while getting entities:"+ioe.getMessage()).type(MediaType.TEXT_PLAIN).build();
			} catch (EadCMetadataException eme) {
				logger.error("Error while getting entities:"+eme.getMessage(),eme);
				r = Response.status(Status.NOT_FOUND).entity("Error while getting entities:"+eme.getMessage()).type(MediaType.TEXT_PLAIN).build();
			}
			
		}else{
			logger.error("Unable to get helpers");
			r = Response.status(Status.INTERNAL_SERVER_ERROR).entity("Unable to get helpers").type(MediaType.TEXT_PLAIN).build();
		}
		return r;	
	}
	
}
