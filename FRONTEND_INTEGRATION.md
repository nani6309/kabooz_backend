# Kabooz Backend — Complete Frontend Integration Guide

## Backend Base URL
```
Development:  http://localhost:8080
Production:   https://api.kabooz.in  (update when deployed)
```

---

## 1. CORS — Already Configured ✅
The backend already allows these origins:
- `http://localhost:5173` (Vite/React dev)
- `http://localhost:3000` (Create React App dev)
- `https://kabooz.in`
- `https://www.kabooz.in`
- `https://admin.kabooz.in`

No extra config needed on frontend for CORS.

---

## 2. Install Axios in Your React Project
```bash
npm install axios
```

---

## 3. API Configuration File

Create `src/api/axiosConfig.js` in your React project:

```javascript
// src/api/axiosConfig.js
import axios from 'axios';

const BASE_URL = 'http://localhost:8080'; // change for production

// ── Public API (no auth needed) ───────────────────────────────
export const publicApi = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
});

// ── Admin API (JWT injected automatically) ────────────────────
export const adminApi = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
});

// Attach JWT token to every admin request automatically
adminApi.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('kabooz_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Handle 401 globally — redirect to login
adminApi.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('kabooz_token');
      localStorage.removeItem('kabooz_user');
      window.location.href = '/admin/login';
    }
    return Promise.reject(error);
  }
);
```

---

## 4. Auth Service (Admin Login)

Create `src/api/authService.js`:

```javascript
// src/api/authService.js
import { publicApi } from './axiosConfig';

const AUTH_KEY   = 'kabooz_token';
const USER_KEY   = 'kabooz_user';
const IDLE_KEY   = 'kabooz_idle_timeout';

export const authService = {

  /**
   * Login with username + password.
   * Saves token + idle timeout to localStorage.
   */
  async login(username, password) {
    const { data } = await publicApi.post('/api/auth/login', {
      username,
      password,
    });
    // Save token
    localStorage.setItem(AUTH_KEY, data.token);
    localStorage.setItem(USER_KEY, data.username);
    localStorage.setItem(IDLE_KEY, data.idleTimeoutSeconds); // 600
    return data;
  },

  /** Remove token and user info */
  logout() {
    localStorage.removeItem(AUTH_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(IDLE_KEY);
  },

  /** Check if admin is currently logged in */
  isLoggedIn() {
    return !!localStorage.getItem(AUTH_KEY);
  },

  getToken()    { return localStorage.getItem(AUTH_KEY); },
  getUsername() { return localStorage.getItem(USER_KEY); },
  getIdleTimeout() {
    return parseInt(localStorage.getItem(IDLE_KEY) || '600', 10);
  },
};
```

---

## 5. Idle Logout Hook (10-Minute Timeout)

Create `src/hooks/useIdleLogout.js`:

```javascript
// src/hooks/useIdleLogout.js
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { authService } from '../api/authService';

/**
 * Automatically logs out the admin after 10 minutes of inactivity.
 * Uses the idleTimeoutSeconds value returned by the login API.
 */
export function useIdleLogout() {
  const navigate = useNavigate();
  const timeoutMs = authService.getIdleTimeout() * 1000; // 600s → 600000ms

  useEffect(() => {
    let timer;

    const resetTimer = () => {
      clearTimeout(timer);
      timer = setTimeout(() => {
        authService.logout();
        alert('Session expired due to inactivity. Please log in again.');
        navigate('/admin/login');
      }, timeoutMs);
    };

    const events = ['mousemove', 'keydown', 'click', 'scroll', 'touchstart'];
    events.forEach((e) => window.addEventListener(e, resetTimer));

    // Start timer on mount
    resetTimer();

    return () => {
      clearTimeout(timer);
      events.forEach((e) => window.removeEventListener(e, resetTimer));
    };
  }, [navigate, timeoutMs]);
}
```

**Use it in your Admin Layout:**
```javascript
// src/layouts/AdminLayout.jsx
import { useIdleLogout } from '../hooks/useIdleLogout';

export default function AdminLayout({ children }) {
  useIdleLogout(); // auto-logout after 10 min idle
  return <div>{children}</div>;
}
```

---

## 6. Public Pricing Service (Homepage)

Create `src/api/pricingService.js`:

