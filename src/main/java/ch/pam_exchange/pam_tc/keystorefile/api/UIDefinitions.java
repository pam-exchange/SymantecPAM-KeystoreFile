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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONException;

import com.ca.pam.extensions.core.util.UIDefinitionManager;
import com.ca.pam.extensions.core.model.LoggerWrapper;

@Path("uiDefinitions")
public class UIDefinitions {
	private static final Logger LOGGER = Logger.getLogger(UIDefinitions.class.getName());
	
    @GET
    @Path("/{uiDefinitionType}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUIDefinitions (@PathParam("uiDefinitionType") String uiDefinitionType) throws JSONException, IOException{
        LOGGER.log(Level.INFO, LoggerWrapper.logMessage("Retreive UI Definitions for " + uiDefinitionType + " type"));
        Response response = UIDefinitionManager.getUIDefinition(uiDefinitionType);    	
    	return response;
    }
}
