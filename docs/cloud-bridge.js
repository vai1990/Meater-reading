import { initializeApp, getApps } from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js';
import {
  getAuth,
  GoogleAuthProvider,
  signInWithPopup,
  signOut,
  onAuthStateChanged
} from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js';
import {
  getFirestore,
  collection,
  doc,
  getDoc,
  getDocs,
  setDoc,
  updateDoc,
  query,
  where,
  onSnapshot,
  serverTimestamp
} from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-firestore.js';

const FIREBASE_CONFIG = {
  apiKey: 'AIzaSyAMW1Iir84mvLoprAL461YfalV-ISx19j4',
  authDomain: 'gen-lang-client-0644163839.firebaseapp.com',
  projectId: 'gen-lang-client-0644163839',
  storageBucket: 'gen-lang-client-0644163839.firebasestorage.app',
  messagingSenderId: '418787384815',
  appId: '1:418787384815:web:30e8e982723daebe7c2a1a'
};

// Exact named Firestore database used by the Android Mobile App (firebase_applet_config.xml)
const FIRESTORE_DATABASE_ID = 'ai-studio-android-meterrea-90820e89-042a-439a-a3d2-171d8a4538f2';

const app = getApps().length ? getApps()[0] : initializeApp(FIREBASE_CONFIG);
const auth = getAuth(app);
const db = getFirestore(app, FIRESTORE_DATABASE_ID);

function sanitizeDocId(raw) {
  const cleaned = String(raw || '').trim().replace(/[^a-zA-Z0-9_\-]/g, '_');
  return cleaned ? cleaned.slice(0, 100) : `doc_${Date.now()}`;
}

function generateDefaultInviteCode(uid) {
  const prefix = String(uid || '').replace(/[^a-zA-Z0-9]/g, '').toUpperCase().slice(0, 6);
  return `TEAM_${prefix.length >= 3 ? prefix : '101A'}`;
}

function dedupeList(arr, max = 10) {
  return Array.from(new Set(arr.filter(Boolean))).slice(0, max);
}

async function signInWithGoogle() {
  const provider = new GoogleAuthProvider();
  provider.setCustomParameters({ prompt: 'select_account' });
  const res = await signInWithPopup(auth, provider);
  return res.user;
}

async function signOutGoogle() {
  await signOut(auth);
}

function onAuthChange(cb) {
  return onAuthStateChanged(auth, cb);
}

function getCurrentFirebaseUser() {
  return auth.currentUser;
}

async function ensureWorkspace(uid, displayName) {
  const wsQ = query(collection(db, 'workspaces'), where('memberUids', 'array-contains', uid));
  const snap = await getDocs(wsQ);
  if (!snap.empty) {
    const first = snap.docs[0];
    return { id: first.id, ...first.data() };
  }

  const cleanCode = sanitizeDocId(generateDefaultInviteCode(uid));
  const ref = doc(db, 'workspaces', cleanCode);
  const existing = await getDoc(ref);
  if (existing.exists()) {
    const data = existing.data();
    const members = Array.isArray(data.memberUids) ? data.memberUids : [];
    if (members.includes(uid)) {
      return { id: existing.id, ...data };
    }
    if (members.length < 10) {
      const updatedUids = dedupeList([...members, uid], 10);
      const updatedNames = dedupeList([...(data.memberNames || []), (displayName || 'Colleague').slice(0, 100)], 10);
      await updateDoc(ref, {
        memberUids: updatedUids,
        memberNames: updatedNames,
        updatedAt: serverTimestamp()
      });
      return { id: existing.id, ...data, memberUids: updatedUids, memberNames: updatedNames };
    }
  }

  const safeName = String(displayName || 'Team Admin').trim().slice(0, 100) || 'Team Admin';
  const payload = {
    ownerId: uid,
    name: 'MeterRead Team',
    inviteCode: cleanCode,
    memberUids: [uid],
    memberNames: [safeName],
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp()
  };
  await setDoc(ref, payload);
  return { id: cleanCode, ...payload };
}

async function joinWorkspace(inviteCode, uid, displayName) {
  const cleanCode = sanitizeDocId(String(inviteCode || '').toUpperCase());
  const ref = doc(db, 'workspaces', cleanCode);
  const snap = await getDoc(ref);
  if (!snap.exists()) {
    throw new Error(`Team code '${cleanCode}' not found`);
  }
  const data = snap.data();
  const members = Array.isArray(data.memberUids) ? data.memberUids : [];
  if (members.includes(uid)) {
    return { id: snap.id, ...data };
  }
  if (members.length >= 10) {
    throw new Error('Team is full (max 10 members)');
  }
  const updatedUids = [...members, uid];
  const safeName = String(displayName || 'Colleague').trim().slice(0, 100) || 'Colleague';
  const updatedNames = [...(data.memberNames || []), safeName].slice(0, 10);
  await updateDoc(ref, {
    memberUids: updatedUids,
    memberNames: updatedNames,
    updatedAt: serverTimestamp()
  });
  return { id: snap.id, ...data, memberUids: updatedUids, memberNames: updatedNames };
}

