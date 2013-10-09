package eu.scape_project.roda.core.plan;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.util.DateParser;
import org.w3c.util.InvalidDateException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import pt.gov.dgarq.roda.util.JAXBUtility;
import at.ac.tuwien.ifs.dp.plato.ObjectFactory;
import at.ac.tuwien.ifs.dp.plato.Plans;
import fr.prados.xpath4sax.SAXXPath;
import fr.prados.xpath4sax.XPathSyntaxException;
import fr.prados.xpath4sax.XPathXMLHandler;

public class Plan {
	static final private Logger logger = Logger.getLogger(Plan.class);

	/**
	 * The directory where the plan is stored.
	 */
	private File directory;

	/**
	 * The plan identifier.
	 */
	private String id;

	/**
	 * The {@link Date} when the identifier was reserved.
	 */
	private Date idReserveDate;

	/**
	 * The user that reserved the identifier.
	 */
	private String idReserveUser;

	/**
	 * Is the Plan enabled?
	 */
	private boolean enabled;

	/**
	 * The {@link Date} when the plan was deployed (uploaded).
	 */
	private Date deployDate;

	/**
	 * The user that deployed the plan.
	 */
	private String deployUser;

	/**
	 * The author of the plan.
	 */
	private String author;

	/**
	 * The description of the plan.
	 */
	private String description;

	/**
	 * The MD5 checksum of the plan.
	 */
	private String md5sum;

	/**
	 * The SHA1 checksum of the plan.
	 */
	private String sha1sum;

	/**
	 * The number of files targeted by this plan.
	 */
	private long numberOfFiles = -1;

	/**
	 * Constructs a new {@link Plan} with a random identifier.
	 */
	public Plan() {
		setId(UUID.randomUUID().toString());
		setIdReserveDate(new Date());
	}

	/**
	 * Constructs a {@link Plan} with the specified identifier and plan file.
	 */
	public Plan(String id, String plan) {
		this.id = id;
	}

	/**
	 * @return the directory
	 */
	public File getDirectory() {
		return directory;
	}

	/**
	 * @param directory
	 *            the directory to set
	 */
	public void setDirectory(File directory) {
		this.directory = directory;
	}

	/**
	 * Gets the {@link Plan} identifier.
	 * 
	 * @return
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the {@link Plan} identifier.
	 * 
	 * @param id
	 *            the {@link Plan} identifier.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @return the idReserveDate
	 */
	public Date getIdReserveDate() {
		return idReserveDate;
	}

	/**
	 * @param idReserveDate
	 *            the idReserveDate to set
	 */
	public void setIdReserveDate(Date idReserveDate) {
		this.idReserveDate = idReserveDate;
	}

	/**
	 * @return the idReserveUser
	 */
	public String getIdReserveUser() {
		return idReserveUser;
	}

	/**
	 * @param idReserveUser
	 *            the idReserveUser to set
	 */
	public void setIdReserveUser(String idReserveUser) {
		this.idReserveUser = idReserveUser;
	}

	/**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * @param enabled
	 *            the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * @return the deployDate
	 */
	public Date getDeployDate() {
		return deployDate;
	}

	/**
	 * @param deployDate
	 *            the deployDate to set
	 */
	public void setDeployDate(Date deployDate) {
		this.deployDate = deployDate;
	}

	/**
	 * @return the deployUser
	 */
	public String getDeployUser() {
		return deployUser;
	}

	/**
	 * @param deployUser
	 *            the deployUser to set
	 */
	public void setDeployUser(String deployUser) {
		this.deployUser = deployUser;
	}

	/**
	 * @return the md5sum
	 */
	public String getMD5sum() {
		return md5sum;
	}

	/**
	 * @param md5sum
	 *            the md5sum to set
	 */
	public void setMD5sum(String md5sum) {
		this.md5sum = md5sum;
	}

	/**
	 * @return the sha1sum
	 */
	public String getSHA1sum() {
		return sha1sum;
	}

	/**
	 * @param sha1sum
	 *            the sha1sum to set
	 */
	public void setSHA1sum(String sha1sum) {
		this.sha1sum = sha1sum;
	}

