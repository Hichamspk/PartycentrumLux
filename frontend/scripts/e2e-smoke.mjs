import { chromium } from 'playwright-core';

const API = process.env.API_URL ?? 'http://localhost:8080/api';
const WEB = process.env.WEB_URL ?? 'http://localhost:4200';
const CHROME = process.env.CHROME_PATH
  ?? 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe';

const runId = Date.now();
const created = {
  customers: [],
  bookings: [],
  invoices: [],
  payments: [],
  users: []
};

const checks = [];
let cleanupToken = null;

function ok(name, detail = '') {
  checks.push({ name, detail });
  console.log(`OK ${name}${detail ? ` - ${detail}` : ''}`);
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function isoDate(offsetDays) {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  return date.toISOString().slice(0, 10);
}

function toIso(date) {
  return date.toISOString().slice(0, 10);
}

async function freeDate(token) {
  const today = new Date();
  const start = new Date(today);
  start.setDate(today.getDate() + 1);
  const end = new Date(today);
  end.setDate(today.getDate() + 120);
  const bookings = await api(`/bookings/calendar?start=${toIso(start)}&end=${toIso(end)}`, { token });
  const occupied = new Set();
  for (const booking of bookings) {
    const eventDate = booking.eventDate ?? booking.date;
    if (eventDate) {
      occupied.add(eventDate);
    }
  }

  for (const cursor = new Date(start); cursor <= end; cursor.setDate(cursor.getDate() + 1)) {
    const iso = toIso(cursor);
    if (!occupied.has(iso)) {
      return iso;
    }
  }

  throw new Error('No free date found for booking test.');
}

async function api(path, { method = 'GET', token, body, expect = 200 } = {}) {
  const response = await fetch(`${API}${path}`, {
    method,
    headers: {
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: body ? JSON.stringify(body) : undefined
  });

  if (response.status !== expect) {
    const text = await response.text();
    throw new Error(`${method} ${path} expected ${expect}, got ${response.status}: ${text}`);
  }

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/pdf')) {
    return Buffer.from(await response.arrayBuffer());
  }
  if (contentType.includes('text/html') || contentType.includes('text/plain')) {
    return response.text();
  }
  return response.json();
}

function settingsPayload(settings) {
  return {
    companyName: settings.companyName,
    logoPath: settings.logoPath ?? '',
    logoBase64: settings.logoBase64 ?? '',
    brandColor: settings.brandColor ?? '#C9A84C',
    address: settings.address,
    postalCode: settings.postalCode,
    city: settings.city,
    kvk: settings.kvk,
    vatNumber: settings.vatNumber,
    iban: settings.iban,
    phone: settings.phone,
    email: settings.email,
    website: settings.website,
    mailFrom: settings.mailFrom,
    docusealApiKey: settings.docusealApiKey ?? '',
    docusealBaseUrl: settings.docusealBaseUrl ?? 'http://docuseal:3000',
    docusealContractTemplateId: settings.docusealContractTemplateId ?? '',
    generalTerms: settings.generalTerms ?? 'Alle genoemde afspraken zijn onder voorbehoud van beschikbaarheid en schriftelijke bevestiging.'
  };
}

async function uploadLogo(token) {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="160" height="80" viewBox="0 0 160 80"><rect width="160" height="80" rx="8" fill="#111827"/><text x="80" y="50" text-anchor="middle" font-size="34" font-family="Arial" font-weight="700" fill="#C9A84C">LUX</text></svg>`;
  const formData = new FormData();
  formData.append('file', new Blob([svg], { type: 'image/svg+xml' }), 'lux-e2e.svg');
  const response = await fetch(`${API}/settings/logo`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: formData
  });

  if (response.status !== 200) {
    const text = await response.text();
    throw new Error(`POST /settings/logo expected 200, got ${response.status}: ${text}`);
  }
  return response.json();
}

async function login(email, password) {
  return api('/auth/login', {
    method: 'POST',
    body: { email, password }
  });
}

async function cleanup(token) {
  for (const id of [...created.payments].reverse()) {
    await api(`/payments/${id}`, { method: 'DELETE', token, expect: 204 }).catch(() => {});
  }
  for (const id of [...created.invoices].reverse()) {
    await api(`/invoices/${id}`, { method: 'DELETE', token, expect: 204 }).catch(() => {});
  }
  for (const id of [...created.bookings].reverse()) {
    await api(`/bookings/${id}`, { method: 'DELETE', token, expect: 204 }).catch(() => {});
  }
  for (const id of [...created.customers].reverse()) {
    await api(`/customers/${id}`, { method: 'DELETE', token, expect: 204 }).catch(() => {});
  }
  for (const id of [...created.users].reverse()) {
    await api(`/users/${id}`, { method: 'DELETE', token, expect: 204 }).catch(() => {});
  }
}

async function backendFlow() {
  const owner = await login('owner@partycentrumlux.nl', 'LuxAdmin123!');
  const employee = await login('employee@partycentrumlux.nl', 'LuxEmployee123!');
  assert(owner.role === 'OWNER', 'Owner seed user should have OWNER role.');
  assert(employee.role === 'EMPLOYEE', 'Employee seed user should have EMPLOYEE role.');
  ok('JWT login OWNER and EMPLOYEE');

  const ownerToken = owner.accessToken;
  const employeeToken = employee.accessToken;
  cleanupToken = ownerToken;

  await api('/auth/refresh', {
    method: 'POST',
    body: { refreshToken: owner.refreshToken }
  });
  ok('JWT refresh token');

  const customer = await api('/customers', {
    method: 'POST',
    token: ownerToken,
    expect: 201,
    body: {
      name: `E2E Klant ${runId}`,
      email: `e2e-${runId}@example.com`,
      phone: '+31 6 11111111',
      address: 'Teststraat 12, Amsterdam'
    }
  });
  created.customers.push(customer.id);
  assert(customer.name.includes('E2E Klant'), 'Customer create failed.');
  ok('Customers create');

  const updatedCustomer = await api(`/customers/${customer.id}`, {
    method: 'PUT',
    token: ownerToken,
    body: {
      name: `E2E Klant Gewijzigd ${runId}`,
      email: `e2e-${runId}@example.com`,
      phone: '+31 6 22222222',
      address: 'Nieuwe Teststraat 34, Amsterdam'
    }
  });
  assert(updatedCustomer.phone.endsWith('22222222'), 'Customer update failed.');
  ok('Customers update');

  const deleteCustomer = await api('/customers', {
    method: 'POST',
    token: ownerToken,
    expect: 201,
    body: {
      name: `E2E Delete ${runId}`,
      email: `delete-${runId}@example.com`,
      phone: '+31 6 33333333',
      address: 'Deletepad 1'
    }
  });
  await api(`/customers/${deleteCustomer.id}`, { method: 'DELETE', token: ownerToken, expect: 204 });
  ok('Customers delete');

  const bookingDate = await freeDate(ownerToken);
  const booking = await api('/bookings', {
    method: 'POST',
    token: ownerToken,
    expect: 201,
    body: {
      customerId: customer.id,
      eventDate: bookingDate,
      date: bookingDate,
      endDate: bookingDate,
      startTime: '18:00',
      endTime: '23:00',
      eventType: 'BRUILOFT',
      guestCount: 88,
      price: 2500,
      status: 'CONCEPT',
      notes: 'E2E testboeking',
      properties: ['Catering inbegrepen', 'Parkeren gratis'],
      conditions: 'E2E aanvullende voorwaarden'
    }
  });
  created.bookings.push(booking.id);
  ok('Bookings create');

  await api('/bookings', {
    method: 'POST',
    token: ownerToken,
    expect: 409,
    body: {
      customerId: customer.id,
      eventDate: bookingDate,
      date: bookingDate,
      endDate: bookingDate,
      startTime: '18:00',
      endTime: '23:00',
      eventType: 'VERJAARDAG',
      guestCount: 40,
      price: 1000,
      status: 'CONCEPT',
      notes: 'Conflict test'
    }
  });
  ok('Bookings one-hall overlap validation');

  const confirmedBooking = await api(`/bookings/${booking.id}/status`, {
    method: 'PATCH',
    token: ownerToken,
    body: { status: 'CONTRACT_ONDERTEKEND' }
  });
  assert(confirmedBooking.status === 'CONTRACT_ONDERTEKEND', 'Booking status change failed.');
  ok('Bookings status change through new booking flow status chain');

  const calendar = await api(`/bookings/calendar?start=${bookingDate}&end=${bookingDate}`, { token: ownerToken });
  assert(calendar.some((entry) => entry.id === booking.id), 'Calendar did not include created booking.');
  ok('Calendar occupied date data');

  const history = await api(`/customers/${customer.id}/bookings`, { token: ownerToken });
  assert(history.some((entry) => entry.id === booking.id), 'Customer history missing booking.');
  ok('Customer booking history');

  const contractBookingDate = await freeDate(ownerToken);
  const contractBooking = await api('/bookings', {
    method: 'POST',
    token: ownerToken,
    expect: 201,
    body: {
      customerId: customer.id,
      eventDate: contractBookingDate,
      date: contractBookingDate,
      endDate: contractBookingDate,
      startTime: '17:00',
      endTime: '23:30',
      eventType: 'BRUILOFT',
      guestCount: 120,
      price: 3200,
      status: 'CONCEPT',
      notes: 'E2E contractboeking',
      properties: ['Catering inbegrepen', 'Bruidskamer beschikbaar'],
      conditions: 'Extra voorwaarden voor het contract.'
    }
  });
  created.bookings.push(contractBooking.id);

  const initialContract = await api(`/bookings/${contractBooking.id}/contract`, { token: ownerToken });
  assert(initialContract.status === 'GEEN', 'New booking should start without contract.');
  const generatedContract = await api(`/bookings/${contractBooking.id}/contract/generate`, {
    method: 'POST',
    token: ownerToken
  });
  assert(generatedContract.status === 'CONCEPT' && generatedContract.html.includes('Huurovereenkomst Evenementenlocatie'), 'Contract generation failed.');
  const editedContract = await api(`/bookings/${contractBooking.id}/contract/concept`, {
    method: 'PUT',
    token: ownerToken,
    body: { html: generatedContract.html.replace('Extra voorwaarden voor het contract.', 'E2E aangepaste voorwaarden.') }
  });
  assert(editedContract.html.includes('E2E aangepaste voorwaarden.'), 'Contract concept save failed.');
  const signedContract = await api(`/bookings/${contractBooking.id}/contract/mark-signed`, {
    method: 'POST',
    token: ownerToken
  });
  assert(signedContract.status === 'ONDERTEKEND', 'Contract mark signed failed.');
  const autoInvoices = await api('/invoices', { token: ownerToken });
  const autoInvoice = autoInvoices.find((entry) => entry.bookingId === contractBooking.id);
  assert(autoInvoice?.status === 'CONCEPT', 'Signed contract should auto-generate concept invoice.');
  created.invoices.push(autoInvoice.id);
  const generatedAutoInvoice = await api(`/invoices/${autoInvoice.id}/generate-pdf`, {
    method: 'POST',
    token: ownerToken
  });
  assert(Boolean(generatedAutoInvoice.pdfPath), 'Auto invoice PDF generation failed.');
  const autoInvoicePdf = await api(`/invoices/${autoInvoice.id}/download`, { token: ownerToken });
  assert(autoInvoicePdf.slice(0, 4).toString() === '%PDF', 'Auto invoice download is not a PDF.');
  const signedPdf = await api(`/bookings/${contractBooking.id}/contract/download-signed`, { token: ownerToken });
  assert(signedPdf.slice(0, 4).toString() === '%PDF', 'Signed contract download is not a PDF.');
  ok('Contract generate, concept save, signed webhook simulation and invoice auto-generation');

  const invoice = await api('/invoices', {
    method: 'POST',
    token: ownerToken,
    expect: 201,
    body: {
      bookingId: booking.id,
      amount: 2500,
      dueDate: isoDate(-1)
    }
  });
  created.invoices.push(invoice.id);
  assert(invoice.invoiceNumber.startsWith(`LUX-${new Date().getFullYear()}-`), 'Invoice number format failed.');
  ok('Invoices create and numbering');

  const pdfInvoice = await api(`/invoices/${invoice.id}/generate-pdf`, {
    method: 'POST',
    token: ownerToken
  });
  assert(Boolean(pdfInvoice.pdfPath), 'PDF path missing after generation.');
  const pdf = await api(`/invoices/${invoice.id}/download`, { token: ownerToken });
  assert(pdf.slice(0, 4).toString() === '%PDF', 'Invoice download is not a PDF.');
  ok('Invoices PDF generation and download');

  await api(`/invoices/${invoice.id}/send-reminder`, { method: 'POST', token: ownerToken });
  ok('Invoices payment reminder endpoint');

  const payment = await api('/payments', {
    method: 'POST',
    token: ownerToken,
    expect: 201,
    body: {
      invoiceId: invoice.id,
      amount: 1000,
      paymentDate: isoDate(0),
      paymentMethod: 'BANK',
      notes: 'E2E aanbetaling'
    }
  });
  created.payments.push(payment.id);
  ok('Payments create');

  const updatedPayment = await api(`/payments/${payment.id}`, {
    method: 'PUT',
    token: ownerToken,
    body: {
      invoiceId: invoice.id,
      amount: 1000,
      paymentDate: isoDate(0),
      paymentMethod: 'PIN',
      notes: 'E2E aanbetaling gewijzigd'
    }
  });
  assert(updatedPayment.paymentMethod === 'PIN', 'Payment update failed.');
  ok('Payments update and invoice reconciliation');

  const paidInvoice = await api(`/invoices/${invoice.id}/mark-paid`, {
    method: 'POST',
    token: ownerToken
  });
  assert(paidInvoice.status === 'BETAALD', 'Mark paid failed.');
  const paymentsAfterMarkPaid = await api('/payments', { token: ownerToken });
  const invoicePayments = paymentsAfterMarkPaid.filter((entry) => entry.invoiceId === invoice.id);
  assert(invoicePayments.length >= 2, 'Mark paid should create a balancing payment record.');
  assert(invoicePayments.some((entry) => entry.notes?.includes('Automatisch geregistreerd')), 'Automatic mark-paid payment note missing.');
  ok('Invoices mark paid creates payment record');

  const stats = await api('/dashboard/stats', { token: ownerToken });
  assert(Array.isArray(stats.revenuePerMonth) && stats.revenuePerMonth.length === 12, 'Dashboard stats incomplete.');
  ok('Dashboard stats');

  const settings = await api('/settings', { token: ownerToken });
  const invoiceTemplate = await api('/settings/invoice-template', { token: ownerToken });
  assert(invoiceTemplate.includes('{{invoiceNumber}}') && invoiceTemplate.includes('{{brandColor}}'), 'Invoice HTML template endpoint incomplete.');
  ok('Settings invoice HTML template endpoint');
  assert(settings.brandColor === '#C9A84C' || settings.brandColor.startsWith('#'), 'Settings brand color missing.');
  assert(Boolean(settings.postalCode) && Boolean(settings.city), 'Settings invoice address fields missing.');
  assert(Boolean(settings.docusealBaseUrl) && Boolean(settings.generalTerms), 'Settings contract fields missing.');
  ok('Settings invoice customization fields');

  const logoSettings = await uploadLogo(ownerToken);
  assert(logoSettings.logoBase64.startsWith('data:image/svg+xml;base64,'), 'Settings logo upload did not store base64 data URL.');
  ok('Settings logo base64 upload');

  const savedSettings = await api('/settings', {
    method: 'PUT',
    token: ownerToken,
    body: {
      ...settingsPayload(settings),
      logoBase64: logoSettings.logoBase64,
      brandColor: '#B8943D'
    }
  });
  assert(savedSettings.companyName === settings.companyName, 'Settings update failed.');
  assert(savedSettings.brandColor === '#B8943D', 'Settings brand color update failed.');
  ok('Settings read and update');
  await api('/settings', { method: 'PUT', token: ownerToken, body: settingsPayload(settings) });
  ok('Settings restore after customization test');

  const user = await api('/users', {
    method: 'POST',
    token: ownerToken,
    expect: 201,
    body: {
      name: `E2E Medewerker ${runId}`,
      email: `employee-${runId}@example.com`,
      password: 'E2ePassword123!',
      role: 'EMPLOYEE'
    }
  });
  created.users.push(user.id);
  const updatedUser = await api(`/users/${user.id}`, {
    method: 'PUT',
    token: ownerToken,
    body: {
      name: `E2E Medewerker Gewijzigd ${runId}`,
      email: `employee-${runId}@example.com`,
      role: 'EMPLOYEE'
    }
  });
  assert(updatedUser.name.includes('Gewijzigd'), 'User update failed.');
  ok('Users create/update');

  await api('/users', { token: employeeToken, expect: 403 });
  await api('/invoices', { token: employeeToken });
  await api('/contracts', { token: employeeToken });
  await api('/dashboard/stats', { token: employeeToken, expect: 403 });
  ok('Role restrictions and employee read access for invoices/contracts');

  const employeeCustomer = await api('/customers', {
    method: 'POST',
    token: employeeToken,
    expect: 201,
    body: {
      name: `E2E Employee Klant ${runId}`,
      email: `employee-customer-${runId}@example.com`,
      phone: '+31 6 44444444',
      address: 'Employeepad 4'
    }
  });
  created.customers.push(employeeCustomer.id);
  const employeeBookingDate = isoDate(80 + (runId % 20));
  const employeeBooking = await api('/bookings', {
    method: 'POST',
    token: employeeToken,
    expect: 201,
    body: {
      customerId: employeeCustomer.id,
      eventDate: employeeBookingDate,
      date: employeeBookingDate,
      endDate: employeeBookingDate,
      startTime: '14:00',
      endTime: '18:00',
      eventType: 'CONGRES',
      guestCount: 32,
      price: 900,
      status: 'CONCEPT',
      notes: 'Employee create test',
      properties: ['Parkeren gratis'],
      conditions: 'Employee E2E voorwaarden'
    }
  });
  created.bookings.push(employeeBooking.id);
  await api(`/bookings/${employeeBooking.id}`, { method: 'DELETE', token: employeeToken, expect: 403 });
  ok('Employee can create bookings but cannot delete');

  return { ownerToken, employeeToken, customer: updatedCustomer, booking, invoice };
}

async function expectVisible(page, text, name = text) {
  await page.getByText(text, { exact: false }).first().waitFor({ state: 'visible', timeout: 15000 });
  ok(`UI ${name}`);
}

async function closeDialog(page) {
  await page.keyboard.press('Escape').catch(() => {});
}

async function uiFlow(seed) {
  const errors = [];
  const browser = await chromium.launch({
    executablePath: CHROME,
    headless: true
  });
  const page = await browser.newPage({ viewport: { width: 1440, height: 950 } });
  page.on('console', (message) => {
    if (message.type() === 'error') {
      errors.push(message.text());
    }
  });
  page.on('pageerror', (error) => errors.push(error.message));

  await page.goto(`${WEB}/login`, { waitUntil: 'domcontentloaded' });
  await page.locator('input[formcontrolname="email"]').fill('owner@partycentrumlux.nl');
  await page.locator('input[type="password"]').fill('LuxAdmin123!');
  await page.getByRole('button', { name: 'Inloggen' }).click();
  await page.waitForURL('**/dashboard', { timeout: 15000 });
  await expectVisible(page, 'Omzet deze maand', 'OWNER dashboard KPIs');
  await expectVisible(page, 'Facturen', 'OWNER nav financial pages');

  const routes = [
    ['/dashboard', 'Dashboard'],
    ['/kalender', 'Kalender'],
    ['/boekingen', 'Boekingen'],
    ['/contracten', 'Contracten'],
    ['/klanten', 'Klanten'],
    ['/facturen', 'Facturen'],
    ['/betalingen', 'Betalingen'],
    ['/medewerkers', 'Medewerkers'],
    ['/instellingen', 'Instellingen']
  ];

  for (const [route, heading] of routes) {
    await page.goto(`${WEB}${route}`, { waitUntil: 'domcontentloaded' });
    await expectVisible(page, heading, `${heading} page`);
  }

  await page.goto(`${WEB}/boekingen`, { waitUntil: 'domcontentloaded' });
  await expectVisible(page, seed.customer.name, 'created booking/customer visible in bookings');
  await page.getByRole('button', { name: 'Facturen openen' }).first().waitFor({ state: 'visible', timeout: 15000 });
  ok('UI booking invoice link action');
  await page.getByRole('button', { name: 'Nieuwe boeking' }).click();
  await expectVisible(page, 'Nieuwe boeking', 'booking modal');
  await closeDialog(page);

  await page.goto(`${WEB}/klanten`, { waitUntil: 'domcontentloaded' });
  await expectVisible(page, seed.customer.name, 'created customer visible in customers');
  await page.getByRole('button', { name: 'Nieuwe klant' }).click();
  await expectVisible(page, 'Nieuwe klant', 'customer modal');
  await closeDialog(page);

  await page.goto(`${WEB}/facturen`, { waitUntil: 'domcontentloaded' });
  await expectVisible(page, seed.invoice.invoiceNumber, 'created invoice visible in invoices');
  await page.getByRole('button', { name: 'Nieuwe factuur' }).click();
  await expectVisible(page, 'Nieuwe factuur', 'invoice modal');
  await closeDialog(page);

  await page.goto(`${WEB}/betalingen`, { waitUntil: 'domcontentloaded' });
  await expectVisible(page, seed.invoice.invoiceNumber, 'created payment visible in payments');
  await page.getByRole('button', { name: 'Nieuwe betaling' }).click();
  await expectVisible(page, 'Nieuwe betaling', 'payment modal');
  await closeDialog(page);

  await page.goto(`${WEB}/medewerkers`, { waitUntil: 'domcontentloaded' });
  await page.getByRole('button', { name: 'Nieuwe medewerker' }).click();
  await expectVisible(page, 'Nieuwe medewerker', 'employee modal');
  await closeDialog(page);

  await page.goto(`${WEB}/kalender`, { waitUntil: 'domcontentloaded' });
  const bookingMonth = new Date(`${seed.booking.eventDate ?? seed.booking.date}T00:00:00`);
  const activeMonth = new Date();
  const monthSteps = (bookingMonth.getFullYear() - activeMonth.getFullYear()) * 12
    + bookingMonth.getMonth() - activeMonth.getMonth();
  if (monthSteps > 0) {
    for (let index = 0; index < monthSteps; index++) {
      await page.locator('.p-datepicker-next').click();
      await page.waitForTimeout(100);
    }
  }
  await expectVisible(page, seed.customer.name, 'calendar shows occupied booking');

  await page.goto(`${WEB}/instellingen`, { waitUntil: 'domcontentloaded' });
  await expectVisible(page, 'Factuur instellingen', 'invoice settings section');
  await expectVisible(page, 'Live voorbeeld', 'invoice live preview');
  const previewFrame = page.frameLocator('iframe[title="Live factuurvoorbeeld"]');
  await previewFrame.locator('.invoice-number-badge').waitFor({ state: 'visible', timeout: 15000 });
  const previewInvoiceNumber = await previewFrame.locator('.invoice-number-badge').textContent();
  assert(previewInvoiceNumber === 'LUX-2026-001', 'Invoice preview dummy number missing.');
  ok('UI invoice preview dummy number');
  await page.locator('input[aria-label="Huisstijl kleur"]').evaluate((input) => {
    input.value = '#b8943d';
    input.dispatchEvent(new Event('input', { bubbles: true }));
    input.dispatchEvent(new Event('change', { bubbles: true }));
  });
  let previewAccent = '';
  for (let attempt = 0; attempt < 12; attempt++) {
    await page.waitForTimeout(150);
    previewAccent = await previewFrame.locator('.invoice-number-badge').evaluate((element) =>
      getComputedStyle(element).backgroundColor
    );
    if (previewAccent === 'rgb(184, 148, 61)') {
      break;
    }
  }
  assert(previewAccent === 'rgb(184, 148, 61)', `Invoice preview color did not update live, got ${previewAccent}.`);
  ok('UI invoice preview live color update');
  await page.getByLabel('Thema wisselen').click();
  await page.waitForTimeout(250);
  const isDark = await page.locator('html').evaluate((html) => html.classList.contains('dark'));
  assert(isDark, 'Dark mode toggle did not set dark class.');
  ok('UI dark mode toggle');

  await page.getByLabel('Uitloggen').click();
  await page.waitForURL('**/login', { timeout: 15000 });
  await page.locator('input[formcontrolname="email"]').fill('employee@partycentrumlux.nl');
  await page.locator('input[type="password"]').fill('LuxEmployee123!');
  await page.getByRole('button', { name: 'Inloggen' }).click();
  await page.waitForURL('**/dashboard', { timeout: 15000 });
  await expectVisible(page, 'Financiele cijfers zijn alleen zichtbaar voor de eigenaar.', 'EMPLOYEE dashboard');
  assert(await page.getByText('Facturen', { exact: true }).count() > 0, 'Employee should see Facturen nav item.');
  await page.goto(`${WEB}/facturen`, { waitUntil: 'domcontentloaded' });
  await expectVisible(page, 'Facturen', 'EMPLOYEE invoices page');
  await page.goto(`${WEB}/betalingen`, { waitUntil: 'domcontentloaded' });
  await page.waitForURL('**/dashboard', { timeout: 15000 });
  ok('UI employee can view invoices but not owner-only payments');

  await browser.close();

  const relevantErrors = errors.filter((error) =>
    !error.includes('favicon') &&
    !error.includes('Failed to load resource: the server responded with a status of 403')
  );
  assert(relevantErrors.length === 0, `Browser console errors: ${relevantErrors.join(' | ')}`);
}

async function main() {
  let ownerToken;
  try {
    const seed = await backendFlow();
    ownerToken = seed.ownerToken;
    await uiFlow(seed);
    ok('End-to-end smoke test complete', `${checks.length} checks`);
  } finally {
    const token = ownerToken ?? cleanupToken;
    if (token) {
      await cleanup(token);
    }
  }
}

main().catch((error) => {
  console.error(`FAILED ${error.message}`);
  process.exit(1);
});
