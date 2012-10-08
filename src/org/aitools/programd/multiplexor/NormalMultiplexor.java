/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package org.aitools.programd.multiplexor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.aitools.programd.Core;
import org.aitools.programd.bot.Bot;
import org.aitools.programd.graph.Graphmaster;
import org.aitools.programd.graph.Match;
import org.aitools.programd.parser.TemplateParser;
import org.aitools.programd.parser.TemplateParserException;
import org.aitools.programd.processor.ProcessorException;
import org.aitools.programd.responder.Responder;
import org.aitools.programd.util.DeveloperError;
import org.aitools.programd.util.FileManager;
import org.aitools.programd.util.InputNormalizer;
import org.aitools.programd.util.NoMatchException;
import org.aitools.programd.util.UserError;
import org.aitools.programd.util.XMLKit;

/**
 *
 * @since NEW
 * @version 4.5.ont
 * @author <a href="mailto:k.o.lundqvist@rdg.ac.uk">Karsten Oster Lundqvist</a>
 * 20/10/05 sis05kol: Build to keep standard functionality 
 * 
 * Developed using
 * org.aitools.programd.multiplexor
 * @author <a href="mailto:noel@aitools.org">Noel Bush</a>
 * @author Richard Wallace, Jon Baer
 * @author Thomas Ringate/Pedro Colla
 */
abstract public class NormalMultiplexor extends Multiplexor
{
 

    /**
     * Constructs the NormalMultiplexor, using some values taken from the Core
     * object's settings. Note that the {@link #predicateMaster} is <i>not</i>
     * initialized -- it must be {@link #attach}ed subsequently.
     * 
     * @param coreOwner the Core that owns this Multiplexor
     */
    public NormalMultiplexor(Core coreOwner)
    {
    	super(coreOwner);
    }

    /**
     * Attaches the given
     * {@link org.aitools.programd.multiplexor.PredicateMaster PredicateMaster}
     * to this <code>Multiplexor</code>.
     * 
     * @param predicateMasterToAttach
     */
    public void attach(PredicateMaster predicateMasterToAttach)
    {
        this.predicateMaster = predicateMasterToAttach;
    }

    /**
     * Initializes the <code>Multiplexor</code>, creating the secret key that
     * can be used for a weak form of authentication.
     */
    public void initialize()
    {
        SECRET_KEY = new Double(Math.random() * System.currentTimeMillis()).toString();
        File keyFile = FileManager.getFile("secret.key");
        keyFile.delete();
        try
        {
            keyFile.createNewFile();
        }
        catch (IOException e)
        {
            throw new UserError("Error creating secret key file.", e);
        }
        PrintWriter out;
        try
        {
            out = new PrintWriter(new FileOutputStream(keyFile));
        }
        catch (FileNotFoundException e)
        {
            throw new UserError("Error writing secret key file.", e);
        }
        out.print(SECRET_KEY);
        out.flush();
        out.close();
    }

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
    public synchronized String getResponse(String input, String userid, String botid, Responder responder)
    {
    	if (input.equals("/EXIT")) 
    	{
    		@SuppressWarnings("unused") int i=1;
    	}
    	
        // Get the specified bot object.
        Bot bot = this.bots.getBot(botid);

        // Split sentences (after performing substitutions and responder
        // pre-processing).
        List<String> sentenceList = bot.sentenceSplit(bot.applyInputSubstitutions(responder.preprocess(input)));

        // Get an iterator on the replies.
        Iterator<String> replies = getReplies(sentenceList, userid, botid).iterator();

        // Start by assuming an empty response.
        String response = EMPTY_STRING;

        // For each input sentence...
        for (String sentence : sentenceList)
        {
            // ...ask the responder to append the reply to the response, and
            // accumulate the result.
            response = responder.append(sentence, replies.next(), response);
        }

        logResponse(input, response, userid, botid);
        
        // Finally, ask the responder to postprocess the response, and return
        // the result.
        response = responder.postprocess(response);

        // Return the response (may be just EMPTY_STRING!)
        return response;
    }

