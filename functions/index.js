const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore, FieldValue} = require("firebase-admin/firestore");

// Initialize Firebase Admin SDK
initializeApp();
const db = getFirestore();

/**
 * Submits a new score to the leaderboard.
 * Expects data in the request body: { playerName: string, score: number, timeTakenSeconds: number }
 */
exports.submitScore = onCall(async (request) => {
  // 1. Get data from the app's request
  const playerName = request.data.playerName;
  const score = request.data.score;
  const timeTakenSeconds = request.data.timeTakenSeconds;

  // 2. Validate the data to ensure it's correct
  if (!playerName || typeof playerName !== "string" || playerName.trim() === "") {
    throw new HttpsError("invalid-argument", "Player name is required and must be a non-empty string.");
  }
  if (typeof score !== "number" || isNaN(score)) {
    throw new HttpsError("invalid-argument", "Score must be a valid number.");
  }
  if (typeof timeTakenSeconds !== "number" || isNaN(timeTakenSeconds)) {
    throw new HttpsError("invalid-argument", "Time taken must be a valid number.");
  }

  // 3. Try to save the validated data to the database
  try {
    await db.collection("leaderboardEntries").add({
      playerName: playerName.trim().substring(0, 25), // Limit player name length
      score: Math.round(score),
      timeTakenSeconds: Math.round(timeTakenSeconds),
      timestamp: FieldValue.serverTimestamp(), // Add a server-side timestamp
    });
    // 4. Send a success response back to the app
    return {success: true, message: "Score submitted successfully!"};
  } catch (error) {
    console.error("Error submitting score:", error);
    // If something goes wrong, throw an error
    throw new HttpsError("internal", "An unexpected error occurred while submitting the score.");
  }
});

/**
 * Retrieves the top leaderboard entries.
 * Expects optional data in the request body: { limit: number } (defaults to 10)
 */
exports.getLeaderboard = onCall(async (request) => {
  // 1. Get the requested limit, or default to 10. Also set a max reasonable limit.
  const limit = request.data.limit || 10;
  const maxLimit = 50;

  if (limit > maxLimit) {
    throw new HttpsError("invalid-argument", `Limit cannot exceed ${maxLimit}.`);
  }

  // 2. Try to fetch the data from the database
  try {
    const snapshot = await db.collection("leaderboardEntries")
        .orderBy("score", "desc") // Sort by highest score first
        .orderBy("timeTakenSeconds", "asc") // For ties, the fastest time wins
        .limit(Math.floor(limit)) // Limit the number of results
        .get();

    // 3. Format the data to send back to the app
    const leaderboard = [];
    snapshot.forEach((doc) => {
      leaderboard.push({id: doc.id, ...doc.data()});
    });

    // 4. Send the formatted data back to the app
    return {success: true, leaderboard: leaderboard};
  } catch (error) {
    console.error("Error fetching leaderboard:", error);
    // If something goes wrong, throw an error
    throw new HttpsError("internal", "An unexpected error occurred while fetching the leaderboard.");
  }
});
