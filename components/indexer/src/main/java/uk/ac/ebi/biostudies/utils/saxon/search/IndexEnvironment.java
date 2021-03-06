/*
 * Copyright 2009-2016 European Molecular Biology Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package uk.ac.ebi.biostudies.utils.saxon.search;

import net.sf.saxon.om.NodeInfo;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexEnvironment {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static Configuration configuration = null;

    public HierarchicalConfiguration indexConfig;

    // index configuration, parsed
    public String indexId;
    public Directory indexDirectory;
    public PerFieldAnalyzerWrapper indexAnalyzer;
    public String defaultField;
    public String idField;
    public int searchSnippetFragmentSize;
    public SpellChecker spellChecker;
    public static HashMap<String,SpellChecker> spellCheckerMap = new HashMap<>();

    // index document xpath
    public String indexDocumentPath;

    // field information, parsed
    public static class FieldInfo {
        public String name;
        public String title;
        public String type;
        public String path;
        public boolean shouldAnalyze;
        public String analyzer;
        public boolean shouldStore;
        public boolean shouldEscape;
        public boolean shouldExpand;
        public String docValueType;
        public float boost;

        public FieldInfo(HierarchicalConfiguration fieldConfig) {
            this.name = fieldConfig.getString("[@name]");
            this.title = fieldConfig.containsKey("[@title]") ? fieldConfig.getString("[@title]") : null;
            this.type = fieldConfig.getString("[@type]");
            this.path = fieldConfig.getString("[@path]");
            this.shouldStore = fieldConfig.getBoolean("[@store]", true);

            if ("string".equals(this.type)) {
                this.shouldAnalyze = fieldConfig.getBoolean("[@analyze]");
                this.analyzer = fieldConfig.getString("[@analyzer]");
                this.docValueType =  fieldConfig.getString("[@docValueType]", "none");
                this.shouldEscape = fieldConfig.getBoolean("[@escape]");
                this.shouldExpand = fieldConfig.getBoolean("[@expand]");
                this.boost =  fieldConfig.getFloat("[@boost]", 1.0f);
            }
        }
    }
    
    public Map<String, FieldInfo> fields;

    // document info
    public String documentHashCode;
    //public List<NodeInfo> documentNodes;


    public static Configuration getConfiguration() {
        if (configuration==null) {
            XMLConfiguration.setDefaultListDelimiter('\uffff');
            XMLConfiguration xmlConfig = new XMLConfiguration();
            InputStream prefStream = Indexer.class.getResourceAsStream("/indices.xml");
            try {
                xmlConfig.load(prefStream);
            } catch (ConfigurationException e) {
                e.printStackTrace();
            }
            configuration = new Configuration(xmlConfig);
        }
        return configuration;
    }

    public IndexEnvironment(String indexId) {
        this.indexConfig = getConfiguration().getIndexConfig(indexId);
        populateIndexConfiguration();
    }


    public void setDocumentInfo(String documentHashCode, List<NodeInfo> documentNodes) {
        this.documentHashCode = documentHashCode;
        //this.documentNodes = documentNodes;
    }

    private void populateIndexConfiguration() {
        try {
            this.indexId = this.indexConfig.getString("[@id]");

            String indexBaseLocation = this.indexConfig.getString("[@location]");
            FacetManager.TAXO_PATH = this.indexConfig.getString("[@taxonomy]");
            this.indexDirectory = FSDirectory.open(new File(indexBaseLocation, this.indexId).toPath());
            String indexAnalyzer = this.indexConfig.getString("[@defaultAnalyzer]");
            Analyzer a = (Analyzer) Class.forName(indexAnalyzer).newInstance();

            this.indexDocumentPath = indexConfig.getString("document[@path]");

            this.defaultField = indexConfig.getString("document[@defaultField]");

            this.idField = indexConfig.getString("document[@idField]", defaultField);

            this.searchSnippetFragmentSize = indexConfig.getInt("[@searchSnippetFragmentSize]", 256);

            List fieldsConfig = indexConfig.configurationsAt("document.field");

            this.fields = new HashMap<>();
            Map<String, Analyzer> fieldAnalyzers = new HashMap<>();

            for (Object fieldConfig : fieldsConfig) {
                FieldInfo fieldInfo = new FieldInfo((HierarchicalConfiguration) fieldConfig);
                fields.put(fieldInfo.name, fieldInfo);
                if (null != fieldInfo.analyzer) {
                    Analyzer fa = (Analyzer) Class.forName(fieldInfo.analyzer).newInstance();
                    fieldAnalyzers.put(fieldInfo.name, fa);
                }
            }

            this.indexAnalyzer = new PerFieldAnalyzerWrapper(a, fieldAnalyzers);
            this.spellChecker = getSpellChecker(this.indexId,this.indexDirectory,this.defaultField,this.indexAnalyzer);

        } catch (Exception x) {
            logger.error("Caught an exception:", x);
        }
    }

    private static SpellChecker getSpellChecker(String indexId, Directory indexDirectory, String defaultField, PerFieldAnalyzerWrapper indexAnalyzer) throws Exception{
        if (!spellCheckerMap.containsKey(indexId)) {
            SpellChecker spellChecker = new SpellChecker(indexDirectory);
            try (IndexReader reader = DirectoryReader.open(indexDirectory)){
                spellChecker.indexDictionary(
                        new LuceneDictionary(reader,defaultField),
                        new IndexWriterConfig(indexAnalyzer),
                        false
                );
                spellCheckerMap.put(indexId, spellChecker);
            }
        }
        return spellCheckerMap.get(indexId);
    }
    public boolean doesFieldExist(String fieldName) {
        return fields.containsKey(fieldName);
    }



}
