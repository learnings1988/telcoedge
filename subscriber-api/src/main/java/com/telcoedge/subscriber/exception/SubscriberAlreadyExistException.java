package com.telcoedge.subscriber.exception;

public class SubscriberAlreadyExistException extends RuntimeException{
    public SubscriberAlreadyExistException(String operatorId, String msisdn){
        super("Subscriber with operatorID = " + operatorId +", MSISDN = " + msisdn + " already exist");
    }
}
