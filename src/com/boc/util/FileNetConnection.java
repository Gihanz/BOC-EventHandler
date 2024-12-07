package com.boc.util;

import java.util.Properties;
import javax.security.auth.Subject;
import org.apache.log4j.Logger;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.util.UserContext;

/**
 * 
 * @author C734363
 *
 */
public class FileNetConnection {

	 public static Logger log = Logger.getLogger(FileNetConnection.class);
	 
	/**
	 * @param args
	 */
	@SuppressWarnings("finally")
	public ObjectStore getConnection() {
	
		if(log.isDebugEnabled()){
			log.debug("FileNetConnection : getConnection() begin");
		}
		ObjectStore ceObjectStore = null;
		try {
			PropertyReader property=new PropertyReader();
			Properties prop=property.loadPropertyFile();
			Connection ceConnection;
			Domain ceDomain;
			String url=PropertyReader.getProperty(prop, "CONNECTION_URL");
			ceConnection = Factory.Connection.getConnection(url);
			Subject ceSubject = UserContext.createSubject(ceConnection, PropertyReader.getProperty(prop, "CONNECTION_USER"), PropertyReader.getProperty(prop, "CONNECTION_PASSWORD"), "FileNetP8WSI");
			UserContext.get().pushSubject(ceSubject);
			
			ceDomain = Factory.Domain.fetchInstance(ceConnection, null, null);
			ceObjectStore = Factory.ObjectStore.fetchInstance(ceDomain, PropertyReader.getProperty(prop, "CONNECTION_OS"), null);
			if(ceObjectStore!=null ){
				if(log.isDebugEnabled()){
					log.debug("FileNetConnection : getConnection() successfull. Object Store connected --"+ceObjectStore.get_DisplayName());
				}	
			}
		} catch (Exception e) {
			log.error("FileNetConnection : getConnection() failed due to "+e.getCause());
			
		}finally{
			
			if(log.isDebugEnabled()){
				log.debug("FileNetConnection : getConnection() end");
			}
			return ceObjectStore;
		}
		
		
    
	}
	
	public static void main(String args[]){
		new FileNetConnection().getConnection();
		
	}

}
