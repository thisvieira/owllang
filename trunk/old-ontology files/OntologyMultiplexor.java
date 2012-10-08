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
import java.util.Collection;
import java.util.Random;

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

//Libraries used to communicate with ontologies.
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.*;


/**
 * @since NEW
 * @version 4.5.ont
 * @author <a href="mailto:k.o.lundqvist@rdg.ac.uk">Karsten Oster Lundqvist</a>
 * 20/10/05 sis05kol: Build to make communication with ontology possible
 * 
 * Developed using
 * org.aitools.programd.multiplexor
 * @author <a href="mailto:noel@aitools.org">Noel Bush</a>
 * @author Richard Wallace, Jon Baer
 * @author Thomas Ringate/Pedro Colla
 */
abstract public class OntologyMultiplexor extends Multiplexor
{
	//Private variables of the Ontology
	private Project project;
	private KnowledgeBase knowledgeBase;
	private OntologySettings settings;
	
	// Convenience constants.

    /**Seperator in AIML*/
	protected static final String O_SEP = "$";
	
	/** The name of the Ontology Instance Refreshing Call in AIML
	 * This is called in the beginning of a sentence which needs ontology material in it
	 */
	protected static final String O_QUERY = O_SEP + "ONTQUE" + O_SEP;
	
    /** The name of the Ontology Instance Call in AIML. */
    protected static final String O_INSTANCE = "ONTINS" + O_SEP;

    /** The name of the THAT return call in AIML. */
    protected static final String O_THAT = "THATRETURN=";
    
    /**An unknown AIML input*/
    protected static final String O_UNKNOWN = "UNKNOWN";
    


     /**
     * Constructs the OntologyMultiplexor, using some values taken from the Core
     * object's settings. Note that the {@link #predicateMaster} is <i>not</i>
     * initialized -- it must be {@link #attach}ed subsequently.
     * 
     * @param coreOwner the Core that owns this Multiplexor
     */
    /**
     * @param coreOwner
     */
    public OntologyMultiplexor(Core coreOwner)
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
     * 
     * sis05kol: 	Added initilization of protege file
     * 				Find the entry point of the ontology for the chat
     */
    @SuppressWarnings("unchecked")
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
        
        //Initialize ontology settings
        settings = new OntologySettings(core.getSettings().getOntology());
        
    	//Initialize protege file       
    	String ontologyFileName = settings.getFileName();
    	File protegeFile;
    	try
    	{
    		protegeFile = new File(ontologyFileName);
    		Collection errors = new ArrayList();
    		project = new Project(protegeFile.getAbsolutePath(),errors);
    		if(errors.size()==0)
    		{
    			knowledgeBase = project.getKnowledgeBase();
    		}
    		else
    		{
    			Exception kb = new Exception("Errors in Ontology Project File");
    			throw kb;
    		}
    	}
    	catch(Exception e)
    	{
    		throw new UserError("Error in OntologyMultiplexor",e);
    	}
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
     * If reply contains any ontology requests these will be fetched from the ontology
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
        
        //There is an ontology query
        if (reply.contains(O_QUERY))
        {
        	String ontologyInput = InputNormalizer.patternFitIgnoreCase(getOntologyInput(reply));
        	String ontologyThat = getOntologyThat(reply);
        	
        	reply = getMatchResult(ontologyInput,ontologyThat, topic, userid, botid, parser);
        }
                
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
    
    @SuppressWarnings("unchecked")
	private String getOntologyInput(String query)
    {   	
    	StringBuffer res = new StringBuffer(O_QUERY);

    	int pos = query.indexOf(O_INSTANCE);
    	if(pos==-1) return O_UNKNOWN;
    	
    	do
    	{
        	res.append(O_INSTANCE);
    		
        	//Get subQuery
    		int endpos = query.indexOf(O_SEP, pos + O_INSTANCE.length());
    		
    		//String to be used to get query to Ontology
    		String subQuery;
    		
    		String testQuery = query.substring(pos,endpos);
    		//l.log(Level.INFO,testQuery);
    		
    		//Does subQuery contain reference to previous result
    		int numpos = testQuery.indexOf("#");
    		if (numpos != -1)
    		{
    			String dummyResult = testQuery.substring(0/*O_INSTANCE.length()*/,numpos);
    			do
    			{
    				String endSlip;
    			
    				int count = 0;
    				char dummy;
    				do
    				{
    					count++;
    					dummy = testQuery.charAt(numpos+count);
    					if(!Character.isDigit(dummy)) count--;
    				
    				} while (Character.isDigit(dummy) && numpos+count+1<testQuery.length());
    			
    				int endOfNumber = numpos+count+1;
    			
    				String interMediate = testQuery.substring(numpos+1,endOfNumber);
    				int num = Integer.parseInt(interMediate);
    			
    				int bufferNumPos = 0;
    				while (num>0)
    				{
    					bufferNumPos = res.indexOf(O_INSTANCE,bufferNumPos+1);
    					num--;
    				}
    			
    				bufferNumPos = bufferNumPos + O_INSTANCE.length();
    				int bufferEndNumPos = res.indexOf(O_SEP,bufferNumPos);
    			
    				int anotherHashPos = testQuery.indexOf("#",endOfNumber);
    				if(anotherHashPos==-1)
    					endSlip = "";
    				else
    					endSlip = testQuery.substring(endOfNumber);
    				
    				dummyResult = dummyResult + 
    				              res.substring(bufferNumPos,bufferEndNumPos) + 
    							  endSlip;
    				
    				numpos = testQuery.indexOf("#",endOfNumber);
    				
    				if(numpos!=-1)
    				{
    					int testpos = dummyResult.indexOf("#");
    					dummyResult = dummyResult.substring(0,testpos);
    				}
    			} while(numpos!=-1);
    				
    			subQuery = dummyResult;
    			//res.append(getOntologyInstance(testQuery));
    		}
    		else
    		{
    			subQuery = testQuery;
    		}
    		
    		//Collection of Instance descriptions not to be answers
    		Collection<String> notAnswers = null;
    		
    		//query to be sent to ontology
    		String finalQuery;
    		
    		pos = subQuery.indexOf("|");
    		
    		if (pos!=-1) 
    		{
    			notAnswers = new ArrayList();
    			
    			finalQuery = subQuery.substring(0,pos);
    			
        		while(pos!=-1)
        		{
        			int endposOfNot = subQuery.indexOf("|",pos+1);
        			
        			String notAns;
        			if (endposOfNot==-1)
        				notAns = subQuery.substring(pos+1);
        			else
        				notAns = subQuery.substring(pos+1,endposOfNot);
        			
        			notAnswers.add(notAns);
        			
        			pos = endposOfNot;    				
        		}
    		}
    		else
    		{
    			finalQuery = subQuery.substring(0);
    		}
    		
    		res.append(getOntologyInstance(finalQuery,notAnswers));
    		res.append("$");
    		
    		pos = query.indexOf(O_INSTANCE,endpos);
    	} while (pos!=-1);
    	
    	

    	return res.toString();// + "ONTINS INTERESTING ONTINS RELEVANT ONTINS INTERACTIVE";
    }
    
