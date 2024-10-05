/*******************************************************************************************************
 ****  Copyright (c) 2018 CA.  All rights reserved.  
 ****  This software and all information contained therein is confidential and proprietary and shall 
 ****  not be duplicated, used, disclosed or disseminated in any way except as authorized by the 
 ****  applicable license agreement, without the express written permission of CA. All authorized 
 ****  reproductions must be marked with this language.  
 ****  
 ****  EXCEPT AS SET FORTH IN THE APPLICABLE LICENSE AGREEMENT, TO THE EXTENT PERMITTED BY APPLICABLE 
 ****  LAW, CA PROVIDES THIS SOFTWARE WITHOUT WARRANTY OF ANY KIND, INCLUDING WITHOUT LIMITATION, ANY 
 ****  IMPLIED WARRANTIES OF MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  IN NO EVENT WILL CA 
 ****  BE LIABLE TO THE END USER OR ANY THIRD PARTY FOR ANY LOSS OR DAMAGE, DIRECT OR INDIRECT, FROM 
 ****  THE USE OF THIS SOFTWARE, INCLUDING WITHOUT LIMITATION, LOST PROFITS, BUSINESS INTERRUPTION, 
 ****  GOODWILL, OR LOST DATA, EVEN IF CA IS EXPRESSLY ADVISED OF SUCH LOSS OR DAMAGE.
 ********************************************************************************************************/

package ch.pam_exchange.pam_tc.keystorefile.api;

import com.ca.pam.extensions.core.model.ExtensionResponse;
import com.ca.pam.extensions.core.util.MessageConstants;
import com.ca.pam.extensions.core.api.exception.ExtensionException;

//import java.io.IOException;
//import java.util.Iterator;
//import java.util.List;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Map;
//import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

//import org.codehaus.jettison.json.JSONObject;

//import com.fasterxml.jackson.core.JsonParseException;
//import com.fasterxml.jackson.core.type.TypeReference;
//import com.fasterxml.jackson.databind.JsonMappingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;

//import com.ca.pam.extensions.core.Account;
//import com.ca.pam.extensions.core.Application;
import com.ca.pam.extensions.core.ConnectorJSONUtil;
//import com.ca.pam.extensions.core.Tab;
//import com.ca.pam.extensions.core.FieldObject;
//import com.ca.pam.extensions.core.util.UIDefinitionManager;
//import com.ca.pam.extensions.core.TargetServer;
//import com.ca.pam.extensions.core.TargetApplication;
import com.ca.pam.extensions.core.TargetAccount;
import com.ca.pam.extensions.core.model.LoggerWrapper;

//import ch.pam_exchange.pam_tc.configfile.api.ConfigFile;

/**
 * Service class that handles request for verifying and updating credentials
 * on the target device.
 *
 * URL Pattern for this service is 
 * https://<TC_HOST>:<TC_PORT>/<TC_NAME>/credentials/{validate|update}
 * TC_HOST is the host name of the device where target connector is deployed.
 * TC_PORT is the port of the tomcat connector where target connector is 
 * deployed.
 * TC_NAME is the name of target connector.
 * 
 * Two service methods are defined in this service.
 * 1. credentialsValidate -- This supports POST method. Validates credentials 
 *                           with target device.
 * 2. credentialsUpdate -- This supports POST method. Updates credentials on 
 *                         target device.
 * Arguments for these methods is a json string which is sent by CA PAM.
 * These methods process json received and validate them. The processed
 * request is set in instance level variables.
 *
 * credentialsValidate method call processCredentialVerify private method to
 * verify credentials.
 * This processCredentialVerify method is private method and it is stub. This
 * is where Target Connector developer should add logic to communicate with 
 * target device and verify credentials. In case of failures this method should
 * throw ExtensionException with proper error codes.
 * 
 * credentialsUpdate method call processCredentialUpdate private method to 
 * update credentials.
 * This processCredentialUpdate method is a private method and it is a stub. 
 * This is where Target Connector developer should add logic to communicate 
 * with target device and update credentials. In case of any failures during
 * this operation this method should throw ExtensionException with proper 
 * error codes. 
 */
