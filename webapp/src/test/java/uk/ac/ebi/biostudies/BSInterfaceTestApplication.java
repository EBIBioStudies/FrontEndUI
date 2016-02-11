package uk.ac.ebi.biostudies;

import uk.ac.ebi.biostudies.app.Application;

import java.net.MalformedURLException;
import java.net.URL;

public class BSInterfaceTestApplication extends Application
{
    public BSInterfaceTestApplication()
    {
        super("biostudies");

        // test-instance only code to emulate functionality missing from tomcat container
        // add a shutdown hook to to a proper termination

        /* // Commenting the block below since we don't test individual components in integration tests
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        addComponent(new SaxonEngine());
        addComponent(new SearchEngine());
        addComponent(new Studies());
        addComponent(new Users());
        addComponent(new Files());
        addComponent(new JobsController());
        */
        initialize();
    }

    public String getName()
    {
        return "BioStudies Test Application";
    }

    public URL getResource(String path) throws MalformedURLException
    {
        return getClass().getResource(path.replaceFirst("/WEB-INF/classes", ""));
    }

    // this is to receive termination notification and shutdown system properly
    private class ShutdownHook extends Thread
    {
        public void run()
        {
            Application.getInstance().terminate();
        }
    }
}
