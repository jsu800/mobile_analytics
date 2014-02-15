import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.net.ssl.HttpsURLConnection;
import java.util.zip.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
//import java.util.Map.Entry;
import java.util.TreeMap;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.List;

import java.io.FileWriter;
import com.google.gson.Gson;


public class MobileAnalytics {

    private final String outFilePath = "report.txt";
    private final String attachmentPath = "analytics.xls";
    private final String iTuneURI = "http://itunes.apple.com/lookup?id=";
	private final String configDataPath = "/Users/joe/Documents/workspace/MobileAnalytics/bin/competitors_config.txt";
    private final String outputDataPath = "/Users/joe/Documents/workspace/MobileAnalytics/bin/analytics.xls";
	
    private List<String> dateList = new ArrayList<String>();

	public ArrayList<String> getDateList() {
		return (ArrayList<String>)dateList;
	}

	private String version = null;
	private String userRatingCountForCurrentVersion = null;
	private String averageUserRatingForCurrentVersion = null;
	private String averageUserRating = null; 
	private String userRatingCount = null;    
    
    
	// Map: <appName, ArrayList> 
	private Map<String, Map<String, ArrayList<String[]>>> masterMap = new TreeMap<String, Map<String, ArrayList<String[]>>>();

	public Map<String, Map<String, ArrayList<String[]>>> getMasterMap() {
		return masterMap;
	}

	public void setMasterMap(Map<String, Map<String, ArrayList<String[]>>> masterMap) {
		this.masterMap = masterMap;
	}

	private List<String[]> accountsList = new ArrayList<String[]>();	
	
	private Map<String, String[]> configHash = new HashMap<String, String[]>();
	
	
	
	
	public static void main(String[] params) throws Throwable {

		MobileAnalytics analytics = new MobileAnalytics();
		
		
		// params:
		// Account#1 <p1, p2, p3, p4, p5, p6, p7, p8, p9>
		// Account#2 <p1, p2, p3, p4, p5, p6, p7, p8, p9>
		// etc
		// Date1, Date2, Date3, etc
		//
		// paramsList:
		// Account#1 <p1, p2, p3, p4, p5, p6, p7, p8, p9>
		// Account#2 <p1, p2, p3, p4, p5, p6, p7, p8, p9>
	    int numEntries = analytics.sanitizeParams(params);	
	    
	    //int numDates = params.length-numEntries;
	    //System.out.println("numDates = " + numDates);
	    
	    
	    // Run through the specified accounts 
	    List<String[]> list = analytics.getAccountsList();
	    Iterator<String[]> it = list.iterator();
	    while (it.hasNext()) {

	    	String[] account = (String[])it.next();
	    	
		    // Run through the specified dates per account 
		    for (int i = numEntries; i<params.length; i++) {
		    	analytics.goFetchMyData(account, params[i]);
		    	analytics.setDates(params[i]);
		    }
	    }
	    
		//print data
		//String dataText = analytics.printData();		
		//analytics.writeFile(dataText);
		
		// write them to excel
		analytics.writeExcelFile();
		
		//send email
	    //analytics.sendEmail();
	    
	    
		
	    // Let's run competitor's reports here
	    //analytics.loadConfig();
	    
	    //analytics.getCompetitorsData();
	    
	}
	
	public void setDates(String date) {		
			if (!dateList.contains(date)) {
				System.out.println("ADDING date: " + date);
				dateList.add(date);
			}
	}
	
	public List<String[]> getAccountsList() {
		return accountsList;
	}

	public void setAccountsList(List<String[]> accountsList) {
		this.accountsList = accountsList;
	}

	public void getCompetitorsData() {
		
	}
	
