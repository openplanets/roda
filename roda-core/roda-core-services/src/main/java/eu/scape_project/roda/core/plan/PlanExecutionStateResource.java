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

import pt.gov.dgarq.roda.core.data.PluginInfo;
import pt.gov.dgarq.roda.core.data.Task;
import pt.gov.dgarq.roda.core.plugins.PluginManager;
import pt.gov.dgarq.roda.core.plugins.PluginManagerException;
import pt.gov.dgarq.roda.core.scheduler.RODASchedulerException;
import pt.gov.dgarq.roda.core.scheduler.SchedulerManager;
import eu.scape_project.model.plan.PlanExecutionState;
import eu.scape_project.model.plan.PlanExecutionState.ExecutionState;
import eu.scape_project.model.plan.PlanExecutionStateCollection;

/**
 * JAX-RS Resource for Plan Execution States
 * 
 * @author Rui Castro
 * 
 */
@Path("plan-execution-state")
public class PlanExecutionStateResource {
	static final private Logger logger = Logger
			.getLogger(PlanExecutionStateResource.class);

	@GET
	@Path("{id}")
	public Response retrievePlanExecutionState(
			@PathParam("id") final String planId, @Context UriInfo uriInfo) {
		logger.debug("!!!!!!!!!!!!!!!!!!retrievePlanExecutionState(planID="+planId+")");
		try {
			Plan plan = PlanManager.INSTANCE.getPlan(planId);
			logger.info("Plan " + planId + " exists. Sending response...");
			if(plan.getPlanExecutionStateCollection()==null){
				logger.error("plan.getPlanExecutionsStateCollection()==null...");
			}else{
				PlanExecutionStateCollection pesc = plan.getPlanExecutionStateCollection();
				logger.error("!!!!!!!!! "+plan.getPlanExecutionStateCollection().executionStates.size());
				for(PlanExecutionState pes : pesc.executionStates){
					logger.debug(pes.getTimeStamp()+" - "+pes.getState());
				}
			}
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
		} catch(Throwable t){
			logger.error(t.getMessage(),t);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error while retrieving plan execution state - " + t.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		}
	}

	@POST
	@Path("{id}")
	public Response addExecutionState(@PathParam("id") final String planId,
			PlanExecutionState state) {
		logger.debug("addExecutionState(planID="+planId+")");
		if(state==null){
			logger.debug("State null");
			return Response.status(406).entity("State NULL").build();
		}else{
			if(state.getState()==null){
				logger.debug("state.getState()==null");
			}else{
				logger.debug("state.getState()=='"+state.getState()+"'");
			}
			if(state.getTimeStamp()==null){
				logger.debug("state.getTimeStamp()==null");
			}else{
				logger.debug("state.getTimeStamp()=='"+state.getTimeStamp()+"'");
			}
			
			try {
				Plan plan = PlanManager.INSTANCE.getPlan(planId);
				logger.info("State has date " + state.getTimeStamp()
						+ ". Changing the date to NOW");
				if(state.getTimeStamp()==null){
					state.setTimeStamp(new Date());
				}
				state.getTimeStamp().setTime(new Date().getTime());
				logger.info("State date changed to " + state.getTimeStamp());
	
				logger.debug("Adding state to plan...");
				plan.addPlanExecutionState(state);
				PlanManager.INSTANCE.addPlanToIndex(plan);
				if(state.getState()!=null){
					logger.info("Plan " + planId + " state added: "+ state.getState().toString());
				}
	
				/*
				if (state.equals(ExecutionState.EXECUTION_IN_PROGRESS)) {
	
					try {
	
						PluginInfo executePlanPluginInfo = PluginManager
								.getDefaultPluginManager()
								.getPluginInfo(
										"pt.keep.roda.core.plugins.ExecutePlanPlugin");
	
						Task task = new Task("plan", "description", "admin",
								new Date(), 0, 1, true, false, false,
								executePlanPluginInfo);
	
						Task addedTask = SchedulerManager
								.getDefaultSchedulerManager().addTask(task);
	
					} catch (PluginManagerException e) {
						
					} catch (RODASchedulerException e) {
						
					}
	
				} else {
	
				}*/
				
	
	
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
			} catch (Throwable e) {
				logger.error("Error adding execution state - " + e.getMessage(), e);
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
						.entity("Couldn't retrieve plan - " + e.getMessage())
						.type(MediaType.TEXT_PLAIN).build();
			}
		}

	}
}
