/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package org.aitools.programd.multiplexor;

import java.util.logging.Logger;

import org.aitools.programd.Core;
import org.aitools.programd.CoreSettings;
import org.aitools.programd.bot.Bots;
import org.aitools.programd.graph.Graphmaster;
import org.aitools.programd.parser.TemplateParser;
import org.aitools.programd.responder.Responder;


/**
 * <p>
 * &quot;To multiplex&quot; means &quot;to select one from many inputs&quot;. A
 * <code>Multiplexor</code> multiplexes the clients of a bot and keeps track
 * of all their predicate values.
 * </p>
 * <p>
 * The following metaphor was supplied by Richard Wallace: The
 * <code>Multiplexor</code> controls a short &quot;carnival ride&quot; for
 * each user. The Multiplexor puts the client in his/her seat, hands him/her an
 * id card, and closes the door. The client gets one &quot;turn of the
 * crank&quot;. He/she enters his/her id, multiline query, and then receives the
 * reply. The door opens, the Multiplexor ushers him/her out, and seats the next
 * client.
 * </p>
 * 
 * @since 4.1.3
 * @version 4.5
 * @author <a href="mailto:noel@aitools.org">Noel Bush</a>
 * @author Richard Wallace, Jon Baer
 * @author Thomas Ringate/Pedro Colla
 */

/*20/10/05 sis05kol: Removed functionality from Multiplexor
 * Created two Multiplex sub-classes to accomodate for Ontology interaction:
 * 		NormalMultiplexor : as old Multiplexor
 * 		OntologyMultiplexor : with Ontology communication
 */
abstract public class Multiplexor
{
    // Convenience constants.

    /** The name of the <code>that</code> special predicate. */
    protected static final String THAT = "that";

    /** The name of the <code>topic</code> special predicate. */
    protected static final String TOPIC = "topic";

    /** The name of the <code>input</code> special predicate. */
    protected static final String INPUT = "input";

    /** The name of the <code>star</code> special predicate. */
    protected static final String STAR = "star";

    /** An empty string. */
    protected static final String EMPTY_STRING = "";

    /** A space. */
    protected static final String SPACE = " ";

    /** The word &quot;value&quot;. */
    protected static final String VALUE = "value";

    /** An asterisk (used in String production) */
    protected static final String ASTERISK = "*";

    /** A quote mark. */
    protected static final String QUOTE_MARK = "\"";

    /** The string &quot;{@value}&quot;. */
    protected static final String PROMPT = "> ";

    /** The string &quot;{@value}&quot;. */
    protected static final String LABEL_MATCH = "Match: ";

    /** The string &quot;{@value}&quot;. */
    protected static final String LABEL_FILENAME = "Filename: ";

    /** The string &quot;{@value}&quot;. */
    protected static final String RESPONSE_SPACE = "Response ";

    /** The string &quot;{@value}&quot;. */
    protected static final String SPACE_IN_SPACE = " in ";

    /** The string &quot;{@value}&quot;. */
    protected static final String MS_AVERAGE = " ms. (Average: ";

    /* The string &quot; ms).&quot; */
    protected static final String MS = " ms.)";

    /** The predicate empty default. */
    protected String predicateEmptyDefault;

    /** A secret key used for (weakly) authorizing authentication requests. */
    protected static String SECRET_KEY;

    // Class variables.

    /** The Core that owns this Multiplexor. */
    protected Core core;

    /** The Graphmaster in use by the Core. */
    protected Graphmaster graphmaster;

    /** The PredicateMaster in use by the Core. */
    protected PredicateMaster predicateMaster;

    /** The Bots object that belongs to the Core. */
    protected Bots bots;

    /** The general log where we will record some events. */
    protected Logger logger;

    /**
     * The match logger where information about individual matches can be
     * recorded.
     */
    protected Logger matchLogger;

    /** Whether or not to record match trace info. */
    protected boolean recordMatchTrace;

    /** The time that the Multiplexor started operation. */
    protected long startTime = System.currentTimeMillis();

    /** A counter for tracking the number of responses produced. */
    protected long responseCount = 0;

    /** The total response time. */
    protected long totalTime = 0;

    /** A counter for tracking average response time. */
    protected float avgResponseTime = 0;

