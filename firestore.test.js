const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_123";
const BOB_UID = "bob_uid_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

test("Unauthenticated user cannot read or write pets, vaccinations, or healthRecords", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = pastDate();

  await assertFails(unauthDb.collection(`users/${ALICE_UID}/pets`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/vaccinations`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/healthRecords`).get());

  await assertFails(
    unauthDb.doc(`users/${ALICE_UID}/pets/pet_1`).set({
      userId: ALICE_UID,
      name: "Milo",
      species: "DOG",
      breed: "Golden Retriever",
      ageYears: 3,
      ageMonths: 2,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Owner can list empty user-scoped collections and perform full Pet CRUD", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(10000);

  // Empty list queries succeed
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/pets`).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/vaccinations`).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/healthRecords`).get());

  // Create pet (mirroring Kotlin PetRepository payload without "id" field)
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/pets/pet_1`).set({
      userId: ALICE_UID,
      name: "Milo",
      species: "DOG",
      breed: "Golden Retriever",
      ageYears: 3,
      ageMonths: 4,
      weightKg: 28.5,
      microchipId: "985112000123456",
      notes: "Loves peanut butter",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/pets/pet_1`).update({
      name: "Milo Bear",
      ageYears: 4,
      ageMonths: 0,
      updatedAt: pastDate(1000),
    })
  );

  // Read & list populated collection succeeds
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/pets/pet_1`).get());
  await assertSucceeds(aliceDb.collection(`users/${ALICE_UID}/pets`).get());

  // Delete succeeds
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/pets/pet_1`).delete());
});

test("Pet validation rejects invalid fields, ghost fields, @DocumentId 'id' key, and immutable edits", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(10000);

  // Reject "id" field in payload (would break @DocumentId deserialization)
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/pets/pet_bad_id`).set({
      id: "pet_bad_id",
      userId: ALICE_UID,
      name: "Luna",
      species: "CAT",
      breed: "Tabby",
      ageYears: 2,
      ageMonths: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/pets/pet_ghost`).set({
      userId: ALICE_UID,
      name: "Luna",
      species: "CAT",
      breed: "Tabby",
      ageYears: 2,
      ageMonths: 0,
      isVerified: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject invalid species enum
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/pets/pet_species`).set({
      userId: ALICE_UID,
      name: "Luna",
      species: "DRAGON",
      breed: "Mythic",
      ageYears: 2,
      ageMonths: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/pets/pet_future`).set({
      userId: ALICE_UID,
      name: "Luna",
      species: "CAT",
      breed: "Tabby",
      ageYears: 2,
      ageMonths: 0,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: created,
    })
  );

  // Create valid pet then attempt immutable createdAt change
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/pets/pet_immutable`).set({
      userId: ALICE_UID,
      name: "Luna",
      species: "CAT",
      breed: "Tabby",
      ageYears: 2,
      ageMonths: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/pets/pet_immutable`).update({
      createdAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );
});

test("Vaccination and HealthRecord enforce parent pet existence, schema bounds, and immutable petId", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = pastDate(10000);
  const nowMs = Date.now() - 86400000;

  // Reject vaccination for non-existent pet
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/vaccinations/vac_orphan`).set({
      userId: ALICE_UID,
      petId: "missing_pet",
      vaccineName: "Rabies",
      administeredDateEpochMs: nowMs,
      status: "COMPLETED",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create parent pet first
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/pets/pet_1`).set({
      userId: ALICE_UID,
      name: "Milo",
      species: "DOG",
      breed: "Beagle",
      ageYears: 2,
      ageMonths: 6,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid vaccination create + update + delete
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/vaccinations/vac_1`).set({
      userId: ALICE_UID,
      petId: "pet_1",
      vaccineName: "Rabies 1-Year",
      administeredDateEpochMs: nowMs,
      nextDueDateEpochMs: nowMs + 31536000000,
      clinicOrVet: "Green Valley Vet",
      notes: "No reaction",
      status: "COMPLETED",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/vaccinations/vac_1`).update({
      status: "SCHEDULED",
      updatedAt: pastDate(1000),
    })
  );

  // Reject changing petId on update
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/vaccinations/vac_1`).update({
      petId: "other_pet",
      updatedAt: pastDate(1000),
    })
  );

  // Reject orphaned healthRecord
  await assertFails(
    aliceDb.doc(`users/${ALICE_UID}/healthRecords/hr_orphan`).set({
      userId: ALICE_UID,
      petId: "missing_pet",
      title: "Annual Checkup",
      category: "CHECKUP",
      recordDateEpochMs: nowMs,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Valid healthRecord create + update + delete
  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/healthRecords/hr_1`).set({
      userId: ALICE_UID,
      petId: "pet_1",
      title: "Annual Wellness Exam",
      category: "CHECKUP",
      recordDateEpochMs: nowMs,
      vetName: "Dr. Rivera",
      details: "Healthy teeth and coat.",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/healthRecords/hr_1`).update({
      details: "Healthy teeth, coat, and joints.",
      updatedAt: pastDate(1000),
    })
  );

  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/vaccinations/vac_1`).delete());
  await assertSucceeds(aliceDb.doc(`users/${ALICE_UID}/healthRecords/hr_1`).delete());
});

test("Cross-user access is completely denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = pastDate(10000);

  await assertSucceeds(
    aliceDb.doc(`users/${ALICE_UID}/pets/pet_alice`).set({
      userId: ALICE_UID,
      name: "Milo",
      species: "DOG",
      breed: "Poodle",
      ageYears: 1,
      ageMonths: 3,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(bobDb.collection(`users/${ALICE_UID}/pets`).get());
  await assertFails(bobDb.doc(`users/${ALICE_UID}/pets/pet_alice`).get());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/pets/pet_alice`).update({
      name: "Stolen",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(bobDb.doc(`users/${ALICE_UID}/pets/pet_alice`).delete());
  await assertFails(
    bobDb.doc(`users/${ALICE_UID}/pets/pet_spoof`).set({
      userId: ALICE_UID,
      name: "Spoof",
      species: "CAT",
      breed: "Siamese",
      ageYears: 1,
      ageMonths: 0,
      createdAt: created,
      updatedAt: created,
    })
  );
});
