package eu.scape_project.roda.core.connector.utils;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import pt.gov.dgarq.roda.core.BrowserHelper;
import pt.gov.dgarq.roda.core.EditorHelper;
import pt.gov.dgarq.roda.core.IngestHelper;
import pt.gov.dgarq.roda.core.RodaWebApplication;
import pt.gov.dgarq.roda.core.common.EditorException;
import pt.gov.dgarq.roda.core.data.User;
import pt.gov.dgarq.roda.core.fedora.FedoraClientException;
import pt.gov.dgarq.roda.core.fedora.FedoraClientUtility;
import pt.gov.dgarq.roda.servlet.ExtendedUserPrincipal;
import pt.gov.dgarq.roda.servlet.LdapAuthenticationFilter;
import pt.gov.dgarq.roda.servlet.RodaServletRequestWrapper;

public class HelperUtils {
	static final private Logger logger = Logger.getLogger(HelperUtils.class);
	private static String error;
	
	
	public static String getError() {
		return error;
	}

	
	protected static User getClientUser(HttpServletRequest request) {
		User user = null;

		if (request instanceof RodaServletRequestWrapper) {
			RodaServletRequestWrapper rodaRequestWrapper = (RodaServletRequestWrapper) request;
			user = rodaRequestWrapper.getLdapUserPrincipal();
		} else {
			// user = null
			user = new User(request.getUserPrincipal().getName());
		}

		return user;
	}
	protected static String getClientUserPassword(HttpServletRequest request) {

		String password = null;

		if (request instanceof RodaServletRequestWrapper) {

			RodaServletRequestWrapper rodaRequestWrapper = (RodaServletRequestWrapper) request;
			ExtendedUserPrincipal userPrincipal = (ExtendedUserPrincipal) rodaRequestWrapper.getLdapUserPrincipal();
			password = userPrincipal.getPassword();
		} else {

			String[] usernamePassword = LdapAuthenticationFilter.parseUsernamePassword(request);
			password = usernamePassword[1];
		}

		return password;
	}
	
	@SuppressWarnings("rawtypes")
	public static BrowserHelper getBrowserHelper(HttpServletRequest req,Class c) {
		
		BrowserHelper helper = null;
		try{
			if(req==null){
				error = "Context null";
			}else{
				User u = getClientUser(req);
				if(u==null){
					error = "User null";
				}else{
					String password = getClientUserPassword(req);
					Configuration configuration = RodaWebApplication.getConfiguration(c, "roda-core.properties");
					if(configuration==null){
						error = "Configuration null";
					}else{
						FedoraClientUtility fcu = new FedoraClientUtility(configuration.getString("fedoraURL"), configuration.getString("fedoraGSearchURL"),  u, password);
						helper = new BrowserHelper(fcu, configuration);
					}
	
				}
			}
		}catch(Exception e){
			logger.error("Error while getting BrowserHelper: "+e.getMessage(),e);
		}
		return helper;
	}

	
	
	@SuppressWarnings("rawtypes")
	public static Uploader getUploader(HttpServletRequest req,Class c){
		User u = getClientUser(req);
		String username = u.getName();
		String password = getClientUserPassword(req);
		Uploader uploader = null;
		try{
			if(req==null){
				error = "Context null";
			}else{
				Configuration configuration = RodaWebApplication.getConfiguration(c, "roda-in.properties");
				uploader = new Uploader(new URL(configuration.getString("roda.services.url")), username, password);
			}
			
		}catch(Exception e){
			logger.error("Error while getting Uploader: "+e.getMessage(),e);
		}
		return uploader;
	}

	@SuppressWarnings("rawtypes")
	public static EditorHelper getEditorHelper(HttpServletRequest req, Class c){
		EditorHelper helper = null;
		try{
			if(req==null){
				error = "Context null";
			}else{
				User u = getClientUser(req);
				if(u==null){
					error = "User null";
				}else{
					String password = getClientUserPassword(req);
					Configuration configuration = RodaWebApplication.getConfiguration(c, "roda-core.properties");
					if(configuration==null){
						error = "Configuration null";
					}else{
						FedoraClientUtility fcu = new FedoraClientUtility(configuration.getString("fedoraURL"), configuration.getString("fedoraGSearchURL"),  u, password);
						helper = new EditorHelper(fcu, configuration);
					}
	
				}
			}
		}catch(Exception e){
			logger.error("Error while getting EditorHelper: "+e.getMessage(),e);
		}
		return helper;
	}
	
	@SuppressWarnings("rawtypes")
	public static IngestHelper getIngestHelper(HttpServletRequest req, Class c) throws ConfigurationException, FedoraClientException, MalformedURLException, EditorException {
		IngestHelper helper = null;
		try{
			if(req==null){
				error = "Context null";
			}else{
				User u = getClientUser(req);
				if(u==null){
					error = "User null";
				}else{
					String password = getClientUserPassword(req);
					Configuration configuration = RodaWebApplication.getConfiguration(c, "roda-core.properties");
					if(configuration==null){
						error = "Configuration null";
					}else{
						FedoraClientUtility fcu = new FedoraClientUtility(configuration.getString("fedoraURL"), configuration.getString("fedoraGSearchURL"),  u, password);
						helper = new IngestHelper(fcu, configuration);
					}
	
				}
			}
		}catch(Exception e){
			logger.error("Error while getting IngestHelper: "+e.getMessage(),e);
		}
		return helper;
	}
}
