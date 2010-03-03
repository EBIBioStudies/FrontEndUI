package uk.ac.ebi.arrayexpress.utils.persistence;

/*
 * Copyright 2009-2010 Microarray Informatics Group, European Bioinformatics Institute
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

/**
 * Persistable class implements two basic functions
 * <p/>
 * - toPersisence() that creates a String based upon object contents
 * - fromPersistence( String ) that initializes an Object based upon a String parameter
 * - shouldLoadFromPersistence() that returns true if object is just created and an attempt
 * to load from persistence should be made
 */
public interface Persistable
{
    public String toPersistence();

    public void fromPersistence( String str );

    public boolean isEmpty();

    public final static String EOL = System.getProperty("line.separator");
}
