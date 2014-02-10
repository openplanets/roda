package eu.scape_project.roda.core.connector;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("bitstream")
public class BitstreamResource {
	
	@GET
	@Path("{entityID}/{representationID}/{fileID}/{bitstreamID}")
	public Response getBitstream(@Context HttpServletRequest req, @PathParam("entityID") String entityID,@PathParam("representationID") String representationID,@PathParam("fileID") String fileID,@PathParam("bitstreamID") String bitstreamID) {
		return Response.status(Status.SERVICE_UNAVAILABLE).build();
	}
	
	@GET
	@Path("{entityID}/{representationID}/{fileID}/{bitstreamID}/{versionID}")
	public Response getVersionBitstream(@Context HttpServletRequest req, @PathParam("entityID") String entityID,@PathParam("representationID") String representationID,@PathParam("fileID") String fileID,@PathParam("bitstreamID") String bitstreamID,@PathParam("versionID") String versionID) {
		return Response.status(Status.SERVICE_UNAVAILABLE).build();
	}
}
