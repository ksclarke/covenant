
package info.freelibrary.ark.pages.partials;

import j2html.attributes.Attribute;

/**
 * A language attribute for a page.
 */
public class Language extends Attribute {

    /**
     * Creates a language attribute for a page.
     *
     * @param aAcceptLanguage A language to accept
     */
    public Language(final String aAcceptLanguage) {
        super("language", "en"); // TODO: set according to Accept-Language
    }

}
