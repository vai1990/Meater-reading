const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_owner_123";
const BOB_UID = "bob_colleague_456";
const EVE_UID = "eve_outsider_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or create workspaces, customers, or readings", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("workspaces").doc("TEAM101").get());
  await assertFails(unauthDb.collection("cloud_customers").get());
  await assertFails(unauthDb.collection("cloud_readings").get());
});

test("Workspace creation & Colleague Join via invite code succeeds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("workspaces").doc("TEAM101").set({
      ownerId: ALICE_UID,
      name: "Anand Textiles Team",
      inviteCode: "TEAM101",
      memberUids: [ALICE_UID],
      memberNames: ["Vaibhav Ghag"],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Colleague Bob fetches workspace by invite code and joins
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("workspaces").doc("TEAM101").get());

  await assertSucceeds(
    bobDb.collection("workspaces").doc("TEAM101").update({
      memberUids: [ALICE_UID, BOB_UID],
      memberNames: ["Vaibhav Ghag", "Rahul Patil"],
      updatedAt: new Date(),
    })
  );
});

test("Colleague can log reading in shared workspace and Owner can read/list it; Outsider is blocked", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore();
  const now = new Date();

  // Colleague Bob logs a reading shared with [ALICE_UID, BOB_UID]
  await assertSucceeds(
    bobDb.collection("cloud_readings").doc("reading_1").set({
      workspaceId: "TEAM101",
      ownerId: BOB_UID,
      createdByName: "Rahul Patil",
      memberUids: [ALICE_UID, BOB_UID],
      customerMeterNumber: "LT-449201",
      customerName: "Anand Textiles",
      readingDate: "15-Nov-2026",
      readingTime: "09:30 AM",
      readingTimestamp: 1731663000000,
      kwh: 13200.5,
      kvarh: 3650.0,
      kvah: 13695.0,
      powerFactor: 0.964,
      isPfAutoCalculated: true,
      previousKwh: 12820.0,
      unitsConsumed: 380.5,
      remarks: "Logged by colleague Rahul",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice (Owner) can read and list the colleague's entry using memberUids filter
  await assertSucceeds(aliceDb.collection("cloud_readings").doc("reading_1").get());
  await assertSucceeds(
    aliceDb.collection("cloud_readings").where("memberUids", "array-contains", ALICE_UID).get()
  );

  // Outsider Eve cannot get or list the reading
  await assertFails(eveDb.collection("cloud_readings").doc("reading_1").get());
  await assertFails(eveDb.collection("cloud_readings").get());
});

test("Shadow Update / Ghost Field injection is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertFails(
    aliceDb.collection("cloud_customers").doc("cust_1").set({
      workspaceId: "TEAM101",
      ownerId: ALICE_UID,
      createdByName: "Vaibhav",
      memberUids: [ALICE_UID],
      name: "Anand Textiles",
      address: "MIDC",
      meterNumber: "LT-449201",
      consumerNumber: "CA-123",
      phoneNumber: "9876543210",
      connectionType: "3-Phase Industrial",
      sanctionedLoad: "65 kW",
      createdAt: now,
      updatedAt: now,
      isAdminGhostField: true,
    })
  );
});
