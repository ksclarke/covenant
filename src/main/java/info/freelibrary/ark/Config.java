
package info.freelibrary.ark;

/**
 * Properties that are used to configure the application
 */
public final class Config {

    /**
     * The configuration property for the port that Covenant should run on.
     */
    public static final String HTTP_PORT = "http.port";

    /**
     * The location where our database files are kept, preferably on local disk.
     */
    public static final String DB_FILES_DIR = "db.files.dir";

    // Constant classes should have private constructors
    private Config() {
    }

}
