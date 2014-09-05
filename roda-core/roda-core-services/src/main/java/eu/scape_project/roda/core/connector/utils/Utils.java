package eu.scape_project.roda.core.connector.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import pt.gov.dgarq.roda.core.BrowserHelper;
import pt.gov.dgarq.roda.core.EditorHelper;
import pt.gov.dgarq.roda.core.common.NoSuchRODAObjectException;
import pt.gov.dgarq.roda.core.data.DescriptionObject;
import pt.gov.dgarq.roda.core.data.RODAObject;
import pt.gov.dgarq.roda.core.data.RepresentationFile;
import pt.gov.dgarq.roda.core.data.RepresentationObject;
import pt.gov.dgarq.roda.core.data.SearchParameter;
import pt.gov.dgarq.roda.core.data.SearchResult;
import pt.gov.dgarq.roda.core.data.SearchResultObject;
import pt.gov.dgarq.roda.core.data.eadc.DescriptionLevel;
import pt.gov.dgarq.roda.core.data.search.DefaultSearchParameter;
import pt.gov.dgarq.roda.core.data.search.EadcSearchFields;
import scape.dc.ElementContainer;
import scape.mix20.Mix;
import scape.premis.PremisComplexType;
import scape.text.TextMD;
import eu.scape_project.model.File;
import eu.scape_project.model.Identifier;
import eu.scape_project.model.LifecycleState;
import eu.scape_project.model.LifecycleState.State;
import eu.scape_project.model.Representation;
import fedora.server.types.gen.Datastream;

public class Utils {
	static final private Logger logger = Logger.getLogger(Utils.class);
	
	
	
	
	
	
	
	
	public static Comparator<SearchResultObject> dateComparator = new Comparator<SearchResultObject>() {

        @Override
        public int compare(SearchResultObject sro1, SearchResultObject sro2) {
            return sro1.getDescriptionObject().getLastModifiedDate().compareTo(sro2.getDescriptionObject().getLastModifiedDate());
        }
    };
    
	
    public static DescriptionObject findById(String id,BrowserHelper helper) throws NoSuchRODAObjectException{
    	return findById(id, -1, helper);
    }
    
    
    
	public static DescriptionObject findById(String id,int version,BrowserHelper helper) throws NoSuchRODAObjectException{
		try{
			return helper.getDescriptionObject(id);
		}catch(Throwable t){
			logger.debug("Unable to get findDO with PID "+id);
		}
		try{
			SearchParameter parameter = new DefaultSearchParameter(
					new String[] { EadcSearchFields.UNITID },
					id,
					DefaultSearchParameter.MATCH_ALL_WORDS);
			SearchResult sr =  helper.getFedoraClientUtility().getFedoraGSearch().advancedSearch(new SearchParameter[] {parameter}, 0, 1000, 500, 500);
			SearchResultObject[] results = sr.getSearchResultObjects();
			if(results==null || results.length==0){
				throw new NoSuchRODAObjectException("The Description with id/pid '"+id+"' and version '"+version+"' doesn't exist...");
			}else{
				if(version==-1){
					return results[0].getDescriptionObject();
				}else{
					String pid = results[0].getDescriptionObject().getPid();
					Datastream[] datastreamHistory = helper.getFedoraClientUtility().getAPIM().getDatastreamHistory(pid, "EAD-C");
					if(datastreamHistory.length<version){
						throw new NoSuchRODAObjectException("The Description with id/pid '"+id+"' and version '"+version+"' doesn't exist...");
					}else{
						Datastream datastreamVersion = datastreamHistory[datastreamHistory.length-version];
						return helper.getDescriptionObject(pid,datastreamVersion.getCreateDate());
					}
				}
			}
		}catch(Exception e){
			logger.error(e.getMessage(),e);
		}
		throw new NoSuchRODAObjectException("The Description with id/pid '"+id+"' and version '"+version+"' doesn't exist...");
	}




