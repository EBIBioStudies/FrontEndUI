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

package uk.ac.ebi.biostudies.app;

import com.google.common.base.Strings;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostudies.utils.EmailSender;
import uk.ac.ebi.biostudies.utils.LinuxShellCommandExecutor;
import uk.ac.ebi.biostudies.utils.StringTools;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    private final ApplicationPreferences prefs;
    private Map<String, ApplicationComponent> components;
    private EmailSender emailer;
    private LinuxShellCommandExecutor executor;

    private static Application appInstance = null;

    public Application(String prefsName) {
        prefs = new ApplicationPreferences(prefsName);
        components = new LinkedHashMap<>();
        // setting application instance available to whoever wants it
        if (null == appInstance) {
            appInstance = this;
        }
    }

    public abstract String getName();

    public abstract String getContextPath();

    public abstract URL getResource(String path) throws MalformedURLException;

    protected <T extends ApplicationComponent> void addComponent(T component) {
        String className = component.getClass().getSimpleName();
        if (components.containsKey(className)) {
            logger.error("The component [{}] has already been added to the application", className);
        } else {
            components.put(className, component);
        }
    }

    public <T extends ApplicationComponent> T getComponent(Class<T> clazz) {
        ApplicationComponent component = components.get(clazz.getSimpleName());
        if (component.getClass().isAssignableFrom(clazz)) {
            return clazz.cast(component);
        }
        return null;
    }

    public ApplicationPreferences getPreferences() {
        return prefs;
    }

    public void initialize() {
        logger.debug("Initializing the application...");
        prefs.initialize();
        emailer = new EmailSender(
                getPreferences().getString("app.reports.smtp.host")
                , getPreferences().getInteger("app.reports.smtp.port")
        );

        executor = new LinuxShellCommandExecutor();

        for (ApplicationComponent c : components.values()) {
            String componentName = c.getClass().getSimpleName();
            logger.info("Initializing component [{}]", componentName);
            try {
                c.initialize();
            } catch (RuntimeException x) {
                logger.error("[SEVERE] Caught a runtime exception while initializing [" + componentName + "]:", x);
                handleException("[SEVERE] Caught a runtime exception while initializing [" + componentName + "]", x);
            } catch (Error x) {
                logger.error("[SEVERE] Caught an error while initializing [" + componentName + "]:", x);
                handleException("[SEVERE] Caught an error while initializing [" + componentName + "]", x);
            } catch (Exception x) {
                logger.error("Caught an exception while initializing [" + componentName + "]:", x);
            }
        }
    }

    public void terminate() {
        logger.debug("Terminating the application...");
        ApplicationComponent[] compArray = components.values().toArray(new ApplicationComponent[components.size()]);

        for (int i = compArray.length - 1; i >= 0; --i) {
            ApplicationComponent c = compArray[i];
            String componentName = c.getClass().getSimpleName();
            logger.info("Terminating component [{}]", componentName);
            try {
                c.terminate();
            } catch (Throwable x) {
                logger.error("Caught an exception while terminating [" + componentName + "]:", x);
            }
        }
        // release references to application components
        components.clear();
        components = null;

        if (null != appInstance) {
            // remove reference to self
            appInstance = null;
        }
    }

    public void sendEmail(String originator, String[] recipients, String subject, String message) {
        try {
            Thread currentThread = Thread.currentThread();
            String hostName = "unknown";
            try {
                InetAddress localMachine = InetAddress.getLocalHost();
                hostName = localMachine.getHostName();
            } catch (Exception xx) {
                logger.debug("Caught an exception:", xx);
            }

            if (null == recipients || 0 == recipients.length) {
                recipients = getPreferences().getStringArray("app.reports.recipients");
            }

            if (null == originator || "".equals(originator)) {
                originator = getPreferences().getString("app.reports.originator");
            }

            Map<String, String> params = new HashMap<>();
            params.put("variable.appname", getName());
            params.put("variable.contextpath", getContextPath());
            params.put("variable.thread", String.valueOf(currentThread));
            params.put("variable.hostname", hostName);
            StrSubstitutor sub = new StrSubstitutor(params);

            emailer.send(recipients
                    , getPreferences().getStringArray("app.reports.hidden-recipients")
                    , subject
                    , sub.replace(message)
                    , originator
            );

        } catch (Throwable x) {
            logger.error("[SEVERE] Cannot even send an email without an exception:", x);
        }
    }

    public void handleException(String message, Throwable x) {
        sendEmail(
                null
                , null
                , getPreferences().getString("app.reports.subject")
                , message + ": " + x.getMessage() + StringTools.EOL
                        + "Application [${variable.appname}]" + StringTools.EOL
                        + "Context Path [${variable.contextpath}]" + StringTools.EOL
                        + "Host [${variable.hostname}]" + StringTools.EOL
                        + "Thread [${variable.thread}]" + StringTools.EOL
                        + getStackTrace(x)
        );

        if (x instanceof OutOfMemoryError) {
            requestRestart();
        }
    }

    private String getStackTrace(Throwable x) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        x.printStackTrace(printWriter);
        return result.toString();
    }

    public void requestRestart() {
        String command = getPreferences().getString("app.restart");
        if (!Strings.isNullOrEmpty(command)) {
            logger.info("Restart requested, performing [{}]", command);
            try {
                executor.execute(command, true);
                sendEmail(
                        null
                        , null
                        , "Restart succesfully requested"
                        , "Application [${variable.appname}]" + StringTools.EOL
                                + "Context Path [${variable.contextpath}]" + StringTools.EOL
                                + "Host [${variable.hostname}]" + StringTools.EOL
                                + "Thread [${variable.thread}]" + StringTools.EOL);

            } catch (Exception x) {
                logger.error("Restart error", x);
            }
        }
    }

    public static Application getInstance() {
        if (null == appInstance) {
            logger.error("Attempted to obtain application instance before initialization or after destruction");
        }
        return appInstance;
    }

    public static <T extends ApplicationComponent> T getAppComponent(Class<T> clazz) {
        return getInstance().getComponent(clazz);
    }
}
