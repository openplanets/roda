package eu.scape_project.roda.core.connector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLParseException;
import org.z3950.zing.cql.CQLParser;

import pt.gov.dgarq.roda.core.BrowserHelper;
import pt.gov.dgarq.roda.core.EditorHelper;
import pt.gov.dgarq.roda.core.data.SearchResult;
import pt.gov.dgarq.roda.core.data.SearchResultObject;
import pt.gov.dgarq.roda.core.data.search.EadcSearchFields;
import pt.gov.dgarq.roda.core.metadata.eadc.EadCHelper;
import eu.scape_project.model.IntellectualEntity;
import eu.scape_project.model.IntellectualEntityCollection;
import eu.scape_project.roda.core.BasicLuceneQueryTranslator;
import eu.scape_project.roda.core.connector.utils.DataModelUtils;
import eu.scape_project.roda.core.connector.utils.HelperUtils;
import eu.scape_project.util.ScapeMarshaller;

@Path("sru")
public class SruResource {
	static final private Logger logger = Logger.getLogger(SruResource.class);
	
	public enum SearchType {
	    INTELLECTUALENTITY, FILE
	}
	
	static final  protected String[] intellectualEntitySearchFields = new String[] { 
		EadcSearchFields.ACCESSRESTRICT,
		EadcSearchFields.ACCRUALS,
		EadcSearchFields.ACQINFO,
		EadcSearchFields.ACQINFO_DATE,
		EadcSearchFields.ACQINFO_NUM,
		EadcSearchFields.APPRAISAL,
		EadcSearchFields.ARRANGEMENT,
		EadcSearchFields.BIBLIOGRAPHY,
		EadcSearchFields.BIOGHIST,
		EadcSearchFields.COMPLETE_REFERENCE,
		EadcSearchFields.CUSTODHIST,
		EadcSearchFields.LANGMATERIAL_LANGUAGE,
		EadcSearchFields.LEVEL,
		EadcSearchFields.MATERIALSPEC,
		EadcSearchFields.NOTE,
		EadcSearchFields.ORIGINATION,
		EadcSearchFields.OTHERFINDAID,
		EadcSearchFields.PHYSDESC,
		EadcSearchFields.PHYSDESC_DATE,
		EadcSearchFields.PHYSDESC_DATE_FINAL,
		EadcSearchFields.PHYSDESC_DATE_INITIAL,
		EadcSearchFields.PHYSDESC_DIMENSIONS,
		EadcSearchFields.PHYSDESC_DIMENSIONS_UNIT,
		EadcSearchFields.PHYSDESC_EXTENT,
		EadcSearchFields.PHYSDESC_EXTENT_UNIT,
		EadcSearchFields.PHYSDESC_PHYSFACET,
		EadcSearchFields.PHYSDESC_PHYSFACET_UNIT,
		EadcSearchFields.PHYSTECH,
		EadcSearchFields.PREFERCITE,
		EadcSearchFields.PROCESSINFO,
		EadcSearchFields.RELATEDMATERIAL,
		EadcSearchFields.REPOSITORYCODE,
		EadcSearchFields.SCOPECONTENT,
		EadcSearchFields.UNITDATE,
		EadcSearchFields.UNITDATE_FINAL,
		EadcSearchFields.UNITDATE_INITIAL,
		EadcSearchFields.UNITID,
		EadcSearchFields.UNITTITLE,
		EadcSearchFields.USERESTRICT
		};
	static final  protected String[] fileSearchFields = intellectualEntitySearchFields;
	
