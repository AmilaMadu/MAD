# MAD — Mobile Application Development: Word Guessing Game

**Short summary**  
A mobile word-guessing game built for ITE 2152 (MAD). The app picks a random secret word from an online API and challenges the user to guess it within a limited number of attempts while managing score and time. This repository contains the app source, a simple backend (if used), demo video link and documentation.

---

## Table of contents
- [Features](#features)
- [How it works](#how-it-works)
- [Tech stack](#tech-stack)
- [How to run (developer)](#how-to-run-developer)
- [APIs used](#apis-used)
- [Demo & submission files](#demo--submission-files)
- [Project structure](#project-structure)
- [Known issues & future improvements](#known-issues--future-improvements)
- [Author](#author)
- [License](#license)

---

## Features
- Onboarding: app asks and remembers the player's name.  
- Guess the word: submit whole-word guesses. Score starts at **100**; each wrong guess = **-10**. Max 10 attempts.  
- Letter occurrence: reveal how many times a chosen letter appears (cost **5** points).  
- Word length: show how many letters the secret word has (cost **5** points).  
- Tip/clue: one hint allowed after the 5th failed attempt (rhyming or synonym).  
- Timer: measures how long the player takes to guess correctly.  
- Leaderboard (optional): posts scores/times to a backend service for global rankings.

---

## How it works
1. On app launch, ask for the player's name (stored locally).  
2. App fetches a random word from an external word API and hides it from the player.  
3. Player may guess up to 10 times. Wrong guesses subtract points.  
4. Player can request clues (letter count or letter occurrences) at point cost.  
5. On success, player advances to a higher level (longer words). On failure (0 points or 10 failed guesses), new word & reset to 100 points.

---

## Tech stack
- Mobile: (specify) Android (Kotlin) / Flutter / React Native — *replace with your actual stack*  
- Networking: fetch random words from public APIs (see below).  
- Storage: SharedPreferences / local storage for player name and local leaderboard.  
- Optional backend: simple REST service (e.g., dreamlo) for global leaderboard.

---

## APIs used
- Random word: `https://api.api-ninjas.com/v1/randomword` or `https://random-word-api.herokuapp.com/word`  
- Rhyme / thesaurus (for hints): API Ninjas Rhyme & Thesaurus.  
- Leaderboard: `https://dreamlo.com/` (if implemented)

---

## How to run (developer)
> Replace `android/ios` steps with your actual platform steps.

**Android (Kotlin)**
```bash
# clone repo
git clone <your-repo-url>
cd <repo>

# open in Android Studio
# or via command line:
./gradlew assembleDebug

# install to device/emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