@Path("credentials")
public class Credentials {

   private static final Logger LOGGER = Logger.getLogger(Credentials.class.getName());
//   private ExtensionException efe = null;
//   private String durationLogString = "Duration of {0}call to {1} method";

   /**
   * Service method that serves credential validation request. 
   * URL mapping for this is /credentials/validate.
   * @param json -- JSON Request from PAM as a string.
   * This method parses and validates the json request and stores the data in
   * instance variables.
   * It calls processCredentialVerify private method that verifies credential 
   * with target device.
   */
   @POST
   @Path("/validate")
   @Consumes(MediaType.TEXT_PLAIN)
   @Produces(MediaType.APPLICATION_JSON)
   public Response credentialsValidate(String json) {

	  LOGGER.fine(LoggerWrapper.logMessage("JSON= "+json));
	   
      String duration;
      final long methodStartTime = System.currentTimeMillis();
      long methodEndTime;
      TargetAccount targetAccount = new TargetAccount();

      try {
          targetAccount = ConnectorJSONUtil.getTargetAccountFromJSON(json);
          validateData(targetAccount, false);
      } catch (ExtensionException e) {
          LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Failed to parse credential verification request."));
          methodEndTime = System.currentTimeMillis();
          duration = Long.toString(methodEndTime - methodStartTime);
          LOGGER.log(Level.INFO, LoggerWrapper.logMessage(getDurationLogMessage("credentialsValidate", false, duration, targetAccount)));
          throw e;
      } catch (Exception e) {
          LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Failed to parse credential verification request."), e);
          methodEndTime = System.currentTimeMillis();
          duration = Long.toString(methodEndTime - methodStartTime);
          LOGGER.log(Level.INFO, LoggerWrapper.logMessage(getDurationLogMessage("credentialsValidate", false, duration, targetAccount)));
          throw new ExtensionException(MessageConstants.SERVER_ERROR, false, new String[0]);
      }
      
      ExtensionResponse response = null;

      try {
          /**
           * Add the logic that validates the credentials against target device in the private method processCredentialVerify.
           * This method does not return anything if successful. If there is a failure it should throw ExtensionException.
           */
          processCredentialVerify(targetAccount) ;
      } catch (ExtensionException e) {
          LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Failed to verify credentials for user: '" + targetAccount.getUserName() + "' on device: '" + targetAccount.getTargetApplication().getTargetServer().getDeviceName()
                    + "' with targetApplicationName: '" + targetAccount.getTargetApplication().getName() + "'."));
          methodEndTime = System.currentTimeMillis();
          duration = Long.toString(methodEndTime - methodStartTime);
          LOGGER.log(Level.INFO, LoggerWrapper.logMessage(getDurationLogMessage("credentialsValidate", false, duration, targetAccount)));
          throw e;
      } catch (Exception e) {
          LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Failed to verify credentials for user: '" + targetAccount.getUserName() + "' on device: '" + targetAccount.getTargetApplication().getTargetServer().getDeviceName() 
                    + "' with targetApplicationName: '" + targetAccount.getTargetApplication().getName() + "'. Reason: "), e);
          methodEndTime = System.currentTimeMillis();
          duration = Long.toString(methodEndTime - methodStartTime);
          LOGGER.log(Level.INFO, LoggerWrapper.logMessage(getDurationLogMessage("credentialsValidate", false, duration, targetAccount)));
          throw new ExtensionException(MessageConstants.SERVER_ERROR, false, new String[0]);
      }
    
      response =  buildSuccessResponse(true);
      methodEndTime = System.currentTimeMillis();
      duration = Long.toString(methodEndTime - methodStartTime);
      LOGGER.log(Level.INFO, LoggerWrapper.logMessage(getDurationLogMessage("credentialsValidate", true, duration, targetAccount)));
      return Response.status(Response.Status.OK).entity(response).build();
   }

   /**
   * Service method that serves credential update request. 
   * URL mapping for this is /credentials/update.
   * @param json -- JSON Request from PAM as a string.
   * This method parses and validates the json request and stores the data in
   * instance variables.
   * It calls processCredentialUpdate private method that updates credential 
   * on target device.
   */
   @POST
   @Path("/update")
   @Consumes(MediaType.TEXT_PLAIN)
   @Produces(MediaType.APPLICATION_JSON)
   public Response credentialsUpdate(String json) {
	  LOGGER.fine(LoggerWrapper.logMessage("JSON= "+json));

	  ExtensionResponse response = null;
      String duration;
      final long methodStartTime = System.currentTimeMillis();
      long methodEndTime;
      
      TargetAccount targetAccount = new TargetAccount();

      try {
          targetAccount = ConnectorJSONUtil.getTargetAccountFromJSON(json);
          validateData(targetAccount, false);
      } catch (ExtensionException e) {
          LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Failed to parse credential update request."));
          methodEndTime = System.currentTimeMillis();
          duration = Long.toString(methodEndTime - methodStartTime);
          LOGGER.log(Level.INFO, LoggerWrapper.logMessage(getDurationLogMessage("credentialsUpdate", false, duration, targetAccount)));
          throw e;
      } catch (Exception e) {
          LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Failed to parse credential update request."), e);
          methodEndTime = System.currentTimeMillis();
          duration = Long.toString(methodEndTime - methodStartTime);
          LOGGER.log(Level.INFO, LoggerWrapper.logMessage(getDurationLogMessage("credentialsUpdate", false, duration, targetAccount)));
          throw new ExtensionException(MessageConstants.SERVER_ERROR, false, new String[0]);
      }

      try {
          processCredentialUpdate (targetAccount);
      } catch (ExtensionException e) {
          LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Failed to update credentials for user: '" + targetAccount.getUserName() + "' on device: '" + targetAccount.getTargetApplication().getTargetServer().getDeviceName()
                  + "' with targetApplicationName: '" + targetAccount.getTargetApplication().getName() + "'."));
          methodEndTime = System.currentTimeMillis();
          duration = Long.toString(methodEndTime - methodStartTime);
          LOGGER.log(Level.INFO, LoggerWrapper.logMessage(getDurationLogMessage("credentialsUpdate", false, duration, targetAccount)));
          throw e;  
      } catch (Exception e) {
          LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Failed to update credentials for user: '" + targetAccount.getUserName() + "' on device: '" + targetAccount.getTargetApplication().getTargetServer().getDeviceName()
                  + "' with targetApplicationName: '" + targetAccount.getTargetApplication().getName() + "'. Reason: "), e);
          methodEndTime = System.currentTimeMillis();
          duration = Long.toString(methodEndTime - methodStartTime);
          LOGGER.log(Level.INFO, LoggerWrapper.logMessage(getDurationLogMessage("credentialsUpdate", false, duration,targetAccount)));
          throw new ExtensionException(MessageConstants.SERVER_ERROR, false, new String[0]);
      }

      response =  buildSuccessResponse(true);
      methodEndTime = System.currentTimeMillis();
      duration = Long.toString(methodEndTime - methodStartTime);
      LOGGER.log(Level.INFO, LoggerWrapper.logMessage(getDurationLogMessage("credentialsUpdate", true, duration,targetAccount)));
      return Response.status(Response.Status.OK).entity(response).build();     
      
   }

   /**
   * Build the success response
   * @param successValue
   * @return
   */
   private ExtensionResponse buildSuccessResponse(boolean successValue) {
     ExtensionResponse extensionResponse = new ExtensionResponse();
     extensionResponse.setMeta(extensionResponse.new MetaData(successValue));    
     return extensionResponse;
   }

   /**
   * Parses and Validates Request JSON String.
   * Set these variables in instance variables.
   * @param json -- JSON Request string.
   * @throws ExtensionException if there is any error parsing the request or
   * if the data is invalid.
   */
   private boolean validateData (TargetAccount targetAccount, boolean isUpdate) throws ExtensionException {
      StringBuffer missingArgs = new StringBuffer();
      String delimiter = "";
      boolean isDataValid = true;
      if(targetAccount.getTargetApplication().getTargetServer().getHostName().isEmpty()) {
          isDataValid = false;
          missingArgs.append("hostname");
          delimiter = ", ";
      }
      if(targetAccount.getUserName().isEmpty()) {
          isDataValid = false;
          missingArgs.append(delimiter);
          missingArgs.append("userName");
          delimiter = ", ";
      }

      if(targetAccount.getPassword().isEmpty()) {
          isDataValid = false;
          missingArgs.append(delimiter);
          missingArgs.append(" password");
          delimiter = ", ";
      }
      
      if (isUpdate) {
          if(targetAccount.getOldPassword().isEmpty()) {
              isDataValid = false;
              missingArgs.append(delimiter);
              missingArgs.append(" oldPassword");
              delimiter = ", ";
          }
          if(targetAccount.getOldUserName().isEmpty()) {
              isDataValid = false;
              missingArgs.append(delimiter);
              missingArgs.append(" oldUserName");
              delimiter = ", ";
          }
      }
      if (!isDataValid) {
        LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Missing mandatory arguments in the request: " + missingArgs.toString()));
        throw new ExtensionException(MessageConstants.MISSING_ARGUMENTS, false, missingArgs.toString());
      }
      return true;
   }

   private String getDurationLogMessage(String methodName, boolean success, String duration, TargetAccount targetAccount) {
      StringBuffer durationLog = new StringBuffer("Duration of ");
      if (!success) {
          durationLog.append("failed ");
      }
      durationLog.append("call to method ");
      durationLog.append(methodName);

      StringBuffer inputInfo = new StringBuffer();
      if (targetAccount != null && targetAccount.getUserName() != null && !targetAccount.getUserName().isEmpty() 
          && targetAccount.getTargetApplication().getTargetServer().getDeviceName() != null && !targetAccount.getTargetApplication().getTargetServer().getDeviceName().isEmpty()
          && targetAccount.getTargetApplication().getName() != null && !targetAccount.getTargetApplication().getName().isEmpty()) {
          inputInfo.append (" for username: '").append(targetAccount.getUserName()).append("' on the device: '").append(targetAccount.getTargetApplication().getTargetServer().getDeviceName())
                   .append("' with applicationName: '").append(targetAccount.getTargetApplication().getName()).append("' ");
      }

      return durationLog.append(inputInfo).append("= ").append(duration).append(" ms.").toString();
   }



   /**
    * Verifies credentials against target device. Stub method should be
    * implemented by Target Connector Developer.
    * @param targetAccount object that contains details for the account for verification
    *        Refer to TargetAccount java docs for more details.
    * @throws ExtensionException if there is any problem while verifying the
    * credential
    *
    */
   private void processCredentialVerify (TargetAccount targetAccount) throws ExtensionException {
		KeyStoreFile keyStoreFile= new KeyStoreFile(targetAccount);
		keyStoreFile.keyStoreFileCredentialVerify();    
   }

    /**
    * Updates credentials against target device. Stub method should be 
    * implemented by Target Connector Developer.
    * @param targetAccount object that contains details for the account for verification
    *        Refer to TargetAccount java docs for more details.
    * @throws ExtensionException if there is any problem while update the 
    * credential
    */
   private void processCredentialUpdate (TargetAccount targetAccount) throws ExtensionException {
		KeyStoreFile keyStoreFile= new KeyStoreFile(targetAccount);
		keyStoreFile.keyStoreFileCredentialUpdate();    
   }
}