function subscribeCloudData(uid, { onWorkspaces, onCustomers, onReadings, onError }) {
  const wsQ = query(collection(db, 'workspaces'), where('memberUids', 'array-contains', uid));
  const custQ = query(collection(db, 'cloud_customers'), where('memberUids', 'array-contains', uid));
  const readQ = query(collection(db, 'cloud_readings'), where('memberUids', 'array-contains', uid));

  const unsubWs = onSnapshot(
    wsQ,
    snap => onWorkspaces?.(snap.docs.map(d => ({ id: d.id, ...d.data() }))),
    err => onError?.('workspaces', err)
  );
  const unsubCust = onSnapshot(
    custQ,
    snap => onCustomers?.(snap.docs.map(d => ({ id: d.id, ...d.data() }))),
    err => onError?.('cloud_customers', err)
  );
  const unsubRead = onSnapshot(
    readQ,
    snap => onReadings?.(snap.docs.map(d => ({ id: d.id, ...d.data() }))),
    err => onError?.('cloud_readings', err)
  );

  return () => {
    try { unsubWs(); } catch {}
    try { unsubCust(); } catch {}
    try { unsubRead(); } catch {}
  };
}

async function fetchCloudSnapshotNow(uid) {
  const [wsSnap, custSnap, readSnap] = await Promise.all([
    getDocs(query(collection(db, 'workspaces'), where('memberUids', 'array-contains', uid))),
    getDocs(query(collection(db, 'cloud_customers'), where('memberUids', 'array-contains', uid))),
    getDocs(query(collection(db, 'cloud_readings'), where('memberUids', 'array-contains', uid)))
  ]);
  return {
    workspaces: wsSnap.docs.map(d => ({ id: d.id, ...d.data() })),
    customers: custSnap.docs.map(d => ({ id: d.id, ...d.data() })),
    readings: readSnap.docs.map(d => ({ id: d.id, ...d.data() }))
  };
}

async function pushCustomer(workspace, customer, uid, displayName) {
  const cleanWorkspaceId = sanitizeDocId(workspace.inviteCode || workspace.id);
  const meter = String(customer.meterNumber || 'MTR-01').trim().slice(0, 64) || 'MTR-01';
  const docId = sanitizeDocId(`${cleanWorkspaceId}_${meter}`);
  const ref = doc(db, 'cloud_customers', docId);
  const memberUids = dedupeList([...(workspace.memberUids || []), uid], 10);

  const fields = {
    memberUids,
    name: String(customer.name || 'Customer').trim().slice(0, 150) || 'Customer',
    address: String(customer.address || '').trim().slice(0, 300),
    meterNumber: meter,
    consumerNumber: String(customer.consumerNumber || '').trim().slice(0, 64),
    phoneNumber: String(customer.phoneNumber || '').trim().slice(0, 32),
    connectionType: String(customer.connectionType || '3-Phase Industrial').trim().slice(0, 64) || '3-Phase Industrial',
    sanctionedLoad: String(customer.sanctionedLoad || '').trim().slice(0, 64)
  };

  try {
    await setDoc(ref, {
      workspaceId: cleanWorkspaceId,
      ownerId: uid,
      createdByName: String(displayName || 'Team Member').trim().slice(0, 100) || 'Team Member',
      ...fields,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
  } catch {
    await updateDoc(ref, {
      ...fields,
      updatedAt: serverTimestamp()
    });
  }
  return docId;
}

async function pushReading(workspace, customer, reading, uid, displayName) {
  const cleanWorkspaceId = sanitizeDocId(workspace.inviteCode || workspace.id);
  const meter = String(customer.meterNumber || 'MTR-01').trim().slice(0, 64) || 'MTR-01';
  const ts = Math.max(0, Math.floor(Number(reading.timestamp) || Date.now()));
  const docId = sanitizeDocId(`${cleanWorkspaceId}_${meter}_${ts}`);
  const ref = doc(db, 'cloud_readings', docId);
  const memberUids = dedupeList([...(workspace.memberUids || []), uid], 10);

  const safeKwh = Number(reading.kwh) > 0 ? Number(reading.kwh) : 0.01;
  const safeKvarh = Number(reading.kvarh) >= 0 ? Number(reading.kvarh) : 0;
  const safeKvah = Number(reading.kvah) > 0 ? Number(reading.kvah) : safeKwh;
  const safePf = Math.min(1, Math.max(0, Number(reading.powerFactor) || 0));
  const safePrevKwh = Math.max(0, Number(reading.previousKwh) || 0);
  const safeConsumed = Math.max(0, Number(reading.unitsConsumed) || 0);

  const fields = {
    memberUids,
    customerMeterNumber: meter,
    customerName: String(customer.name || 'Customer').trim().slice(0, 150) || 'Customer',
    readingDate: String(reading.readingDate || '2026-11-20').trim().slice(0, 32) || '2026-11-20',
    readingTime: String(reading.readingTime || '10:00 AM').trim().slice(0, 32) || '10:00 AM',
    readingTimestamp: ts,
    kwh: safeKwh,
    kvarh: safeKvarh,
    kvah: safeKvah,
    powerFactor: safePf,
    isPfAutoCalculated: Boolean(reading.isPfAutoCalculated ?? true),
    previousKwh: safePrevKwh,
    unitsConsumed: safeConsumed,
    remarks: String(reading.remarks || '').trim().slice(0, 300)
  };

  try {
    await setDoc(ref, {
      workspaceId: cleanWorkspaceId,
      ownerId: uid,
      createdByName: String(displayName || 'Team Member').trim().slice(0, 100) || 'Team Member',
      ...fields,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    });
  } catch {
    await updateDoc(ref, {
      ...fields,
      updatedAt: serverTimestamp()
    });
  }
  return docId;
}

window.SmartMeterCloud = {
  databaseId: FIRESTORE_DATABASE_ID,
  signInWithGoogle,
  signOutGoogle,
  onAuthChange,
  getCurrentFirebaseUser,
  ensureWorkspace,
  joinWorkspace,
  subscribeCloudData,
  fetchCloudSnapshotNow,
  pushCustomer,
  pushReading
};

window.dispatchEvent(new Event('smartmeter-cloud-ready'));