    /**
     * Returns the response to a non-internal input, without using a Responder.
     * 
     * @param input the &quot;non-internal&quot; (possibly multi-sentence,
     *            non-substituted) input
     * @param userid the userid for whom the response will be generated
     * @param botid the botid from which to get the response
     * @return the response
     */
    public synchronized String getResponse(String input, String userid, String botid)
    {
    	if (input.equals("/EXIT")) 
    	{
    		@SuppressWarnings("unused") int i=1;
    	}
    	
        // Get the specified bot object.
        Bot bot = this.bots.getBot(botid);

        // Split sentences (after performing substitutions).
        List<String> sentenceList = bot.sentenceSplit(bot.applyInputSubstitutions(input));

        // Get an iterator on the replies.
        Iterator<String> replies = getReplies(sentenceList, userid, botid).iterator();

        // Start by assuming an empty response.
        String response = EMPTY_STRING;

        // For each input sentence...
        for (@SuppressWarnings("unused") String sentence : sentenceList)
        {
            // Append the reply to the response.
            response += replies.next();
        }

        logResponse(input, response, userid, botid);
        
        // Return the response (may be just EMPTY_STRING!)
        return response;
    }

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
    public String getInternalResponse(String input, String userid, String botid, TemplateParser parser)
    {
        // Get the requested bot.
        Bot bot = this.bots.getBot(botid);

        // Ready the that and topic predicates for constructing the match path.
        List<String> thatSentences = bot.sentenceSplit(this.predicateMaster.get(THAT, 1, userid, botid));
        String that = InputNormalizer.patternFitIgnoreCase(thatSentences.get(thatSentences.size() - 1));

        if (that.equals(EMPTY_STRING) || that.equals(this.predicateEmptyDefault))
        {
            that = ASTERISK;
        }

        String topic = InputNormalizer.patternFitIgnoreCase(this.predicateMaster.get(TOPIC, userid, botid));
        if (topic.equals(EMPTY_STRING) || topic.equals(this.predicateEmptyDefault))
        {
            topic = ASTERISK;
        }

        return getMatchResult(input, that, topic, userid, botid, parser);
    }

    /**
     * Gets the list of replies to some input sentences. Assumes that the
     * sentences have already had all necessary pre-processing and substitutions
     * performed.
     * 
     * @param sentenceList the input sentences
     * @param userid the userid requesting the replies
     * @param botid
     * @return the list of replies to the input sentences
     */
    private List<String> getReplies(List<String> sentenceList, String userid, String botid)
    {
        // All replies will be assembled in this ArrayList.
        List<String> replies = Collections.checkedList(new ArrayList<String>(sentenceList.size()), String.class);

        // Get the requested bot.
        Bot bot = this.bots.getBot(botid);

        // Ready the that and topic predicates for constructing the match path.
        List<String> thatSentences = bot.sentenceSplit(this.predicateMaster.get(THAT, 1, userid, botid));
        String that = InputNormalizer.patternFitIgnoreCase(thatSentences.get(thatSentences.size() - 1));

        if (that.equals(EMPTY_STRING) || that.equals(this.predicateEmptyDefault))
        {
            that = ASTERISK;
        }

        String topic = InputNormalizer.patternFitIgnoreCase(this.predicateMaster.get(TOPIC, userid, botid));
        if (topic.equals(EMPTY_STRING) || topic.equals(this.predicateEmptyDefault))
        {
            topic = ASTERISK;
        }

        // We might use this to track matching statistics.
        long time = 0;

        // If match trace info is on, mark the time just before matching starts.
        if (this.recordMatchTrace)
        {
            time = System.currentTimeMillis();
        }

        // Get a reply for each sentence.
        for (String sentence : sentenceList)
        {
            replies.add(getReply(sentence, that, topic, userid, botid));
        }

        // Increment the (static) response count.
        this.responseCount++;

        // If match trace info is on, produce statistics about the response
        // time.
        if (this.recordMatchTrace)
        {
            // Mark the time that processing is finished.
            time = System.currentTimeMillis() - time;

            // Calculate the average response time.
            this.totalTime += time;
            this.avgResponseTime = (float) this.totalTime / (float) this.responseCount;
            this.matchLogger.log(Level.FINE, RESPONSE_SPACE + this.responseCount + SPACE_IN_SPACE + time + MS_AVERAGE + this.avgResponseTime + MS);
        }

        // Invoke targeting if appropriate.
        /*
         * if (responseCount % TARGET_SKIP == 0) { if (USE_TARGETING) {
         * Graphmaster.checkpoint(); } }
         */

        // If no replies, return an empty string.
        if (replies.size() == 0)
        {
            replies.add(EMPTY_STRING);
        }
        return replies;
    }

