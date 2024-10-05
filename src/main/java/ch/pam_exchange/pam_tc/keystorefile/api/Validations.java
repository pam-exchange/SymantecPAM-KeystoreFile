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

import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.ca.pam.extensions.core.ConnectorJSONUtil;
import com.ca.pam.extensions.core.TargetAccount;
import com.ca.pam.extensions.core.TargetApplication;
import com.ca.pam.extensions.core.api.exception.ExtensionException;
import com.ca.pam.extensions.core.model.ExtensionResponse;
import com.ca.pam.extensions.core.model.LoggerWrapper;
import com.ca.pam.extensions.core.util.MessageConstants;
import com.ca.pam.extensions.core.util.ValidationManager;


/**
 * Service class for validating target account and application data before
 * saving in CA PAM Database.
 * 
 * URL pattern forthis service is 
 * https://<TC_HOST>:<TC_PORT>/<TC_NAME>/validations/{validationType}
 * TC_HOST is the host name of the device where target connector is deployed.
 * TC_PORT is the port of the tomcat connector where target connector is 
 * deployed.
 * TC_NAME is the name of target connector.
 * validationType is a parmeter which is either account or application.
 * 
 * Two service methods are defined that support POST and PUT methods.
 * 1. validateCreateData -- This method support POST method and validates
 * target application and account data before the object is created by PAM.
 * 2. validateUpdateData -- This method supports PUT method and validates 
 * target application and account data when the object is udpated by PAM.
 *
 * Both methods validate target account and application data based on the 
 * constratints defined in uiDefinitions.json file. 
 *
 * These methods call performCustomAccountValidations to do custom validations
 * on Account and performCustomApplicationValidations to do custom validations
 * on Application.
 * These methods are stub method which has to be implemented
 * by Target Connector Developer for adding any custom validations.
 *
 */
@Path("validations")
public class Validations {

  private static final Logger LOGGER = Logger.getLogger(Validations.class.getName());
  
//  private ExtensionException exception;
  
  @POST
  @Path("/{validationType}")
  @Consumes(MediaType.TEXT_PLAIN)
  @Produces(MediaType.APPLICATION_JSON)
  public Response validateCreateData(@PathParam("validationType") String validationType, String jsonObject) throws Exception{

    String duration;
    final long methodStartTime = System.currentTimeMillis();
    long methodEndTime;
    
    ExtensionException extensionException = null;
    TargetAccount targetAccount = new TargetAccount();
    TargetApplication targetApplication = new TargetApplication();
    try {
      if ("account".equals(validationType)) {
          targetAccount = ConnectorJSONUtil.getTargetAccountFromJSON(jsonObject);
      } else if ("application".equals(validationType)) {
          targetApplication = ConnectorJSONUtil.getTargetApplicationFromJSON(jsonObject);
      }
      ValidationManager.validateData(validationType, jsonObject);
    } catch (ExtensionException e) {
      extensionException = e;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Failed to validate data: "), e);
      methodEndTime = System.currentTimeMillis();
      duration = Long.toString(methodEndTime - methodStartTime);
      LOGGER.log(Level.INFO, LoggerWrapper.logMessage("Duration of failed call to validateCreateData method" 
                 + " = " + duration + " ms."));
      throw new ExtensionException(MessageConstants.SERVER_ERROR, false, new String[0]);
    }
    try {
        if ("account".equals(validationType)) {
            performCustomAccountValidation(targetAccount);
        } else if ("application".equals(validationType)) {
            performCustomApplicationValidation(targetApplication);
        }
    } catch (ExtensionException e) {
        if (extensionException != null) {
            extensionException.addErrors(e.getErrors());
        } else {
            extensionException = e;
        }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Failed to validate data: "), e);
      methodEndTime = System.currentTimeMillis();
      duration = Long.toString(methodEndTime - methodStartTime);
      LOGGER.log(Level.INFO, LoggerWrapper.logMessage("Duration of failed call to validateCreateData method" 
                 + " = " + duration + " ms."));
      throw new ExtensionException(MessageConstants.SERVER_ERROR, false, new String[0]);
    }
    if (extensionException != null) {
        methodEndTime = System.currentTimeMillis();
        duration = Long.toString(methodEndTime - methodStartTime);
        LOGGER.log(Level.INFO, LoggerWrapper.logMessage("Duration of failed call to validateUpdateData method" 
             + " = " + duration + " ms."));
        throw extensionException;
    } 
    ExtensionResponse response = buildSuccessResponse(true);
    methodEndTime = System.currentTimeMillis();
    duration = Long.toString(methodEndTime - methodStartTime);
    LOGGER.log(Level.INFO, LoggerWrapper.logMessage("Duration of call to validateCreateData method" 
          + " = " + duration + " ms."));
    return Response.status(Response.Status.OK).entity(response).build();
  }

