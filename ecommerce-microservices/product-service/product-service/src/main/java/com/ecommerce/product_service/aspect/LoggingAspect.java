package com.ecommerce.product_service.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect     // marks this as an AOP aspect
@Component  // registers it as a Spring bean
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    // pointcut — matches all methods in the service package
    @Pointcut("execution(* com.ecommerce.product_service.service.*.*(..))")
    public void serviceLayer() {}

    // pointcut — matches methods annotated with @TrackExecutionTime
    @Pointcut("@annotation(com.ecommerce.product_service.aspect.TrackExecutionTime)")
    public void trackExecutionTime() {}

    // @Around — runs before AND after the method, controls execution
    @Around("serviceLayer()")
    public Object logServiceCall(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        String args       = Arrays.toString(joinPoint.getArgs());

        log.info("→ Calling {} with args: {}", methodName, args);
        long start = System.currentTimeMillis();

        Object result = joinPoint.proceed();  // actually call the method

        long duration = System.currentTimeMillis() - start;
        log.info("← {} completed in {}ms", methodName, duration);
        return result;
    }

    // @Around on @TrackExecutionTime — logs execution time for annotated methods
    @Around("trackExecutionTime()")
    public Object trackTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start  = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;
        log.info("[TRACK] {} executed in {}ms", joinPoint.getSignature().toShortString(), duration);
        return result;
    }

    // @AfterThrowing — runs when a method throws an exception (does NOT suppress it)
    @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
    public void logException(JoinPoint joinPoint, Exception ex) {
        log.error("✗ Exception in {}: {} — {}",
                joinPoint.getSignature().toShortString(),
                ex.getClass().getSimpleName(),
                ex.getMessage());
    }
}
