import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MoodRecommenderServer2 {
    private static final int PORT = Integer.parseInt(System.getProperty("port", "8000"));

    public record Track(
        String id,
        String title,
        String artist,
        String language,
        String mood,
        String youtubeId,
        String duration,
        String spotifyPlaylist
    ) {}

    public record MoodInfo(
        String key,
        String name,
        String emoji,
        String accent,
        String description,
        String spotifyPlaylist,
        List<String> vibes
    ) {}

    private static final Map<String, MoodInfo> MOODS = new LinkedHashMap<>();
    private static final List<Track> TRACKS = new ArrayList<>();
    private static final Map<String, AtomicInteger> MOOD_ANALYTICS = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> LANG_ANALYTICS = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> ARTIST_ANALYTICS = new ConcurrentHashMap<>();
    private static final AtomicInteger TOTAL_PLAYS = new AtomicInteger(0);

    static {
        // Initialize 10 Rich Moods
        registerMood("happy", "Happy", "😊", "#ffc857",
            "Bright, joyful, and radiant sounds to elevate your spirit",
            "37i9dQZF1DXdPec7aLTmlC", List.of("Joyful", "Celebration", "Sunny", "Upbeat"));

        registerMood("sad", "Sad", "🌧️", "#79a8ff",
            "Gentle melancholia and emotional spaces to let you feel deeply",
            "37i9dQZF1DWVrtsSlLKzro", List.of("Heartbreak", "Acoustic", "Reflective", "Slow"));

        registerMood("chill", "Chill", "🌊", "#69e0bd",
            "Lo-fi grooves, soft ambient beats, and calming downtempo rhythms",
            "37i9dQZF1DX4WYpdgoIcn6", List.of("Lo-Fi", "Relax", "Late Night", "Mellow"));

        registerMood("romantic", "Romantic", "💗", "#ff8ac7",
            "Soulful melodies, passionate vocals, and intimate butterflies",
            "37i9dQZF1DX50QitC6Oqtn", List.of("Love", "Intimate", "Warm", "Acoustic"));

        registerMood("energetic", "Energetic", "⚡", "#ff9b54",
            "High-octane bass, adrenaline rushes, and unstoppable tempo",
            "37i9dQZF1DX76Wlfdnj7AP", List.of("Workout", "Bass", "Hype", "Electric"));

        registerMood("party", "Party", "🎉", "#ff729f",
            "Dancefloor anthems, heavy drops, and unstoppable celebration vibes",
            "37i9dQZF1DX8a1tdzq5ZxC", List.of("Club", "Banger", "Dance", "Bass"));

        registerMood("motivated", "Motivated", "🚀", "#65e6d3",
            "Focus fuel, victory beats, and relentless drive for your ambitions",
            "37i9dQZF1DXdxcBWuJkbcy", List.of("Focus", "Gym", "Grind", "Victory"));

        registerMood("peaceful", "Peaceful", "🌿", "#76d6e8",
            "Zen soundscapes, acoustic warmth, and quiet moments of clarity",
            "37i9dQZF1DWZqd5JICZI0u", List.of("Meditation", "Piano", "Nature", "Calm"));

        registerMood("nostalgic", "Nostalgic", "🕰️", "#c4a7ff",
            "Golden era classics, timeless melodies, and cherished memories",
            "37i9dQZF1DX5Ejj0EkURtP", List.of("Retro", "Classics", "Memories", "Evergreen"));

        registerMood("angry", "Angry", "🔥", "#ff6b6b",
            "Raw distortion, aggressive riffs, and sound to vent pure intensity",
            "37i9dQZF1DX3rgXAfq1Eo0", List.of("Heavy Rock", "Metal", "Rage", "Intense"));

        // ==================== TRACKS CATALOG (80+ songs across languages & artists) ====================
        // HAPPY
        t("Happy", "Pharrell Williams", "English", "happy", "ZbZSe6N_BXs", "3:53");
        t("Can't Stop The Feeling!", "Justin Timberlake", "English", "happy", "ru0K8uYEZWw", "3:56");
        t("24K Magic", "Bruno Mars", "English", "happy", "UqyT8IEBkvY", "3:46");
        t("Ilahi", "Arijit Singh", "Hindi", "happy", "fdubeMFwuGs", "3:50");
        t("Gallan Goodiyaan", "Shankar Mahadevan, Neeti Mohan", "Hindi", "happy", "jCEdTq3j-0U", "4:12");
        t("Matargashti", "Mohit Chauhan, A.R. Rahman", "Hindi", "happy", "6vKucgAeF_Q", "5:29");
        t("Rowdy Baby", "Dhanush, Dhee, Yuvan Shankar Raja", "Tamil", "happy", "x6Q7c9RyMzk", "4:43");
        t("Enjoy Enjaami", "Dhee, Arivu, Santhosh Narayanan", "Tamil", "happy", "eYq7WapuDLU", "5:01");
        t("Chellamma", "Anirudh Ravichander, Jonita Gandhi", "Tamil", "happy", "20pYgYI1t40", "3:58");
        t("Butta Bomma", "Armaan Malik, Thaman S", "Telugu", "happy", "2mDCVzruYzQ", "3:18");
        t("Born To Shine", "Diljit Dosanjh", "Punjabi", "happy", "4Bi_54_e5kQ", "3:32");
        t("Dynamite", "BTS", "K-Pop", "happy", "gdZLi9oWNZg", "3:43");
        t("Despacito", "Luis Fonsi, Daddy Yankee", "Spanish", "happy", "kJQP7kiw5Fk", "4:42");

        // SAD
        t("Someone You Loved", "Lewis Capaldi", "English", "sad", "bCuhuePlP8o", "3:02");
        t("Easy On Me", "Adele", "English", "sad", "U3ASj1L6_sY", "3:44");
        t("Lovely", "Billie Eilish, Khalid", "English", "sad", "V1Pl8CzNzCw", "3:20");
        t("Channa Mereya", "Arijit Singh", "Hindi", "sad", "284VoJQT8oc", "4:49");
        t("Agar Tum Saath Ho", "Arijit Singh, Alka Yagnik, A.R. Rahman", "Hindi", "sad", "sK7riqg2mr4", "5:41");
        t("Tujhe Bhula Diya", "Mohit Chauhan, Shekhar", "Hindi", "sad", "mC-zw0zCCtg", "4:39");
        t("Kanave Kanave", "Anirudh Ravichander", "Tamil", "sad", "kH_N85VqM8Y", "4:45");
        t("Po Nee Po", "Anirudh Ravichander, Mohit Chauhan", "Tamil", "sad", "Q1NkmwW3Z_A", "3:42");
        t("Kannaana Kanney", "Sid Sriram, D. Imman", "Tamil", "sad", "co_kG_Rk1o0", "4:30");
        t("295", "Sidhu Moose Wala", "Punjabi", "sad", "n_FCrCQ6-9U", "4:30");
        t("Shinunoga E-Wa", "Fujii Kaze", "Japanese", "sad", "dFf4AgBNR1E", "3:05");

        // ROMANTIC
        t("Perfect", "Ed Sheeran", "English", "romantic", "2Vv-BfVoq4g", "4:23");
        t("Lover", "Taylor Swift", "English", "romantic", "-BjZmE2gtdo", "3:58");
        t("Die For You", "The Weeknd", "English", "romantic", "QLCpqdqeoII", "4:20");
        t("Just The Way You Are", "Bruno Mars", "English", "romantic", "LjhCEhWiKXk", "3:40");
        t("Kesariya", "Pritam, Arijit Singh", "Hindi", "romantic", "BddP6PYo2gs", "4:28");
        t("Tum Hi Ho", "Arijit Singh, Mithoon", "Hindi", "romantic", "Umqb9KENgmk", "4:22");
        t("Tere Hawaale", "Arijit Singh, Shreya Ghoshal", "Hindi", "romantic", "KUpwupYjPBw", "5:50");
        t("Arabic Kuthu (Halamithi Habibo)", "Anirudh Ravichander, Jonita Gandhi", "Tamil", "romantic", "KUN5Uf9mObQ", "4:40");
        t("Maruvaarthai", "Sid Sriram, Darbuka Siva", "Tamil", "romantic", "U3lygvgqFuo", "5:57");
        t("Samajavaragamana", "Sid Sriram, Thaman S", "Telugu", "romantic", "oc_E1110Ems", "4:34");
        t("Inkem Inkem Inkem Kaavaale", "Sid Sriram, Gopi Sundar", "Telugu", "romantic", "mODuC3p4hL0", "4:28");
        t("Lover", "Diljit Dosanjh", "Punjabi", "romantic", "mH_LFkW3Hag", "3:25");

        // CHILL
        t("Sunflower", "Post Malone, Swae Lee", "English", "chill", "ApXoWvfEYVU", "2:38");
        t("Ocean Eyes", "Billie Eilish", "English", "chill", "viimfQi_pUw", "3:20");
        t("Lofi Hip Hop Beats To Relax", "Lofi Girl", "English", "chill", "jfKfPfyJRdk", "3:30");
        t("Kasoor", "Prateek Kuhad", "Hindi", "chill", "TgtP_jWJb4g", "3:17");
        t("Sham (Aisha)", "Amit Trivedi, Nikhil D'Souza", "Hindi", "chill", "pM_w_K7B46s", "4:44");
        t("Enna Sona / Mental Manadhil", "A.R. Rahman", "Tamil", "chill", "mIqFjJ2rB-o", "3:34");
        t("Bodhai Kaname", "Anirudh Ravichander, Shashaa Tirupati", "Tamil", "chill", "h52-F7Cgq9I", "4:10");
        t("Excuses", "AP Dhillon, Gurinder Gill", "Punjabi", "chill", "vX2cDW8LUWk", "2:56");
        t("Hype Boy", "NewJeans", "K-Pop", "chill", "11cta61Wi0g", "2:59");
        t("Lemon", "Kenshi Yonezu", "Japanese", "chill", "SX_ViT4KD2k", "4:15");

        // ENERGETIC
        t("Levitating", "Dua Lipa", "English", "energetic", "TUVcZfQe-Kw", "3:23");
        t("Blinding Lights", "The Weeknd", "English", "energetic", "4NRXx6U8ABQ", "3:20");
        t("Believer", "Imagine Dragons", "English", "energetic", "7wtfhZwyrcc", "3:24");
        t("Malhari", "Vishal Dadlani, Sanjay Leela Bhansali", "Hindi", "energetic", "l_MyUGq7pgs", "4:05");
        t("Apna Time Aayega", "Ranveer Singh, DIVINE", "Hindi", "energetic", "jFGKJBPFdUA", "2:10");
        t("Vaathi Coming", "Anirudh Ravichander", "Tamil", "energetic", "vx6e_6hE_OQ", "3:50");
        t("Naa Ready (Leo)", "Thalapathy Vijay, Anirudh Ravichander", "Tamil", "energetic", "szvt1vD0Uug", "4:08");
        t("Naatu Naatu", "Rahul Sipligunj, Kaala Bhairava, MM Keeravaani", "Telugu", "energetic", "OsU0Hmu__n8", "4:35");
        t("Brown Munde", "AP Dhillon, Gurinder Gill", "Punjabi", "energetic", "VNs_cCtdbPc", "4:27");
        t("Pink Venom", "BLACKPINK", "K-Pop", "energetic", "gQlMMD8auMs", "3:06");

        // PARTY
        t("Uptown Funk", "Mark Ronson ft. Bruno Mars", "English", "party", "OPf0YbXqDm0", "4:30");
        t("Summer", "Calvin Harris", "English", "party", "ebXbPhFKV7Y", "3:44");
        t("Kala Chashma", "Badshah, Neha Kakkar", "Hindi", "party", "k4yXQkG2s1E", "3:07");
        t("Aankh Marey", "Mika Singh, Neha Kakkar", "Hindi", "party", "_KhQT-LGb-4", "3:32");
        t("Jimikki Ponnu", "Anirudh Ravichander, Jonita Gandhi", "Tamil", "party", "X_q3b3lqP-o", "3:44");
        t("Jalabulajangu", "Anirudh Ravichander", "Tamil", "party", "3E4I_pG2gZk", "3:26");
        t("Kurchi Madathapetti", "Thaman S, Mahesh Babu", "Telugu", "party", "q_0xI11r2p0", "3:36");
        t("Ramuloo Ramulaa", "Anurag Kulkarni, Thaman S", "Telugu", "party", "sk1Z-HCIRUc", "4:14");
        t("Tití Me Preguntó", "Bad Bunny", "Spanish", "party", "Cr8K8423QHM", "4:03");
        t("Super Shy", "NewJeans", "K-Pop", "party", "ArmDp-zijuc", "2:34");

        // MOTIVATED
        t("Lose Yourself", "Eminem", "English", "motivated", "_Yhyp-_hX2s", "5:26");
        t("Eye Of The Tiger", "Survivor", "English", "motivated", "btPJPFnesV4", "4:04");
        t("Unstoppable", "The Score", "English", "motivated", "glgPfdfl79E", "3:12");
        t("Kar Har Maidaan Fateh", "Sukhwinder Singh, Shreya Ghoshal", "Hindi", "motivated", "0deQ3813k94", "5:11");
        t("Zinda", "Siddharth Mahadevan", "Hindi", "motivated", "3nQ4n2N_b0M", "3:31");
        t("Chak De India", "Sukhwinder Singh, Salim-Sulaiman", "Hindi", "motivated", "O-rS8rZ_i4Y", "4:43");
        t("Surviva", "Anirudh Ravichander, Yogi B", "Tamil", "motivated", "Qc3iQk_2k8Y", "3:42");
        t("Ethir Neechal", "Anirudh Ravichander, Yo Yo Honey Singh", "Tamil", "motivated", "XmJ0Y_tV4bM", "4:24");
        t("Butter", "BTS", "K-Pop", "motivated", "WMweEpGlu_U", "2:44");
        t("DESPECHÁ", "Rosalía", "Spanish", "motivated", "o9zmyz5q99g", "2:37");

        // PEACEFUL
        t("Nuvole Bianche", "Ludovico Einaudi", "English", "peaceful", "qmxFAT581T4", "5:58");
        t("River Flows In You", "Yiruma", "English", "peaceful", "7maJOI3QMu0", "3:05");
        t("Come Away With Me", "Norah Jones", "English", "peaceful", "lbjZPFDY2IA", "3:18");
        t("Kun Faya Kun", "A.R. Rahman, Javed Ali, Mohit Chauhan", "Hindi", "peaceful", "T94PHkuydcw", "7:50");
        t("Arziyan", "A.R. Rahman, Javed Ali, Kailash Kher", "Hindi", "peaceful", "sK6zTqK1a0E", "8:41");
        t("Saibo", "Shreya Ghoshal, Tochi Raina", "Hindi", "peaceful", "jHNNMj5bNQw", "3:16");
        t("New York Nagaram", "A.R. Rahman", "Tamil", "peaceful", "7r0wY1q7n4U", "6:18");
        t("Vaseegara", "Bombay Jayashri, Harris Jayaraj", "Tamil", "peaceful", "3x6lC_q1bOQ", "5:00");
        t("Sparkle", "RADWIMPS", "Japanese", "peaceful", "a2GujJZfXpg", "6:48");
        t("Weightless", "Marconi Union", "English", "peaceful", "UfcAVejslrU", "8:08");

        // NOSTALGIC
        t("I Want It That Way", "Backstreet Boys", "English", "nostalgic", "4fndeDfaWCg", "3:39");
        t("Yellow", "Coldplay", "English", "nostalgic", "yKNxeF4KMsY", "4:29");
        t("Wonderwall", "Oasis", "English", "nostalgic", "6hzrDeceEKc", "4:18");
        t("Kal Ho Naa Ho", "Sonu Nigam, Shankar-Ehsaan-Loy", "Hindi", "nostalgic", "g0eO74UmRBs", "5:21");
        t("Pehla Nasha", "Udit Narayan, Jatin-Lalit", "Hindi", "nostalgic", "eZ_3YV0y0aE", "4:51");
        t("Chaiyya Chaiyya", "Sukhwinder Singh, Sapna Awasthi, A.R. Rahman", "Hindi", "nostalgic", "PQmrmVs10X8", "6:54");
        t("Roja Janeman (Pudhu Vellai)", "A.R. Rahman, S.P. Balasubrahmanyam", "Tamil", "nostalgic", "F5jI4Jv5M-Y", "5:17");
        t("Vennilave Vennilave", "A.R. Rahman, Hariharan", "Tamil", "nostalgic", "w8p8-vG0oY8", "5:58");
        t("Ennavale Adi Ennavale", "A.R. Rahman, Unni Krishnan", "Tamil", "nostalgic", "1U2o8mB7eP4", "5:12");
        t("Oruvan Oruvan Mudhalali", "A.R. Rahman, S.P. Balasubrahmanyam", "Tamil", "nostalgic", "G51kQ2V8pEw", "6:24");

        // ANGRY
        t("In The End", "Linkin Park", "English", "angry", "eVTXPUF4Oz4", "3:36");
        t("Numb", "Linkin Park", "English", "angry", "kXYiU_JCYtU", "3:07");
        t("Monster", "Skillet", "English", "angry", "1mjlM_RnsVE", "2:57");
        t("Sadda Haq", "Mohit Chauhan, A.R. Rahman", "Hindi", "angry", "p9DQINKFVWE", "6:05");
        t("Bhaag D.K. Bose", "Ram Sampath", "Hindi", "angry", "1b9GZgO15eY", "4:02");
        t("Jee Karda", "Divya Kumar, Sachin-Jigar", "Hindi", "angry", "2Gf2t9dJ97s", "3:58");
        t("Vikram Title Track", "Anirudh Ravichander", "Tamil", "angry", "tG_7T3f0VvU", "3:37");
        t("Hukum - Thalaivar Alappara", "Anirudh Ravichander", "Tamil", "angry", "1F3hm634R08", "3:27");
        t("Neruppu Da", "Santhosh Narayanan, Rajinikanth", "Tamil", "angry", "LHaGDT6PDKk", "3:38");
        t("Badass (Leo)", "Anirudh Ravichander", "Tamil", "angry", "1O0yazhqaxs", "3:49");
    }

    private static void registerMood(String key, String name, String emoji, String accent,
                                     String desc, String playlist, List<String> vibes) {
        MOODS.put(key, new MoodInfo(key, name, emoji, accent, desc, playlist, vibes));
        MOOD_ANALYTICS.put(key, new AtomicInteger(0));
    }

    private static void t(String title, String artist, String language, String mood, String yt, String duration) {
        String id = "t_" + (TRACKS.size() + 1);
        String sp = MOODS.containsKey(mood) ? MOODS.get(mood).spotifyPlaylist() : "37i9dQZF1DXdPec7aLTmlC";
        Track trk = new Track(id, title, artist, language, mood, yt, duration, sp);
        TRACKS.add(trk);
        LANG_ANALYTICS.computeIfAbsent(language, k -> new AtomicInteger(0));
        String primaryArtist = artist.split("[,&ft.]")[0].trim();
        ARTIST_ANALYTICS.computeIfAbsent(primaryArtist, k -> new AtomicInteger(0));
    }

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new UnifiedAppHandler());
        server.createContext("/recommend", new RecommendHandler());
        server.createContext("/detect", new DetectMoodHandler());
        server.createContext("/analytics", new AnalyticsHandler());
        server.createContext("/api/tracks", new ApiTracksHandler());
        server.createContext("/api/play", new ApiPlayHandler());
        server.createContext("/api/analytics", new ApiAnalyticsHandler());
        server.createContext("/api/search", new ApiMusicSearchHandler());

        server.setExecutor(Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors())));
        server.start();
        System.out.println("=================================================");
        System.out.println("  MoodTunes 2.0 Ultra is running at http://localhost:" + PORT);
        System.out.println("  Free API Music Streaming & Search Ready");
        System.out.println("  Press Ctrl+C to stop the server.");
        System.out.println("=================================================");
    }

    // ==================== HTTP HANDLERS ====================

    static class UnifiedAppHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if ("HEAD".equalsIgnoreCase(ex.getRequestMethod())) {
                respondHeadersOnly(ex, 200, "text/html; charset=UTF-8");
                return;
            }
            Map<String, String> params = queryParams(ex.getRequestURI().getRawQuery());
            String initialMood = params.getOrDefault("mood", "chill").toLowerCase();
            String initialLang = params.getOrDefault("lang", "all").toLowerCase();
            String initialArtist = params.getOrDefault("artist", "").toLowerCase();
            String initialTrack = params.getOrDefault("track", "");
            respond(ex, renderAppHtml(initialMood, initialLang, initialArtist, initialTrack));
        }
    }

    static class RecommendHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            Map<String, String> params = queryParams(ex.getRequestURI().getRawQuery());
            String mood = params.getOrDefault("mood", "happy").toLowerCase();
            String lang = params.getOrDefault("lang", "all").toLowerCase();
            String artist = params.getOrDefault("artist", "");
            recordPlay(mood, lang, artist);
            ex.getResponseHeaders().set("Location", "/?mood=" + mood + "&lang=" + lang + (artist.isEmpty() ? "" : "&artist=" + artist));
            ex.sendResponseHeaders(302, -1);
            ex.close();
        }
    }

    static class DetectMoodHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            Map<String, String> params = queryParams(ex.getRequestURI().getRawQuery());
            String text = params.getOrDefault("text", "").toLowerCase();
            String detectedMood = detectMoodFromText(text);
            String detectedLang = detectLanguageFromText(text);
            String detectedArtist = detectArtistFromText(text);

            recordPlay(detectedMood, detectedLang, detectedArtist);
            String redirectUrl = "/?mood=" + detectedMood + "&lang=" + detectedLang +
                (detectedArtist.isEmpty() ? "" : "&artist=" + urlEncode(detectedArtist));
            ex.getResponseHeaders().set("Location", redirectUrl);
            ex.sendResponseHeaders(302, -1);
            ex.close();
        }
    }

    static class AnalyticsHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            if ("HEAD".equalsIgnoreCase(ex.getRequestMethod())) {
                respondHeadersOnly(ex, 200, "text/html; charset=UTF-8");
                return;
            }
            respond(ex, renderAppHtml("chill", "all", "", "#analytics"));
        }
    }

    static class ApiTracksHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            Map<String, String> params = queryParams(ex.getRequestURI().getRawQuery());
            String mood = params.getOrDefault("mood", "").toLowerCase();
            String lang = params.getOrDefault("lang", "").toLowerCase();
            String artist = params.getOrDefault("artist", "").toLowerCase();
            String q = params.getOrDefault("q", "").toLowerCase();

            List<Track> filtered = TRACKS.stream().filter(t -> {
                if (!mood.isEmpty() && !mood.equals("all") && !t.mood().equalsIgnoreCase(mood)) return false;
                if (!lang.isEmpty() && !lang.equals("all") && !t.language().equalsIgnoreCase(lang)) return false;
                if (!artist.isEmpty() && !t.artist().toLowerCase().contains(artist)) return false;
                if (!q.isEmpty()) {
                    String combo = (t.title() + " " + t.artist() + " " + t.language() + " " + t.mood()).toLowerCase();
                    return combo.contains(q);
                }
                return true;
            }).collect(Collectors.toList());

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < filtered.size(); i++) {
                if (i > 0) json.append(",");
                json.append(trackToJson(filtered.get(i)));
            }
            json.append("]");
            respondJson(ex, json.toString());
        }
    }

    static class ApiPlayHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            Map<String, String> params = queryParams(ex.getRequestURI().getRawQuery());
            String mood = params.getOrDefault("mood", "chill");
            String lang = params.getOrDefault("lang", "english");
            String artist = params.getOrDefault("artist", "");
            recordPlay(mood, lang, artist);
            respondJson(ex, "{\"status\":\"ok\",\"totalPlays\":" + TOTAL_PLAYS.get() + "}");
        }
    }

    static class ApiAnalyticsHandler implements HttpHandler {
        public void handle(HttpExchange ex) throws IOException {
            StringBuilder json = new StringBuilder("{");
            json.append("\"totalPlays\":").append(TOTAL_PLAYS.get()).append(",");
            json.append("\"moods\":{");
            int mIdx = 0;
            for (var entry : MOOD_ANALYTICS.entrySet()) {
                if (mIdx++ > 0) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":").append(entry.getValue().get());
            }
            json.append("},\"languages\":{");
            int lIdx = 0;
            for (var entry : LANG_ANALYTICS.entrySet()) {
                if (lIdx++ > 0) json.append(",");
                json.append("\"").append(entry.getKey()).append("\":").append(entry.getValue().get());
            }
            json.append("},\"artists\":{");
            int aIdx = 0;
            for (var entry : ARTIST_ANALYTICS.entrySet()) {
                if (aIdx++ > 0) json.append(",");
                json.append("\"").append(entry.getKey().replace("\"", "\\\"")).append("\":").append(entry.getValue().get());
            }
            json.append("}}");
            respondJson(ex, json.toString());
        }
    }

    static class ApiMusicSearchHandler implements HttpHandler {
        private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

        public void handle(HttpExchange ex) throws IOException {
            Map<String, String> params = queryParams(ex.getRequestURI().getRawQuery());
            String query = params.getOrDefault("q", params.getOrDefault("term", "")).trim();
            if (query.isEmpty()) {
                respondJson(ex, "{\"resultCount\":0,\"results\":[]}");
                return;
            }
            try {
                String itunesUrl = "https://itunes.apple.com/search?term=" + urlEncode(query) + "&media=music&limit=15";
                HttpRequest req = HttpRequest.newBuilder(URI.create(itunesUrl))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) MoodTunes/2.0")
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();
                HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                respondJson(ex, resp.body());
            } catch (Exception e) {
                respondJson(ex, "{\"resultCount\":0,\"results\":[],\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
            }
        }
    }

    private static void recordPlay(String mood, String lang, String artist) {
        TOTAL_PLAYS.incrementAndGet();
        if (mood != null && MOOD_ANALYTICS.containsKey(mood.toLowerCase())) {
            MOOD_ANALYTICS.get(mood.toLowerCase()).incrementAndGet();
        }
        if (lang != null && !lang.equalsIgnoreCase("all")) {
            LANG_ANALYTICS.computeIfAbsent(capitalize(lang), k -> new AtomicInteger(0)).incrementAndGet();
        }
        if (artist != null && !artist.isBlank()) {
            String primary = artist.split("[,&ft.]")[0].trim();
            ARTIST_ANALYTICS.computeIfAbsent(primary, k -> new AtomicInteger(0)).incrementAndGet();
        }
    }

    // ==================== NATURAL LANGUAGE SCANNER ====================

    private static String detectMoodFromText(String text) {
        if (containsAny(text, "furious", "rage", "angry", "mad", "annoyed", "frustrated", "screaming", "fight", "heavy rock", "metal")) return "angry";
        if (containsAny(text, "heartbroken", "lonely", "depressed", "sad", "down", "upset", "grief", "crying", "tears", "pain", "alone")) return "sad";
        if (containsAny(text, "romantic", "love", "crush", "heart", "date", "wedding", "darling", "sweetheart", "kiss", "butterflies")) return "romantic";
        if (containsAny(text, "nostalg", "memory", "remember", "childhood", "old days", "90s", "2000s", "classic", "retro", "past", "evergreen")) return "nostalgic";
        if (containsAny(text, "party", "dance", "celebrat", "club", "weekend", "dj", "bass", "turnup", "banger")) return "party";
        if (containsAny(text, "motiv", "inspire", "determined", "focus", "productive", "goal", "study", "hustle", "grind", "work", "gym workout")) return "motivated";
        if (containsAny(text, "energetic", "energy", "pump", "hyped", "active", "workout", "run", "running", "fast", "speed", "boost")) return "energetic";
        if (containsAny(text, "peace", "zen", "meditat", "tranquil", "quiet", "gentle", "sleep", "soothing", "morning", "spiritual")) return "peaceful";
        if (containsAny(text, "relax", "calm", "chill", "unwind", "tired", "stress", "lofi", "cozy", "evening", "slow down")) return "chill";
        if (containsAny(text, "happy", "great", "fun", "excited", "joy", "good", "awesome", "cheerful", "smile", "sunshine", "vibrant")) return "happy";
        return "chill";
    }

    private static String detectLanguageFromText(String text) {
        if (containsAny(text, "tamil", "kollywood", "anirudh", "dhanush", "thalapathy", "vijay", "maruvaarthai", "rowdy baby")) return "tamil";
        if (containsAny(text, "hindi", "bollywood", "arijit", "shreya", "mohit", "pritam", "kesariya", "channa mereya")) return "hindi";
        if (containsAny(text, "telugu", "tollywood", "thaman", "butta bomma", "samajavaragamana", "naatu naatu", "mahesh babu")) return "telugu";
        if (containsAny(text, "punjabi", "diljit", "bhangra", "ap dhillon", "sidhu", "moose wala", "karan aujla")) return "punjabi";
        if (containsAny(text, "kpop", "k-pop", "korean", "bts", "blackpink", "newjeans", "dynamite")) return "kpop";
        if (containsAny(text, "spanish", "latin", "bad bunny", "rosalia", "despacito", "reggaeton")) return "spanish";
        if (containsAny(text, "japanese", "anime", "radwimps", "j-pop", "fujii kaze", "lemon")) return "japanese";
        if (containsAny(text, "english", "hollywood", "ed sheeran", "the weeknd", "taylor swift", "billie eilish", "linkin park")) return "english";
        return "all";
    }

    private static String detectArtistFromText(String text) {
        String[] artists = {
            "Anirudh", "Arijit Singh", "A.R. Rahman", "Sid Sriram", "Taylor Swift",
            "The Weeknd", "Diljit Dosanjh", "BTS", "Bad Bunny", "Linkin Park",
            "Shreya Ghoshal", "AP Dhillon", "Ed Sheeran", "Bruno Mars", "Billie Eilish",
            "NewJeans", "Thaman S", "Sukhwinder", "Mohit Chauhan", "Dua Lipa", "Coldplay",
            "Eminem", "Post Malone", "Adele", "Lewis Capaldi"
        };
        for (String a : artists) {
            if (text.contains(a.toLowerCase())) return a;
        }
        return "";
    }

    // ==================== TEMPLATE & RENDERING ====================

    private static String loadTemplate() {
        try {
            Path p = Path.of("app.html");
            if (Files.exists(p)) {
                return Files.readString(p, StandardCharsets.UTF_8);
            }
            Path alt = Path.of("MoodTunes_Upgraded", "app.html");
            if (Files.exists(alt)) {
                return Files.readString(alt, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "<html><body><h1>MoodTunes Error: app.html template not found</h1></body></html>";
    }

    private static String renderAppHtml(String initialMood, String initialLang, String initialArtist, String initialTarget) {
        StringBuilder moodsJson = new StringBuilder("{");
        int mCount = 0;
        for (var entry : MOODS.entrySet()) {
            if (mCount++ > 0) moodsJson.append(",");
            MoodInfo mi = entry.getValue();
            moodsJson.append("\"").append(mi.key()).append("\":{")
                .append("\"name\":\"").append(mi.name()).append("\",")
                .append("\"emoji\":\"").append(mi.emoji()).append("\",")
                .append("\"accent\":\"").append(mi.accent()).append("\",")
                .append("\"desc\":\"").append(escapeJson(mi.description())).append("\",")
                .append("\"spotify\":\"").append(mi.spotifyPlaylist()).append("\",")
                .append("\"vibes\":[")
                .append(mi.vibes().stream().map(v -> "\"" + v + "\"").collect(Collectors.joining(",")))
                .append("]}");
        }
        moodsJson.append("}");

        StringBuilder tracksJson = new StringBuilder("[");
        for (int i = 0; i < TRACKS.size(); i++) {
            if (i > 0) tracksJson.append(",");
            tracksJson.append(trackToJson(TRACKS.get(i)));
        }
        tracksJson.append("]");

        MoodInfo defaultMood = MOODS.getOrDefault(initialMood, MOODS.get("chill"));
        String firstYt = !TRACKS.isEmpty() ? TRACKS.get(0).youtubeId() : "ZbZSe6N_BXs";
        String firstSpotify = defaultMood != null ? defaultMood.spotifyPlaylist() : "37i9dQZF1DXdPec7aLTmlC";

        String appHtml = loadTemplate();

        return appHtml
            .replace("__ACCENT__", defaultMood != null ? defaultMood.accent() : "#79a8ff")
            .replace("__NAME__", defaultMood != null ? defaultMood.name() : "Chill")
            .replace("__EMOJI__", defaultMood != null ? defaultMood.emoji() : "🌊")
            .replace("__DESC__", defaultMood != null ? defaultMood.description() : "Find your sound")
            .replace("__FIRST_YT__", firstYt)
            .replace("__FIRST_SPOTIFY__", firstSpotify)
            .replace("__MOODS_JSON__", moodsJson.toString())
            .replace("__TRACKS_JSON__", tracksJson.toString())
            .replace("__INITIAL_MOOD__", initialMood)
            .replace("__INITIAL_LANG__", initialLang)
            .replace("__INITIAL_ARTIST__", escapeJson(initialArtist));
    }

    // ==================== HTTP UTILITIES ====================

    private static String trackToJson(Track t) {
        return "{" +
            "\"id\":\"" + t.id() + "\"," +
            "\"title\":\"" + escapeJson(t.title()) + "\"," +
            "\"artist\":\"" + escapeJson(t.artist()) + "\"," +
            "\"language\":\"" + escapeJson(t.language()) + "\"," +
            "\"mood\":\"" + escapeJson(t.mood()) + "\"," +
            "\"youtubeId\":\"" + t.youtubeId() + "\"," +
            "\"duration\":\"" + t.duration() + "\"," +
            "\"spotify\":\"" + t.spotifyPlaylist() + "\"" +
            "}";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static Map<String, String> queryParams(String query) {
        Map<String, String> params = new LinkedHashMap<>();
        if (query == null || query.isBlank()) return params;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length > 1
                ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }
        return params;
    }

    private static void respond(HttpExchange exchange, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Referrer-Policy", "strict-origin-when-cross-origin");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static void respondHeadersOnly(HttpExchange exchange, int statusCode, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, -1);
        exchange.close();
    }

    private static void respondJson(HttpExchange exchange, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) return true;
        }
        return false;
    }

    private static String capitalize(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
