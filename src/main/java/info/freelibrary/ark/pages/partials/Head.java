
package info.freelibrary.ark.pages.partials;

import static j2html.TagCreator.*;

import j2html.tags.ContainerTag;
import j2html.tags.EmptyTag;

/**
 * A page's head element.
 */
public class Head {

    private final String myTitle;

    /**
     * Creates a new head element.
     *
     * @param aTitle The title for the head element
     */
    public Head(final String aTitle) {
        myTitle = aTitle;
    }

    /**
     * Gets the head element's tag.
     *
     * @return The head element's tag
     */
    public ContainerTag getTag() {
        final EmptyTag charset = meta();
        final EmptyTag viewport = meta();
        final EmptyTag stylesheet = link();

        // Charset
        charset.attr("charset", "utf-8");

        // Viewport
        viewport.attr("name", "viewport");
        viewport.attr("content", "width=device-width, initial-scale=1, shrink-to-fit=no");

        // Stylesheet
        stylesheet.attr("rel", "stylesheet");
        stylesheet.attr("href", "https://stackpath.bootstrapcdn.com/bootstrap/4.3.0/css/bootstrap.min.css");
        stylesheet.attr("integrity", "sha384-PDle/QlgIONtM1aqA2Qemk5gPOE7wFq8+Em+G/hmo5Iq0CCmYZLv3fVRDJ4MMwEA");
        stylesheet.attr(new AnonCrossOrigin());

        return head(charset, viewport, stylesheet, title(myTitle));
    }

}
