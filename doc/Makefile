all: index.html index.pdf

disnix-soap-client.1: ../scripts/disnix-soap-client
	env CLASSPATH=../bin:../jargs.jar help2man --output=$@ --no-info --name 'Provides access to the disnix-service through the SOAP protocol' --libtool "$(SHELL) ../scripts/disnix-soap-client"

disnix-soap-client.1.xml: disnix-soap-client.1
	doclifter -x $<
	sed -i -e "s|xml:id='disnixsoapclient1'|sec-disnix-soap-client|" \
	    -e "s|&copy;|(C)|" $<

XMLLINT = xmllint
XSLTPROC = xsltproc \
 --param section.autolabel 1 \
 --param section.label.includes.component.label 1 \
 --param html.stylesheet \'style.css\' \
 --param make.valid.html 1 \
 --param xref.with.number.and.title 1 \
 --param toc.section.depth 3 \
 --param admon.style \'\' \
 --param callout.graphics.extension \'.gif\' \
 --param contrib.inline.enabled 0

dblatex = dblatex

dblatex_opts = \
 -P doc.collab.show=0 \
 -P latex.output.revhistory=0

VERSION = $(shell cat ../version)

MANUAL_SRCS = index.xml introduction.xml disnix-soap-client.1.xml

version.txt:
	echo -n $(VERSION) > version.txt

manual.is-valid: $(MANUAL_SRCS) version.txt
	$(XMLLINT) --noout --nonet --xinclude --noxincludenode --relaxng $(docbookrng)/docbook.rng $<
	touch $@

index.html: $(MANUAL_SRCS) manual.is-valid
	$(XSLTPROC) --nonet --xinclude --output index.html \
	  $(docbookxsl)/html/docbook.xsl index.xml

index.pdf: $(MANUAL_SRCS) manual.is-valid
	if test "$(dblatex)" != ""; then \
		$(dblatex) $(dblatex_opts) index.xml; \
	else \
		echo "Please install dblatex and rerun configure."; \
		exit 1; \
	fi
