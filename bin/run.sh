
rm *.class

javac -classpath .:../lib/jxl.jar:../lib/mail.jar:../lib/gson-2.1.jar ../src/WriteExcel.java ../src/MobileAnalytics.java 

mv ../src/*.class .

java -classpath .:../lib/jxl.jar:../lib/mail.jar:../lib/gson-2.1.jar MobileAnalytics [ACCOUNT/CREDENTIALS] 80085056 Sales Daily Summary [ACCOUNT/CREDENTIALS] 80070699 Sales Daily Summary 20120511 20120512 20120513 20120514 20120515
