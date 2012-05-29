%ECHO
set PATH=D:\devtools\jdk\jdk1.6.0_29\bin;%PATH%
set CLASSPATH=classes;lib\*
set INSTALLDIR=D:\install
mkdir classes
javac -d classes -sourcepath . *.java 