    /**
     * Constructs the Multiplexor, using some values taken from the Core
     * object's settings. Note that the {@link #predicateMaster} is <i>not</i>
     * initialized -- it must be {@link #attach}ed subsequently.
     * 
     * @param coreOwner the Core that owns this Multiplexor
     */
    public Multiplexor(Core coreOwner)
    {
        this.core = coreOwner;
        this.graphmaster = this.core.getGraphmaster();
        this.logger = Logger.getLogger("programd");
        this.matchLogger = Logger.getLogger("programd.matching");
        this.bots = this.core.getBots();
        CoreSettings coreSettings = this.core.getSettings();
        this.predicateEmptyDefault = coreSettings.getPredicateEmptyDefault();
        this.recordMatchTrace = coreSettings.recordMatchTrace();
    }

    /**
     * Saves a predicate for a given <code>userid</code>. This only applies
     * to Multiplexors that provide long-term storage (others may just do
     * nothing).
     * 
     * @since 4.1.4
     * @param name predicate name
     * @param value predicate value
     * @param userid user identifier
     * @param botid
     */
    abstract public void savePredicate(String name, String value, String userid, String botid);

    /**
     * Loads a predicate into memory for a given <code>userid</code>. This
     * only applies to Multiplexors that provide long-term storage (others may
     * just do nothing).
     * 
     * @since 4.1.4
     * @param name predicate name
     * @param userid user identifier
     * @param botid
     * @return the predicate value
     * @throws NoSuchPredicateException if there is no predicate with this name
     */
    abstract public String loadPredicate(String name, String userid, String botid) throws NoSuchPredicateException;

    /**
     * Checks whether a given userid and password combination is valid.
     * Multiplexors for which this makes no sense should just return true.
     * 
     * @param userid the userid to check
     * @param botid
     * @param password the password to check
     * @param secretKey the secret key that should authenticate this request
     * @return whether the userid and password combination is valid
     */
    abstract public boolean checkUser(String userid, String password, String secretKey, String botid);

    /**
     * Creates a new user entry, given a userid and password. Multiplexors for
     * which this makes no sense should just return true.
     * 
     * @param userid the userid to use
     * @param botid
     * @param password the password to assign
     * @param secretKey the secret key that should authenticate this request
     * @throws DuplicateUserIDError if the given userid was already found in the
     *             system
     */
    abstract public void createUser(String userid, String password, String secretKey, String botid) throws DuplicateUserIDError;

    /**
     * Changes the password associated with a userid. Multiplexors for which
     * this makes no sense should just return true.
     * 
     * @param userid the userid
     * @param botid
     * @param password the new password
     * @param secretKey the secret key that should authenticate this request
     * @return whether the change was successful
     */
    abstract public boolean changePassword(String userid, String password, String secretKey, String botid);

    /**
     * Returns a count of known userids. This may be defined differently for
     * different multiplexors.
     * 
     * @param botid the botid for which we want a count of known userids
     * @return a count of known userids
     */
    abstract public int useridCount(String botid);

    /**
     * Attaches the given
     * {@link org.aitools.programd.multiplexor.PredicateMaster PredicateMaster}
     * to this <code>Multiplexor</code>.
     * 
     * @param predicateMasterToAttach
     */
    abstract public void attach(PredicateMaster predicateMasterToAttach);
    
    /**
     * Initializes the <code>Multiplexor</code>, creating the secret key that
     * can be used for a weak form of authentication.
     */
    abstract public void initialize();

    /**
     * Returns the response to a non-internal input, using a Responder.
     * 
     * @param input the &quot;non-internal&quot; (possibly multi-sentence,
     *            non-substituted) input
     * @param userid the userid for whom the response will be generated
     * @param botid the botid from which to get the response
     * @param responder the Responder who cares about this response
     * @return the response
     */
    abstract public String getResponse(String input, String userid, String botid, Responder responder);

    /**
     * Returns the response to a non-internal input, without using a Responder.
     * 
     * @param input the &quot;non-internal&quot; (possibly multi-sentence,
     *            non-substituted) input
     * @param userid the userid for whom the response will be generated
     * @param botid the botid from which to get the response
     * @return the response
     */
    abstract public String getResponse(String input, String userid, String botid);

    /**
     * <p>
     * Produces a response to an &quot;internal&quot; input sentence -- i.e., an
     * input that has been produced by a <code>srai</code>.
     * </p>
     * <p>
     * The main differences between this and
     * {@link #getResponse(String,String,String,Responder)} are that this method
     * takes an already-existing <code>TemplateParser</code>, <i>doesn't </i>
     * take a <code>Responder</code>, and assumes that the inputs have
     * already been normalized.
     * </p>
     * 
     * @param input the input sentence
     * @param userid the userid requesting the response
     * @param botid the botid from which to get the response
     * @param parser the parser object to update when generating the response
     * @return the response
     */
    abstract public String getInternalResponse(String input, String userid, String botid, TemplateParser parser);
}