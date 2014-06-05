package eu.scape_project.roda.core.connector.utils;

import gov.loc.mix.v20.Mix;
import info.lc.xmlns.premis_v2.PremisComplexType;
import info.lc.xmlns.textmd_v3.TextMD;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.purl.dc.elements._1.ElementContainer;

import pt.gov.dgarq.roda.core.BrowserHelper;
import pt.gov.dgarq.roda.core.EditorHelper;
import pt.gov.dgarq.roda.core.common.NoSuchRODAObjectException;
import pt.gov.dgarq.roda.core.data.DescriptionObject;
import pt.gov.dgarq.roda.core.data.RODAObject;
import pt.gov.dgarq.roda.core.data.SearchParameter;
import pt.gov.dgarq.roda.core.data.SearchResult;
import pt.gov.dgarq.roda.core.data.SearchResultObject;
import pt.gov.dgarq.roda.core.data.eadc.DescriptionLevel;
import pt.gov.dgarq.roda.core.data.search.DefaultSearchParameter;
import pt.gov.dgarq.roda.core.data.search.EadcSearchFields;

public class Utils {
	static final private Logger logger = Logger.getLogger(Utils.class);
	
	
	
	
	
	
	
	
	public static Comparator<SearchResultObject> dateComparator = new Comparator<SearchResultObject>() {

        @Override
        public int compare(SearchResultObject sro1, SearchResultObject sro2) {
            return sro1.getDescriptionObject().getLastModifiedDate().compareTo(sro2.getDescriptionObject().getLastModifiedDate());
        }
    };
    
	
    public static DescriptionObject findById(String id,BrowserHelper helper){
    	return findById(id, -1, helper);
    }
	public static DescriptionObject findById(String id,int version,BrowserHelper helper){
		DescriptionObject descriptionObject = null;
		try{
			SearchParameter parameter = new DefaultSearchParameter(
					new String[] { EadcSearchFields.UNITID },
					id,
					DefaultSearchParameter.MATCH_ALL_WORDS);

			SearchResult sr =  helper.getFedoraClientUtility().getFedoraGSearch().advancedSearch(new SearchParameter[] {parameter}, 0, 1000, 500, 500);
			SearchResultObject[] results = sr.getSearchResultObjects();
			
			if(results!=null && results.length>0){
				Arrays.sort(results, dateComparator);
				if(version==-1){	//current version
					descriptionObject = results[results.length-1].getDescriptionObject();
				}else if(version<=results.length && version>0){
					descriptionObject = results[version-1].getDescriptionObject();
				}
			}
			if(descriptionObject==null){
				descriptionObject = helper.getDescriptionObject(id);
			}
			if(descriptionObject==null){
				throw new NoSuchRODAObjectException("The Description with id/pid '"+id+"' and version '"+version+"' doesn't exist...");
			}
		}catch(Exception e){
	
		}
		return descriptionObject;
	}




	public static String getDefaultScapeParentID(BrowserHelper browser,EditorHelper helper) {
		String defaultId = null;
		try{
			logger.debug("getDefaultScapeParentID()");
			DescriptionObject defaultScapeDescriptionObject = null;
			SearchParameter parameterTitle = new DefaultSearchParameter(
					new String[] { EadcSearchFields.UNITTITLE },
					"Connector subseries",
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
				fondsDO.setId("SCAPE-FOND");
				fondsDO.setTitle("Connector Fond");
				fondsDO.setDateInitial("1000");
				fondsDO.setDateFinal("2500");
				fondsDO.setOrigination("Connector API");
				fondsDO.setScopecontent("-");
				String fondsPID = helper.createDescriptionObject(fondsDO);
				fondsDO.setState(RODAObject.STATE_ACTIVE);
				helper.modifyDescriptionObject(fondsDO);
				
				
				DescriptionObject tempDObject = new DescriptionObject();
				tempDObject.setLevel(DescriptionLevel.SUBFONDS);
				tempDObject.setId("SCAPE-SUBFOND");
				tempDObject.setCountryCode("PT");
				tempDObject.setRepositoryCode("SCAPE");
				tempDObject.setTitle("Connector Subfond");
				tempDObject.setOrigination("Connector API");
				tempDObject.setScopecontent("-");
				tempDObject.setParentPID(fondsPID);
				String subfondsPID = helper.createDescriptionObject(tempDObject);
				
				tempDObject = new DescriptionObject();
				tempDObject.setLevel(DescriptionLevel.SERIES);
				tempDObject.setId("SCAPE-SERIES");
				tempDObject.setCountryCode("PT");
				tempDObject.setRepositoryCode("SCAPE");
				tempDObject.setTitle("Connector series");
				tempDObject.setOrigination("Connector API");
				tempDObject.setScopecontent("-");
				tempDObject.setParentPID(subfondsPID);
				String seriesPID = helper.createDescriptionObject(tempDObject);
		
				
				tempDObject = new DescriptionObject();
				tempDObject.setLevel(DescriptionLevel.SUBSERIES);
				tempDObject.setId("SCAPE-SUBSERIES");
				tempDObject.setCountryCode("PT");
				tempDObject.setRepositoryCode("SCAPE");
				tempDObject.setTitle("Connector subseries");
				tempDObject.setOrigination("Connector API");
				tempDObject.setScopecontent("-");
				tempDObject.setParentPID(seriesPID);
				String subSeriesPID = helper.createDescriptionObject(tempDObject);
				defaultId = subSeriesPID;
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
			for(int i=0;i<results.length;i++){
				versions.add(""+i);
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
}
