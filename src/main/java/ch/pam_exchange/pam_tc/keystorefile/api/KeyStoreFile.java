package ch.pam_exchange.pam_tc.keystorefile.api;

import com.ca.pam.extensions.core.api.exception.ExtensionException;
import com.ca.pam.extensions.core.model.LoggerWrapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.security.cert.Certificate;

import com.ca.pam.extensions.core.TargetAccount;

import ch.pam_exchange.pam_tc.filecopy.api.FileCopy;
import ch.pam_exchange.pam_tc.filecopy.api.FileCopyMessageConstants;

public class KeyStoreFile {

	private static final Logger LOGGER = Logger.getLogger(KeyStoreFile.class.getName());
	private static final boolean EXTENDED_DEBUG = true;

	/*
	 * Constants
	 */
	private static final int DEFAULT_PORT = 22;
	private static final String LOCATION_LOCAL = "local";
	private static final String LOCATION_REMOTEUNIX = "remoteUNIX";
	private static final String LOCATION_REMOTEWINDOWS = "remoteWindows";

	private static final String KEYSTOREFORMAT_JKS= "jks";
	private static final String KEYSTOREFORMAT_PKCS12= "pkcs12";

	private static final String FIELD_LOCATION = "location";
	private static final String FIELD_PORT = "port";
	private static final String FIELD_LOGINACCOUNT = "loginAccount";
	private static final String FIELD_FILENAME = "filename";
	private static final String FIELD_CREATEBACKUP = "createBackup";
	private static final String FIELD_ALIAS = "alias";
	private static final String FIELD_SAMEKEYPASSWD= "sameKeyPasswd";
	private static final String FIELD_KEYSTOREFORMAT= "keystoreFormat";

	private static final String EXTENSIONTYPE_ACTIVEDIRECTORY= "windowsDomainService";
	private static final String ATTR_APPL_DOMAIN= "domainName";

	/*
	 * Instance variables used in the processCredentialsVerify and
	 * processCredentialsUpdate
	 */
	private String location = LOCATION_LOCAL;
	private String username = "";
	private String oldPassword = "";
	private String newPassword = "";
	private String hostname = "";
	private int port = DEFAULT_PORT;
	private String loginUsername = "";
	private String loginPassword = "";
	private String domain = "";
	private String filename = "";
	private boolean createBackup = false;
	private boolean sameKeyPasswd= false;
	private String alias= "";
	private String keyStoreFormat= KEYSTOREFORMAT_PKCS12;

