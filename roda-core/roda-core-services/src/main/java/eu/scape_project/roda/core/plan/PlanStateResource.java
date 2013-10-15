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

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

/**
 * JAX-RS Resource for Plan Execution States
 * 
 * @author Rui Castro
 */
@Path("plan-state")
public class PlanStateResource {
	static final private Logger logger = Logger
			.getLogger(PlanStateResource.class);

	@GET
	@Path("{id}")
	public Response retrievePlanLifecycleState(@PathParam("id") final String id)
			throws PlanException {

		try {

			Plan plan = PlanManager.INSTANCE.getPlan(id);

			logger.info("Plan " + id + " exists and enabled="
					+ plan.isEnabled() + ". Sending response...");

			return Response.ok(plan.isEnabled() ? "ENABLED" : "DISABLED",
					MediaType.TEXT_PLAIN).build();

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
		}

	}

	@PUT
	@Path("{id}/{state}")
	public Response updateLifecycleState(@PathParam("id") final String id,
			@PathParam("state") String state) throws PlanException {

		if (state == null) {
			throw new PlanException("Illegal state: '" + state
					+ "' only one of [ENABLED,DISABLED] is allowed");
		} else {

			state = state.trim().toUpperCase();

			if (!"ENABLED".equals(state) && !"DISABLED".equals(state)) {
				throw new PlanException("Illegal state: '" + state
						+ "' only one of [ENABLED,DISABLED] is allowed");
			}
		}

		try {

			Plan plan = PlanManager.INSTANCE.getPlan(id);

			if (plan.isEnabled() == "ENABLED".equals(state)) {

				logger.info("Plan is already " + state + ". Ignoring update");

			} else {

				plan.setEnabled(!plan.isEnabled());
				plan.store();

				PlanManager.INSTANCE.addPlanToIndex(plan);

				logger.info("Plan status updated to " + state);
			}

			return Response.ok().build();

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
		}
	}
}