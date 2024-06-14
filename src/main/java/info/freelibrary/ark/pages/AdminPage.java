
package info.freelibrary.ark.pages;

import static j2html.TagCreator.*;

import info.freelibrary.ark.pages.partials.Footer;
import info.freelibrary.ark.pages.partials.Head;
import info.freelibrary.ark.pages.partials.Language;

import j2html.tags.ContainerTag;

/**
 * A renderer for the administration page.
 */
public final class AdminPage {

    /* Creates an administration page. */
    private AdminPage() {
    }

    /**
     * Render the main page as an HTML string.
     *
     * @param aTitle A title
     * @param aAcceptLanguage An accept language
     * @return An HTML string
     */
    public static String render(final String aTitle, final String aAcceptLanguage) {
        final ContainerTag head = new Head(aTitle).getTag();
        final ContainerTag html = html(head, getBody());

        // Set the language of our page
        html.attr(new Language(aAcceptLanguage));

        return document(html);
    }

    /* Gets the administration page's body. */
    private static ContainerTag getBody() {
        return body(div("Hello world!").attr("class", "container-fluid"), new Footer().getTag());
    }

}
