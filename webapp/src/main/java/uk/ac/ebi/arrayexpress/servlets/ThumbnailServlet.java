package uk.ac.ebi.arrayexpress.servlets;


import net.sf.saxon.trans.XPathException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.app.ApplicationServlet;
import uk.ac.ebi.arrayexpress.components.Files;
import uk.ac.ebi.arrayexpress.components.Studies;
import uk.ac.ebi.arrayexpress.components.Thumbnails;
import uk.ac.ebi.arrayexpress.utils.saxon.SaxonException;
import uk.ac.ebi.microarray.arrayexpress.shared.auth.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by awais on 19/08/2015.
 */
public class ThumbnailServlet extends AuthAwareApplicationServlet {

    private static final long serialVersionUID = 3061219919204683614L;
    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    protected boolean canAcceptRequest(HttpServletRequest request, RequestType requestType) {
        return (requestType == RequestType.GET || requestType == RequestType.POST);
    }

    @Override
    protected void doAuthenticatedRequest(HttpServletRequest request, HttpServletResponse response, RequestType requestType, User authenticatedUser) throws ServletException, IOException {
        logRequest(logger, request, requestType);
        String accession = "";
        String name = "";

        String[] requestArgs = request.getPathInfo().replaceFirst("^/", "").split("/");
        if (1 == requestArgs.length) { // name only passed
            name = requestArgs[0];
        } else if (2 == requestArgs.length) { // accession/name passed
            accession = requestArgs[0];
            name = requestArgs[1];
        }
        logger.info("Requested thumbnail of [" + name + "], accession [" + accession + "]");

        Studies studies = getComponent(Studies.class);
        String relativePath = null;
        try {
            relativePath = studies.getRelativePath(accession, authenticatedUser);
        } catch (Exception e) {
            throw new ServletException(e);
        }

        Files files = getComponent(Files.class);
        Thumbnails thumbnails = getComponent(Thumbnails.class);

        if (relativePath==null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
                throw new IOException("File [" + name + "], accession [" + accession + "] is not present");
        }
        try {
            thumbnails.sendThumbnail(response, relativePath);
        } catch (Exception ex) {
            logger.warn("Could not generate thumbnail. User might have moved their mouse too fast. "+ ex.getMessage());
        }



    }


}