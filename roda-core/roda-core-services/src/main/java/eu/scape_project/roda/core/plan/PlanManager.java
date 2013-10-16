package eu.scape_project.roda.core.plan;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import pt.gov.dgarq.roda.core.RodaWebApplication;

public enum PlanManager {
	/**
	 * Returns the default {@link PlanManager}. If it doesn't exist, a new
	 * {@link PlanManager} it will be created and returned.
	 * 
	 * @return a {@link PlanManager}.
	 * 
	 * @throws PlanException
	 *             if the {@link PlanManager} couldn't be created.
	 */
	INSTANCE;

	static final private Logger logger = Logger.getLogger(PlanManager.class);

	/**
	 * The directory where the plans are stored.
	 */
	private File plansDirectory = null;

	/**
	 * Lucene index writer.
	 */
	private IndexWriter indexWriter = null;

	/**
	 * Lucene index searcher
	 */
	private IndexSearcher indexSearcher;

	final static protected String[] planSearchFields = new String[] { "ID",
			"IDReserveDate", "IDReserveUser", "Enabled", "DeployDate",
			"DeployUser", "Author", "Description", "NumberOfFiles" };

	/**
	 * Creates a new {@link PlanManager}.
	 * 
	 * @throws PlanException
	 */
	private PlanManager() {
	}

	public synchronized void start() throws PlanException {
		logger.trace("start()");

		try {

			getIndexWriter(true);
			logger.info("Plan index created/opened successfully");

		} catch (IOException e) {
			logger.debug("Error creating/opening index - " + e.getMessage(), e);
			throw new PlanException("Error creating/opening index - "
					+ e.getMessage(), e);
		}

		// int count = reindexPlans();
		// logger.debug(count + " plans reindexed");
	}

	public synchronized void stop() {
		logger.trace("stop()");

		if (this.indexSearcher != null) {
			this.indexSearcher = null;
		}
		if (this.indexWriter != null) {
			try {
				this.indexWriter.commit();
			} catch (IOException e) {
				logger.warn("Error calling commit() on Lucene index writer - "
						+ e.getMessage(), e);
			}
			try {
				this.indexWriter.close();
			} catch (IOException e) {
				logger.warn("Error calling close() on Lucene index writer - "
						+ e.getMessage(), e);
			}
			this.indexWriter = null;
		}
	}

	/**
	 * @return the plansDirectory
	 * @throws PlanException
	 */
	private synchronized File getPlansDirectory() throws PlanException {
		logger.trace("getPlansDirectory()");

		if (this.plansDirectory == null) {
			try {

				Configuration configuration = RodaWebApplication
						.getConfiguration(getClass(), "roda-core.properties");

				File plansRelativeDir = new File("data", "plans");

				String plansDirectoryConf = configuration
						.getString("plansDirectory");
				logger.debug("roda-core.properties: plansDirectory="
						+ plansDirectoryConf);

				File plansDir = new File(RodaWebApplication.RODA_HOME,
						configuration.getString("plansDirectory",
								plansRelativeDir.toString()));

				if (plansDir.exists()) {
					plansDirectory = plansDir.getAbsoluteFile();
					logger.info("Plans directory is " + plansDirectory);
				} else {
					logger.info("Plans directory doesn't exist. Creating a new one in "
							+ plansDir.toString());

					if (plansDir.mkdirs()) {

						logger.info("Plan directory successfully created in "
								+ plansDir.toString());
						plansDirectory = plansDir.getAbsoluteFile();

					} else {
						logger.error("Plan directory could not be created in "
								+ plansDir.toString());
						throw new PlanException(
								"Plan directory could not be created in "
										+ plansDir.toString());
					}
				}

			} catch (ConfigurationException e) {
				logger.debug(
						"Error reading configuration file - " + e.getMessage(),
						e);
				throw new PlanException("Error reading configuration file - "
						+ e.getMessage(), e);
			}
		}

		return plansDirectory;
	}

	public Plan createPlan() throws PlanException {
		logger.trace("createPlan()");

		Plan plan = new Plan();
		File planMetadata = plan.store(getPlansDirectory());

		addPlanToIndex(plan);

		logger.info("Plan metadata successfully stored to " + planMetadata);

		return plan;
	}

	public Plan getPlan(String id) throws NoSuchPlanException, PlanException {
		logger.trace("getPlan(id=" + id + ")");

		File planMetadataFile = getPlanMetadataFile(id);

		logger.debug("getPlan(id=" + id + ") plan file should be "
				+ planMetadataFile.getAbsolutePath());

		if (planMetadataFile.exists()) {
			return Plan.loadPlan(planMetadataFile);
		} else {

			logger.warn("getPlan(id=" + id + "): plan file doesn't exist: "
					+ planMetadataFile.getAbsolutePath()
					+ ". Throwing NoSuchPlanException");

			throw new NoSuchPlanException("Plan " + id + " could not be found");
		}
	}

