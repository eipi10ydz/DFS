BUILD=build/classes
NODE=$(BUILD)/node
SERVER=$(BUILD)/server
DIST=dist
NOT="notexist"
build : src/nat/*
    ifeq ($(shell if [ -d build ]; then echo '"exist"'; else echo '"notexist"'; fi;),$(NOT))
	mkdir build
	mkdir $(BUILD)
    endif
    ifeq ($(shell if [ -d $(NODE) ]; then echo '"exist"'; else echo '"notexist"'; fi;),$(NOT))
	mkdir $(NODE)
    endif
    ifeq ($(shell if [ -d $(SERVER) ]; then echo '"exist"'; else echo '"notexist"'; fi;),$(NOT))
	mkdir $(SERVER)
    endif
	javac -encoding UTF-8 -d $(NODE) -cp "lib/*" src/nat/node/*
	javac -encoding UTF-8 -d $(SERVER) -cp "lib/*" src/nat/server/*
jar : build
	cp -R $(NODE)/nodetest .
	cp -R $(SERVER)/server .
	cp META-INF/node/MANIFEST.MF .
	jar cvfm nodeTest.jar MANIFEST.MF nodetest/*
	cp META-INF/server/MANIFEST.MF .
	jar cvfm Server.jar MANIFEST.MF server/*
	rm -rf nodetest
	rm -rf server
	rm MANIFEST.MF
    ifeq ($(shell if [ -d $(DIST) ]; then echo '"exist"'; else echo '"notexist"'; fi;),$(NOT))
	mkdir $(DIST)
    endif
	mv nodeTest.jar dist
	mv Server.jar dist
	cp -R lib dist 

help :
	@echo '"make jar"' if you want to create jar'(jar is in dist)'
	@echo '"make build"' if you want to create classes'(classes are in build)'

.PHONY : clean

clean : 
	rm -rf build
	rm -rf dist