    private String getOntologyThat(String query)
    {
    	int pos = query.indexOf(O_THAT);
    	if(pos==-1)
    		return O_UNKNOWN;
    	else
    		return query.substring(pos+O_THAT.length());
    }
    
    @SuppressWarnings("unchecked")
	private String getOntologyInstance(String query, Collection<String> notAnswer)
    {
    	//Find cls type of the instance needed
    	int pos = query.indexOf(":");
    	String clsName = query.substring(O_INSTANCE.length(),pos);
    	Cls cls = knowledgeBase.getCls(clsName);
    	
    	//Unknown class
    	if(cls == null)	return O_UNKNOWN;
    	
    	//Get all subclasses
    	Collection<Cls> Clses = new ArrayList();
    	if(cls.getDirectSubclassCount()>0)
    		Clses.addAll(cls.getSubclasses());
    	else
    		Clses.add(cls);
    	
    	
    	//Find all instances of the possible clses.
    	Collection<Instance> possibleInstances = new ArrayList();
    	for (Cls c : Clses) possibleInstances.addAll(c.getInstances());
    	
    	//Get all predicates in the query
    	Collection<String> predicates = new ArrayList();
    	do
    	{
    		pos++;
    		
    		String predString;
    		int predEnd = query.indexOf("@",pos);
    		
    		if (predEnd==-1)
    		{
    			predString = query.substring(pos);
    		}
    		else
    		{
    			predString = query.substring(pos,predEnd);
    		}
    		
    		predicates.add(predString);
    		
    		pos = predEnd;		
    	} while(pos!=-1);
    	
    	//Find possible instances of the correct type   	
    	Collection<Instance> succInstances = new ArrayList();
    	for (Instance i: possibleInstances)
    	{    		
    		boolean contender = true;
    		
    		//Check if instance satisfies predicate
    		for(String pred: predicates)
    		{		
    			//Predicate name
    			String predName = pred.substring(0,pred.indexOf("="));
    			
    			//Predicate value
    			String predValue = pred.substring(pred.indexOf("=")+1);
    			boolean predTrue = false;
    			
    			//Check list of all slots in the instance
    			Collection<Instance> slotVal = i.getOwnSlotValues(knowledgeBase.getSlot(predName));
    			for(Instance ins :slotVal)
    			{
    				String testString = (String)ins.getOwnSlotValue(knowledgeBase.getSlot("description"));
    				if (testString.equalsIgnoreCase(predValue)) 
    				{
    					predTrue = true;
    					break;
    				}
    			}
  			
    			//instance did not satisfy the predicate
    			if(!predTrue) 
    			{
    				contender = false;
    				break;
    			}
    		}
    		
    		//It satisfies all predicates
    		if (contender) succInstances.add(i);
    	}
    	
    	//Find a winner among the instances which satisfies the predicates
    	if (succInstances.size()>0)
    	{
    		Long seed = System.nanoTime();
    		
    		Random rand = new Random(seed);
    		
    		String winner = null;;
    		Object [] res = succInstances.toArray();
    		
    		if (notAnswer!=null)
    		{
    			if(succInstances.size()==1)
    			{
    				winner = O_UNKNOWN;
    				return winner;
    			}
    			
    			do
    			{
    				String dummy = (String)((Instance)res[rand.nextInt(res.length)]).getOwnSlotValue(knowledgeBase.getSlot("description"));
    				
    				if(!notAnswer.contains(dummy))
    				{
    					winner = dummy;
    					break;
    				}
    			} while (winner==null);
    		}
    		else	
    			winner = (String)((Instance)res[rand.nextInt(res.length)]).getOwnSlotValue(knowledgeBase.getSlot("description"));
    		
    		return winner;
    	}
    	else
    		return O_UNKNOWN;
    
    }
}