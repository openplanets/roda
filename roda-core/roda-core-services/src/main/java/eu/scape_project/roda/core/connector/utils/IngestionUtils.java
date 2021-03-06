package eu.scape_project.roda.core.connector.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.w3c.util.DateParser;

import pt.gov.dgarq.roda.common.FormatUtility;
import pt.gov.dgarq.roda.core.BrowserHelper;
import pt.gov.dgarq.roda.core.EditorHelper;
import pt.gov.dgarq.roda.core.IngestHelper;
import pt.gov.dgarq.roda.core.common.BrowserException;
import pt.gov.dgarq.roda.core.common.EditorException;
import pt.gov.dgarq.roda.core.common.IngestException;
import pt.gov.dgarq.roda.core.common.InvalidDescriptionLevel;
import pt.gov.dgarq.roda.core.common.InvalidDescriptionObjectException;
import pt.gov.dgarq.roda.core.common.NoSuchRODAObjectException;
import pt.gov.dgarq.roda.core.common.RepresentationAlreadyPreservedException;
import pt.gov.dgarq.roda.core.data.AgentPreservationObject;
import pt.gov.dgarq.roda.core.data.Attribute;
import pt.gov.dgarq.roda.core.data.DescriptionObject;
import pt.gov.dgarq.roda.core.data.EventPreservationObject;
import pt.gov.dgarq.roda.core.data.Report;
import pt.gov.dgarq.roda.core.data.ReportItem;
import pt.gov.dgarq.roda.core.data.RepresentationFile;
import pt.gov.dgarq.roda.core.data.RepresentationObject;
import pt.gov.dgarq.roda.core.fedora.FedoraClientException;
import pt.gov.dgarq.roda.core.metadata.eadc.EadCMetadataException;
import pt.gov.dgarq.roda.core.reports.ReportManager;
import pt.gov.dgarq.roda.core.reports.ReportManagerException;
import pt.gov.dgarq.roda.core.reports.ReportRegistryException;
import pt.gov.dgarq.roda.util.TempDir;
import eu.scape_project.model.BitStream;
import eu.scape_project.model.IntellectualEntity;
import eu.scape_project.model.Representation;
import eu.scape_project.util.ScapeMarshaller;

public class IngestionUtils {
static final private Logger logger = Logger.getLogger(IngestionUtils.class);
	private static ExecutorService asyncIngesterThreadPool;
	private static Map<String,IntellectualEntity> statusIDToEntityMapping; 
	
	
	
	
	private static IngestionUtils ingestionUtils;
	
	public String getStatusIDFromEntityID(String entityID){
		if(statusIDToEntityMapping!=null){
			for(Map.Entry<String, IntellectualEntity> entry : statusIDToEntityMapping.entrySet()){
				if(entry.getValue().getIdentifier().getValue().equalsIgnoreCase(entityID)){
					return entry.getKey();
				}
			}
		}
		return null;
	}
	
	public IntellectualEntity getIntellectualEntityFromStatusId(String statusID){
		if(!statusIDToEntityMapping.containsKey(statusID)){
			return null;
		}else{
			return statusIDToEntityMapping.get(statusID);
		}
	}
	
	public boolean statusIdExists(String id){
		return statusIDToEntityMapping.containsKey(id);
	}


	protected IngestionUtils(){
		asyncIngesterThreadPool = Executors.newFixedThreadPool(1);
		statusIDToEntityMapping = new Hashtable<String, IntellectualEntity>();
	}
	
	
	private String generateRandomID(){
		 UUID uuid = UUID.randomUUID();
		  long l = ByteBuffer.wrap(uuid.toString().getBytes()).getLong();
		  return Long.toString(l, Character.MAX_RADIX);
	}
	
	public synchronized static IngestionUtils getInstance() {
		if (ingestionUtils == null) {
			ingestionUtils = new IngestionUtils();
		}
		return ingestionUtils;
	}
	
	public String async(IntellectualEntity entity, EditorHelper editor, IngestHelper ingest, BrowserHelper browser,Uploader uploader, String planID){
		boolean newIdGenerated=false;
		String id = null;
		while(!newIdGenerated){
			id = generateRandomID();
			if(!IngestionUtils.existStatusID(id)){
				newIdGenerated = true;
				IngestionUtils.addStatusIDToEntityMapping(id,entity);
				statusIDToEntityMapping.put(id, entity);
				IngestionThread it = new IngestionThread(entity,editor, ingest, browser,uploader, planID);
				asyncIngesterThreadPool.execute(it);
			}
		}
		return id;
	}


	private static void addStatusIDToEntityMapping(String id,IntellectualEntity e) {
		statusIDToEntityMapping.put(id, e);
		
	}


	private static boolean existStatusID(String id) {
		return statusIDToEntityMapping.containsKey(id);
	}


