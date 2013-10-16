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

import eu.scape_project.roda.core.plan.PlanManager.SearchResults;

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

		if (!"searchRetrieve".equalsIgnoreCase(operation)) {
			logger.error("Operation '" + operation
					+ "' is not supported. Returning HTTP BAD_REQUEST");
			return Response.status(Response.Status.BAD_REQUEST)
					.entity("Operation '" + operation + "' is not supported.")
					.type(MediaType.TEXT_PLAIN).build();
		}

		if (StringUtils.isBlank(version)) {
			logger.error("Parameter 'version' is blank. Returning HTTP BAD_REQUEST");
			return Response
					.status(Response.Status.BAD_REQUEST)
					.entity("Parameter 'version' is mandatory and was not provided")
					.type(MediaType.TEXT_PLAIN).build();
		}
		if (!"1.2".equalsIgnoreCase(version)) {
			logger.warn("Requested version '" + version
					+ "' is not supported. Ignoring and using version 1.2");
		}

		if (StringUtils.isBlank(query)) {
			logger.error("Parameter 'query' is blank. Returning HTTP BAD_REQUEST");
			return Response
					.status(Response.Status.BAD_REQUEST)
					.entity("Parameter 'query' is mandatory and was not provided")
					.type(MediaType.TEXT_PLAIN).build();
		}

		try {

			if (StringUtils.isBlank(luceneQuery)) {

				luceneQuery = getOCLCTranslatorQuery(query);
				logger.info("CQL query successully parsed by OCLC translator: "
						+ luceneQuery);

			} else {
				logger.warn("Ignoring 'query' parameter, because 'luceneQuery' exists: "
						+ luceneQuery);
			}

			final SearchResults searchResults = PlanManager.INSTANCE
					.searchPlans(offset, limit, luceneQuery);

			/*
			 * create a stream from the plan XMLs to be written to the HTTP
			 * response the reponse does include the whole of the PLATO XML body
			 * so every hit is written from the repo to the httpresponse
			 */
			StreamingOutput entity = new StreamingOutput() {

				@Override
				public void write(OutputStream output) throws IOException {
					writeSRUHeader(output, searchResults.totalNumberOfResults);

					for (int i = 0; i < searchResults.results.size(); i++) {
						Plan plan = searchResults.results.get(i);
						try {
							writeSRURecord(output, plan, i);
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

				private void writeSRUHeader(OutputStream output, int size)
						throws UnsupportedEncodingException, IOException {
					final StringBuilder sru = new StringBuilder();
					sru.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
					sru.append("<searchRetrieveResponse>");
					sru.append("<version>1.2</version>");
					sru.append("<numberOfRecords>" + size
							+ "</numberOfRecords>");
					sru.append("<records>");
					output.write(sru.toString().getBytes("UTF-8"));
				}

				private void writeSRURecord(OutputStream output, Plan plan,
						int index) throws IOException, PlanException {

					final StringBuilder sru = new StringBuilder();
					sru.append("<record>");
					sru.append("<recordSchema>http://ifs.tuwien.ac.at/dp/plato</recordSchema>");
					sru.append("<recordIdentifier>" + plan.getId()
							+ "</recordIdentifier>");

					sru.append("<recordData>");
					// output.write(sru.toString().getBytes());
					// InputStream dataInputStream = plan.getDataInputStream();
					// IOUtils.copyLarge(dataInputStream, output);
					// dataInputStream.close();
					// sru.setLength(0);
					sru.append("</recordData>");

					sru.append("<recordPosition>" + index + "</recordPosition>");
					sru.append("</record>");
					output.write(sru.toString().getBytes());

				}

				private void writeSRUFooter(OutputStream output)
						throws IOException {
					final StringBuilder sru = new StringBuilder();
					sru.append("</records>");
					sru.append("</searchRetrieveResponse>");
					output.write(sru.toString().getBytes());
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

		BasicLuceneQueryTranslator translator = new BasicLuceneQueryTranslator(
				PlanManager.planSearchFields);
		Query query = translator.makeQuery(cqlNode);
		return query.toString();
	}
}