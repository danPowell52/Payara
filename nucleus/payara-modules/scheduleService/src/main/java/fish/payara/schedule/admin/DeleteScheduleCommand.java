/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.schedule.admin;

import fish.payara.schedule.service.ScheduleConfig;
import fish.payara.schedule.service.ScheduleService;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.common.util.timer.TimerSchedule;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Daniel
 */
@Service(name="delete-schedule")
@PerLookup
public class DeleteScheduleCommand implements AdminCommand{
        @Inject
    ScheduleConfig config;
    
    @Param(name="name",optional=false)
    String name;   
    
    @Inject
    ScheduleService service;
    
    String job;

    @Override
    public void execute(AdminCommandContext context) {
            final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
            
               
        System.out.println("SET RUN");
        try {
            ConfigSupport.apply(new SingleConfigCode<ScheduleConfig>(){
                public Object run(ScheduleConfig configProxy)
                    throws PropertyVetoException, TransactionFailure{
                    if (name != null){
                        Boolean found = false;
                        System.out.println(" NUMBER ONE");
                        List<String> jobs = new ArrayList();
                        for (int i =0; i < configProxy.getJobs().size();i++){
                            job = configProxy.getJobs().get(i);
                            String[] configInfo = job.split(",");
                            String[] nameInfo = configInfo[0].split("=");

                            System.out.println("Delete: the name form the domain.xml is "+nameInfo[1]);
                            if (nameInfo[1].equals(name) ){                                
                                    found=true;                                                             
                            } else{
                                jobs.add(configProxy.getJobs().get(i));
                            }

                            HashMap david = service.getFuturesList();
                            
                            System.out.println("futures = "+ david.toString());
                            System.out.println(" NUMBER FOUR");
                            System.out.println(david.get(name));
                            Future job = (Future)david.get(name);
                            job.cancel(true);
                        }
                        System.out.println(" NUMBER SIX");
                        if (found.equals(false)){
                            System.out.println("The schedule name was not found, please check that schedule exists");
                        }else {
                            //configProxy.setJobs(jobs);
                        }
                    }
                    return null;
                }
            },config);
        }catch (TransactionFailure ex){
            ex.printStackTrace();
            System.out.println("The transaction has failed ");
        }//en transaction
    }
}
