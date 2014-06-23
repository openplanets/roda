package eu.scape_project.roda.core.connector.utils;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import pt.gov.dgarq.roda.core.BrowserHelper;
import pt.gov.dgarq.roda.core.EditorHelper;
import pt.gov.dgarq.roda.core.common.BrowserException;
import pt.gov.dgarq.roda.core.common.EditorException;
import pt.gov.dgarq.roda.core.common.InvalidDescriptionObjectException;
import pt.gov.dgarq.roda.core.common.NoSuchRODAObjectException;
import pt.gov.dgarq.roda.core.common.RODAServiceException;
import pt.gov.dgarq.roda.core.data.DescriptionObject;
import pt.gov.dgarq.roda.core.data.RODAObject;
import pt.gov.dgarq.roda.core.data.RepresentationFile;
import pt.gov.dgarq.roda.core.data.RepresentationObject;
import pt.gov.dgarq.roda.core.data.eadc.DescriptionLevel;
import pt.gov.dgarq.roda.core.data.eadc.LangmaterialLanguages;
import pt.gov.dgarq.roda.core.fedora.FedoraClientException;
import pt.gov.dgarq.roda.core.metadata.eadc.EadCHelper;
import pt.gov.dgarq.roda.core.metadata.eadc.EadCMetadataException;
import scape.dc.ElementContainer;
import scape.dc.ObjectFactory;
import scape.dc.SimpleLiteral;
import scape.eadc.EadC;
import scape.mix20.Mix;
import scape.premis.PremisComplexType;
import scape.text.TextMD;
import eu.scape_project.model.File;
import eu.scape_project.model.Identifier;
import eu.scape_project.model.IntellectualEntity;
import eu.scape_project.model.LifecycleState;
import eu.scape_project.model.LifecycleState.State;
import eu.scape_project.model.Representation;
import eu.scape_project.util.ScapeMarshaller;

public class DataModelUtils {
	static final private Logger logger = Logger.getLogger(DataModelUtils.class);
	
	private String defaultParentID;
	
	private static DataModelUtils datamodelUtility;
	public DataModelUtils() throws NoSuchRODAObjectException, InvalidDescriptionObjectException, EditorException, ConfigurationException, FedoraClientException, MalformedURLException {
		
	}
	
	
	protected DataModelUtils(BrowserHelper browser,EditorHelper editor) throws NoSuchRODAObjectException, InvalidDescriptionObjectException, EditorException, ConfigurationException, FedoraClientException, MalformedURLException, BrowserException {
		defaultParentID = Utils.getDefaultScapeParentID(browser,editor);
	}
	
	public synchronized static DataModelUtils getInstance(BrowserHelper browser,EditorHelper editor) {
		if (datamodelUtility == null) {
			try{
				datamodelUtility = new DataModelUtils(browser,editor);
			}catch(Exception e){
				logger.error("Error while creating DataModelUtility instance:"+e.getMessage(),e);
			}
		}
		return datamodelUtility;
	}

	
	
