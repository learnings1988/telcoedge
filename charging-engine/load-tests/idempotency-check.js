import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const FIXED_EVENT_ID = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee';

export const options = {
    vus: 20,
    duration: '10s',
};

export default function(){
    const payload = JSON.stringify({
        eventId: FIXED_EVENT_ID,
        operatorId: 'acme',
        msisdn: '9876540001',
        usageType: 'VOICE',
        quantity: 60,
        startTime: '2026-06-03T10:00:00Z',
        endTime: '2026-06-03T10:01:00Z',
    });

    const res = http.post(`${BASE_URL}/api/v1/charging/cdr`,payload,{
    headers: {'Content-Type': 'application/json'},
    });

    check(res, {
        'status 200': (r)=> res.status ===200,
        'CHARGED or DUPLICATE': (r)=> r.body &&
            (r.body.includes('CHARGED') || r.body.includes('DUPLICATE')),
    });
}