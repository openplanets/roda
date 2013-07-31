package eu.scape_project.roda.core.plan;

import java.io.File;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

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
	 * Creates a new {@link PlanManager}.
	 * 
	 * @throws PlanException
	 */
	private PlanManager() {
	}

	/**
	 * @return the plansDirectory
	 * @throws PlanException
	 */
	private synchronized File getPlansDirectory() throws PlanException {
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
		Plan plan = new Plan();
		File planMetadata = plan.store(getPlansDirectory());

		logger.info("Plan metadata successfully stored to " + planMetadata);

		return plan;
	}

	public Plan getPlan(String id) throws NoSuchPlanException, PlanException {
		File planMetadataFile = getPlanMetadataFile(id);
		if (planMetadataFile.exists()) {
			return Plan.loadPlan(planMetadataFile);
		} else {
			throw new NoSuchPlanException("Plan " + id + " could not be found");
		}
	}

	private File getPlanMetadataFile(String id) throws PlanException {
		return new File(getPlansDirectory(), id + ".metadata");
	}
}