	public String ingest(IntellectualEntity entity,EditorHelper editor, IngestHelper ingest, BrowserHelper browser,Uploader uploader, String planID) throws DataConnectorException{
		if(entity.getIdentifier()!=null){
			logger.debug("Ingest entity  "+entity.getIdentifier().getValue());
		}else{
			logger.debug("Ingest entity without identifier");
		}
		Report report = new Report();
		String error;
		Exception exception;
		ReportManager reportManager = null;
		
		try {
			reportManager = ReportManager.getDefaultReportManager();
		} catch (ReportManagerException e) {
			report.addItem(generateErrorReportItem("IntellectualEntity ingestion error report","ReportManager exception while getting default ReportManager",e));
			throw new DataConnectorException("ReportManager exception while getting default ReportManager",e);
		}
		try{
			DescriptionObject descriptionObject = null;
			String descriptionObjectPID = null;
			String epoPID = null;
			report.setType(Report.TYPE_DATACONNECTOR_REPORT);
			report.setTitle("Data connector IntellectualEntity ingestion.");
			report.addAttribute(new Attribute("Start datetime", DateParser.getIsoDate(new Date())));
			ReportItem reportItem = new ReportItem("IntellectualEntity ingestion");
			reportItem.addAttribute(new Attribute("Start datetime", DateParser.getIsoDate(new Date())));
			reportItem.addAttribute(new Attribute("IntellectualEntity to DescriptionObject conversion - start datetime",DateParser.getIsoDate(new Date())));
			descriptionObject = DataModelUtils.getInstance(browser,editor).intellectualEntityToDescriptionObject(entity);
			reportItem.addAttribute(new Attribute("IntellectualEntity to DescriptionObject conversion - end datetime",DateParser.getIsoDate(new Date())));
			reportItem.addAttribute(new Attribute("DescriptionObject ingestion - start datetime",DateParser.getIsoDate(new Date())));
			descriptionObjectPID = editor.createDescriptionObject(descriptionObject);
			if(entity.getDescriptive()!=null){
				logger.debug("Adding DESCRIPTIVE datastream to "+descriptionObjectPID);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ScapeMarshaller.newInstance().serialize(entity.getDescriptive(), bos);
				String tempURL = browser.getFedoraClientUtility().temporaryUpload(bos.toByteArray());
				browser.getFedoraClientUtility().getAPIM().addDatastream(descriptionObjectPID, "DESCRIPTIVE", new String[0],"Descriptive Metadata", true, "text/xml", null, tempURL,"X", "A", null, null, "Added by RODA Connector API");
			}
			reportItem.addAttribute(new Attribute("DescriptionObject ingestion - end datetime",DateParser.getIsoDate(new Date())));
			logger.debug("Ingesting representations...");
			List<String> representationPIDS = new ArrayList<String>();
			if(entity.getRepresentations()!=null && entity.getRepresentations().size()>0){
				reportItem.addAttribute(new Attribute("RepresentationObjects ingestion - start datetime",DateParser.getIsoDate(new Date())));
				File temp = TempDir.createUniqueTemporaryDirectory("representations");
				for(Representation r : entity.getRepresentations()){
					try{
						RepresentationObject ro = DataModelUtils.getInstance(browser,editor).representationToRepresentationObject(r);
						logger.debug("RepresentationObject ID1: "+ro.getId());
						ro.setDescriptionObjectPID(descriptionObjectPID);
						logger.debug("Processing representation files...");
						List<RepresentationFile> representationFilesToUpload = new ArrayList<RepresentationFile>();
						if(r.getFiles()!=null && r.getFiles().size()>0){
							for(eu.scape_project.model.File file : r.getFiles()){
								try{
									String fileName = file.getFilename();
									if(fileName==null){
										String[] values=file.getUri().toString().split("/");
										fileName=values[values.length-1];
									}
									File f = new File(temp,fileName);
									IOUtils.copy(file.getUri().toURL().openStream(), new FileOutputStream(f));
									String mimetype = FormatUtility.getMimetype(f);
									RepresentationFile rFile = new RepresentationFile(
										file.getIdentifier().getValue(),
										f.getName(),
										mimetype==null?"application/octet-stream":mimetype, 
										f.length(), 
										f.toURI().toURL().toExternalForm()
									);
									logger.debug("PartFile ID: "+rFile.getId());
									logger.debug("PartFile Name: "+rFile.getOriginalName());
									ro.addPartFile(rFile);
									representationFilesToUpload.add(rFile);
								}catch(Exception e){
									
									logger.error("Error while getting file from URL:"+file.getUri().toString()+": "+e.getMessage(),e);
								}
								
							}
						}
						ro.setType(RepresentationObject.UNKNOWN);
						ro.setStatuses(new String[] { RepresentationObject.STATUS_ORIGINAL });
						ro.setDescriptionObjectPID(descriptionObjectPID);
						logger.debug("RepresentationFiles to upload: "+representationFilesToUpload.size());
						if(representationFilesToUpload.size()>0){
							logger.debug("Creating RepresentationObject with ID:"+ro.getId());
							String rObjectPID = ingest.createRepresentationObject(ro);
							logger.debug("RepresentationObject PID:"+rObjectPID);
							if(r.getProvenance()!=null){
								logger.debug("Adding PROVENANCE datastream to "+rObjectPID);
								ByteArrayOutputStream bos = new ByteArrayOutputStream();
								ScapeMarshaller.newInstance().serialize(r.getProvenance(), bos);
								String tempURL = browser.getFedoraClientUtility().temporaryUpload(bos.toByteArray());
								browser.getFedoraClientUtility().getAPIM().addDatastream(rObjectPID, "PROVENANCE", new String[0],"Provenance Metadata", true, "text/xml", null, tempURL,"X", "A", null, null, "Added by RODA Connector API");
							}
							if(r.getRights() !=null){
								logger.debug("Adding RIGHTS datastream to "+rObjectPID);
								ByteArrayOutputStream bos = new ByteArrayOutputStream();
								ScapeMarshaller.newInstance().serialize(r.getRights(), bos);
								String tempURL = browser.getFedoraClientUtility().temporaryUpload(bos.toByteArray());
								browser.getFedoraClientUtility().getAPIM().addDatastream(rObjectPID, "RIGHTS", new String[0],"Rights Metadata", true, "text/xml", null, tempURL,"X", "A", null, null, "Added by RODA Connector API");
							}
							if(r.getSource() !=null){
								logger.debug("Adding SOURCE datastream to "+rObjectPID);
								ByteArrayOutputStream bos = new ByteArrayOutputStream();
								ScapeMarshaller.newInstance().serialize(r.getSource(), bos);
								String tempURL = browser.getFedoraClientUtility().temporaryUpload(bos.toByteArray());
								browser.getFedoraClientUtility().getAPIM().addDatastream(rObjectPID, "SOURCE", new String[0],"Source Metadata", true, "text/xml", null, tempURL,"X", "A", null, null, "Added by RODA Connector API");
							}
							if(r.getTechnical() !=null){
								logger.debug("Adding TECHNICAL datastream to "+rObjectPID);
								ByteArrayOutputStream bos = new ByteArrayOutputStream();
								ScapeMarshaller.newInstance().serialize(r.getTechnical(), bos);
								String tempURL = browser.getFedoraClientUtility().temporaryUpload(bos.toByteArray());
								browser.getFedoraClientUtility().getAPIM().addDatastream(rObjectPID, "TECHNICAL", new String[0],"Technical Metadata", true, "text/xml", null, tempURL,"X", "A", null, null, "Added by RODA Connector API");
							}
							
							/*
							if(r.getFiles()!=null && r.getFiles().size()>0){
								for(eu.scape_project.model.File file : r.getFiles()){
									if(file.getBitStreams()!=null && file.getBitStreams().size()>0){
										for(BitStream bs : file.getBitStreams()){
											logger.debug("Adding bitstream to Fedora...");
											ByteArrayOutputStream bos = new ByteArrayOutputStream();
											ScapeMarshaller.newInstance().serialize(bs.getTechnical(), bos);
											String tempURL = browser.getFedoraClientUtility().temporaryUpload(bos.toByteArray());
											browser.getFedoraClientUtility().getAPIM().addDatastream(rObjectPID, bs.getIdentifier().toString()+"#"+"TECHNICAL", new String[0],"Technical Metadata", true, "text/xml", null, tempURL,"X", "A", null, null, "Added by RODA Connector API");
										}
									}
								}
							}
							*/
							representationPIDS.add(rObjectPID);
							logger.debug("Uploading RepresentationFiles");
							for(RepresentationFile rf : representationFilesToUpload){
								logger.debug("Uploading RepresentationFile "+rf.getId());
								uploader.uploadRepresentationFile(rObjectPID, rf);
							}
							logger.debug("End of upload");
						}
					}catch(Exception e){
						logger.error("Error while ingesting representation: "+e.getMessage(),e);
					}
				}
				reportItem.addAttribute(new Attribute("RepresentationObjects ingestion - end datetime",DateParser.getIsoDate(new Date())));
			}
			AgentPreservationObject agentPO = null;
			reportItem.addAttribute(new Attribute("Register event - start datetime", DateParser.getIsoDate(new Date())));
			agentPO = new AgentPreservationObject();
			agentPO.setAgentName("Data Connector");
			
			agentPO.setAgentType(AgentPreservationObject.PRESERVATION_AGENT_TYPE_INGEST_TASK);//TODO (replace with P_A_...DATACONNECTOR
			agentPO.setAgentName("DATA CONNECTOR - INGEST");
			reportItem.addAttribute(new Attribute("Register event - event agent", agentPO.toString()));
			EventPreservationObject eventPO = new EventPreservationObject();
			eventPO.setEventDetail("The IntellectualEntity with ID = '"+descriptionObject.getId()+"' was successfully ingested (PID='"+descriptionObjectPID+"')");
			eventPO.setAgentRole(EventPreservationObject.PRESERVATION_EVENT_AGENT_ROLE_INGEST_TASK);
			eventPO.setEventType(EventPreservationObject.PRESERVATION_EVENT_TYPE_DATACONNECTOR_INGESTION);
			eventPO.setOutcome("success");
			eventPO.setOutcomeDetailNote("Connector details");
			eventPO.setOutcomeDetailExtension("no details");
			
			epoPID = ingest.registerEvent(descriptionObjectPID, eventPO, agentPO);
			
			reportItem.addAttributes(new Attribute("Register event - event PID", epoPID), new Attribute("finnish datetime", DateParser.getIsoDate(new Date())));
			
			report.addItem(reportItem);
			
			report.addAttribute(new Attribute("Finish datetime",DateParser.getIsoDate(new Date())));
			reportManager.insertReport(report);
			logger.debug("REGISTER INGEST EVENT");
			String details = "The IntellectualEntity with ID = '"+descriptionObject.getId()+"' was successfully ingested (PID='"+descriptionObjectPID+"')";
			if(planID!=null){
				details+="<br/>[Plan:"+planID+"]";
			}
			ingest.registerIngestEvent(new String[]{descriptionObjectPID}, representationPIDS.toArray(new String[representationPIDS.size()]), null, agentPO.getAgentName(), details);
			
			if(descriptionObjectPID!=null){
				return descriptionObject.getId();
			}else{
				logger.error("Error while creating DescriptionObject");
				throw new DataConnectorException("Error while creating DescriptionObject");
			}
		} catch (ReportRegistryException e) {
			error = "Report registry exception while registering event";
			exception = e;
		} catch (IngestException e) {
			error = "Ingestion exception while creating description object";
			logger.error(error+": "+e.getMessage(),e);
			exception = e;
		} catch (MalformedURLException e) {
			error = "MalformedURL exception while getting IngestHelper";
			logger.error(error+": "+e.getMessage(),e);
			exception = e;
		} catch (EadCMetadataException e) {
			error = "EadCMetadataException exception while converting from IntellectualEntity to DescriptionObject";
			logger.error(error+": "+e.getMessage(),e);
			exception = e;
		} catch (IOException e) {
			error = "IO exception while converting from IntellectualEntity to DescriptionObject";
			logger.error(error+": "+e.getMessage(),e);
			exception = e;
		} catch (JAXBException e) {
			error = "JAXB exception while converting from IntellectualEntity to DescriptionObject";
			logger.error(error+": "+e.getMessage(),e);
			exception = e;
		}  catch (NoSuchRODAObjectException e) {
			error = "NoSuchRodaObject exception while creating description object";
			logger.error(error+": "+e.getMessage(),e);
			exception = e;
		} catch (InvalidDescriptionObjectException e) {
			error = "InvalidDescriptionObject exception while creating description object";
			logger.error(error+": "+e.getMessage(),e);
			exception = e;
		} catch (InvalidDescriptionLevel e) {
			error = "InvalidDescriptionLevel exception while getting original DescriptionObject";
			logger.error(error+": "+e.getMessage(),e);
			exception = e;
		} catch (EditorException e) {
			error = "Editor exception while getting IngestHelper";
			logger.error(error+": "+e.getMessage(),e);
			exception = e;
		} catch (RepresentationAlreadyPreservedException e) {
			error = "RepresentationAlreadyPreserved exception while getting IngestHelper";
			logger.error(error+": "+e.getMessage(),e);
			exception = e;
		} catch (FedoraClientException e) {
			error = "FedoraClientException exception while getting IngestHelper";
			logger.error(error+": "+e.getMessage(),e);
			exception = e;
		}
		report.addItem(generateErrorReportItem("IntellectualEntity ingestion error report",error,exception));
		if(reportManager!=null){
			try {
				closeReport(reportManager,report);
			} catch (ReportRegistryException e) {
				logger.error("Error while closing report:"+e.getMessage(),e);
			}
		}
		throw new DataConnectorException("Report registry exception while registering event",exception);
	}
	


