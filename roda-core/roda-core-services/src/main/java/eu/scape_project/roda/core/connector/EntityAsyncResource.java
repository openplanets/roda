package eu.scape_project.roda.core.connector;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
import pt.gov.dgarq.roda.core.common.EditorException;
import pt.gov.dgarq.roda.core.fedora.FedoraClientException;
import eu.scape_project.model.IntellectualEntity;
import eu.scape_project.roda.core.connector.utils.HelperUtils;
import eu.scape_project.roda.core.connector.utils.IngestionUtils;
import eu.scape_project.roda.core.connector.utils.Uploader;
import eu.scape_project.util.ScapeMarshaller;


@Path("entity-async")
public class EntityAsyncResource {
	static final private Logger logger = Logger.getLogger(EntityAsyncResource.class);
	
	@POST
	public Response ingestIntellectualEntity(@Context HttpServletRequest req,byte[] binaryEntity,@QueryParam("planID")String planID) {
		Response r = Response.status(Status.UNSUPPORTED_MEDIA_TYPE).build();
		BrowserHelper browser=null;
		EditorHelper editor = null;
		IngestHelper ingest = null;	
		Uploader uploader = null;
		try {
			browser = HelperUtils.getBrowserHelper(req,getClass());
			editor = HelperUtils.getEditorHelper(req, getClass());
			ingest = HelperUtils.getIngestHelper(req, getClass());
			uploader = HelperUtils.getUploader(req, getClass());
			IntellectualEntity entity = ScapeMarshaller.newInstance().deserialize(IntellectualEntity.class, new ByteArrayInputStream(binaryEntity));
			logger.debug("IntellectualEntity ID:"+entity.getIdentifier().getValue());
			
			String statusID = IngestionUtils.getInstance().async(entity,editor,ingest,browser,uploader,planID);
			r = Response.ok().entity(statusID).header("Content-Type", MediaType.TEXT_PLAIN).build();
		}catch(JAXBException je){
			logger.error("Error while deserializing POST content");
		} catch (ConfigurationException e) {
			logger.error("Error while creating Helper");
		} catch (FedoraClientException e) {
			logger.error("Error while creating Helper");
		} catch (MalformedURLException e) {
			logger.error("Error while creating Helper");
		} catch (EditorException e) {
			logger.error("Error while creating Helper");
		}
		return r;
	}

}
