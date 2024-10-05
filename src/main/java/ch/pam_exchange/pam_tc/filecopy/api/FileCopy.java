package ch.pam_exchange.pam_tc.filecopy.api;

import com.ca.pam.extensions.core.api.exception.ExtensionException;
import com.ca.pam.extensions.core.model.LoggerWrapper;
import com.ca.pam.extensions.core.util.MessageConstants;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import jcifs.Configuration;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;

public class FileCopy {

	private final static int BUFFER_SIZE= 8;
	
	private final byte[] buffer= new byte[BUFFER_SIZE*1024];
	private String username= "";
	private String password= "";
	private String domain= "";
	private String hostname= "";
	private int port= 22;
	private boolean isWindowsPlatform= true;
	private NtlmPasswordAuthenticator auth= null;

	private static final Logger LOGGER = Logger.getLogger(FileCopy.class.getName());
	   
	/*
	 * Constructors
	 */
	public FileCopy() {
		LOGGER.fine("Construct - local");
	}
	
	public FileCopy(String usr, String pwd, String dom, String host) {
		LOGGER.fine(LoggerWrapper.logMessage("Construct - windows, username="+usr+" password=***** hostname="+host+" domain="+dom));
		this.username= usr;
		this.password= pwd;
		this.domain= dom;
		this.hostname= host;
		this.auth= new NtlmPasswordAuthenticator(this.domain,this.username,this.password);
		this.isWindowsPlatform= true;
	}
	
	public FileCopy(String usr, String pwd, String host, int po) {
		LOGGER.fine(LoggerWrapper.logMessage("Construct - unix, , username="+usr+" password=***** hostname="+host+" port="+po));
		this.username= usr;
		this.password= pwd;
		this.hostname= host;
		this.port= po;
		this.isWindowsPlatform= false;
	}
	
	/*
	 * construct and return a backup filename
	 */
	public String getBackupFilename( String filename ) {
		String dateStr= new SimpleDateFormat("yyyyMMdd-HHmmss.SSS").format(new Date());
		return filename+".backup-"+dateStr;
	}
	
	/*
	 * construct and return a temp filename
	 */
	public String getTempFilename( ) {
		String dateStr= new SimpleDateFormat("yyyyMMdd-HHmmss.SSS").format(new Date());
		return System.getProperty("java.io.tmpdir")+"/temp-"+dateStr;
	}

	/*
	 * is the remote system a Windows platform?
	 */
	public boolean isWindows() {
		return isWindowsPlatform;
	}
	
	/*
	 * set the remote system platform
	 */
	public void isWindows( boolean is ) {
		isWindowsPlatform= is;
	}

	/*
	 * copy from remote server
	 */
	public void copyFromRemote(String src, String dst) throws ExtensionException {
		
		LOGGER.fine(LoggerWrapper.logMessage("Copy file: "+src+" --> "+dst));
		
		if (isWindowsPlatform) {
			copyFromRemoteWindows(src,dst);
		} 
		else {
			copyFromRemoteLinux(src,dst);
		}
	}

