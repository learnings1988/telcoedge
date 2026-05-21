package com.telcoedge.subscriber.exception;

public class SubscriberNotFoundException extends RuntimeException{
    public SubscriberNotFoundException( String operatorId, String msisdn){
        super("Subscriber not found with Operator = " + operatorId + ", MSISDN = " + msisdn);
    }
}
