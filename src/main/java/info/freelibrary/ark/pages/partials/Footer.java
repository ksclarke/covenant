
package info.freelibrary.ark.pages.partials;

import j2html.TagCreator;
import j2html.tags.ContainerTag;

/**
 * A page footer element.
 */
public class Footer {

    private static final String SRC = "src";

    private static final String INTEGRITY = "integrity";

    /**
     * Creates a new page footer.
     */
    public Footer() {

    }

    /**
     * Gets the page footer tag.
     *
     * @return The page footer tag
     */
    public ContainerTag getTag() {
        final ContainerTag jquery = TagCreator.script();
        final ContainerTag popper = TagCreator.script();
        final ContainerTag bootstrap = TagCreator.script();
        final AnonCrossOrigin crossOrigin = new AnonCrossOrigin();

        // JQuery Javascript
        jquery.attr(SRC, "https://code.jquery.com/jquery-3.3.1.slim.min.js");
        jquery.attr(INTEGRITY, "sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo");
        jquery.attr(crossOrigin);

        // Popper Javascript
        popper.attr(SRC, "https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js");
        popper.attr(INTEGRITY, "sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1");
        popper.attr(crossOrigin);

        // Bootstrap Javascript
        bootstrap.attr(SRC, "https://stackpath.bootstrapcdn.com/bootstrap/4.3.0/js/bootstrap.min.js");
        bootstrap.attr(INTEGRITY, "sha384-7aThvCh9TypR7fIc2HV4O/nFMVCBwyIUKL8XCtKE+8xgCgl/PQGuFsvShjr74PBp");
        bootstrap.attr(crossOrigin);

        return TagCreator.footer(jquery, popper, bootstrap);
    }
}