  @PUT
  @Path("/{validationType}")
  @Consumes(MediaType.TEXT_PLAIN)
  @Produces(MediaType.APPLICATION_JSON)
  public Response validateUpdateData(@PathParam("validationType") String validationType, String jsonObject) throws Exception{
    String duration;
    final long methodStartTime = System.currentTimeMillis();
    long methodEndTime;
    
    ExtensionException extensionException = null;
    TargetAccount targetAccount = new TargetAccount();
    TargetApplication targetApplication = new TargetApplication();
    try {
      if ("account".equals(validationType)) {
          targetAccount = ConnectorJSONUtil.getTargetAccountFromJSON(jsonObject);
      } else if ("application".equals(validationType)) {
          targetApplication = ConnectorJSONUtil.getTargetApplicationFromJSON(jsonObject);
      }
      ValidationManager.validateData(validationType, jsonObject); 
    } catch (ExtensionException e) {
      extensionException = e;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Failed to validate data: "), e);
      throw new ExtensionException(MessageConstants.SERVER_ERROR, false, new String[0]);
    }
    try {
        if ("account".equals(validationType)) {
            performCustomAccountValidation(targetAccount);
        } else if ("application".equals(validationType)) {
            performCustomApplicationValidation(targetApplication);
        }
    } catch (ExtensionException e) {
        if (extensionException != null) {
            extensionException.addErrors(e.getErrors());
        } else {
            extensionException = e;
        }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Failed to validate data: "), e);
      methodEndTime = System.currentTimeMillis();
      duration = Long.toString(methodEndTime - methodStartTime);
      LOGGER.log(Level.INFO, LoggerWrapper.logMessage("Duration of failed call to validateUpdateData method" 
                 + " = " + duration + " ms."));
      throw new ExtensionException(MessageConstants.SERVER_ERROR, false, new String[0]);
    }
    if (extensionException != null) {
        methodEndTime = System.currentTimeMillis();
        duration = Long.toString(methodEndTime - methodStartTime);
        LOGGER.log(Level.INFO, LoggerWrapper.logMessage("Duration of failed call to validateUpdateData method" 
             + " = " + duration + " ms."));
        throw extensionException;
    } 
    ExtensionResponse response = buildSuccessResponse(true);
    methodEndTime = System.currentTimeMillis();
    duration = Long.toString(methodEndTime - methodStartTime);
    LOGGER.log(Level.INFO, LoggerWrapper.logMessage("Duration of call to validateUpdateData method" 
                 + " = " + duration + " ms."));
    return Response.status(Response.Status.OK).entity(response).build();
  }

  /** Build the success response
    * @param successValue
    * @return
    */
   private ExtensionResponse buildSuccessResponse(boolean successValue) {
     ExtensionResponse extensionResponse = new ExtensionResponse();
     extensionResponse.setMeta(extensionResponse.new MetaData(successValue));    
     return extensionResponse;
   }
   
   /**
    * Build the exception to be thrown
    * @param messageCode
    * @param isSuccess
    * @param args
    */
/*
   private void buildException(String messageCode, boolean isSuccess, String... args) {
       if(null == exception) {
               exception = new ExtensionException(messageCode,isSuccess,args);
       }else {
               exception.addErrorCode(messageCode,args);
               exception.updateSuccess(isSuccess);
       }
   }
*/
   /**
   * This method must implemented to add any custom validations of 
   * account extended attributes in addition to those that are defined in 
   * ui definitions constraints.
   * @param targetAccount -- TargetAccount object.
   * @throws ExtensionException if there is a validation failure with appropriate
   * error message.
   */
  private void performCustomAccountValidation(TargetAccount targetAccount) throws ExtensionException {
      
  }
  
  /**
  * This method must implemented to add any custom validations of application
  * extended attributes in addition to those that are defined in 
  * ui definitions constraints.
  * @param targetApplication -- TargetApplication object.
  * @throws ExtensionException if there is a validation failure with appropriate
  * error message.
  */
 private void performCustomApplicationValidation(TargetApplication targetApplication) throws ExtensionException {
     
 }

}