	public IntellectualEntity descriptionObjectToIntellectualEntity(BrowserHelper helper,DescriptionObject object,HttpServletRequest req) throws RODAServiceException, NoSuchRODAObjectException, URISyntaxException, JAXBException, IOException, EadCMetadataException{
		logger.debug("Converting DO "+object.getPid() +" to IntellectualEntity");
		String coreURL = req.getRequestURL().substring(0,req.getRequestURL().indexOf("/rest/")); 
		try{
		
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

		/**
		 * TODO...
		 */
		lcs = new LifecycleState("",State.INGESTED);
		logger.debug("LifecycleState: "+lcs.getState());
		
		List<Representation> representations = new ArrayList<Representation>();
		try{
			RepresentationObject[] ros = helper.getDORepresentations(object.getPid());
			logger.debug("Number of representations:"+ros.length);
			for(RepresentationObject ro : ros){
				logger.debug("Representation:"+ro.getId());
				Representation rep = representationObjectToRepresentation(helper, ro,coreURL);
				if(rep!=null){
					representations.add(rep);
				}
			}
		}catch(Exception e){
			logger.error("Error while parsing representations:"+e.getMessage());
		}
		
		Object descriptive = dcFromDescriptionObject(object);
		
		IntellectualEntity e = null;
        if(representations.size()>0){
		    e = new IntellectualEntity.Builder().identifier(
		    	new Identifier(object.getId()))
		    	.descriptive(descriptive)
		    	.representations(representations)
		    	.lifecycleState(lcs)
		    	.build();
        }else{
        	e = new IntellectualEntity.Builder().identifier(
		    	new Identifier(object.getId()))
		    	.descriptive(descriptive)
		    	.lifecycleState(lcs)
		    	.build();
        }
		return e;
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return null;
	}
	

	private Object dcFromRepresentationObject(RepresentationObject representation) {
		logger.debug("dcFromRepresentationObject()");
		ObjectFactory dcFac = new ObjectFactory();
		ElementContainer cnt = dcFac.createElementContainer();
		if(representation.getContentModel()!=null){
			logger.debug("ContentModel not null. Setting title in ElementContainer instance");
			SimpleLiteral literal = new SimpleLiteral();
			literal.getContent().add(representation.getContentModel());
			cnt.getAny().add(dcFac.createSubject(literal));
		}
		if(representation.getCreatedDate()!=null){
			logger.debug("CreatedDate not null. Setting date in ElementContainer instance");
			SimpleLiteral literal = new SimpleLiteral();
			literal.getContent().add(representation.getCreatedDate().toString());
			cnt.getAny().add(dcFac.createDate(literal));
		}
		if(representation.getId()!=null){
			logger.debug("Id not null. Setting identifier in ElementContainer instance");
			SimpleLiteral literal = new SimpleLiteral();
			literal.getContent().add(representation.getId());
			cnt.getAny().add(dcFac.createIdentifier(literal));
		}
		if(representation.getType()!=null){
			logger.debug("Type not null. Setting type in ElementContainer instance");
			SimpleLiteral literal = new SimpleLiteral();
			literal.getContent().add(representation.getId());
			cnt.getAny().add(dcFac.createType(literal));
		}		
		return cnt;
	}

	private Object dcFromRepresentationFile(RepresentationFile file) {
		logger.debug("dcFromRepresentationFile()");
		ObjectFactory dcFac = new ObjectFactory();
		ElementContainer cnt = dcFac.createElementContainer();
		if(file.getId()!=null){
			logger.debug("Id not null. Setting identifier in ElementContainer instance");
			SimpleLiteral literal = new SimpleLiteral();
			literal.getContent().add(file.getId());
			cnt.getAny().add(dcFac.createIdentifier(literal));
		}
		if(file.getMimetype()!=null){
			logger.debug("Mimetype not null. Setting type in ElementContainer instance");
			SimpleLiteral literal = new SimpleLiteral();
			literal.getContent().add(file.getId());
			cnt.getAny().add(dcFac.createType(literal));
		}
		if(file.getOriginalName()!=null){
			logger.debug("OriginalName not null. Setting title in ElementContainer instance");
			SimpleLiteral literal = new SimpleLiteral();
			literal.getContent().add(file.getOriginalName());
			cnt.getAny().add(dcFac.createTitle(literal));
		}
		if(file.getSize()!=0){
			logger.debug("Size not null. Setting format in ElementContainer instance");
			SimpleLiteral literal = new SimpleLiteral();
			literal.getContent().add(file.getId());
			cnt.getAny().add(dcFac.createFormat(literal) );
		}
		return cnt;
	}

	private ElementContainer dcFromDescriptionObject(DescriptionObject object) {
		logger.debug("dcFromDescriptionObject()");
		ObjectFactory dcFac = new ObjectFactory();
		ElementContainer cnt = dcFac.createElementContainer();
		if(object.getTitle()!=null){
			logger.debug("Title not null. Setting title in ElementContainer instance");
			SimpleLiteral lit_title = new SimpleLiteral();
			lit_title.getContent().add(object.getTitle());
			cnt.getAny().add(dcFac.createTitle(lit_title));
		}
		if(object.getDateInitial()!=null){
			logger.debug("DateInitial not null. Setting date in ElementContainer instance");
			SimpleLiteral lit_title = new SimpleLiteral();
			lit_title.getContent().add(object.getDateInitial());
			cnt.getAny().add(dcFac.createDate(lit_title));
		}
		if(object.getDateFinal()!=null){
			logger.debug("DateFinal not null. Setting date in ElementContainer instance");
			SimpleLiteral lit_title = new SimpleLiteral();
			lit_title.getContent().add(object.getDateFinal());
			cnt.getAny().add(dcFac.createDate(lit_title));
		}
		if(object.getDescription()!=null){
			logger.debug("Description not null. Setting description in ElementContainer instance");
			SimpleLiteral lit_title = new SimpleLiteral();
			lit_title.getContent().add(object.getDescription());
			cnt.getAny().add(dcFac.createDescription(lit_title));
		}
		if(object.getNote()!=null){
			logger.debug("Note not null. Setting description in ElementContainer instance");
			SimpleLiteral lit_title = new SimpleLiteral();
			lit_title.getContent().add(object.getNote());
			cnt.getAny().add(dcFac.createDescription(lit_title));
		}
		if(object.getPhysdesc()!=null){
			logger.debug("PhysDesc not null. Setting type in ElementContainer instance");
			SimpleLiteral lit_title = new SimpleLiteral();
			lit_title.getContent().add(object.getPhysdesc());
			cnt.getAny().add(dcFac.createType(lit_title));
		}
		if(object.getRelatedmaterial()!=null){
			logger.debug("RelatedMaterial not null. Setting relation in ElementContainer instance");
			SimpleLiteral lit_title = new SimpleLiteral();
			lit_title.getContent().add(object.getRelatedmaterial());
			cnt.getAny().add(dcFac.createRelation(lit_title));
		}
		if(object.getOrigination()!=null){
			logger.debug("Origination not null. Setting source in ElementContainer instance");
			SimpleLiteral lit_title = new SimpleLiteral();
			lit_title.getContent().add(object.getOrigination());
			cnt.getAny().add(dcFac.createSource(lit_title));
		}
		if(object.getScopecontent()!=null){
			logger.debug("ScopeContent not null. Setting subject in ElementContainer instance");
			SimpleLiteral lit_title = new SimpleLiteral();
			lit_title.getContent().add(object.getScopecontent());
			cnt.getAny().add(dcFac.createSubject(lit_title));
		}
		
		if(object.getAccessrestrict()!=null){
			logger.debug("Accessrestrict not null. Setting rights in ElementContainer instance");
			SimpleLiteral lit_title = new SimpleLiteral();
			lit_title.getContent().add(object.getAccessrestrict());
			cnt.getAny().add(dcFac.createRights(lit_title));
		}
		
		if(object.getPhysdesc()!=null){
			logger.debug("PhysDesc not null. Setting format in ElementContainer instance");
			SimpleLiteral lit_title = new SimpleLiteral();
			lit_title.getContent().add(object.getPhysdesc());
			cnt.getAny().add(dcFac.createFormat(lit_title));
		}
		if(object.getId()!=null){
			logger.debug("PID not null. Setting identifier in ElementContainer instance");
			SimpleLiteral lit_title = new SimpleLiteral();
			lit_title.getContent().add(object.getId());
			cnt.getAny().add(dcFac.createIdentifier(lit_title));
		}
		if(object.getLangmaterialLanguages()!=null && object.getLangmaterialLanguages().getLangmaterialLanguages().length>0){
			logger.debug("LangmaterialLanguages not null. Setting language in ElementContainer instance");
			SimpleLiteral lit_title = new SimpleLiteral();
			lit_title.getContent().add(object.getLangmaterialLanguages().getLangmaterialLanguages()[0]);
			cnt.getAny().add(dcFac.createLanguage(lit_title));
		}
		
		
		return cnt;
	}
	private Object eadcFromDescriptionObject(DescriptionObject object) throws IOException, EadCMetadataException, JAXBException {
		EadCHelper eadcHelper = new EadCHelper(object);
		java.io.File eadcFile = java.io.File.createTempFile("ead", ".xml");
		eadcHelper.saveToFile(eadcFile);
		logger.debug("EAD XML FILE:"+eadcFile.getPath());
		logger.debug("DO URL:"+object.getHandleURL());
		JAXBContext jc = JAXBContext.newInstance(scape.eadc.EadC.class);

        Unmarshaller unmarshaller = jc.createUnmarshaller();
        scape.eadc.EadC eadc = (scape.eadc.EadC) unmarshaller.unmarshal(eadcFile);
        logger.debug("EAD NEW LEVEL:"+eadc.getLevel().value()+" - "+eadc.getLevel().name());
        
        ObjectFactory dcFac = new ObjectFactory();
        ElementContainer cnt = dcFac.createElementContainer();
        SimpleLiteral lit_title = new SimpleLiteral();
        lit_title.getContent().add("Object 1");
        cnt.getAny().add(dcFac.createTitle(lit_title));
        return cnt;
	}


	public File representationFileToFile(RepresentationFile rf) throws URISyntaxException{
		logger.debug("Converting RF "+rf.getId() +" to Scape File");
		File f = new File.Builder().identifier(new Identifier(rf.getId()))
                .uri(new URI(rf.getAccessURL()))
                .technical(rf).mimetype(rf.getMimetype()).build();
		return f;
	}


	public Representation representationObjectToRepresentation(BrowserHelper helper, RepresentationObject ro, String mainURL) throws URISyntaxException {
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


	public RepresentationObject representationToRepresentationObject(Representation rep) {
		if(rep.getIdentifier()!=null){
			logger.debug("Converting Scape Representation "+rep.getIdentifier().getValue() +" to RO");
		}else{
			logger.debug("Converting Scape Representation without identifier to RO");
		}
		RepresentationObject ro = new RepresentationObject();
		Object o = rep.getTechnical();
		if(o instanceof Mix){
			Mix temp = (Mix)o;
			ro.setType("Mix");
			try{
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ScapeMarshaller.newInstance().serialize(temp, bos);
				ro.setSubType(bos.toString("UTF-8"));
			}catch(Exception e){
				logger.error("Error while serializing scape.mix20.Mix instance: "+e.getMessage(),e);
			}
		}
		ro.setId(rep.getIdentifier().getValue());
		if(rep.getFiles()!=null && rep.getFiles().size()>0){
			for(File f : rep.getFiles()){
				RepresentationFile rf = fileToRepresentationFile(f);
				ro.addPartFile(rf);
			}
		}
		return ro;
	}



	private RepresentationFile fileToRepresentationFile(File f) {
		logger.debug("fileToRepresentationFile");
		RepresentationFile rf = new RepresentationFile();

		try{
			logger.debug("Setting id:"+f.getIdentifier().getValue());
			rf.setId(f.getIdentifier().getValue());
			logger.debug("Setting mime type:"+f.getMimetype());
			rf.setMimetype(f.getMimetype());
			logger.debug("Setting original name:"+f.getFilename());
			rf.setOriginalName(f.getFilename());
			URL url = f.getUri().toURL(); 
			rf.setAccessURL(url.toString());
		}catch(Exception e){
			logger.error("Error while creating RepresentationFile: "+e.getMessage(),e);
		}
		return rf;
	}
	public DescriptionObject intellectualEntityToDescriptionObject(IntellectualEntity entity) throws IOException, JAXBException, EadCMetadataException {
		
		if(entity.getIdentifier()!=null){
			logger.debug("Converting Scape Intellectual Entity "+entity.getIdentifier().getValue() +" to DO");
		}else{
			logger.debug("Converting Scape Intellectual Entity without identifier to DO");
		}
		Object o = entity.getDescriptive();
			DescriptionObject convertedDescriptionObject = null;
			if(o instanceof EadC){
				logger.debug("Intellectual Entity contains EadC");
				convertedDescriptionObject = descriptionObjectFromEadc((EadC)o);
			}else if(o instanceof ElementContainer){
				logger.debug("Intellectual Entity contains DC");
				convertedDescriptionObject = descriptionObjectFromDc(null,(ElementContainer)o);
			}
			if(entity.getIdentifier()!=null && entity.getIdentifier().getValue()!=null){
				convertedDescriptionObject.setId(entity.getIdentifier().getValue());
			}
			
			convertedDescriptionObject = fillMandatoryFields(convertedDescriptionObject);
			logger.debug("Entity title:"+convertedDescriptionObject.getTitle());
			logger.debug("Entity ID:"+convertedDescriptionObject.getId());
			return convertedDescriptionObject;
	}

	private DescriptionObject descriptionObjectFromDc(DescriptionObject descriptionObject,ElementContainer o) {
		logger.debug("descriptionObjectFromDc()");
		if(descriptionObject==null){
			logger.debug("DO NULL... Creating new.");
			descriptionObject = new DescriptionObject();
		}else{
			logger.debug("DO NOT NULL... Removing metadata...");
			descriptionObject.setDescription(null);
			descriptionObject.setNote(null);
			descriptionObject.setPhysdesc(null);
			descriptionObject.setRelatedmaterial(null);
			descriptionObject.setOrigination(null);
			descriptionObject.setScopecontent("");
			descriptionObject.setTitle("");
			descriptionObject.setAccessrestrict(null);
			descriptionObject.setLangmaterialLanguages(null);
		}
		for(JAXBElement<SimpleLiteral> element : o.getAny()){
			if(element.getName().getLocalPart().equalsIgnoreCase("coverage")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("description")){
				logger.debug("Description:"+StringUtils.join(element.getValue().getContent().toArray()));
				descriptionObject.setDescription(StringUtils.join(element.getValue().getContent().toArray()));
				descriptionObject.setNote(StringUtils.join(element.getValue().getContent().toArray()));
			}else if(element.getName().getLocalPart().equalsIgnoreCase("type")){
				logger.debug("Type:"+StringUtils.join(element.getValue().getContent().toArray()));
				descriptionObject.setPhysdesc(StringUtils.join(element.getValue().getContent().toArray()));
			}else if(element.getName().getLocalPart().equalsIgnoreCase("relation")){
				logger.debug("Relation:"+StringUtils.join(element.getValue().getContent().toArray()));
				descriptionObject.setRelatedmaterial(StringUtils.join(element.getValue().getContent().toArray()));
			}else if(element.getName().getLocalPart().equalsIgnoreCase("source")){
				logger.debug("Source:"+StringUtils.join(element.getValue().getContent().toArray()));
				descriptionObject.setOrigination(StringUtils.join(element.getValue().getContent().toArray()));
			}else if(element.getName().getLocalPart().equalsIgnoreCase("subject")){
				logger.debug("Subject:"+StringUtils.join(element.getValue().getContent().toArray()));
				descriptionObject.setScopecontent(StringUtils.join(element.getValue().getContent().toArray()));
			}else if(element.getName().getLocalPart().equalsIgnoreCase("title")){
				logger.debug("Title:"+StringUtils.join(element.getValue().getContent().toArray()));
				descriptionObject.setTitle(StringUtils.join(element.getValue().getContent().toArray()));
			}else if(element.getName().getLocalPart().equalsIgnoreCase("creator")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("contributor")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("publisher")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("rights")){
				descriptionObject.setAccessrestrict(StringUtils.join(element.getValue().getContent().toArray()));
			}else if(element.getName().getLocalPart().equalsIgnoreCase("date")){
				logger.debug("Date:"+StringUtils.join(element.getValue().getContent().toArray()));
				String date = extractDate(StringUtils.join(element.getValue().getContent().toArray()));
				if(date!=null){
					descriptionObject.setDateInitial(date);
					descriptionObject.setDateFinal(date);
				}
			}else if(element.getName().getLocalPart().equalsIgnoreCase("format")){
				logger.debug("Format:"+StringUtils.join(element.getValue().getContent().toArray()));
				descriptionObject.setPhysdesc(StringUtils.join(element.getValue().getContent().toArray()));
			}else if(element.getName().getLocalPart().equalsIgnoreCase("identifier")){
				logger.debug("Identifier:"+StringUtils.join(element.getValue().getContent().toArray()));
				logger.debug("Setting ID:"+StringUtils.join(element.getValue().getContent().toArray()));
				descriptionObject.setId(StringUtils.join(element.getValue().getContent().toArray()));
			}else if(element.getName().getLocalPart().equalsIgnoreCase("language")){
				logger.debug("Language:"+StringUtils.join(element.getValue().getContent().toArray()));
				String[] lang = new String[element.getValue().getContent().size()];
				int i=0;
				for(String s : element.getValue().getContent()){
					lang[i] = s;
					i++;
				}
				descriptionObject.setLangmaterialLanguages(new LangmaterialLanguages(lang));
			}else if(element.getName().getLocalPart().equalsIgnoreCase("abstract")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("accessRights")){

			}else if(element.getName().getLocalPart().equalsIgnoreCase("accrualMethod")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("accrualPeriodicity")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("accrualPolicy")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("alternative")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("audience")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("available")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("bibliographicCitation")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("conformsTo")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("created")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("dateAccepted")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("dateCopyrighted")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("dateSubmitted")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("educationLevel")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("extent")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("hasFormat")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("hasPart")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("hasVersion")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("instructionalMethod")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("isFormatOf")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("isPartOf")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("isReferencedBy")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("isReplacedBy")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("isRequiredBy")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("issued")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("isVersionOf")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("license")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("mediator")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("medium")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("modified")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("provenance")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("references")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("replaces")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("replaces")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("requires")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("rightsHolder")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("spatial")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("tableOfContents")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("temporal")){
				
			}else if(element.getName().getLocalPart().equalsIgnoreCase("valid")){
				
			}

		}
		return descriptionObject;
	}

	private String extractDate(String d) {
		logger.debug("Extracting date from "+d);
		String date = null;
		  Matcher m = Pattern.compile("(0[1-9]|[12][0-9]|3[01])[- /.](0[1-9]|1[012])[- /.](19|20)\\d\\d").matcher(d);
		  if(m.find()){
			  date = m.group();
		  }
		  logger.debug("Date:"+date);
		return date;
	}
	private DescriptionObject fillMandatoryFields(DescriptionObject convertedDescriptionObject) {
		if(convertedDescriptionObject.getCountryCode()==null || convertedDescriptionObject.getCountryCode().trim().equalsIgnoreCase("")){
			logger.debug("Country code null - Setting country code = PT");
			convertedDescriptionObject.setCountryCode("PT");
		}
		if(convertedDescriptionObject.getOrigination()==null || convertedDescriptionObject.getOrigination().trim().equalsIgnoreCase("")){
			logger.debug("Origination null - Setting origination = 'Data Connector API'");
			convertedDescriptionObject.setOrigination("Data Connector API");
		}
		if(convertedDescriptionObject.getRepositoryCode()==null || convertedDescriptionObject.getRepositoryCode().trim().equalsIgnoreCase("")){
			logger.debug("Repository code null - Setting repository code = 'DCA'"); 
			convertedDescriptionObject.setRepositoryCode("DCA");
		}
		if(convertedDescriptionObject.getScopecontent()==null || convertedDescriptionObject.getScopecontent().trim().equalsIgnoreCase("")){
			logger.debug("Scope content null - Setting scope content = '-'"); 
			convertedDescriptionObject.setScopecontent("-");
		}
		if(convertedDescriptionObject.getDateFinal()==null || convertedDescriptionObject.getDateFinal().trim().equalsIgnoreCase("")){
			Calendar now = Calendar.getInstance();
			int year = now.get(Calendar.YEAR);  
			logger.debug("Date final null - Setting date final = '"+year+"'"); 
			convertedDescriptionObject.setDateFinal(""+year);
		}
		if(convertedDescriptionObject.getLevel()==null){
			logger.debug("Level null - Setting level = '"+DescriptionLevel.ITEM+"'"); 
			convertedDescriptionObject.setLevel(DescriptionLevel.ITEM);
		}
		if(!convertedDescriptionObject.getLevel().equals(DescriptionLevel.FONDS)){
			logger.debug("Level ='"+DescriptionLevel.FONDS+"' - Setting parent pid = "+defaultParentID); 
			convertedDescriptionObject.setParentPID(defaultParentID);
		}
		if(convertedDescriptionObject.getTitle()==null || convertedDescriptionObject.getTitle().trim().equalsIgnoreCase("")){
			logger.debug("Title null - Setting title = '-'"); 
			convertedDescriptionObject.setTitle("-");
		}
		if(convertedDescriptionObject.getId()==null || convertedDescriptionObject.getId().trim().equalsIgnoreCase("")){
			logger.debug("ID null - Setting title = random"); 
			convertedDescriptionObject.setId(UUID.randomUUID().toString());
		}
		return convertedDescriptionObject;
	}

	private DescriptionObject descriptionObjectFromEadc(EadC o) throws IOException, JAXBException, EadCMetadataException {
		java.io.File tempEADFile = java.io.File.createTempFile("ead", ".xml");
		EadC eadc = (EadC)o;
		JAXBContext jcScape = JAXBContext.newInstance(scape.eadc.EadC.class);
		OutputStream fileOutputStream = new FileOutputStream(tempEADFile);
		jcScape.createMarshaller().marshal(eadc, fileOutputStream);
		fileOutputStream.flush();
		fileOutputStream.close();
		
		EadCHelper eadcHelper = EadCHelper.newInstance(tempEADFile);
		
		DescriptionObject convertedDO = eadcHelper.getDescriptionObject();
		
		logger.debug("Converted DO LEVEL:"+convertedDO.getLevel().getLevel());
		if(convertedDO.getLevel()==null){
			convertedDO.setLevel(DescriptionLevel.ITEM);
		}
		if(!convertedDO.getLevel().equals(DescriptionLevel.FONDS)){
			logger.debug("Setting default parent ID:"+defaultParentID);
			convertedDO.setParentPID(defaultParentID);
		}
		return convertedDO;
	}
	
	public List<RepresentationObject> extractRepresentationObjectsFromIntellectualEntity(IntellectualEntity entity) {
		List<RepresentationObject> representationObjects = null;
		if(entity.getRepresentations()!=null && entity.getRepresentations().size()>0){
			representationObjects = new ArrayList<RepresentationObject>();
			for(Representation r : entity.getRepresentations()){
				RepresentationObject ro = representationToRepresentationObject(r);
				representationObjects.add(ro);
			}
		}
		return representationObjects;
	}


	public DescriptionObject updateDescriptionObjectMetadata(DescriptionObject descriptionObject, Object metadata) {
		logger.debug("ScopeAndContent 1:"+descriptionObject.getScopecontent());
		if(metadata instanceof ElementContainer){
			descriptionObject = descriptionObjectFromDc(descriptionObject,(ElementContainer)metadata);
		}
		descriptionObject = fillMandatoryFields(descriptionObject);
		logger.debug("ScopeAndContent 2:"+descriptionObject.getScopecontent());
		return fillMandatoryFields(descriptionObject);
	}


	public Object extractMetadataFromDescriptionObject(DescriptionObject o,String metadataID) {
		if(metadataID.equalsIgnoreCase("DESCRIPTIVE")){
			return dcFromDescriptionObject(o);
		}else{
			return null;
		}
	}


	public Object extractMetadataFromRepresentationObject(BrowserHelper browser,RepresentationObject representation, String metadataID) {
		logger.debug("extractMetadataFromRepresentationObject(metadataID='"+metadataID+"')");
		if(metadataID.equalsIgnoreCase("TECHNICAL")){
			return new TextMD();
		}else if(metadataID.equals("PROVENANCE")){
			return new PremisComplexType();
		}else if(metadataID.equalsIgnoreCase("RIGHTS")){
			return new PremisComplexType();
		}else if(metadataID.equalsIgnoreCase("SOURCE")){
			return new ElementContainer();
		}else{
			return null;
		}
	}


	
	//TODO
	public Object extractMetadataFromRepresentationFile(RepresentationFile file, String metadataID) {
		Mix m = new Mix();
		return m;
	}


	
}
