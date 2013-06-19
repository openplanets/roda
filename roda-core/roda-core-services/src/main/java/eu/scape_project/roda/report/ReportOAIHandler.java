package eu.scape_project.roda.report;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import pt.gov.dgarq.roda.core.data.User;
import ORG.oclc.oai.server.OAIHandler;

public class ReportOAIHandler extends OAIHandler {
	private static final long serialVersionUID = 3962673274590817242L;

	/**
	 * The logger.
	 */
	private static Logger logger = Logger.getLogger(ReportOAIHandler.class);

	@Override
	public void init() throws ServletException {
		logger.debug("init()");
		super.init();
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		logger.debug(String.format("init(config=%s)", config));
		super.init(config);
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException {
		logger.debug("doGet(...)");

		getServletContext().setAttribute("EventCatalog.user",
				getClientUser(request));
		getServletContext().setAttribute("EventCatalog.password",
				getClientUserPassword(request));

		super.doGet(request, response);
	}

	/**
	 * Gets the {@link User} that requested this service.
	 * 
	 * @return the {@link User} that requested this service or <code>null</code>
	 *         if it doesn't exist.
	 */
	protected User getClientUser(HttpServletRequest request) {

		User user = null;

		// if (request instanceof RodaServletRequestWrapper) {
		// RodaServletRequestWrapper rodaRequestWrapper =
		// (RodaServletRequestWrapper) request;
		// user = rodaRequestWrapper.getLdapUserPrincipal();
		// } else {
		// // user = null
		// user = new User(request.getUserPrincipal().getName());
		// }

		return user;
	}

	/**
	 * Gets the IP address of the client that requested this service.
	 * 
	 * @return the the IP address of the client that requested this service or
	 *         <code>null</code> if it doesn't exist.
	 */
	protected String getClientAddress(HttpServletRequest request) {
		return request.getRemoteAddr();
	}

	/**
	 * Gets the password used to authenticate the current request.
	 * 
	 * This method
	 * 
	 * @return the password use to authenticate this current request or
	 *         <code>null</code> if it doesn't exist.
	 */
	protected String getClientUserPassword(HttpServletRequest request) {

		String password = null;

		// if (request instanceof RodaServletRequestWrapper) {
		//
		// RodaServletRequestWrapper rodaRequestWrapper =
		// (RodaServletRequestWrapper) request;
		// ExtendedUserPrincipal userPrincipal = (ExtendedUserPrincipal)
		// rodaRequestWrapper
		// .getLdapUserPrincipal();
		// password = userPrincipal.getPassword();
		//
		// } else {
		//
		// String[] usernamePassword = LdapAuthenticationFilter
		// .parseUsernamePassword(request);
		// password = usernamePassword[1];
		//
		// }

		return password;
	}

}
