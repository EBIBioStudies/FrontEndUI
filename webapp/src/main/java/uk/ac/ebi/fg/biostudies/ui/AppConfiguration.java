package uk.ac.ebi.fg.biostudies.ui;

import com.google.inject.AbstractModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppConfiguration extends AbstractModule {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected void configure() {
        logger.info("About to configure BioStudies UI");
    }
}
