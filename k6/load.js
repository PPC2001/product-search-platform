import { sleep } from 'k6';
import { searchProducts, fuzzySearch, createProduct } from './lib.js';

// Realistic mixed workload: mostly reads (search), some writes.
//   search_users : ramps to 50 VUs  (override with SEARCH_VUS)
//   write_users  : constant 5 VUs   (override with WRITE_VUS)
const SEARCH_VUS = Number(__ENV.SEARCH_VUS || 50);
const WRITE_VUS = Number(__ENV.WRITE_VUS || 5);

export const options = {
  scenarios: {
    search_users: {
      executor: 'ramping-vus',
      exec: 'searchScenario',
      startVUs: 0,
      stages: [
        { duration: '30s', target: SEARCH_VUS },
        { duration: '2m', target: SEARCH_VUS },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
    write_users: {
      executor: 'constant-vus',
      exec: 'writeScenario',
      vus: WRITE_VUS,
      duration: '3m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    'http_req_duration{name:GET /api/products/search}': ['p(95)<500', 'p(99)<1000'],
    'http_req_duration{name:POST /api/products}': ['p(95)<1000'],
    checks: ['rate>0.98'],
  },
};

export function searchScenario() {
  if (Math.random() < 0.85) {
    searchProducts();
  } else {
    fuzzySearch();
  }
  sleep(Math.random() * 1.5 + 0.5);
}

export function writeScenario() {
  createProduct();
  sleep(Math.random() * 2 + 1);
}
