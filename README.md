# MoodTunes — upgraded Java mood-based music recommender

A refreshed, responsive version of the MoodTunes app using Java's built-in `HttpServer`.

## Requirements
- JDK 17 or newer
- Internet connection for Google Fonts, YouTube embeds, and Spotify embeds

## Run on Windows
1. Extract this ZIP.
2. Open the extracted folder in Terminal / Command Prompt.
3. Run `run.bat`.
4. Open http://localhost:8000 in your browser.

Or run manually:
```bash
javac MoodRecommenderServer2.java
java MoodRecommenderServer2
```

## Run on macOS / Linux
```bash
chmod +x run.sh
./run.sh
```

To use another port:
```bash
java -Dport=8080 MoodRecommenderServer2
```

## Included
- Responsive dark glass panels, ambient gradients, subtle grain texture, hover lighting, and mobile layouts
- Ten mood cards and mood-text matching
- YouTube playlist navigation with previous / next / shuffle
- Spotify playlist embeds
- Mood analytics with proportional bars
- Concurrent request handling and thread-safe in-memory counters
- UTF-8 response handling, URL query decoding, basic security headers, and reduced-motion support

## Notes
- Mood text detection is a keyword-based demo, not a trained AI sentiment model.
- Analytics are stored in memory and reset when the Java server restarts.
- YouTube and Spotify content requires internet access and may depend on embed availability, regional access, or platform settings.
- The Spotify playlist IDs and YouTube video IDs are carried over from the provided starter code.
