package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.formatter.IdentityFormatter;
import org.jabref.logic.formatter.bibtexfields.EscapeAmpersandsFormatter;
import org.jabref.logic.formatter.bibtexfields.EscapeDollarSignFormatter;
import org.jabref.logic.formatter.bibtexfields.EscapeUnderscoresFormatter;
import org.jabref.logic.formatter.bibtexfields.LatexCleanupFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeDateFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.formatter.casechanger.LowerCaseFormatter;
import org.jabref.logic.layout.format.ReplaceUnicodeLigaturesFormatter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FieldFormatterCleanupsTest {

    private BibEntry entry;

    @BeforeEach
    void setUp() {
        entry = new BibEntry(StandardEntryType.InProceedings)
                .withCitationKey("6055279")
                .withField(StandardField.TITLE, "Educational session 1")
                .withField(StandardField.BOOKTITLE, "Custom Integrated Circuits Conference (CICC), 2011 IEEE")
                .withField(StandardField.YEAR, "2011")
                .withField(StandardField.MONTH, "Sept.")
                .withField(StandardField.PAGES, "1-7")
                .withField(StandardField.ABSTRACT, "Start of the above-titled section of the conference proceedings record.")
                .withField(StandardField.DOI, "10.1109/CICC.2011.6055279")
                .withField(StandardField.ISSN, "0886-5930");
    }

    @Test
    void checkSimpleUseCase() {
        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, FieldFormatterCleanups.parse("title[identity]"));

        FieldFormatterCleanup identityInTitle = new FieldFormatterCleanup(StandardField.TITLE, new IdentityFormatter());
        assertEquals(List.of(identityInTitle), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals(Optional.of("Educational session 1"), entry.getField(StandardField.TITLE));
    }

    @Test
    void invalidSaveActionSting() {
        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, FieldFormatterCleanups.parse("title"));

        assertEquals(List.of(), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals(Optional.of("Educational session 1"), entry.getField(StandardField.TITLE));
    }

    @Test
    void checkLowerCaseSaveAction() {
        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, FieldFormatterCleanups.parse("title[lower_case]"));

        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter());
        assertEquals(List.of(lowerCaseTitle), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals(Optional.of("educational session 1"), entry.getField(StandardField.TITLE));
    }

    @Test
    void checkTwoSaveActionsForOneField() {
        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, FieldFormatterCleanups.parse("title[lower_case,identity]"));

        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter());
        FieldFormatterCleanup identityInTitle = new FieldFormatterCleanup(StandardField.TITLE, new IdentityFormatter());
        assertEquals(Arrays.asList(lowerCaseTitle, identityInTitle), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals(Optional.of("educational session 1"), entry.getField(StandardField.TITLE));
    }

    @Test
    void checkThreeSaveActionsForOneField() {
        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, FieldFormatterCleanups.parse("title[lower_case,identity,normalize_date]"));

        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter());
        FieldFormatterCleanup identityInTitle = new FieldFormatterCleanup(StandardField.TITLE, new IdentityFormatter());
        FieldFormatterCleanup normalizeDatesInTitle = new FieldFormatterCleanup(StandardField.TITLE, new NormalizeDateFormatter());
        assertEquals(Arrays.asList(lowerCaseTitle, identityInTitle, normalizeDatesInTitle), actions.getConfiguredActions());

        actions.applySaveActions(entry);

        assertEquals(Optional.of("educational session 1"), entry.getField(StandardField.TITLE));
    }

    @Test
    void checkMultipleSaveActions() {
        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, FieldFormatterCleanups.parse("pages[normalize_page_numbers]title[lower_case]"));
        List<FieldFormatterCleanup> formatterCleanups = actions.getConfiguredActions();

        FieldFormatterCleanup normalizePages = new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter());
        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter());
        assertEquals(Arrays.asList(normalizePages, lowerCaseTitle), formatterCleanups);

        actions.applySaveActions(entry);

        assertEquals(Optional.of("educational session 1"), entry.getField(StandardField.TITLE));
        assertEquals(Optional.of("1--7"), entry.getField(StandardField.PAGES));
    }

    @Test
    void checkMultipleSaveActionsWithMultipleFormatters() {
        FieldFormatterCleanups actions = new FieldFormatterCleanups(true,
                FieldFormatterCleanups.parse("pages[normalize_page_numbers,normalize_date]title[lower_case]"));
        List<FieldFormatterCleanup> formatterCleanups = actions.getConfiguredActions();

        FieldFormatterCleanup normalizePages = new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter());
        FieldFormatterCleanup normalizeDatesInPages = new FieldFormatterCleanup(StandardField.PAGES, new NormalizeDateFormatter());
        FieldFormatterCleanup lowerCaseTitle = new FieldFormatterCleanup(StandardField.TITLE, new LowerCaseFormatter());
        assertEquals(Arrays.asList(normalizePages, normalizeDatesInPages, lowerCaseTitle), formatterCleanups);

        actions.applySaveActions(entry);

        assertEquals(Optional.of("educational session 1"), entry.getField(StandardField.TITLE));
        assertEquals(Optional.of("1--7"), entry.getField(StandardField.PAGES));
    }

    @Test
    void clearFormatterRemovesField() {
        FieldFormatterCleanups actions = new FieldFormatterCleanups(true, FieldFormatterCleanups.parse("month[clear]"));
        actions.applySaveActions(entry);

        assertEquals(Optional.empty(), entry.getField(StandardField.MONTH));
    }

    @Test
    void parserKeepsSaveActions() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = FieldFormatterCleanups.parse("""
                month[normalize_month]
                pages[normalize_page_numbers]
                title[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]
                booktitle[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]
                publisher[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]
                journal[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]
                abstract[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]
                """);

        List<FieldFormatterCleanup> expected = new ArrayList<>(30);
        expected.add(new FieldFormatterCleanup(StandardField.MONTH, new NormalizeMonthFormatter()));
        expected.add(new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter()));
        for (Field field : List.of(StandardField.TITLE, StandardField.BOOKTITLE, StandardField.PUBLISHER, StandardField.JOURNAL, StandardField.ABSTRACT)) {
            expected.add(new FieldFormatterCleanup(field, new EscapeAmpersandsFormatter()));
            expected.add(new FieldFormatterCleanup(field, new EscapeDollarSignFormatter()));
            expected.add(new FieldFormatterCleanup(field, new EscapeUnderscoresFormatter()));
            expected.add(new FieldFormatterCleanup(field, new LatexCleanupFormatter()));
        }

        assertEquals(expected, fieldFormatterCleanups);
    }

    @Test
    void parserParsesLatexCleanupFormatter() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = FieldFormatterCleanups.parse("""
                title[latex_cleanup]
                """);
        assertEquals(
                List.of(new FieldFormatterCleanup(StandardField.TITLE, new LatexCleanupFormatter())),
                fieldFormatterCleanups);
    }

    @Test
    void parserParsesTwoFormatters() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = FieldFormatterCleanups.parse("""
                title[escapeUnderscores,latex_cleanup]
                """);
        assertEquals(
                List.of(
                        new FieldFormatterCleanup(StandardField.TITLE, new EscapeUnderscoresFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new LatexCleanupFormatter())
                ),
                fieldFormatterCleanups);
    }

    @Test
    void parserParsesFourFormatters() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = FieldFormatterCleanups.parse("""
                title[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]
                """);
        assertEquals(
                List.of(
                        new FieldFormatterCleanup(StandardField.TITLE, new EscapeAmpersandsFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new EscapeDollarSignFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new EscapeUnderscoresFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new LatexCleanupFormatter())
                ),
                fieldFormatterCleanups);
    }

    @Test
    void parserParsesTwoFormattersWithCommas() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = FieldFormatterCleanups.parse("""
                title[escapeUnderscores,latex_cleanup]
                booktitle[escapeAmpersands,escapeDollarSign]
                """);
        assertEquals(
                List.of(
                        new FieldFormatterCleanup(StandardField.TITLE, new EscapeUnderscoresFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new LatexCleanupFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new EscapeAmpersandsFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new EscapeDollarSignFormatter())
                ),
                fieldFormatterCleanups);
    }

    @Test
    void parserParsesTwoFormattersOneWithComma() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = FieldFormatterCleanups.parse("""
                pages[normalize_page_numbers]
                booktitle[escapeAmpersands,escapeDollarSign]
                """);
        assertEquals(
                List.of(
                        new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new EscapeAmpersandsFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new EscapeDollarSignFormatter())
                ),
                fieldFormatterCleanups);
    }

    @Test
    void parserParsesThreeFormattersTwoWithComma() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = FieldFormatterCleanups.parse("""
                pages[normalize_page_numbers]
                title[escapeUnderscores,latex_cleanup]
                booktitle[escapeAmpersands,escapeDollarSign]
                """);
        assertEquals(
                List.of(
                        new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new EscapeUnderscoresFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new LatexCleanupFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new EscapeAmpersandsFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new EscapeDollarSignFormatter())
                ),
                fieldFormatterCleanups);
    }

    @Test
    void parserWithTwoAndThree() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = FieldFormatterCleanups.parse("""
                title[escapeAmpersands,escapeUnderscores,latex_cleanup]
                booktitle[escapeAmpersands,escapeUnderscores,latex_cleanup]
                """);

        List<FieldFormatterCleanup> expected = new ArrayList<>(30);
        for (Field field : List.of(StandardField.TITLE, StandardField.BOOKTITLE)) {
            expected.add(new FieldFormatterCleanup(field, new EscapeAmpersandsFormatter()));
            expected.add(new FieldFormatterCleanup(field, new EscapeUnderscoresFormatter()));
            expected.add(new FieldFormatterCleanup(field, new LatexCleanupFormatter()));
        }

        assertEquals(expected, fieldFormatterCleanups);
    }

    @Test
    void parserWithFourEntries() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = FieldFormatterCleanups.parse("""
                title[escapeUnderscores,latex_cleanup]
                booktitle[escapeAmpersands,escapeUnderscores,latex_cleanup]
                """);
        assertEquals(
                List.of(
                        new FieldFormatterCleanup(StandardField.TITLE, new EscapeUnderscoresFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new LatexCleanupFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new EscapeAmpersandsFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new EscapeUnderscoresFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new LatexCleanupFormatter())
                ),
                fieldFormatterCleanups);
    }

    @Test
    void parserTest() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = FieldFormatterCleanups.parse("""
                title[escapeAmpersands,escapeUnderscores,latex_cleanup]
                booktitle[escapeAmpersands,latex_cleanup]
                """);
        assertEquals(
                List.of(
                        new FieldFormatterCleanup(StandardField.TITLE, new EscapeAmpersandsFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new EscapeUnderscoresFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new LatexCleanupFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new EscapeAmpersandsFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new LatexCleanupFormatter())
                ),
                fieldFormatterCleanups);
    }

    @Test
    void identityCanBeParsed() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = FieldFormatterCleanups.parse("""
                all-text-fields[identity]
                date[normalize_date]
                month[normalize_month]
                pages[normalize_page_numbers]
                """);
        assertEquals(
                List.of(
                        new FieldFormatterCleanup(InternalField.INTERNAL_ALL_TEXT_FIELDS_FIELD, new IdentityFormatter()),
                        new FieldFormatterCleanup(StandardField.DATE, new NormalizeDateFormatter()),
                        new FieldFormatterCleanup(StandardField.MONTH, new NormalizeMonthFormatter()),
                        new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter())
                ),
                fieldFormatterCleanups);
    }

    @Test
    void getMetaDataStringWorks() {
        assertEquals("""
                pages[normalize_page_numbers]
                date[normalize_date]
                month[normalize_month]
                all-text-fields[replace_unicode_ligatures]
                keywords[MSC_codes_to_descriptions]
                """, FieldFormatterCleanups.getMetaDataString(FieldFormatterCleanups.DEFAULT_SAVE_ACTIONS, "\n"));
    }

    @Test
    void parsingOfDefaultSaveActions() {
        assertEquals(FieldFormatterCleanups.DEFAULT_SAVE_ACTIONS, FieldFormatterCleanups.parse("""
                pages[normalize_page_numbers]
                date[normalize_date]
                month[normalize_month]
                all-text-fields[replace_unicode_ligatures]
                keywords[MSC_codes_to_descriptions]
                """));
    }

    public static Stream<Arguments> formatterFromString() {
        return Stream.of(
                Arguments.of(new ReplaceUnicodeLigaturesFormatter(), "replace_unicode_ligatures"),
                Arguments.of(new LowerCaseFormatter(), "lower_case")
        );
    }

    @ParameterizedTest
    @MethodSource
    void formatterFromString(Formatter expected, String input) {
        assertEquals(expected, FieldFormatterCleanups.getFormatterFromString(input));
    }
}
