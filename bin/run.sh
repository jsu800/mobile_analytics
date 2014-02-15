
rm *.class

javac -classpath .:../lib/jxl.jar:../lib/mail.jar:../lib/gson-2.1.jar ../src/WriteExcel.java ../src/MobileAnalytics.java 

mv ../src/*.class .

java -classpath .:../lib/jxl.jar:../lib/mail.jar:../lib/gson-2.1.jar MobileAnalytics mobileappsupport@nbcuni.com matt2237hew 80085056 Sales Daily Summary mobileapps@fandango.com F@nd@ng0mobile1 80070699 Sales Daily Summary 20120511 20120512 20120513 20120514 20120515
