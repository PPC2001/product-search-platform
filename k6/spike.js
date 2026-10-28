import { sleep } from 'k6';
import { searchProducts } from './lib.js';

// Sudden traffic spike (flash-sale style): 5 -> 200 VUs in 10 s, hold, then recover.
export const options = {
  stages: [
    { duration: '30s', target: 5 },
    { duration: '10s', target: 200 },
    { duration: '1m', target: 200 },
    { duration: '10s', target: 5 },
    { duration: '1m', target: 5 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<2000'],
  },
};

export default function () {
  searchProducts();
  sleep(0.5);
}
