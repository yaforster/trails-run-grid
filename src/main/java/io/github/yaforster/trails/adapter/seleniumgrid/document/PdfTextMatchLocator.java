package io.github.yaforster.trails.adapter.seleniumgrid.document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

/**
 * Retains PDFBox character positions during text extraction so a visual proof can scroll to the first matching text.
 * Plain extracted text loses that geometry; whitespace-insensitive matching here keeps each searched character linked
 * to its position on the rendered page.
 */
final class PdfTextMatchLocator extends PDFTextStripper {

    private final String expectedText;
    private final boolean caseSensitive;
    private final Map<Integer, List<TextPosition>> positionsByPage = new LinkedHashMap<>();

    PdfTextMatchLocator(String expectedText, boolean caseSensitive) throws IOException {
        this.expectedText = expectedText;
        this.caseSensitive = caseSensitive;
        setSortByPosition(true);
    }

    Optional<TextMatch> findFirstMatch(PDDocument document) throws IOException {
        // Text extraction invokes PDFBox's writeString callback, which supplies the positions
        // needed to scroll the later image to the matching words.
        getText(document);
        if (expectedText == null || expectedText.isBlank()) {
            return Optional.empty();
        }
        String searchText = normalizeSearchText();
        for (Map.Entry<Integer, List<TextPosition>> page : positionsByPage.entrySet()) {
            int pageIndex = page.getKey();
            List<TextPosition> positions = page.getValue();
            Optional<TextMatch> match = findOnPage(pageIndex, positions, searchText);
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    @Override
    protected void writeString(String text, List<TextPosition> positions) {
        // PDFBox numbers pages from one; its image renderer indexes them from zero.
        int pageIndex = getCurrentPageNo() - 1;
        List<TextPosition> pagePositions = positionsByPage.computeIfAbsent(pageIndex, _ -> new ArrayList<>());
        pagePositions.addAll(positions);
    }

    private Optional<TextMatch> findOnPage(int pageIndex, List<TextPosition> positions, String searchText) {
        List<PositionedCharacter> characters = indexSearchableCharacters(positions);
        String pageText = searchablePageText(characters);
        int matchStart = pageText.indexOf(searchText);
        if (matchStart < 0) {
            return Optional.empty();
        }
        int matchEnd = matchStart + searchText.length();
        List<PositionedCharacter> matchingCharacters = characters.subList(matchStart, matchEnd);
        float topEdge = topEdgeOfMatch(matchingCharacters);
        TextMatch match = new TextMatch(pageIndex, topEdge);
        return Optional.of(match);
    }

    private List<PositionedCharacter> indexSearchableCharacters(List<TextPosition> positions) {
        List<PositionedCharacter> characters = new ArrayList<>();
        for (TextPosition position : positions) {
            for (char character : position.getUnicode()
                                          .toCharArray()) {
                if (!Character.isWhitespace(character)) {
                    char normalizedCharacter = normalizeCase(character);
                    PositionedCharacter positionedCharacter = new PositionedCharacter(normalizedCharacter, position);
                    characters.add(positionedCharacter);
                }
            }
        }
        return characters;
    }

    private String searchablePageText(List<PositionedCharacter> characters) {
        int characterCount = characters.size();
        StringBuilder text = new StringBuilder(characterCount);
        for (PositionedCharacter character : characters) {
            char value = character.value();
            text.append(value);
        }
        return text.toString();
    }

    private String normalizeSearchText() {
        int textLength = expectedText.length();
        StringBuilder text = new StringBuilder(textLength);
        for (char character : expectedText.toCharArray()) {
            if (!Character.isWhitespace(character)) {
                char normalizedCharacter = normalizeCase(character);
                text.append(normalizedCharacter);
            }
        }
        return text.toString();
    }

    private float topEdgeOfMatch(List<PositionedCharacter> characters) {
        // PDFBox reports the text baseline. Subtracting the letter height gives its
        // upper edge, so the proof shows the whole word rather than cutting it off.
        float topEdge = Float.MAX_VALUE;
        for (PositionedCharacter character : characters) {
            TextPosition position = character.position();
            float textBaseline = position.getYDirAdj();
            float characterHeight = position.getHeightDir();
            float characterTopEdge = textBaseline - characterHeight;
            topEdge = Math.min(topEdge, characterTopEdge);
        }
        return topEdge;
    }

    private char normalizeCase(char character) {
        return caseSensitive ? character : Character.toLowerCase(character);
    }

}
