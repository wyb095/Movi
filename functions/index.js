"use strict";

const crypto = require("node:crypto");
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {onDocumentUpdated} = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

const STATUS_ACCEPTED = "ACCEPTED";
const STATUS_PICKED_UP = "PICKED_UP";
const STATUS_DELIVERED = "DELIVERED";
const STATUS_CONFIRMED = "CONFIRMED";

exports.verifyRealName = onCall(async (request) => {
  if (!request.auth?.uid) {
    throw new HttpsError("unauthenticated", "Please sign in before verifying identity.");
  }

  const realName = String(request.data?.realName || "").trim();
  const idCardNumber = String(request.data?.idCardNumber || "").trim().toUpperCase();
  if (realName.length < 2 || !looksLikeIdentityDocument(idCardNumber)) {
    throw new HttpsError("invalid-argument", "Invalid verification payload.");
  }

  const appCode = process.env.ALIYUN_APPCODE;
  if (!appCode) {
    throw new HttpsError(
        "failed-precondition",
        "Aliyun verification is not configured. Set ALIYUN_APPCODE before deployment."
    );
  }

  const providerResponse = await fetch(
      "https://idcardcert.market.alicloudapi.com/idcard" +
      `?idCard=${encodeURIComponent(idCardNumber)}` +
      `&name=${encodeURIComponent(realName)}`,
      {
        headers: {
          Authorization: `APPCODE ${appCode}`,
        },
      }
  );

  if (!providerResponse.ok) {
    logger.error("Verification provider request failed", {
      status: providerResponse.status,
      uid: request.auth.uid,
    });
    throw new HttpsError("internal", "Verification provider request failed.");
  }

  const payload = await providerResponse.json();
  const statusCode = String(payload.status || payload.code || payload?.data?.status || "");
  if (statusCode !== "01") {
    const providerMessage = String(payload.msg || payload.message || "Verification failed.");
    throw new HttpsError("permission-denied", providerMessage);
  }

  const userRef = db.collection("users").doc(request.auth.uid);
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(userRef);
    const existing = snap.data() || {};
    const trustScore = calculateTrustScore({
      isVerified: existing.isVerified === true,
      hasRealNameVerification: true,
      rating: Number(existing.rating || 0),
      totalReviews: Number(existing.totalReviews || 0),
      tasksCompleted: Number(existing.tasksCompleted || 0),
    });
    tx.set(
        userRef,
        {
          realNameVerification: {
            realName,
            idCardNumberHash: sha256(idCardNumber),
            idCardLast4: idCardNumber.slice(-4),
            verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
            verificationProvider: "aliyun",
          },
          trustScore,
          trustBadge: trustBadgeFor(trustScore),
        },
        {merge: true}
    );
  });

  return {
    realName,
    idCardNumberHash: sha256(idCardNumber),
    idCardLast4: idCardNumber.slice(-4),
    verificationProvider: "aliyun",
  };
});

exports.sendTaskStatusNotification = onDocumentUpdated("tasks/{taskId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  if (!before || !after || before.status === after.status) return;

  const nextStatus = String(after.status || "");
  const taskTitle = String(after.title || "your task");
  let targetUserId = null;
  let payload = null;

  switch (nextStatus) {
    case STATUS_ACCEPTED:
      targetUserId = after.requesterId || null;
      payload = {
        title: "Task accepted",
        body: `${after.carrierName || "A carrier"} accepted "${taskTitle}".`,
        userRole: "requester",
      };
      break;
    case STATUS_PICKED_UP:
      targetUserId = after.requesterId || null;
      payload = {
        title: "Item picked up",
        body: `${after.carrierName || "Your carrier"} picked up "${taskTitle}".`,
        userRole: "requester",
      };
      break;
    case STATUS_DELIVERED:
      targetUserId = after.requesterId || null;
      payload = {
        title: "Item delivered",
        body: `"${taskTitle}" is marked delivered. Please confirm receipt.`,
        userRole: "requester",
      };
      break;
    case STATUS_CONFIRMED:
      targetUserId = after.carrierId || null;
      payload = {
        title: "Task completed",
        body: `"${taskTitle}" was confirmed and payment has been released.`,
        userRole: "carrier",
      };
      break;
    default:
      return;
  }

  if (!targetUserId || !payload) return;
  const userSnap = await db.collection("users").doc(targetUserId).get();
  const token = userSnap.get("fcmToken");
  if (!token) {
    logger.info("Skipping notification because the user has no FCM token", {
      targetUserId,
      taskId: event.params.taskId,
      status: nextStatus,
    });
    return;
  }

  await messaging.send({
    token,
    notification: {
      title: payload.title,
      body: payload.body,
    },
    data: {
      type: "TASK_STATUS_CHANGE",
      taskId: event.params.taskId,
      status: nextStatus,
      userRole: payload.userRole,
      title: payload.title,
      body: payload.body,
    },
  });
});

function looksLikeIdentityDocument(value) {
  return /^\d{17}[\dX]$/i.test(value) || /^[A-Z0-9]{6,18}$/.test(value);
}

function sha256(value) {
  return crypto.createHash("sha256").update(value).digest("hex");
}

function calculateTrustScore(input) {
  const ratingPoints = Math.round((Math.min(Math.max(input.rating || 0, 0), 5) / 5) * 20);
  const reviewPoints = Math.min(Math.max(input.totalReviews || 0, 0), 10);
  const completedPoints = Math.min(Math.max(input.tasksCompleted || 0, 0), 15);
  return Math.min(
      100,
      40 +
      (input.isVerified ? 5 : 0) +
      (input.hasRealNameVerification ? 10 : 0) +
      ratingPoints +
      reviewPoints +
      completedPoints
  );
}

function trustBadgeFor(score) {
  if (score <= 59) return "BRONZE";
  if (score <= 74) return "SILVER";
  if (score <= 89) return "GOLD";
  return "PLATINUM";
}
