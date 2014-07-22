package eu.scape_project.roda.core.plan;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
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
		logger.debug("deployPlan(id="+id+")");
		try {

			Plan plan = PlanManager.INSTANCE.getPlan(id);
			plan.storeData(src);
			PlanManager.INSTANCE.addPlanToIndex(plan);

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
		} catch (PlanAlreadyExistsException e) {
			logger.error("Couldn't deploy plan - Plan already exists.");
			return Response.status(Response.Status.BAD_REQUEST)
					.entity("Couldn't deploy plan - Plan already exists.")
					.type(MediaType.TEXT_PLAIN).build();
		} catch(Throwable t){
			logger.error("Error while deploying plan: "+t.getMessage(),t);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error while retrieving plan execution state - " + t.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		}

	}
	
	
	@DELETE
	public Response deletePlan(@PathParam("id") final String id){
		logger.debug("deletePlan(id="+id+")");
		try {
			boolean deleted = PlanManager.INSTANCE.deletePlan(id);
			
			if(deleted){
				return Response.ok().header("Content-Type", MediaType.TEXT_XML).build();
			}else{
				return Response.notModified().header("Content-Type", MediaType.TEXT_XML).build();
			}
		} catch (NoSuchPlanException e) {
			logger.error("Plan " + id + " doesn't exist - " + e.getMessage(), e);
			return Response
					.status(Response.Status.NOT_FOUND)
					.entity("Plan " + id + " doesn't exist - " + e.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		} catch (PlanException e) {
			logger.error("Couldn't retrieve plan - " + e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Couldn't retrieve plan - " + e.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		} catch (IOException e) {
			logger.error("Error deleting plan - " + e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Couldn't retrieve plan - " + e.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		} catch(Throwable t){
			logger.error("Error deleting plan: "+t.getMessage(),t);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error deleting plan - " + t.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		}
	}
	
	@GET
	public Response getPlan(@PathParam("id") final String id, @QueryParam("noData")@DefaultValue("false") final String noData) {
		logger.debug("getPlan(id="+id+")");
		try {

			Plan plan = PlanManager.INSTANCE.getPlan(id);
			
			boolean nd=false;
			if(noData.equalsIgnoreCase("true")){
				nd=true;
			}
			
			InputStream dataInputStream = plan.getDataInputStream(nd);

			logger.info("Plan "
					+ id
					+ " exists and data file successfully open. Sending response...");

			return Response.ok().entity(dataInputStream)
					.header("Content-Type", MediaType.TEXT_XML).build();

		} catch (NoSuchPlanException e) {
			logger.error("Plan " + id + " doesn't exist - " + e.getMessage(), e);
			return Response
					.status(Response.Status.NOT_FOUND)
					.entity("Plan " + id + " doesn't exist - " + e.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		} catch (PlanException e) {
			logger.error("Couldn't retrieve plan - " + e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Couldn't retrieve plan - " + e.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		} catch(Throwable t){
			logger.error("Error getting plan: "+t.getMessage(),t);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error getting plan - " + t.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		}
	}
}
