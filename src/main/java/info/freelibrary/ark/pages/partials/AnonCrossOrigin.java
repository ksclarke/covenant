
package info.freelibrary.ark.pages.partials;

import j2html.attributes.Attribute;

/**
 * An attribute that allows anonymous cross-origin access.
 */
public final class AnonCrossOrigin extends Attribute {

    /**
     * Creates a new cross-origin attribute that allows anonymous access.
     */
    public AnonCrossOrigin() {
        super("crossorigin", "anonymous");
    }
}
