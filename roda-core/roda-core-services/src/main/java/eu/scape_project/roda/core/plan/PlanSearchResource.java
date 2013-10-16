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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;

/**
 * JAX-RS Resource for Plan search
 * 
 * @author frank asseg
 * @author Rui Castro
 * 
 */
@Path("/plan/sru")
public class PlanSearchResource {
	static final private Logger logger = Logger
			.getLogger(PlanSearchResource.class);

	@GET
	@Produces(MediaType.TEXT_XML)
	public Response searchPlans(
			@QueryParam("operation") final String operation,
			@QueryParam("query") final String query,
			@QueryParam("luceneQuery") String luceneQuery,
			@QueryParam("version") final String version,
			@QueryParam("startRecord") final int offset,
			@QueryParam("maximumRecords") @DefaultValue("25") final int limit) {

		try {

			if (StringUtils.isBlank(luceneQuery)) {

				luceneQuery = getOCLCTranslatorQuery(query);
				logger.info("CQL query successully parsed by OCLC translator: "
						+ luceneQuery);

			} else {
				logger.warn("Ignoring 'query' parameter, because 'luceneQuery' exists: "
						+ luceneQuery);
			}

			final List<Plan> plans = PlanManager.INSTANCE.searchPlans(offset,
					limit, luceneQuery);

			/*
			 * create a stream from the plan XMLs to be written to the HTTP
			 * response the reponse does include the whole of the PLATO XML body
			 * so every hit is written from the repo to the httpresponse
			 */
			StreamingOutput entity = new StreamingOutput() {

				@Override
				public void write(OutputStream output) throws IOException {
					writeSRUHeader(output, plans.size());

					for (Plan plan : plans) {
						try {
							writeSRURecord(output, plan);
						} catch (PlanException e) {
							logger.debug(
									"Error writing SRU Record - "
											+ e.getMessage(), e);
							throw new IOException("Error writing SRU Record - "
									+ e.getMessage(), e);
						}
					}

					writeSRUFooter(output);
				}

				private void writeSRURecord(OutputStream output, Plan plan)
						throws IOException, PlanException {

					final StringBuilder sru = new StringBuilder();
					sru.append("<srw:record>");
					sru.append("<srw:recordSchema>http://scapeproject.eu/schema/plato</srw:recordSchema>");
					sru.append("<recordIdentifier>" + plan.getId()
							+ "</recordIdentifier>");

					// sru.append("<srw:recordData>");
					// output.write(sru.toString().getBytes());
					// InputStream dataInputStream = plan.getDataInputStream();
					// IOUtils.copyLarge(dataInputStream, output);
					// dataInputStream.close();
					// sru.setLength(0);
					// sru.append("</srw:recordData>");

					sru.append("</srw:record>");

					output.write(sru.toString().getBytes());

				}

				private void writeSRUFooter(OutputStream output)
						throws IOException {
					final StringBuilder sru = new StringBuilder();
					sru.append("</srw:records>");
					sru.append("</srw:searchRetrieveResponse>");
					output.write(sru.toString().getBytes());
				}

				private void writeSRUHeader(OutputStream output, int size)
						throws UnsupportedEncodingException, IOException {
					final StringBuilder sru = new StringBuilder();
					sru.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
					sru.append("<srw:searchRetrieveResponse xmlns:srw=\"http://scapeproject.eu/srw/\">");
					sru.append("<srw:numberOfRecords>" + size
							+ "</srw:numberOfRecords>");
					sru.append("<srw:records>");
					output.write(sru.toString().getBytes("UTF-8"));
				}

			};

			return Response.ok(entity, MediaType.TEXT_XML).build();

		} catch (PlanException e) {
			logger.error("Couldn't retrieve plan - " + e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Couldn't retrieve plan - " + e.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		} catch (CQLParseException e) {
			logger.error("Couldn't parse query - " + e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Couldn't parse query - " + e.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		} catch (IOException e) {
			logger.error("Couldn't parse query - " + e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Couldn't parse query - " + e.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		} catch (ParseException e) {
			logger.error(
					"Error parsing CQL query with OCLC translator - "
							+ e.getMessage(), e);
			return Response
					.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error parsing CQL query with OCLC translator - "
							+ e.getMessage()).type(MediaType.TEXT_PLAIN)
					.build();
		}
	}

	@GET
	@Path("reindex")
	@Produces(MediaType.TEXT_PLAIN)
	public Response reindex() {
		try {

			int indexedPlansCount = PlanManager.INSTANCE.reindexPlans();

			return Response.ok(
					"Successfully indexed " + indexedPlansCount + " plans.",
					MediaType.TEXT_PLAIN).build();

		} catch (PlanException e) {
			logger.error("Error reindexing plans - " + e.getMessage(), e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
					.entity("Error reindexing plans - " + e.getMessage())
					.type(MediaType.TEXT_PLAIN).build();
		}
	}

	// private String getKBCQLtoLuceneQuery(String cqlQuery) throws IOException
	// {
	// String query = CQLtoLucene.translate(cqlQuery);
	// return query;
	// }

	private String getOCLCTranslatorQuery(String cqlQuery)
			throws CQLParseException, ParseException, IOException {

		CQLParser cqlParser = new CQLParser();
		CQLNode cqlNode = cqlParser.parse(cqlQuery);

		BasicLuceneQueryTranslator translator = new BasicLuceneQueryTranslator();
		Query query = translator.makeQuery(cqlNode);
		return query.toString();
	}
}