```javascript
// src/api/pricingService.js
import { publicApi } from './axiosConfig';

export const pricingService = {

  /**
   * GET /api/public/pricing
   * Returns glass + PET pricing table.
   * Call this when homepage loads — never hardcode prices.
   */
  async getPricing() {
    const { data } = await publicApi.get('/api/public/pricing');
    return data;
    /* Response shape:
    {
      glass: { bottlesPerCrate: 24, prices: [{ ppb: 20, crateTotal: 480 }, ...] },
      pet:   { bottlesPerCase: 30,  prices: [{ ppb: 22, caseTotal: 660 }, ...] }
    } */
  },
};
```

---

## 7. Public Order Service (Homepage)

Create `src/api/orderService.js`:

```javascript
// src/api/orderService.js
import { publicApi } from './axiosConfig';

export const publicOrderService = {

  /**
   * POST /api/public/orders
   * Place an order from the public homepage (no login needed).
   *
   * @param {object} orderData
   * @returns {Promise<{ id, invoiceNo, grandTotal, status, message }>}
   */
  async placeOrder(orderData) {
    const { data } = await publicApi.post('/api/public/orders', orderData);
    return data;
  },
};

/* Example usage in Homepage component:
  const result = await publicOrderService.placeOrder({
    customer: {
      name: "Ravi Kumar",
      mobile: "9876543210",
      address: "Bengaluru",
      placeOfSupply: "Karnataka",
      customerShopName: "Ravi Beverages"
    },
    items: [
      { bottleType: "GLASS", flavor: "Lemon", pricePerBottle: 20, quantity: 2 }
    ],
    notes: "Please deliver before noon"
  });
  console.log(result.invoiceNo); // "183"
  console.log(result.grandTotal); // 960
*/
```

---

## 8. Admin Services

Create `src/api/adminService.js`:

```javascript
// src/api/adminService.js
import { adminApi } from './axiosConfig';

export const adminService = {

  // ── Dashboard ──────────────────────────────────────────────

  /**
   * GET /api/admin/dashboard
   * Returns totals for orders, revenue, pending/overdue counts.
   */
  async getDashboard() {
    const { data } = await adminApi.get('/api/admin/dashboard');
    return data;
    /* Response:
    {
      totalOrders: 42, totalRevenue: 85000.00, totalReceived: 62000.00,
      pendingCount: 8,  overdueCount: 2,
      thisMonthOrders: 12, thisMonthRevenue: 24000.00
    } */
  },

  // ── Orders List ────────────────────────────────────────────

  /**
   * GET /api/admin/orders
   * Paginated list with optional filters.
   *
   * @param {number} page   - 0-based page number (default 0)
   * @param {number} size   - page size (default 20)
   * @param {string} status - 'all' | 'PENDING' | 'PAID' | 'OVERDUE'
   * @param {string} search - search by name / mobile / invoice no
   */
  async getOrders({ page = 0, size = 20, status = 'all', search = '' } = {}) {
    const params = { page, size, status };
    if (search) params.search = search;
    const { data } = await adminApi.get('/api/admin/orders', { params });
    return data;
    /* Response (Spring Page):
    {
      content: [ { id, invoiceNo, customerName, customerShopName, mobile, invoiceDate,
                   grandTotal, receivedAmount, balanceDue, status, itemCount } ],
      totalElements: 42,
      totalPages: 3,
      number: 0,
      size: 20
    } */
  },

  // ── Single Order ───────────────────────────────────────────

  /**
   * GET /api/admin/orders/{id}
   * Full order with customer, all items, and tax breakdown.
   */
  async getOrder(id) {
    const { data } = await adminApi.get(`/api/admin/orders/${id}`);
    return data;
  },

  // ── Create Order ───────────────────────────────────────────

  /**
   * POST /api/admin/orders
   * Admin creates an order (can set any price + receivedAmount).
   */
  async createOrder(orderData) {
    const { data } = await adminApi.post('/api/admin/orders', orderData);
    return data;
  },

  // ── Update Status ──────────────────────────────────────────

  /**
   * PUT /api/admin/orders/{id}/status
   * Update payment status and/or received amount.
   *
   * @param {number} id
   * @param {string} status         - 'PAID' | 'PENDING' | 'OVERDUE'
   * @param {number} receivedAmount - amount received so far
   */
  async updateStatus(id, status, receivedAmount) {
    const { data } = await adminApi.put(`/api/admin/orders/${id}/status`, {
      status,
      receivedAmount,
    });
    return data;
  },

  // ── Soft Delete ────────────────────────────────────────────

  /**
   * DELETE /api/admin/orders/{id}
   * Soft delete — data is kept in DB, just hidden from UI.
   */
  async deleteOrder(id) {
    await adminApi.delete(`/api/admin/orders/${id}`);
  },

  // ── PDF Invoice ────────────────────────────────────────────

  /**
   * GET /api/admin/orders/{id}/invoice/pdf
   * Downloads the PDF invoice as a file.
   */
  async downloadInvoicePdf(id, invoiceNo) {
    const response = await adminApi.get(
      `/api/admin/orders/${id}/invoice/pdf`,
      { responseType: 'blob' }       // ← must be blob for PDF
    );

    // Trigger browser download
    const url    = window.URL.createObjectURL(new Blob([response.data]));
    const link   = document.createElement('a');
    link.href    = url;
    link.setAttribute('download', `Invoice-${invoiceNo}.pdf`);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);
  },
};
```