	public void loadConfig() {

		try {
			
			FileInputStream fstream = new FileInputStream(configDataPath);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine = null;
			String regex = "=";
			
			while ((strLine = br.readLine()) != null)   {
				
				String[] theline = strLine.split(regex);
				
				String key = theline[0];
				String competitors = theline[1];
				String[] competitorArray = null;
				
				if (competitors.isEmpty() == false) {
												
					competitorArray = competitors.split(",");
				}
								
				if (configHash.containsKey(key)) {
					configHash.remove(key);
				}
				
				configHash.put(key, competitorArray);				
				System.out.println(key + " | " + Arrays.toString(competitorArray));
			}
			in.close();

			
		} catch (Exception e) {
			System.err.println("loadConfig Error: " + e.getMessage());
		}
	}				
	
	
	/* Group all of the params into account list for use by auto ingestion tool for data
	 * fetching 
	 */
	private int sanitizeParams(String[] params) {
		
		int numParams = params.length;
			
		int listIndex = 0;
		for (int i = 0; i<numParams; i++) {
			
			if (params[i].matches("\\S*[@]\\S*")) {
				accountsList.add(listIndex, new String[] {params[i], params[i+1], params[i+2], params[i+3], params[i+4], params[i+5]});
				listIndex++;
				i = i + 5;
			}
	    }
		
		return listIndex*6;

	}
	
	
	private void getFile(HttpsURLConnection paramHttpsURLConnection) throws IOException, Throwable {
	
		String filename = paramHttpsURLConnection.getHeaderField("filename");
		System.out.println(filename);
		int i = 0;
				
		BufferedInputStream localBufferedInputStream = new BufferedInputStream(paramHttpsURLConnection.getInputStream());	
		BufferedOutputStream localBufferedOutputStream = new BufferedOutputStream(new FileOutputStream(filename));
		
		byte[] arrayOfByte = new byte[1024];
		
		while ((i = localBufferedInputStream.read(arrayOfByte)) != -1) {			
		  localBufferedOutputStream.write(arrayOfByte, 0, i);
		}
		
		localBufferedInputStream.close();
		localBufferedOutputStream.close();
		System.out.println("File Downloaded Successfully ");

		//Let's unzip the file
		unzip(filename);
	
		//Let's parse the file
		parseFile(outFilePath);
		
	}

	
	private String unzip(String inFilePath) throws IOException
	{
	    GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(inFilePath));
	 
	    OutputStream out = new FileOutputStream(outFilePath);
	 
	    byte[] buf = new byte[1024];
	    int len;
	    while ((len = gzipInputStream.read(buf)) > 0)
	        out.write(buf, 0, len);
	 
	    gzipInputStream.close();
	    out.close();
	 
	    new File(inFilePath).delete();
	 