	//----
	private void copyFromRemoteWindows(String src, String dst) throws ExtensionException {
		final long methodStartTime = System.currentTimeMillis();
		
		OutputStream out= null;
		SmbFileInputStream inp= null;

		LOGGER.fine(LoggerWrapper.logMessage("Copy file: "+src+" --> "+dst));
		try {
			BaseContext baseCxt = null;
			Properties jcifsProperties  = new Properties();
			jcifsProperties.setProperty("jcifs.smb.client.enableSMB2", "true");
			jcifsProperties.setProperty("jcifs.smb.client.dfs.disabled","true");
			Configuration config = new PropertyConfiguration(jcifsProperties);
			baseCxt = new BaseContext(config);
			baseCxt.withCredentials(auth);
			
			LOGGER.fine(LoggerWrapper.logMessage("smb://"+this.hostname+"/"+src));
			inp= new SmbFileInputStream(new SmbFile("smb://"+this.hostname+"/"+ src.replace("\\", "/"), baseCxt.withCredentials(this.auth)));
	        out= new FileOutputStream(dst);

	        for (int length; (length=inp.read(buffer))!=-1; ) {
	        	LOGGER.fine(LoggerWrapper.logMessage("inp.read length="+length));
	        	out.write(buffer,0,length);
	        }
	        
		} catch (jcifs.smb.SmbAuthException e) {
	        LOGGER.severe(LoggerWrapper.logMessage("Authentication failure"));
	        throw new ExtensionException(FileCopyMessageConstants.ERR_AUTH, false, this.hostname,this.username);
        	
	    } catch (jcifs.smb.SmbException e) {
			if (e.getMessage().equals("The system cannot find the file specified.")) {
		        LOGGER.severe(LoggerWrapper.logMessage("File not found. '"+src+"'"));
		        throw new ExtensionException(FileCopyMessageConstants.ERR_FILENOTFOUND, false, src);
	        } 
			else {
		        LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("General jcifs.smb.SmbException"), e);
		        throw new ExtensionException(MessageConstants.SERVER_ERROR, false);
			}
	        
	    } catch (Exception e) {
			LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("General Exception"), e);
			throw new ExtensionException(MessageConstants.SERVER_ERROR, false);
	    }
		finally {
			LOGGER.fine(LoggerWrapper.logMessage("closing streams"));
	    	try {out.close();} catch(Exception e) {}
	    	try {inp.close();} catch(Exception e) {}
		}
		
        LOGGER.fine(LoggerWrapper.logMessage("copyFromRemoteWindows duration= "+ Long.toString(System.currentTimeMillis() - methodStartTime)+ " ms"));
	}

	//----
	private void copyFromRemoteLinux(String src, String dst) throws ExtensionException {
		final long methodStartTime = System.currentTimeMillis();
		JSch jsch= null;
		ChannelSftp sftp= null;
		Session session= null;
		
		LOGGER.fine(LoggerWrapper.logMessage("Copy file: "+src+" --> "+dst));
		try {
			if (jsch==null) {
				LOGGER.fine(LoggerWrapper.logMessage("hostname="+hostname+" port="+port+" username="+username+" password=*****"));
				jsch= new JSch();
				session= jsch.getSession(username, hostname, port);
			    session.setPassword(password);
			    session.setConfig("StrictHostKeyChecking", "no");
			    session.connect();
			    Channel channel = session.openChannel("sftp");
			    channel.connect();
			    sftp= (ChannelSftp) channel;
			}
		    
			LOGGER.info(LoggerWrapper.logMessage("sftp.get, "+src+" --> "+dst));
			sftp.get(src,dst);
		    
	    } 
		catch (com.jcraft.jsch.JSchException e) {
	        if (e.getCause() instanceof java.net.ConnectException) {
		        LOGGER.severe(LoggerWrapper.logMessage("Cannot connect to host '"+this.hostname+":"+this.port+"'"));
				throw new ExtensionException(FileCopyMessageConstants.ERR_CONNECT, false, this.hostname+":"+this.port);
	        }
	        if (e.getMessage().equals("Auth fail")) {
		        LOGGER.severe(LoggerWrapper.logMessage("Authentication failure"));
				throw new ExtensionException(FileCopyMessageConstants.ERR_AUTH, false, this.hostname+":"+this.port,this.username);
	        }
	        LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("General com.jcraft.jsch.JSchException"), e);
	        throw new ExtensionException(MessageConstants.SERVER_ERROR, false);
	        
	    } 
		catch (Exception e) {
	        if (e.getMessage().equals("No such file")) {
		        LOGGER.severe(LoggerWrapper.logMessage("File not found. '"+src+"'"));
	        	throw new ExtensionException(FileCopyMessageConstants.ERR_FILENOTFOUND, false, src);
	        } 
	        else if (e.getMessage().equals("Permission denied")) {
		        LOGGER.severe(LoggerWrapper.logMessage("Permission denied for read/write. '"+src+"'"));
		        throw new ExtensionException(FileCopyMessageConstants.ERR_PERMISSION, false, src);
	        } 
	        else {
		        LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("General Exception"), e);
		        throw new ExtensionException(MessageConstants.SERVER_ERROR, false);
	        }
	    }
		finally {
		    try {sftp.disconnect();} catch(Exception e) {}
		    try {session.disconnect();} catch(Exception e) {}
		}
		
        LOGGER.fine(LoggerWrapper.logMessage("copyFromRemoteLinux duration= "+ Long.toString(System.currentTimeMillis() - methodStartTime)+ " ms"));
	}

	/*
	 * copy to remote server
	 */
	public void copyToRemote(String src, String dst) throws ExtensionException {
		if (isWindowsPlatform) {
			copyToRemoteWindows(src,dst);
		} 
		else {
			copyToRemoteLinux(src,dst);
		}
	}
	
	//----
	private void copyToRemoteWindows(String src, String dst) throws ExtensionException {
		final long methodStartTime = System.currentTimeMillis();
		SmbFileOutputStream out= null;
		InputStream inp= null;
		
		LOGGER.fine(LoggerWrapper.logMessage("Copy file: "+src+" --> "+dst));
		try {
			BaseContext baseCxt = null;
			Properties jcifsProperties  = new Properties();
			jcifsProperties.setProperty("jcifs.smb.client.enableSMB2", "true");
			jcifsProperties.setProperty("jcifs.smb.client.dfs.disabled","true");
			Configuration config = new PropertyConfiguration(jcifsProperties);
			baseCxt = new BaseContext(config);
			baseCxt.withCredentials(auth);
			
			LOGGER.fine(LoggerWrapper.logMessage("smb://"+this.hostname+"/"+src));
	        inp= new FileInputStream(src);
			out= new SmbFileOutputStream(new SmbFile("smb://"+this.hostname+"/"+ dst.replace("\\", "/"), baseCxt.withCredentials(this.auth)));

	        for (int length; (length = inp.read(buffer)) != -1; ){
	        	LOGGER.fine(LoggerWrapper.logMessage("inp.read length="+length));
	        	out.write(buffer, 0, length);
	        }
	        
		} 
		catch (jcifs.smb.SmbAuthException e) {
	        LOGGER.severe(LoggerWrapper.logMessage("Authentication failure"));
			throw new ExtensionException(FileCopyMessageConstants.ERR_AUTH, false, this.hostname,this.username);
        	
	    } 
		catch (jcifs.smb.SmbException e) {
			if (e.getMessage().equals("The system cannot find the file specified.")) {
		        LOGGER.severe(LoggerWrapper.logMessage("File not found. '"+src+"'"));
		        throw new ExtensionException(FileCopyMessageConstants.ERR_FILENOTFOUND, false, src);
	        }
			LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("General jcifs.smb.SmbException"), e);
			throw new ExtensionException(MessageConstants.SERVER_ERROR, false);
	        
	    } 
		catch (Exception e){
			LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("General Exception"), e);
			throw new ExtensionException(MessageConstants.SERVER_ERROR, false);
	    } 
		finally {
			LOGGER.fine(LoggerWrapper.logMessage("closing streams"));
	    	try {out.close();} catch(Exception e) {}
	    	try {inp.close();} catch(Exception e) {}
	    }
        
		LOGGER.info(LoggerWrapper.logMessage("copyToRemoteWindows duration= "+ Long.toString(System.currentTimeMillis() - methodStartTime)+ " ms"));
	}
	
	//----
	private void copyToRemoteLinux(String src, String dst) throws ExtensionException {
		final long methodStartTime = System.currentTimeMillis();
		JSch jsch= new JSch();
		ChannelSftp sftp= null;
		Session session= null;
		
		LOGGER.fine(LoggerWrapper.logMessage("Copy file: "+src+" --> "+dst));
		try {
			LOGGER.fine(LoggerWrapper.logMessage("hostname="+this.hostname+" port="+this.port+" username="+this.username+" password=*****"));
			session= jsch.getSession(this.username, this.hostname, this.port);
		    session.setPassword(this.password);
		    session.setConfig("StrictHostKeyChecking", "no");
		    session.connect();
		    Channel channel = session.openChannel("sftp");
		    channel.connect();
		    sftp= (ChannelSftp) channel;
		    
			LOGGER.info(LoggerWrapper.logMessage("sftp.put, "+src+" --> "+dst));
		    sftp.put(src,dst);
		    
	    } 
		catch (com.jcraft.jsch.JSchException e) {
	        if (e.getCause() instanceof java.net.ConnectException) {
	        	LOGGER.severe(LoggerWrapper.logMessage("Cannot connect to host '"+hostname+":"+port+"'"));
				throw new ExtensionException(FileCopyMessageConstants.ERR_CONNECT, false, this.hostname+":"+this.port);
	        } 
	        else if (e.getMessage().equals("Auth fail")) {
		        LOGGER.severe(LoggerWrapper.logMessage("Authentication failure"));
		        throw new ExtensionException(FileCopyMessageConstants.ERR_AUTH, false, this.hostname+":"+this.port,this.username);
	        } 
	        else {
		        LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("General com.jcraft.jsch.JSchException"), e);
		        throw new ExtensionException(MessageConstants.SERVER_ERROR, false);
	        }
	        
	    } 
		catch (Exception e) {
	        if (e.getMessage().equals("No such file")) {
		        LOGGER.severe(LoggerWrapper.logMessage("File not found"));
		        throw new ExtensionException(FileCopyMessageConstants.ERR_FILENOTFOUND, false, src);
	        } 
	        else if (e.getMessage().equals("Permission denied")) {
		        LOGGER.severe(LoggerWrapper.logMessage("Permission denied for read/write"));
		        throw new ExtensionException(FileCopyMessageConstants.ERR_PERMISSION, false, src);
	        } 
	        else {
		        LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("General Exception"), e);
		        throw new ExtensionException(MessageConstants.SERVER_ERROR, false);
	        }
	    }
		finally {
		    try {sftp.disconnect();} catch(Exception e) {}
		    try {session.disconnect();} catch(Exception e) {}
		}
		
        LOGGER.fine(LoggerWrapper.logMessage("copyToRemoteLinux duration= "+ Long.toString(System.currentTimeMillis() - methodStartTime)+ " ms"));
	}
	
	/*
	 * copy file local server
	 */
	public void copyLocal(String src, String dst) throws ExtensionException {
		final long methodStartTime = System.currentTimeMillis();

		InputStream inp= null;
		OutputStream out= null;
		
		LOGGER.fine(LoggerWrapper.logMessage("Copy file: "+src+" --> "+dst));
		try {
	        inp= new FileInputStream(src);
			out= new FileOutputStream(dst);

	        for (int length; (length = inp.read(buffer)) != -1; ){
	        	LOGGER.fine(LoggerWrapper.logMessage("inp.read length="+length));
	        	out.write(buffer, 0, length);
	        }
		} 
		catch (java.io.FileNotFoundException e) {
	        LOGGER.severe(LoggerWrapper.logMessage("File not found. '"+src+"'"));
	        throw new ExtensionException(FileCopyMessageConstants.ERR_FILENOTFOUND, false, src);
	        
	    } 
		catch (Exception e) {
			LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("General Exception"), e);
			throw new ExtensionException(MessageConstants.SERVER_ERROR, false);
			
	    } 
		finally {
			LOGGER.fine(LoggerWrapper.logMessage("closing streams"));
	    	try {out.close();} catch(Exception e) {}
	    	try {inp.close();} catch(Exception e) {}
	    }
		
        LOGGER.fine(LoggerWrapper.logMessage("copyLocal duration= "+ Long.toString(System.currentTimeMillis() - methodStartTime)+ " ms"));
	}
}