	public static String getDefaultScapeParentID(BrowserHelper browser,EditorHelper helper) {
		String defaultId = null;
		try{
			logger.debug("getDefaultScapeParentID()");
			DescriptionObject defaultScapeDescriptionObject = null;
			SearchParameter parameterTitle = new DefaultSearchParameter(
					new String[] { EadcSearchFields.UNITTITLE },
					"Connector Fond",
					DefaultSearchParameter.MATCH_ALL_WORDS);
			
			SearchResult sr =  browser.getFedoraClientUtility().getFedoraGSearch().advancedSearch(new SearchParameter[] {parameterTitle}, 0, 1, 500, 500);
			logger.debug("getDefaultScapeParentID Result size:"+sr.getResultCount());
			if(sr!=null){
				if(sr.getHitTotal()>0){
					defaultScapeDescriptionObject = sr.getSearchResultObjects()[0].getDescriptionObject();
				}
			}
			
			
			if(defaultScapeDescriptionObject==null){
				DescriptionObject fondsDO = new DescriptionObject();
				fondsDO.setLevel(DescriptionLevel.FONDS);
				fondsDO.setCountryCode("PT");
				fondsDO.setRepositoryCode("SCAPE");
				fondsDO.setId("SCAPE");
				fondsDO.setTitle("Connector Fond");
				fondsDO.setDateInitial("1000");
				fondsDO.setDateFinal("2500");
				fondsDO.setOrigination("Connector API");
				fondsDO.setScopecontent("-");
				String fondsPID = helper.createDescriptionObject(fondsDO);
				fondsDO.setState(RODAObject.STATE_ACTIVE);
				helper.modifyDescriptionObject(fondsDO);
				
				defaultId = fondsPID;
			}else{
				defaultId = defaultScapeDescriptionObject.getPid(); 
			}
		}catch(Exception e){
			logger.error("Error while getting default SCAPE DO: "+e.getMessage(),e);
		}
		return defaultId;
	}




	public static List<String> getVersions(String entityID,BrowserHelper helper) {
		List<String> versions = new ArrayList<String>();
		try{
			SearchParameter parameter = new DefaultSearchParameter(
					new String[] { EadcSearchFields.UNITID },
					entityID,
					DefaultSearchParameter.MATCH_ALL_WORDS);
			SearchResult sr =  helper.getFedoraClientUtility().getFedoraGSearch().advancedSearch(new SearchParameter[] {parameter}, 0, 1000, 500, 500);
			SearchResultObject[] results = sr.getSearchResultObjects();
			if(results==null || results.length==0){
				throw new NoSuchRODAObjectException("The Description with id/pid '"+entityID+"' doesn't exist...");
			}else{
				String pid = results[0].getDescriptionObject().getPid();
				Datastream[] datastreamHistory = helper.getFedoraClientUtility().getAPIM().getDatastreamHistory(pid, "EAD-C");
				int i=1;
				for(Datastream ds : datastreamHistory){
					versions.add(""+i);
					i++;
				}
			}
		}catch(Exception e){
	
		}
		return versions;
	}
	public static Object createMetadata(byte[] binaryMetadata) {
		String decodedData = new String(binaryMetadata); 
		
		logger.debug("METADATA:\n"+decodedData);
		
		ElementContainer dc = null;
		try{
			JAXBContext context = JAXBContext.newInstance(ElementContainer.class);
	        Unmarshaller unmarshaller = context.createUnmarshaller();
	        dc = (ElementContainer) unmarshaller.unmarshal(new ByteArrayInputStream(binaryMetadata));
		} catch (JAXBException e) {
			logger.error("Error while deserializing metadata: "+e.getMessage(),e);
		}
		return dc;
	}
	public static String metadataToXML(Object metadata) {
		logger.debug("metadataToXml()");
		try{
			if(metadata instanceof Mix){
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				Mix temp = (Mix)metadata;
				JAXBContext jc = JAXBContext.newInstance(Mix.class);
				Marshaller m = jc.createMarshaller();
				m.marshal(temp, bos);
				return bos.toString("UTF-8");
			}else if(metadata instanceof TextMD){
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				TextMD temp = (TextMD)metadata;
				JAXBContext jc = JAXBContext.newInstance(TextMD.class);
				Marshaller m = jc.createMarshaller();
				m.marshal(temp, bos);
				return bos.toString("UTF-8");
			}else if(metadata instanceof ElementContainer){
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ElementContainer temp = (ElementContainer)metadata;
				JAXBContext jc = JAXBContext.newInstance(ElementContainer.class);
				Marshaller m = jc.createMarshaller();
				m.marshal(temp, bos);
				return bos.toString("UTF-8");
			}else if(metadata instanceof PremisComplexType){
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				PremisComplexType temp = (PremisComplexType)metadata;
				JAXBContext jc = JAXBContext.newInstance(PremisComplexType.class);
				Marshaller m = jc.createMarshaller();
				m.marshal(temp, bos);
				return bos.toString("UTF-8");
			}else{
				return null;
			}
		}catch(Exception e){
			logger.error("Error while deserializing metadata:"+e.getMessage(),e);
			return null;
		}
	}


