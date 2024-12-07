package com.boc.deployer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.FactoryConfigurationError;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import com.boc.eventhandler.BOCLoanDocumentEventhandler;
import com.boc.util.PropertyReader;
import com.filenet.api.admin.CodeModule;
import com.filenet.api.collection.ActionSet;
import com.filenet.api.collection.ContentElementList;
import com.filenet.api.collection.VersionableSet;
import com.filenet.api.constants.AutoClassify;
import com.filenet.api.constants.AutoUniqueName;
import com.filenet.api.constants.CheckinType;
import com.filenet.api.constants.DefineSecurityParentage;
import com.filenet.api.constants.PropertyNames;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.constants.ReservationType;
import com.filenet.api.core.ContentTransfer;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.core.VersionSeries;
import com.filenet.api.events.Action;
import com.filenet.api.property.FilterElement;
import com.filenet.api.property.PropertyFilter;

/**
 * 
 * @author C734363
 *
 */
public class CodeModulesManager {
	
	public static Logger log = Logger.getLogger(CodeModulesManager.class);	
	 
	private static Calendar lastLog4jPropertiesReloadedOn = null;
	public static String log4jpath;
	com.filenet.api.core.ObjectStore objectStore;
	
	
	 static void init()
	    {
	       try {
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
		} catch (FactoryConfigurationError e) {
			 log.debug("CodeModulesManager init() failed due to "+e.fillInStackTrace());
		} catch (Exception e) {			
			 log.debug("CodeModulesManager : init() due to  "+e.fillInStackTrace());
		}
	    }
	
	
	public CodeModulesManager(ObjectStore objectStore) {
		init();
		this.objectStore = objectStore;
	}

	public void update(String name, Collection<File> files) {
		updateCodeModule(files, getCodeModule(name) );
		updateReferencingActions( getCodeModule(name) );
	}

	public void create(String name, Collection<File> files ) {
		
	  Document doc = Factory.Document.createInstance(objectStore, "CodeModule");
	  doc.getProperties().putValue("DocumentTitle", name );
	  doc.set_ContentElements( createContent(files) );
	  doc.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION );
	  doc.save(RefreshMode.REFRESH);
	  
	  Folder folder = Factory.Folder.getInstance(objectStore, "Folder", "/CodeModules" );
	  folder.file(doc, AutoUniqueName.AUTO_UNIQUE, null,
			    DefineSecurityParentage.DO_NOT_DEFINE_SECURITY_PARENTAGE).save(RefreshMode.NO_REFRESH);
	}
	
	public CodeModule getCodeModule(String name) {
		CodeModule codeModule = Factory.CodeModule.getInstance(objectStore, "/CodeModules/" + name );
		codeModule.refresh( new String[] { PropertyNames.VERSION_SERIES } );
		return codeModule;
	}

	private void updateCodeModule(Collection<File> files, CodeModule codeModule) {
		VersionSeries versionSeries = codeModule.get_VersionSeries(); 
		versionSeries.fetchProperties( new String[]  { PropertyNames.CURRENT_VERSION } );
		Document document = (Document) versionSeries.get_CurrentVersion();
		
		document.checkout(ReservationType.EXCLUSIVE, null, document.getClassName(), null);
		document.save(RefreshMode.REFRESH);
		Document reservation = (Document) document.get_Reservation();
		
		ContentElementList contentElementList = createContent(files);
		
		reservation.set_ContentElements(contentElementList);
		reservation.checkin(AutoClassify.DO_NOT_AUTO_CLASSIFY, CheckinType.MAJOR_VERSION );
		reservation.save(RefreshMode.REFRESH);
	}

	@SuppressWarnings("unchecked")
	protected ContentElementList createContent(Collection<File> files) {
		
		ContentElementList contentElementList = Factory.ContentElement.createList();

		for ( File file : files ) {
			
			if ( ! file.exists() ) {
				continue;
			}
			
			ContentTransfer content = createFileContent(file);
			contentElementList.add(content);
		}
		return contentElementList;
	}

	private ContentTransfer createFileContent(File file) {
		ContentTransfer content = Factory.ContentTransfer.createInstance();
		content.set_RetrievalName( file.getName() );
		try {
			content.setCaptureSource( new FileInputStream( file ) );
		} catch (FileNotFoundException e) {
			// Should not happen as only existing files are added...
		}
		
		content.set_ContentType( getFileContentType(file) );
		return content;
	}

	private String getFileContentType(File file) {
		String contentType = null;
		if ( file.getName().toLowerCase().endsWith(".jar") ||
			 file.getName().toLowerCase().endsWith(".zip") ) {
			contentType = "application/x-zip-compressed";
		} else if ( file.getName().endsWith( ".class" ) ) {
			contentType = "application/java";
		}
		return contentType;
	}

	private void updateReferencingActions(CodeModule codeModule) {
		for (Action  action : getCodeModuleActions(codeModule) ) 
		{
			action.set_CodeModule(codeModule);
			action.save(RefreshMode.NO_REFRESH);
		}
	}

	private Collection<Action> getCodeModuleActions(CodeModule codeModule) {
		VersionableSet versions = getCodeModuleVersionsAndActions(codeModule);
		Iterator<?> versionsIterator = versions.iterator();
		Set<Action> actions = new HashSet<Action>();
		
		while ( versionsIterator.hasNext() ) {
			
			Document document = (Document) versionsIterator.next();
			ActionSet actionSet = ((CodeModule)document).get_ReferencingActions();
			Iterator<?> iterator = actionSet.iterator();
	
			while (iterator.hasNext()) {
				Action action = (Action) iterator.next();
				actions.add( action );
			}
		}
		
		return actions;
	}

	private VersionableSet getCodeModuleVersionsAndActions(CodeModule codeModule) {
		VersionSeries versionSeries = codeModule.get_VersionSeries(); 

		PropertyFilter propertyFilter = new PropertyFilter();
		propertyFilter.addIncludeProperty( new FilterElement(null, null, null, PropertyNames.VERSIONS, null ) );
		propertyFilter.addIncludeProperty( new FilterElement(null, null, null, PropertyNames.REFERENCING_ACTIONS, null ) );
		versionSeries.fetchProperties(propertyFilter);
		
		VersionableSet versions = versionSeries.get_Versions();
		return versions;
	}
}