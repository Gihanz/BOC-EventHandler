package com.boc.eventhandler;


import java.io.File;
import java.util.Calendar;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.boc.util.PropertyReader;
import com.filenet.api.collection.RepositoryRowSet;
import com.filenet.api.constants.AutoUniqueName;
import com.filenet.api.constants.DefineSecurityParentage;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Document;
import com.filenet.api.core.DynamicReferentialContainmentRelationship;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.engine.EventActionHandler;
import com.filenet.api.events.ObjectChangeEvent;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.Id;
import com.filenet.apiimpl.query.RepositoryRowImpl;


/**
 * 
 * @author C734363
 *
 */
public class BOCLoanDocumentEventhandler implements EventActionHandler {

 public static Logger log = Logger.getLogger(BOCLoanDocumentEventhandler.class);
 private static Calendar lastLog4jPropertiesReloadedOn = null;
 public static String log4jpath;
 
 public static void init() throws Exception
 {
    // String propPath = "/opt/subscription/log4j.xml";
	 PropertyReader property=new PropertyReader();
	Properties prop=property.loadPropertyFile();
	log4jpath=PropertyReader.getProperty(prop, "LOGPATH");
     File fin = new File(log4jpath);
     Calendar lastModCal = Calendar.getInstance();
     lastModCal.setTimeInMillis(fin.lastModified());
     if(lastLog4jPropertiesReloadedOn != null)
     {
         log.debug((new StringBuilder("Log4j property file last loaded on:[")).append(lastLog4jPropertiesReloadedOn.getTime()).append("] ").append("Log4j property file last modified on:[").append(lastModCal.getTime()).append("]").toString());
     }
     if(lastLog4jPropertiesReloadedOn == null || lastLog4jPropertiesReloadedOn.before(lastModCal))
     {
         DOMConfigurator.configure(log4jpath);
         lastLog4jPropertiesReloadedOn = lastModCal;
         log.debug("Reloaded the Log4j property file as it has been modified since its last loaded time");
     }
 }
	 
	public void onEvent(ObjectChangeEvent event, Id id)
			throws EngineRuntimeException {
	try {
			
		init();
		log.debug("BOCAttachmentEventhandler : onEvent() begin ");
		ObjectStore os =  event.getObjectStore();
		
		String nicNo = null;
		String loanReference = null;
	    Folder folder= null;
	    
	    	Document form = Factory.Document.fetchInstance(os, event.get_SourceObjectId(), null);	  
	    	loanReference=form.getProperties().getStringValue("BOC_ReferenceNo");
	    	nicNo=form.getProperties().getStringValue("BOC_NICNo");
	    	  SearchSQL sql = new SearchSQL();
	    	if(nicNo!=null && !nicNo.isEmpty()){
	    	
	    	log.debug("BOCAttachmentEventhandler : onEvent()  --nic No -"+nicNo);
		      sql.setSelectList("PathName");
		      sql.setFromClauseInitialValue("BOC_PersonalLoan", null, false);    
		      sql.setWhereClause("BOC_nicNo ='"+nicNo+"'");
	    	} 
	    	else if(loanReference!=null && !loanReference.isEmpty()){
	    		  log.debug("BOCAttachmentEventhandler : onEvent()  LoanReferenceNumber -"+loanReference);
			      sql.setSelectList("PathName");
			      sql.setFromClauseInitialValue("BOC_FrontDesk", null, false);    
			      sql.setWhereClause("BOC_LoanReferenceNumber ='"+loanReference+"'");
	    		
	    	}
	    SearchScope search = new SearchScope(os);
	    log.debug("BOCAttachmentEventhandler : onEvent() Query-- "+sql);
	    String loanfolder=null;
	    RepositoryRowSet myRows = search.fetchRows(sql, null, null, null);
	    log.debug("BOCAttachmentEventhandler : onEvent() Cases not Found "+ myRows.isEmpty());
	    if(myRows!=null){
	    	RepositoryRowImpl rowImpl = (RepositoryRowImpl) myRows.iterator().next();
	  	loanfolder=rowImpl.getProperties().getStringValue("PathName");
	  	  log.debug("BOCAttachmentEventhandler : onEvent() Cases Folder path "+ loanfolder);
	    }
	    
	   if(loanfolder!=null){
	    folder = Factory.Folder.fetchInstance(os, loanfolder.toString(), null);
	    
	    DynamicReferentialContainmentRelationship rcr = 
	        Factory.DynamicReferentialContainmentRelationship.createInstance(os, null, 
	                           AutoUniqueName.AUTO_UNIQUE, 
	                           DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE);
	    rcr.set_Tail(folder);
	    rcr.set_Head(event.get_SourceObject());
	    rcr.save(RefreshMode.REFRESH);
	    
	    log.debug("BOCAttachmentEventhandler : onEvent() Document Filed in successfully ");
	      }
	    }
	    catch (EngineRuntimeException e)
	    {
	      log.error("BOCAttachmentEventhandler : onEvent() Failed due to "+e.fillInStackTrace());
	    }
	    catch(Exception e){
	    	log.error("BOCAttachmentEventhandler : onEvent() Failed due to "+e.fillInStackTrace());
	    	
	    }	finally{
	    }
	    	
	    	log.debug("BOCAttachmentEventhandler : onEvent() end ");
	    }

}
