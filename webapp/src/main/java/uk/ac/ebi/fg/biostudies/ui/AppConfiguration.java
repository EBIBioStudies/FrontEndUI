package uk.ac.ebi.fg.biostudies.ui;

import com.google.inject.servlet.ServletModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostudies.servlets.AccessLoggingSuppressFilter;
import uk.ac.ebi.biostudies.servlets.StatusServlet;

public class AppConfiguration extends ServletModule {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    protected void configureServlets() {
        logger.info("About to configure BioStudies UI");
        
        filter("/servlets/status").through(AccessLoggingSuppressFilter.class);
        
        serve("/servlets/status").with(StatusServlet.class);
        //serveRegex("/servlets/error/.*").with(ErrorServlet.class);
        //serveRegex("/servlets/query/.*").with(QueryServlet.class);
    }
}