	class IngestionThread implements Runnable {
		IntellectualEntity ie;
		String descriptionObjectID;
		BrowserHelper browser;
		EditorHelper editor;
		IngestHelper ingest;
		Uploader uploader;
		String planID;
		   public IngestionThread(IntellectualEntity ie,EditorHelper editor, IngestHelper ingest, BrowserHelper browser,Uploader uploader,String planID) {
		       this.ie = ie;
		       this.ingest = ingest; 
		       this.editor = editor;
		       this.browser = browser;
		       this.uploader = uploader;
		       this.planID=planID;
		   }

		   public void run() {
			   try{
				   descriptionObjectID = ingest(ie, editor,ingest,browser,uploader,planID);
				   IngestionUtils.getInstance().addRodaID(ie,descriptionObjectID);
			   }catch(Exception e){
				   e.printStackTrace();
			   }
		   }
		}


	public void addRodaID(IntellectualEntity ie, String rodaID) {
		String statusID = getStatusIDFromEntityID(ie.getIdentifier().getValue());
		if(statusID!=null){
			statusIDToEntityMapping.remove(statusID);
		}
		
	}

	
	private ReportItem generateErrorReportItem(String reportItemTitle,String message, Exception e) {
		logger.info(message + " - " + e.getMessage(), e);
		ReportItem reportItem = new ReportItem(reportItemTitle);
		reportItem.addAttribute(new Attribute("Error", e.getMessage()));
		return reportItem;
	}

