package org.apache.freemarker.core;

import java.io.IOException;

import org.apache.freemarker.core.outputformat.impl.HTMLOutputFormat;
import org.apache.freemarker.core.pluggablebuiltin.impl.DefaultTruncateBuiltinAlgorithm;
import org.apache.freemarker.test.TemplateTest;
import org.junit.Before;
import org.junit.Test;

public class TruncateBuiltInTest extends TemplateTest {

    private static final String M_TERM_SRC = "<span class=trunc>&hellips;</span>";

    @Override
    protected void setupConfigurationBuilder(Configuration.ExtendableBuilder<?> cb) {
        cb.outputFormat(HTMLOutputFormat.INSTANCE);
    }

    @Before
    public void setup() throws TemplateException {
        addToDataModel("t", "Some text for truncation testing.");
        addToDataModel("u", "CaNotBeBrokenAnywhere");
        addToDataModel("mTerm", HTMLOutputFormat.INSTANCE.fromMarkup(M_TERM_SRC));
    }

    @Test
    public void testTruncate() throws IOException, TemplateException {
        assertOutput("${t?truncate(20)}", "Some text for [...]");
        assertOutput("${t?truncate(20, '|')}", "Some text for |");
        assertOutput("${t?truncate(20, '|', 7)}", "Some text |");

        assertOutput("${u?truncate(20)}", "CaNotBeBrokenAn[...]");
        assertOutput("${u?truncate(20, '|')}", "CaNotBeBrokenAnywhe|");
        assertOutput("${u?truncate(20, '|', 3)}", "CaNotBeBrokenAnyw|");

        assertOutput("${t?truncate(20)?isMarkupOutput?c}", "false");

        // Edge cases that are still allowed:
        assertOutput("${t?truncate(0)}", "[...]");
        assertOutput("${u?truncate(3, '', 0)}", "CaN");

        // Disallowed:
        assertErrorContains("${t?truncate(200, mTerm)}", "2nd", "string", "markupOutput");
        assertErrorContains("${t?truncate(-1)}", "1st", "at least 0");
        assertErrorContains("${t?truncate(200, 'x', -1)}", "3rd", "at least 0");
    }

    @Test
    public void testTruncateM() throws IOException, TemplateException {
        assertOutput("${t?truncateM(15)}", "Some text <span class='truncateTerminator'>[&#8230;]</span>"); // String arg allowed...
        assertOutput("${t?truncateM(15, mTerm)}", "Some text for " + M_TERM_SRC);
        assertOutput("${t?truncateM(15, mTerm)}", "Some text for " + M_TERM_SRC);
        assertOutput("${t?truncateM(15, mTerm, 3)}", "Some text " + M_TERM_SRC);

        assertOutput("${u?truncateM(20, mTerm)}", "CaNotBeBrokenAnywhe" + M_TERM_SRC);
        assertOutput("${u?truncateM(20, mTerm, 3)}", "CaNotBeBrokenAnyw" + M_TERM_SRC);

        assertOutput("${t?truncateM(15, '|')}", "Some text for |"); // String arg allowed...
        assertOutput("${t?truncateM(15, '|')?isMarkupOutput?c}", "false"); // ... and results in string.
        assertOutput("${t?truncateM(15, mTerm)?isMarkupOutput?c}", "true");
    }

    @Test
    public void testTruncateC() throws IOException, TemplateException {
        assertOutput("${t?truncateC(20)}", "Some text for t[...]");
        assertOutput("${t?truncateC(20)}", "Some text for t[...]");
        assertOutput("${t?truncateC(20, '|')}", "Some text for trunc|");
        assertOutput("${t?truncateC(20, '|', 0)}", "Some text for trunca|");

        assertErrorContains("${t?truncateC(200, mTerm)}", "2nd", "string", "markupOutput");

        assertOutput("${t?truncateC(20)?isMarkupOutput?c}", "false");
    }

    @Test
    public void testTruncateCM() throws IOException, TemplateException {
        assertOutput("${t?truncateCM(20, mTerm)}", "Some text for trunc" + M_TERM_SRC);
        assertOutput("${t?truncateCM(20, mTerm, 3)}", "Some text for tru" + M_TERM_SRC);

        assertOutput("${t?truncateCM(20)?isMarkupOutput?c}", "true");
        assertOutput("${t?truncateCM(20, '|')?isMarkupOutput?c}", "false");
        assertOutput("${t?truncateCM(20, mTerm)?isMarkupOutput?c}", "true");
    }

    @Test
    public void testTruncateW() throws IOException, TemplateException {
        assertOutput("${t?truncateW(20)}", "Some text for [...]");
        assertOutput("${u?truncateW(20)}", "[...]");  // Proof of no fallback to C

        assertErrorContains("${t?truncateW(200, mTerm)}", "2nd", "string", "markupOutput");

        assertOutput("${t?truncateW(20)?isMarkupOutput?c}", "false");
        assertOutput("${t?truncateW(20, '|')?isMarkupOutput?c}", "false");
    }

    @Test
    public void testTruncateWM() throws IOException, TemplateException {
        assertOutput("${t?truncateWM(15, mTerm)}", "Some text for " + M_TERM_SRC);
        assertOutput("${t?truncateWM(15, mTerm)}", "Some text for " + M_TERM_SRC);
        assertOutput("${t?truncateWM(15, mTerm, 3)}", "Some text " + M_TERM_SRC);

        assertOutput("${u?truncateWM(20, mTerm)}", M_TERM_SRC); // Proof of no fallback to C

        assertOutput("${t?truncateCM(20)?isMarkupOutput?c}", "true");
        assertOutput("${t?truncateCM(20, '|')?isMarkupOutput?c}", "false");
        assertOutput("${t?truncateCM(20, mTerm)?isMarkupOutput?c}", "true");
    }

    @Test
    public void testSettingHasEffect() throws IOException, TemplateException {
        assertOutput("${t?truncate(20)}", "Some text for [...]");
        assertOutput("${t?truncateC(20)}", "Some text for t[...]");
        setConfiguration(newConfigurationBuilder().truncateBuiltinAlgorithm(
                DefaultTruncateBuiltinAlgorithm.UNICODE_INSTANCE));
        assertOutput("${t?truncate(20)}", "Some text for [\u2026]");
        assertOutput("${t?truncateC(20)}", "Some text for tru[\u2026]");
    }

    @Test
    public void testDifferentMarkupSeparatorSetting() throws IOException, TemplateException {
        assertOutput("${t?truncate(20)}", "Some text for [...]");
        assertOutput("${t?truncateM(20)}", "Some text for <span class='truncateTerminator'>[&#8230;]</span>");
        setConfiguration(newConfigurationBuilder().truncateBuiltinAlgorithm(
                new DefaultTruncateBuiltinAlgorithm("|...", HTMLOutputFormat.INSTANCE.fromMarkup(M_TERM_SRC), true)));
        assertOutput("${t?truncate(20)}", "Some text for |...");
        assertOutput("${t?truncateM(20)}", "Some text for " + M_TERM_SRC);
    }

}
