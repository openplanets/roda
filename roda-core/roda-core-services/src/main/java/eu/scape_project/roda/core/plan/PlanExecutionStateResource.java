package eu.scape_project.roda.core.plan;

/*
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import java.util.Date;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;

import eu.scape_project.model.plan.PlanExecutionState;

/**
 * JAX-RS Resource for Plan Execution States
 * 
 * @author Rui Castro
 * 
 */
@Path("/plan-execution-state")
public class PlanExecutionStateResource {
	static final private Logger logger = Logger
			.getLogger(PlanExecutionStateResource.class);

	@GET
	@Path("{id}")
	public Response retrievePlanExecutionState(
			@PathParam("id") final String planId, @Context UriInfo uriInfo) {

		try {

			Plan plan = PlanManager.INSTANCE.getPlan(planId);

			logger.info("Plan " + planId + " exists. Sending response...");

			return Response.ok().entity(plan.getPlanExecutionStateCollection())
					.header("Content-Type", MediaType.TEXT_XML).build();

		} catch (NoSuchPlanException e) {
			logger.error(
					"Plan " + planId + " doesn't exist - " + e.getMessage(), e);
			return Response
					.status(Response.Status.NOT_FOUND)
					.entity("Plan " + planId + " doesn't exist - "
							+ e.getMessage()).type(MediaType.TEXT_PLAIN)
					.build();
		} catch (PlanException e) {
			logger.error("Couldn't retrieve plan - " + e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Couldn't retrieve plan - " + e.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		}
	}

	@POST
	@Path("{id}")
	public Response addExecutionState(@PathParam("id") final String planId,
			PlanExecutionState state) {

		try {

			Plan plan = PlanManager.INSTANCE.getPlan(planId);

			logger.info("State has date " + state.getTimeStamp()
					+ ". Changing the date to NOW");
			state.getTimeStamp().setTime(new Date().getTime());
			logger.info("State date changed to " + state.getTimeStamp());

			plan.addPlanExecutionState(state);

			logger.info("Plan " + planId + " state added: "
					+ state.getState().toString());

			return Response.ok().build();

		} catch (NoSuchPlanException e) {
			logger.error(
					"Plan " + planId + " doesn't exist - " + e.getMessage(), e);
			return Response
					.status(Response.Status.NOT_FOUND)
					.entity("Plan " + planId + " doesn't exist - "
							+ e.getMessage()).type(MediaType.TEXT_PLAIN)
					.build();
		} catch (PlanException e) {
			logger.error("Couldn't retrieve plan - " + e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Couldn't retrieve plan - " + e.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		}

	}
}
