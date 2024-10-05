# SymantecPAM-KeystoreFile
Symantec PAM target connector for Java KeyStore files.

This connector is used to verify and update the password required to open a Java keystore.
It is not used to verify or update the keys stored in the keystore.

A Java keystore can be used to store certificates, asymmetric key pairs and symmetric keys. 
There can be multiple certificates and keys in a keystore.
The keystore is protected with a password and the individual keys are also protected with
a password. Depending on where the keystore is used, the password for the keystore and for the keys
can be the same or it can be different. Individual keys can have different passwords.
If the keystore is used with Tomcat, the password for the keystore and the key must be the same.

The connector here can verify/update the password for keystore file and for one (1) key.

The connector copies the keystore file to the TCF server into the Tomcat temp directory.  
For a verify operation this now local keystore is inspected and it is validated if the
current password can be used to open the keystore.  
For an update operation a backup keystore can be sent to the originating server (remote or local). 
This is a copy of the original keystore with a new filename. 
The password used to open the keystore is updated to the new password from PAM and the updated 
keystore file is sent to the remote or local server.  
Finally, the temporary file on the TCF server is deleted.


## Build KeystoreFile connector

### Environment
The environment used is as follows:

- CentOS 9 (with SELINUX)
- Java JDK, version 17.0.12
- Apache Tomcat, version 10.1.30
- Symantec PAM, version 4.2.0.826
- capamextensioncore, version 4.21.0.82

### Installation
- Download the project sources from GitHub.
- Add the `capamextensioncore.jar` from Symantec PAM as part of local Maven repository.
- Edit the files `keystorefile_messages.properties` and `KeyStoreFileMessageConstants.java`
and adjust the message numbers to to match your environment.
It is important that the numbers does not conflict with any other numbers from other connectors.
- There is an important variable in the KeyStoreFile.java file. It is the constant EXTENDED_DEBUG. If this is set to true when compiling the connector, additional debugging information may be written to the catalina.out log file. If it is written will depend on the loglevel for the connector as defined in the Tomcat logging.properties file. If extended debugging is enabled at compile time, additional information including current and new passwords may be visible in the catalina.out log file. This should not be enabled when compiling the connector for a production environment.
- Run the command `mvnw package` to compile the connector.
- Copy the target connector `keystorefile.war` to the Tomcat `webapps_targetconnector` directory.
- It is recommended to enable logging from the connector by adding the following to the
Tomcat `logging.properties` file.

```
#
# Target Connectors
#
ch.pam_exchange.pam_tc.keystorefile.api.level = FINE
ch.pam_exchange.pam_tc.keystorefile.api.handlers= java.util.logging.ConsoleHandler

ch.pam_exchange.pam_tc.filecopy.api.level = FINE
ch.pam_exchange.pam_tc.filecopy.api.handlers= java.util.logging.ConsoleHandler
```

- Finally start/restart Tomcat

## KeystoreFile connector in PAM

### Appliction

![KeystoreFile Appliction](/docs/KeystoreFile-Application-1.png)
![KeystoreFile Appliction](/docs/KeystoreFile-Application-2.png)

The fields used are:

- Location of keystore file  
API/CLI field: `location`  
This can be `Remote UNIX`, `Remote Windows` or `Local TCF`. Set the radio button
accordingly.


- Port (for Remote UNIX) 
API/CLI field: `port`   
This is the port used when establishing an SSH connection to a remote UNIX server.


### Account

The information for a KeystoreFile account will require a login account. This account
is an account configured in PAM. It can be a domain or local account.

![KeystoreFile Account](/docs/KeystoreFile-Account-1.png)
![KeystoreFile Account](/docs/KeystoreFile-Account-2.png)

The fields used are:

- Login account to remote server filename  
API/CLI field name: `loginAccount`  
Valid value is the internal ID for an account defined in PAM.  
If the application uses a remote Windows or UNIX system, select an account with
login permissions to the remote server. The account must have read/write permissions
to the keystore path and filename.


- Keystore (path+) filename    
API/CLI field name: `filename`  
Valid value is a path+filename for a keystore file on the remomte or local TCF server.  
For Windows servers the protocol used is SMB and you must specify a share/path to the file.
This can be `c$/tmp/test.keystore` or any other network share on the server.
For UNIX servers just specify the path and filename for the Keystore file.
The login account must have read/write permissions to the path and file.
For files on the local TCF server, specify the path+filename. The account running Tomcat
must have read/write permissions to the path and file.


- Keystore format  
API/CLI field name: `keystoreFormat`  
Valid values are PKCS12 and JKS  
 Select the format of the keystore file. Typically PKCS12 is used, but support for older
JKS format is avaialble.


- Same password for key entry  
API/CLI field name: `sameKeyPasswd`  
Valid values are `true` and `false`  
If this is enabled the connector will verify/update both the keystore file password
and the password for the key with the given alias.


- Key entry alias (if different from username)  
API/CLI field name: `alias`  
If the alias for a key is different from the account username it can be specified here. 
The field is only used if the same password is used both for the file and the key entry.


- Create backup file    
API/CLI field name: `createBackup`  
Valid values are `true` and `false`  
If this is checked a backup of the keystore file is created. The login account
must have permissions to create a new file in the path for the Keystore file.


## Example keystore

The example used to test the connector is a tomcat-users.xml file. It is available
on a remote Windows, remote Linux and locally on the TCF server.

```
keytool -genkey -keyalg RSA -alias rsakey -keystore test.keystore -storepass mySecretPassword -validity 360 -keysize 4096 -dname "CN=rsaKey, OU=Test, O=PAM-Exchange, C=CH"
```

After adding two keys the content of the keystore should look something like this

```
C:\tmp>keytool -list -keystore test.keystore -storepass mySecretPassword
Keystore type: PKCS12
Keystore provider: SUN

Your keystore contains 1 entries

rsakey, 1. okt. 2024, PrivateKeyEntry,
Certificate fingerprint (SHA-256): EC:61:5D:AF:DA:CB:BC:F0:F1:F1:D9:B6:B0:68:B0:5C:8A:DD:C6:79:1F:E5:A7:16:55:5E:20:CF:A8:D4:3A:E4
```


## Login accounts in PAM
In PAM it is necessary to setup accounts, which can login to the remote servers used.
For the Windows server it can be a local user or a domain user. What is important is
that the user can connect to the `c$` share or a share where the keystore file
is located.
For the (remote) Linux server an account is required to login to the server. The permissions
for the user must be sufficient to write to the directory and the file used.

**Windows login account**

![Login Account for Windows](/docs/LoginAccount-Windows.png)


## Version history

1.0.0 - Initial release

