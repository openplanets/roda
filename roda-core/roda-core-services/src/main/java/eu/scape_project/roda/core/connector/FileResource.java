package eu.scape_project.roda.core.connector;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import pt.gov.dgarq.roda.core.BrowserHelper;
import pt.gov.dgarq.roda.core.common.NoSuchRODAObjectException;
import pt.gov.dgarq.roda.core.common.RODAServiceException;
import pt.gov.dgarq.roda.core.data.DescriptionObject;
import pt.gov.dgarq.roda.core.data.RepresentationFile;
import pt.gov.dgarq.roda.core.data.RepresentationObject;
import eu.scape_project.roda.core.connector.utils.HelperUtils;
import eu.scape_project.roda.core.connector.utils.Utils;

@Path("file")
public class FileResource {
	static final private Logger logger = Logger.getLogger(FileResource.class);
	@GET
	@Path("{entityID}/{representationID}/{fileID}")
	public Response getFile(@Context HttpServletRequest req, @PathParam("entityID") String entityID,@PathParam("representationID") String representationID,@PathParam("fileID") String fileID) {
		return getFile(req, entityID, representationID, fileID, "-1");
	}
	
	@Path("{entityID}/{representationID}/{fileID}/{versionID}")
	public Response getFile(@Context HttpServletRequest req, @PathParam("entityID") String entityID,@PathParam("representationID") String representationID,@PathParam("fileID") String fileID,@PathParam("versionID") String versionID) {
		
		logger.debug("getFile(entity='"+entityID+"', representation='"+representationID+"', file='"+fileID+"')");
		String coreURL = req.getRequestURL().substring(0,req.getRequestURL().indexOf("/rest/")); 
		Response r=null;
		try {
			BrowserHelper browser = HelperUtils.getBrowserHelper(req,getClass());
			if(browser!=null){
				DescriptionObject o = Utils.findById(entityID, browser);
				if(o==null){
					logger.debug("Entity not found...");
				}
				boolean representationFound=false, fileFound=false;
				URI fileURI = null;
				RepresentationObject[] representationObjects = browser.getDORepresentations(o.getPid());
				logger.debug("Number of RepresentationObjects:"+representationObjects.length);
				
				for(RepresentationObject ro : representationObjects){
					logger.debug("RO ID:"+ro.getId());
					if(ro.getId().equalsIgnoreCase(representationID) || ro.getPid().equalsIgnoreCase(representationID)){
						representationFound=true;
						if(ro.getRootFile()!=null){
							if(ro.getRootFile().getId().equalsIgnoreCase(fileID)){
								fileFound=true;
								fileURI =  new URI(coreURL+ro.getRootFile().getAccessURL());
								break;
							}
						}
						if(ro.getPartFiles()!=null){
							logger.debug("Number of PartFiles:"+ro.getPartFiles().length);
							for(RepresentationFile rp : ro.getPartFiles()){
								logger.debug("PartFile ID:"+rp.getId());
								if(rp.getId().equalsIgnoreCase(fileID)){
									fileFound=true;
									fileURI =  new URI(coreURL+rp.getAccessURL());
									break;
								}
							}
						}
						logger.debug("FileFound:"+fileFound);
						if(fileFound){
							break;
						}
					}
				}
				if(fileURI==null){
					logger.debug("FILE URI NULL...");
				}else{
					logger.debug("FILE URI:"+fileURI.toString());
				}
				if(!representationFound){
					logger.error("Couldn't retrieve representation with id "+representationID+" for entity "+entityID);
					r = Response.status(Response.Status.NOT_FOUND).entity("Couldn't retrieve representation with id "+representationID+" for entity "+entityID).type(MediaType.TEXT_PLAIN).build();
				}else if(!fileFound){
					logger.error("Couldn't retrieve file "+fileID+" for representation with id "+representationID+" for entity "+entityID);
					r = Response.status(Response.Status.NOT_FOUND).entity("Couldn't retrieve file "+fileID+" for representation with id "+representationID+" for entity "+entityID).type(MediaType.TEXT_PLAIN).build();
				}else{
					r = Response.temporaryRedirect(fileURI).build();
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
		} catch (URISyntaxException use) {
			logger.error("Error creating URI - " + use.getMessage(), use);
			r = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error creating URI - " + use.getMessage()).type(MediaType.TEXT_PLAIN).build();
		} 
		return r;
	}
}