    /**
     * Gets a reply to an input. Assumes that the input has already had all
     * necessary substitutions and pre-processing performed, and that the input
     * is a single sentence.
     * 
     * @param input the input sentence
     * @param that the input that value
     * @param topic the input topic value
     * @param userid the userid requesting the reply
     * @param botid
     * @return the reply to the input sentence
     */
    private String getReply(String input, String that, String topic, String userid, String botid)
    {
        // Push the input onto the <input/> stack.
        this.predicateMaster.push(INPUT, input, userid, botid);

        // Create a new TemplateParser.
        TemplateParser parser;
        try
        {
            parser = new TemplateParser(input, userid, botid, this.core);
        }
        catch (TemplateParserException e)
        {
            throw new DeveloperError("Error occurred while creating new TemplateParser.", e);
        }

        String reply = getMatchResult(input, that, topic, userid, botid, parser);
        if (reply == null)
        {
            this.core.fail(new DeveloperError("getMatchReply generated a null reply!", new NullPointerException()));
        }

        // Push the reply onto the <that/> stack.
        this.predicateMaster.push(THAT, reply, userid, botid);

        return XMLKit.filterWhitespace(reply);
    }

    /**
     * Gets the match result from the Graphmaster.
     * 
     * @param input the input to match
     * @param that the current that value
     * @param topic the current topic value
     * @param userid the userid for whom to perform the match
     * @param botid the botid for whom to perform the match
     * @param parser the parser to use
     * @return the match result
     */
    private String getMatchResult(String input, String that, String topic, String userid, String botid, TemplateParser parser)
    {
        // Always show the input path (in any case, if match trace is on).
        if (this.recordMatchTrace)
        {
            this.matchLogger.log(Level.FINE, userid + PROMPT + input + SPACE + Graphmaster.PATH_SEPARATOR + SPACE + that + SPACE
                    + Graphmaster.PATH_SEPARATOR + SPACE + topic + SPACE + Graphmaster.PATH_SEPARATOR + SPACE + botid);
        }

        Match match = null;

        try
        {
            match = this.graphmaster.match(InputNormalizer.patternFitIgnoreCase(input), that, topic, botid);
        }
        catch (NoMatchException e)
        {
            this.logger.log(Level.INFO, e.getMessage());
            return EMPTY_STRING;
        }

        if (match == null)
        {
            this.logger.log(Level.INFO, "No match found for input \"" + input + "\".");
            return EMPTY_STRING;
        }

        if (this.recordMatchTrace)
        {
            this.matchLogger.log(Level.FINE, LABEL_MATCH + match.getPath());
            this.matchLogger.log(Level.FINE, LABEL_FILENAME + QUOTE_MARK + match.getFileName() + QUOTE_MARK);
        }

        ArrayList<String> stars = match.getInputStars();
        if (stars.size() > 0)
        {
            parser.setInputStars(stars);
        }

        stars = match.getThatStars();
        if (stars.size() > 0)
        {
            parser.setThatStars(stars);
        }

        stars = match.getTopicStars();
        if (stars.size() > 0)
        {
            parser.setTopicStars(stars);
        }

        String template = match.getTemplate();
        String reply = null;

        try
        {
            reply = parser.processResponse(template);
        }
        catch (ProcessorException e)
        {
            // Log the error message.
            Logger.getLogger("programd").log(Level.SEVERE, e.getExplanatoryMessage());

            // Set response to empty string.
            return EMPTY_STRING;
        }

        // Record activation, if targeting is in use.
        // Needs review in light of multi-bot update
        /*
         * if (USE_TARGETING) { Nodemapper matchNodemapper =
         * match.getNodemapper(); if (matchNodemapper == null) {
         * Trace.devinfo("Match nodemapper is null!"); } else { Set<Object>
         * activations = (Set<Object>)
         * matchNodemapper.get(Graphmaster.ACTIVATIONS); if (activations ==
         * null) { activations = new HashSet<Object>(); } String path =
         * match.getPath() + SPACE + Graphmaster.PATH_SEPARATOR + SPACE +
         * inputIgnoreCase + SPACE + Graphmaster.PATH_SEPARATOR + SPACE + that +
         * SPACE + Graphmaster.PATH_SEPARATOR + SPACE + topic + SPACE +
         * Graphmaster.PATH_SEPARATOR + SPACE + botid + SPACE +
         * Graphmaster.PATH_SEPARATOR + SPACE + reply; if
         * (!activations.contains(path)) { activations.add(path);
         * match.getNodemapper().put(Graphmaster.ACTIVATIONS, activations);
         * Graphmaster.activatedNode(match.getNodemapper()); } } }
         */
        return reply;
    }

    /**
     * Returns the average response time.
     * 
     * @return the average response time
     */
    public float averageResponseTime()
    {
        return this.avgResponseTime;
    }

    /**
     * Returns the number of queries per hour.
     * 
     * @return the number of queries per hour
     */
    public float queriesPerHour()
    {
        return this.responseCount / ((System.currentTimeMillis() - this.startTime) / 3600000.00f);
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
}