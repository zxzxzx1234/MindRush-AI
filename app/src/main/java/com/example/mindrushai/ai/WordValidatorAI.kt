package com.example.mindrushai.ai

import com.example.mindrushai.ai.llm.LLMClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * AI Agent 2 — WordValidatorAI
 *
 * Determines whether a word typed by the player exists in English.
 *
 * Three-tier lookup (fastest → slowest):
 *   Tier 1 — Instant: single letters, session cache, offline dictionary
 *   Tier 2 — Instant: knownInvalid cache (prevents repeated LLM calls for gibberish)
 *   Tier 3 — LLM:     only for genuinely unknown words; result cached in session
 *
 * On timeout → assumed valid (never block the player unfairly).
 * Timeout: 10s (generous for slow local models).
 */
class WordValidatorAI(
    private val llmClient: LLMClient? = null
) {

    companion object {
        private const val LLM_TIMEOUT_MS = 10_000L
    }

    data class ValidationResult(
        val isValid: Boolean,
        val reason: String
    ) {
        companion object {
            val SINGLE_LETTER = ValidationResult(true,  "Single-letter accepted.")
            val EMPTY         = ValidationResult(false, "Please type a word.")
            val ASSUMED_VALID = ValidationResult(true,  "Could not verify — assumed valid.")
            val OFFLINE_MISS  = ValidationResult(false, "Not a recognised English word.")
        }
    }

    private val knownValid   = mutableSetOf<String>()
    private val knownInvalid = mutableSetOf<String>()

    // Offline dictionary — covers all SequenceGeneratorAI fallback pools
    private val offlineDictionary: Set<String> = buildSet {
        addAll(listOf(
            // 3-letter pool
            "cat","dog","run","joy","sun","map","key","cup","fly","pen",
            "arm","sky","red","ant","bus","gem","hop","ice","jar","log",
            "mud","net","owl","pod","ski","web","bay","elm","fur","gut",
            // 4-letter pool
            "book","lamp","tree","boat","cake","door","fire","gold","hand","iron",
            "jump","kind","lake","moon","nose","open","park","rain","salt","time",
            "wave","year","zero","blue","calm","dark","edge","farm","gate","hill",
            // 5-letter pool
            "apple","cloud","brave","stone","flame","crisp","brand","chess","drift","eagle",
            "flair","grace","hover","ivory","joker","kneel","lemon","maple","noble","ozone",
            "pixel","quiet","risen","scout","thorn","unity","vapor","whirl","xenon","yacht",
            // 6-letter pool
            "bridge","planet","silver","rocket","jungle","candle","castle","desert","engine",
            "glider","harbor","island","magnet","needle","quartz","riddle","temple","velvet",
            "anchor","cactus","goblet","hammer","mirror","pocket","sunset","forest","market",
            "rubber","tunnel","window",
            // 7-letter pool
            "blanket","journey","crystal","lantern","thunder","mystery","cabinet","climate",
            "dolphin","eclipse","fantasy","granite","harmony","iceberg","justice","kingdom",
            "leopard","monitor","nervous","organic","paradox","quantum","railway","silence",
            "torpedo","upgrade","villain","whisper","extreme","fragile",
            // 8+ pool
            "labyrinth","cognition","resonance","melancholy","symposium","eloquence",
            "paradigm","serendipity","tenacious","juxtapose","soliloquy","ephemeral",
            "clandestine","oscillate","ubiquitous","ineffable","sycophant","recondite",
            "palimpsest","perfidious","loquacious","equivocate","byzantine","inscrutable",
            "sanguine","truculent","mellifluous","obfuscate","querulous","perihelion",
            // common short words
            "a","i","the","and","for","are","but","not","you","all","can","her","was",
            "one","our","out","day","get","has","him","his","how","its","may","new",
            "now","old","see","two","who","boy","did","man","end","far","few","got",
            "let","put","say","she","too","use","via","yet","ago","air","bad","big",
            "bit","cry","cut","dry","ear","eat","egg","era","eye","fee","fit","fix",
            "hit","hug","hut","ill","jab","lab","lap","lid","lip","lit","lot","low",
            "mix","mob","mop","nap","nod","oak","odd","oil","opt","orb","pad","pan",
            "paw","pay","pie","pig","pin","pit","pop","pot","pun","pup","raw","rib",
            "rip","rob","rod","rot","row","rub","rug","rum","rut","sad","sap","sat",
            "saw","set","sew","sin","sip","sit","six","sob","son","spa","sub","sum",
            "tab","tan","tap","tar","tax","tea","ten","tie","tin","tip","toe","ton",
            "top","toy","tug","urn","van","vat","vow","wax","wed","wet","wig","win",
            "wit","woe","won","yam","yew","zip","zoo","able","also","area","away",
            "back","ball","band","bank","base","bath","bean","bear","beat","beer",
            "bell","belt","bird","bite","blue","bolt","bond","bone","born","both",
            "bowl","burn","cage","call","calm","card","care","case","cash","cave",
            "cell","chat","chip","city","coat","code","coin","cold","come","cook",
            "cool","copy","cord","corn","cost","crop","curl","cute","dark","data",
            "date","dawn","dead","deal","dear","deep","deer","dent","dirt","disk",
            "dive","down","draw","drop","drum","duck","dull","dusk","dust","duty",
            "each","earn","east","edge","even","ever","evil","face","fact","fail",
            "fair","fake","fall","fame","farm","fast","fate","feed","feel","feet",
            "fell","felt","file","fill","film","find","fine","firm","fish","flag",
            "flat","flip","flow","foam","fold","folk","fond","food","fool","foot",
            "fork","form","fort","four","free","frog","fuel","full","fund","fuse",
            "gaze","gift","girl","give","glad","glow","glue","goal","golf","good",
            "gray","grew","grid","grin","grip","grow","gulf","gust","hack","hair",
            "half","hall","halt","hang","hard","hare","harm","harp","hate","haul",
            "hawk","head","heal","heap","heat","heel","held","help","herb","here",
            "hero","hide","high","hint","hire","hold","hole","home","hook","hope",
            "horn","hose","host","hour","huge","hull","hunt","hurt","idea","idle",
            "join","joke","just","keep","kick","kill","kind","king","kiss","knew",
            "know","lack","lane","last","late","lawn","lead","leaf","lean","leap",
            "left","lend","lens","less","lick","life","lift","like","lime","line",
            "link","lion","list","live","load","loan","lock","loft","lone","long",
            "look","loop","lord","lose","loss","lost","loud","love","luck","lung",
            "lure","lush","made","mail","main","make","male","mall","mane","many",
            "mash","mass","mast","mate","math","meal","mean","meet","melt","memo",
            "mere","mesh","mild","mile","milk","mill","mind","mine","mint","miss",
            "mode","mole","monk","more","most","move","much","mule","muse","must",
            "myth","name","navy","near","neck","need","next","nice","nick","nine",
            "node","none","noon","norm","note","null","numb","once","only","oral",
            "over","oven","pace","pack","page","pain","pale","palm","part","pass",
            "past","path","peak","pear","peel","perk","pest","pick","pile","pine",
            "pink","pipe","plan","play","plum","plus","pond","pool","poor","pore",
            "port","pose","post","pour","pray","prey","prod","prop","pull","pump",
            "pure","push","race","rack","rage","rake","ramp","rang","rank","rare",
            "rate","read","real","reap","reed","reel","rely","rent","rest","rice",
            "rich","ride","ring","riot","rise","risk","road","roam","roar","robe",
            "rock","role","roof","room","root","rope","rose","ruin","rule","rush",
            "rust","safe","sage","sake","sale","same","sand","sane","sang","sank",
            "save","scan","seal","seam","seek","seem","seep","self","sell","send",
            "sent","shed","shin","ship","shoe","shop","shot","show","shut","sick",
            "side","sigh","silk","sing","sink","size","skin","skip","slam","slap",
            "slim","slip","slow","slug","snap","snip","snow","soap","soft","soil",
            "sold","sole","some","song","soot","soul","soup","span","spin","spit",
            "spot","star","stay","stem","step","stew","stir","stop","stun","such",
            "suit","sung","sunk","sure","swap","swim","tail","tale","tall","tame",
            "task","team","tear","tell","tend","tent","term","test","text","than",
            "that","them","then","they","thin","this","thus","tide","till","tiny",
            "toad","told","toll","took","tool","torn","toss","tour","town","trap",
            "trim","trip","true","tuck","tune","turf","turn","twin","type","ugly",
            "unit","upon","urge","used","user","vain","vary","vast","veil","verb",
            "very","view","vine","void","volt","vote","wade","wage","wake","walk",
            "wall","want","ward","warm","warn","wash","weak","wear","weed","week",
            "well","went","were","west","what","when","wide","wife","wild","will",
            "wind","wine","wing","wink","wire","wise","wish","with","wolf","wood",
            "word","wore","work","worm","worn","wrap","yard","year","yell","your",
            "zero","zone"
        ))
    }

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun validate(word: String): ValidationResult {
        val w = word.trim().lowercase()

        if (w.isEmpty()) return ValidationResult.EMPTY
        if (w.length == 1) {
            AILogger.log("WORD_SINGLE_LETTER", w, "accepted")
            return ValidationResult.SINGLE_LETTER
        }

        if (w in knownValid)   { AILogger.log("WORD_CACHE_VALID",   w, "true");  return ValidationResult(true,  "Previously confirmed.") }
        if (w in knownInvalid) { AILogger.log("WORD_CACHE_INVALID", w, "false"); return ValidationResult(false, "Previously rejected.") }

        if (w in offlineDictionary) {
            knownValid.add(w)
            AILogger.log("WORD_OFFLINE_DICT", w, "true")
            return ValidationResult(true, "Found in offline dictionary.")
        }

        return if (llmClient != null) tryLLMValidate(w) else {
            knownInvalid.add(w)
            AILogger.log("WORD_NO_CLIENT", w, "false")
            ValidationResult.OFFLINE_MISS
        }
    }

    fun clearCache() {
        knownValid.clear()
        knownInvalid.clear()
    }

    // ── LLM path ──────────────────────────────────────────────────────────────

    private suspend fun tryLLMValidate(word: String): ValidationResult {
        return try {
            val raw = withContext(Dispatchers.IO) {
                withTimeoutOrNull(LLM_TIMEOUT_MS) {
                    llmClient!!.generate(buildPrompt(word))
                }
            }

            AILogger.log("WORD_VALIDATE_RESPONSE", word, raw ?: "NULL/TIMEOUT")

            if (raw.isNullOrBlank()) {
                knownValid.add(word)
                return ValidationResult.ASSUMED_VALID
            }

            parseResponse(word, raw)

        } catch (e: Exception) {
            knownValid.add(word)
            AILogger.log("WORD_VALIDATE_EXCEPTION", "word=$word err=${e.message}", "assumed valid")
            ValidationResult.ASSUMED_VALID
        }
    }

    private fun buildPrompt(word: String): String =
        "Is \"$word\" a real English word? Reply with only YES or NO."

    private fun parseResponse(word: String, raw: String): ValidationResult {
        // Accept anything that starts with Y or contains YES (case-insensitive)
        // Reject if it clearly starts with N or contains NO
        val upper = raw.trim().uppercase()
        val isValid = when {
            upper.startsWith("YES") -> true
            upper.startsWith("Y")   -> true
            upper.startsWith("NO")  -> false
            upper.startsWith("N")   -> false
            upper.contains("YES")   -> true
            upper.contains("NO")    -> false
            else                    -> true  // unclear → assume valid
        }

        if (isValid) knownValid.add(word) else knownInvalid.add(word)

        AILogger.log(
            if (isValid) "WORD_VALID_LLM" else "WORD_INVALID_LLM",
            word,
            raw.take(40)
        )

        return ValidationResult(
            isValid,
            if (isValid) "Valid English word." else "Not a recognised English word."
        )
    }
}