/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.schedule.service;


import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.PARAMETER;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.common.util.timer.TimerSchedule;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;



/**
 *
 * @author Daniel
 */
@Service(name="payara-timer")
@RunLevel(StartupRunLevel.VAL)
public class ScheduleService implements EventListener, PostConstruct{

    @Inject
    Events events;
    
    private List<Future> futureList;
    
    HashMap map = new HashMap();
    
    @Inject
    ScheduleConfig config;
    
    private Boolean running;
    
    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_STARTUP)){
            System.out.println("PREPARE YOURSELF");
            //start service
            bootstrapScheduler();
        }
        if (event.is(EventTypes.SERVER_SHUTDOWN)){
            System.out.println("SHUTTING DOWN");
            //end service
            
        }
    }

    @Override
    public void postConstruct() {
        events.register(this);
        System.out.println("DONE HERE");
        
    }
 
    public void bootstrapScheduler(){
        if (config.getEnabled()==true){
            startPool();
            prepareSchedules();
        }
    }
    
    public void startPool(){
        if (config.getCoreSize()<1){
            executor = (ScheduledThreadPoolExecutor)Executors.newScheduledThreadPool(1);
        } else{
            executor = (ScheduledThreadPoolExecutor)Executors.newScheduledThreadPool(config.getCoreSize());
        }
        
    }
    
    public void prepareSchedules(){
        for (String job: config.getJobs()){
            validateSchedule(job, true);
        }
    }
    
    ScheduledThreadPoolExecutor executor;
    String hello = "hello i am the runnable";
    public void startRun(final String name,final String cron, String filePath){
            System.out.println("EXECUTOR BEING RUN");
            
               TimerSchedule schedule = new TimerSchedule(cron);
               Calendar date = schedule.getNextTimeout();
                Date endDate = date.getTime();
            
            
            
            
            
        executor.schedule(new Runnable() {
            
            @Override
            public void run(){
                //do the job
                running=true;
                System.out.println(hello);
                
                ThreadLocal nameInstance = new ThreadLocal();
                nameInstance.set(name);
                System.out.println("the thread name value is " +nameInstance.get());
                ThreadLocal cronInstance = new ThreadLocal();
                cronInstance.set(cron);
                
                ThreadLocal filePathInstance = new ThreadLocal();
                
                
                
                //schedule next task
                ArrayList jobList = new ArrayList();
                Boolean found = false;
                for (String job: config.getJobs()){
                    String[] configInfo = job.split(",");
                    String[] nameInfo = configInfo[0].split("=");
                    String[] cronInfo = configInfo[1].split("=");
                    String[] filepathInfo = configInfo[2].split("=");
                    System.out.println("the name form the domain.xml is "+nameInfo[1]);
                    if (nameInfo[1].equals(nameInstance.get()) ){
                        System.out.println("THE NAME MATCHED");
                        if (validateSchedule(job,false)){
                            System.out.println("The job was validated");
                            cronInstance.set(cronInfo[1]);
                            filePathInstance.set(filepathInfo[1]);
                            found=true;
                            System.out.println("THE BOOLEAN IS "+found);
                            break;
                        }
                    }
                }
                System.out.println("THE SECOND BOOLEAN IS "+found);
                if (found.equals(false)){
                    System.out.println(nameInstance.get()+" was not found, will not schedule job for it");
                    
                } else if(found.equals(true)){
                    System.out.println("Scheduling next job for "+nameInstance.get());
                    System.out.println("THE Cron is "+cronInstance.get().toString());
                    TimerSchedule checkSchedule = new TimerSchedule(cronInstance.get().toString());
                    System.out.println("THE Cron is "+cronInstance.get().toString());
                    ScheduledFuture<?> nextJob = executor.schedule(this, checkSchedule.getNextTimeout().getTime().getTime()-System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                    map.put(name, nextJob);
                    futureList.add(nextJob);
                    running = false;
                    
                }
            }
        }, (endDate.getTime()-System.currentTimeMillis()), TimeUnit.MILLISECONDS);

    } //end executor
    

    public HashMap getFuturesList(){
        return map;
    }
    
    public void setFuturesList(HashMap newMap){
        map=newMap;
    }
    
    public Boolean validateSchedule(String job, Boolean start){
        List<String> names = new ArrayList();
            try {
                String[] configInfo = job.split(",");
                String[] nameInfo = configInfo[0].split("=");
                String[] cronInfo = configInfo[1].split("=");
                String[] filepathInfo = configInfo[2].split("=");
                
            
                String[] cronParts = cronInfo[1].split(" # ");
                if (cronParts.length != 10){
                    System.out.println("size is"+cronParts.length);
                    System.out.println("The cron sequence"+Arrays.toString(cronInfo)+" for "+nameInfo[1]+"is incomplete it must have 10 fields eg * * * * * * *");
                    return false;
                }
                

                if( new File(filepathInfo[1]).exists() == false){
                    System.out.println("Cannot read the file"+filepathInfo[1] +" for "+nameInfo[1]);
                    return false;
                }
                if (names.contains(nameInfo[1])){
                    System.out.println("The name "+nameInfo[1]+ "is already used please change it");
                    return false;
                }

                names.add(nameInfo[1]);
                if (start.equals(true)){
                   startRun(nameInfo[1], cronInfo[1], filepathInfo[1]); 
                }
                return true;
            }catch(IndexOutOfBoundsException OofB) {
                System.out.println("Could not parse <jobs> in Domain.xml please ensure it is correctly formatted, for help look at the create-schedule command");
                System.out.println(OofB);
            }
        return false;
    }
    
    public String buildCron(String cron){
        String[] cronParts=cron.split(" ");
        String cronSequence = cronParts[0] + " # " + cronParts[1] + " # "+ cronParts[2] + " # "+ cronParts[3]+ " # " + cronParts[4]+ " # " + cronParts[5]
        + " # " + cronParts[6]+ " # " + "null" +" # "+"null" + " # "+"null";
        return cronSequence;
    }
    
    public void add(){
        System.out.println("add was called");
        executor.setCorePoolSize(executor.getCorePoolSize()+1);
        System.out.println(executor.getCorePoolSize());
    }
    
    public void delete(){
        System.out.println("add was called");
        executor.setCorePoolSize(executor.getCorePoolSize()-1);
        System.out.println(executor.getCorePoolSize());
    }
    
    public void stop(String name){
        if (running=true){
            System.out.println("job is running, will wait 2 seconds then retry");
            try {
                wait(2000);
                stop(name);
            } catch (InterruptedException ex) {
                Logger.getLogger(ScheduleService.class.getName()).log(Level.SEVERE, "wait failed", ex);
            }
            
        }else{
            
        }
        
        
    }
    
    public void shutdown(){
        
    }
}
