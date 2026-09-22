package net.runelite.client.plugins.projectx.util.player;

import java.util.Random;

public class NameGenerator {
    private static final int DEFAULT_LENGTH = 7;
    private static final int ASCII_LOWERCASE_A = 'a';
    private static final int LETTER_RANGE = 26;

    private final Random random;
    private final int length;

    public NameGenerator(int lengthOfName) {
        if (lengthOfName < 5 || lengthOfName > 10) {
            System.out.println("Setting default length to " + DEFAULT_LENGTH);
            lengthOfName = DEFAULT_LENGTH;
        }
        this.length = lengthOfName;
        this.random = new Random();
    }

    public String getName() {
        return generateRandomName();
    }

    private String generateRandomName() {
        StringBuilder name = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            char nextChar = (i % 2 == 0) ? getRandomConsonant() : getRandomVowel();
            name.append(nextChar);
        }

        // Capitalize the first letter
        name.setCharAt(0, Character.toUpperCase(name.charAt(0)));
        return name.toString();
    }

    private char getRandomConsonant() {
        char c;
        do {
            c = (char) (random.nextInt(LETTER_RANGE) + ASCII_LOWERCASE_A);
        } while (isVowel(c));
        return c;
    }

    private char getRandomVowel() {
        char[] vowels = {'a', 'e', 'i', 'o', 'u'};
        return vowels[random.nextInt(vowels.length)];
    }

    private boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }
}