package ai;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;

/**
 * AI Self-Healing Locator Engine
 * Author: Muhammad Ammar Ahmed — Senior Test Automation Engineer
 *
 * Automatically recovers from broken locators using similarity scoring
 * and multiple fallback strategies — eliminating flaky tests.
 */
public class SelfHealingLocator {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private static final Logger logger = Logger.getLogger(SelfHealingLocator.class.getName());
    private static final double CONFIDENCE_THRESHOLD = 0.6;
    private final List<HealingEvent> healingHistory = new ArrayList<>();

    private enum Strategy { ID, NAME, CSS, ARIA_LABEL, DATA_TESTID, PARTIAL_TEXT, FUZZY }

    public SelfHealingLocator(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    /** Find element — tries primary locator, auto-heals if broken */
    public WebElement find(By primary, ElementContext ctx) {
        try {
            return wait.until(ExpectedConditions.presenceOfElementLocated(primary));
        } catch (TimeoutException | NoSuchElementException e) {
            logger.warning("Primary locator failed: " + primary + " — healing...");
            return heal(primary, ctx);
        }
    }

    private WebElement heal(By failed, ElementContext ctx) {
        Map<WebElement, Double> candidates = new LinkedHashMap<>();
        for (Strategy s : Strategy.values()) {
            for (WebElement el : getCandidates(s, ctx)) {
                double score = score(el, ctx, s);
                if (score >= CONFIDENCE_THRESHOLD) candidates.put(el, score);
            }
        }
        if (candidates.isEmpty())
            throw new NoSuchElementException("AI healing failed for: " + failed);

        WebElement best = candidates.entrySet().stream()
            .max(Map.Entry.comparingByValue()).get().getKey();
        double confidence = candidates.get(best);

        logger.info(String.format("AI healed! Confidence: %.0f%% | %s", confidence * 100, desc(best)));
        healingHistory.add(new HealingEvent(failed.toString(), desc(best), confidence));
        return best;
    }

    private List<WebElement> getCandidates(Strategy s, ElementContext ctx) {
        try {
            switch (s) {
                case ID:          return ctx.id != null ? driver.findElements(By.id(ctx.id)) : List.of();
                case NAME:        return ctx.name != null ? driver.findElements(By.name(ctx.name)) : List.of();
                case CSS:         return ctx.css != null ? driver.findElements(By.cssSelector(ctx.css)) : List.of();
                case ARIA_LABEL:  return ctx.ariaLabel != null ? driver.findElements(By.cssSelector("[aria-label='" + ctx.ariaLabel + "']")) : List.of();
                case DATA_TESTID: return ctx.testId != null ? driver.findElements(By.cssSelector("[data-testid='" + ctx.testId + "']")) : List.of();
                case PARTIAL_TEXT:return ctx.text != null ? driver.findElements(By.xpath("//*[contains(text(),'" + ctx.text + "')]")) : List.of();
                case FUZZY:       return fuzzyMatch(ctx.text);
                default:          return List.of();
            }
        } catch (Exception e) { return List.of(); }
    }

    private List<WebElement> fuzzyMatch(String text) {
        if (text == null) return List.of();
        List<WebElement> result = new ArrayList<>();
        for (WebElement el : driver.findElements(By.xpath("//*[text()]"))) {
            if (similarity(el.getText(), text) > 0.7) result.add(el);
        }
        return result;
    }

    private double score(WebElement el, ElementContext ctx, Strategy s) {
        double score = 0, weight = 0;
        if (ctx.tag != null)       { weight += 0.2; if (el.getTagName().equalsIgnoreCase(ctx.tag)) score += 0.2; }
        if (ctx.text != null)      { weight += 0.3; score += 0.3 * similarity(el.getText(), ctx.text); }
        if (ctx.className != null) { weight += 0.2; score += 0.2 * similarity(el.getAttribute("class"), ctx.className); }
        weight += 0.15; score += 0.15 * strategyBonus(s);
        return weight > 0 ? score / weight : 0;
    }

    private double similarity(String a, String b) {
        if (a == null || b == null) return 0;
        a = a.toLowerCase().trim(); b = b.toLowerCase().trim();
        if (a.equals(b)) return 1.0;
        int max = Math.max(a.length(), b.length());
        return max == 0 ? 1.0 : 1.0 - (double) levenshtein(a, b) / max;
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length()+1][b.length()+1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++)
            for (int j = 1; j <= b.length(); j++)
                dp[i][j] = a.charAt(i-1)==b.charAt(j-1) ? dp[i-1][j-1] :
                    1 + Math.min(dp[i-1][j-1], Math.min(dp[i-1][j], dp[i][j-1]));
        return dp[a.length()][b.length()];
    }

    private double strategyBonus(Strategy s) {
        switch (s) {
            case DATA_TESTID: return 1.0; case ID: return 0.9;
            case ARIA_LABEL:  return 0.8; case NAME: return 0.7;
            case CSS:         return 0.6; case PARTIAL_TEXT: return 0.5;
            case FUZZY:       return 0.3; default: return 0.2;
        }
    }

    private String desc(WebElement el) {
        String text = el.getText();
        return String.format("<%s id='%s' text='%s'>", el.getTagName(),
            el.getAttribute("id"), text.length() > 30 ? text.substring(0, 30) : text);
    }

    public void printReport() {
        System.out.println("\n=== AI Self-Healing Report ===");
        System.out.println("Healing events: " + healingHistory.size());
        for (HealingEvent e : healingHistory)
            System.out.printf("  FAILED: %s%n  HEALED: %s (%.0f%%)%n%n",
                e.failed, e.healed, e.confidence * 100);
    }

    // Context builder
    public static class ElementContext {
        public String id, name, css, text, tag, className, ariaLabel, testId;
        public static Builder builder() { return new Builder(); }
        public static class Builder {
            private final ElementContext c = new ElementContext();
            public Builder id(String v)        { c.id = v; return this; }
            public Builder name(String v)      { c.name = v; return this; }
            public Builder css(String v)       { c.css = v; return this; }
            public Builder text(String v)      { c.text = v; return this; }
            public Builder tag(String v)       { c.tag = v; return this; }
            public Builder className(String v) { c.className = v; return this; }
            public Builder ariaLabel(String v) { c.ariaLabel = v; return this; }
            public Builder testId(String v)    { c.testId = v; return this; }
            public ElementContext build()      { return c; }
        }
    }

    public static class HealingEvent {
        public final String failed, healed;
        public final double confidence;
        public HealingEvent(String f, String h, double c) { failed=f; healed=h; confidence=c; }
    }
                        }
