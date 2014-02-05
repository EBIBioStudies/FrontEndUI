package uk.ac.ebi.arrayexpress.utils.search;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.NullFragmenter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.utils.RegexHelper;
import uk.ac.ebi.arrayexpress.utils.saxon.search.IQueryHighlighter;
import uk.ac.ebi.arrayexpress.utils.saxon.search.IndexEnvironment;
import uk.ac.ebi.arrayexpress.utils.saxon.search.QueryInfo;

/*
 * Copyright 2009-2014 European Molecular Biology Laboratory
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

public class EFOExpandedHighlighter implements IQueryHighlighter
{
    // logging machinery
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private IndexEnvironment env;

    private static final String HIT_OPEN_MARK = "\u00ab";
    private static final String HIT_CLOSE_MARK = "\u00bb";
    private static final String SYN_OPEN_MARK = "\u2039";
    private static final String SYN_CLOSE_MARK = "\u203a";
    private static final String EFO_OPEN_MARK = "\u2035";
    private static final String EFO_CLOSE_MARK = "\u2032";

    private final RegexHelper SYN_AND_HIT_REGEX = new RegexHelper(HIT_OPEN_MARK + SYN_OPEN_MARK + "([^" + SYN_CLOSE_MARK + HIT_CLOSE_MARK + "]+)" + SYN_CLOSE_MARK + HIT_CLOSE_MARK, "g");
    private final RegexHelper EFO_AND_SYN_REGEX = new RegexHelper(SYN_OPEN_MARK + EFO_OPEN_MARK + "([^" + EFO_CLOSE_MARK + SYN_CLOSE_MARK + "]+)" + EFO_CLOSE_MARK + SYN_CLOSE_MARK, "g");
    private final RegexHelper EFO_AND_HIT_REGEX = new RegexHelper(HIT_OPEN_MARK + EFO_OPEN_MARK + "([^" + EFO_CLOSE_MARK + HIT_CLOSE_MARK + "]+)" + EFO_CLOSE_MARK + HIT_CLOSE_MARK, "g");

    public IQueryHighlighter setEnvironment( IndexEnvironment env )
    {
        this.env = env;
        return this;
    }

    public String highlightQuery( QueryInfo info, String fieldName, String text )
    {
        EFOExpandableQueryInfo queryInfo = null;

        if (info instanceof EFOExpandableQueryInfo) {
            queryInfo = (EFOExpandableQueryInfo)info;
        }

        if (null == queryInfo) {
            return doHighlightQuery(info.getQuery(), fieldName, text, HIT_OPEN_MARK, HIT_CLOSE_MARK);
        } else {
            String result = doHighlightQuery(queryInfo.getOriginalQuery(), fieldName, text, HIT_OPEN_MARK, HIT_CLOSE_MARK);
            result = doHighlightQuery(queryInfo.getSynonymPartQuery(), fieldName, result, SYN_OPEN_MARK, SYN_CLOSE_MARK);
            result = doHighlightQuery(queryInfo.getEfoExpansionPartQuery(), fieldName, result, EFO_OPEN_MARK, EFO_CLOSE_MARK);

            result = EFO_AND_SYN_REGEX.replace(result, SYN_OPEN_MARK + "$1" + SYN_CLOSE_MARK);
            result = SYN_AND_HIT_REGEX.replace(result, HIT_OPEN_MARK + "$1" + HIT_CLOSE_MARK);
            result = EFO_AND_HIT_REGEX.replace(result, HIT_OPEN_MARK + "$1" + HIT_CLOSE_MARK);

            return result;
        }
    }

    private String doHighlightQuery( Query query, String fieldName, String text, String openMark, String closeMark )
    {
        try {
            SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter(openMark, closeMark);
            Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query, fieldName, this.env.defaultField));
            highlighter.setTextFragmenter(new NullFragmenter());

            String str = highlighter.getBestFragment(this.env.indexAnalyzer, "".equals(fieldName) ? this.env.defaultField : fieldName, text);

            return null != str ? str : text;
        } catch (Exception x) {
            logger.error("Caught an exception:", x);
        }
        return text;

    }
}