	/*
	 * Constructor
	 */
	public KeyStoreFile(TargetAccount targetAccount) {

		this.username = targetAccount.getUserName();
		LOGGER.fine(LoggerWrapper.logMessage("username= " + this.username));

		this.newPassword = targetAccount.getPassword();
		if (EXTENDED_DEBUG)
			LOGGER.fine(LoggerWrapper.logMessage("newPassword= " + this.newPassword));

		this.oldPassword = targetAccount.getOldPassword();
		if (this.oldPassword==null || this.oldPassword.isEmpty()) {
			LOGGER.fine(LoggerWrapper.logMessage("oldPassword is empty use newPassword"));
			this.oldPassword= this.newPassword;
		}
		if (EXTENDED_DEBUG)
			LOGGER.fine(LoggerWrapper.logMessage("oldPassword= " + this.oldPassword));

		this.location = targetAccount.getTargetApplication().getExtendedAttribute(FIELD_LOCATION);
		LOGGER.fine(LoggerWrapper.logMessage(FIELD_LOCATION + "= " + this.location));

		if (LOCATION_REMOTEWINDOWS.equals(this.location) || LOCATION_REMOTEUNIX.equals(this.location)) {
			/*
			 * Hostname
			 */
			this.hostname = targetAccount.getTargetApplication().getTargetServer().getHostName();
			LOGGER.fine(LoggerWrapper.logMessage("hostname= " + this.hostname));

			/*
			 * Login account to remote
			 */
			TargetAccount loginAccount = targetAccount.getMasterAccount(FIELD_LOGINACCOUNT).getAsTargetAccount();

			/*
			 * Find domain through Application for loginAccount
			 */
			String loginAccountType= loginAccount.getTargetApplication().getType();
			LOGGER.fine(LoggerWrapper.logMessage("extensionType= " + loginAccountType));
			
			if (EXTENSIONTYPE_ACTIVEDIRECTORY.equals(loginAccountType)) {
				this.domain= loginAccount.getTargetApplication().getExtendedAttribute(ATTR_APPL_DOMAIN);
			} else {
				this.domain= this.hostname;
			}
			LOGGER.fine(LoggerWrapper.logMessage("Domain= '" + this.domain+"'"));
			
			/*
			 * login username from loginAccount
			 */
			this.loginUsername = loginAccount.getUserName();
			if (this.loginUsername == null || this.loginUsername.isEmpty()) {
				LOGGER.severe(LoggerWrapper.logMessage("loginUsername is empty"));
			} else {
				LOGGER.fine(LoggerWrapper.logMessage("loginUsername= " + this.loginUsername));
			}

			/*
			 * login password from loginAccount
			 */
			this.loginPassword = loginAccount.getPassword();
			if (this.loginPassword == null || this.loginPassword.isEmpty()) {
				LOGGER.severe(LoggerWrapper.logMessage("loginPassword is empty"));
			} else {
				if (EXTENDED_DEBUG)
					LOGGER.fine(LoggerWrapper.logMessage("loginPassword= " + this.loginPassword));
			}

			/*
			 * Additional settings for RemoteUNIX
			 */
			if (LOCATION_REMOTEUNIX.equals(this.location)) {
				try {
					this.port = Integer
							.parseUnsignedInt(targetAccount.getTargetApplication().getExtendedAttribute(FIELD_PORT));
				} catch (Exception e) {
					LOGGER.warning("Using default port");
					this.port = DEFAULT_PORT;
				}
				LOGGER.fine(LoggerWrapper.logMessage(FIELD_PORT + "= " + this.port));

				if (!(this.domain == null || this.domain.isEmpty())) {
					/*
					 * Domain is available. Check if it is short/long format
					 * and update the loginUsername accordingly.
					 */
					if (this.domain.contains(".")) {
						this.loginUsername = this.loginUsername + "@" + this.domain;
					} else {
						this.loginUsername = this.domain + "\\" + this.loginUsername;
					}
					LOGGER.fine("Updated loginUsername= " + this.loginUsername);
				}
			}
		}

		/*
		 * KeyStoreFile path+filename
		 */
		this.filename = targetAccount.getExtendedAttribute(FIELD_FILENAME).replace("\\", "/");
		LOGGER.fine(LoggerWrapper.logMessage(FIELD_FILENAME + "= " + this.filename));

		/*
		 * Is a backup file required
		 */
		this.createBackup = "true".equals(targetAccount.getExtendedAttribute(FIELD_CREATEBACKUP));
		LOGGER.fine(LoggerWrapper.logMessage(FIELD_CREATEBACKUP + "= " + this.createBackup));

		/*
		 * Alias
		 */
		this.alias = targetAccount.getExtendedAttribute(FIELD_ALIAS);
		if (this.alias.isEmpty())
			this.alias= this.username;
		LOGGER.fine(LoggerWrapper.logMessage(FIELD_ALIAS + "= " + this.alias));

		/*
		 * use same password for key
		 */
		this.sameKeyPasswd= "true".equals(targetAccount.getExtendedAttribute(FIELD_SAMEKEYPASSWD));
		LOGGER.fine(LoggerWrapper.logMessage(FIELD_SAMEKEYPASSWD + "= " + this.sameKeyPasswd));

		/*
	     * KeyStore format
	     */
		this.keyStoreFormat= targetAccount.getExtendedAttribute(FIELD_KEYSTOREFORMAT);
		LOGGER.fine(LoggerWrapper.logMessage(FIELD_KEYSTOREFORMAT+"= "+this.keyStoreFormat));
	}

