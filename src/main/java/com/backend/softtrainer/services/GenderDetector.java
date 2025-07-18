package com.backend.softtrainer.services;

import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import java.util.Set;

/**
 * Service to detect gender from character names
 */
@Service
@Slf4j
public class GenderDetector {

    // Common male names
    private static final Set<String> MALE_NAMES = Set.of(
        "john", "mike", "david", "chris", "james", "robert", "michael",
        "william", "richard", "joseph", "thomas", "charles", "daniel",
        "matthew", "anthony", "mark", "donald", "steven", "paul", "andrew",
        "joshua", "kenneth", "kevin", "brian", "george", "timothy", "ronald",
        "jason", "edward", "jeffrey", "ryan", "jacob", "gary", "nicholas",
        "eric", "jonathan", "stephen", "larry", "justin", "scott", "brandon",
        "benjamin", "samuel", "frank", "raymond", "alexander", "patrick",
        "jack", "dennis", "jerry", "tyler", "aaron", "jose", "henry",
        "adam", "douglas", "nathan", "peter", "zachary", "kyle", "walter",
        "alex", "derek", "sean", "noah", "luke", "mason", "ethan", "logan",
        "owen", "eli", "caleb", "connor", "liam", "oliver", "tom", "tony",
        "steve", "dave", "bill", "bob", "dan", "jim", "max", "sam", "ben"
    );

    // Common female names
    private static final Set<String> FEMALE_NAMES = Set.of(
        "jane", "sarah", "lisa", "maria", "jennifer", "linda", "patricia",
        "barbara", "elizabeth", "jessica", "susan", "karen", "nancy", "betty",
        "helen", "sandra", "donna", "ruth", "sharon", "michelle",
        "laura", "kimberly", "deborah", "dorothy", "amy", "angela", "ashley",
        "brenda", "emma", "olivia", "sophia", "emily", "madison", "chloe",
        "abigail", "mia", "charlotte", "harper", "evelyn", "ella", "anna",
        "grace", "victoria", "aria", "riley", "hannah", "natalie", "leah",
        "zoe", "nora", "hazel", "ellie", "lily", "nova", "elena", "iris",
        "alice", "rachel", "julie", "mary", "diane", "carol",
        "debra", "kate", "meg", "annie", "tina", "beth"
    );

    // Gender-indicating words/titles
    private static final Set<String> MALE_INDICATORS = Set.of(
        "mr", "mister", "sir", "gentleman", "guy", "man", "male", "he", "his", "him",
        "father", "dad", "son", "brother", "uncle", "grandfather", "husband", "boyfriend"
    );

    private static final Set<String> FEMALE_INDICATORS = Set.of(
        "mrs", "ms", "miss", "madam", "lady", "woman", "female", "she", "her", "hers",
        "mother", "mom", "daughter", "sister", "aunt", "grandmother", "wife", "girlfriend"
    );

    /**
     * Detect gender from character name
     */
    public CharacterTemplate.Gender detectGender(String characterName) {
        if (characterName == null || characterName.trim().isEmpty()) {
            return CharacterTemplate.Gender.MALE; // Default
        }

        String lowerName = characterName.toLowerCase().trim();
        log.debug("🔍 Detecting gender for character: '{}'", characterName);

        // Check for explicit gender indicators first
        if (containsAnyWord(lowerName, MALE_INDICATORS)) {
            log.debug("✅ Detected MALE (indicator): {}", characterName);
            return CharacterTemplate.Gender.MALE;
        }

        if (containsAnyWord(lowerName, FEMALE_INDICATORS)) {
            log.debug("✅ Detected FEMALE (indicator): {}", characterName);
            return CharacterTemplate.Gender.FEMALE;
        }

        // Extract potential first names from the character name
        String[] words = lowerName.split("\\s+");

        // Check each word against name databases
        for (String word : words) {
            word = word.replaceAll("[^a-z]", ""); // Remove non-letters

            if (MALE_NAMES.contains(word)) {
                log.debug("✅ Detected MALE (name match: {}): {}", word, characterName);
                return CharacterTemplate.Gender.MALE;
            }

            if (FEMALE_NAMES.contains(word)) {
                log.debug("✅ Detected FEMALE (name match: {}): {}", word, characterName);
                return CharacterTemplate.Gender.FEMALE;
            }
        }

        // Heuristic: names ending in 'a' are often female
        String lastWord = words[words.length - 1].replaceAll("[^a-z]", "");
        if (lastWord.endsWith("a") && lastWord.length() > 2) {
            // But exclude common male names ending in 'a'
            Set<String> maleExceptions = Set.of("joshua", "luca", "andrea", "ira");
            if (!maleExceptions.contains(lastWord)) {
                log.debug("✅ Detected FEMALE (ends with 'a'): {}", characterName);
                return CharacterTemplate.Gender.FEMALE;
            }
        }

        // Default to male if uncertain
        log.debug("✅ Defaulting to MALE: {}", characterName);
        return CharacterTemplate.Gender.MALE;
    }

    /**
     * Check if text contains any of the specified words
     */
    private boolean containsAnyWord(String text, Set<String> words) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        return words.stream().anyMatch(word ->
            text.contains(" " + word + " ") ||
            text.startsWith(word + " ") ||
            text.endsWith(" " + word) ||
            text.equals(word)
        );
    }
}
