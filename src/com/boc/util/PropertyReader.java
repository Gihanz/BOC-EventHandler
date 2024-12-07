
package com.boc.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * 
 * @author C734363
 *
 */
public class PropertyReader {
	
	 public static Logger log = Logger.getLogger(PropertyReader.class);
	 
	String appPath;
	
	public PropertyReader() throws Exception{
		
		appPath = this.getAppPath();
	}
	/**
	 * This function returns the current directry oof the executable
	 * @return
	 */
	public static String getAppPath() {
		return System.getProperty("user.dir")+"/resources/env.properties";
	}
	
	/**
	 * 
	 * @param propertyFilePath
	 * @return
	 * @throws Exception
	 */

	public Properties loadPropertyFile() throws Exception
	{

		try
		{
			Properties props = new Properties();
			FileInputStream fis = new FileInputStream(appPath);
			props.load(fis);
			fis.close();
			return props;
		} 
		catch (Exception e)
		{
			e.fillInStackTrace();
			throw new Exception(e);
		}
	}
	/**
	 * 
	 * @param props
	 * @param propertyName
	 * @return
	 * @throws Exception
	 */
	public static String getProperty(Properties props, String propertyName)throws Exception
	{
		try
		{
			String propertyValue = (String) props.get(propertyName);
			if (propertyValue == null)
			{
				throw new Exception("Property "+ propertyName + " is not define in loaded *.properties file");
			}
			return propertyValue;
		} 
		catch (Exception e)
		{
			throw e;
		}
	}
	/**
	 * 
	 * @param logPropertyFile
	 * @param logFilePath
	 */
	public static void loadLogConfiguration(
		String logPropertyFile,
		String logFilePath) {
		Properties logProperties = new Properties();
		try {
			FileInputStream fis = new FileInputStream(logPropertyFile);
			logProperties.load(fis);
			fis.close();
		} catch (FileNotFoundException e) {
			System.out.println("Warning : " + e);
		} catch (IOException e) {
			System.out.println("Warning : " + e);
		}
		logProperties.setProperty("log4j.appender.A1.File", logFilePath);
		PropertyConfigurator.configure(logProperties);
	}

	public static void loadLogConfiguration(
		String logPropertyFile,
		String logFilePath,
		String logFileName) {
		Properties logProperties = new Properties();
		try {
			FileInputStream fis = new FileInputStream(logPropertyFile);
			logProperties.load(fis);
			fis.close();
		} catch (FileNotFoundException e) {
			System.out.println("Warning : " + e);
		} catch (IOException e) {
			System.out.println("Warning : " + e);
		}
		File file = new File(logFilePath);
		file.mkdirs();
		String logFile = logFilePath + "\\" + logFileName;
		logProperties.setProperty("log4j.appender.A1.File", logFile);
		PropertyConfigurator.configure(logProperties);
		
	}
	public static void loadLogConfiguration(String logPropertyFile) {
		Properties logProperties = new Properties();
		try {
			FileInputStream fis = new FileInputStream(logPropertyFile);
			logProperties.load(fis);
			fis.close();
		} catch (FileNotFoundException e) {
			System.out.println("Warning : " + e);
		} catch (IOException e) {
			System.out.println("Warning : " + e);
		}
		PropertyConfigurator.configure(logProperties);
	}
	public static void main(String[] args) {
		try {
			PropertyReader propReader = new PropertyReader();
			propReader.loadPropertyFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