	/*
	public static Object getDescriptive(String id, int version, BrowserHelper helper) {
		logger.debug("getDescriptive(id="+id+",version="+version+")");
		try{
			SearchParameter parameter = new DefaultSearchParameter(new String[] { EadcSearchFields.UNITID },id,DefaultSearchParameter.MATCH_ALL_WORDS);
			SearchResult sr =  helper.getFedoraClientUtility().getFedoraGSearch().advancedSearch(new SearchParameter[] {parameter}, 0, 1000, 500, 500);
			SearchResultObject[] results = sr.getSearchResultObjects();
			if(results==null || results.length==0){
				throw new NoSuchRODAObjectException("The Description with id/pid '"+id+"' and version '"+version+"' doesn't exist...");
			}else{
				String pid = results[0].getDescriptionObject().getPid();
				Object descriptive = getDatastreamObject(pid,version,"DESCRIPTIVE",helper);
				return descriptive;
			}
		}catch(Exception e){
			logger.error(e.getMessage(),e);
		}
		return null;
	}
*/


	public static Object getDatastreamObject(String id, int version,String datastreamName, BrowserHelper helper) throws NoSuchRODAObjectException, IOException {
		logger.debug("getDatastreamObject(id="+id+",version="+version+",datastreamname="+datastreamName+")");
		Datastream[] datastreamHistory = helper.getFedoraClientUtility().getAPIM().getDatastreamHistory(id, datastreamName);
		String date = null;
		
		if(datastreamHistory==null){
			logger.debug("Datastream history null");
		}else{
			logger.debug("Datastreamsize: "+datastreamHistory.length);
		}
		if(datastreamHistory==null || datastreamHistory.length==0){
			throw new NoSuchRODAObjectException();
		}
		if(version==-1){
			date = datastreamHistory[datastreamHistory.length-1].getCreateDate();
		}else{
			if(datastreamHistory.length<version){
				throw new NoSuchRODAObjectException("The Description with id/pid '"+id+"' and version '"+version+"' doesn't exist...");
			}else{
				
				Datastream datastreamVersion = datastreamHistory[datastreamHistory.length-version];
				date = datastreamVersion.getCreateDate();
			}
		}
		logger.debug("Date of Descriptive datastream: "+date);
		if(date!=null){
			InputStream is = helper.getFedoraClientUtility().getDatastream(id, datastreamName, date);
			try{
				JAXBContext jc = JAXBContext.newInstance(scape.dc.ElementContainer.class,scape.eadc.EadC.class,scape.mix20.Mix.class);
		        Unmarshaller unmarshaller = jc.createUnmarshaller();
		        return unmarshaller.unmarshal(is);
			}catch(Exception e){
				logger.error("Error unmarshalling DESCRIPTIVE datastream: "+e.getMessage(),e);
			}	
		}
		return null;
	}



	public static LifecycleState getLifecycleState(DescriptionObject object) {
		LifecycleState lcs = null;
		if(object.getState()!=null){
			logger.debug("STATE:"+object.getState());
		}else{
			logger.debug("STATE NULL");
		}
		if(object.getState()==null || object.getState().trim().equalsIgnoreCase("")){
			lcs = new LifecycleState("", State.INGESTING);
		}else if(object.getState().equalsIgnoreCase(RODAObject.STATE_INACTIVE)){
			lcs = new LifecycleState("", State.INGESTING);
		}else if(object.getState().equalsIgnoreCase(RODAObject.STATE_ACTIVE)){
			lcs = new LifecycleState("", State.INGESTED);
		}else if(object.getState().equalsIgnoreCase(RODAObject.STATE_DELETED)){
			lcs = new LifecycleState("", State.INGEST_FAILED);
		}else{
			lcs = new LifecycleState("", State.OTHER);
		}
		return lcs;
	}



