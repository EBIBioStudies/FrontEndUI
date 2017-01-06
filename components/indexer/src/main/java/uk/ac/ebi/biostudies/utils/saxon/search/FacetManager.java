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


    private static TaxonomyWriter TAXONOMY_WRITER;
    public final static FacetsConfig FACET_CONFIG = new FacetsConfig();
    public static TaxonomyReader TAXO_READER;
    public static String TAXO_PATH = null;
    private static String FACET_RESULTS;
    private static Map <String, String> AllDims = new HashMap<>();
    public static void init(){
        Directory taxoDirectory = null;
        try {
            taxoDirectory = FSDirectory.open(new File(TAXO_PATH).toPath());
            TAXONOMY_WRITER = new DirectoryTaxonomyWriter(taxoDirectory, IndexWriterConfig.OpenMode.CREATE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static TaxonomyWriter getTaxonomyWriter(){
        if(TAXONOMY_WRITER==null)
            init();
        return TAXONOMY_WRITER;

    }

    public static TaxonomyReader getTaxonomyReader(){
        Directory taxoDirectory = null;
        try {
            taxoDirectory = FSDirectory.open(new File(TAXO_PATH).toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            TAXO_READER = new DirectoryTaxonomyReader(taxoDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  TAXO_READER;
    }

    public static void closeTaxonomy(){
        try {
            TAXONOMY_WRITER.close();
            TAXONOMY_WRITER = null;
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
            List <FacetResult>allResults = new ArrayList();
            allResults.add(organ);
            allResults.add(compound);
            allResults.add(tech);
            FacetManager.setFacetXmlFromFacetResults(allResults);
        }catch (Exception ex){
            logger.error("Problem in extracting facet counts", ex);
        }
    }

    public static String getFacetDim(String facetName){
        return AllDims.get(facetName);
    }

    private static String getFullName(String dim){
        if(dim.equalsIgnoreCase("tech"))
            return "Assay Technology Type";
        else return  dim;
    }

}
