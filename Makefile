all jar: xmuta.jar

clean:
	-rm *.class xmuta.jar *~

compile: xmuta.class

tgz:
	xargs < FILES tar cvzf xmuta.tar.gz
zip:
	xargs < FILES zip xmuta.zip

xmuta.jar: manifest xmuta.class
	jar cvfm $@ $< *.class

xmuta.class: xmuta.java
	javac -Xlint:unchecked $<

CS_NAME=checkstyle
CS_VERSION=5.6
CS_PKG=$(CS_NAME)-$(CS_VERSION)
CS_TGZ=$(CS_PKG)-bin.tar.gz
CS_JAR=$(CS_PKG)-all.jar
SF_URL=http://downloads.sourceforge.net/project
CS_URL=$(SF_URL)/$(CS_NAME)/$(CS_NAME)/$(CS_VERSION)/$(CS_TGZ)

CHECK=java -cp $(CS_JAR) com.puppycrawl.tools.checkstyle.Main

check: $(CS_JAR)
	$(CHECK) -c checkstyle.xml xmuta.java

$(CS_JAR): 
	wget $(CS_URL)
	tar xzf $(CS_TGZ) $(CS_PKG)/$@
	mv $(CS_PKG)/$@ $@
	rm -r $(CS_PKG)
	rm $(CS_TGZ)
