#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────
# firebase_deploy.sh
# Usage: ./scripts/firebase_deploy.sh YOUR_PROJECT_ID
# Requires: firebase-tools (npm install -g firebase-tools)
# ─────────────────────────────────────────────────────────────
set -euo pipefail

PROJECT_ID="${1:?Usage: $0 <firebase-project-id>}"

echo "🚀 Deploying Firestore rules to project: $PROJECT_ID"

# ── Firestore Security Rules ──────────────────────────────────
cat > /tmp/firestore.rules << 'RULES'
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helpers
    function isSignedIn() { return request.auth != null; }
    function isAdmin() {
      return isSignedIn() &&
        (request.auth.token.email.matches(".*@campusconnect\\.app") ||
         request.auth.token.email.matches(".*@admin\\.campusconnect\\.ke"));
    }

    // Notices: any signed-in user can read; only admin can write
    match /notices/{noticeId} {
      allow read: if isSignedIn();
      allow create, update, delete: if isAdmin();
      // Allow users to toggle their own favourite
      allow update: if isSignedIn() &&
        request.resource.data.diff(resource.data).affectedKeys().hasOnly(['isFavorite']);
    }

    // Timetable: read-only for all signed-in users
    match /timetable/{entryId} {
      allow read: if isSignedIn();
      allow write: if isAdmin();
    }

    // Events: read for all; RSVP update for signed-in users
    match /events/{eventId} {
      allow read: if isSignedIn();
      allow create, delete: if isAdmin();
      allow update: if isSignedIn() &&
        request.resource.data.diff(resource.data).affectedKeys()
          .hasOnly(['rsvpCount', 'hasRsvp']);
    }

    // Users: only own document
    match /users/{userId} {
      allow read, write: if isSignedIn() && request.auth.uid == userId;
    }
  }
}
RULES

firebase firestore:rules /tmp/firestore.rules --project "$PROJECT_ID"
echo "✅ Firestore rules deployed."

# ── Seed sample data ──────────────────────────────────────────
echo "🌱 Seeding sample data…"

firebase firestore:delete --all-collections --project "$PROJECT_ID" -y 2>/dev/null || true

# Seed via node inline script
node - <<NODE
const admin = require('firebase-admin');
const app = admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId: '${PROJECT_ID}'
});
const db = admin.firestore();

async function seed() {
  // Notices
  const notices = [
    { title: "Welcome to Campus Connect!", content: "Stay informed about campus updates in real-time.", category: "GENERAL", priority: "MEDIUM", author: "Admin", authorEmail: "admin@campusconnect.app", isActive: true, isFavorite: false, viewCount: 0, timestamp: admin.firestore.FieldValue.serverTimestamp(), tags: ["welcome"] },
    { title: "⚠️ Exam Timetable Released", content: "The end-of-semester exam timetable is now available on the student portal. Please check your exam dates and rooms.", category: "EXAM", priority: "HIGH", author: "Registrar", authorEmail: "registrar@campusconnect.app", isActive: true, isFavorite: false, viewCount: 0, timestamp: admin.firestore.FieldValue.serverTimestamp(), tags: ["exams", "timetable"] },
    { title: "🚨 URGENT: Campus Closure", content: "The campus will be closed this Friday for maintenance. All classes are moved online. Check your email for details.", category: "URGENT", priority: "CRITICAL", author: "Admin", authorEmail: "admin@campusconnect.app", isActive: true, isFavorite: false, viewCount: 0, timestamp: admin.firestore.FieldValue.serverTimestamp(), tags: ["urgent", "closure"] },
    { title: "Fee Payment Reminder", content: "Semester fees are due by the 30th of this month. Late payments attract a 10% penalty. Visit the finance office or pay online.", category: "FINANCE", priority: "HIGH", author: "Finance Office", authorEmail: "finance@campusconnect.app", isActive: true, isFavorite: false, viewCount: 0, timestamp: admin.firestore.FieldValue.serverTimestamp(), tags: ["fees", "finance"] }
  ];
  for (const n of notices) await db.collection('notices').add(n);

  // Timetable
  const timetable = [
    { courseCode: "CS301", courseName: "Data Structures & Algorithms", lecturer: "Dr. Kamau", room: "Lab 2", building: "ICT Block", dayOfWeek: 0, startHour: 8, startMinute: 0, endHour: 10, endMinute: 0, colorHex: "#1E3A8A", notes: "Bring laptop", semester: "Semester 1 2024" },
    { courseCode: "MATH201", courseName: "Calculus II", lecturer: "Prof. Wanjiku", room: "LH 3", building: "Main Block", dayOfWeek: 0, startHour: 11, startMinute: 0, endHour: 13, endMinute: 0, colorHex: "#059669", notes: "", semester: "Semester 1 2024" },
    { courseCode: "ENG102", courseName: "Technical Communication", lecturer: "Ms. Njeri", room: "LH 1", building: "Humanities", dayOfWeek: 1, startHour: 9, startMinute: 0, endHour: 11, endMinute: 0, colorHex: "#7C3AED", notes: "Assignment due", semester: "Semester 1 2024" },
    { courseCode: "CS301", courseName: "Data Structures & Algorithms", lecturer: "Dr. Kamau", room: "Lab 2", building: "ICT Block", dayOfWeek: 2, startHour: 14, startMinute: 0, endHour: 16, endMinute: 0, colorHex: "#1E3A8A", notes: "Practical session", semester: "Semester 1 2024" },
    { courseCode: "PHYS101", courseName: "Physics I", lecturer: "Dr. Omondi", room: "Lab 1", building: "Science Block", dayOfWeek: 3, startHour: 8, startMinute: 0, endHour: 10, endMinute: 0, colorHex: "#D97706", notes: "", semester: "Semester 1 2024" }
  ];
  for (const t of timetable) await db.collection('timetable').add(t);

  // Events
  const now = new Date();
  const events = [
    { title: "Tech Innovation Summit 2024", description: "Join us for a day of talks, workshops, and demos from leading tech companies. Network with industry professionals.", location: "Main Auditorium", latitude: -1.2921, longitude: 36.8219, startTimestamp: new Date(now.getTime() + 7*24*60*60*1000), endTimestamp: new Date(now.getTime() + 7*24*60*60*1000 + 8*60*60*1000), organizer: "Computer Science Department", maxAttendees: 200, rsvpCount: 45, hasRsvp: false, isOnline: false, category: "TECH" },
    { title: "Annual Sports Day", description: "Cheer on your department in the annual inter-departmental sports competition. Food and drinks provided!", location: "Sports Grounds", latitude: -1.2945, longitude: 36.8234, startTimestamp: new Date(now.getTime() + 14*24*60*60*1000), endTimestamp: new Date(now.getTime() + 14*24*60*60*1000 + 6*60*60*1000), organizer: "Sports Department", maxAttendees: 500, rsvpCount: 120, hasRsvp: false, isOnline: false, category: "SPORTS" }
  ];
  for (const e of events) await db.collection('events').add(e);

  console.log('✅ Seed complete: ' + notices.length + ' notices, ' + timetable.length + ' timetable entries, ' + events.length + ' events.');
  process.exit(0);
}
seed().catch(e => { console.error(e); process.exit(1); });
NODE

echo "🎉 Deployment complete for project: $PROJECT_ID"
