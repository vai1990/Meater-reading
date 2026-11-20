(() => {
  'use strict';

  const LEGACY_STORAGE_KEY = 'smart_meter_pro_web_v1';
  const USERS_KEY = 'smart_meter_pro_users_v1';
  const SESSION_KEY = 'smart_meter_pro_session_v1';
  const PREF_LANG_KEY = 'smart_meter_pro_lang_v1';

  const FIREBASE_CONFIG = {
    apiKey: 'AIzaSyAMW1Iir84mvLoprAL461YfalV-ISx19j4',
    authDomain: 'gen-lang-client-0644163839.firebaseapp.com',
    projectId: 'gen-lang-client-0644163839',
    storageBucket: 'gen-lang-client-0644163839.firebasestorage.app',
    messagingSenderId: '418787384815',
    appId: '1:418787384815:web:30e8e982723daebe7c2a1a'
  };

  let firebaseAuth = null;

  const THEMES = [
    { id: 'cyber', className: 'theme-cyber', isDark: true, labelMr: 'सायबर डार्क निऑन', labelEn: 'Cyber Dark Neon', start: '#060B14', primary: '#38BDF8' },
    { id: 'emerald', className: 'theme-emerald', isDark: false, labelMr: 'एमराल्ड एनर्जी', labelEn: 'Emerald Energy', start: '#064E3B', primary: '#047857' },
    { id: 'indigo', className: 'theme-indigo', isDark: false, labelMr: 'रॉयल इंडिगो', labelEn: 'Royal Indigo', start: '#1E1B4B', primary: '#4338CA' },
    { id: 'solar', className: 'theme-solar', isDark: false, labelMr: 'सोलर अंबर गोल्ड', labelEn: 'Solar Amber Gold', start: '#78350F', primary: '#B45309' },
    { id: 'blue', className: 'theme-blue', isDark: false, labelMr: 'इलेक्ट्रिक ब्लू', labelEn: 'Electric Blue', start: '#0F172A', primary: '#1D4ED8' }
  ];

  const ICONS = ['⚡', '📟', '🏭', '☀️', '🔋', '🛡️'];

  const SAMPLE_TIRUPATI_CSV = `TIRUPATI CRUSHER,,,,,
DATE,TIME,Kwh,KvArh,KvAh,PF
07-Sep-2026,8:41:00 AM,54654,24897,70422,513
08-Sep-2026,8:15:00 AM,54667,24903,70436,985
09-Sep-2026,10:37:04 AM,54717,24917,70490,992
10-Sep-2026,8:23:00 AM,54746,24926,70520,-984
11-Sep-2026,8:35:04 AM,54763,24934,70538,745
12-Sep-2026,4:31:06 PM,54781,24945,70560,981
13-Sep-2026,8:46:04 AM,54808,24955,70589,990
01-Nov-2026,8:10:04 AM,56179,25696,72227,456
02-Nov-2026,8:31:54 AM,56211,25713,72265,651
03-Nov-2026,8:12:38 AM,56250,25732,72309,653`;

  // ---------- Auth & Per-User Storage Helpers ----------
  function loadUsers() {
    try {
      const raw = localStorage.getItem(USERS_KEY);
      const list = raw ? JSON.parse(raw) : [];
      return Array.isArray(list) ? list : [];
    } catch {
      return [];
    }
  }

  function saveUsers(users) {
    try {
      localStorage.setItem(USERS_KEY, JSON.stringify(users));
    } catch (err) {
      console.error('Users save failed:', err);
    }
  }

  function loadSession() {
    try {
      const raw = localStorage.getItem(SESSION_KEY);
      if (!raw) return null;
      const u = JSON.parse(raw);
      return u && u.id ? u : null;
    } catch {
      return null;
    }
  }

  function saveSession(user) {
    try {
      if (user) localStorage.setItem(SESSION_KEY, JSON.stringify(user));
      else localStorage.removeItem(SESSION_KEY);
    } catch (err) {
      console.error('Session save failed:', err);
    }
  }

  async function hashPassword(plain) {
    const salted = 'smp_v1_' + String(plain || '');
    if (window.crypto && window.crypto.subtle && window.TextEncoder) {
      try {
        const bytes = new TextEncoder().encode(salted);
        const digest = await window.crypto.subtle.digest('SHA-256', bytes);
        return Array.from(new Uint8Array(digest)).map(b => b.toString(16).padStart(2, '0')).join('');
      } catch {
        // Fallback below for non-secure contexts
      }
    }
    let h1 = 0x811c9dc5;
    let h2 = 0x01000193;
    for (let i = 0; i < salted.length; i++) {
      const ch = salted.charCodeAt(i);
      h1 ^= ch;
      h1 = Math.imul(h1, 0x01000193);
      h2 = Math.imul(h2 ^ ch, 0x85ebca6b);
    }
    return 'fb_' + (h1 >>> 0).toString(16).padStart(8, '0') + (h2 >>> 0).toString(16).padStart(8, '0');
  }

  let currentUser = loadSession();
  let authMode = 'login';

  function getUserStorageKey() {
    return currentUser ? `${LEGACY_STORAGE_KEY}_${currentUser.id}` : LEGACY_STORAGE_KEY;
  }

  // ---------- Default Persistent State ----------
  function defaultState() {
    let savedLangMr = true;
    try {
      const l = localStorage.getItem(PREF_LANG_KEY);
      if (l === 'en') savedLangMr = false;
    } catch {}
    return {
      isMr: savedLangMr,
      currentTab: 0, // 0: Dashboard, 1: History, 2: Customers, 3: Backup/Export
      dateFilter: 'ALL',
      sortDesc: true,
      searchQuery: '',
      targetPf: 0.99,
      expandedIds: {},
      branding: {
        appName: 'Smart Meter Pro',
        subtitle: '',
        icon: '⚡',
        themeId: 'cyber'
      },
      sheetConfig: {
        sheetUrl: '',
        tabNamesCsv: '',
        lastSyncTs: 0,
        lastTabsCount: 0,
        lastReadingsCount: 0
      },
      selectedCustomerId: null,
      customers: [],
      readings: []
    };
  }

  let state = loadState();
  let isSheetSyncing = false;

  function loadState() {
    try {
      const key = getUserStorageKey();
      let raw = localStorage.getItem(key);
      // Migrate pre-login legacy data once into the first user who logs in
      if (!raw && currentUser) {
        const legacyRaw = localStorage.getItem(LEGACY_STORAGE_KEY);
        if (legacyRaw) {
          localStorage.setItem(key, legacyRaw);
          localStorage.removeItem(LEGACY_STORAGE_KEY);
          raw = legacyRaw;
        }
      }
      if (!raw) return defaultState();
      const parsed = JSON.parse(raw);
      return {
        ...defaultState(),
        ...parsed,
        branding: { ...defaultState().branding, ...(parsed.branding || {}) },
        sheetConfig: { ...defaultState().sheetConfig, ...(parsed.sheetConfig || {}) },
        customers: Array.isArray(parsed.customers) ? parsed.customers : [],
        readings: Array.isArray(parsed.readings) ? parsed.readings : []
      };
    } catch {
      return defaultState();
    }
  }

  function saveState() {
    try {
      localStorage.setItem(PREF_LANG_KEY, state.isMr ? 'mr' : 'en');
      if (!currentUser) return;
      localStorage.setItem(getUserStorageKey(), JSON.stringify({
        isMr: state.isMr,
        branding: state.branding,
        sheetConfig: state.sheetConfig,
        selectedCustomerId: state.selectedCustomerId,
        customers: state.customers,
        readings: state.readings
      }));
    } catch (err) {
      console.error('Storage save failed:', err);
    }
  }

  // ---------- Power Factor & kVAr Math (100% Parity with Android PowerFactorCalculator.kt) ----------
  function calculatePf(kwh, kvah) {
    if (!kvah || kvah <= 0) return 0;
    const pf = kwh / kvah;
    return Math.min(1, Math.max(0, Math.round(pf * 1000) / 1000));
  }

  function calculateKvahFromKvarh(kwh, kvarh) {
    return Math.round(Math.sqrt(kwh * kwh + kvarh * kvarh) * 100) / 100;
  }

  function calculateAutoPf(kwh, kvarh, kvah) {
    if (!kwh || kwh <= 0) return 0;
    if (kvah >= kwh && kvah > 0) return calculatePf(kwh, kvah);
    if (kvarh > 0) return calculatePf(kwh, calculateKvahFromKvarh(kwh, kvarh));
    if (kvah > 0) return calculatePf(kwh, kvah);
    return 0.95;
  }

  function getPfRating(pf) {
    if (pf >= 0.95) {
      return {
        key: 'EXCELLENT',
        color: '#10B981',
        labelMr: 'उत्कृष्ट (इन्सेंटिव्ह पात्र >= 0.95)',
        labelEn: 'Excellent (>= 0.95)',
        short: 'Good'
      };
    }
    if (pf >= 0.85) {
      return {
        key: 'FAIR',
        color: '#F59E0B',
        labelMr: 'समाधानकारक (सुधारणा आवश्यक 0.85 - 0.94)',
        labelEn: 'Fair (0.85 - 0.94)',
        short: 'Fair'
      };
    }
    return {
      key: 'POOR',
      color: '#EF4444',
      labelMr: 'कमी (दंड / पेनल्टी इशारा < 0.85!)',
      labelEn: 'Low / Penalty (< 0.85)',
      short: 'Low!'
    };
  }

  function recommendStandardBankSize(rawKvar) {
    if (rawKvar <= 0) return 0;
    const std = [1, 2, 3, 5, 7.5, 10, 12.5, 15, 20, 25, 30, 40, 50, 60, 75, 100, 125, 150, 175, 200, 250, 300, 400, 500];
    return std.find(v => v >= rawKvar) || Math.ceil(rawKvar / 50) * 50;
  }

  function generateApfcSteps(total) {
    const steps = [];
    let rem = Math.round(total);
    while (rem > 0) {
      if (rem >= 50) { steps.push(50); rem -= 50; }
      else if (rem >= 25) { steps.push(25); rem -= 25; }
      else if (rem >= 20) { steps.push(20); rem -= 20; }
      else if (rem >= 15) { steps.push(15); rem -= 15; }
      else if (rem >= 10) { steps.push(10); rem -= 10; }
      else if (rem >= 5) { steps.push(5); rem -= 5; }
      else { steps.push(rem); rem = 0; }
    }
    return steps;
  }

  function calculateRequiredKvar(loadKw, initialPf, targetPf = 0.99) {
    const safeLoad = Math.max(0.1, Number(loadKw) || 50);
    const safeInit = Math.min(0.999, Math.max(0.2, Number(initialPf) || 0.85));
    const safeTarget = Math.min(1.0, Math.max(0.2, Number(targetPf) || 0.99));
    if (safeInit >= safeTarget) {
      const kva = Math.round((safeLoad / safeInit) * 100) / 100;
      return { requiredKvar: 0, bankSize: 0, steps: [], initialKva: kva, targetKva: kva, kvaSaved: 0, currentRedPct: 0, multiplierK: 0 };
    }
    const tan1 = Math.sqrt(1 - safeInit * safeInit) / safeInit;
    const tan2 = safeTarget >= 1 ? 0 : Math.sqrt(1 - safeTarget * safeTarget) / safeTarget;
    const k = Math.max(0, tan1 - tan2);
    const rawKvar = Math.max(0, safeLoad * k);
    const requiredKvar = Math.round(rawKvar * 100) / 100;
    const bankSize = recommendStandardBankSize(rawKvar);
    const initialKva = Math.round((safeLoad / safeInit) * 100) / 100;
    const targetKva = Math.round((safeLoad / safeTarget) * 100) / 100;
    const kvaSaved = Math.max(0, Math.round((initialKva - targetKva) * 100) / 100);
    const currentRedPct = Math.max(0, Math.round((1 - safeInit / safeTarget) * 1000) / 10);
    return {
      requiredKvar,
      bankSize,
      steps: generateApfcSteps(bankSize),
      initialKva,
      targetKva,
      kvaSaved,
      currentRedPct,
      multiplierK: Math.round(k * 1000) / 1000
    };
  }

  function parseLoadKw(str) {
    const clean = String(str || '').trim().toLowerCase();
    const num = parseFloat(clean.replace(/[^0-9.]/g, '')) || 50;
    if (clean.includes('hp')) return Math.max(1, Math.round(num * 0.746 * 10) / 10);
    return Math.max(1, num);
  }

  // ---------- Date & Number Parsing Utilities ----------
  const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

  function formatDateDisplay(d) {
    const dd = String(d.getDate()).padStart(2, '0');
    return `${dd}-${MONTHS[d.getMonth()]}-${d.getFullYear()}`;
  }

  function formatTimeDisplay(d) {
    let h = d.getHours();
    const ampm = h >= 12 ? 'PM' : 'AM';
    h = h % 12 || 12;
    const m = String(d.getMinutes()).padStart(2, '0');
    const s = String(d.getSeconds()).padStart(2, '0');
    return `${h}:${m}:${s} ${ampm}`;
  }

  function parseNumber(val) {
    if (typeof val === 'number' && !isNaN(val)) return val;
    const clean = String(val || '').trim().replace(/,/g, '');
    if (!clean) return null;
    const direct = Number(clean);
    if (!isNaN(direct)) return direct;
    const m = clean.match(/[-+]?\d*\.?\d+/);
    return m ? Number(m[0]) : null;
  }

  function parseFlexibleDate(raw) {
    if (raw instanceof Date && !isNaN(raw.getTime())) return raw;
    const s = String(raw || '').trim();
    if (!s) return new Date();

    // Excel serial date
    if (/^\d{4,6}(\.\d+)?$/.test(s)) {
      const serial = Number(s);
      if (serial >= 1000 && serial <= 100000) {
        return new Date(Date.UTC(1899, 11, 30) + Math.floor(serial) * 86400000);
      }
    }

    // dd-MMM-yyyy or dd-MMM-yy
    const mMon = s.match(/^(\d{1,2})[-\s/]([A-Za-z]{3,})[-\s/](\d{2,4})$/);
    if (mMon) {
      const day = parseInt(mMon[1], 10);
      const monIdx = MONTHS.findIndex(m => m.toLowerCase() === mMon[2].slice(0, 3).toLowerCase());
      let yr = parseInt(mMon[3], 10);
      if (yr < 100) yr += 2000;
      if (monIdx >= 0) return new Date(yr, monIdx, day);
    }

    // yyyy-mm-dd
    const mIso = s.match(/^(\d{4})[-/](\d{1,2})[-/](\d{1,2})/);
    if (mIso) {
      return new Date(parseInt(mIso[1], 10), parseInt(mIso[2], 10) - 1, parseInt(mIso[3], 10));
    }

    // dd/mm/yyyy or dd-mm-yy
    const mDmy = s.match(/^(\d{1,2})[-/.](\d{1,2})[-/.](\d{2,4})$/);
    if (mDmy) {
      let d = parseInt(mDmy[1], 10);
      let mo = parseInt(mDmy[2], 10);
      let yr = parseInt(mDmy[3], 10);
      if (yr < 100) yr += 2000;
      if (mo > 12 && d <= 12) [d, mo] = [mo, d];
      return new Date(yr, mo - 1, d);
    }

    const parsed = new Date(s);
    return isNaN(parsed.getTime()) ? new Date() : parsed;
  }

  function normalizeTime(raw) {
    if (typeof raw === 'number' && raw >= 0 && raw < 100000) {
      const frac = raw - Math.floor(raw);
      const totalSec = Math.round(frac * 86400);
      const hrs = Math.floor(totalSec / 3600) % 24;
      const mins = Math.floor((totalSec % 3600) / 60);
      const secs = totalSec % 60;
      const d = new Date();
      d.setHours(hrs, mins, secs, 0);
      return formatTimeDisplay(d);
    }
    const s = String(raw || '').trim();
    if (!s) return '08:30:00 AM';
    if (/^0\.\d+$/.test(s)) return normalizeTime(Number(s));
    return s;
  }

  function dateTimeToMillis(dateStr, timeStr) {
    const d = parseFlexibleDate(dateStr);
    const t = String(timeStr || '').trim();
    const m12 = t.match(/^(\d{1,2}):(\d{2})(?::(\d{2}))?\s*(AM|PM)$/i);
    if (m12) {
      let h = parseInt(m12[1], 10);
      const min = parseInt(m12[2], 10);
      const sec = parseInt(m12[3] || '0', 10);
      const ap = m12[4].toUpperCase();
      if (ap === 'PM' && h < 12) h += 12;
      if (ap === 'AM' && h === 12) h = 0;
      d.setHours(h, min, sec, 0);
      return d.getTime();
    }
    const m24 = t.match(/^(\d{1,2}):(\d{2})(?::(\d{2}))?$/);
    if (m24) {
      d.setHours(parseInt(m24[1], 10), parseInt(m24[2], 10), parseInt(m24[3] || '0', 10), 0);
      return d.getTime();
    }
    d.setHours(8, 30, 0, 0);
    return d.getTime();
  }

  // ---------- CSV & Google Sheet Parsing (PF -> Remark + Auto-Calculated PF) ----------
  function parseCsvLines(csvText) {
    const rows = [];
    let row = [];
    let cell = '';
    let inQuotes = false;
    const text = csvText.replace(/^\uFEFF/, '');
    for (let i = 0; i < text.length; i++) {
      const ch = text[i];
      if (inQuotes) {
        if (ch === '"' && text[i + 1] === '"') { cell += '"'; i++; }
        else if (ch === '"') inQuotes = false;
        else cell += ch;
      } else {
        if (ch === '"') inQuotes = true;
        else if (ch === ',') { row.push(cell.trim()); cell = ''; }
        else if (ch === '\n' || ch === '\r') {
          if (ch === '\r' && text[i + 1] === '\n') i++;
          row.push(cell.trim());
          if (row.some(c => c !== '')) rows.push(row);
          row = [];
          cell = '';
        } else cell += ch;
      }
    }
    if (cell.length > 0 || row.length > 0) {
      row.push(cell.trim());
      if (row.some(c => c !== '')) rows.push(row);
    }
    return rows;
  }

  function buildSheetRemarks(rawPfStr, rawRemarkStr) {
    const parts = [];
    const pfClean = String(rawPfStr || '').trim();
    const remClean = String(rawRemarkStr || '').trim();
    if (pfClean) {
      parts.push(/^pf/i.test(pfClean) ? pfClean : `PF: ${pfClean}`);
    }
    if (remClean && remClean.toLowerCase() !== pfClean.toLowerCase()) {
      if (!pfClean && parseNumber(remClean) !== null && !/^pf/i.test(remClean)) {
        parts.push(`PF: ${remClean}`);
      } else {
        parts.push(remClean);
      }
    }
    return parts.join(' • ');
  }

  function parseRowsForCustomerTab(rows, fallbackName = 'Sheet Customer') {
    const nonEmpty = rows.filter(r => Array.isArray(r) && r.some(c => String(c || '').trim() !== ''));
    if (!nonEmpty.length) throw new Error(`शीटमध्ये डेटा नाही (${fallbackName})`);

    let headerIdx = nonEmpty.findIndex(r => {
      const n = r.map(c => String(c || '').trim().toLowerCase());
      return n.some(c => c === 'date' || c.includes('reading date') || c.includes('दिनांक')) &&
        n.some(c => c.includes('kwh'));
    });

    if (headerIdx === -1) {
      headerIdx = nonEmpty.findIndex(r => {
        const n = r.map(c => String(c || '').trim().toLowerCase());
        return n.some(c => c.includes('kwh')) && (n.some(c => c.includes('kvarh')) || n.some(c => c.includes('kvah')) || n.some(c => c.includes('date')));
      });
    }

    if (headerIdx === -1) {
      throw new Error(`शीट '${fallbackName}' मध्ये DATE, TIME, Kwh, KvArh, KvAh, PF कॉलम्स सापडले नाहीत.`);
    }

    let customerName = fallbackName.trim();
    for (let i = 0; i < headerIdx; i++) {
      const cand = nonEmpty[i].map(c => String(c || '').trim()).find(Boolean);
      if (cand && cand.toUpperCase() !== 'DATE' && !cand.toUpperCase().includes('METERREAD')) {
        customerName = cand;
        break;
      }
    }
    if (!customerName) customerName = 'Sheet Customer';

    const headers = nonEmpty[headerIdx].map(c => String(c || '').trim().toLowerCase());
    const findCol = (...kws) => {
      for (const kw of kws) {
        const ex = headers.findIndex(h => h === kw);
        if (ex !== -1) return ex;
      }
      for (const kw of kws) {
        const sub = headers.findIndex(h => h.includes(kw));
        if (sub !== -1) return sub;
      }
      return -1;
    };

    const colDate = findCol('date', 'reading date', 'दिनांक', 'तारीख', 'dt');
    const colTime = findCol('time', 'reading time', 'वेळ', 'समय');
    const colKwh = findCol('kwh', 'current kwh', 'active energy');
    const colKvarh = findCol('kvarh', 'reactive');
    const colKvah = findCol('kvah', 'apparent');
    const colPf = findCol('pf', 'power factor', 'p.f', 'p.f.', 'cos');
    const colRemarks = findCol('remark', 'remarks', 'note', 'notes', 'टिप्पणी', 'शेरा');

    const readings = [];
    for (let i = headerIdx + 1; i < nonEmpty.length; i++) {
      const r = nonEmpty[i];
      const cell = idx => (idx >= 0 && idx < r.length && r[idx] != null ? String(r[idx]).trim() : '');
      const kwh = parseNumber(cell(colKwh));
      if (kwh === null) continue;

      const kvarh = parseNumber(cell(colKvarh)) ?? 0;
      const kvah = parseNumber(cell(colKvah)) ?? calculateKvahFromKvarh(kwh, kvarh);
      const rawPf = cell(colPf);
      const rawRem = cell(colRemarks);

      // Core rule: Auto-calculate PF + store Sheet PF in Remarks
      const autoPf = calculateAutoPf(kwh, kvarh, kvah);
      const remarks = buildSheetRemarks(rawPf, rawRem);

      const dObj = parseFlexibleDate(cell(colDate));
      const readingDate = formatDateDisplay(dObj);
      const readingTime = normalizeTime(colTime >= 0 ? r[colTime] : '');
      const timestamp = dateTimeToMillis(readingDate, readingTime);

      readings.push({
        id: 'r_' + Math.random().toString(36).slice(2, 10) + '_' + i,
        readingDate,
        readingTime,
        timestamp,
        kwh,
        kvarh,
        kvah,
        powerFactor: autoPf,
        isPfAutoCalculated: true,
        previousKwh: null,
        unitsConsumed: null,
        remarks
      });
    }

    readings.sort((a, b) => a.timestamp - b.timestamp);
    for (let i = 1; i < readings.length; i++) {
      const prev = readings[i - 1].kwh;
      readings[i].previousKwh = prev;
      readings[i].unitsConsumed = readings[i].kwh >= prev ? Math.round((readings[i].kwh - prev) * 100) / 100 : null;
    }

    const suffix = customerName.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 6) || 'SHEET';
    return {
      tabName: customerName,
      customer: {
        id: 'c_' + Math.random().toString(36).slice(2, 10),
        name: customerName,
        address: `Google Sheet Sync (${fallbackName})`,
        meterNumber: `SHT-${suffix}`,
        consumerNumber: '',
        phoneNumber: '',
        connectionType: '3-Phase Industrial',
        sanctionedLoad: '65 kW'
      },
      readings
    };
  }

  // ---------- Browser CORS-Free Google Sheet Sync via JSONP (gviz) + CSV fallback ----------
  function extractSheetMeta(url) {
    const idMatch = url.match(/spreadsheets(?:\/u\/\d+)?\/d\/(?!e\/)([a-zA-Z0-9-_]+)/i);
    const pubMatch = url.match(/spreadsheets(?:\/u\/\d+)?\/d\/e\/([a-zA-Z0-9-_]+)/i);
    const gidMatch = url.match(/[#&?]gid=([0-9]+)/i);
    return {
      spreadsheetId: idMatch ? idMatch[1] : null,
      publishedId: pubMatch ? pubMatch[1] : null,
      gid: gidMatch ? gidMatch[1] : null
    };
  }

  function fetchGvizJsonp(spreadsheetId, { sheetName = null, gid = null } = {}) {
    return new Promise((resolve, reject) => {
      const cbName = '__smpGvizCb_' + Date.now() + '_' + Math.floor(Math.random() * 10000);
      const params = new URLSearchParams();
      params.set('tqx', `out:json;responseHandler:${cbName}`);
      if (sheetName) params.set('sheet', sheetName);
      else if (gid) params.set('gid', gid);
      params.set('_cb', String(Date.now()));

      const script = document.createElement('script');
      const timeout = setTimeout(() => {
        cleanup();
        reject(new Error('Google Sheet प्रतिसाद वेळ संपली (Timeout). Sheet Share → "Anyone with the link" तपासा.'));
      }, 15000);

      function cleanup() {
        clearTimeout(timeout);
        delete window[cbName];
        if (script.parentNode) script.parentNode.removeChild(script);
      }

      window[cbName] = resp => {
        cleanup();
        if (!resp || resp.status === 'error' || !resp.table) {
          const msg = resp?.errors?.[0]?.detailed_message || 'Google Sheet वाचता आली नाही. Sharing "Anyone with the link (Viewer)" ठेवा.';
          reject(new Error(msg));
          return;
        }
        resolve(gvizTableToRows(resp.table));
      };

      script.onerror = () => {
        cleanup();
        reject(new Error('Google Sheet लोड झाली नाही. कृपया इंटरनेट कनेक्शन आणि "Anyone with the link" परवानगी तपासा.'));
      };

      script.src = `https://docs.google.com/spreadsheets/d/${spreadsheetId}/gviz/tq?${params.toString()}`;
      document.head.appendChild(script);
    });
  }

  function gvizTableToRows(table) {
    const rows = [];
    const colLabels = (table.cols || []).map(c => (c.label || '').trim());
    if (colLabels.some(Boolean)) {
      rows.push(colLabels);
    }
    for (const r of (table.rows || [])) {
      const cells = (r.c || []).map(cell => {
        if (!cell) return '';
        if (cell.f != null && String(cell.f).trim() !== '') return String(cell.f).trim();
        if (cell.v == null) return '';
        if (typeof cell.v === 'string' && cell.v.startsWith('Date(')) {
          const m = cell.v.match(/Date\((\d+),(\d+),(\d+)(?:,(\d+),(\d+),(\d+))?\)/);
          if (m) {
            const yr = parseInt(m[1], 10);
            const mo = parseInt(m[2], 10);
            const dy = parseInt(m[3], 10);
            if (yr === 1899 && m[4] !== undefined) {
              const dt = new Date(2000, 0, 1, parseInt(m[4], 10), parseInt(m[5], 10), parseInt(m[6], 10));
              return formatTimeDisplay(dt);
            }
            return formatDateDisplay(new Date(yr, mo, dy));
          }
        }
        return String(cell.v).trim();
      });
      if (cells.some(Boolean)) rows.push(cells);
    }
    return rows;
  }

  async function syncGoogleSheetFromUrl(rawUrl, tabNamesCsv = '', isSilent = false) {
    const url = rawUrl.trim();
    if (!/^https?:\/\//i.test(url)) {
      showToast(t('वैध https:// Google Sheet लिंक टाका', 'Please enter a valid https:// Google Sheet URL'));
      return;
    }

    isSheetSyncing = true;
    render();

    try {
      const { spreadsheetId, publishedId, gid } = extractSheetMeta(url);
      const explicitTabs = tabNamesCsv.split(/[,;\n]/).map(s => s.trim()).filter(Boolean);
      const parsedTabs = [];

      if (spreadsheetId) {
        if (explicitTabs.length > 0) {
          for (const tabName of explicitTabs) {
            const rows = await fetchGvizJsonp(spreadsheetId, { sheetName: tabName });
            const parsed = parseRowsForCustomerTab(rows, tabName);
            if (parsed.readings.length > 0) parsedTabs.push(parsed);
          }
        } else {
          const rows = await fetchGvizJsonp(spreadsheetId, { gid });
          const parsed = parseRowsForCustomerTab(rows, 'Sheet Customer');
          if (parsed.readings.length > 0) parsedTabs.push(parsed);
        }
      } else if (publishedId || /output=csv|\.csv/i.test(url)) {
        const csvUrl = publishedId
          ? (gid
            ? `https://docs.google.com/spreadsheets/d/e/${publishedId}/pub?gid=${gid}&single=true&output=csv&_cb=${Date.now()}`
            : `https://docs.google.com/spreadsheets/d/e/${publishedId}/pub?output=csv&_cb=${Date.now()}`)
          : `${url}${url.includes('?') ? '&' : '?'}_cb=${Date.now()}`;
        const res = await fetch(csvUrl, { cache: 'no-store' });
        if (!res.ok) throw new Error(`HTTP ${res.status} — शीट लिंक तपासणे आवश्यक आहे.`);
        const text = await res.text();
        if (/<!doctype html|<html/i.test(text.trim().slice(0, 200))) {
          throw new Error('Sheet खाजगी (Private) आहे. कृपया Share → "Anyone with the link" निवडा.');
        }
        const parsed = parseRowsForCustomerTab(parseCsvLines(text), 'Sheet Customer');
        if (parsed.readings.length > 0) parsedTabs.push(parsed);
      } else {
        throw new Error('Google Sheet लिंक ओळखली गेली नाही. कृपया https://docs.google.com/spreadsheets/d/... लिंक वापरा.');
      }

      if (!parsedTabs.length) {
        throw new Error('शीटमध्ये वैध मीटर रीडिंग नोंदी सापडल्या नाहीत.');
      }

      const { custCreated, readUpserted } = mergeParsedTabs(parsedTabs);
      const totalRows = parsedTabs.reduce((acc, tab) => acc + tab.readings.length, 0);
      const latestEntry = parsedTabs.flatMap(tab => tab.readings).sort((a, b) => b.timestamp - a.timestamp)[0];

      state.sheetConfig = {
        sheetUrl: url,
        tabNamesCsv: tabNamesCsv.trim(),
        lastSyncTs: Date.now(),
        lastTabsCount: parsedTabs.length,
        lastReadingsCount: totalRows
      };
      saveState();
      closeModal();

      if (!isSilent || readUpserted > 0 || custCreated > 0) {
        const latestDate = latestEntry ? latestEntry.readingDate : '-';
        if (readUpserted === 0 && custCreated === 0) {
          showToast(t(
            `शीट सिंक पूर्ण (${parsedTabs.length} टॅब, ${totalRows} नोंदी • शेवटची: ${latestDate}). कोणतीही नवीन एन्ट्री नाही.`,
            `Sync complete (${parsedTabs.length} tab, ${totalRows} rows • latest: ${latestDate}). No new entries.`
          ));
        } else {
          showToast(t(
            `Google Sheet सिंक पूर्ण! ${readUpserted} नवीन/अपडेटेड एन्ट्री सिंक झाल्या (शेवटची: ${latestDate}).`,
            `Synced! ${readUpserted} new/updated entries (latest: ${latestDate}).`
          ));
        }
      }
    } catch (err) {
      console.error(err);
      if (!isSilent) {
        showToast(t(`सिंक त्रुटी: ${err.message}`, `Sync Error: ${err.message}`));
      }
    } finally {
      isSheetSyncing = false;
      render();
    }
  }

  function handleLocalSheetFile(file) {
    if (!file) return;
    const reader = new FileReader();
    const isXlsx = /\.xlsx?$/i.test(file.name);

    reader.onload = e => {
      try {
        const parsedTabs = [];
        if (isXlsx && window.XLSX) {
          const wb = window.XLSX.read(new Uint8Array(e.target.result), { type: 'array' });
          for (const sheetName of wb.SheetNames) {
            const sheet = wb.Sheets[sheetName];
            const rows = window.XLSX.utils.sheet_to_json(sheet, { header: 1, raw: false, defval: '' });
            try {
              const parsed = parseRowsForCustomerTab(rows, sheetName);
              if (parsed.readings.length > 0) parsedTabs.push(parsed);
            } catch {
              // Skip non-reading tabs
            }
          }
        } else {
          const text = typeof e.target.result === 'string'
            ? e.target.result
            : new TextDecoder('utf-8').decode(e.target.result);
          const parsed = parseRowsForCustomerTab(parseCsvLines(text), file.name.replace(/\.[^.]+$/, ''));
          if (parsed.readings.length > 0) parsedTabs.push(parsed);
        }

        if (!parsedTabs.length) {
          showToast(t('फाईलमध्ये वैध मीटर रीडिंग डेटा सापडला नाही.', 'No valid meter reading data found in file.'));
          return;
        }

        const { readUpserted } = mergeParsedTabs(parsedTabs);
        const totalRows = parsedTabs.reduce((s, x) => s + x.readings.length, 0);
        state.sheetConfig.lastSyncTs = Date.now();
        state.sheetConfig.lastTabsCount = parsedTabs.length;
        state.sheetConfig.lastReadingsCount = totalRows;
        saveState();
        closeModal();
        render();
        showToast(t(
          `फाईल सिंक पूर्ण! ${parsedTabs.length} ग्राहक टॅब, ${readUpserted} नवीन एन्ट्री जोडल्या.`,
          `File imported! ${parsedTabs.length} tabs, ${readUpserted} entries synced.`
        ));
      } catch (err) {
        showToast(t(`फाईल त्रुटी: ${err.message}`, `File Error: ${err.message}`));
      }
    };

    if (isXlsx) reader.readAsArrayBuffer(file);
    else reader.readAsText(file);
  }

  function loadOfflineSampleSheet() {
    const tab = parseRowsForCustomerTab(parseCsvLines(SAMPLE_TIRUPATI_CSV), 'TIRUPATI CRUSHER');
    const { readUpserted } = mergeParsedTabs([tab]);
    saveState();
    closeModal();
    render();
    showToast(t(
      `नमुना शीट लोड झाली! TIRUPATI CRUSHER — ${readUpserted} नोंदी जोडल्या.`,
      `Sample sheet loaded! TIRUPATI CRUSHER — ${readUpserted} entries added.`
    ));
  }

  function mergeParsedTabs(tabs) {
    let custCreated = 0;
    let readUpserted = 0;
    let firstCustId = null;

    for (const tab of tabs) {
      let existingCust = state.customers.find(
        c => c.name.trim().toLowerCase() === tab.customer.name.trim().toLowerCase()
      );
      if (!existingCust && tabs.length === 1 && tab.customer.name.toLowerCase() === 'sheet customer') {
        existingCust = state.customers.find(c => c.id === state.selectedCustomerId) ||
          (state.customers.length === 1 ? state.customers[0] : null);
      }

      let targetCust;
      if (existingCust) {
        targetCust = existingCust;
      } else {
        targetCust = { ...tab.customer };
        state.customers.push(targetCust);
        custCreated++;
      }
      if (!firstCustId) firstCustId = targetCust.id;

      const custReadings = state.readings.filter(r => r.customerId === targetCust.id);
      const matchedIds = new Set();

      for (const sr of tab.readings.sort((a, b) => a.timestamp - b.timestamp)) {
        const dup = custReadings.find(lr =>
          !matchedIds.has(lr.id) &&
          lr.readingDate.toLowerCase() === sr.readingDate.toLowerCase() &&
          Math.abs(lr.kwh - sr.kwh) < 0.01
        ) || custReadings.find(lr =>
          !matchedIds.has(lr.id) &&
          lr.readingDate.toLowerCase() === sr.readingDate.toLowerCase() &&
          sr.readingTime !== '08:30:00 AM' &&
          lr.readingTime.toLowerCase() === sr.readingTime.toLowerCase()
        );

        if (dup) {
          matchedIds.add(dup.id);
          const changed =
            Math.abs(dup.kwh - sr.kwh) >= 0.01 ||
            Math.abs(dup.kvarh - sr.kvarh) >= 0.01 ||
            Math.abs(dup.kvah - sr.kvah) >= 0.01 ||
            Math.abs(dup.powerFactor - sr.powerFactor) >= 0.001 ||
            (sr.remarks && dup.remarks !== sr.remarks);
          if (changed) {
            dup.readingTime = sr.readingTime;
            dup.kwh = sr.kwh;
            dup.kvarh = sr.kvarh;
            dup.kvah = sr.kvah;
            dup.powerFactor = sr.powerFactor;
            dup.isPfAutoCalculated = true;
            if (sr.remarks) dup.remarks = sr.remarks;
            readUpserted++;
          }
        } else {
          const item = { ...sr, customerId: targetCust.id };
          state.readings.push(item);
          custReadings.push(item);
          matchedIds.add(item.id);
          readUpserted++;
        }
      }
      recalculateCustomerDeltas(targetCust.id);
    }

    if (!state.selectedCustomerId || !state.customers.some(c => c.id === state.selectedCustomerId)) {
      state.selectedCustomerId = firstCustId || state.customers[0]?.id || null;
    }
    return { custCreated, readUpserted };
  }

  function recalculateCustomerDeltas(customerId) {
    const list = state.readings
      .filter(r => r.customerId === customerId)
      .sort((a, b) => a.timestamp - b.timestamp);
    for (let i = 0; i < list.length; i++) {
      if (i === 0) {
        list[i].previousKwh = null;
        list[i].unitsConsumed = null;
      } else {
        const prev = list[i - 1].kwh;
        list[i].previousKwh = prev;
        list[i].unitsConsumed = list[i].kwh >= prev ? Math.round((list[i].kwh - prev) * 100) / 100 : null;
      }
    }
  }

  // ---------- Helpers ----------
  function t(mr, en) {
    return state.isMr ? mr : en;
  }

  function esc(str) {
    return String(str ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  function getSelectedCustomer() {
    if (!state.customers.length) return null;
    return state.customers.find(c => c.id === state.selectedCustomerId) || state.customers[0];
  }

  function getCustomerReadings(customerId) {
    return state.readings
      .filter(r => r.customerId === customerId)
      .sort((a, b) => b.timestamp - a.timestamp);
  }

  function getFilteredReadings(customerId) {
    let list = getCustomerReadings(customerId);
    const now = new Date();
    if (state.dateFilter === 'THIS_MONTH') {
      const start = new Date(now.getFullYear(), now.getMonth(), 1).getTime();
      list = list.filter(r => r.timestamp >= start);
    } else if (state.dateFilter === 'LAST_7_DAYS') {
      const seven = Date.now() - 7 * 86400000;
      list = list.filter(r => r.timestamp >= seven);
    }
    const q = state.searchQuery.trim().toLowerCase();
    if (q) {
      list = list.filter(r =>
        r.readingDate.toLowerCase().includes(q) ||
        (r.remarks || '').toLowerCase().includes(q) ||
        String(r.kwh).includes(q)
      );
    }
    return state.sortDesc
      ? list.sort((a, b) => b.timestamp - a.timestamp)
      : list.sort((a, b) => a.timestamp - b.timestamp);
  }

  let toastTimer = null;
  function showToast(message) {
    const el = document.getElementById('toast');
    el.textContent = message;
    el.classList.remove('hidden');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.add('hidden'), 4200);
  }

  function applyTheme() {
    const theme = THEMES.find(th => th.id === state.branding.themeId) || THEMES[0];
    document.body.className = theme.className;
    document.getElementById('brandTitleDisplay').textContent = state.branding.appName || 'Smart Meter Pro';
    document.getElementById('brandIconDisplay').textContent = state.branding.icon || '⚡';
    const subs = [
      t('दैनिक मीटर रीडिंग डॅशबोर्ड', 'Daily Meter Reading Dashboard'),
      t('मागील मीटर रिडिंग्स (उतरता क्रम)', 'Past Meter Readings'),
      t('ग्राहक निर्देशिका व मीटर', 'Customer Profiles & Meters'),
      t('डेटा बॅकअप, रिस्टोर व एक्सपोर्ट', 'Backup, Restore & Export')
    ];
    document.getElementById('brandSubtitleDisplay').textContent = state.branding.subtitle || subs[state.currentTab];
    document.getElementById('darkModeToggleBtn').textContent = theme.isDark ? '☀️' : '🌙';
    document.getElementById('langToggleBtn').textContent = state.isMr ? 'EN' : 'म/हिं';
    document.getElementById('fabLabel').textContent = t('नवीन रिडिंग नोंदवा', 'Log Reading');
    document.getElementById('addReadingFab').style.display = state.currentTab === 0 ? 'inline-flex' : 'none';
    const pillName = document.getElementById('userPillName');
    if (pillName && currentUser) {
      pillName.textContent = currentUser.name || currentUser.email || 'User';
    }
  }

  // ---------- Rendering ----------
  function renderNav() {
    const tabs = [
      { id: 0, icon: '📊', label: t('डॅशबोर्ड', 'Dashboard') },
      { id: 1, icon: '🕒', label: t('मागील नोंदी', 'History') },
      { id: 2, icon: '👥', label: t('ग्राहक', 'Customers') },
      { id: 3, icon: '💾', label: t('बॅकअप/रिस्टोर', 'Backup') }
    ];

    document.getElementById('sideNav').innerHTML = tabs.map(tab => `
      <button class="side-nav-item ${state.currentTab === tab.id ? 'active' : ''}" data-tab="${tab.id}" type="button">
        <span>${tab.icon}</span><span>${esc(tab.label)}</span>
      </button>
    `).join('');

    document.getElementById('bottomNav').innerHTML = tabs.map(tab => `
      <button class="bottom-nav-item ${state.currentTab === tab.id ? 'active' : ''}" data-tab="${tab.id}" type="button">
        <span class="bottom-nav-icon">${tab.icon}</span>
        <span>${esc(tab.label)}</span>
      </button>
    `).join('');

    document.querySelectorAll('[data-tab]').forEach(btn => {
      btn.addEventListener('click', () => {
        state.currentTab = Number(btn.dataset.tab);
        render();
      });
    });
  }

  function renderGaugeHtml(latestReading, customer) {
    const pf = latestReading.powerFactor;
    const rating = getPfRating(pf);
    const loadKw = parseLoadKw(customer?.sanctionedLoad || '50 kW');
    const kvar = calculateRequiredKvar(loadKw, pf, state.targetPf);
    const arcLen = 251.2; // pi * r (r=80)
    const dashOffset = arcLen * (1 - Math.min(1, Math.max(0, pf)));

    return `
      <div class="card">
        <div class="card-header">
          <div class="card-title">⚡ ${t('पॉवर फॅक्टर स्थिती (Power Factor)', 'Power Factor Status')}</div>
          <span style="font-size:11px;color:var(--text-subtle);font-weight:700;">${t('आदर्श: 0.950 - 1.000', 'Ideal: 0.950 - 1.000')}</span>
        </div>
        <div class="gauge-wrap">
          <div class="gauge-svg-box">
            <svg viewBox="0 0 200 112" width="220" height="124">
              <path d="M 20 100 A 80 80 0 0 1 180 100" fill="none" stroke="var(--surface-variant)" stroke-width="16" stroke-linecap="round" />
              <path d="M 20 100 A 80 80 0 0 1 180 100" fill="none" stroke="${rating.color}" stroke-width="16" stroke-linecap="round"
                stroke-dasharray="${arcLen}" stroke-dashoffset="${dashOffset.toFixed(1)}" style="transition:stroke-dashoffset 0.6s ease;" />
            </svg>
            <div class="gauge-readout">
              <div class="gauge-val mono" style="color:${rating.color}">${pf.toFixed(3)}</div>
              <div class="gauge-sub">Cos Φ (Auto PF)</div>
            </div>
          </div>

          <div class="pf-pill-badge" style="color:${rating.color};background:${rating.color}20;border-color:${rating.color}55;">
            ${rating.key === 'POOR' ? '⚠️' : '✅'} ${esc(state.isMr ? rating.labelMr : rating.labelEn)}
          </div>

          <div class="kvar-banner">
            <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;flex-wrap:wrap;">
              <div>
                <div style="font-size:11.5px;font-weight:700;color:var(--text-muted);">
                  ${t(`आवश्यक kVAr (Target ${state.targetPf.toFixed(2)} PF साठी):`, `Required kVAr (Target ${state.targetPf.toFixed(2)} PF):`)}
                </div>
                <div style="display:flex;align-items:baseline;gap:8px;margin-top:2px;">
                  <span class="mono" style="font-size:17px;font-weight:800;color:var(--primary);">~${kvar.requiredKvar.toFixed(1)} kVAr</span>
                  <span style="font-size:11.5px;color:var(--text-subtle);font-weight:700;">(${kvar.bankSize} kVAr ${t('बँक', 'Bank')})</span>
                </div>
              </div>
              <button class="btn btn-outline btn-sm" type="button" data-action="open-kvar" data-reading-id="${esc(latestReading.id)}">
                ⚡ ${t('कॅल्क्युलेटर', 'Calculator')}
              </button>
            </div>
            <div class="target-pf-row">
              <span style="font-size:11px;font-weight:700;color:var(--text-subtle);">Target PF:</span>
              ${[0.95, 0.98, 0.99, 1.00].map(val => `
                <button class="target-chip mono ${Math.abs(state.targetPf - val) < 0.002 ? 'active' : ''}" type="button" data-target-pf="${val}">
                  ${val.toFixed(2)}
                </button>
              `).join('')}
            </div>
          </div>
        </div>
      </div>
    `;
  }

  function renderSummaryHtml(readings) {
    const totalUnits = readings.reduce((s, r) => s + (r.unitsConsumed || 0), 0);
    const avgPf = readings.length ? readings.reduce((s, r) => s + r.powerFactor, 0) / readings.length : 0;
    const latestKwh = readings[0]?.kwh || 0;
    return `
      <div class="card">
        <div class="card-header">
          <div class="card-title">📈 ${t('एकूण वीज वापर सारांश', 'Energy Consumption Summary')}</div>
        </div>
        <div class="kpi-grid">
          <div class="kpi-box">
            <div class="kpi-label">${t('एकूण नोंदी', 'Total Readings')}</div>
            <div class="kpi-value mono">${readings.length}</div>
          </div>
          <div class="kpi-box">
            <div class="kpi-label">${t('एकूण युनिट्स', 'Total Units')}</div>
            <div class="kpi-value mono" style="color:#10B981;">${totalUnits.toFixed(1)}</div>
          </div>
          <div class="kpi-box">
            <div class="kpi-label">${t('सरासरी PF', 'Avg Power Factor')}</div>
            <div class="kpi-value mono" style="color:${getPfRating(avgPf).color};">${avgPf.toFixed(3)}</div>
          </div>
          <div class="kpi-box">
            <div class="kpi-label">${t('शेवटचे kWh', 'Latest kWh')}</div>
            <div class="kpi-value mono">${latestKwh.toFixed(1)}</div>
          </div>
        </div>
      </div>
    `;
  }

  function renderReadingCardHtml(r) {
    const rating = getPfRating(r.powerFactor);
    const isExp = !!state.expandedIds[r.id];
    return `
      <div class="reading-card" data-toggle-reading="${esc(r.id)}">
        <div class="reading-strip" style="background:${rating.color};"></div>
        <div class="reading-body">
          <div class="reading-top">
            <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
              <span class="date-badge">📅 ${esc(r.readingDate)}</span>
              <span style="font-size:12px;color:var(--text-subtle);">🕒 ${esc(r.readingTime)}</span>
            </div>
            <div style="display:flex;gap:6px;">
              <button class="btn btn-outline btn-sm" type="button" data-action="edit-reading" data-id="${esc(r.id)}" title="Edit">✏️</button>
              <button class="btn btn-danger btn-sm" type="button" data-action="delete-reading" data-id="${esc(r.id)}" title="Delete">🗑️</button>
            </div>
          </div>

          <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;flex-wrap:wrap;">
            <div>
              <div style="font-size:11px;color:var(--text-subtle);">${t('सक्रिय ऊर्जा (Active kWh)', 'Active Energy (kWh)')}</div>
              <div style="display:flex;align-items:baseline;gap:5px;">
                <span class="mono" style="font-size:23px;font-weight:800;color:var(--primary);">${r.kwh.toFixed(2)}</span>
                <span style="font-size:12px;color:var(--text-subtle);font-weight:700;">kWh</span>
              </div>
            </div>
            ${r.unitsConsumed != null ? `<div class="units-pill mono">↗ +${r.unitsConsumed.toFixed(1)} Units</div>` : ''}
          </div>

          <div class="params-row">
            <div class="param-box">
              <div class="param-lbl">kVArh (Reactive)</div>
              <div class="param-val mono" style="color:var(--secondary);">${r.kvarh.toFixed(1)}</div>
            </div>
            <div class="param-box">
              <div class="param-lbl">kVAh (Apparent)</div>
              <div class="param-val mono" style="color:var(--tertiary);">${r.kvah.toFixed(1)}</div>
            </div>
            <div class="param-box" style="background:${rating.color}1E;border-color:${rating.color}4D;">
              <div class="param-lbl">Auto PF (kWh/kVAh)</div>
              <div class="param-val mono" style="color:${rating.color};">${r.powerFactor.toFixed(3)} <span style="font-size:10px;">${rating.short}</span></div>
            </div>
          </div>

          ${r.remarks ? `
            <div class="remark-row">
              <span><strong>${t('शेरा (Remark):', 'Remark:')}</strong> ${esc(r.remarks)}</span>
              <span>${isExp ? '▲' : '▼'}</span>
            </div>
          ` : ''}

          ${isExp ? `
            <div class="reading-drawer">
              <div class="drawer-meta">
                <div>
                  <div style="font-size:10.5px;color:var(--text-subtle);">${t('मागील kWh → चालू kWh', 'Previous → Current kWh')}</div>
                  <div class="mono" style="font-size:12px;font-weight:800;">
                    ${r.previousKwh != null ? r.previousKwh.toFixed(1) : 'N/A'} → ${r.kwh.toFixed(1)} kWh
                  </div>
                </div>
                <div style="text-align:right;">
                  <div style="font-size:10.5px;color:var(--text-subtle);">${t('PF स्थिती', 'PF Status')}</div>
                  <div style="font-size:11.5px;font-weight:800;color:${rating.color};">${esc(state.isMr ? rating.labelMr : rating.labelEn)}</div>
                </div>
              </div>
              <div style="display:flex;gap:8px;">
                <button class="btn btn-outline btn-sm" style="flex:1;" type="button" data-action="open-kvar" data-reading-id="${esc(r.id)}">⚡ ${t('kVAr तपासा', 'kVAr Calc')}</button>
                <button class="btn btn-outline btn-sm" style="flex:1;" type="button" data-action="edit-reading" data-id="${esc(r.id)}">✏️ ${t('संपादन (Edit)', 'Edit')}</button>
              </div>
            </div>
          ` : ''}
        </div>
      </div>
    `;
  }

  function renderDashboardTab() {
    const selectedCust = getSelectedCustomer();
    const filtered = selectedCust ? getFilteredReadings(selectedCust.id) : [];
    const latest = selectedCust ? getCustomerReadings(selectedCust.id)[0] : null;
    const hasLink = !!state.sheetConfig.sheetUrl;

    return `
      <!-- 1-Tap Look / Theme Switcher -->
      <div class="card no-print">
        <div class="card-header">
          <div class="card-title">🎨 ${t('अॅपचा लूक / थीम बदला (1-Tap Look)', 'App Look / Theme Switcher')}</div>
          <button class="card-link" type="button" data-action="open-branding">${t('कस्टमाईझ →', 'Customize →')}</button>
        </div>
        <div class="h-scroll">
          ${THEMES.map(th => {
            const active = state.branding.themeId === th.id;
            return `
              <button class="theme-chip ${active ? 'active' : ''}" type="button" data-select-theme="${th.id}"
                style="${active ? `background:linear-gradient(90deg,${th.start},${th.primary});` : ''}">
                <span class="theme-dot" style="background:${active ? '#fff' : th.primary};"></span>
                <span>${esc(state.isMr ? th.labelMr : th.labelEn)}</span>
                ${active ? '✓' : ''}
              </button>
            `;
          }).join('')}
        </div>
      </div>

      <!-- Customer-Wise Tabs Strip -->
      <div class="card no-print">
        <div class="card-header">
          <div class="card-title">🏢 ${t('ग्राहक टॅब व Google Sheet सिंक', 'Customer Tabs & Sheet Sync')}</div>
          <div style="display:flex;gap:6px;">
            ${hasLink ? `<button class="btn btn-emerald btn-sm" type="button" data-action="quick-sheet-sync" ${isSheetSyncing ? 'disabled' : ''}>🔄 ${isSheetSyncing ? t('सिंक...', 'Syncing...') : t('आता रिफ्रेश करा', 'Sync Now')}</button>` : ''}
            <button class="btn btn-outline btn-sm" type="button" data-action="open-sheet-dialog">🔗 ${hasLink ? t('शीट लिंक', 'Linked') : t('शीट जोडा', 'Link Sheet')}</button>
          </div>
        </div>
        <div class="h-scroll">
          ${state.customers.map(c => `
            <button class="cust-chip ${selectedCust?.id === c.id ? 'active' : ''}" type="button" data-select-customer="${esc(c.id)}">
              ${selectedCust?.id === c.id ? '✓' : '🏭'} ${esc(c.name)}
            </button>
          `).join('')}
          <button class="cust-chip emerald-pill" type="button" data-action="${hasLink ? 'quick-sheet-sync' : 'open-sheet-dialog'}">📊 ${t('शीट सिंक', 'Sheet Sync')}</button>
          <button class="cust-chip" type="button" data-action="add-customer">＋ ${t('नवीन ग्राहक', 'Add Customer')}</button>
        </div>
      </div>

      <!-- Google Sheet Live Sync Quick Card -->
      <div class="sheet-sync-card no-print">
        <div class="sheet-sync-info">
          <div class="sheet-icon-box">📊</div>
          <div>
            <div class="sheet-title">${t('Google Sheet एन्ट्री लाईव्ह सिंक', 'Google Sheet Live Sync')}</div>
            <div class="sheet-sub">
              ${isSheetSyncing
                ? t('Google Sheet मधून नवीन एन्ट्री सिंक होत आहेत...', 'Syncing latest entries from Google Sheet...')
                : hasLink
                  ? t(`${state.sheetConfig.lastTabsCount} कस्टमर टॅब • ${state.sheetConfig.lastReadingsCount} एन्ट्री जोडलेल्या`, `${state.sheetConfig.lastTabsCount} tabs • ${state.sheetConfig.lastReadingsCount} entries synced`)
                  : t('तुमच्या Google Sheet ची लिंक जोडा व सर्व एन्ट्री सिंक करा', 'Link your Google Sheet to sync live entries')}
            </div>
          </div>
        </div>
        <div style="display:flex;gap:8px;">
          ${hasLink ? `<button class="btn btn-outline btn-sm" type="button" data-action="open-sheet-dialog">${t('लिंक', 'Link')}</button>` : ''}
          <button class="btn btn-emerald btn-sm" type="button" data-action="${hasLink ? 'quick-sheet-sync' : 'open-sheet-dialog'}" ${isSheetSyncing ? 'disabled' : ''}>
            ${isSheetSyncing ? t('सिंक...', 'Syncing...') : hasLink ? t('आता सिंक करा', 'Sync Now') : t('शीट जोडा', 'Link Sheet')}
          </button>
        </div>
      </div>

      <!-- Selected Customer Profile Banner -->
      ${selectedCust ? `
        <div class="card">
          <div style="display:flex;align-items:center;justify-content:space-between;gap:10px;flex-wrap:wrap;">
            <div>
              <div style="font-size:16px;font-weight:800;">🏭 ${esc(selectedCust.name)}</div>
              <div style="font-size:12px;color:var(--text-subtle);margin-top:3px;">
                ${t('मीटर क्र.', 'Meter')}: <strong class="mono">${esc(selectedCust.meterNumber || 'N/A')}</strong> •
                ${t('लोड', 'Load')}: <strong>${esc(selectedCust.sanctionedLoad || '50 kW')}</strong> •
                ${esc(selectedCust.connectionType || '3-Phase')}
              </div>
            </div>
            <div style="display:flex;gap:6px;" class="no-print">
              <button class="btn btn-outline btn-sm" type="button" data-action="edit-customer" data-id="${esc(selectedCust.id)}">✏️ ${t('संपादन', 'Edit')}</button>
              <button class="btn btn-primary btn-sm" type="button" data-action="add-reading">＋ ${t('रीडिंग', 'Reading')}</button>
            </div>
          </div>
        </div>
      ` : ''}

      <!-- Gauge + Summary -->
      ${latest ? `<div class="grid-2">${renderGaugeHtml(latest, selectedCust)}${renderSummaryHtml(filtered)}</div>` : ''}

      <!-- Reading Logs Section -->
      <div class="card">
        <div class="card-header">
          <div class="card-title">📋 ${t('रीडिंग इतिहास (Reading Logs)', 'Daily Reading Logs')}</div>
          <button class="card-link no-print" type="button" data-action="go-history">${t('सर्व पहा →', 'View All →')}</button>
        </div>

        <div class="filter-row no-print" style="margin-bottom:12px;">
          <input id="dashSearchInput" class="input" style="max-width:260px;" type="search"
            value="${esc(state.searchQuery)}" placeholder="${t('तारीख, kWh किंवा शेरा शोधा...', 'Search date, kWh or remark...')}" />
          ${['ALL', 'THIS_MONTH', 'LAST_7_DAYS'].map(f => {
            const labels = {
              ALL: t('सर्व', 'All Time'),
              THIS_MONTH: t('हा महिना', 'This Month'),
              LAST_7_DAYS: t('मागील ७ दिवस', 'Last 7 Days')
            };
            return `<button class="btn btn-sm ${state.dateFilter === f ? 'btn-primary' : 'btn-outline'}" type="button" data-filter="${f}">${labels[f]}</button>`;
          }).join('')}
        </div>

        ${filtered.length === 0 ? `
          <div class="empty-box">
            <div class="empty-icon">⚡</div>
            <div style="font-size:15px;font-weight:800;">${t('दैनंदिन मीटर रीडिंग फ्लो सुरू करा', 'Start Daily Meter Reading Flow')}</div>
            <div style="font-size:12.5px;color:var(--text-subtle);max-width:460px;line-height:1.5;">
              ${t('१. ग्राहक निवडा किंवा Google Sheet सिंक करा → २. kWh, kVArh, kVAh नोंदवा → ३. ऑटो PF व आवश्यक kVAr पहा',
                '1. Select Customer or Sync Google Sheet → 2. Log kWh, kVArh, kVAh → 3. View Auto PF & Required kVAr')}
            </div>
            <div style="display:flex;gap:10px;flex-wrap:wrap;justify-content:center;margin-top:6px;">
              <button class="btn btn-emerald" type="button" data-action="open-sheet-dialog">📊 ${t('Google Sheet सिंक करा', 'Sync Google Sheet')}</button>
              <button class="btn btn-primary" type="button" data-action="${selectedCust ? 'add-reading' : 'add-customer'}">
                ＋ ${selectedCust ? t('रीडिंग नोंदवा', 'Log Reading') : t('नवीन ग्राहक जोडा', 'Add Customer')}
              </button>
            </div>
          </div>
        ` : `
          <div style="display:flex;flex-direction:column;gap:12px;">
            ${filtered.map(renderReadingCardHtml).join('')}
          </div>
        `}
      </div>
    `;
  }

  function renderHistoryTab() {
    const selectedCust = getSelectedCustomer();
    const filtered = selectedCust ? getFilteredReadings(selectedCust.id) : [];
    return `
      <div class="card">
        <div class="card-header">
          <div class="card-title">🕒 ${t('मागील मीटर रिडिंग्स', 'Past Meter Readings')} (${selectedCust ? esc(selectedCust.name) : '-'})</div>
          <div style="display:flex;gap:6px;">
            <button class="btn btn-outline btn-sm" type="button" data-action="toggle-sort">
              ${state.sortDesc ? '⬇️ ' + t('नवीन आधी', 'Newest First') : '⬆️ ' + t('जुने आधी', 'Oldest First')}
            </button>
            <button class="btn btn-primary btn-sm" type="button" data-action="export-csv">📥 CSV</button>
          </div>
        </div>
        <div class="filter-row" style="margin-bottom:12px;">
          <input id="histSearchInput" class="input" style="max-width:300px;" type="search"
            value="${esc(state.searchQuery)}" placeholder="${t('तारीख, kWh किंवा शेरा शोधा...', 'Search date, kWh or remark...')}" />
        </div>
        ${filtered.length === 0
          ? `<div class="empty-box"><div style="color:var(--text-subtle);">${t('कोणत्याही नोंदी उपलब्ध नाहीत.', 'No readings found.')}</div></div>`
          : `<div style="display:flex;flex-direction:column;gap:12px;">${filtered.map(renderReadingCardHtml).join('')}</div>`}
      </div>
    `;
  }

  function renderCustomersTab() {
    return `
      <div class="card">
        <div class="card-header">
          <div class="card-title">👥 ${t('ग्राहक निर्देशिका व मीटर', 'Customer Directory')} (${state.customers.length})</div>
          <button class="btn btn-primary btn-sm" type="button" data-action="add-customer">＋ ${t('नवीन ग्राहक', 'Add Customer')}</button>
        </div>
        ${state.customers.length === 0 ? `
          <div class="empty-box">
            <div class="empty-icon">👥</div>
            <div style="font-size:14.5px;font-weight:800;">${t('अद्याप कोणतेही ग्राहक नाहीत', 'No customers added yet')}</div>
            <div style="display:flex;gap:8px;flex-wrap:wrap;justify-content:center;">
              <button class="btn btn-emerald" type="button" data-action="open-sheet-dialog">📊 ${t('Sheet मधून ग्राहक आणा', 'Import from Sheet')}</button>
              <button class="btn btn-primary" type="button" data-action="add-customer">＋ ${t('नवीन ग्राहक जोडा', 'Add Customer')}</button>
            </div>
          </div>
        ` : `
          <div class="grid-2">
            ${state.customers.map(c => {
              const count = state.readings.filter(r => r.customerId === c.id).length;
              const isActive = state.selectedCustomerId === c.id;
              return `
                <div class="card" style="${isActive ? 'border-color:var(--primary);' : ''}">
                  <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:8px;">
                    <div>
                      <div style="font-size:15px;font-weight:800;">🏭 ${esc(c.name)}</div>
                      <div style="font-size:12px;color:var(--text-subtle);margin-top:3px;">
                        ${t('मीटर', 'Meter')}: <span class="mono">${esc(c.meterNumber || 'N/A')}</span> • ${esc(c.sanctionedLoad || '50 kW')}
                      </div>
                      <div style="font-size:11.5px;color:var(--primary);font-weight:700;margin-top:4px;">
                        ${count} ${t('नोंदी', 'readings')}
                      </div>
                    </div>
                    <div style="display:flex;gap:5px;">
                      <button class="btn btn-outline btn-sm" type="button" data-action="edit-customer" data-id="${esc(c.id)}">✏️</button>
                      <button class="btn btn-danger btn-sm" type="button" data-action="delete-customer" data-id="${esc(c.id)}">🗑️</button>
                    </div>
                  </div>
                  <div style="display:flex;gap:8px;margin-top:12px;">
                    <button class="btn btn-outline btn-sm" style="flex:1;" type="button" data-open-cust-dash="${esc(c.id)}">${t('डॅशबोर्ड पहा →', 'Dashboard →')}</button>
                    <button class="btn btn-primary btn-sm" style="flex:1;" type="button" data-add-reading-cust="${esc(c.id)}">＋ ${t('नवीन रीडिंग', 'New Reading')}</button>
                  </div>
                </div>
              `;
            }).join('')}
          </div>
        `}
      </div>
    `;
  }

  function renderBackupTab() {
    return `
      <div class="grid-2">
        <div class="card">
          <div class="card-header">
            <div class="card-title">📄 ${t('रिपोर्ट एक्सपोर्ट (PDF / Excel CSV)', 'Export Reports (PDF / Excel CSV)')}</div>
          </div>
          <p class="field-help" style="margin-bottom:12px;">
            ${t('सध्याच्या निवडलेल्या ग्राहकाच्या सर्व नोंदी Excel (CSV) मध्ये डाउनलोड करा किंवा थेट PDF प्रिंट करा.',
              'Download the selected customer readings as Excel-compatible CSV or print/save a PDF report.')}
          </p>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <button class="btn btn-emerald" type="button" data-action="export-csv">📊 ${t('Excel / CSV डाउनलोड', 'Download Excel CSV')}</button>
            <button class="btn btn-primary" type="button" data-action="print-pdf">🖨️ ${t('PDF रिपोर्ट प्रिंट करा', 'Print / Save PDF')}</button>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <div class="card-title">💾 ${t('पूर्ण बॅकअप व रिस्टोर (JSON)', 'Full Backup & Restore (JSON)')}</div>
          </div>
          <p class="field-help" style="margin-bottom:12px;">
            ${t(`एकूण ${state.customers.length} ग्राहक आणि ${state.readings.length} रीडिंग्स ब्राउझरमध्ये सुरक्षित आहेत.`,
              `${state.customers.length} customers and ${state.readings.length} readings stored locally.`)}
          </p>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <button class="btn btn-primary" type="button" data-action="backup-json">⬇️ ${t('JSON बॅकअप घ्या', 'Download JSON Backup')}</button>
            <label class="btn btn-outline" style="cursor:pointer;">
              ⬆️ ${t('बॅकअप रिस्टोर करा', 'Restore JSON')}
              <input id="restoreJsonInput" type="file" accept=".json,application/json" style="display:none;" />
            </label>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <div class="card-title">📱 ${t('Android अ‍ॅप (APK डाउनलोड)', 'Android App (Direct APK)')}</div>
          </div>
          <p class="field-help" style="margin-bottom:12px;">
            ${t('मोबाईलमध्ये थेट Android अ‍ॅप इन्स्टॉल करायचे असल्यास खालील बटणावरून लेटेस्ट APK डाउनलोड करा.',
              'Want the native Android app on your phone? Download the latest compiled debug APK below.')}
          </p>
          <a class="btn btn-emerald" style="text-decoration:none;" href="https://github.com/vai1990/Meater-reading/releases/latest/download/app-debug.apk" target="_blank" rel="noopener">
            ⬇️ ${t('Android APK डाउनलोड करा', 'Download Android APK')}
          </a>
        </div>

        <div class="card">
          <div class="card-header">
            <div class="card-title">👤 ${t('युजर खाते व लॉगआउट', 'User Account & Sign Out')}</div>
          </div>
          <p class="field-help" style="margin-bottom:12px;">
            ${currentUser ? `<strong>${esc(currentUser.name || 'User')}</strong> (${esc(currentUser.email || '')}) — ${currentUser.provider === 'google' ? 'Google Sign-In' : t('लोकल खाते', 'Local Account')}` : ''}
          </p>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <button class="btn btn-outline" type="button" data-action="open-account">⚙️ ${t('खाते सेटिंग्ज', 'Account Settings')}</button>
            <button class="btn btn-danger" type="button" data-action="sign-out">🚪 ${t('लॉगआउट करा', 'Sign Out')}</button>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <div class="card-title">🧹 ${t('डेटा क्लीन करा', 'Clear Local Data')}</div>
          </div>
          <p class="field-help" style="margin-bottom:12px;">
            ${t('ब्राउझरमधील सर्व ग्राहक व रीडिंग नोंदी हटवून स्वच्छ सुरुवात करा.',
              'Remove all customers and readings from this browser to start fresh.')}
          </p>
          <button class="btn btn-danger" type="button" data-action="clear-all-data">🗑️ ${t('सर्व डेटा क्लीन करा', 'Clear All Data')}</button>
        </div>
      </div>
    `;
  }

  function render() {
    applyTheme();
    renderNav();
    const main = document.getElementById('mainContent');
    if (state.currentTab === 0) main.innerHTML = renderDashboardTab();
    else if (state.currentTab === 1) main.innerHTML = renderHistoryTab();
    else if (state.currentTab === 2) main.innerHTML = renderCustomersTab();
    else main.innerHTML = renderBackupTab();
    bindMainEvents();
  }

  function bindMainEvents() {
    document.querySelectorAll('[data-select-theme]').forEach(el => {
      el.addEventListener('click', () => {
        state.branding.themeId = el.dataset.selectTheme;
        saveState();
        render();
      });
    });

    document.querySelectorAll('[data-select-customer]').forEach(el => {
      el.addEventListener('click', () => {
        state.selectedCustomerId = el.dataset.selectCustomer;
        saveState();
        render();
      });
    });

    document.querySelectorAll('[data-target-pf]').forEach(el => {
      el.addEventListener('click', () => {
        state.targetPf = Number(el.dataset.targetPf);
        render();
      });
    });

    document.querySelectorAll('[data-filter]').forEach(el => {
      el.addEventListener('click', () => {
        state.dateFilter = el.dataset.filter;
        render();
      });
    });

    document.querySelectorAll('[data-toggle-reading]').forEach(el => {
      el.addEventListener('click', e => {
        if (e.target.closest('button')) return;
        const id = el.dataset.toggleReading;
        state.expandedIds[id] = !state.expandedIds[id];
        render();
      });
    });

    document.querySelectorAll('[data-open-cust-dash]').forEach(el => {
      el.addEventListener('click', () => {
        state.selectedCustomerId = el.dataset.openCustDash;
        state.currentTab = 0;
        saveState();
        render();
      });
    });

    document.querySelectorAll('[data-add-reading-cust]').forEach(el => {
      el.addEventListener('click', () => {
        state.selectedCustomerId = el.dataset.addReadingCust;
        saveState();
        openReadingModal(null);
      });
    });

    document.querySelectorAll('[data-action]').forEach(el => {
      el.addEventListener('click', e => {
        e.stopPropagation();
        const act = el.dataset.action;
        const id = el.dataset.id;
        if (act === 'open-branding') openBrandingModal();
        else if (act === 'open-sheet-dialog') openSheetSyncModal();
        else if (act === 'quick-sheet-sync') syncGoogleSheetFromUrl(state.sheetConfig.sheetUrl, state.sheetConfig.tabNamesCsv, false);
        else if (act === 'add-customer') openCustomerModal(null);
        else if (act === 'edit-customer') openCustomerModal(state.customers.find(c => c.id === id) || null);
        else if (act === 'delete-customer') deleteCustomer(id);
        else if (act === 'add-reading') openReadingModal(null);
        else if (act === 'edit-reading') openReadingModal(state.readings.find(r => r.id === id) || null);
        else if (act === 'delete-reading') deleteReading(id);
        else if (act === 'open-kvar') openKvarModal(state.readings.find(r => r.id === el.dataset.readingId) || null);
        else if (act === 'go-history') { state.currentTab = 1; render(); }
        else if (act === 'toggle-sort') { state.sortDesc = !state.sortDesc; render(); }
        else if (act === 'export-csv') exportSelectedCustomerCsv();
        else if (act === 'print-pdf') window.print();
        else if (act === 'backup-json') exportBackupJson();
        else if (act === 'open-account') openAccountModal();
        else if (act === 'sign-out') signOutUser();
        else if (act === 'clear-all-data') clearAllData();
      });
    });

    const searchInput = document.getElementById('dashSearchInput') || document.getElementById('histSearchInput');
    if (searchInput) {
      searchInput.addEventListener('input', e => {
        state.searchQuery = e.target.value;
        const pos = e.target.selectionStart;
        render();
        const nextInput = document.getElementById('dashSearchInput') || document.getElementById('histSearchInput');
        if (nextInput) {
          nextInput.focus();
          nextInput.setSelectionRange(pos, pos);
        }
      });
    }

    const restoreInput = document.getElementById('restoreJsonInput');
    if (restoreInput) {
      restoreInput.addEventListener('change', e => {
        const file = e.target.files?.[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = ev => {
          try {
            const data = JSON.parse(ev.target.result);
            if (!Array.isArray(data.customers) || !Array.isArray(data.readings)) {
              throw new Error('Invalid JSON structure');
            }
            state.customers = data.customers;
            state.readings = data.readings;
            state.selectedCustomerId = state.customers[0]?.id || null;
            saveState();
            render();
            showToast(t(
              `डेटा रिस्टोर झाला (${state.customers.length} ग्राहक, ${state.readings.length} रीडिंग्स)`,
              `Data restored (${state.customers.length} customers, ${state.readings.length} readings)`
            ));
          } catch (err) {
            showToast(t('अवैध बॅकअप फाईल!', 'Invalid backup file!'));
          }
        };
        reader.readAsText(file);
      });
    }
  }

  // ---------- Modals ----------
  function openModal(title, bodyHtml, footHtml) {
    document.getElementById('modalTitle').textContent = title;
    document.getElementById('modalBody').innerHTML = bodyHtml;
    document.getElementById('modalFoot').innerHTML = footHtml;
    document.getElementById('modalBackdrop').classList.remove('hidden');
  }

  function closeModal() {
    document.getElementById('modalBackdrop').classList.add('hidden');
  }

  function openSheetSyncModal() {
    const cfg = state.sheetConfig;
    const lastStr = cfg.lastSyncTs ? new Date(cfg.lastSyncTs).toLocaleString() : '';
    openModal(
      t('📊 गूगल शीट सिंक (Customer Tabs)', '📊 Google Sheet Live Sync'),
      `
        ${cfg.lastSyncTs ? `
          <div style="background:rgba(16,185,129,0.14);border:1px solid rgba(16,185,129,0.35);padding:10px;border-radius:10px;font-size:12px;">
            <strong style="color:#10B981;">✅ ${t('शेवटचे सिंक:', 'Last Sync:')}</strong> ${esc(lastStr)}<br/>
            <span style="color:var(--text-muted);">${cfg.lastTabsCount} ${t('ग्राहक टॅब', 'tabs')} • ${cfg.lastReadingsCount} ${t('एन्ट्री सिंक', 'entries synced')}</span>
          </div>
        ` : ''}

        <div class="field-group">
          <label class="field-label">${t('Google Sheet लिंक येथे पेस्ट करा *', 'Paste Google Sheet Link *')}</label>
          <input id="sheetUrlInput" class="input" type="url" value="${esc(cfg.sheetUrl)}" placeholder="https://docs.google.com/spreadsheets/d/..." />
          <div class="field-help">${t('Google Sheet मध्ये Share → "Anyone with the link (Viewer)" ठेवा म्हणजे ब्राउझरमध्ये थेट लाईव्ह सिंक होईल.',
            'Set Share → "Anyone with the link (Viewer)" so browser sync pulls live changes immediately.')}</div>
        </div>

        <div class="field-group">
          <label class="field-label">${t('ग्राहक टॅब नावे (ऐच्छिक - स्वल्पविरामाने)', 'Customer Tab Names (Optional, comma-separated)')}</label>
          <input id="sheetTabsInput" class="input" type="text" value="${esc(cfg.tabNamesCsv)}" placeholder="TIRUPATI CRUSHER, SHREE GANESH" />
          <div class="field-help">${t('एकापेक्षा जास्त ग्राहक टॅब असतील तर त्यांची नावे कॉमा (,) देऊन टाका किंवा रिकामे ठेवून पहिला/सक्रिय टॅब सिंक करा.',
            'Enter multiple tab names separated by commas, or leave blank to sync the active sheet tab.')}</div>
        </div>

        <button id="doSheetSyncBtn" class="btn btn-emerald" style="width:100%;padding:11px;" type="button">
          🔄 ${t('शीट लिंकवरून सिंक करा (Sync Sheet)', 'Sync from Google Sheet Link')}
        </button>

        <div style="display:flex;gap:8px;flex-wrap:wrap;">
          <label class="btn btn-outline" style="flex:1;cursor:pointer;">
            📂 ${t('Excel (.xlsx) / CSV निवडा', 'Import .xlsx / .csv')}
            <input id="sheetFileInput" type="file" accept=".xlsx,.xls,.csv,text/csv" style="display:none;" />
          </label>
          <button id="loadSampleBtn" class="btn btn-outline" style="flex:1;" type="button">
            ⬇️ ${t('नमुना शीट लोड करा', 'Load Sample Sheet')}
          </button>
        </div>

        <div style="background:var(--surface-variant);padding:10px;border-radius:10px;font-size:11px;line-height:1.5;color:var(--text-muted);">
          <strong style="color:var(--primary);">${t('PF व Remark नियम:', 'PF & Remark Rule:')}</strong><br/>
          ${t('• शीटमधील PF कॉलमची किंमत अ‍ॅपच्या शेरा (Remark) मध्ये जतन होईल.<br/>• अ‍ॅपचा Power Factor आपोआप kWh, kVArh, kVAh वरून Auto-Feed होईल.',
            '• The Sheet PF column is automatically saved into Remarks.<br/>• App Power Factor is auto-calculated and auto-fed from kWh, kVArh & kVAh.')}
        </div>
      `,
      `<button class="btn btn-outline" type="button" id="cancelModalBtn">${t('बंद करा', 'Close')}</button>`
    );

    document.getElementById('cancelModalBtn').onclick = closeModal;
    document.getElementById('doSheetSyncBtn').onclick = () => {
      syncGoogleSheetFromUrl(
        document.getElementById('sheetUrlInput').value,
        document.getElementById('sheetTabsInput').value,
        false
      );
    };
    document.getElementById('sheetFileInput').onchange = e => handleLocalSheetFile(e.target.files?.[0]);
    document.getElementById('loadSampleBtn').onclick = loadOfflineSampleSheet;
  }

  function openCustomerModal(customer) {
    const isEdit = !!customer;
    openModal(
      isEdit ? t('ग्राहक संपादन (Edit Customer)', 'Edit Customer') : t('नवीन ग्राहक जोडा (Add Customer)', 'Add Customer'),
      `
        <div class="field-group">
          <label class="field-label">${t('ग्राहकाचे / कंपनीचे नाव *', 'Customer / Company Name *')}</label>
          <input id="custName" class="input" value="${esc(customer?.name || '')}" placeholder="उदा. TIRUPATI CRUSHER" />
        </div>
        <div class="grid-2">
          <div class="field-group">
            <label class="field-label">${t('मीटर नंबर', 'Meter Number')}</label>
            <input id="custMeter" class="input mono" value="${esc(customer?.meterNumber || '')}" placeholder="MTR-1001" />
          </div>
          <div class="field-group">
            <label class="field-label">${t('मंजूर लोड (Sanctioned Load)', 'Sanctioned Load')}</label>
            <input id="custLoad" class="input" value="${esc(customer?.sanctionedLoad || '65 kW')}" placeholder="65 kW" />
          </div>
        </div>
        <div class="grid-2">
          <div class="field-group">
            <label class="field-label">${t('कन्झ्युमर नंबर', 'Consumer No.')}</label>
            <input id="custConsumer" class="input mono" value="${esc(customer?.consumerNumber || '')}" />
          </div>
          <div class="field-group">
            <label class="field-label">${t('कनेक्शन प्रकार', 'Connection Type')}</label>
            <select id="custConn" class="select">
              ${['3-Phase Industrial', 'HT Industrial', '3-Phase Commercial', '1-Phase Domestic'].map(opt =>
                `<option value="${opt}" ${customer?.connectionType === opt ? 'selected' : ''}>${opt}</option>`
              ).join('')}
            </select>
          </div>
        </div>
        <div class="field-group">
          <label class="field-label">${t('पत्ता (Address)', 'Address')}</label>
          <input id="custAddress" class="input" value="${esc(customer?.address || '')}" />
        </div>
      `,
      `
        <button class="btn btn-outline" type="button" id="cancelCustBtn">${t('रद्द करा', 'Cancel')}</button>
        <button class="btn btn-primary" type="button" id="saveCustBtn">${t('जतन करा (Save)', 'Save')}</button>
      `
    );

    document.getElementById('cancelCustBtn').onclick = closeModal;
    document.getElementById('saveCustBtn').onclick = () => {
      const name = document.getElementById('custName').value.trim();
      if (!name) {
        showToast(t('कृपया ग्राहकाचे नाव टाका.', 'Please enter customer name.'));
        return;
      }
      const payload = {
        id: customer?.id || ('c_' + Date.now()),
        name,
        meterNumber: document.getElementById('custMeter').value.trim(),
        sanctionedLoad: document.getElementById('custLoad').value.trim() || '50 kW',
        consumerNumber: document.getElementById('custConsumer').value.trim(),
        connectionType: document.getElementById('custConn').value,
        address: document.getElementById('custAddress').value.trim()
      };
      if (isEdit) {
        const idx = state.customers.findIndex(c => c.id === customer.id);
        if (idx >= 0) state.customers[idx] = payload;
      } else {
        state.customers.push(payload);
        state.selectedCustomerId = payload.id;
      }
      saveState();
      closeModal();
      render();
      showToast(t('ग्राहक जतन केला!', 'Customer saved!'));
    };
  }

  function openReadingModal(reading) {
    const cust = getSelectedCustomer();
    if (!cust) {
      openCustomerModal(null);
      showToast(t('आधी नवीन ग्राहक जोडा.', 'Please add a customer first.'));
      return;
    }

    const isEdit = !!reading;
    const now = new Date();
    const defDate = reading?.readingDate || formatDateDisplay(now);
    const defTime = reading?.readingTime || formatTimeDisplay(now);
    const prevKwh = isEdit ? reading.previousKwh : (getCustomerReadings(cust.id)[0]?.kwh ?? null);

    openModal(
      isEdit ? t('रीडिंग संपादन (Edit Reading)', 'Edit Reading') : t(`नवीन मीटर रीडिंग (${cust.name})`, `Log Reading (${cust.name})`),
      `
        <div class="grid-2">
          <div class="field-group">
            <label class="field-label">${t('दिनांक (Date)', 'Date')}</label>
            <input id="rDate" class="input" value="${esc(defDate)}" placeholder="18-Nov-2026" />
          </div>
          <div class="field-group">
            <label class="field-label">${t('वेळ (Time)', 'Time')}</label>
            <input id="rTime" class="input" value="${esc(defTime)}" placeholder="08:30:00 AM" />
          </div>
        </div>

        <div class="field-group">
          <label class="field-label">${t('सक्रिय ऊर्जा kWh *', 'Active Energy (kWh) *')} ${prevKwh != null ? `(${t('मागील', 'Prev')}: ${prevKwh})` : ''}</label>
          <input id="rKwh" class="input mono" type="number" step="any" value="${reading ? reading.kwh : ''}" placeholder="56250" />
        </div>

        <div class="grid-2">
          <div class="field-group">
            <label class="field-label">kVArh (Reactive)</label>
            <input id="rKvarh" class="input mono" type="number" step="any" value="${reading ? reading.kvarh : ''}" placeholder="25732" />
          </div>
          <div class="field-group">
            <label class="field-label">kVAh (Apparent)</label>
            <input id="rKvah" class="input mono" type="number" step="any" value="${reading ? reading.kvah : ''}" placeholder="72309" />
          </div>
        </div>

        <div id="autoPfPreview" style="background:rgba(56,189,248,0.12);border:1px solid rgba(56,189,248,0.3);padding:10px;border-radius:11px;font-size:12.5px;font-weight:700;">
          ⚡ Auto PF: <span class="mono" id="autoPfVal">${reading ? reading.powerFactor.toFixed(3) : '0.000'}</span>
        </div>

        <div class="field-group">
          <label class="field-label">${t('शेरा / शीट PF (Remark)', 'Remark / Sheet PF')}</label>
          <input id="rRemarks" class="input" value="${esc(reading?.remarks || '')}" placeholder="उदा. PF: 653" />
        </div>
      `,
      `
        <button class="btn btn-outline" type="button" id="cancelReadBtn">${t('रद्द करा', 'Cancel')}</button>
        <button class="btn btn-primary" type="button" id="saveReadBtn">${t('रीडिंग जतन करा', 'Save Reading')}</button>
      `
    );

    const updatePreview = () => {
      const kwh = parseNumber(document.getElementById('rKwh').value) || 0;
      const kvarh = parseNumber(document.getElementById('rKvarh').value) || 0;
      const kvah = parseNumber(document.getElementById('rKvah').value) || 0;
      const autoPf = calculateAutoPf(kwh, kvarh, kvah);
      document.getElementById('autoPfVal').textContent = autoPf.toFixed(3);
    };
    ['rKwh', 'rKvarh', 'rKvah'].forEach(id => document.getElementById(id).addEventListener('input', updatePreview));

    document.getElementById('cancelReadBtn').onclick = closeModal;
    document.getElementById('saveReadBtn').onclick = () => {
      const kwh = parseNumber(document.getElementById('rKwh').value);
      if (kwh === null) {
        showToast(t('कृपया वैध kWh रीडिंग टाका.', 'Please enter a valid kWh reading.'));
        return;
      }
      const kvarh = parseNumber(document.getElementById('rKvarh').value) || 0;
      const kvah = parseNumber(document.getElementById('rKvah').value) || calculateKvahFromKvarh(kwh, kvarh);
      const autoPf = calculateAutoPf(kwh, kvarh, kvah);
      const readingDate = formatDateDisplay(parseFlexibleDate(document.getElementById('rDate').value));
      const readingTime = normalizeTime(document.getElementById('rTime').value);
      const timestamp = dateTimeToMillis(readingDate, readingTime);
      const remarks = document.getElementById('rRemarks').value.trim();

      const item = {
        id: reading?.id || ('r_' + Date.now()),
        customerId: cust.id,
        readingDate,
        readingTime,
        timestamp,
        kwh,
        kvarh,
        kvah,
        powerFactor: autoPf,
        isPfAutoCalculated: true,
        previousKwh: null,
        unitsConsumed: null,
        remarks
      };

      if (isEdit) {
        const idx = state.readings.findIndex(r => r.id === reading.id);
        if (idx >= 0) state.readings[idx] = item;
      } else {
        state.readings.push(item);
      }

      recalculateCustomerDeltas(cust.id);
      saveState();
      closeModal();
      render();
      showToast(t('मीटर रीडिंग जतन केली (Auto PF)!', 'Reading saved with Auto PF!'));
    };
  }

  function openKvarModal(reading) {
    const cust = getSelectedCustomer();
    const defLoad = parseLoadKw(cust?.sanctionedLoad || '50 kW');
    const defPf = reading?.powerFactor || getCustomerReadings(cust?.id || '')[0]?.powerFactor || 0.80;

    openModal(
      t('⚡ kVAr कॅपॅसिटर बँक कॅल्क्युलेटर', '⚡ kVAr Capacitor Bank Calculator'),
      `
        <div class="grid-2">
          <div class="field-group">
            <label class="field-label">${t('लोड (Load kW)', 'Load (kW)')}</label>
            <input id="kvLoad" class="input mono" type="number" step="any" value="${defLoad}" />
          </div>
          <div class="field-group">
            <label class="field-label">${t('सध्याचा PF', 'Current PF')}</label>
            <input id="kvInitPf" class="input mono" type="number" step="0.001" min="0.2" max="1" value="${defPf.toFixed(3)}" />
          </div>
        </div>
        <div class="field-group">
          <label class="field-label">${t('लक्ष्य PF (Target PF)', 'Target PF')}</label>
          <input id="kvTargetPf" class="input mono" type="number" step="0.01" min="0.85" max="1" value="${state.targetPf.toFixed(2)}" />
        </div>
        <div id="kvResultBox" class="card" style="background:var(--surface-variant);"></div>
      `,
      `<button class="btn btn-primary" type="button" id="closeKvBtn">${t('बंद करा', 'Close')}</button>`
    );

    const calc = () => {
      const res = calculateRequiredKvar(
        parseFloat(document.getElementById('kvLoad').value) || 50,
        parseFloat(document.getElementById('kvInitPf').value) || 0.8,
        parseFloat(document.getElementById('kvTargetPf').value) || 0.99
      );
      document.getElementById('kvResultBox').innerHTML = `
        <div class="kpi-grid">
          <div class="kpi-box" style="background:var(--surface);">
            <div class="kpi-label">${t('आवश्यक kVAr', 'Required kVAr')}</div>
            <div class="kpi-value mono">${res.requiredKvar.toFixed(2)}</div>
          </div>
          <div class="kpi-box" style="background:var(--surface);">
            <div class="kpi-label">${t('शिफारस बँक', 'Recommended Bank')}</div>
            <div class="kpi-value mono" style="color:#10B981;">${res.bankSize} kVAr</div>
          </div>
          <div class="kpi-box" style="background:var(--surface);">
            <div class="kpi-label">${t('kVA बचत', 'kVA Saved')}</div>
            <div class="kpi-value mono">${res.kvaSaved.toFixed(1)} kVA</div>
          </div>
          <div class="kpi-box" style="background:var(--surface);">
            <div class="kpi-label">${t('करंट बचत', 'Current Saved')}</div>
            <div class="kpi-value mono" style="color:#10B981;">${res.currentRedPct.toFixed(1)}%</div>
          </div>
        </div>
        ${res.steps.length ? `
          <div style="margin-top:10px;font-size:12px;">
            <strong>APFC Steps:</strong> <span class="mono" style="color:var(--primary);">${res.steps.join(' + ')} kVAr</span>
          </div>
        ` : ''}
      `;
    };

    ['kvLoad', 'kvInitPf', 'kvTargetPf'].forEach(id => document.getElementById(id).addEventListener('input', calc));
    calc();
    document.getElementById('closeKvBtn').onclick = closeModal;
  }

  function openBrandingModal() {
    openModal(
      t('🎨 अॅपचा लूक, थीम व नाव बदला', '🎨 Customize App Look & Theme'),
      `
        <div class="field-group">
          <label class="field-label">${t('अॅपचे नाव', 'App Name')}</label>
          <input id="bAppName" class="input" value="${esc(state.branding.appName)}" />
        </div>
        <div class="field-group">
          <label class="field-label">${t('सबटायटल (ऐच्छिक)', 'Custom Subtitle (Optional)')}</label>
          <input id="bSubtitle" class="input" value="${esc(state.branding.subtitle)}" placeholder="उदा. MSEDCL Daily Meter Log" />
        </div>
        <div class="field-group">
          <label class="field-label">${t('आयकॉन निवडा', 'Choose Icon')}</label>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            ${ICONS.map(ic => `
              <button type="button" class="btn btn-sm ${state.branding.icon === ic ? 'btn-primary' : 'btn-outline'}" data-pick-icon="${ic}" style="font-size:18px;">${ic}</button>
            `).join('')}
          </div>
        </div>
        <div class="field-group">
          <label class="field-label">${t('रंग व थीम (Dark / Light)', 'Visual Theme (Dark / Light)')}</label>
          <div style="display:flex;flex-direction:column;gap:7px;">
            ${THEMES.map(th => `
              <button type="button" class="btn btn-outline" data-pick-theme="${th.id}"
                style="justify-content:flex-start;${state.branding.themeId === th.id ? 'border-color:var(--primary);background:var(--surface-variant);' : ''}">
                <span class="theme-dot" style="background:${th.primary};"></span>
                <span>${esc(state.isMr ? th.labelMr : th.labelEn)} ${th.isDark ? '(Dark)' : '(Light)'}</span>
              </button>
            `).join('')}
          </div>
        </div>
      `,
      `
        <button class="btn btn-outline" type="button" id="cancelBrandBtn">${t('रद्द करा', 'Cancel')}</button>
        <button class="btn btn-primary" type="button" id="saveBrandBtn">${t('लागू करा (Apply)', 'Apply')}</button>
      `
    );

    let chosenIcon = state.branding.icon;
    let chosenTheme = state.branding.themeId;

    document.querySelectorAll('[data-pick-icon]').forEach(btn => {
      btn.onclick = () => {
        chosenIcon = btn.dataset.pickIcon;
        document.querySelectorAll('[data-pick-icon]').forEach(b => b.className = 'btn btn-sm ' + (b.dataset.pickIcon === chosenIcon ? 'btn-primary' : 'btn-outline'));
      };
    });
    document.querySelectorAll('[data-pick-theme]').forEach(btn => {
      btn.onclick = () => {
        chosenTheme = btn.dataset.pickTheme;
        document.querySelectorAll('[data-pick-theme]').forEach(b => {
          b.style.borderColor = b.dataset.pickTheme === chosenTheme ? 'var(--primary)' : '';
          b.style.background = b.dataset.pickTheme === chosenTheme ? 'var(--surface-variant)' : '';
        });
      };
    });

    document.getElementById('cancelBrandBtn').onclick = closeModal;
    document.getElementById('saveBrandBtn').onclick = () => {
      state.branding.appName = document.getElementById('bAppName').value.trim() || 'Smart Meter Pro';
      state.branding.subtitle = document.getElementById('bSubtitle').value.trim();
      state.branding.icon = chosenIcon;
      state.branding.themeId = chosenTheme;
      saveState();
      closeModal();
      render();
      showToast(t('अॅपचा लूक अपडेट झाला!', 'App look updated!'));
    };
  }

  // ---------- Delete & Export Actions ----------
  function deleteReading(id) {
    if (!confirm(t('ही मीटर रीडिंग हटवायची का?', 'Delete this meter reading?'))) return;
    const item = state.readings.find(r => r.id === id);
    state.readings = state.readings.filter(r => r.id !== id);
    if (item) recalculateCustomerDeltas(item.customerId);
    saveState();
    render();
    showToast(t('रीडिंग हटवली.', 'Reading deleted.'));
  }

  function deleteCustomer(id) {
    if (!confirm(t('हा ग्राहक व त्याच्या सर्व नोंदी हटवायच्या का?', 'Delete this customer and all readings?'))) return;
    state.customers = state.customers.filter(c => c.id !== id);
    state.readings = state.readings.filter(r => r.customerId !== id);
    if (state.selectedCustomerId === id) {
      state.selectedCustomerId = state.customers[0]?.id || null;
    }
    saveState();
    render();
    showToast(t('ग्राहक हटवला.', 'Customer deleted.'));
  }

  function clearAllData() {
    if (!confirm(t('सर्व ग्राहक व मीटर रीडिंग डेटा कायमचा क्लीन करायचा का?', 'Clear all customers and readings?'))) return;
    state.customers = [];
    state.readings = [];
    state.selectedCustomerId = null;
    saveState();
    render();
    showToast(t('सर्व डेटा क्लीन केला!', 'All local data cleared!'));
  }

  function downloadFile(filename, content, mime) {
    const blob = new Blob([content], { type: mime });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    setTimeout(() => { URL.revokeObjectURL(a.href); a.remove(); }, 100);
  }

  function exportSelectedCustomerCsv() {
    const cust = getSelectedCustomer();
    if (!cust) { showToast(t('कोणताही ग्राहक निवडलेला नाही.', 'No customer selected.')); return; }
    const rows = getFilteredReadings(cust.id);
    if (!rows.length) { showToast(t('एक्सपोर्टसाठी रीडिंग्स नाहीत.', 'No readings to export.')); return; }
    const lines = [
      `"${cust.name.replace(/"/g, '""')}",,,,,,`,
      'DATE,TIME,Kwh,KvArh,KvAh,Auto_PF,Units_Consumed,Remarks'
    ];
    for (const r of rows) {
      lines.push([
        r.readingDate,
        r.readingTime,
        r.kwh,
        r.kvarh,
        r.kvah,
        r.powerFactor.toFixed(3),
        r.unitsConsumed ?? '',
        `"${String(r.remarks || '').replace(/"/g, '""')}"`
      ].join(','));
    }
    const safeName = cust.name.replace(/[^a-zA-Z0-9_-]/g, '_');
    downloadFile(`SmartMeter_${safeName}.csv`, '\uFEFF' + lines.join('\n'), 'text/csv;charset=utf-8');
    showToast(t('Excel / CSV फाईल डाउनलोड झाली!', 'Excel CSV downloaded!'));
  }

  function exportBackupJson() {
    const payload = {
      app: 'Smart Meter Pro',
      exportedAt: new Date().toISOString(),
      customers: state.customers,
      readings: state.readings
    };
    downloadFile('SmartMeterPro_Backup.json', JSON.stringify(payload, null, 2), 'application/json');
    showToast(t('JSON बॅकअप फाईल डाउनलोड झाली!', 'JSON backup downloaded!'));
  }

  // ---------- User Account Modal & Auth Flow ----------
  function openAccountModal() {
    if (!currentUser) return;
    const isLocal = currentUser.provider !== 'google';
    openModal(
      t('👤 युजर खाते (User Account)', '👤 User Account'),
      `
        <div class="card" style="background:var(--surface-variant);margin-bottom:4px;">
          <div style="display:flex;align-items:center;gap:12px;">
            <div style="width:44px;height:44px;border-radius:14px;background:var(--primary);color:#fff;display:grid;place-items:center;font-size:20px;font-weight:800;">
              ${esc((currentUser.name || currentUser.email || 'U').slice(0, 1).toUpperCase())}
            </div>
            <div style="min-width:0;">
              <div style="font-size:15px;font-weight:800;">${esc(currentUser.name || 'User')}</div>
              <div style="font-size:12px;color:var(--text-subtle);word-break:break-all;">${esc(currentUser.email || '')}</div>
              <div style="margin-top:4px;font-size:11px;font-weight:700;color:var(--primary);">
                ${isLocal ? t('🔐 लोकल युजर खाते (Email / Username)', '🔐 Local User Account') : '🌐 Google Account'}
              </div>
            </div>
          </div>
          <div style="margin-top:10px;padding-top:10px;border-top:1px solid var(--border);font-size:12px;color:var(--text-subtle);">
            ${t(`या खात्यात ${state.customers.length} ग्राहक आणि ${state.readings.length} मीटर रीडिंग्स जतन आहेत.`,
              `${state.customers.length} customers and ${state.readings.length} readings saved in this account.`)}
          </div>
        </div>

        ${isLocal ? `
          <div class="card" style="background:var(--surface-variant);">
            <div style="font-size:13px;font-weight:800;margin-bottom:8px;">🔑 ${t('पासवर्ड बदला', 'Change Password')}</div>
            <div style="display:flex;flex-direction:column;gap:8px;">
              <input id="accOldPass" class="input" type="password" placeholder="${t('सध्याचा पासवर्ड', 'Current password')}" />
              <input id="accNewPass" class="input" type="password" placeholder="${t('नवीन पासवर्ड (किमान ४ अक्षरे)', 'New password (min 4 chars)')}" />
              <button id="accChangePassBtn" class="btn btn-outline btn-sm" type="button">
                ${t('पासवर्ड अपडेट करा', 'Update Password')}
              </button>
            </div>
          </div>
        ` : ''}
      `,
      `
        <button class="btn btn-outline" type="button" id="closeAccBtn">${t('बंद करा', 'Close')}</button>
        <button class="btn btn-danger" type="button" id="logoutModalBtn">🚪 ${t('लॉगआउट करा', 'Sign Out')}</button>
      `
    );

    document.getElementById('closeAccBtn').onclick = closeModal;
    document.getElementById('logoutModalBtn').onclick = signOutUser;

    const chBtn = document.getElementById('accChangePassBtn');
    if (chBtn) {
      chBtn.onclick = async () => {
        const oldP = document.getElementById('accOldPass').value;
        const newP = document.getElementById('accNewPass').value;
        if (!oldP || newP.length < 4) {
          showToast(t('कृपया सध्याचा पासवर्ड आणि किमान ४ अक्षरांचा नवीन पासवर्ड टाका.', 'Enter current password and a new password (min 4 chars).'));
          return;
        }
        const users = loadUsers();
        const idx = users.findIndex(u => u.id === currentUser.id);
        if (idx < 0) {
          showToast(t('खाते सापडले नाही.', 'Account not found.'));
          return;
        }
        const oldHash = await hashPassword(oldP);
        if (users[idx].passwordHash !== oldHash) {
          showToast(t('सध्याचा पासवर्ड चुकीचा आहे.', 'Current password is incorrect.'));
          return;
        }
        users[idx].passwordHash = await hashPassword(newP);
        saveUsers(users);
        document.getElementById('accOldPass').value = '';
        document.getElementById('accNewPass').value = '';
        showToast(t('पासवर्ड यशस्वीरित्या बदलला!', 'Password updated successfully!'));
      };
    }
  }

  function showAuthError(msg) {
    const box = document.getElementById('authErrorBox');
    if (!box) return;
    if (!msg) {
      box.classList.add('hidden');
      box.textContent = '';
    } else {
      box.textContent = msg;
      box.classList.remove('hidden');
    }
  }

  function updateAuthScreenText() {
    const isReg = authMode === 'register';
    document.getElementById('authTabLogin').classList.toggle('active', !isReg);
    document.getElementById('authTabRegister').classList.toggle('active', isReg);
    document.getElementById('authTabLogin').textContent = t('लॉगिन (Login)', 'Login');
    document.getElementById('authTabRegister').textContent = t('नवीन खाते (Register)', 'Register');
    document.getElementById('authNameGroup').classList.toggle('hidden', !isReg);
    document.getElementById('authNameInput').required = isReg;

    document.getElementById('authSubtitle').textContent = isReg
      ? t('नवीन युजर खाते तयार करा आणि मीटर रीडिंग सुरू करा', 'Create a new account to manage meter readings')
      : t('दैनिक मीटर रीडिंग व Google Sheet सिंकसाठी लॉगिन करा', 'Sign in to access Daily Meter Readings & Google Sheet Sync');

    document.getElementById('authNameLabel').textContent = t('पूर्ण नाव (Full Name) *', 'Full Name *');
    document.getElementById('authNameInput').placeholder = t('उदा. वैभव घाग', 'e.g. Vaibhav Ghag');
    document.getElementById('authEmailLabel').textContent = t('ईमेल किंवा युजरनेम (Email / Username) *', 'Email or Username *');
    document.getElementById('authEmailInput').placeholder = t('name@example.com किंवा operator1', 'name@example.com or operator1');
    document.getElementById('authPassLabel').textContent = t('पासवर्ड (Password) *', 'Password *');
    document.getElementById('authPassInput').placeholder = t('किमान ४ अक्षरे / अंक', 'At least 4 characters');
    document.getElementById('authPassInput').autocomplete = isReg ? 'new-password' : 'current-password';

    document.getElementById('authSubmitBtn').textContent = isReg
      ? t('✨ खाते तयार करा व लॉगिन करा', '✨ Create Account & Sign In')
      : t('🔐 लॉगिन करा (Sign In)', '🔐 Sign In');
    document.getElementById('authOrText').textContent = t('किंवा (OR)', 'OR');
    document.getElementById('googleSignInLabel').textContent = t('Google खात्याने लॉगिन करा (Sign in with Google)', 'Sign in with Google');
    document.getElementById('authNoteText').textContent = t('प्रत्येक युजरचा डेटा स्वतंत्र व सुरक्षित राहतो.', 'Each user has isolated, private storage.');
    document.getElementById('authLangBtn').textContent = state.isMr ? 'EN' : 'मराठी';
  }

  function showAuthGate() {
    document.getElementById('appContainer').classList.add('hidden');
    document.getElementById('authScreen').classList.remove('hidden');
    const theme = THEMES.find(th => th.id === state.branding.themeId) || THEMES[0];
    document.body.className = theme.className;
    showAuthError('');
    updateAuthScreenText();
  }

  function showAppForUser(user) {
    currentUser = user;
    saveSession(user);
    state = loadState();
    document.getElementById('authScreen').classList.add('hidden');
    document.getElementById('appContainer').classList.remove('hidden');
    showAuthError('');
    render();
    if (state.sheetConfig.sheetUrl) {
      syncGoogleSheetFromUrl(state.sheetConfig.sheetUrl, state.sheetConfig.tabNamesCsv, true);
    }
  }

  async function signOutUser() {
    closeModal();
    const wasGoogle = currentUser?.provider === 'google';
    currentUser = null;
    saveSession(null);
    if (wasGoogle && firebaseAuth) {
      try { await firebaseAuth.signOut(); } catch {}
    }
    const prevMr = state.isMr;
    const prevTheme = state.branding.themeId;
    state = defaultState();
    state.isMr = prevMr;
    state.branding.themeId = prevTheme;
    document.getElementById('authPassInput').value = '';
    showAuthGate();
    showToast(t('तुम्ही यशस्वीरित्या लॉगआउट झाले आहात.', 'Signed out successfully.'));
  }

  function initFirebaseAuth() {
    try {
      if (window.firebase && typeof window.firebase.initializeApp === 'function') {
        if (!window.firebase.apps || window.firebase.apps.length === 0) {
          window.firebase.initializeApp(FIREBASE_CONFIG);
        }
        firebaseAuth = window.firebase.auth();
        firebaseAuth.onAuthStateChanged(fbUser => {
          if (fbUser && !currentUser) {
            showAppForUser({
              id: 'g_' + fbUser.uid,
              name: fbUser.displayName || (fbUser.email ? fbUser.email.split('@')[0] : 'Google User'),
              email: fbUser.email || '',
              provider: 'google'
            });
          }
        });
      }
    } catch (err) {
      console.warn('Firebase init skipped:', err);
    }
  }

  // Auth screen listeners
  document.getElementById('authTabLogin').addEventListener('click', () => {
    authMode = 'login';
    showAuthError('');
    updateAuthScreenText();
  });

  document.getElementById('authTabRegister').addEventListener('click', () => {
    authMode = 'register';
    showAuthError('');
    updateAuthScreenText();
  });

  document.getElementById('authLangBtn').addEventListener('click', () => {
    state.isMr = !state.isMr;
    try { localStorage.setItem(PREF_LANG_KEY, state.isMr ? 'mr' : 'en'); } catch {}
    updateAuthScreenText();
  });

  document.getElementById('authForm').addEventListener('submit', async e => {
    e.preventDefault();
    showAuthError('');

    const name = document.getElementById('authNameInput').value.trim();
    const identifier = document.getElementById('authEmailInput').value.trim();
    const pass = document.getElementById('authPassInput').value;
    const normId = identifier.toLowerCase();

    if (identifier.length < 3) {
      showAuthError(t('कृपया किमान ३ अक्षरांचा ईमेल किंवा युजरनेम टाका.', 'Please enter a valid email or username (min 3 characters).'));
      return;
    }
    if (pass.length < 4) {
      showAuthError(t('पासवर्ड किमान ४ अक्षरांचा असावा.', 'Password must be at least 4 characters.'));
      return;
    }

    const users = loadUsers();

    if (authMode === 'register') {
      if (!name) {
        showAuthError(t('कृपया तुमचे पूर्ण नाव टाका.', 'Please enter your full name.'));
        return;
      }
      if (users.some(u => u.emailLower === normId)) {
        showAuthError(t('हा ईमेल / युजरनेम आधीच नोंदणीकृत आहे. कृपया लॉगिन करा.', 'This email/username is already registered. Please sign in.'));
        return;
      }
      const passwordHash = await hashPassword(pass);
      const newUser = {
        id: 'u_' + Date.now() + '_' + Math.random().toString(36).slice(2, 6),
        name,
        email: identifier,
        emailLower: normId,
        passwordHash,
        createdAt: Date.now()
      };
      users.push(newUser);
      saveUsers(users);
      document.getElementById('authPassInput').value = '';
      showAppForUser({ id: newUser.id, name: newUser.name, email: newUser.email, provider: 'local' });
      showToast(t(`स्वागत आहे, ${newUser.name}! नवीन खाते तयार झाले.`, `Welcome, ${newUser.name}! Account created.`));
    } else {
      if (users.length === 0) {
        showAuthError(t('अजून कोणतेही खाते तयार केलेले नाही. कृपया आधी "नवीन खाते (Register)" टॅबवरून नोंदणी करा.', 'No account exists yet. Please click "Register" first to create your account.'));
        return;
      }
      const found = users.find(u => u.emailLower === normId);
      if (!found) {
        showAuthError(t('हा ईमेल / युजरनेम सापडला नाही. नवीन खात्यासाठी Register निवडा.', 'Email/username not found. Switch to Register to create an account.'));
        return;
      }
      const passwordHash = await hashPassword(pass);
      if (found.passwordHash !== passwordHash) {
        showAuthError(t('चुकीचा पासवर्ड! कृपया पुन्हा प्रयत्न करा.', 'Incorrect password! Please try again.'));
        return;
      }
      document.getElementById('authPassInput').value = '';
      showAppForUser({ id: found.id, name: found.name, email: found.email, provider: 'local' });
      showToast(t(`स्वागत आहे, ${found.name}!`, `Welcome back, ${found.name}!`));
    }
  });

  document.getElementById('googleSignInBtn').addEventListener('click', async () => {
    showAuthError('');
    if (!firebaseAuth) initFirebaseAuth();
    if (!firebaseAuth || !window.firebase?.auth?.GoogleAuthProvider) {
      showAuthError(t(
        'Google Sign-In लोड होऊ शकले नाही. कृपया इंटरनेट तपासा किंवा ईमेल/पासवर्ड लॉगिन वापरा.',
        'Google Sign-In could not load. Check your internet or use Email/Password login.'
      ));
      return;
    }
    const btn = document.getElementById('googleSignInBtn');
    btn.disabled = true;
    try {
      const provider = new window.firebase.auth.GoogleAuthProvider();
      const res = await firebaseAuth.signInWithPopup(provider);
      const u = res.user;
      showAppForUser({
        id: 'g_' + u.uid,
        name: u.displayName || (u.email ? u.email.split('@')[0] : 'Google User'),
        email: u.email || '',
        provider: 'google'
      });
      showToast(t(`Google खात्याने लॉगिन यशस्वी (${u.displayName || u.email})!`, `Signed in with Google (${u.displayName || u.email})!`));
    } catch (err) {
      const code = err?.code || '';
      if (code === 'auth/unauthorized-domain') {
        const host = window.location.hostname || 'this domain';
        showAuthError(t(
          `Google Sign-In साठी Firebase Console → Authentication → Settings → Authorized domains मध्ये "${host}" जोडावे लागेल. तोपर्यंत वरचा ईमेल/युजरनेम व पासवर्ड लॉगिन तात्काळ वापरा!`,
          `To enable Google Sign-In on "${host}", add it in Firebase Console → Authentication → Settings → Authorized domains. Meanwhile, you can sign in immediately with Email/Username & Password above!`
        ));
      } else if (code === 'auth/popup-closed-by-user' || code === 'auth/cancelled-popup-request') {
        showAuthError(t('Google लॉगिन रद्द केले गेले.', 'Google sign-in popup was closed.'));
      } else {
        showAuthError(t(
          `Google लॉगिन त्रुटी: ${err?.message || code || 'अज्ञात त्रुटी'}. कृपया ईमेल/पासवर्ड लॉगिन वापरा.`,
          `Google sign-in error: ${err?.message || code || 'Unknown error'}. Please use Email/Password login.`
        ));
      }
    } finally {
      btn.disabled = false;
    }
  });

  // ---------- Global Header Controls ----------
  document.getElementById('userAccountBtn').addEventListener('click', openAccountModal);
  document.getElementById('brandBtn').addEventListener('click', openBrandingModal);
  document.getElementById('themeModalBtn').addEventListener('click', openBrandingModal);
  document.getElementById('sheetSyncTopBtn').addEventListener('click', openSheetSyncModal);
  document.getElementById('kvarTopBtn').addEventListener('click', () => openKvarModal(null));
  document.getElementById('addReadingFab').addEventListener('click', () => openReadingModal(null));
  document.getElementById('modalCloseBtn').addEventListener('click', closeModal);
  document.getElementById('modalBackdrop').addEventListener('click', e => {
    if (e.target.id === 'modalBackdrop') closeModal();
  });

  document.getElementById('darkModeToggleBtn').addEventListener('click', () => {
    const cur = THEMES.find(th => th.id === state.branding.themeId) || THEMES[0];
    state.branding.themeId = cur.isDark ? 'emerald' : 'cyber';
    saveState();
    render();
  });

  document.getElementById('langToggleBtn').addEventListener('click', () => {
    state.isMr = !state.isMr;
    saveState();
    render();
  });

  // Initialize Auth & Boot App
  initFirebaseAuth();
  if (currentUser) {
    showAppForUser(currentUser);
  } else {
    showAuthGate();
  }
})();
