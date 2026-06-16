package com.telcoedge.charging;

import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.function.Supplier;

public class OptimisticLockRetry {


    private static final Logger log = LoggerFactory.getLogger(OptimisticLockRetry.class);
    private static final int DEFAULT_MAX_RETRIES =3;

    private OptimisticLockRetry(){}

    public static <T> T execute(Supplier<T> operation){
        return execute(operation, DEFAULT_MAX_RETRIES);
    }

    public static <T> T execute(Supplier<T> operation, int maxRetries){
        int attempt = 0;
        while(true){
            try{
                return operation.get();
            }catch(OptimisticLockException | ObjectOptimisticLockingFailureException e){
                attempt++;
                if(attempt >=maxRetries){
                    log.error("OptimisticLock failed after {} attempts" , attempt);
                    throw e;
                }
                log.warn("OptimisticLock conflict retry {}/{}", attempt, maxRetries);
            }
        }
    }
}
