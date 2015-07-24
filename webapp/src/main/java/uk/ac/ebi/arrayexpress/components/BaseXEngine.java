/*
 * Copyright 2009-2015 European Molecular Biology Laboratory
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

package uk.ac.ebi.arrayexpress.components;

import org.basex.core.*;
import org.basex.core.cmd.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.app.ApplicationComponent;

public class BaseXEngine extends ApplicationComponent {
    // logging machinery
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Context context = null;

    @Override
    public void initialize() throws Exception {
        context = new Context();

        //new CreateDB("biostudies-basex", "<database><studies><study><id>1</id></study></studies></database>").execute(context);
    }

    @Override
    public void terminate() throws Exception {
        if (null != context) {
            context.close();
            context = null;
        }
    }
}
