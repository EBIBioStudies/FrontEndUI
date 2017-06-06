package uk.ac.ebi.biostudies.components;

import net.sf.saxon.om.NodeInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biostudies.utils.saxon.SaxonException;
import uk.ac.ebi.biostudies.utils.saxon.search.FacetManager;
import uk.ac.ebi.biostudies.utils.saxon.search.Indexer;
import uk.ac.ebi.biostudies.utils.saxon.search.IndexerException;

import javax.xml.stream.*;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import java.io.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Created by ehsan on 23/01/2017.
 */
public class MultithreadStudyParser {

    private static final Logger logger = LoggerFactory.getLogger(MultithreadStudyParser.class);
    private static ExecutorService Executor_Service;

    // Updates the index one study at a time
    public synchronized static void updateFromXMLFile(File originalXMLFile, boolean deleteFileAfterProcessing, boolean makeCopy, String sourceLocation, Indexer indexer, SaxonEngine saxon, Autocompletion autocompletion ) throws IOException, InterruptedException, SaxonException, TransformerException, IndexerException, XMLStreamException {
        File xmlFile;
        if (makeCopy) {
            xmlFile = new File(System.getProperty("java.io.tmpdir"), originalXMLFile.getName());
            logger.info("Making a local copy  of {} at {}", originalXMLFile.getAbsolutePath(), xmlFile.getAbsolutePath());
            com.google.common.io.Files.copy(originalXMLFile, xmlFile);
        } else {
            xmlFile = originalXMLFile;
        }
        if (isNotBlank(sourceLocation)) {
            logger.info("Reload of experiment data from [{}] requested", sourceLocation);

            if (xmlFile.getName().equalsIgnoreCase("studies.xml")) {
                indexer.clearIndex(false);
            }
            try (InputStreamReader inputStreamReader= new InputStreamReader(new FileInputStream(xmlFile), "UTF-8")) {
                XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(inputStreamReader);
                XMLEventWriter writer = null;
                StringWriter buffer = null;
                XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
                XMLEvent xmlEvent = reader.nextEvent(); // Advance to statements element
                Executor_Service = Executors.newFixedThreadPool(16);
                DocParser.COUNT.set(0);
                do {
                    if (writer != null) writer.add(xmlEvent);
                    if (xmlEvent.isStartElement()
                            && "submission".equalsIgnoreCase(((StartElement) xmlEvent).getName().getLocalPart())) {
                        buffer = new StringWriter();
                        writer = outputFactory.createXMLEventWriter(buffer);
                        writer.add(xmlEvent);
                    } else if (xmlEvent.isEndElement()
                            && "submission".equalsIgnoreCase(((EndElement) xmlEvent).getName().getLocalPart())) {
                        writer.flush();
                        writer.close();
                        try {
                            DocParser docParser = new DocParser(indexer, saxon.buildDocument(buffer.toString()), false, saxon);
                            Executor_Service.execute(docParser);
                        }catch (Throwable throwable){
                            logger.error("Executor service stopped working properly",throwable);
                        }
                        writer = null;
                        buffer = null;
                    }
                    xmlEvent = reader.nextEvent();
                } while (!xmlEvent.isEndDocument());

                Executor_Service.shutdown();
                try {
                    Executor_Service.awaitTermination(5, TimeUnit.HOURS);
                    Executor_Service = null;
                } catch (InterruptedException e) {
                    logger.error("Problem happened in terminating executor service ", e);
                }
            }
            autocompletion.rebuild();
        }
        if (deleteFileAfterProcessing) {
            xmlFile.delete();
        }

        FacetManager.commitTaxonomy();
        boolean reOpenWriter = false;
        if (xmlFile.getName().equalsIgnoreCase("studies.xml"))
            reOpenWriter = true;
        indexer.commit(reOpenWriter);
        logger.info("finished indexing {} documents", DocParser.COUNT);
    }

}

class DocParser implements Runnable {//void processSubmissionQueue() throws XPathException, IndexerException, InterruptedException, IOException, SaxonException {

    Indexer indexer;
    boolean commit;
    Source xmlNode;
    SaxonEngine saxonEngine;
    private static final Logger logger = LoggerFactory.getLogger(DocParser.class);
    static AtomicInteger COUNT = new AtomicInteger(0);

    public DocParser(Indexer indexer, Source xmlNode, boolean commit, SaxonEngine saxonEngine) throws Throwable {
        this.indexer = indexer;
        this.xmlNode = xmlNode;
        this.commit = commit;
        this.saxonEngine = saxonEngine;
    }

    @Override
    public void run() {
        StringBuilder sb = new StringBuilder("<pmdocument><submissions>");
        String deleteAccession;
        int counter = 0;
        try {
            deleteAccession = (this.saxonEngine.evaluateXPath((NodeInfo) xmlNode, "submission/@delete").size() > 0)
                    ? this.saxonEngine.evaluateXPath((NodeInfo) xmlNode, "submission/@acc").get(0).getStringValue()
                    : null;
            if (deleteAccession == null) {
                counter = COUNT.incrementAndGet();
                sb.append(saxonEngine.serializeDocument(xmlNode, true));
            }

            sb.append("</submissions></pmdocument>");
            NodeInfo submissionDocument = saxonEngine.buildDocument(sb.toString());
            NodeInfo updateXml = this.saxonEngine.transform(
                    submissionDocument
                    , "preprocess-studies-xml.xsl"
                    , null
            );
            if (deleteAccession == null)
                indexer.index(updateXml, commit);
            else {
                indexer.delete(deleteAccession);
                logger.info("Delete submission {} ", deleteAccession);
            }
            if (counter % 1000 == 0)
                logger.info("Processed {} submissions", counter);


        } catch (Throwable throwable) {
            logger.error("Parsing problem",throwable);
        }
    }
}