	/**
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * @param author
	 *            the author to set
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description
	 *            the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the numberOfFiles
	 */
	public long getNumberOfFiles() {
		return numberOfFiles;
	}

	/**
	 * @param numberOfFiles
	 *            the numberOfFiles to set
	 */
	public void setNumberOfFiles(long numberOfFiles) {
		this.numberOfFiles = numberOfFiles;
	}

	@Override
	public String toString() {
		return String.format("Plan (%s)", getId());
	}

	@Override
	public boolean equals(Object other) {
		if (other != null && other instanceof Plan) {
			Plan otherPlan = (Plan) other;
			return getId().equals(otherPlan.getId());
		} else {
			return false;
		}
	}

	public String getMetadataFilename() {
		return getId() + ".metadata";
	}

	public String getDataFilename() {
		return getId() + ".data";
	}

	/**
	 * Returns an {@link InputStream} to the Plan's data if it exists.
	 * 
	 * @return an {@link InputStream} to the Plan's data if the {@link Plan} was
	 *         already deployed or <code>null</code> otherwise.
	 * @throws PlanException
	 *             if the Plan directory is not set
	 */
	public InputStream getDataInputStream() throws PlanException {
		if (getDirectory() == null) {
			throw new PlanException("Plan directory is not set");
		} else {
			File dataFile = new File(getDirectory(), getDataFilename());

			InputStream stream = null;

			try {

				stream = new FileInputStream(dataFile);

			} catch (FileNotFoundException e) {
				logger.debug("Plan " + getId() + " doesn't have data yet - "
						+ e.getMessage(), e);
			}

			return stream;
		}
	}

	/**
	 * Stores the {@link Plan} metadata to disk.
	 * 
	 * @return the {@link File} where the metadata was stored.
	 * @throws PlanException
	 *             if the {@link Plan} directory is not set.
	 */
	public File store() throws PlanException {
		if (getDirectory() == null) {
			throw new PlanException("Plan directory is not set");
		} else {
			return store(getDirectory());
		}
	}

	/**
	 * Stores the {@link Plan} metadata to the specified directory.
	 * <p>
	 * <strong>NOTE:</strong> this method does not change the default
	 * {@link Plan} directory. To change it use method
	 * {@link #setDirectory(File)}.
	 * </p>
	 * 
	 * @param directory
	 *            the directory where the {@link Plan} metadata should be
	 *            stored.
	 * 
	 * @return the {@link File} where the metadata was stored.
	 * @throws PlanException
	 *             if an I/O error occurs writing metadata to disk.
	 */
	public File store(File directory) throws PlanException {
		try {

			File metadataFile = new File(directory, getMetadataFilename());

			PropertiesConfiguration meta = new PropertiesConfiguration();
			meta.addProperty("ID", getId());
			meta.addProperty("IDReserveDate",
					DateParser.getIsoDate(getIdReserveDate()));
			meta.addProperty("IDReserveUser", getIdReserveUser());
			meta.addProperty("Enabled", isEnabled());
			if (getDeployDate() != null) {
				meta.addProperty("DeployDate",
						DateParser.getIsoDate(getDeployDate()));
			}
			meta.addProperty("DeployUser", getDeployUser());
			meta.addProperty("MD5sum", getMD5sum());
			meta.addProperty("SHA1sum", getSHA1sum());
			meta.addProperty("Author", getAuthor());
			meta.addProperty("Description", getDescription());
			meta.addProperty("NumberOfFiles", getNumberOfFiles());

			meta.save(metadataFile);

			logger.debug("Metadata written to file " + metadataFile);

			return metadataFile;

		} catch (ConfigurationException e) {
			logger.error("Could not write plan metadata - " + e.getMessage(), e);
			throw new PlanException("Could not write plan metadata - "
					+ e.getMessage(), e);
		}
	}

	public boolean hasData() throws PlanException {
		if (getDirectory() == null) {
			throw new PlanException("Plan directory is not set");
		} else {
			File planDataFile = new File(getDirectory(), getDataFilename());
			return planDataFile.exists();
		}
	}

