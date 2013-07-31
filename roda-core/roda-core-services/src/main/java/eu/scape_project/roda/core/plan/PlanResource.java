package eu.scape_project.roda.core.plan;

import java.io.InputStream;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;

@Path("plan/{id}")
public class PlanResource {
	static final private Logger logger = Logger.getLogger(PlanResource.class);

	// @Produces(MediaType.TEXT_XML)
	// @Consumes(MediaType.TEXT_XML)
	// @PUT
	// public Response deploy(@PathParam("id") String id, Plans plansDocument) {
	// logger.trace(String.format("deploy(id=%s)", id));
	//
	// try {
	//
	// Plan plan = PlanManager.INSTANCE.getPlan(id);
	// plan.storeData(plansDocument);
	//
	// return Response.ok(plansDocument).build();
	//
	// } catch (PlanException e) {
	// logger.error("Couldn't deploy plan - " + e.getMessage(), e);
	// return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
	// .entity("Couldn't deploy plan - " + e.getMessage())
	// .type(MediaType.TEXT_PLAIN).build();
	// }
	//
	// }

	// @Path("{id}")
	@PUT
	public Response deployPlan(@PathParam("id") final String id,
			@Context UriInfo uriInfo, final InputStream src) {

		try {

			Plan plan = PlanManager.INSTANCE.getPlan(id);
			plan.storeData(src);

			logger.info("Plan successfully deployed to "
					+ uriInfo.getRequestUri().toASCIIString());

			return Response.created(uriInfo.getAbsolutePath())
					.entity(uriInfo.getAbsolutePath().toASCIIString())
					.header("Content-Type", "text/plain").build();

		} catch (PlanException e) {
			logger.error("Couldn't deploy plan - " + e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Couldn't deploy plan - " + e.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		}

	}
}