
package info.freelibrary.ark.pages;

import static j2html.TagCreator.*;

import info.freelibrary.ark.pages.partials.Footer;
import info.freelibrary.ark.pages.partials.Head;
import info.freelibrary.ark.pages.partials.Language;

import j2html.tags.specialized.BodyTag;
import j2html.tags.specialized.HeadTag;
import j2html.tags.specialized.HtmlTag;
import org.jetbrains.annotations.NotNull;

/**
 * A renderer for the administration page.
 */
public final class AdminPage {

    /**
     * Creates an administration page.
     */
    private AdminPage() {
        // This is intentionally left empty
    }

    /**
     * Render the main page as an HTML string.
     *
     * @param aTitle A title
     * @param aAcceptLang A language to accept
     * @return An HTML string
     */
    @NotNull
    public static String render(final String aTitle, final String aAcceptLang) {
        final HeadTag head = new Head(aTitle).getTag();
        final HtmlTag html = html(head, getBody());

        // Set the language of our page
        html.attr(new Language(aAcceptLang));

        return document(html);
    }

    /**
     * Gets the administration page's body.
     *
     * @return The administration page's body
     */
    @NotNull
    private static BodyTag getBody() {
        return body(div("Hello world!").attr("class", "container-fluid"), new Footer().getTag());
    }

}
