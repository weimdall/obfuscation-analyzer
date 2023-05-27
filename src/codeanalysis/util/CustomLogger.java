/*
 * Copyright (C) 2022, all rights reserved.
 * Copying content is expressly prohibited without prior written permission of the University or the authors.
 * @author Julien Cassagne <julien.cassagne@polymtl.ca>
 */

package org.polymtl.codeanalysis.util;


import java.io.IOException;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class CustomLogger {
    private static Logger LOGGER                    = null;
    private static FileHandler handlerFile          = null;
    private static FileHandler handlerFileDebug     = null;
    private static ConsoleHandler handlerConsole    = null;
    private static Level level                      = Level.ALL;
    private static Formatter format                 = new SimpleFormatter() {
        @Override
        public synchronized String format(LogRecord record) {
            return String.format("[%1$tF %1$tT] %2$s %3$s%n",
                    new Date(record.getMillis()),
                    record.getLevel().getLocalizedName(),
                    record.getMessage());
        }
    };

    public static Logger getLogger(String name) {
        if(LOGGER != null)
            return LOGGER;
        return CustomLogger.getLogger(name, null);
    }

    public static Logger getLogger(String name, String filename) {
        Logger logger = Logger.getLogger(name);
        logger.setLevel(Level.ALL);
        if(handlerFile == null && filename != null) {
            try {
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'_'HH:mm");
                String date = df.format(new Date());
                handlerFile = new FileHandler(Paths.get("./logs/", filename + "_" + date + ".log").toString());
                handlerFile.setFormatter(format);
                logger.addHandler(handlerFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(handlerFileDebug == null && filename != null) {
            try {
                handlerFileDebug = new FileHandler(Paths.get("./logs/", filename + "_debug.log").toString());
                handlerFileDebug.setFormatter(format);
                handlerFileDebug.setLevel(Level.ALL);
                logger.addHandler(handlerFileDebug);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if(handlerConsole == null) {
            handlerConsole = new ConsoleHandler();
            handlerConsole.setFormatter(format);
            logger.setUseParentHandlers(false);
            logger.addHandler(handlerConsole);
        }
        LOGGER = logger;
        return LOGGER;
    }

    public static void setLevel(Level newLevel) {
        level = newLevel;
        handlerConsole.setLevel(newLevel);
        if(handlerFile != null)
            handlerFile.setLevel(newLevel);
    }

    public static Level getLevel() {
        return level;
    }
}
