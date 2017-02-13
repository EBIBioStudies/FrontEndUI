package uk.ac.ebi.biostudies.utils.saxon.search;

/**
 * Created by ehsan on 24/11/2016.
 */
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.facet.*;
import  org.apache.lucene.facet.taxonomy.*;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FacetManager {

    private final static Logger logger = LoggerFactory.getLogger(FacetManager.class);

    public static String[] COMPOUNDS = {"idarubicin","doxorubicin", "dmso (0.1%)", "epirubicin", "untreated", "daunorubicin", "idarubicin"
    ,"dmso", "dmso (fluctuating)"};
    public static Map<String, String> ALL_COMPOUNDS = new HashMap<>();
    private static TaxonomyWriter TAXONOMY_WRITER;
    public final static FacetsConfig FACET_CONFIG = new FacetsConfig();
    private static TaxonomyReader TAXO_READER;
    private static String TAXO_PATH = null;
    private static String FACET_RESULTS;
    private static Map <String, String> AllDims = new HashMap<>();
    public static synchronized void init(){
        FACET_CONFIG.setMultiValued("compound", true);
        for(String cmp:COMPOUNDS){
            ALL_COMPOUNDS.put(cmp, cmp);
        }
        Directory taxoDirectory = null;
        try {
            taxoDirectory = FSDirectory.open(new File(TAXO_PATH).toPath());
            TAXONOMY_WRITER = new DirectoryTaxonomyWriter(taxoDirectory, IndexWriterConfig.OpenMode.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized TaxonomyWriter getTaxonomyWriter(){
        if(TAXONOMY_WRITER==null)
            init();
        return TAXONOMY_WRITER;

    }

    public static TaxonomyReader getTaxonomyReader(){
        if(TAXO_READER!=null)
            return TAXO_READER;
        return  createTaxoReader();
    }

    public static TaxonomyReader createTaxoReader(){
        try {
            if(TAXO_READER == null){
                Directory taxoDirectory = FSDirectory.open(new File(TAXO_PATH).toPath());
                TAXO_READER = new DirectoryTaxonomyReader(taxoDirectory);
            }
            TaxonomyReader tempReader;
            if((tempReader=TaxonomyReader.openIfChanged(TAXO_READER))!= null)
                TAXO_READER = tempReader;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  TAXO_READER;
    }

    public static void commitTaxonomy(){
        try {
            TAXONOMY_WRITER.commit();
            FACET_RESULTS = null;
        } catch (IOException e) {
            logger.error("Closing taxonomy writer failed! ",e);
        }
    }

    public static void setFacetXmlFromFacetResults(List<FacetResult> facetResults){
        StringBuilder stringBuilder = new StringBuilder("<facets>");
        if(facetResults==null){
            stringBuilder.append("</facets>");
            FACET_RESULTS = stringBuilder.toString();
        }
        String fctName;
        int value;
        int NAFreq = 0;
        for(FacetResult fcr:facetResults) {
            if(fcr==null)
                continue;

            for (LabelAndValue lbv : fcr.labelValues) {
                fctName = lbv.label;
                value = lbv.value.intValue();
                if(value==0)
                    continue;
                if(fctName.equalsIgnoreCase("n/a")){
                    NAFreq = value;
                    continue;
                }
                addToStringBuilder(stringBuilder, fcr.dim, fctName, value);
            }
            addToStringBuilder(stringBuilder, fcr.dim, "n/a", NAFreq);
        }
        stringBuilder.append("</facets>");
        FACET_RESULTS = stringBuilder.toString();
    }

    private static void addToStringBuilder(StringBuilder stringBuilder, String dim, String fctName, int value){
        stringBuilder.append("<facet>");
        stringBuilder.append("<dim>");
        stringBuilder.append(getFullName(dim));
        stringBuilder.append("</dim>");
        stringBuilder.append("<label>");
        stringBuilder.append(fctName);
        stringBuilder.append("</label>");
        stringBuilder.append("<value>");
        stringBuilder.append(value);
        stringBuilder.append("</value>");
        stringBuilder.append("</facet>");
        AllDims.put(fctName, dim);
    }


    public static String getFacetResults(){
        return  FACET_RESULTS;
    }

    public static void setHecatosFacets(IndexSearcher searcher, Query query) {
        try {
            FacetsCollector facetsCollector = new FacetsCollector();
            FacetsCollector.search(searcher, query, 10, facetsCollector);
            //                searcher.search(new MatchAllDocsQuery(), facetsCollector); new MatchAllDocsQuery()
            List<FacetResult> results = new ArrayList<>();
            Facets facets = new FastTaxonomyFacetCounts(FacetManager.getTaxonomyReader(), FacetManager.FACET_CONFIG, facetsCollector);
            FacetResult organ = facets.getTopChildren(20, "organ");
            FacetResult compound = facets.getTopChildren(20, "compound");
            FacetResult tech = facets.getTopChildren(20, "tech");
            FacetResult dataType = facets.getTopChildren(20, "datatype");
            FacetResult rawProcessed = facets.getTopChildren(20, "rawprocessed");
            List <FacetResult>allResults = new ArrayList();
            allResults.add(organ);
            allResults.add(compound);
            allResults.add(tech);
            allResults.add(dataType);
            allResults.add(rawProcessed);
            FacetManager.setFacetXmlFromFacetResults(allResults);
        }catch (Exception ex){
            logger.error("Problem in extracting facet counts", ex);
        }
    }

    public static String getFacetDim(String facetName){
        if(facetName.equalsIgnoreCase("n/a1"))
            return "tech";
        else
        if(facetName.equalsIgnoreCase("n/a2"))
            return "compound";
        else
        if(facetName.equalsIgnoreCase("n/a3"))
            return "organ";
        else
        if(facetName.equalsIgnoreCase("n/a4"))
            return "datatype";
        else
        if(facetName.equalsIgnoreCase("n/a5"))
            return "rawprocessed";
        else
        return AllDims.get(facetName);
    }

    private static String getFullName(String dim){
        if(dim.equalsIgnoreCase("tech"))
            return "Assay Technology Type";
        if(dim.equalsIgnoreCase("datatype"))
            return "Data Type";
        if(dim.equalsIgnoreCase("rawprocessed"))
            return "Raw/Processed";
        else return  dim;
    }

}