---

## 9. Error Handling Helper

Create `src/api/errorHelper.js`:

```javascript
// src/api/errorHelper.js

/**
 * Extract a human-readable error message from an Axios error.
 * The backend always returns: { status, error, message, timestamp }
 */
export function getErrorMessage(error) {
  if (error.response) {
    // Backend returned an error response
    const { data, status } = error.response;
    if (data?.message) return data.message;
    if (data?.fieldErrors) {
      // Validation errors — join all field messages
      return Object.values(data.fieldErrors).join(', ');
    }
    if (status === 401) return 'Session expired. Please log in again.';
    if (status === 403) return 'You do not have permission to do this.';
    if (status === 404) return 'Record not found.';
    if (status === 409) return 'A record with this information already exists.';
    return `Server error (${status})`;
  }
  if (error.request) return 'No response from server. Check your connection.';
  return error.message || 'An unexpected error occurred.';
}
```

---

## 10. React Usage Examples

### Homepage — Load Pricing on Mount
```javascript
import { useEffect, useState } from 'react';
import { pricingService } from '../api/pricingService';

function PricingSection() {
  const [pricing, setPricing] = useState(null);

  useEffect(() => {
    pricingService.getPricing().then(setPricing).catch(console.error);
  }, []);

  if (!pricing) return <p>Loading prices...</p>;

  return (
    <div>
      <h3>Glass Bottles — 1 crate = {pricing.glass.bottlesPerCrate} bottles</h3>
      {pricing.glass.prices.map(p => (
        <p key={p.ppb}>₹{p.ppb}/bottle → ₹{p.crateTotal}/crate</p>
      ))}
      <h3>PET Bottles — 1 case = {pricing.pet.bottlesPerCase} bottles</h3>
      {pricing.pet.prices.map(p => (
        <p key={p.ppb}>₹{p.ppb}/bottle → ₹{p.caseTotal}/case</p>
      ))}
    </div>
  );
}
```

### Homepage — Place Order
```javascript
import { publicOrderService } from '../api/orderService';
import { getErrorMessage } from '../api/errorHelper';

async function handleSubmit(formData) {
  try {
    const result = await publicOrderService.placeOrder({
      customer: {
        name: formData.name,
        mobile: formData.mobile,
        address: formData.address,
        placeOfSupply: 'Karnataka',
        customerShopName: formData.customerShopName,
      },
      items: formData.items.map(item => ({
        bottleType: item.bottleType,   // "GLASS" or "PET"
        flavor: item.flavor,
        pricePerBottle: item.pricePerBottle,
        quantity: item.quantity,
      })),
      notes: formData.notes,
    });
    alert(`Order placed! Invoice #${result.invoiceNo} | Total: ₹${result.grandTotal}`);
  } catch (error) {
    alert(getErrorMessage(error));
  }
}
```

### Admin Login Page
```javascript
import { authService } from '../api/authService';
import { getErrorMessage } from '../api/errorHelper';
import { useNavigate } from 'react-router-dom';

function LoginPage() {
  const navigate = useNavigate();

  async function handleLogin(e) {
    e.preventDefault();
    const username = e.target.username.value;
    const password = e.target.password.value;
    try {
      await authService.login(username, password);
      navigate('/admin/dashboard');
    } catch (error) {
      alert(getErrorMessage(error));
    }
  }

  return (
    <form onSubmit={handleLogin}>
      <input name="username" placeholder="Username" />
      <input name="password" type="password" placeholder="Password" />
      <button type="submit">Login</button>
    </form>
  );
}
```

### Admin Dashboard
```javascript
import { useEffect, useState } from 'react';
import { adminService } from '../api/adminService';
import { getErrorMessage } from '../api/errorHelper';