	public DescriptionObject update(String entityID, IntellectualEntity entity, EditorHelper editor,BrowserHelper browser, Uploader uploader,IngestHelper ingest, String planID) throws DataConnectorException{
		Report report = new Report();
		ReportManager reportManager = null;
		String error;
		Exception exception;
		
		try {
			reportManager = ReportManager.getDefaultReportManager();
		} catch (ReportManagerException e) {
			report.addItem(generateErrorReportItem("IntellectualEntity ingestion error report","ReportManager exception while getting default ReportManager",e));
			throw new DataConnectorException("ReportManager exception while getting default ReportManager",e);
		}
		try{
			
			
			report.setType(Report.TYPE_DATACONNECTOR_REPORT);
			report.setTitle("Data connector IntellectualEntity update.");
			report.addAttribute(new Attribute("Start datetime", DateParser.getIsoDate(new Date())));
			
			ReportItem reportItem = new ReportItem("IntellectualEntity update");
			reportItem.addAttribute(new Attribute("Start datetime", DateParser.getIsoDate(new Date())));
			
			reportItem.addAttribute(new Attribute("IntellectualEntity to DescriptionObject - start datetime",DateParser.getIsoDate(new Date())));
			DescriptionObject newDO = DataModelUtils.getInstance(browser,editor).intellectualEntityToDescriptionObject(entity);
			newDO.setId(entity.getIdentifier().getValue());
			reportItem.addAttribute(new Attribute("IntellectualEntity to DescriptionObject - end datetime",DateParser.getIsoDate(new Date())));
			
			reportItem.addAttribute(new Attribute("DescriptionObject update - start datetime",DateParser.getIsoDate(new Date())));
			
			String originalPID = Utils.findById(entityID, browser).getPid();
			newDO.setPid(originalPID);
			
			newDO = editor.modifyDescriptionObject(newDO);
			
			
			if(entity.getDescriptive()!=null){
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				ScapeMarshaller.newInstance().serialize(entity.getDescriptive(), bos);
				String tempURL = browser.getFedoraClientUtility().temporaryUpload(bos.toByteArray());
				browser.getFedoraClientUtility().getAPIM().modifyDatastreamByValue(newDO.getPid(), "DESCRIPTIVE", null,
						"Descriptive Metadata", "text/xml", null,
						bos.toByteArray(), null, null,
						"Modified by RODA Core", false);
			}
			RepresentationObject[] oldRepresentations = browser.getDORepresentations(newDO.getPid());
			List<String> repPids = new ArrayList<String>();
			for(RepresentationObject ro : oldRepresentations){
				repPids.add(ro.getPid());
				
			}
			if(repPids.size()>0){
				editor.removeObjects(repPids);
			}
			logger.debug("Ingesting representations...");
			List<String> representationPIDS = new ArrayList<String>();
			if(entity.getRepresentations()!=null && entity.getRepresentations().size()>0){
				reportItem.addAttribute(new Attribute("RepresentationObjects ingestion - start datetime",DateParser.getIsoDate(new Date())));
				File temp = TempDir.createUniqueTemporaryDirectory("representations");
				for(Representation r : entity.getRepresentations()){
					try{
						RepresentationObject ro = DataModelUtils.getInstance(browser,editor).representationToRepresentationObject(r);
						logger.debug("RepresentationObject ID1: "+ro.getId());
						ro.setDescriptionObjectPID(newDO.getPid());
						logger.debug("Processing representation files...");
						List<RepresentationFile> representationFilesToUpload = new ArrayList<RepresentationFile>();
						if(r.getFiles()!=null && r.getFiles().size()>0){
							for(eu.scape_project.model.File file : r.getFiles()){
								try{
									String fileName = file.getFilename();
									if(fileName==null){
										String[] values=file.getUri().toString().split("/");
										fileName=values[values.length-1];
									}
									File f = new File(temp,fileName);
									IOUtils.copy(file.getUri().toURL().openStream(), new FileOutputStream(f));
									String mimetype = FormatUtility.getMimetype(f);
									RepresentationFile rFile = new RepresentationFile(
										file.getIdentifier().getValue(),
										f.getName(),
										mimetype==null?"application/octet-stream":mimetype, 
										f.length(), 
										f.toURI().toURL().toExternalForm()
									);
									logger.debug("Adding PartFile "+rFile.getId());
									ro.addPartFile(rFile);
									representationFilesToUpload.add(rFile);
								}catch(Exception e){
									
									logger.error("Error while getting file from URL:"+file.getUri().toString()+": "+e.getMessage(),e);
								}
								
							}
						}
						ro.setType(RepresentationObject.UNKNOWN);
						ro.setStatuses(new String[] { RepresentationObject.STATUS_ORIGINAL });
						ro.setDescriptionObjectPID(newDO.getPid());
						logger.debug("RepresentationFiles to upload: "+representationFilesToUpload.size());
						if(representationFilesToUpload.size()>0){
							logger.debug("Creating RepresentationObject with ID:"+ro.getId());
							String rObjectPID = ingest.createRepresentationObject(ro);
							
							if(r.getProvenance()!=null){
								ByteArrayOutputStream bos = new ByteArrayOutputStream();
								ScapeMarshaller.newInstance().serialize(r.getProvenance(), bos);
								byte[] bytearray = bos.toByteArray();
								String tempURL = browser.getFedoraClientUtility().temporaryUpload(bytearray);
								try{
									browser.getFedoraClientUtility().getAPIM().addDatastream(rObjectPID, "PROVENANCE", new String[0],"Provenance Metadata", true, "text/xml", null, tempURL,"X", "A", null, null, "Added by RODA Connector API");
								}catch(Exception e){
									browser.getFedoraClientUtility().getAPIM().modifyDatastreamByValue(rObjectPID, "PROVENANCE", null,"Provenance Metadata", "text/xml", null,bytearray, null, null,"Modified by RODA Connector API", false);
								}
							}
							if(r.getRights()!=null){
								ByteArrayOutputStream bos = new ByteArrayOutputStream();
								ScapeMarshaller.newInstance().serialize(r.getRights(), bos);
								byte[] bytearray = bos.toByteArray();
								String tempURL = browser.getFedoraClientUtility().temporaryUpload(bytearray);
								try{
									browser.getFedoraClientUtility().getAPIM().addDatastream(rObjectPID, "RIGHTS", new String[0],"Rights Metadata", true, "text/xml", null, tempURL,"X", "A", null, null, "Added by RODA Connector API");
								}catch(Exception e){
									browser.getFedoraClientUtility().getAPIM().modifyDatastreamByValue(rObjectPID, "RIGHTS", null,"Rights Metadata", "text/xml", null,bytearray, null, null,"Modified by RODA Connector API", false);
								}
							}
							if(r.getSource()!=null){
								ByteArrayOutputStream bos = new ByteArrayOutputStream();
								ScapeMarshaller.newInstance().serialize(r.getSource(), bos);
								byte[] bytearray = bos.toByteArray();
								String tempURL = browser.getFedoraClientUtility().temporaryUpload(bytearray);
								try{
									browser.getFedoraClientUtility().getAPIM().addDatastream(rObjectPID, "SOURCE", new String[0],"Source Metadata", true, "text/xml", null, tempURL,"X", "A", null, null, "Added by RODA Connector API");
								}catch(Exception e){
									browser.getFedoraClientUtility().getAPIM().modifyDatastreamByValue(rObjectPID, "SOURCE", null,"Source Metadata", "text/xml", null,bytearray, null, null,"Modified by RODA Connector API", false);
								}
							}
							if(r.getTechnical()!=null){
								ByteArrayOutputStream bos = new ByteArrayOutputStream();
								ScapeMarshaller.newInstance().serialize(r.getTechnical(), bos);
								byte[] bytearray = bos.toByteArray();
								String tempURL = browser.getFedoraClientUtility().temporaryUpload(bytearray);
								try{
									browser.getFedoraClientUtility().getAPIM().addDatastream(rObjectPID, "TECHNICAL", new String[0],"Technical Metadata", true, "text/xml", null, tempURL,"X", "A", null, null, "Added by RODA Connector API");
								}catch(Exception e){
									browser.getFedoraClientUtility().getAPIM().modifyDatastreamByValue(rObjectPID, "TECHNICAL", null,"Technical Metadata", "text/xml", null,bytearray, null, null,"Modified by RODA Connector API", false);
								}
							}
							
							representationPIDS.add(rObjectPID);
							logger.debug("RepresentationObject PID:"+rObjectPID);
							logger.debug("Uploading RepresentationFiles");
							for(RepresentationFile rf : representationFilesToUpload){
								logger.debug("Uploading RepresentationFile "+rf.getId());
								uploader.uploadRepresentationFile(rObjectPID, rf);
							}
							logger.debug("End of upload");
						}
					}catch(Exception e){
						logger.error("Error while ingesting representation: "+e.getMessage(),e);
					}
				}
				reportItem.addAttribute(new Attribute("RepresentationObjects ingestion - end datetime",DateParser.getIsoDate(new Date())));
			}
			
			reportItem.addAttribute(new Attribute("DescriptionObject update - end datetime",DateParser.getIsoDate(new Date())));
					
			
			
			AgentPreservationObject agentPO = null;
			reportItem.addAttribute(new Attribute("Register event - start datetime", DateParser.getIsoDate(new Date())));
			agentPO = new AgentPreservationObject();
			agentPO.setAgentName("Data Connector");
			
			agentPO.setAgentType(AgentPreservationObject.PRESERVATION_AGENT_TYPE_DATACONNECTOR);//TODO (replace with P_A_...DATACONNECTOR
			agentPO.setAgentName("DATA CONNECTOR");
			reportItem.addAttribute(new Attribute("Register event - event agent", agentPO.toString()));
			EventPreservationObject eventPO = new EventPreservationObject();
			eventPO.setEventDetail("The IntellectualEntity with ID = '"+newDO.getId()+"' was successfully updated (PID='"+newDO.getPid()+"')");
			eventPO.setAgentRole(EventPreservationObject.PRESERVATION_EVENT_TYPE_NORMALIZATION);
			eventPO.setEventType(EventPreservationObject.PRESERVATION_EVENT_TYPE_DATACONNECTOR_INGESTION);
			eventPO.setOutcome("success");
			eventPO.setOutcomeDetailNote("Connector details");
			eventPO.setOutcomeDetailExtension("no details");
			String epoPID = ingest.registerEvent(newDO.getPid(), eventPO, agentPO);
			
			reportItem.addAttributes(new Attribute("Register event - event PID", epoPID), new Attribute("finnish datetime", DateParser.getIsoDate(new Date())));
			
			report.addItem(reportItem);
			report.addAttribute(new Attribute("Finish datetime",DateParser.getIsoDate(new Date())));
			reportManager.insertReport(report);
			logger.debug("REGISTER INGEST EVENT");
			String details = "The IntellectualEntity with ID = '"+newDO.getId()+"' was successfully ingested (PID='"+newDO.getPid()+"')";
			if(planID!=null){
				details+="<br/>[Plan:"+planID+"]";
			}
			ingest.registerIngestEvent(new String[]{newDO.getPid()}, representationPIDS.toArray(new String[representationPIDS.size()]), null, agentPO.getAgentName(), details);
			
			
			
			
			
			
			
			
			
			
			
			
			return newDO;
		} catch (ReportRegistryException e) {
			error = "Report registry exception while registering event";
			exception = e;
		} catch (IngestException e) {
			error = "Ingestion exception while creating description object";
			exception = e;
		} catch (MalformedURLException e) {
			error = "MalformedURL exception while getting IngestHelper";
			exception = e;
		} catch (EadCMetadataException e) {
			error = "EadCMetadataException exception while converting from IntellectualEntity to DescriptionObject";
			exception = e;
		} catch (IOException e) {
			error = "IO exception while converting from IntellectualEntity to DescriptionObject";
			exception = e;
		} catch (JAXBException e) {
			error = "JAXB exception while converting from IntellectualEntity to DescriptionObject";
			exception = e;
		}  catch (NoSuchRODAObjectException e) {
			error = "NoSuchRodaObject exception while creating description object";
			exception = e;
		} catch (InvalidDescriptionObjectException e) {
			error = "InvalidDescriptionObject exception while creating description object";
			exception = e;
		} catch (InvalidDescriptionLevel e) {
			error = "InvalidDescriptionLevel exception while getting original DescriptionObject";
			exception = e;
		} catch (EditorException e) {
			error = "Editor exception while getting IngestHelper";
			exception = e;
		} catch (BrowserException e) {
			error = "Browser exception while getting updated IntellectualEntity";
			exception = e;
		} catch (FedoraClientException e) {
			error = "FedoraClientException while getting updated IntellectualEntity";
			exception = e;
		} catch (RepresentationAlreadyPreservedException e) {
			error = "RepresentationAlreadyPreservedException";
			exception = e;
		} 
		report.addItem(generateErrorReportItem("IntellectualEntity ingestion error report",error,exception));
		if(reportManager!=null){
			try {
				closeReport(reportManager,report);
			} catch (ReportRegistryException e) {
				logger.error("Error while closing report:"+e.getMessage(),e);
			}
		}
		throw new DataConnectorException("Report registry exception while registering event",exception);
	}