	public static List<Representation> getRepresentations(String id,int version, BrowserHelper helper,String coreURL) {
		logger.debug("getRepresentations(id="+id+",version="+version+")");
		List<Representation> representations = new ArrayList<Representation>();
		try{
			RepresentationObject[] ros = helper.getDORepresentations(id);
			logger.debug("Number of representations:"+ros.length);
			for(RepresentationObject ro : ros){
				
				List<File> files = new ArrayList<File>();
				if(ro.getRootFile()!=null){
					File f = new File.Builder().identifier(new Identifier(ro.getRootFile().getId())).technical(extractMetadataFromRepresentationFile(ro.getRootFile(), "TECHNICAL"))
				            .uri(new URI(coreURL+ro.getRootFile().getAccessURL())).mimetype(ro.getRootFile().getMimetype()).build();
							files.add(f);
				}
				if(ro.getPartFiles()!=null && ro.getPartFiles().length>0){
					for(RepresentationFile rp : ro.getPartFiles()){
						File f = new File.Builder().identifier(new Identifier(rp.getId())).technical(extractMetadataFromRepresentationFile(ro.getRootFile(), "TECHNICAL"))
			            .uri(new URI(coreURL+rp.getAccessURL())).mimetype(rp.getMimetype()).build();
						files.add(f);
					}
				}
				
				Object provenance = null;
				Object rights = null;
				Object source = null;
				Object technical = null;
				try{
					provenance = getDatastreamObject(ro.getPid(), version, "PROVENANCE", helper);
				}catch(Exception e){
					logger.debug("No PROVENANCE datastream");
				}
				try{
					rights = getDatastreamObject(ro.getPid(), version, "RIGHTS", helper);
				}catch(Exception e){
					logger.debug("No RIGHTS datastream");
				}
				try{
					source = getDatastreamObject(ro.getPid(), version, "SOURCE", helper);
				}catch(Exception e){
					logger.debug("No SOURCE datastream");
				}
				try{
					technical = getDatastreamObject(ro.getPid(), version, "TECHNICAL", helper);
				}catch(Exception e){
					logger.debug("No TECHNICAL datastream");
				}
				Representation rep = null;
				if(provenance!=null || rights!=null || source!=null || technical!=null){
					rep = new Representation.Builder(new Identifier(ro.getId())).title(ro.getLabel()).provenance(provenance).rights(rights).source(source).technical(technical).files(files).build();
				}else{
					rep = representationObjectToRepresentation(helper, ro,coreURL);
				}
				if(rep!=null){
					representations.add(rep);
				}
			}
		}catch(Exception e){
			logger.error("Error while parsing representations:"+e.getMessage());
		}
		return representations;
	}
	
	public static Representation representationObjectToRepresentation(BrowserHelper helper, RepresentationObject ro, String mainURL) throws URISyntaxException {
		logger.debug("Converting RO "+ro.getPid() +" to Scape Representation");
		List<File> files = new ArrayList<File>();
		if(ro.getRootFile()!=null){
			File f = new File.Builder().identifier(new Identifier(ro.getRootFile().getId())).technical(extractMetadataFromRepresentationFile(ro.getRootFile(), "TECHNICAL"))
		            .uri(new URI(mainURL+ro.getRootFile().getAccessURL())).mimetype(ro.getRootFile().getMimetype()).build();
					files.add(f);
		}
		if(ro.getPartFiles()!=null && ro.getPartFiles().length>0){
			for(RepresentationFile rp : ro.getPartFiles()){
				File f = new File.Builder().identifier(new Identifier(rp.getId())).technical(extractMetadataFromRepresentationFile(ro.getRootFile(), "TECHNICAL"))
	            .uri(new URI(mainURL+rp.getAccessURL())).mimetype(rp.getMimetype()).build();
				files.add(f);
			}
		}
		Representation rep = new Representation.Builder(new Identifier(ro.getId()))
        .files(files).build();
		return rep;
	}
	
	//TODO
		public static Object extractMetadataFromRepresentationFile(RepresentationFile file, String metadataID) {
			Mix m = new Mix();
			return m;
		}
		
		public static String prettyPrintXML(String xml) {
		  String output = null;
		  try {
		    Source xmlInput = new StreamSource(new StringReader(xml));
		    StringWriter stringWriter = new StringWriter();
		    StreamResult xmlOutput = new StreamResult(stringWriter);
		    TransformerFactory transformerFactory = TransformerFactory.newInstance();
		    Transformer transformer;
		    
		    transformer = transformerFactory.newTransformer();

		    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
		    transformer.transform(xmlInput, xmlOutput);

		    output = xmlOutput.getWriter().toString();
		  } catch (TransformerConfigurationException e) {
		  } catch (TransformerException e) {
		  }

		  return output;
		}
		
		public static String grabPlanIdFromPlanExecutionDetails(String planExecutionDetails){
		  String output = null;
		  Pattern p = Pattern.compile("plan=\"([^\"]+)\"");
                  Matcher matcher = p.matcher(planExecutionDetails);
                  if(matcher.find()){
                    output = matcher.group(1);
                  }
                  return output;
		}
}