	private File getPlanMetadataFile(String id) throws PlanException {
		logger.trace("getPlanMetadataFile(" + id + ")");
		return new File(getPlansDirectory(), id + ".metadata");
	}

	private synchronized File getIndexDirectory() throws PlanException {
		logger.trace("getIndexDirectory()");
		return new File(getPlansDirectory(), "index");
	}

	private synchronized IndexWriter getIndexWriter() throws PlanException,
			IOException {
		logger.trace("getIndexWriter()");
		return getIndexWriter(false);
	}

	private synchronized IndexWriter getIndexWriter(boolean create)
			throws PlanException, IOException {
		logger.trace("getIndexWriter(create=" + create + ")");

		if (this.indexWriter == null) {

			File indexDirFile = getIndexDirectory();
			if (indexDirFile.exists() && indexDirFile.isDirectory()) {
				create = false;
				logger.info("Index already exists. Not creating again.");
			}

			Directory dir = FSDirectory.open(getIndexDirectory());
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_44);
			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_44,
					analyzer);

			if (create) {
				// Create a new index in the directory, removing any
				// previously indexed documents:
				iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
			} else {
				iwc.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
			}

			this.indexWriter = new IndexWriter(dir, iwc);

			if (create) {
				this.indexWriter.commit();
				logger.debug("Plan index created successfully");
			} else {
				logger.debug("Plan index opened successfully");
			}

		}

		return this.indexWriter;
	}

	private synchronized IndexSearcher getIndexSearcher() throws PlanException,
			IOException {
		logger.trace("getIndexSearcher()");

		if (this.indexSearcher == null) {

			IndexReader indexReader = null;
			IndexSearcher indexSearcher = null;
			Directory dir = FSDirectory.open(getIndexDirectory());
			indexReader = DirectoryReader.open(dir);
			indexSearcher = new IndexSearcher(indexReader);

			this.indexSearcher = indexSearcher;
		}

		return this.indexSearcher;
	}

	public void addPlanToIndex(Plan plan) throws PlanException {
		logger.trace("addPlanToIndex(" + plan + ")");
		addPlanToIndex(plan, true, true);
	}

	public void addPlanToIndex(Plan plan, boolean deleteExisting, boolean commit)
			throws PlanException {
		logger.trace("addPlanToIndex(plan=" + plan + ", deleteExisting="
				+ deleteExisting + ", commit=" + commit + ")");

		if (deleteExisting) {
			try {

				logger.debug("Deleting plan from index if already exists");
				getIndexWriter().deleteDocuments(
						new TermQuery(new Term("ID", plan.getId())));

			} catch (IOException e) {
				logger.warn(
						"Error trying to delete existing plan - "
								+ e.getMessage(), e);
			}
		}

		Document document = new Document();

		document.add(new StringField("ID", plan.getId(), Field.Store.YES));

		if (plan.getIdReserveDate() != null) {
			document.add(new StringField("IDReserveDate", DateTools
					.dateToString(plan.getIdReserveDate(),
							Resolution.MILLISECOND), Field.Store.YES));
		}

		if (StringUtils.isNotBlank(plan.getIdReserveUser())) {
			document.add(new StringField("IDReserveUser", plan
					.getIdReserveUser(), Field.Store.YES));
		}

		document.add(new StringField("Enabled", new Boolean(plan.isEnabled())
				.toString(), Field.Store.YES));

		if (plan.getDeployDate() != null) {
			document.add(new StringField("DeployDate", DateTools.dateToString(
					plan.getDeployDate(), Resolution.MILLISECOND),
					Field.Store.YES));
		}
		if (StringUtils.isNotBlank(plan.getDeployUser())) {
			document.add(new StringField("DeployUser", plan.getDeployUser(),
					Field.Store.YES));
		}

		if (StringUtils.isNotBlank(plan.getAuthor())) {
			document.add(new StringField("Author", plan.getAuthor(),
					Field.Store.YES));
		}

		if (StringUtils.isNotBlank(plan.getDescription())) {
			document.add(new TextField("Description", plan.getDescription(),
					Field.Store.YES));
		}

		document.add(new LongField("NumberOfFiles", plan.getNumberOfFiles(),
				Field.Store.YES));

		try {

			getIndexWriter().addDocument(document);
			if (commit) {
				getIndexWriter().commit();
			}

			logger.info("Plan " + plan.getId() + " successfully added to index");

		} catch (IOException e) {
			logger.debug("Error adding plan to index - " + e.getMessage(), e);
			throw new PlanException("Error adding plan to index - "
					+ e.getMessage(), e);
		}
	}

	public List<Plan> getPlansByField(int startIndex, int maxResults,
			String field, String value) throws PlanException {
		logger.trace("getPlansByField(" + startIndex + ", " + maxResults + ", "
				+ field + ", " + value + ")");

		if (startIndex < 0) {
			startIndex = 0;
		}
		if (maxResults <= 0) {
			maxResults = 100;
		}

		List<Plan> results = new ArrayList<Plan>();
		// Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_44);
		// QueryParser parser = new QueryParser(Version.LUCENE_44, field,
		// analyzer);

		BooleanQuery query = new BooleanQuery();
		query.add(new TermQuery(new Term(field, value)),
				BooleanClause.Occur.MUST);

		try {

			// Query query = parser.Query(value);

			ScoreDoc[] hits = getIndexSearcher().search(query,
					startIndex + maxResults).scoreDocs;

			for (int i = startIndex; i < hits.length
					&& i < startIndex + maxResults; i++) {
				try {

					Document doc = getIndexSearcher().doc(hits[i].doc);
					results.add(getPlan(doc.get("ID")));

				} catch (NoSuchPlanException e) {
					logger.warn("Error getting Plan from Lucene document "
							+ hits[i].doc + " - " + e.getMessage(), e);
				} catch (PlanException e) {
					logger.warn("Error getting Plan from Lucene document "
							+ hits[i].doc + " - " + e.getMessage(), e);
				}
			}

		} catch (IOException e) {
			logger.debug("Error searching index - " + e.getMessage(), e);
			throw new PlanException(
					"Error searching index - " + e.getMessage(), e);
		}

		return results;
	}

	public SearchResults searchPlans(int startIndex, int maxResults,
			String queryStr) throws PlanException {
		logger.trace("searchPlans(" + startIndex + ", " + maxResults + ", "
				+ queryStr + ")");

		if (startIndex < 0) {
			startIndex = 0;
		}
		if (maxResults <= 0) {
			maxResults = 100;
		}

		try {
			List<Plan> results = new ArrayList<Plan>();

			Query query = new MultiFieldQueryParser(Version.LUCENE_44,
					planSearchFields, new StandardAnalyzer(Version.LUCENE_44))
					.parse(queryStr);

			// Query query = parser.Query(value);

			TopDocs topDocs = getIndexSearcher().search(query,
					startIndex + maxResults);
			ScoreDoc[] hits = topDocs.scoreDocs;

			logger.debug("Query '" + query + "' returned " + hits.length
					+ " results");

			for (int i = startIndex; i < hits.length
					&& i < startIndex + maxResults; i++) {
				try {

					Document doc = getIndexSearcher().doc(hits[i].doc);
					results.add(getPlan(doc.get("ID")));

				} catch (NoSuchPlanException e) {
					logger.warn("Error getting Plan from Lucene document "
							+ hits[i].doc + " - " + e.getMessage(), e);
				} catch (PlanException e) {
					logger.warn("Error getting Plan from Lucene document "
							+ hits[i].doc + " - " + e.getMessage(), e);
				}
			}

			return new SearchResults(results, topDocs.totalHits);

		} catch (IOException e) {
			logger.debug("Error searching index - " + e.getMessage(), e);
			throw new PlanException(
					"Error searching index - " + e.getMessage(), e);
		} catch (ParseException e) {
			logger.debug("Error parsing query - " + e.getMessage(), e);
			throw new PlanException("Error parsing query - " + e.getMessage(),
					e);
		}

	}

	public int reindexPlans() throws PlanException {
		logger.trace("reindexPlans()");

		logger.info("Reindexing all plans");

		try {

			getIndexWriter().deleteAll();
			logger.info("Deleted all records from index");

		} catch (IOException e) {
			logger.debug(
					"Error deleting all records from index - " + e.getMessage(),
					e);
			logger.warn("Ignoring exception in deleteAll() and continuing adding Plans to index");
		}

		File[] planFiles = getPlansDirectory().listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return "metadata".equalsIgnoreCase(FilenameUtils
						.getExtension(name));
			}
		});

		int count = 0;
		for (File planFile : planFiles) {

			String planId = FilenameUtils.getBaseName(planFile.getName());

			try {

				addPlanToIndex(PlanManager.INSTANCE.getPlan(planId), false,
						false);
				count++;

			} catch (NoSuchPlanException e) {
				logger.warn(
						"Error reading Plan " + planId + " - " + e.getMessage(),
						e);
			} catch (PlanException e) {
				logger.warn(
						"Error reading Plan " + planId + " - " + e.getMessage(),
						e);
			}
		}

		try {

			getIndexWriter().commit();
			logger.info(count + " plans indexed");

			return count;

		} catch (IOException e) {
			logger.debug(
					"Error commiting changes after reindex " + e.getMessage(),
					e);
			throw new PlanException("Error commiting changes after reindex "
					+ e.getMessage(), e);
		}

	}

	class SearchResults {
		List<Plan> results = null;
		int totalNumberOfResults = 0;

		public SearchResults(List<Plan> results, int totalNumberOfResults) {
			this.results = results;
			this.totalNumberOfResults = totalNumberOfResults;
		}
	}
}
