/**
 * Copyright (C) 2016  Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 * 
 * This program is free software: you can redistribute it and/or modify it under 
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation, either version 3 of the License, or (at your option) any later 
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package saffranexperiment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import jchrest.architecture.Chrest;
import jchrest.architecture.Node;
import jchrest.lib.ListPattern;
import jchrest.lib.Modality;
import jchrest.lib.Pattern;
import jchrest.lib.PrimitivePattern;

/**
 * Represents a single experiment used by Saffran, Aslin and Newport in their
 * 1996 study: "Statistical Learning by 8-Month Old Infants."
 * 
 * REF: Science; Dec 13, 1996; Vol. 274, Issue 5294, pp. 1926-1928
 * DOI: 10.1126/science.274.5294.1926
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class Experiment {
  
  private int _experimentClock = 0; //Used by CHREST.
  private final Chrest _participant;
  private int _syllablesUttered = 0;
  private final String[] _learningPhaseWords;
  private final String[] _testPhaseWords;
  
  private final ArrayList<String> _phonologicalStore = new ArrayList();
  private final int _phonologicalStoreTraceDecayValue;
  private List<Integer> _phonologicalStoreTraceDecayTimes = new ArrayList();
  
  /**
   * Constructor.
   * 
   * @param learningPhaseWords
   * @param testPhaseWords
   * @param phonologicalStoreTraceDecayValue
   * @param participant 
   */
  public Experiment(String[] learningPhaseWords, String[] testPhaseWords, int phonologicalStoreTraceDecayValue, Chrest participant){
    this._learningPhaseWords = learningPhaseWords;
    this._testPhaseWords = testPhaseWords;
    this._phonologicalStoreTraceDecayValue = phonologicalStoreTraceDecayValue;
    this._participant = participant;
  }
  
  /**
   * @return A stream of words constructed from the set of learning phase words
   * in a random order.  The stream generated conforms to the following 
   * requirements:
   * 
   * <ul>
   *  <li>Stream is composed of 45 words.</li>
   *  <li>
    No experimentWord occurs twice in succession.  This also means that the first and 
    last experimentWord of the stream is not the same since the stream is repeated 
    during the learning phase.
  </li>
   * </ul>
   */
  private String generateLearningPhaseWordStream(){
    String learningPhaseWordStream = "";
    Random rand = new Random();
    String firstWord = "";
    String prevWordAdded = "";
    
    for(int wordCount = 0; wordCount < 45; wordCount++){
      String stringToAdd = this._learningPhaseWords[(rand.nextInt(this._learningPhaseWords.length))];
      if(wordCount == 0) firstWord = stringToAdd;
      
      while(
        stringToAdd.equals(prevWordAdded) || 
        (wordCount == 44 && stringToAdd.equals(firstWord)) 
      ){
        stringToAdd = this._learningPhaseWords[(rand.nextInt(this._learningPhaseWords.length))];
      }
      
      learningPhaseWordStream += stringToAdd;
      prevWordAdded = stringToAdd;
    }
    
    return learningPhaseWordStream;
  }
  
  /**
   * Places the experimentWordSyllable specified into the phonological store of the 
 participant taking part in the experiment, adds a new trace decay time to 
 the relevant data structure and increments the experimentWordSyllable utterance counter.
   * 
   * @param syllable
   * @param time 
   */
  private void utterSyllable(String syllable, int time){
    this._phonologicalStore.add(syllable);
    this._phonologicalStoreTraceDecayTimes.add(time + this._phonologicalStoreTraceDecayValue);
    this._syllablesUttered++;
  }
  
  /**
   * Constructs a new {@link jchrest.lib.Modality#VERBAL} {@link 
   * jchrest.lib.ListPattern} by iterating through the participant's 
 phonological store and adding experimentWordSyllables in chronological order to the
 {@link jchrest.lib.ListPattern} (oldest experimentWordSyllable added first).
   * 
   * When the {@link jchrest.lib.ListPattern} is constructed, the participant
   * is asked to (recognise) and learn it at the time specified.
   * 
   * @param time 
   */
  private void learnPhonologicalStoreContent(int time){
    ListPattern listPatternToLearn = new ListPattern(Modality.VERBAL);
    
    for(int i = 0; i < this._phonologicalStore.size(); i++){
      listPatternToLearn.add(PrimitivePattern.makeString(this._phonologicalStore.get(i)));
    }
    
    if(!listPatternToLearn.isEmpty()){
      this._participant.recogniseAndLearn(listPatternToLearn, time);
    }
  }
  
  /**
   * Performs an experiment with a participant.
   * 
   * Each experiment is constructed of a learning phase and a testing phase.
   * However, before the learning phase begins, the participant's LTM is 
   * initialised with all the syllables that will be encountered.  This is in 
   * accordance with an experimental assumption that the participants in Saffran 
   * et al.'s study were familiar with all the syllables used.  This is assumed 
   * because the syllables used are taken from the participant's native 
   * language and the participant <i>must</i> have heard some/all of the 
   * syllables before and have committed some to LTM.  The alternative is to 
   * leave the participant's LTM empty, which is implausible.
   * 
   * During the learning phase, the participant is "played" a randomly generated
   * stream of words for 2 minutes constructed from the set of learning phase 
   * words specified when the experiment is constructed.  Hence, a syllable is
   * placed into the participant's phonological store every 222ms and the 
   * participant attempts to learn the contents of its phonological store every
   * millisecond (if the phonological store is empty, the participant does 
   * nothing).  When the learning phase ends, the participant's S/LTM operations 
   * are allowed to complete and the experiment continues immediately afterwards 
   * by starting the testing phase.
   * 
   * During the testing phase, a word from the set of test phase words is 
   * "played" for 15 seconds or until the participant recognises the word before
   * 15 seconds elapses.  Hence, a syllable is placed into the participant's 
   * phonological store every 222ms and after a word (3 syllables) has been 
   * played, a 500ms break occurs.  The participant attempts to recognise the 
   * test word being played every millisecond, if the test word is not 
   * recognised then the participant attempts to learn the contents of its
   * phonological store. If the participant's phonological store is empty when 
   * it attempts to learn, the participant does nothing.  Note: test word 
   * presentation order adheres to the order that test words are specified when
   * {@link #this} is constructed.
   * 
   * Before each test word is played in the testing phase, the participant's 
   * phonological store is cleared (an experimental assumption: prevents the 
   * presentation of a previous word in the learning/testing phase affecting the 
   * recognition of the subsequent word in the testing phase).
   * 
   * @return The time that each test word is presented for mapped against the
   * relevant test word.
   */
  public HashMap perform(){
    
    ////////////////////////////////////////////////////////////////////////////
    // NOTE: All times are measured in milliseconds since this is the unit of //
    //       time used by CHREST.                                             //
    ////////////////////////////////////////////////////////////////////////////
    
    /************************/
    /**** INITIALISE LTM ****/
    /************************/
    
    ArrayList<String> allExperimentWords = new ArrayList();
    for(String familiarisationWord : this._learningPhaseWords){
      allExperimentWords.add(familiarisationWord);
    }
    
    for(String testWord : this._testPhaseWords){
      allExperimentWords.add(testWord);
    }
    
    for(String experimentWord : allExperimentWords){
      String [] experimentWordSyllables = new String[3];
      experimentWordSyllables[0] = experimentWord.substring(0, 2);
      experimentWordSyllables[1] = experimentWord.substring(2, 4);
      experimentWordSyllables[2] = experimentWord.substring(4);
      
      for(String experimentWordSyllable : experimentWordSyllables){
        ListPattern experimentWordSyllableAsListPattern = new ListPattern(Modality.VERBAL);
        experimentWordSyllableAsListPattern.add(Pattern.makeString(experimentWordSyllable));
        boolean learnExperimentWordSyllable = true;
        
        while(learnExperimentWordSyllable){
          this._participant.recogniseAndLearn(experimentWordSyllableAsListPattern, this._experimentClock);

          for(Node stmNode : this._participant.getStm(Modality.VERBAL).getContents(_experimentClock)){
            ListPattern chunk = stmNode.getImage(_experimentClock);
            if(chunk.equals(experimentWordSyllableAsListPattern)){
              learnExperimentWordSyllable = false;
            }
          }
          this._experimentClock++;
        }
      }
    }
    
    this._experimentClock = Math.max(this._participant.getAttentionClock(), this._participant.getCognitionClock());
    
    /************************/
    /**** LEARNING PHASE ****/
    /************************/
    
    //Set-up a learning phase clock since a syllable needs to be "uttered" 
    //every 222ms precisely. Using the experiment clock makes this calculation 
    //difficult.
    //
    //Initialse the learning phase clock to 1ms so that a syllable is only 
    //"uttered" for the first time after 222ms.  If the clock is set to 0ms then 
    //the first syllable will be "uttered" at time 0, which is incorrect.
    int learningPhaseClock = 1;
    
    String learningPhaseWordStream = this.generateLearningPhaseWordStream();
        
    //Used for chopping up the learningPhaseWordStream into syllables.
    int learningPhaseWordStreamIndex = 0;
    
    //Present learningPhaseWordStream for 2 minutes (120000ms).
    while(learningPhaseClock <= 120000){
      
      //Apply phonological store trace decay, if applicable.
      if(
        !this._phonologicalStoreTraceDecayTimes.isEmpty() && 
        (this._experimentClock + learningPhaseClock) == this._phonologicalStoreTraceDecayTimes.get(0)
      ){
        this._phonologicalStore.remove(0);
        this._phonologicalStoreTraceDecayTimes.remove(0);
      }

      //Every 222ms, "utter" the next syllable from the learningPhaseWordStream.
      if( (learningPhaseClock % 222) == 0){

        //Every syllable is two characters long so, from the current 
        //learningPhaseWordStreamIndex, add 2.  An IndexOutOfBoundsException 
        //will be thrown on the final syllable so code against this.
        String syllable = (learningPhaseWordStreamIndex + 2) >= learningPhaseWordStream.length() ?
          learningPhaseWordStream.substring(learningPhaseWordStreamIndex) :
          learningPhaseWordStream.substring(learningPhaseWordStreamIndex, learningPhaseWordStreamIndex + 2)
        ;
        
        //Reset learningPhaseWordStreamIndex if its greater than the length of 
        //learningPhaseWordStream, otherwise, increment it by 2 for the next
        //syllable.
        if(learningPhaseWordStreamIndex >= learningPhaseWordStream.length()){
          learningPhaseWordStreamIndex = 0;
        } 
        else {
          learningPhaseWordStreamIndex += 2;
        }
        
        //Utter the current syllable.
        this.utterSyllable(syllable, this._experimentClock + learningPhaseClock);
      }

      //Ask the participant to try and learn the current contents of its
      //phonological store.
      this.learnPhonologicalStoreContent(this._experimentClock + learningPhaseClock);
      
      //Increment the learningPhaseClock by 1ms.
      learningPhaseClock++;
    }
    
    /**********************************************************/
    /**** ALLOW PARTICIPANT'S S/LTM OPERATIONS TO COMPLETE ****/
    /**********************************************************/
    
    this._experimentClock = Math.max(this._participant.getAttentionClock(), this._participant.getCognitionClock());
    
    /********************/
    /**** TEST PHASE ****/
    /********************/
    
    //Create a data structure to store the test word -> presentation time 
    //mappings.
    HashMap<String, Double> testWordPresentationTimes = new HashMap();
    
    for(int i = 0; i < this._testPhaseWords.length; i++){ 
      
      //Get the next test word and clear the participant's phonological store
      //data.
      String testWord = this._testPhaseWords[i];
      this._phonologicalStore.clear();
      this._phonologicalStoreTraceDecayTimes = new ArrayList();
      
      //Create a ListPattern version of the test word so that it can be 
      //determined when the participant recognises the test word (recogniition 
      //in CHREST involves checking the image of a Node object in verbal STM and
      //a Node image is a ListPattern, not a String).
      ListPattern testWordAsListPattern = new ListPattern(Modality.VERBAL);
      testWordAsListPattern.add(Pattern.makeString(testWord.substring(0, 2)));
      testWordAsListPattern.add(Pattern.makeString(testWord.substring(2, 4)));
      testWordAsListPattern.add(Pattern.makeString(testWord.substring(4)));
      
      //Set-up a counter for the number of syllables uttered for this test word.
      //This will be used to determine when the whole test word has been 
      //"uttered" and therefore, when the 500ms break between test word 
      //utterances should occur.
      this._syllablesUttered = 0;
      
      //Set-up a variable to track when syllables should be uttered since the 
      //500ms break complicates matters.
      int syllableUtteranceTime = 222;
      
      //Used for chopping up the test word into syllables.
      int testWordSyllableIndex = 0;
      
      //Set-up presentation end condition variables.  
      int presentationTime = 0;
      boolean testWordRecognised = false;
      
      //Present the test word for 15s (15000ms) or until the test word is
      //recognised.
      while(presentationTime < 15000 && !testWordRecognised){
        
        //Apply phonological store trace decay, if applicable.
        if(
          !this._phonologicalStoreTraceDecayTimes.isEmpty() && 
          (this._experimentClock + presentationTime) == this._phonologicalStoreTraceDecayTimes.get(0)
          ){
          this._phonologicalStore.remove(0);
          this._phonologicalStoreTraceDecayTimes.remove(0);
        }
      
        //"Utter" the next syllable from the test word, if applicable.
        if( presentationTime == syllableUtteranceTime){

          //Every syllable is two characters long so, from the current 
          //testWordSyllableIndex, add 2.  An IndexOutOfBoundsException will be 
          //thrown on the final syllable so code against this.
          String syllable = ( (testWordSyllableIndex + 2) >= testWord.length() ?
            testWord.substring(testWordSyllableIndex) :
            testWord.substring(testWordSyllableIndex, testWordSyllableIndex + 2)
          );
          
          //"Utter" the syllable.
          this.utterSyllable(syllable, this._experimentClock + presentationTime);

          //Reset the experimentWordSyllable begin index if its greater than the length of the
          //test experimentWord.
          if(testWordSyllableIndex >= testWord.length()){
            testWordSyllableIndex = 0;
          }
          else{
            testWordSyllableIndex += 2;
          }
          
          //If a word has now been uttered, pause for 500ms and reset the 
          //syllable utterance counter.  Otherwise, schedule the next syllable 
          //to be uttered 222ms from the current presentation time. 
          if(this._syllablesUttered == 3){
            syllableUtteranceTime += 500;
            this._syllablesUttered = 0;
          } 
          else{
            syllableUtteranceTime = presentationTime + 222;
          }
        }

        //Check if the test word has been recognised.
        for(Node stmNode : this._participant.getStm(Modality.VERBAL).getContents(this._experimentClock + presentationTime)){
          ListPattern chunk = stmNode.getImage(this._experimentClock + presentationTime);
          if(chunk.equals(testWordAsListPattern)){
            testWordRecognised = true;
          }
        }
        
        //If the test word hasn't been recognised, ask the participant to learn
        //its phonological store content and increment the presentation time
        //timer by 1ms.
        if(!testWordRecognised){
          this.learnPhonologicalStoreContent(_experimentClock + presentationTime);
          presentationTime++;
        }
      }
      
      //Convert the presentation time from milliseconds to seconds since this is 
      //the unit of time used by Saffran et al. in their study and map the 
      //result to the test word in the data structure created at the beginning
      //of the test phase.
      testWordPresentationTimes.put(testWord, (double)presentationTime/1000);
      
      //Present the next test word after the participant's S/LTM is free or 
      //after the presnetation time, whichever value is largest.
      this._experimentClock = Math.max(
        presentationTime, 
        Math.max(
          this._participant.getAttentionClock(), 
          this._participant.getCognitionClock()
        )
      );
    }
    
    return testWordPresentationTimes;
  }  
}
