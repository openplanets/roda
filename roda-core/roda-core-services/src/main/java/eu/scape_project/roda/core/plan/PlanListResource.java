package eu.scape_project.roda.core.plan;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import eu.scape_project.model.plan.PlanDataCollection;

@Path("plan-list")
public class PlanListResource {
	static final private Logger logger = Logger.getLogger(PlanListResource.class);
	
	@GET
	public Response getPlanList() {
		try {
			PlanDataCollection collection = PlanManager.INSTANCE.getPlanDataCollection();
			return Response.ok().entity(collection).header("Content-Type", MediaType.TEXT_XML).build();
		}catch(PlanException pe){
			logger.error(
					"Couldn't fetch the plan list - " + pe.getMessage(), pe);
			return Response
					.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Couldn't fetch the plan list - "
							+ pe.getMessage()).type(MediaType.TEXT_PLAIN)
					.build();
		}
	}
}