	public File storeData(Plans plansDocument) throws PlanException {

		if (hasData()) {

			throw new PlanException("Plan already has data");

		} else {

			try {

				File planDataFile = new File(getDirectory(), getDataFilename());
				Writer writer = new FileWriter(planDataFile);
				JAXBUtility
						.marshal(
								new ObjectFactory().createPlans(plansDocument),
								true,
								false,
								null,
								"at.ac.tuwien.ifs.dp.plato:net.sf.taverna._2008.xml.t2flow",
								writer);
				writer.close();

				return planDataFile;

			} catch (IOException e) {
				logger.debug(
						"Couln't marshal plan into file - " + e.getMessage(), e);
				throw new PlanException("Couln't marshal plan into file - "
						+ e.getMessage(), e);
			} catch (JAXBException e) {
				logger.debug(
						"Couln't marshal plan into file - " + e.getMessage(), e);
				throw new PlanException("Couln't marshal plan into file - "
						+ e.getMessage(), e);
			}
		}

	}

	public File storeData(InputStream data) throws PlanException {
		logger.trace("storeData(InputStream...)");

		if (hasData()) {

			throw new PlanException("Plan already has data");

		} else {

			MessageDigest md5digest = null;
			MessageDigest sha1digest = null;
			try {
				md5digest = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				logger.warn(
						"Couln't create MD5 message digest. MD5 will not be calculated - "
								+ e.getMessage(), e);
			}
			try {
				sha1digest = MessageDigest.getInstance("SHA1");
			} catch (NoSuchAlgorithmException e) {
				logger.warn(
						"Couln't create SHA1 message digest. SHA1 will not be calculated - "
								+ e.getMessage(), e);
			}

			try {

				File planDataFile = new File(getDirectory(), getDataFilename());

				if (md5digest != null) {
					data = new DigestInputStream(data, md5digest);
				}
				if (sha1digest != null) {
					data = new DigestInputStream(data, sha1digest);
				}

				FileOutputStream out = new FileOutputStream(planDataFile);
				long bytesCopied = IOUtils.copyLarge(data, out);
				out.close();

				logger.info(bytesCopied + " bytes written to " + planDataFile);

				setDeployDate(new Date());
				// setDeployUser();

				if (md5digest != null) {
					byte[] digest = md5digest.digest();
					String signature = new String(Hex.encodeHex(digest));
					setMD5sum(signature);
					logger.info("MD5 checksum: " + signature);
				}
				if (sha1digest != null) {
					byte[] digest = sha1digest.digest();
					String signature = new String(Hex.encodeHex(digest));
					setSHA1sum(signature);
					logger.info("SHA1 checksum: " + signature);
				}

				// long start = System.currentTimeMillis();
				// Document planDocument = getPlanDocument(planDataFile);
				// logger.debug("Successfully parsed plan");
				long afterRead = System.currentTimeMillis();
				// List<String> fileURLs = getFileURLsFromPlan(planDataFile);
				extractValuesFromPlan(planDataFile);
				long afterXpath = System.currentTimeMillis();

				// logger.debug("XML parse duration: " + (afterRead - start) +
				// " ms");
				logger.debug("XPath values extraction duration: "
						+ (afterXpath - afterRead) + " ms");

				// Store updated plan metadata
				store();

				return planDataFile;

			} catch (IOException e) {
				logger.debug(
						"Couln't write plan into file - " + e.getMessage(), e);
				throw new PlanException("Couln't write plan into file - "
						+ e.getMessage(), e);
			}
		}
	}

	/**
	 * Creates a new {@link Plan} from the specified Plan metadata file.
	 * 
	 * @param metadataFile
	 *            the {@link Plan} metadata file.
	 * @return
	 * @throws PlanException
	 */
	public static Plan loadPlan(File metadataFile) throws PlanException {
		try {

			FileReader metadataFileReader = new FileReader(metadataFile);
			Plan plan = loadPlan(metadataFileReader);
			plan.setDirectory(metadataFile.getParentFile());
			metadataFileReader.close();
			return plan;

		} catch (IOException e) {
			logger.debug("Couldn't load plan from file " + metadataFile + " - "
					+ e.getMessage(), e);
			throw new PlanException("Couldn't load plan from file "
					+ metadataFile + " - " + e.getMessage(), e);
		}
	}