function Dashboard() {
  const [stats, setStats] = useState(null);

  useEffect(() => {
    adminService.getDashboard()
      .then(setStats)
      .catch(err => alert(getErrorMessage(err)));
  }, []);

  if (!stats) return <p>Loading...</p>;

  return (
    <div>
      <h2>Dashboard</h2>
      <p>Total Orders: {stats.totalOrders}</p>
      <p>Total Revenue: ₹{stats.totalRevenue}</p>
      <p>Total Received: ₹{stats.totalReceived}</p>
      <p>Pending: {stats.pendingCount}</p>
      <p>Overdue: {stats.overdueCount}</p>
      <p>This Month Orders: {stats.thisMonthOrders}</p>
      <p>This Month Revenue: ₹{stats.thisMonthRevenue}</p>
    </div>
  );
}
```

### Admin Orders List
```javascript
import { useEffect, useState } from 'react';
import { adminService } from '../api/adminService';

function OrdersList() {
  const [orders, setOrders] = useState([]);
  const [page, setPage]     = useState(0);
  const [total, setTotal]   = useState(0);
  const [status, setStatus] = useState('all');
  const [search, setSearch] = useState('');

  const fetchOrders = async () => {
    const data = await adminService.getOrders({ page, size: 20, status, search });
    setOrders(data.content);
    setTotal(data.totalElements);
  };

  useEffect(() => { fetchOrders(); }, [page, status, search]);

  const handleDownloadPdf = async (order) => {
    await adminService.downloadInvoicePdf(order.id, order.invoiceNo);
  };

  const handleMarkPaid = async (order) => {
    await adminService.updateStatus(order.id, 'PAID', order.grandTotal);
    fetchOrders();
  };

  return (
    <div>
      <input
        placeholder="Search name / mobile / invoice..."
        onChange={e => setSearch(e.target.value)}
      />
      <select onChange={e => setStatus(e.target.value)}>
        <option value="all">All</option>
        <option value="PENDING">Pending</option>
        <option value="PAID">Paid</option>
        <option value="OVERDUE">Overdue</option>
      </select>
      <p>Total: {total} orders</p>
      <table>
        <thead>
          <tr>
            <th>Invoice</th><th>Customer</th><th>Shop</th><th>Mobile</th>
            <th>Total</th><th>Received</th><th>Status</th><th>Actions</th>
          </tr>
        </thead>
        <tbody>
          {orders.map(order => (
            <tr key={order.id}>
              <td>#{order.invoiceNo}</td>
              <td>{order.customerName}</td>
              <td>{order.customerShopName || '-'}</td>
              <td>{order.mobile}</td>
              <td>₹{order.grandTotal}</td>
              <td>₹{order.receivedAmount}</td>
              <td>{order.status}</td>
              <td>
                <button onClick={() => handleDownloadPdf(order)}>PDF</button>
                <button onClick={() => handleMarkPaid(order)}>Mark Paid</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
```

---

## 11. Protected Admin Route

```javascript
// src/components/ProtectedRoute.jsx
import { Navigate } from 'react-router-dom';
import { authService } from '../api/authService';

export default function ProtectedRoute({ children }) {
  if (!authService.isLoggedIn()) {
    return <Navigate to="/admin/login" replace />;
  }
  return children;
}

// Usage in App.jsx:
// <Route path="/admin/dashboard" element={
//   <ProtectedRoute><AdminLayout><Dashboard /></AdminLayout></ProtectedRoute>
// } />
```

---

## 12. Complete API Reference

| Method | URL | Auth | Purpose |
|--------|-----|------|---------|
| POST | `/api/auth/login` | ❌ None | Admin login |
| GET | `/api/public/pricing` | ❌ None | Fetch price table |
| POST | `/api/public/orders` | ❌ None | Place order (homepage) |
| GET | `/api/admin/dashboard` | ✅ JWT | Dashboard stats |
| GET | `/api/admin/orders` | ✅ JWT | List orders (paginated) |
| GET | `/api/admin/orders/{id}` | ✅ JWT | Full order details |
| POST | `/api/admin/orders` | ✅ JWT | Create order |
| PUT | `/api/admin/orders/{id}/status` | ✅ JWT | Update status + payment |
| DELETE | `/api/admin/orders/{id}` | ✅ JWT | Soft delete |
| GET | `/api/admin/orders/{id}/invoice/pdf` | ✅ JWT | Download PDF |

---

## 13. Environment Variables (.env)

Create `.env` in your React project root:
```env
VITE_API_BASE_URL=http://localhost:8080
```

Then in `axiosConfig.js`:
```javascript
const BASE_URL = import.meta.env.VITE_API_BASE_URL;
```
