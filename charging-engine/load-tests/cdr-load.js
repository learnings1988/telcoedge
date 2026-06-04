import http from 'k6/http';
import {check,cleep} from 'k6';
import {uuidv4} from 'https://jslib.k6.io/k6-utils/1.4.0/index.js'

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080"

export const options = {
    stages: [
        {duration: '30s', target: 20},
        {duration: '30s', target: 50},
        {duration: '30s', target: 0},
    ],
    thresholds: {
        http_req_duration: ['p(99)<50'],
        http_req_failed: ['rate<0.01'],
    },
};

const MSISDNS = Array.from({length:1000},(_,i) =>
    '987654'+String(i).padStart(4,'0'));

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

    check(res, {
        'status is 200':(r)=>r.status===200,
        'body contains CHARGED':(r)=>r.body && r.body.includes('CHARGED'),
    });

}