	public static Plan loadPlan(Reader metadataFileReader) throws PlanException {
		try {

			PropertiesConfiguration metadata = new PropertiesConfiguration();
			metadata.load(metadataFileReader);

			Plan plan = new Plan();

			plan.setId(metadata.getString("ID"));
			plan.setIdReserveDate(DateParser.parse(metadata
					.getString("IDReserveDate")));
			plan.setIdReserveUser(metadata.getString("IDReserveUser"));
			plan.setEnabled(metadata.getBoolean("Enabled"));
			if (metadata.containsKey("DeployDate")) {
				plan.setDeployDate(DateParser.parse(metadata
						.getString("DeployDate")));
			}
			plan.setDeployUser(metadata.getString("DeployUser"));
			plan.setMD5sum(metadata.getString("MD5sum"));
			plan.setSHA1sum(metadata.getString("SHA1sum"));
			plan.setAuthor(metadata.getString("Author"));
			plan.setDescription(metadata.getString("Description"));
			plan.setNumberOfFiles(metadata.getLong("NumberOfFiles", -1));

			return plan;

		} catch (ConfigurationException e) {
			logger.error("Couldn't read plan metadata - " + e.getMessage(), e);
			throw new PlanException("Couldn't read plan metadata - "
					+ e.getMessage(), e);
		} catch (InvalidDateException e) {
			logger.error(
					"Couldn't parse date in plan metadata - " + e.getMessage(),
					e);
			throw new PlanException("Couldn't parse date in plan metadata - "
					+ e.getMessage(), e);
		}

	}

	/**
	 * Parses the plan XML file and returns a {@link Document} with the plan.
	 * 
	 * @param planFile
	 *            the File with the plan.
	 * @return the {@link Document} containing the plan.
	 * @throws PlanException
	 *             if an error occurs reading, parsing or creating the
	 *             {@link Document} from the plan XML file.
	 */
	private Document getPlanDocument(final File planFile) throws PlanException {

		final DocumentBuilderFactory factory = DocumentBuilderFactory
				.newInstance();

		logger.debug("DocumentBuilderFactory class: " + factory.getClass());

		factory.setNamespaceAware(true);

		try {

			final DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.parse(planFile);

		} catch (ParserConfigurationException e) {
			logger.error("Couldn't create Document - " + e.getMessage());
			throw new PlanException("Couldn't create Document - "
					+ e.getMessage(), e);
		} catch (SAXException e) {
			logger.error("Couldn't parse plan XML file - " + e.getMessage());
			throw new PlanException("Couldn't parse plan XML file - "
					+ e.getMessage(), e);
		} catch (IOException e) {
			logger.error("Couldn't read plan XML file - " + e.getMessage());
			throw new PlanException("Couldn't read plan XML file - "
					+ e.getMessage(), e);
		}
	}

	/**
	 * Gets the URLs of files this plan applies to.
	 * 
	 * @param plan
	 *            the plan
	 * @param planDataFile
	 * @return a {@link List} of file URLs.
	 * @throws PlanException
	 *             if an error occurred extracting URLs from the plan.
	 */
	private List<String> getFileURLsFromPlan(final File planDataFile)
			throws PlanException {

		String xpathSelectIDs = "/plato:plans/plato:plan/plato:preservationActionPlan/plato:objects/plato:object/@uid";

		XPathFactory xPathFactory = XPathFactory.newInstance();
		logger.debug("XPathFactory class: " + xPathFactory.getClass());

		final XPath xpath = xPathFactory.newXPath();
		xpath.setNamespaceContext(new PlanNamespaceContext());

		final List<String> fileURLs = new ArrayList<String>();

		try {

			final XPathExpression expr = xpath.compile(xpathSelectIDs);

			InputStream is = new FileInputStream(planDataFile);
			String result = expr.evaluate(new InputSource(is));
			is.close();

			logger.debug("XPath result: " + result);

			// final NodeList nl = (NodeList) expr.evaluate(plan,
			// XPathConstants.NODESET);
			//
			// for (int i = 0; i < nl.getLength(); i++) {
			// final Node item = nl.item(i);
			// fileURLs.add(item.getNodeValue());
			// }

			return fileURLs;

		} catch (XPathExpressionException e) {
			logger.debug("Couln't read file URLs in plan - " + e.getMessage());
			throw new PlanException("Couln't read file URLs in plan - "
					+ e.getMessage(), e);
		} catch (IOException e) {
			logger.debug("Couln't read file URLs in plan - " + e.getMessage());
			throw new PlanException("Couln't read file URLs in plan - "
					+ e.getMessage(), e);
		}
	}