	private void closeReport(ReportManager reportManager, Report report) throws ReportRegistryException {
		report.addAttribute(new Attribute("Finish datetime",DateParser.getIsoDate(new Date())));
		reportManager.insertReport(report);
	}

	public DescriptionObject updateDescriptionObjectMetadata(DescriptionObject descriptionObject, Object metadata,EditorHelper editor,BrowserHelper browser,IngestHelper ingest) throws DataConnectorException {
		logger.debug("updateDescriptionObjectMetadata()");
		Report report = new Report();
		ReportManager reportManager = null;
		String error;
		Exception exception;
		
		try {
			reportManager = ReportManager.getDefaultReportManager();
		} catch (ReportManagerException e) {
			report.addItem(generateErrorReportItem("IntellectualEntity ingestion error report","ReportManager exception while getting default ReportManager",e));
			throw new DataConnectorException("ReportManager exception while getting default ReportManager",e);
		}
		try{
			report.setType(Report.TYPE_DATACONNECTOR_REPORT);
			report.setTitle("Data connector IntellectualEntity update.");
			report.addAttribute(new Attribute("Start datetime", DateParser.getIsoDate(new Date())));
			
			ReportItem reportItem = new ReportItem("IntellectualEntity update");
			reportItem.addAttribute(new Attribute("Start datetime", DateParser.getIsoDate(new Date())));
			
			
			
			reportItem.addAttribute(new Attribute("DescriptionObject update - start datetime",DateParser.getIsoDate(new Date())));
			descriptionObject = DataModelUtils.getInstance(browser, editor).updateDescriptionObjectMetadata(descriptionObject,metadata);
			logger.debug("ScopeAndContent: "+descriptionObject.getScopecontent());
			descriptionObject = editor.modifyDescriptionObject(descriptionObject);
			reportItem.addAttribute(new Attribute("DescriptionObject update - end datetime",DateParser.getIsoDate(new Date())));
			report.addItem(reportItem);
			
			report.addAttribute(new Attribute("Finish datetime",DateParser.getIsoDate(new Date())));
			reportManager.insertReport(report);
			
			AgentPreservationObject agentPO = null;
			reportItem.addAttribute(new Attribute("Register event - start datetime", DateParser.getIsoDate(new Date())));
			agentPO = new AgentPreservationObject();
			agentPO.setAgentName("Data Connector");
			agentPO.setAgentType(AgentPreservationObject.PRESERVATION_AGENT_TYPE_DATACONNECTOR);
			agentPO.setAgentName("DATA CONNECTOR");
			reportItem.addAttribute(new Attribute("Register event - event agent", agentPO.toString()));
			EventPreservationObject eventPO = new EventPreservationObject();
			
			eventPO.setEventDetail("The metadata of IntellectualEntity with ID = '"+descriptionObject.getId()+"' were successfully updated");
			eventPO.setAgentRole(EventPreservationObject.PRESERVATION_EVENT_AGENT_ROLE_INGEST_TASK);
			eventPO.setEventType(EventPreservationObject.PRESERVATION_EVENT_TYPE_UPDATE);
			eventPO.setOutcome("success");
			eventPO.setOutcomeDetailNote("Connector details");
			eventPO.setOutcomeDetailExtension("no details");
			String epoPID = ingest.registerEvent(descriptionObject.getPid(), eventPO, agentPO);
			reportItem.addAttributes(new Attribute("Register event - event PID", epoPID), new Attribute("finnish datetime", DateParser.getIsoDate(new Date())));
			return descriptionObject;
		} catch (ReportRegistryException e) {
			error = "Report registry exception while registering event";
			exception = e;
		} catch (IngestException e) {
			error = "Ingestion exception while creating description object";
			exception = e;
		} catch (NoSuchRODAObjectException e) {
			error = "NoSuchRodaObject exception while creating description object";
			exception = e;
		} catch (InvalidDescriptionObjectException e) {
			error = "InvalidDescriptionObject exception while creating description object";
			exception = e;
		} catch (InvalidDescriptionLevel e) {
			error = "InvalidDescriptionLevel exception while getting original DescriptionObject";
			exception = e;
		} catch (EditorException e) {
			error = "Editor exception while getting IngestHelper";
			exception = e;
		} catch (BrowserException e) {
			error = "Editor exception while modifying DO";
			exception = e;
		} 
		report.addItem(generateErrorReportItem("IntellectualEntity ingestion error report",error,exception));
		if(reportManager!=null){
			try {
				closeReport(reportManager,report);
			} catch (ReportRegistryException e) {
				logger.error("Error while closing report:"+e.getMessage(),e);
			}
		}
		throw new DataConnectorException("Report registry exception while registering event",exception);
	}

