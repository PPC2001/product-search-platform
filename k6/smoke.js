import { sleep } from 'k6';
import { healthCheck, searchProducts, fuzzySearch, createProduct } from './lib.js';

// Quick sanity check: 1 VU, 30 s. Run this before any heavier test.
export const options = {
  vus: 1,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
    checks: ['rate>0.99'],
  },
};

export default function () {
  healthCheck();
  searchProducts();
  fuzzySearch();
  createProduct();
  sleep(1);
}
