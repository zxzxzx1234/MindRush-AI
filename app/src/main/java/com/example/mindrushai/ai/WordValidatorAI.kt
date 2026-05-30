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
 * Why a language model and not a static dictionary?
 *   An LLM reasons contextually about edge cases — informal vocabulary,
 *   neologisms, technical terms, regional spellings — in ways a fixed list
 *   cannot. Its judgement is non-deterministic and contextual.
 *
 * Three-tier lookup (fastest → slowest):
 *
 *   Tier 1 — Instant accept  : single-letter words, session knownValid cache,
 *             offline dictionary (covers all SequenceGeneratorAI pools + ~600
 *             common words). Zero I/O.
 *
 *   Tier 2 — Instant reject  : session knownInvalid cache. Zero I/O.
 *             Prevents repeated LLM calls for the same gibberish input.
 *
 *   Tier 3 — LLM call        : only for genuinely unknown words. Result is
 *             cached in the session so the same word is never sent twice.
 *             On timeout → assumed valid (never block the player unfairly).
 *
 * Cache is cleared at the start of each new game via [clearCache].
 */
class WordValidatorAI(
    private val llmClient: LLMClient? = null
) {

    companion object {
        private const val LLM_TIMEOUT_MS = 4000L
    }

    data class ValidationResult(
        val isValid: Boolean,
        val reason: String
    ) {
        companion object {
            val SINGLE_LETTER  = ValidationResult(true,  "Single-letter word accepted.")
            val EMPTY          = ValidationResult(false, "Please type a word.")
            val ASSUMED_VALID  = ValidationResult(true,  "Could not verify — assumed valid.")
            val OFFLINE_MISS   = ValidationResult(false, "Not a recognised English word.")
        }
    }

    // ── Session caches ────────────────────────────────────────────────────────

    private val knownValid   = mutableSetOf<String>()
    private val knownInvalid = mutableSetOf<String>()

    // ── Offline dictionary ────────────────────────────────────────────────────
    // Covers all SequenceGeneratorAI pools + common short English words.

    private val offlineDictionary: Set<String> = buildSet {
        addAll(listOf( // easy pool
            "cat","run","joy","sun","map","key","cup","fly","pen","arm","sky","red",
            "ant","bus","gem","hop","ice","jar","log","mud","net","owl","pod","ski",
            "web","bay","elm","fur","gut","ivy","box","hat","dog","leg","fan","fog"
        ))
        addAll(listOf( // medium pool
            "blanket","journey","pillow","rocket","ladder","candle","silver","tunnel",
            "sunset","forest","planet","castle","desert","engine","glider","harbor",
            "island","jungle","magnet","needle","quartz","riddle","temple","velvet",
            "anchor","cactus","goblet","hammer","mirror","pocket","garden","bridge",
            "window","market","rubber"
        ))
        addAll(listOf( // hard pool
            "ephemeral","labyrinth","cognition","resonance","melancholy","symposium",
            "oscillate","ubiquitous","clandestine","eloquence","paradigm","serendipity",
            "tenacious","juxtapose","soliloquy","querulous","recondite","palimpsest",
            "ineffable","sycophant","perfidious","obfuscate","perihelion","loquacious",
            "mellifluous","equivocate","byzantine","inscrutable","sanguine","truculent"
        ))
        addAll(listOf( // common short words
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
            "bell","belt","bird","bite","blue","boat","bold","bond","book","boot",
            "born","both","bowl","burn","cage","cake","call","calm","card","care",
            "case","cash","cave","cell","chat","chip","city","coat","code","coin",
            "cold","come","cook","cool","copy","cord","corn","cost","crop","curl",
            "cute","dark","data","date","dawn","dead","deal","dear","deep","deer",
            "dent","dirt","disk","dive","door","dove","down","draw","drop","drum",
            "duck","dull","dusk","dust","duty","each","earn","east","edge","even",
            "ever","evil","face","fact","fail","fair","fake","fall","fame","farm",
            "fast","fate","feed","feel","feet","fell","felt","file","fill","film",
            "find","fine","fire","firm","fish","flag","flat","flip","flow","foam",
            "fold","folk","fond","food","fool","foot","fork","form","fort","four",
            "free","frog","fuel","full","fund","fuse","gaze","gift","girl","give",
            "glad","glow","glue","goal","gold","golf","good","gray","grew","grid",
            "grin","grip","grow","gulf","gust","hack","hair","half","hall","halt",
            "hand","hang","hard","hare","harm","harp","hate","haul","hawk","head",
            "heal","heap","heat","heel","held","help","herb","here","hero","hide",
            "high","hill","hint","hire","hold","hole","home","hook","hope","horn",
            "hose","host","hour","huge","hull","hunt","hurt","idea","idle","join",
            "joke","jump","just","keep","kick","kill","kind","king","kiss","knee",
            "knew","know","lack","lake","lamp","land","lane","last","late","lawn",
            "lead","leaf","lean","leap","left","lend","lens","less","lick","life",
            "lift","like","lime","line","link","lion","list","live","load","loan",
            "lock","loft","lone","long","look","loop","lord","lose","loss","lost",
            "loud","love","luck","lung","lure","lush","made","mail","main","make",
            "male","mall","mane","many","mash","mass","mast","mate","math","meal",
            "mean","meet","melt","memo","mere","mesh","mild","mile","milk","mill",
            "mind","mine","mint","miss","mode","mole","monk","moon","more","most",
            "move","much","mule","muse","must","myth","name","navy","near","neck",
            "need","next","nice","nick","nine","node","none","noon","norm","nose",
            "note","null","numb","once","only","open","oral","over","oven","pace",
            "pack","page","pain","pair","pale","palm","park","part","pass","past",
            "path","peak","pear","peel","perk","pest","pick","pile","pine","pink",
            "pipe","plan","play","plum","plus","poll","pond","pool","poor","pore",
            "port","pose","post","pour","pray","prey","prod","prop","pull","pump",
            "pure","push","race","rack","rage","rain","rake","ramp","rang","rank",
            "rare","rate","read","real","reap","reed","reel","rely","rent","rest",
            "rice","rich","ride","ring","riot","rise","risk","road","roam","roar",
            "robe","rock","role","roof","room","root","rope","rose","ruin","rule",
            "rush","rust","safe","sage","sake","sale","salt","same","sand","sane",
            "sang","sank","save","scan","seal","seam","seek","seem","seep","self",
            "sell","send","sent","shed","shin","ship","shoe","shop","shot","show",
            "shut","sick","side","sigh","silk","sing","sink","size","skin","skip",
            "slam","slap","slim","slip","slow","slug","snap","snip","snow","soap",
            "soft","soil","sold","sole","some","song","soot","soul","soup","span",
            "spin","spit","spot","star","stay","stem","step","stew","stir","stop",
            "stun","such","suit","sung","sunk","sure","swap","swim","tail","tale",
            "tall","tame","task","team","tear","tell","tend","tent","term","test",
            "text","than","that","them","then","they","thin","this","thus","tide",
            "till","time","tiny","toad","told","toll","took","tool","torn","toss",
            "tour","town","trap","tree","trim","trip","true","tuck","tune","turf",
            "turn","twin","type","ugly","unit","upon","urge","used","user","vain",
            "vary","vast","veil","verb","very","view","vine","void","volt","vote",
            "wade","wage","wake","walk","wall","want","ward","warm","warn","wash",
            "wave","weak","wear","weed","week","well","went","were","west","what",
            "when","wide","wife","wild","will","wind","wine","wing","wink","wire",
            "wise","wish","with","wolf","wood","word","wore","work","worm","worn",
            "wrap","yard","year","yell","your","zero","zone"
        ))
    }

    // ── Public API ────────────────────────────────────────────────────────────

    suspend fun validate(word: String): ValidationResult {
        val cleaned = word.trim().lowercase()

        if (cleaned.isEmpty()) return ValidationResult.EMPTY
        if (cleaned.length == 1) {
            AILogger.log("WORD_SINGLE_LETTER", cleaned, "accepted")
            return ValidationResult.SINGLE_LETTER
        }

        // Tier 1: session valid cache
        if (cleaned in knownValid) {
            AILogger.log("WORD_CACHE_HIT_VALID", cleaned, "true")
            return ValidationResult(true, "Previously confirmed.")
        }

        // Tier 2: session invalid cache
        if (cleaned in knownInvalid) {
            AILogger.log("WORD_CACHE_HIT_INVALID", cleaned, "false")
            return ValidationResult(false, "Previously rejected.")
        }

        // Tier 1 (extended): offline dictionary
        if (cleaned in offlineDictionary) {
            knownValid.add(cleaned)
            AILogger.log("WORD_OFFLINE_DICT", cleaned, "true")
            return ValidationResult(true, "Found in offline dictionary.")
        }

        // Tier 3: LLM
        return if (llmClient != null) {
            tryLLMValidate(cleaned)
        } else {
            knownInvalid.add(cleaned)
            AILogger.log("WORD_NO_CLIENT", cleaned, "false")
            ValidationResult.OFFLINE_MISS
        }
    }

    /** Clears session caches. Call at the start of each new game. */
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

            if (raw.isNullOrBlank()) {
                // On timeout → assume valid to avoid blocking the player unfairly
                knownValid.add(word)
                AILogger.log("WORD_VALIDATE_TIMEOUT", word, "assumed valid")
                return ValidationResult.ASSUMED_VALID
            }

            parseResponse(word, raw)

        } catch (e: Exception) {
            knownValid.add(word)
            AILogger.log("WORD_VALIDATE_EXCEPTION", "word=$word err=${e.message}", "assumed valid")
            ValidationResult.ASSUMED_VALID
        }
    }

    // ── Prompt ────────────────────────────────────────────────────────────────

    private fun buildPrompt(word: String): String = """
Does "$word" exist in standard English dictionaries?

Consider: common words, informal vocabulary, technical terms, and recognised abbreviations.
Do NOT accept: random strings, gibberish, proper names, or non-English words.

Reply with ONLY one of these formats (no extra text):
YES: <reason, max 5 words>
NO: <reason, max 5 words>
    """.trimIndent()

    // ── Response parsing ──────────────────────────────────────────────────────

    private fun parseResponse(word: String, raw: String): ValidationResult {
        val isValid = raw.trim().uppercase().startsWith("YES")
        val reason  = raw.trim().substringAfter(":").trim()
            .ifBlank { if (isValid) "Valid English word." else "Not a recognised English word." }

        if (isValid) knownValid.add(word) else knownInvalid.add(word)

        AILogger.log(
            if (isValid) "WORD_VALID_LLM" else "WORD_INVALID_LLM",
            word, reason
        )

        return ValidationResult(isValid, reason)
    }

    // ── Local fallback (used when llmClient == null) ──────────────────────────

    private fun localValidate(word: String): ValidationResult {
        val isValid = word in offlineDictionary
        AILogger.log(
            if (isValid) "WORD_VALID_LOCAL" else "WORD_INVALID_LOCAL",
            word, isValid.toString()
        )
        return if (isValid) ValidationResult(true, "Found in local dictionary.")
        else ValidationResult.OFFLINE_MISS
    }
}