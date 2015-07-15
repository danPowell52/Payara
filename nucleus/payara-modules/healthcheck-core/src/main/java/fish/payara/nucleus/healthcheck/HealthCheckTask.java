/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2015 C2B2 Consulting Limited. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.healthcheck;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author steve
 */
class HealthCheckTask implements Runnable {
    
    private final String name;
    private final long period;
    private final TimeUnit unit;
    private boolean enabled = true;
    private final HealthCheck check;
    private CheckResult lastResult;
    
    private static final Logger logger = Logger.getLogger(HealthCheckService.class.getCanonicalName());

    HealthCheckTask(String name, long period, TimeUnit unit, HealthCheck check) {
        this.name = name;
        this.period = period;
        this.unit = unit;
        this.check = check;
    }

    String getName() {
        return name;
    }

    long getPeriod() {
        return period;
    }

    TimeUnit getUnit() {
        return unit;
    }

    boolean isEnabled() {
        return enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public void run() {
        if (enabled) {
            lastResult = check.doCheck();
            
            switch (lastResult) {
                case CRITICAL:
                    logger.log(Level.SEVERE, "{0}:{1}", new Object[]{name, check.getLastMessage()});
                    break;
                case GOOD:
                    logger.log(Level.INFO, "{0}:{1}", new Object[]{name, check.getLastMessage()});
                    break;
                case WARNING:
                    logger.log(Level.WARNING, "{0}:{1}", new Object[]{name, check.getLastMessage()});
                    break;
            }
        }
    }
    
}
