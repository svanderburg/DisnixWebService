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

MANUAL_SRCS = index.xml introduction.xml

MANS = disnix-soap-client.1

all: index.html index.pdf man

version.txt:
	echo -n $(VERSION) > version.txt

manual.is-valid: $(MANUAL_SRCS) version.txt
	$(XMLLINT) --noout --nonet --xinclude --noxincludenode --relaxng $(docbookrng)/docbook.rng $<
	touch $@

man $(MANS): $(MANUAL_SRCS) manual.is-valid
	$(XSLTPROC) --nonet --xinclude $(docbookxsl)/manpages/docbook.xsl index.xml

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
