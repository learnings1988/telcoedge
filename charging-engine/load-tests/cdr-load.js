import http from 'k6/http';
import {check,cleep} from 'k6';
import {uuidv4} from 'https://jslib.k6.io/k6-utils/1.4.0/index.js'
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || "http://localhost:8081"

const KNOWN_STATUSES = ['CHARGED', 'DUPLICATE', 'INSUFFICIENT_BALANCE', 'SUBSCRIBER_NOT_FOUND',
'NO_PLAN_FOUND', 'NO_RATE_FOUND', 'HTTP_429','HTTP_500','HTTP_503','UNPARSEABLE_BODY','OTHER',];

const counters = {};

for(const s of KNOWN_STATUSES){
    counters[s] = new Counter('CDR_' + s);
}


export const options = {
    stages: [
        {duration: '30s', target: 50},
        {duration: '30s', target: 200},
        {duration: '1m', target: 500},
        {duration: '1m', target: 1000},
        {duration: '30s', target: 0},
    ],
    thresholds: {
        http_req_duration: ['p(99)<50'],
        http_req_failed: ['rate<0.01'],
    },
};

const MSISDNS = Array.from({length:1000},(_,i) =>
    '9876543'+String(i).padStart(3,'0'));

export default function(){
    const msisdn = MSISDNS[Math.floor((Math.random()*MSISDNS.length))];
    const usageTypes = ['VOICE','DATA','SMS'];
    const usageType = usageTypes[Math.floor(Math.random()*usageTypes.length)];

    const payload = JSON.stringify({
        eventId: uuidv4(),
        operatorId: 'acme',
        msisdn: msisdn,
        usageType: usageType,
        quantity: usageType === "VOICE" ? 120 : usageType === 'DATA' ? 50 : 1,
        startTime: new Date().toISOString(),
        endTime: new Date().toISOString(),
    });

    const res = http.post(`${BASE_URL}/api/v1/charging/cdr`,payload,{
        headers: {'Content-type': 'application/json' },
    });

    let status = 'HTTP_' + res.status;
    if(res.status == 200){
        try{
            status = JSON.parse(res.body).status;
        }catch(e){
            status = 'UNPARSEABLE_BODY'
        }
    }

    (counters[status] || counters['OTHER']).add(1);

    check(res, {
        'status is 200':(r)=>r.status===200,
        'body contains CHARGED':(r)=>r.body && r.body.includes('CHARGED'),
    });

}