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

package uk.ac.ebi.biostudies.components;

import uk.ac.ebi.biostudies.app.ApplicationComponent;
import uk.ac.ebi.biostudies.utils.autocompletion.AutocompleteData;
import uk.ac.ebi.biostudies.utils.autocompletion.AutocompleteStore;
import uk.ac.ebi.arrayexpress.utils.efo.EFONode;
import uk.ac.ebi.arrayexpress.utils.efo.IEFO;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class Autocompletion extends ApplicationComponent {
    private AutocompleteStore autocompleteStore;

    private Studies studies;
    private SearchEngine search;
    private IEFO efo;

    public Autocompletion() {
    }

    @Override
    public void initialize() throws Exception {
        this.autocompleteStore = new AutocompleteStore();

        this.studies = getComponent(Studies.class);
        this.search = getComponent(SearchEngine.class);
    }

    @Override
    public void terminate() throws Exception {
    }

    private IEFO getEfo() {
        return this.efo;
    }

    public void setEfo(IEFO efo) {
        this.efo = efo;
    }

    public String getKeywords(String prefix, String field, Integer limit) {
        StringBuilder sb = new StringBuilder("");
        List<AutocompleteData> matches = getStore().findCompletions(prefix, field, limit);
        for (AutocompleteData match : matches) {
            sb.append(match.getText()).append('|').append(match.getDataType()).append('|').append(match.getData()).append('\n');
        }
        return sb.toString();
    }

    public String getEfoWords(String prefix, Integer limit) {
        StringBuilder sb = new StringBuilder("");
        List<AutocompleteData> matches = getStore().findCompletions(prefix, "", 1000);
        int counter = 0;
        for (AutocompleteData match : matches) {
            if (AutocompleteData.DATA_EFO_NODE == match.getDataType()) {
                if (limit == null || counter < limit) {
                    sb.append(match.getText()).append('|').append(match.getDataType()).append('|').append(match.getData()).append('\n');
                    counter++;
                } else {
                    break;
                }
            }
        }
        return sb.toString();
    }

    public String getEfoChildren(String efoId) {
        StringBuilder sb = new StringBuilder();

        if (null != getEfo()) {
            EFONode node = getEfo().getMap().get(efoId);
            if (null != node) {
                Set<EFONode> children = node.getChildren();
                if (null != children) {
                    for (EFONode child : children) {
                        sb.append(child.getTerm()).append("|o|");
                        if (child.hasChildren()) {
                            sb.append(child.getId());
                        }
                        sb.append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }

    public void rebuild() throws IOException {
        getStore().clear();

        // adding field terms (for all non-numerical fields) and names (if there is a description)
        Set<String> fields = search.getController().getFieldNames(studies.INDEX_ID);
        for (String field : fields) {
            String fieldTitle = search.getController().getFieldTitle(studies.INDEX_ID, field);
            if (null != fieldTitle && fieldTitle.length() > 0) {
                getStore().addData(
                        new AutocompleteData(
                                field
                                , AutocompleteData.DATA_FIELD
                                , fieldTitle
                        )
                );
            }
            String fieldType = search.getController().getFieldType(studies.INDEX_ID, field);
            if (null != fieldType && "string".equals(fieldType)) {
                List<String> terms = search.getController().getTerms(studies.INDEX_ID, field, "keywords".equals(field) ? 10 : 1);
                for (String term : terms ) {
                    getStore().addData(
                            new AutocompleteData(
                                    term
                                    , AutocompleteData.DATA_TEXT
                                    , field
                            )
                    );
                }
            }
        }

        if (null != getEfo()) {
            addEfoNodeWithDescendants(IEFO.ROOT_ID);
        }
    }

    private void addEfoNodeWithDescendants(String nodeId) {
        EFONode node = getEfo().getMap().get(nodeId);
        // make node expandable only if it has children and not organizational class
        if (null != node) {
            getStore().addData(
                    new AutocompleteData(
                            node.getTerm()
                            , AutocompleteData.DATA_EFO_NODE
                            , node.hasChildren() && !node.isOrganizationalClass() ? node.getId() : ""
                    )
            );

            if (node.hasChildren()) {
                for (EFONode child : node.getChildren()) {
                    addEfoNodeWithDescendants(child.getId());
                }
            }
        }
    }

    private AutocompleteStore getStore() {
        return this.autocompleteStore;
    }
}