	public void updateRepresentation(String descriptionObjectPid,RepresentationObject oldRepresentationObject,RepresentationObject newRepresentationObject,IngestHelper ingest, Uploader uploader,List<eu.scape_project.model.File> files) throws IOException, EditorException, NoSuchRODAObjectException, UploadException {
		
		File temp = TempDir.createUniqueTemporaryDirectory("representations");
		List<RepresentationFile> representationFilesToUpload = new ArrayList<RepresentationFile>();
		if(files!=null && files.size()>0){
			
			for(eu.scape_project.model.File file : files){
				try{
					String fileName = file.getFilename();
					if(fileName==null){
						String[] values=file.getUri().toString().split("/");
						fileName=values[values.length-1];
					}
					File f = new File(temp,fileName);
					IOUtils.copy(file.getUri().toURL().openStream(), new FileOutputStream(f));
					RepresentationFile rFile = new RepresentationFile(
						file.getIdentifier().getValue(),
						f.getName(),
						"application/octet-stream", 
						f.length(), 
						f.toURI().toURL().toExternalForm()
					);
					newRepresentationObject.addPartFile(rFile);
					representationFilesToUpload.add(rFile);
				}catch(Exception e){
					logger.error("Error while getting file from URL:"+file.getUri().toString()+": "+e.getMessage(),e);
				}
				
			}
		}
		newRepresentationObject.setType(RepresentationObject.UNKNOWN);
		newRepresentationObject.setStatuses(new String[] { RepresentationObject.STATUS_ORIGINAL });
		newRepresentationObject.setDescriptionObjectPID(descriptionObjectPid);
		String rObjectPID = ingest.createRepresentationObject(newRepresentationObject);
		for(RepresentationFile rf : representationFilesToUpload){
			logger.debug("Uploading RepresentationFile "+rf.getId());
			uploader.uploadRepresentationFile(rObjectPID, rf);
		}
		logger.debug("End of upload");
		
	}
}