	@GET
	@Path("entities")
	@Produces(MediaType.APPLICATION_XML)
	public Response searchIntellectualEntities(@Context final HttpServletRequest req, 
			@QueryParam("operation") final String operation,
			@QueryParam("query") final String query,
			@QueryParam("version") final String version,
			@QueryParam("startRecord") final int startRecord,
			@QueryParam("maximumRecords") @DefaultValue("25") final int maximumRecords) {
		logger.debug("searchIntellectualEntities(operation='"+operation+"',query='"+query+"', version='"+version+"',startRecord='"+startRecord+"',maximumRecord='"+maximumRecords+"'");
		Response r = null;
		
		try {
			final BrowserHelper browser = HelperUtils.getBrowserHelper(req,getClass());
			final EditorHelper editor = HelperUtils.getEditorHelper(req,getClass());
			String luceneQuery = getOCLCTranslatorQuery(query,SearchType.INTELLECTUALENTITY);
			logger.debug("LUCENE QUERY:"+luceneQuery);
			
			final SearchResult searchResults = browser.getFedoraClientUtility().getFedoraGSearch().search(luceneQuery, startRecord, maximumRecords, 0, 0);

			logger.debug("NUMBER OF RESULTS:"+searchResults.getHitTotal());
			List<IntellectualEntity> intellectualEntities = new ArrayList<IntellectualEntity>();
			for(SearchResultObject sro : searchResults.getSearchResultObjects()){
        		logger.debug("Converting DO '"+sro.getDescriptionObject().getId()+"' to IE");
				IntellectualEntity ie = DataModelUtils.getInstance(browser,editor).descriptionObjectToIntellectualEntity(browser, sro.getDescriptionObject(), req);
				intellectualEntities.add(ie);
			}
			
			
			IntellectualEntityCollection iec = new IntellectualEntityCollection(intellectualEntities);
			logger.debug("Number of IE in IntellectualEntityCollection: "+iec.getEntities().size());
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ScapeMarshaller.newInstance().serialize(iec, bos,false);
			
			
			String output = bos.toString("UTF-8");
			logger.debug("OUTPUT:\n");
			logger.debug(output);
			return Response.status(Status.OK).entity(output).header("Content-Type", MediaType.TEXT_XML).build();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return r;
		
	}
	
	
	@GET
	@Path("files")
	@Produces(MediaType.APPLICATION_XML)
	public Response searchFiles(@Context HttpServletRequest req, 
			@QueryParam("operation") final String operation,
			@QueryParam("query") final String query,
			@QueryParam("version") final String version,
			@QueryParam("startRecord") final int startRecord,
			@QueryParam("maximumRecords") @DefaultValue("25") final int maximumRecords) {
		return Response.status(Status.SERVICE_UNAVAILABLE).build();

	}
	
	
	
	private String getOCLCTranslatorQuery(String cqlQuery,SearchType st)
			throws CQLParseException, ParseException, IOException {

		String[] fields = intellectualEntitySearchFields;
		if(st==SearchType.FILE){
			fields = fileSearchFields;
		}
		CQLParser cqlParser = new CQLParser();
		CQLNode cqlNode = cqlParser.parse(cqlQuery);

		BasicLuceneQueryTranslator translator = new BasicLuceneQueryTranslator(fields);
		Query query = translator.makeQuery(cqlNode);
		return query.toString();
	}
	
	
	 private void writeSRURecord(Object o, OutputStream output)
	            throws IOException {
	        final StringBuilder sru = new StringBuilder();
	        sru.append("<srw:record>");
	        sru.append("<srw:recordSchema>http://scapeproject.eu/schema/plato</srw:recordSchema>");
	        sru.append("<srw:recordData>");
	        output.write(sru.toString().getBytes());
	        try {
	        	ScapeMarshaller.newInstance().serialize(o, output);
	        } catch (JAXBException e) {
	            throw new IOException(e);
	        }
	        sru.setLength(0);
	        sru.append("</srw:recordData>");
	        sru.append("</srw:record>");
	        output.write(sru.toString().getBytes());
	    }

	    private void writeSRUFooter(OutputStream output) throws IOException {
	        final StringBuilder sru = new StringBuilder();
	        sru.append("</srw:records>");
	        sru.append("</srw:searchRetrieveResponse>");
	        output.write(sru.toString().getBytes());
	    }

	    private void writeSRUHeader(OutputStream output, int size)
	            throws IOException {
	        final StringBuilder sru = new StringBuilder();
	        sru.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
	        sru.append("<srw:searchRetrieveResponse xmlns:srw=\"http://scapeproject.eu/srw/\">");
	        sru.append("<srw:numberOfRecords>" + size + "</srw:numberOfRecords>");
	        sru.append("<srw:records>");
	        output.write(sru.toString().getBytes("UTF-8"));
	    }
}
