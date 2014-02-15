import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import jxl.CellView;
import jxl.Workbook;
import jxl.WorkbookSettings;
import jxl.format.UnderlineStyle;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

public class WriteExcel {


	private WritableCellFormat timesBoldUnderline;
	private WritableCellFormat times;
	private String inputFile;
	private ArrayList<String> dateList = null;
	private MobileAnalytics analytics;
	private Map<String, Map<String, ArrayList<String[]>>> masterMap;
	
	public WriteExcel(Map<String, Map<String, ArrayList<String[]>>> map) {
		this.masterMap = map;
	}
	
	public void setOutputFiles(String inputFile) {
		this.inputFile = inputFile;
	}

	public void write(ArrayList<String> dateList) throws IOException, WriteException {
		
		this.dateList = dateList;
		int numDates = dateList.size();
		
		System.out.println("numDate = " + numDates);
		
		File file = new File(inputFile);
		WorkbookSettings wbSettings = new WorkbookSettings();

		wbSettings.setLocale(new Locale("en", "EN"));

		WritableWorkbook workbook = Workbook.createWorkbook(file, wbSettings);
		workbook.createSheet("Mobile Analytics", 0);
		WritableSheet excelSheet = workbook.getSheet(0);
		
		createLabels(excelSheet, numDates);
		createContent(excelSheet, numDates);		
		workbook.write();		
		workbook.close();
	}

	
	public void createLabels(WritableSheet sheet, int numDates) throws WriteException {
		
		// Lets create a times font
		WritableFont times12pt = new WritableFont(WritableFont.TIMES, 12);
		// Define the cell format
		times = new WritableCellFormat(times12pt);
		
		// Lets automatically wrap the cells
		times.setWrap(false);

		// Create create a bold font with underlines
		WritableFont times10ptBoldUnderline = new WritableFont(
				WritableFont.TIMES, 12, WritableFont.BOLD, false,
				UnderlineStyle.SINGLE);
		timesBoldUnderline = new WritableCellFormat(times10ptBoldUnderline);
		
		// Lets automatically wrap the cells
		timesBoldUnderline.setWrap(false);

		CellView cv = new CellView();
		cv.setFormat(times);
		cv.setFormat(timesBoldUnderline);
		cv.setAutosize(true);
		

		// Write a few headers
		addCaption(sheet, 0, 1, "App Name");
		addCaption(sheet, 1, 1, "Brand");
		addCaption(sheet, 2, 1, "Platform");
		addCaption(sheet, 3, 0, "Downloads");
		addCaption(sheet, numDates+3, 0, "Updates");

		Iterator itr = dateList.iterator();
		int colIndex = 3;
		while (itr.hasNext()) {
			String date = (String)itr.next();
			addCaption(sheet, colIndex, 1, date);
			addCaption(sheet, numDates+colIndex, 1, date);
			colIndex++;
		}
		
		addCaption(sheet, 2*numDates+3, 1, "Version");
		addCaption(sheet, 2*numDates+4, 1, "UserRatingCountForCurrentVersion");
		addCaption(sheet, 2*numDates+5, 1, "AverageUserRatingForCurrentVersion");
		addCaption(sheet, 2*numDates+6, 1, "AverageUserRating");
		addCaption(sheet, 2*numDates+7, 1, "UserRatingCount");
	

	}

	public void createContent(WritableSheet sheet, int numDates) throws WriteException, RowsExceededException {

		//Let's print master map 
		if (!masterMap.isEmpty()) {

			int recordIndex = 2;
			int dateIndex = 3;
			
			for (Map.Entry<String, Map<String, ArrayList<String[]>>> masterEntry : masterMap.entrySet()) {
				
				// App name
				addLabel(sheet, 0, recordIndex, masterEntry.getKey());
				
				//dataText += "\n\tDate\tBrand\tPlatform\tDownloads\tUpdates\tVersion\tUserRatingCountForCurrentVersion\tAverageUserRatingForCurrentVersion\tAverageUserRating\tUserRatingCount"; 
				
				Map<String, ArrayList<String[]>> dataEntryMap = masterEntry.getValue();
				
				for (Map.Entry<String, ArrayList<String[]>>  dataEntry : dataEntryMap.entrySet()) {
									
					// All data associated with each Date object
					ArrayList<String[]> dataList = dataEntry.getValue(); 
					int columnIndex = 2 + 2*numDates + 1; 
					for (String[] sa : dataList) { // there should only be 1 vertical entry in dataList

						// Brand & Platform
						addLabel(sheet, 1, recordIndex, sa[0]);								
						addLabel(sheet, 2, recordIndex, sa[1]);	
						
						// Downloads & Updates
						addLabel(sheet, dateIndex, recordIndex, sa[2]);
						addLabel(sheet, dateIndex+numDates, recordIndex, sa[3]);					
						dateIndex++;
						

						for (int j=4; j<sa.length; j++)

							addLabel(sheet, columnIndex++, recordIndex, sa[j]);								
					}
					
				}
				
				// recordIndex only gets incremented every time we are ready to print the next app name
				recordIndex++;
				
				// Resetting dates
				dateIndex = 3;
				
			} // end masterMap
		} // end if
		
	}

	private void addCaption(WritableSheet sheet, int column, int row, String s)
			throws RowsExceededException, WriteException {
		
		Label label;
		label = new Label(column, row, s, timesBoldUnderline);
		sheet.setColumnView(0, 35);
		sheet.setColumnView(1, 20);
		sheet.setColumnView(2, 25);
		sheet.setColumnView(3, 12);
		sheet.addCell(label);
	}

	private void addNumber(WritableSheet sheet, int column, int row, double d) throws WriteException, RowsExceededException {
		Number number;
		number = new Number(column, row, d, times);
		sheet.addCell(number);
	}

	private void addLabel(WritableSheet sheet, int column, int row, String s)
			throws WriteException, RowsExceededException {
		Label label;
		label = new Label(column, row, s, times);
		sheet.addCell(label);
	}
}
