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
package fish.payara.appserver.demo.module.admin;

import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.hk2.api.PerLookup;

import org.glassfish.internal.api.Target;
import org.glassfish.config.support.TargetType;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.appserver.demo.module.DemoServiceConfiguration;
import java.beans.PropertyVetoException;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author steve
 */
@Service(name = "set-demo-service-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.EXCLUSIVE)
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-demo-service-configuration",
            description = "Set Demo Configuration")
})
public class SetDemoServiceConfiguration implements AdminCommand {

    @Param(name = "message")
    private String message;

    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;

    @Inject
    protected Target targetUtil;

    @Override
    public void execute(AdminCommandContext acc) {

        final ActionReport actionReport = acc.getActionReport();
        Config config = targetUtil.getConfig(target);
        DemoServiceConfiguration demoConfig = config.getExtensionByType(DemoServiceConfiguration.class);
        

        try {
            
        ConfigSupport.apply(new SingleConfigCode<DemoServiceConfiguration>() {
            
            @Override
            public Object run(final DemoServiceConfiguration demoConfigProxy ) throws PropertyVetoException, TransactionFailure {
                demoConfigProxy.setHelloWorldMessage(message);
                actionReport.setMessage("Hello World Message set to " + message);
                actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                return null;
            }

        }, demoConfig);
    }
    catch (TransactionFailure ex) {
        actionReport.setMessage(ex.getCause().getMessage());
        actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        return;
    }
}

}
