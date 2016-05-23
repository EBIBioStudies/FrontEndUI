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

package uk.ac.ebi.biostudies.utils.search;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import uk.ac.cam.ch.wwmm.oscar.document.Token;
import uk.ac.cam.ch.wwmm.oscartokeniser.TokenClassifier;
import uk.ac.cam.ch.wwmm.oscartokeniser.Tokeniser;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;

public final class Oscar4Analyzer extends Analyzer {
    static Tokeniser tokeniser = new Tokeniser(TokenClassifier.getDefaultInstance());

    String text;
    public static final Analyzer.ReuseStrategy NO_REUSE_STRATEGY = new Analyzer.ReuseStrategy() {
        public Analyzer.TokenStreamComponents getReusableComponents(Analyzer analyzer, String fieldName) {
            return null;
        }

        public void setReusableComponents(Analyzer analyzer, String fieldName, Analyzer.TokenStreamComponents components) {

        }
    };

    @Override
    protected Reader initReader(String fieldName, Reader reader) {
        try {
            text = IOUtils.toString(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new StringReader(text);
    }

    public Oscar4Analyzer(ReuseStrategy reuseStrategy) {
        super(reuseStrategy);
    }

    public Oscar4Analyzer() {
        //TODO:Keep checking how this can be handled more elegantly
        super(NO_REUSE_STRATEGY); // hack to avoid creating multiple analysers.
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer source = new Oscar4Tokenizer(text);
        TokenStream filter = new ASCIIFoldingFilter(source);
        return new TokenStreamComponents(source, filter);
    }

    @Override
    public void close() {
        super.close();
    }

    private class Oscar4Tokenizer extends Tokenizer {
        int pos;
        int length;
        List<Token> tokens;

        @Override
        public void end() throws IOException {
            offsetAttribute.setOffset(length, length);
            super.end();
            pos = 0;
        }

        @Override
        public void close() throws IOException {
            super.close();
            pos = 0;
        }

        protected CharTermAttribute charTermAttribute = addAttribute(CharTermAttribute.class);
        protected PositionIncrementAttribute positionIncrementAttribute =  addAttribute(PositionIncrementAttribute.class);
        protected PositionLengthAttribute positionLengthAttribute =  addAttribute(PositionLengthAttribute.class);
        protected OffsetAttribute offsetAttribute = addAttribute(OffsetAttribute.class);


        Oscar4Tokenizer(String text) {
            tokens = tokeniser.tokenise(text).getTokens();
            length = text.length();
            pos = 0;
        }

        @Override
        public void reset() throws IOException {
            super.reset();
            pos = 0;
        }

        public boolean incrementToken() throws IOException {
            if (pos >= tokens.size()) return false;
            Token token = tokens.get(pos);
            this.clearAttributes();
            this.charTermAttribute.setEmpty();
            this.charTermAttribute.resizeBuffer(token.getSurface().length());
            //this.charTermAttribute.setLength(token.getSurface().length());
            this.charTermAttribute.append(token.getSurface());
            this.positionIncrementAttribute.setPositionIncrement(1);
            this.positionLengthAttribute.setPositionLength(1);
            this.offsetAttribute.setOffset(token.getStart(), token.getEnd());
            pos++;
            return true;
        }
    }
}
