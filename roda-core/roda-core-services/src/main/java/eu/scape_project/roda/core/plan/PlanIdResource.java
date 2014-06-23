package eu.scape_project.roda.core.plan;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

@Path("plan-id/reserve")
public class PlanIdResource {
	static final private Logger logger = Logger.getLogger(PlanIdResource.class);

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response reserve() {
		logger.debug("reserve()");

		try {

			Plan plan = PlanManager.INSTANCE.createPlan();
			return Response.ok(plan.getId()).build();

		} catch (PlanException e) {
			logger.error(
					"Couldn't reserve a plan identifier - " + e.getMessage(), e);
			return Response
					.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Couldn't reserve a plan identifier - "
							+ e.getMessage()).type(MediaType.TEXT_PLAIN)
					.build();
		} catch(Throwable t){
			logger.error("Error while reserving plan ID: "+t.getMessage(),t);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error while reserving plan ID - " + t.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		}

	}
}