	/**
	 * Verifies credentials against target device. Stub method should be implemented
	 * by Target Connector Developer.
	 *
	 * @param targetAccount object that contains details for the account for
	 *                      verification Refer to TargetAccount java docs for more
	 *                      details.
	 * @throws ExtensionException if there is any problem while verifying the
	 *                            credential
	 *
	 */
	public void keyStoreFileCredentialVerify() throws ExtensionException {

		String tmpFilename= copyFileWithBackup(false);

		/* 
		 * Load keystore and verify key entry
		 */
		try {
			KeyStore ks= loadKeystore(tmpFilename,this.newPassword,this.keyStoreFormat);
			LOGGER.fine(LoggerWrapper.logMessage("Keystore loaded"));

			if (this.sameKeyPasswd) {
				@SuppressWarnings("unused")
				KeyEntry ke= getKey(ks,this.alias,this.oldPassword);
				LOGGER.fine(LoggerWrapper.logMessage("Key fetched in keystore"));
			}				
		}
		catch (ExtensionException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("General Exception"), e);
			throw e;
		} 
		finally {
			/*
			 * remove tmp file received from remote
			 */
			if (LOCATION_REMOTEWINDOWS.equals(this.location) || LOCATION_REMOTEUNIX.equals(this.location)) {
				try {
					LOGGER.fine(LoggerWrapper.logMessage("Remove file, filename= " + tmpFilename));
					File f = new File(tmpFilename);
					f.delete();
				} 
				catch (Exception e) {
					LOGGER.warning(LoggerWrapper.logMessage("File delete - " + e.getMessage()));
				}
			}
		}
	}

	/**
	 * Updates credentials against target device. Stub method should be implemented
	 * by Target Connector Developer.
	 *
	 * @param targetAccount object that contains details for the account for
	 *                      verification Refer to TargetAccount java docs for more
	 *                      details.
	 * @throws ExtensionException if there is any problem while update the
	 *                            credential
	 */
	public void keyStoreFileCredentialUpdate() throws ExtensionException {

		String tmpFilename= copyFileWithBackup(this.createBackup);

		/*
		 * The file is available in tmpFilename (copied from remote or just the original
		 * filename). Verify that password (or placeholder) is found Update with content
		 * with new password
		 */
		FileOutputStream fos= null;

		try {
			if (EXTENDED_DEBUG)
				LOGGER.fine(LoggerWrapper.logMessage("Load keystore '"+tmpFilename+"' using oldPassword= "+this.oldPassword));
			else
				LOGGER.fine(LoggerWrapper.logMessage("Load keystore '"+tmpFilename+"' using oldPassword"));
		
			KeyStore ks= loadKeystore(tmpFilename,this.oldPassword,this.keyStoreFormat);
			LOGGER.fine(LoggerWrapper.logMessage("Keystore is loaded"));

			if (this.sameKeyPasswd) {
				KeyEntry ke= getKey(ks,this.alias,this.oldPassword);
				LOGGER.fine(LoggerWrapper.logMessage("Key is found in keystore"));

				/*
				 * Update password for key with cert. chain (or null)
				 */
				if (EXTENDED_DEBUG)
					LOGGER.fine(LoggerWrapper.logMessage("Update key password to newPassword= "+this.newPassword));
				else 
					LOGGER.fine(LoggerWrapper.logMessage("Update key password to newPassword"));
				ks.setKeyEntry(this.alias,ke.key,this.newPassword.toCharArray(), ke.certs);
			}

			/*
			 * save keystore file with new password
			 */
			fos= new FileOutputStream(tmpFilename);
			ks.store(fos, this.newPassword.toCharArray());
			fos.close();;

			if (EXTENDED_DEBUG)
				LOGGER.fine(LoggerWrapper.logMessage("Saved keystore using newPassword= "+this.newPassword));
			else 
				LOGGER.fine(LoggerWrapper.logMessage("Saved keystore using newPassword"));

			/*
			 * copy updated keystore to remote
			 */
			if (LOCATION_REMOTEWINDOWS.equals(this.location) || LOCATION_REMOTEUNIX.equals(this.location)) {
				FileCopy kfc;
				if (LOCATION_REMOTEWINDOWS.equals(this.location)) {
					kfc = new FileCopy(this.loginUsername, this.loginPassword, this.domain, this.hostname);
				} else {
					kfc = new FileCopy(this.loginUsername, this.loginPassword, this.hostname, this.port);
				}
				kfc.copyToRemote(tmpFilename, this.filename);
				LOGGER.info(LoggerWrapper.logMessage("Copy file to remote complete"));
			}
			else {
				LOGGER.info(LoggerWrapper.logMessage("Local file updated"));
				// local file on TCF
				// tmpFilename is the same as filename -- no need to copy
			}
		}
		catch (ExtensionException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Exception"), e);
			throw new ExtensionException(KeyStoreFileMessageConstants.ERR_EXCEPTION, false);
		}
		finally {
			/*
			 * Cleanup
			 */
			try {fos.close();} catch (Exception e) {}

			/*
			 * remove tmp file from remote
			 */
			if (LOCATION_REMOTEWINDOWS.equals(this.location) || LOCATION_REMOTEUNIX.equals(this.location)) {
				try {
					LOGGER.fine(LoggerWrapper.logMessage("Remove file, filename= " + tmpFilename));
					File f = new File(tmpFilename);
					f.delete();
				} catch (Exception e) {
					LOGGER.warning(LoggerWrapper.logMessage("File delete - " + e.getMessage()));
				}
			}
		}
	}

	/*
	 * Copy file from remote to Tomcat temp
	 * For local TCF, just return the filename
	 */
	private String copyFileWithBackup(boolean backup) throws ExtensionException {
		FileCopy kfc = null;

		if (LOCATION_LOCAL.equals(this.location)) {
			File f = new File(this.filename);
			if (!f.exists()) {
				LOGGER.severe(LoggerWrapper.logMessage("File '" + this.filename + "' not found"));
				throw new ExtensionException(FileCopyMessageConstants.ERR_FILENOTFOUND, false, this.filename);
			} 
			else {
				/*
				 * For local, just use the filename
				 */
				String tmpFilename= this.filename;
				
				if (backup) {
					kfc= new FileCopy();
					String backupFilename= kfc.getBackupFilename(filename);
					LOGGER.finer(LoggerWrapper.logMessage("backupFilename= "+backupFilename));
					kfc.copyLocal(tmpFilename, backupFilename);
					LOGGER.info(LoggerWrapper.logMessage("Backup complete (local): "+tmpFilename+" --> "+backupFilename));
				}

				return tmpFilename;
			}
		} else {

			if (LOCATION_REMOTEWINDOWS.equals(this.location)) {
				kfc = new FileCopy(this.loginUsername, this.loginPassword, this.domain, this.hostname);
			} else {
				kfc = new FileCopy(this.loginUsername, this.loginPassword, this.hostname, this.port);
			}

			String tmpFilename = kfc.getTempFilename().replace("\\", "/");
			LOGGER.finer(LoggerWrapper.logMessage("tmpFilename= " + tmpFilename));

			kfc.copyFromRemote(filename, tmpFilename);
			LOGGER.finer(LoggerWrapper.logMessage("File copy complete"));

			String backupFilename= kfc.getBackupFilename(filename);
			LOGGER.finer(LoggerWrapper.logMessage("backupFilename= "+backupFilename));

			kfc.copyToRemote(tmpFilename, backupFilename);
			LOGGER.info(LoggerWrapper.logMessage("Backup complete (remote): "+tmpFilename+" --> "+backupFilename));
			
			return tmpFilename;
		}
	}

	/*
	 * Load and return keystore from file with given password
	 * 
	 */
	private KeyStore loadKeystore(String filename, String password, String keyStoreFormat) throws ExtensionException {
		FileInputStream fis= null;

		if (EXTENDED_DEBUG) 
			LOGGER.fine(LoggerWrapper.logMessage("Load keystore '"+filename+"' using password '"+password+"'"));
		else
			LOGGER.fine(LoggerWrapper.logMessage("Load keystore '"+filename+"'"));
		
		try {
			KeyStore ks= KeyStore.getInstance(keyStoreFormat);
			fis= new FileInputStream(filename);
			ks.load(fis,password.toCharArray());
			fis.close();
			LOGGER.fine(LoggerWrapper.logMessage("Keystore loaded --> password OK"));

			return ks;
		}
		catch (java.io.FileNotFoundException e)
		{
			LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Exception - File not found "));
			throw new ExtensionException(FileCopyMessageConstants.ERR_FILENOTFOUND, false, filename);
		}
		catch (java.security.KeyStoreException | java.io.IOException e)
		{
			LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Exception - Password"));
			throw new ExtensionException(KeyStoreFileMessageConstants.ERR_PASSWORD, false, filename);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("--> Exception in loadKeystore"), e);
			throw new ExtensionException(KeyStoreFileMessageConstants.ERR_EXCEPTION, false, filename);
		}
		finally {
			try { fis.close(); } catch (Exception e) {}
		}
	}

	/*
	 * Fetch a key entry from keystore
	 * Uses an alias and a password
	 */
	private KeyEntry getKey(KeyStore ks, String alias, String password) throws ExtensionException {

		if (EXTENDED_DEBUG) 
			LOGGER.fine(LoggerWrapper.logMessage("Fetch key from keystore, alias= "+alias+"' using password '"+password+"'"));
		else
			LOGGER.fine(LoggerWrapper.logMessage("Fetch key from keystore, alias= "+alias));
		
		try {
			Key key= ks.getKey(alias, password.toCharArray());
			Certificate[] certs= ks.getCertificateChain(alias);
			
			return new KeyEntry(key,certs);
		}
		catch (java.security.KeyStoreException e) {
			/*
			 *  keystore not initialized (opened)
			 */
			LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Exception - not initialized"));
			throw new ExtensionException(KeyStoreFileMessageConstants.ERR_EXCEPTION, false);
		}
		catch (java.security.UnrecoverableKeyException e)
		{
			/*
			 *  Incorrect password
			 */
			LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Exception - password"));
			throw new ExtensionException(KeyStoreFileMessageConstants.ERR_PASSWORD, false);
		}
		catch (Exception e) {
			LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("--> Exception in getKey"), e);
			throw new ExtensionException(KeyStoreFileMessageConstants.ERR_EXCEPTION, false);
		}
	}
	
	/*
	 * Local class pairing a key and certificate chain together
	 */
	class KeyEntry {
		Key key= null;
		Certificate[] certs= null;
	 
	    // Constructor
	    KeyEntry(Key k, Certificate[] c)
	    {
	        this.key= k;
	        this.certs= c;
	    }
	}		
	
}