	/**
	 * Updates the {@link Plan} metadata with values extracted from the
	 * {@link Plan} XML file.
	 * 
	 * @param planDataFile
	 *            the {@link File} that contains the {@link Plan} data.
	 * 
	 * @throws PlanException
	 *             if an error occurred extracting values from the plan.
	 */
	private void extractValuesFromPlan(final File planDataFile)
			throws PlanException {

		try {

			final List<String> fileURLs = new ArrayList<String>();

			// String xpathSelectIDs =
			// "/plato:plans/plato:plan/plato:preservationActionPlan/plato:objects/plato:object/@uid/text()";
			final SAXXPath xpathSelectUIDs = new SAXXPath(
					"/plans/plan/preservationActionPlan/objects/object/@uid");
			final SAXXPath xpathSelectAuthor = new SAXXPath(
					"/plans/plan/properties/@author");
			final SAXXPath xpathSelectDescription = new SAXXPath(
					"/plans/plan/properties/description/text()");

			XPathXMLHandler handler = new XPathXMLHandler() {
				@Override
				public void findXpathNode(SAXXPath xpath, Object objNode) {

					logger.debug("xpath=" + xpath);
					logger.debug("node=" + objNode);
					logger.debug("node.getClass()=" + objNode.getClass());

					if (objNode instanceof Node) {
						Node node = (Node) objNode;

						logger.debug(node.getNodeName() + " = "
								+ node.getNodeValue());

						if (xpathSelectUIDs.equals(xpath)) {

							fileURLs.add(node.getNodeValue());
							logger.info("Added file: " + node.getNodeValue());

						} else if (xpathSelectAuthor.equals(xpath)) {

							setAuthor(node.getNodeValue());
							logger.info("Author: " + getAuthor());

						} else if (xpathSelectDescription.equals(xpath)) {

							setDescription(node.getNodeValue());
							logger.info("Description: " + getDescription());

						} else {
							logger.warn("Ignoring unknown SAXXPath " + xpath);
						}

					} else {
						logger.warn(xpathSelectUIDs
								+ " is not a org.w3c.dom.Node - "
								+ objNode.toString());
					}

				}
			};
			handler.setXPaths(XPathXMLHandler.toXPaths(xpathSelectUIDs,
					xpathSelectAuthor, xpathSelectDescription));
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

			FileReader reader = new FileReader(planDataFile);
			parser.parse(new InputSource(reader), handler);
			reader.close();

			setNumberOfFiles(fileURLs.size());
			logger.info("NumberOfFiles: " + getNumberOfFiles());

		} catch (IOException e) {
			logger.debug("Couln't read file URLs in plan - " + e.getMessage());
			throw new PlanException("Couln't read file URLs in plan - "
					+ e.getMessage(), e);
		} catch (SAXException e) {
			logger.debug("Couln't read file URLs in plan - " + e.getMessage());
			throw new PlanException("Couln't read file URLs in plan - "
					+ e.getMessage(), e);
		} catch (ParserConfigurationException e) {
			logger.debug("Couln't read file URLs in plan - " + e.getMessage());
			throw new PlanException("Couln't read file URLs in plan - "
					+ e.getMessage(), e);
		} catch (XPathSyntaxException e) {
			logger.debug("Couln't read file URLs in plan - " + e.getMessage());
			throw new PlanException("Couln't read file URLs in plan - "
					+ e.getMessage(), e);
		}

	}

}