	    return outFilePath;
	}
	
	
	private void parseFile(String fileName) throws Throwable {
		String line = "";
		String regex = "\\t";
		
		try {
			
			FileReader fr = new FileReader(fileName);
			BufferedReader br = new BufferedReader(fr);

			//Let's bypass the first line because it's just column names
			br.readLine();
			
			while((line = br.readLine()) != null) {
				
				String[] theline = line.split(regex);				
				String appName = theline[4];
				String brandName = theline[3];
				String productTypeId = theline[6];
				int unitNum = Integer.parseInt(theline[7]);
				String platformName = getPlatform(productTypeId);
				String type = getType(productTypeId);
				String appId = theline[14];

				int dlCount = 0;
				int upCount = 0;
				String currentDate = theline[9];
				
				if (type == "dl") {
					dlCount = unitNum;
				} else {
					upCount = unitNum;
				}
					
				
				//Let's check to see if app name is already in the map
				if (appName != null) {
				
					if (masterMap.containsKey(appName) == false) {

						//System.out.println("AppId = " + appId);
						
						// First go fetch the app's iTune data and populate them in method scope
						AppDataResults results = getJSONFromItune(appId);
						version = results.getVersion();
						userRatingCountForCurrentVersion = results.getUserRatingCountForCurrentVersion();
						averageUserRatingForCurrentVersion = results.getAverageUserRatingForCurrentVersion();
						averageUserRating = results.getAverageUserRating();
						userRatingCount = results.getUserRatingCount();
						
						// If appName does not exist in the master map, let's create its data now, keying to the current date
						Map<String, ArrayList<String[]>> appDataMap = new TreeMap<String, ArrayList<String[]>>();

						// Create appData ArrayList. Note ArrayList is used to support multiple brands and platforms per appName
						// ArrayList<Brand, Platform, DL count, update count, version, userRatingCountForCurrentVersion, averageUserRatingForCurrentVersion, averageUserRating, userRatingCount>
						ArrayList<String[]> appData = new ArrayList<String[]>();
						appData.add(new String[] {brandName, platformName, Integer.toString(dlCount), 
								Integer.toString(upCount), version, userRatingCountForCurrentVersion, averageUserRatingForCurrentVersion, averageUserRating, userRatingCount});

						// Convert current date to Linux stamp since 1970 and use it as key in appDataMap, adding appData to it					
						appDataMap.put(currentDate, appData);
						
						// Add appDataMap to the master map using the appName as key
						masterMap.put(appName, appDataMap);
					
					} else {
						
						Map<String, ArrayList<String[]>> appDataMap = masterMap.get(appName);
						
						//Check to see if currentDate is already in the map as key
						if (appDataMap.containsKey(currentDate) == false) {

							// First go fetch the app's iTune data and populate them in method scope
							AppDataResults results = getJSONFromItune(appId);
							version = results.getVersion();
							userRatingCountForCurrentVersion = results.getUserRatingCountForCurrentVersion();
							averageUserRatingForCurrentVersion = results.getAverageUserRatingForCurrentVersion();
							averageUserRating = results.getAverageUserRating();
							userRatingCount = results.getUserRatingCount();							
							
							ArrayList<String[]> appData = new ArrayList<String[]>();
							appData.add(new String[] {brandName, platformName, Integer.toString(dlCount), Integer.toString(upCount), 
									version, userRatingCountForCurrentVersion, averageUserRatingForCurrentVersion, averageUserRating, userRatingCount});

							appDataMap.put(currentDate, appData);

							// Add appDataMap to the master map using the appName as key
							masterMap.put(appName, appDataMap);
							
							
						} else {

							// appData is only 1 dimensionally deep for now
							ArrayList<String[]> appData = appDataMap.get(currentDate);
							
							String dl = appData.get(0)[2];
							String up = appData.get(0)[3];

							dlCount += Integer.parseInt(dl);
							upCount += Integer.parseInt(up);
							
							// Replace what is at Index with the new String[]
							appData.set(0, new String[] {brandName, platformName, Integer.toString(dlCount), Integer.toString(upCount), 
									appData.get(0)[4], appData.get(0)[5], appData.get(0)[6], appData.get(0)[7], appData.get(0)[8]});
							
							appDataMap.put(currentDate, appData);

							// Add appDataMap to the master map using the appName as key
							masterMap.put(appName, appDataMap);

						}
						
					}				
				}
			} // end while

		} catch(FileNotFoundException fN) {
			fN.printStackTrace();
		} catch(IOException e) {
			System.out.println(e);
		}
		
	}
		
	private String printData() {
		
		String dataText = "";
			
		//Let's print master map 
		if (!masterMap.isEmpty()) {
			for (Map.Entry<String, Map<String, ArrayList<String[]>>> masterEntry : masterMap.entrySet()) {
				
				dataText += "\n\n" + masterEntry.getKey();							
				dataText += "\n\tDate\tBrand\tPlatform\tDownloads\tUpdates\tVersion\tUserRatingCountForCurrentVersion\tAverageUserRatingForCurrentVersion\tAverageUserRating\tUserRatingCount"; 
				
				Map<String, ArrayList<String[]>> dataEntryMap = masterEntry.getValue();
				
				for (Map.Entry<String, ArrayList<String[]>>  dataEntry : dataEntryMap.entrySet()) {
					
					dataText += "\n\t" + dataEntry.getKey();
				
					ArrayList<String[]> dataList = dataEntry.getValue(); 					
					for (String[] sa : dataList) {
						for (int j=0; j<sa.length; j++)
							dataText += "\t" + sa[j];		
					}				
				}
			}
			
			System.out.println(dataText);
			
		}
		
		return dataText;
	}
	
	private void writeFile(String text) {
		
		try {
			
			// Delete file is already exist
			File file = new File(attachmentPath);			
			if (file.exists()) {
				file.delete();
			} 
			
			// Create file
			FileWriter fstream = new FileWriter(attachmentPath);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(text);
			
			//Close the output stream
			out.close();
			
		} catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}		
	}
	
	
	private String getPlatform(String productTypeId) {
		
		String platform = null;
		
		if (productTypeId.equalsIgnoreCase("1")) {
			platform = "iPhone and iPod Touch, iOS";
		} else if (productTypeId.equalsIgnoreCase("7")) {
			platform = "iPhone and iPod Touch, iOS";
		} else if (productTypeId.equalsIgnoreCase("1F")) {
			platform = "Universal, iOS";
		} else if (productTypeId.equalsIgnoreCase("7F")) {
			platform = "Universal, iOS";
		} else if (productTypeId.equalsIgnoreCase("1T")) {
			platform = "iPad, iOS";
		} else if (productTypeId.equalsIgnoreCase("7T")) {
			platform = "iPad, iOS";
		} else if (productTypeId.equalsIgnoreCase("F1")) {
			platform = "Mac OS";
		} else if (productTypeId.equalsIgnoreCase("F7")) {
			platform = "Mac OS";
		} 
		
		return platform;
	}
	
	private String getType(String productTypeId) {
		
		String type = null;
		
		if (productTypeId.equalsIgnoreCase("1")) {
			type = "dl";
		} else if (productTypeId.equalsIgnoreCase("7")) {
			type = "up";
		} else if (productTypeId.equalsIgnoreCase("1F")) {
			type = "dl";
		} else if (productTypeId.equalsIgnoreCase("7F")) {
			type = "up";
		} else if (productTypeId.equalsIgnoreCase("1T")) {
			type = "dl";
		} else if (productTypeId.equalsIgnoreCase("7T")) {
			type = "up";
		} else if (productTypeId.equalsIgnoreCase("F1")) {
			type = "dl";
		} else if (productTypeId.equalsIgnoreCase("F7")) {
			type = "up";
		} 
		
		return type;
	}
	
	private void sendEmail() {
		
		final String username = "jsu900";
		final String fromEmail = "joseph.su@nbcuni.com";

		String[] emails = {
						"joseph.su@nbcuni.com",
						"charles.burden@nbcuni.com"
						};
 
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", "smtp.gmail.com");
		props.put("mail.smtp.port", "587");
 
		Session session = Session.getInstance(props,
		  new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, "jcs6410");
			}
		});
 
		for (int i=0; i<emails.length; i++) {
			
			try {
				 
				Message message = new MimeMessage(session);
				message.setFrom(new InternetAddress(fromEmail));
				message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(emails[i]));
				//message.setSubject("iTune analytic data");
				
				// Create the message part
			    MimeBodyPart messageBodyPart = new MimeBodyPart();
			  
			    // Fill message
			    messageBodyPart.setText("Please see the attached. This is a test for Charles.");
				
			    Multipart multipart = new MimeMultipart();
			    multipart.addBodyPart(messageBodyPart);
			    
			    // Attachment
			    messageBodyPart = new MimeBodyPart();
			    DataSource source = new FileDataSource(attachmentPath);
			    messageBodyPart.setDataHandler(new DataHandler(source));
			    messageBodyPart.setFileName(attachmentPath);
			    multipart.addBodyPart(messageBodyPart);

			    // Put parts in message
			    message.setContent(multipart);

			    // Send
			    Transport.send(message);

				System.out.println("Done");	

			} catch (MessagingException e) {
				throw new RuntimeException(e);
			}
			
		}

	}
	
	private AppDataResults getJSONFromItune(String appId) throws Throwable {
			
		String json = "";
		
        URL url = new URL(iTuneURI + appId);
        
        BufferedReader in = new BufferedReader(
                new InputStreamReader(url.openStream()));
        
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            json += inputLine;
        }
        in.close();

        AppData data = new Gson().fromJson(json, AppData.class);
		
        return data.getResults();
	}

	
	/* Pulling this from iTune for each application
	 * 
	 */
	public class AppData {
		
		private AppDataResults[] results;
		private int resultCount;
		
		public AppDataResults getResults() {
			
			AppDataResults retVal = null;
			
			if (resultCount == 1) {
				retVal = results[0];
			} else {
				retVal = new AppDataResults(null, null, null, null, null, null, null, null, null);
			}
			
			return retVal;
		}
		
	}
	
	public class AppDataResults {
		
		private String version = null;
		private String userRatingCountForCurrentVersion = null;
		private String averageUserRatingForCurrentVersion = null;
		private String averageUserRating = null;
		private String userRatingCount = null;
		private String trackId = null;
		
		
		private String artistName = null;
		private String [] genres;		
		private String releaseDate = null;
		private String[] supportedDevices = null;
		
		private String trackName = null;
		public String getTrackName() {
			return trackName;
		}

		public void setTrackName(String trackName) {
			this.trackName = trackName;
		}

		
		public String getArtistName() {
			return artistName;
		}

		public void setArtistName(String artistName) {
			this.artistName = artistName;
		}

		
		public String getReleaseDate() {
			return releaseDate;
		}

		public void setReleaseDate(String releaseDate) {
			this.releaseDate = releaseDate;
		}

		
		public String[] getSupportedDevices() {
			return supportedDevices;
		}

		public void setSupportedDevices(String[] supportedDevices) {
			this.supportedDevices = supportedDevices;
		}
		

		public AppDataResults(String a, String b, String c, String d, String e, String f, String[] g, String h, String[] i) {
			this.version = a;
			this.userRatingCountForCurrentVersion = b;
			this.averageUserRatingForCurrentVersion = c;
			this.averageUserRating = d;
			this.userRatingCount = e;
			this.artistName = f;
			this.genres = g;		
			this.releaseDate = h;
			this.supportedDevices = i;
		}
		
		public String[] getGenres() {
			return genres;
		}

		public void setGenres(String[] genres) {
			this.genres = genres;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public String getUserRatingCountForCurrentVersion() {
			return userRatingCountForCurrentVersion;
		}

		public void setUserRatingCountForCurrentVersion(
				String userRatingCountForCurrentVersion) {
			this.userRatingCountForCurrentVersion = userRatingCountForCurrentVersion;
		}

		public String getAverageUserRatingForCurrentVersion() {
			return averageUserRatingForCurrentVersion;
		}

		public void setAverageUserRatingForCurrentVersion(
				String averageUserRatingForCurrentVersion) {
			this.averageUserRatingForCurrentVersion = averageUserRatingForCurrentVersion;
		}

		public String getAverageUserRating() {
			return averageUserRating;
		}

		public void setAverageUserRating(String averageUserRating) {
			this.averageUserRating = averageUserRating;
		}

		public String getUserRatingCount() {
			return userRatingCount;
		}

		public void setUserRatingCount(String userRatingCount) {
			this.userRatingCount = userRatingCount;
		}

		public String getTrackId() {
			return trackId;
		}

		public void setTrackId(String trackId) {
			this.trackId = trackId;
		}

		public String toString() {
	        return String.format("trackId:%s,version:%s,userRatingCountForCurrentVersion:%s,averageUserRatingForCurrentVersion:%s,averageUserRating:%s,userRatingCount:%s", trackId, version, userRatingCountForCurrentVersion, averageUserRatingForCurrentVersion, averageUserRating, userRatingCount);			
		}
		
	}

	
	private void goFetchMyData(String[] account, String dateString) throws Throwable {
		
	    Object localObject1 = null;
	    Object localObject2 = null;
		String str1 = null;
		
	    if (null != dateString) {
	      str1 = dateString;
	    } else {
	      localObject1 = Calendar.getInstance();
	      localObject2 = new SimpleDateFormat("yyyyMMdd");
	      ((Calendar)localObject1).add(5, -1);

	      str1 = ((SimpleDateFormat)localObject2).format(((Calendar)localObject1).getTime()).toString();
	    }

	    String str2 = "USERNAME=" + URLEncoder.encode(account[0], "UTF-8");
	    str2 = str2 + "&PASSWORD=" + URLEncoder.encode(account[1], "UTF-8");
	    str2 = str2 + "&VNDNUMBER=" + URLEncoder.encode(account[2], "UTF-8");
	    str2 = str2 + "&TYPEOFREPORT=" + URLEncoder.encode(account[3], "UTF-8");
	    str2 = str2 + "&DATETYPE=" + URLEncoder.encode(account[4], "UTF-8");
	    str2 = str2 + "&REPORTTYPE=" + URLEncoder.encode(account[5], "UTF-8");
	    str2 = str2 + "&REPORTDATE=" + URLEncoder.encode(str1, "UTF-8");
	  
	    System.out.println("str = " + str2);
	    
	    try
	    {
	      localObject2 = new URL("https://reportingitc.apple.com/autoingestion.tft?");

	      localObject1 = (HttpsURLConnection)((URL)localObject2).openConnection();

	      ((HttpsURLConnection)localObject1).setRequestMethod("POST");
	      ((HttpsURLConnection)localObject1).setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

	      ((HttpsURLConnection)localObject1).setDoOutput(true);
	      OutputStreamWriter localOutputStreamWriter = new OutputStreamWriter(((HttpsURLConnection)localObject1).getOutputStream());
	      localOutputStreamWriter.write(str2);
	      localOutputStreamWriter.flush();
	      localOutputStreamWriter.close();

	      if (((HttpsURLConnection)localObject1).getHeaderField("ERRORMSG") != null)
	        System.out.println(((HttpsURLConnection)localObject1).getHeaderField("ERRORMSG"));
	      else if (((HttpsURLConnection)localObject1).getHeaderField("filename") != null)
	        getFile((HttpsURLConnection)localObject1);
	    }
	    catch (Exception localException)
	    {
	      localException.printStackTrace();
	      System.out.println("The report you requested is not available at this time.  Please try again in a few minutes.");
	    }
	    finally
	    {
	      if (localObject1 != null) {
	        ((HttpsURLConnection)localObject1).disconnect();
	        localObject1 = null;
	      }
	    }	
	}
	
	
	public void writeExcelFile() throws Throwable {

		WriteExcel write = new WriteExcel(this.getMasterMap());
		write.setOutputFiles(outputDataPath);
		write.write(this.getDateList());
	}
	
}	
	


	
	
	

