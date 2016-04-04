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

package uk.ac.ebi.biostudies.utils;

import uk.ac.ebi.biostudies.servlets.ZipStatusServlet;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

public class HttpTools {

    public static final String AE_TOKEN_COOKIE = "AeLoginToken";
    public static final String AE_USERNAME_COOKIE = "AeLoggedUser";
    public static final String AE_AUTH_MESSAGE_COOKIE = "AeAuthMessage";
    public static final String AE_AUTH_USERNAME_COOKIE = "AeAuthUser";

    public static final String REFERER_HEADER = "Referer";

    public static void setCookie(HttpServletResponse response, String name, String value, Integer maxAge) {
        Cookie cookie = new Cookie(name, null != value ? value : "");
        // if the value is null - delete cookie by expiring it
        cookie.setPath("/");
        if (null == value) {
            cookie.setMaxAge(0);
        } else if (null != maxAge) {
            cookie.setMaxAge(maxAge);
        }
        response.addCookie(cookie);
    }

    public static String getCookie(HttpServletRequest request, String name) {
        CookieMap cookies = new CookieMap(request.getCookies());
        return cookies.containsKey(name) ? cookies.get(name).getValue() : null;
    }

    public static String getFileNameFromPart(final Part part) {
        for (String content : part.getHeader("content-disposition").split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(
                        content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }

    public static void displayMessage(HttpServletRequest request, HttpServletResponse response, String title, String message) throws ServletException, IOException {
        String forwardedParams = String.format("?title=%s&message=%s",
                URLEncoder.encode(title, "UTF-8"),
                URLEncoder.encode(message, "UTF-8"));
        request.getRequestDispatcher("/servlets/view/display/message/html"+forwardedParams).forward(request, response);
    }

    public static File uploadFile(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String sourceLocation = System.getProperty("java.io.tmpdir");
        Part filePart = request.getPart("file");
        if (filePart == null) {
            displayMessage(request,response,"Error!", "Could not upload file.");
            return null;
        }
        String fileName = getFileNameFromPart(filePart);
        if ("studies.xml".equalsIgnoreCase(fileName)) {
            displayMessage(request,response,"Error!", fileName+" can't be overwritten.");
            return null;
        }
        File uploadedFile = new File(sourceLocation, fileName);
        try (FileOutputStream out = new FileOutputStream(uploadedFile);
             InputStream fileContent = filePart.getInputStream();
        ) {
            //logger.debug("File {} will be uploaded to {}", fileName, uploadedFile.getAbsolutePath());
            int read;
            final byte[] bytes = new byte[1024];
            while ((read = fileContent.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
        }
        return uploadedFile;
    }
}
