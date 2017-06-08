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
import jchrest.architecture.Chrest;
import jchrest.lib.GenericDomain;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.commons.math3.stat.regression.SimpleRegression;

/**
 * Runs the simulations designed to replicate Saffran, Aslin and Newport's
 * 1996 study: "Statistical Learning by 8-Month Old Infants."
 * 
 * REF: Science; Dec 13, 1996; Vol. 274, Issue 5294, pp. 1926-1928
 * DOI: 10.1126/science.274.5294.1926
 * 
 * @author Martyn Lloyd-Kelly <martynlk@liverpool.ac.uk>
 */
public class Main {
  
  private static int _totalParticipantTypes;
  private final static int _totalRepeats = 50;
  private final static int _totalParticipants = 24;
  private final static int[] phonologicalStoreTraceDecayTimes = new int[]{600, 800, 1000};
  private final static int[] discriminationTimes = new int[]{8000, 9000, 10000};
  private final static int[] familiarisationTimes = new int[]{1000, 1500, 2000};
  
  /**
   * Runs Saffran et al.'s {@link saffranexperiment.Experiment}s according to 
   * the following simulation structure:
   * 
   * <ul>
   *    <li>Participant type (1-27)</li>
   *    <ul>
   *      <li>Repeat (1-50)</li>
   *        <ul>
   *          <li>Experiment (1-2)</li>
   *            <ul>
   *              <li>Participant (1-24)</li>
   *            </ul>
   *        </ul>
   *    </ul>
   * </ul>
   * 
   * The participant type denotes a unique combination of values for the
   * phonological store trace decay variable, the discrimination time variable
   * and the familiarisation time variable (the last two variables are CHREST 
   * parameters).  Since each variable has 3 values 3 * 3 * 3 = 27.
   * 
   * The progress of the simulation in real-time will be output to {@link 
   * java.lang.System#out} along with the simulation results as CSV-formatted 
   * strings.  Results are calculated and displayed in the following order:
   * 
   * <ol type="1">
   *  <li>
   *    For each experiment repeat, the presentation times for familiar and 
   *    novel words achieved by each participant are aggregated together into
   *    two data sets: familiar word presentation times and novel word 
   *    presentation times.  The mean and standard deviation for each data set
   *    are then calculated along with t and p values by taking all data in 
   *    these sets into consideration.
   *  </li>
   *  <li>
   *    The mean familiar and novel word presentation time and standard error 
   *    thereof for each participant type.  These values are calculated from 
   *    the average familiar/novel word values for each repeat (calculated 
   *    above).
   *  </li>
   *  <li>
   *    The fit of the model (r<sup>2</sup> and RMSE) for each participant 
   *    type's experiment repeat.  These values are calculated by comparing the 
   *    mean presentation times for familiar and novel words in each participant
   *    type's experiment repeat against the mean familiar and novel word 
   *    listening times in each of the experiments of Saffran et al.'s study.  
   *    Therefore, 8 data points are used per r<sup>2</sup> and 
   *    RMSE calculation: 4 from the simulation (average presentation time for 
   *    familiar words in experiments 1 and 2 and average presentation time for
   *    novel words in experiments 1 and 2) and the 4 average listening times 
   *    reported by Saffran et al. (average listening times for familiar words 
   *    in experiments 1 and 2 and average listening times for novel words in 
   *    experiments 1 and 2).
   *  </li>
   *  <li>
   *    The mean (r<sup>2</sup> and RMSE) values for each participant type.
   *    Calculated by taking the mean r<sup>2</sup> and RMSE values for each
   *    repeat (calculated above) and taking their mean.
   *  </li>
   *  <li>
   *    The presentation times achieved by each participant over the entire
   *    simulation (the data from which all other results are calculated).
   *  </li>
   * </ol>
   * 
   * CSV strings always start with 3 "columns" that denote the trace decay time,
   * discrimination time and familiarisation time used by the participants in
   * the experiments.
   * 
   * @param args Not used so not applicable.
   */
  public static void main(String[] args) {
    
    String participantDataCsv =
      "trace_decay_t," +
      "disc_t," +
      "famil_t," +
      "repeat," + 
      "expt," + 
      "prt," +
      "fml_wrd_1_t," +
      "fml_wrd_2_t," +
      "nvl_wrd_1_t," +
      "nvl_wrd_2_t," +
      "\n"
    ;
    
    //Set the total parameter settings variable now.
    _totalParticipantTypes = phonologicalStoreTraceDecayTimes.length * discriminationTimes.length * familiarisationTimes.length;
    
    /**********************************************/
    /**** PERFORM EXPERIMENTS AND COLLECT DATA ****/
    /**********************************************/
    
    double[][][][][] simulationData = new double[_totalParticipantTypes][][][][];
    for(int participantType = 0; participantType < _totalParticipantTypes; participantType++){
      System.out.println("Participant type " + (participantType + 1));
      
      int traceDecayTime = phonologicalStoreTraceDecayTimes[participantType / 9];
      int discriminationTime = discriminationTimes[(participantType/3) % 3];
      int familiarisationTime = familiarisationTimes[participantType % 3];
    
      double[][][][] repeatData = new double[_totalRepeats][][][];
      for(int repeat = 0; repeat < _totalRepeats; repeat++){
        System.out.println("   Repeat " + (repeat + 1));

        double[][][] experimentData = new double[2][][]; 
        for(int experimentNumber = 0; experimentNumber < 2; experimentNumber++){ 
          System.out.println("      Experiment " + (experimentNumber + 1));
          
          //Set test phase words.  Test phase words for experiment 1 are 
          //different to experiment 2's.
          String[] testPhaseWords = (
            experimentNumber == 0 ?
              new String[]{"tupiro", "golabu", "dapiku", "tilado"} :
              new String[]{"pabiku", "tibudo", "tudaro", "pigola"}
          );

          double[][] participantsData = new double[_totalParticipants][];
          System.out.println("         Participant ");
          for(int participantNumber = 0; participantNumber < _totalParticipants; participantNumber++){
            System.out.append( (participantNumber == 0 ? "            " : "") + (participantNumber + 1) + (participantNumber + 1 == _totalParticipants ? "\n" : ","));
            
            double[] participantData = new double[8];

            //Set learning phase words.  For each experiment, half of the 
            //participants should be in condition A, the other in condition B 
            //and the learning phase words differ between conditions.
            String[] learningPhaseWords = (
              experimentNumber == 0 ?
                participantNumber < _totalParticipants/2 ? 
                  new String[]{"tupiro", "golabu", "bidaku", "padoti"} :
                  new String[]{"dapiku", "tilado", "burobi", "pagotu"}
                : //Experiment 2
                participantNumber < _totalParticipants/2 ?
                  new String[]{"pabiku", "tibudo", "golatu", "daropi"} :
                  new String[]{"tudaro", "pigola", "bikuti", "budopa"}
            );

            //Create the experiment.
            Chrest participant = new Chrest(0, GenericDomain.class);
            participant.setDiscriminationTime(discriminationTime);
            participant.setFamiliarisationTime(familiarisationTime);
            Experiment experiment = new Experiment(learningPhaseWords, testPhaseWords, traceDecayTime, participant); 

            //Perform the experiment.
            HashMap<String, Double> experimentResults = experiment.perform();

            //In both experiments 1 and 2, the familiar words for condition A 
            //participants are the first two test strings whilst the novel words 
            //are the last two strings whilst the familiar words for codition
            //B participants are the last two test strings and the novel words
            //are the first two test strings.
            participantData[0] = experimentResults.get(testPhaseWords[participantNumber <= 11 ? 0 : 2]); //First familiar word rec time
            participantData[1] = experimentResults.get(testPhaseWords[participantNumber <= 11 ? 1 : 3]); //Second familiar word rec time
            participantData[2] = experimentResults.get(testPhaseWords[participantNumber <= 11 ? 2 : 0]); //First novel word rec time
            participantData[3] = experimentResults.get(testPhaseWords[participantNumber <= 11 ? 3 : 1]); //Second novel word rec time

            participantsData[participantNumber] = participantData;
            participantDataCsv +=
              traceDecayTime + "," +
              discriminationTime + "," +
              familiarisationTime + "," +
              (repeat + 1) + "," + 
              (experimentNumber + 1) + "," + 
              (participantNumber + 1) + "," +
              participantData[0] + "," +
              participantData[1] + "," +
              participantData[2] + "," +
              participantData[3] + "\n"
            ;
          }
          
          experimentData[experimentNumber] = participantsData;
        }
        
        repeatData[repeat] = experimentData;
      }
      
      simulationData[participantType] = repeatData;
    }
    
    /***************************/
    /**** CALCULATE RESULTS ****/
    /***************************/
    
    double[][][][] presentationTimeRepeatMetrics = calculateMeanSdTAndPValuesForEachExperimentRepeat(simulationData);
    double[][][] presentationTimeAvgAndSeForFamilAndNovelWordsInEachExperiment = calculateMeanAndStandardErrorOfMeanFamiliarAndNovelWordPresentationTimesForEachExperiment(presentationTimeRepeatMetrics);
    double[][][] presentationTimeRepeatModelFits = calculateRSquareAndRmseForEachRepeat(presentationTimeRepeatMetrics);
    double[][] presentationTimeAverageModelFits = calculateMeanRsquareAndRmseForEachParticipantType(presentationTimeRepeatModelFits);
    
    String presentationTimeRepeatMetricsCsv = "trace_decay_t,disc_t,famil_t,repeat,expt,fml_wrd_t_mean,fml_wrd_t_sd,nvl_wrd_t_mean,nvl_wrd_t_sd,t_value,p_value\n";
    String presentationTimeAvgAndSeForFamilAndNovelWordsInEachExperimentCsv = "trace_decay_t,disc_t,famil_t,expt,fml_wrd_avg,fml_wrd_se,nvl_wrd_avg_nvl_wrd_se\n";
    String presentationTimeRepeatModelFitsCsv = "trace_decay_t,disc_t,famil_t,repeat,r_2,rmse\n";
    String presentationTimeAvgModelFitsCsv = "trace_decay_t,disc_t,famil_t,avg_r_2,avg_rmse\n";
    
    for(int participantType = 0; participantType < _totalParticipantTypes; participantType++){
      
      int traceDecayTime = phonologicalStoreTraceDecayTimes[participantType / 9];
      int discriminationTime = discriminationTimes[(participantType/3) % 3];
      int familiarisationTime = familiarisationTimes[participantType % 3];
        
      for(int repeat = 0; repeat < _totalRepeats; repeat++){
        for(int experiment = 0; experiment < 2; experiment++){
          
          presentationTimeRepeatMetricsCsv += 
            traceDecayTime + "," +
            discriminationTime + "," +
            familiarisationTime+ "," +
            (repeat + 1) + "," + 
            (experiment + 1) + ","
          ;
          
          for(int metric = 0; metric < presentationTimeRepeatMetrics[participantType][repeat][experiment].length; metric++){
            presentationTimeRepeatMetricsCsv += presentationTimeRepeatMetrics[participantType][repeat][experiment][metric] + 
              (metric == (presentationTimeRepeatMetrics[participantType][repeat][experiment].length - 1) ? "\n" : ",")
            ;
          }
        }
        
        presentationTimeRepeatModelFitsCsv += 
          traceDecayTime + "," +
          discriminationTime + "," +
          familiarisationTime + "," +
          (repeat + 1) + "," + 
          presentationTimeRepeatModelFits[participantType][repeat][0] + "," + 
          presentationTimeRepeatModelFits[participantType][repeat][1] + "\n";
      }
      
      for(int experiment = 0; experiment < 2; experiment++){
        presentationTimeAvgAndSeForFamilAndNovelWordsInEachExperimentCsv += 
          traceDecayTime + "," +
          discriminationTime + "," +
          familiarisationTime + "," +
          experiment + "," +
          presentationTimeAvgAndSeForFamilAndNovelWordsInEachExperiment[participantType][experiment][0] + "," +
          presentationTimeAvgAndSeForFamilAndNovelWordsInEachExperiment[participantType][experiment][1] + "," +
          presentationTimeAvgAndSeForFamilAndNovelWordsInEachExperiment[participantType][experiment][2] + "," +
          presentationTimeAvgAndSeForFamilAndNovelWordsInEachExperiment[participantType][experiment][3] + "\n"
        ;
      }
      
      presentationTimeAvgModelFitsCsv +=
        traceDecayTime + "," +
        discriminationTime + "," +
        familiarisationTime + "," +
        presentationTimeAverageModelFits[participantType][0] + "," +
        presentationTimeAverageModelFits[participantType][1] + "\n"
      ;
    }
    
    System.out.println(presentationTimeRepeatMetricsCsv);
    System.out.println();
    System.out.println(presentationTimeAvgAndSeForFamilAndNovelWordsInEachExperimentCsv);
    System.out.println();
    System.out.println(presentationTimeRepeatModelFitsCsv);
    System.out.println();
    System.out.println(presentationTimeAvgModelFitsCsv);
    System.out.println();
    System.out.println(participantDataCsv);
  }
  
  /**
   * @param simulationData A 5D {@link java.util.Arrays Array} with the 
   * following structure:
   * 
   * <ul>
   *  <li>First dimension: participant types</li>
   *  <li>Second dimension: repeats</li>
   *  <li>Third dimension: experiments</li>
   *  <li>Fourth dimension: participants</li>
   *  <li>Fifth dimension:</li>
   *    <ol type="1">
   *      <li>Familiar word 1 presentation time</li>
   *      <li>Familiar word 2 presentation time</li>
   *      <li>Novel word 1 presentation time</li>
   *      <li>Novel word 2 presentation time</li>
   *    </ol>
   * </ul>
   * 
   * For example, simulationData[7][8][1][17][2] should return the presentation
   * time for the first novel word achieved by participant 18 in the 9th repeat
   * of experiment 2 when participant type is set to 8.
   * 
   * @return A 4D {@link java.util.Arrays Array} with the following structure:
   * 
   * <ul>
   *  <li>First dimension: participant types</li>
   *  <li>Second dimension: repeats</li>
   *  <li>Third dimension: experiments</li>
   *  <li>Fourth dimension:</li> 
   *    <ol type="1">
   *      <li>Familiar word presentation time mean</li>
   *      <li>Familiar word presentation time standard deviation</li>
   *      <li>Novel word presentation time mean</li>
   *      <li>Novel word presentation time standard deviation</li>
   *      <li>t value for familiar/novel word presentation time means</li>
   *      <li>p value for familiar/novel word presentation time means</li>
   *    </ol>
   * </ul>
   * 
   * For example, assigning the result to a variable j and invoking 
   * j[5][3][1][4] would return the t-value associated with the second 
   * experiment of the fourth repeat for participant type 6.
   */
  private static double[][][][] calculateMeanSdTAndPValuesForEachExperimentRepeat(double[][][][][] simulationData){
    
    double[][][][] values = new double[_totalParticipantTypes][_totalRepeats][2][6];
    
    for(int participantType = 0; participantType < _totalParticipantTypes; participantType++){
      for(int repeat = 0; repeat < _totalRepeats; repeat++){
        for(int experiment = 0; experiment < 2; experiment++){
        
          List<Double> familWordPresentationTimes = new ArrayList();
          List<Double> novelWordPresentationTimes = new ArrayList();

          for(int participant = 0; participant < _totalParticipants; participant++){
            familWordPresentationTimes.add(simulationData[participantType][repeat][experiment][participant][0]);
            familWordPresentationTimes.add(simulationData[participantType][repeat][experiment][participant][1]);
            novelWordPresentationTimes.add(simulationData[participantType][repeat][experiment][participant][2]);
            novelWordPresentationTimes.add(simulationData[participantType][repeat][experiment][participant][3]);
          }

          //Calculate familiar word presentation time mean for experiment.
          DescriptiveStatistics familWordDescStat = new DescriptiveStatistics();
          familWordPresentationTimes.forEach((value) -> familWordDescStat.addValue(value));
          values[participantType][repeat][experiment][0] = familWordDescStat.getMean();
          values[participantType][repeat][experiment][1] = familWordDescStat.getStandardDeviation();

          //Calculate novel word presentation time mean for experiment.
          DescriptiveStatistics novelWordDescStat = new DescriptiveStatistics();
          novelWordPresentationTimes.forEach((value) -> novelWordDescStat.addValue(value));
          values[participantType][repeat][experiment][2] = novelWordDescStat.getMean();
          values[participantType][repeat][experiment][3] = novelWordDescStat.getStandardDeviation();

          //Convert lists containing familiar and novel presentation times to 
          //arrays so we can use the Apache stats library functions for 
          //calculating t and p values. 
          double[] familWordPresentationTimesArr = convertDoubleListToDoubleArray(familWordPresentationTimes);
          double[] novelWordPresentationTimesArr = convertDoubleListToDoubleArray(novelWordPresentationTimes);

          //Calculate t value between familiar and novel word presentation times.
          values[participantType][repeat][0][4] = Math.abs(TestUtils.pairedT(
            familWordPresentationTimesArr, 
            novelWordPresentationTimesArr
          ));

          //Calculate p value between familiar and novel word presentation times.
          values[participantType][repeat][0][5] = TestUtils.pairedTTest(
            familWordPresentationTimesArr, 
            novelWordPresentationTimesArr
          );
        }
      }
    }
    
    return values;
  }
  
  /**
   * @param data A 4D {@link java.util.Arrays Array} with the following 
   * structure (can be obtained from {@link 
   * #calculateMeanSdTAndPValuesForEachExperimentRepeat(double[][][][][])}:
   * 
   * <ul>
   *  <li>First dimension: participant types</li>
   *  <li>Second dimension: repeats</li>
   *  <li>Third dimension: experiments</li>
   *  <li>Fourth dimension:</li> 
   *    <ol type="1">
   *      <li>Familiar word presentation time mean</li>
   *      <li>Familiar word presentation time standard deviation</li>
   *      <li>Novel word presentation time mean</li>
   *      <li>Novel word presentation time standard deviation</li>
   *      <li>t value for familiar/novel word presentation time means</li>
   *      <li>p value for familiar/novel word presentation time means</li>
   *    </ol>
   * </ul>
   * 
   * For example, invoking data[5][3][1][4] would return the t-value associated 
   * with the second experiment of the fourth repeat for participant type 6.  
   * 
   * @return A 3D {@link java.util.Arrays Array}:
   * <ul>
   *  <li>1st dimension: participant types</li>
   *  <li>2nd dimension: experiments</li>
   *  <li>3rd dimension</li>
   *    <ol type="1">
   *      <li>Mean of familiar word presentation time repeat means</li>
   *      <li>Standard error of familiar word presentation time repeat means</li>
   *      <li>Mean of novel word presentation time repeat means</li>
   *      <li>Standard error of novel word presentation time repeat means</li>
   *    </ol>
   * </ul>
   * 
   * For example, assigning the result to a variable j and invoking j[5][0][1] 
   * would return the standard error of familiar word presentation time repeat 
   * means associated with the repeats of experiment 1 for participant type 6.
   */
  private static double[][][] calculateMeanAndStandardErrorOfMeanFamiliarAndNovelWordPresentationTimesForEachExperiment(double[][][][] data){
    
    double[][][] participantTypeExperimentAverageFamiliarNovelValues = new double[_totalParticipantTypes][2][4];
    for(int participantType = 0; participantType < _totalParticipantTypes; participantType++){
      DescriptiveStatistics expt1FamiliarWordValues = new DescriptiveStatistics();
      DescriptiveStatistics expt1NovelWordValues = new DescriptiveStatistics();
      DescriptiveStatistics expt2FamiliarWordValues = new DescriptiveStatistics();
      DescriptiveStatistics expt2NovelWordValues = new DescriptiveStatistics();
      
      for(int repeat = 0; repeat < _totalRepeats; repeat++){
        expt1FamiliarWordValues.addValue(data[participantType][repeat][0][0]);
        expt1NovelWordValues.addValue(data[participantType][repeat][0][2]);
        expt2FamiliarWordValues.addValue(data[participantType][repeat][1][0]);
        expt2NovelWordValues.addValue(data[participantType][repeat][1][2]);
      }
      
      participantTypeExperimentAverageFamiliarNovelValues[participantType][0][0] = expt1FamiliarWordValues.getMean();
      participantTypeExperimentAverageFamiliarNovelValues[participantType][0][1] = expt1FamiliarWordValues.getStandardDeviation()/(Math.sqrt(_totalRepeats));
      participantTypeExperimentAverageFamiliarNovelValues[participantType][0][2] = expt1NovelWordValues.getMean();
      participantTypeExperimentAverageFamiliarNovelValues[participantType][0][3] = expt1NovelWordValues.getStandardDeviation()/(Math.sqrt(_totalRepeats));
      participantTypeExperimentAverageFamiliarNovelValues[participantType][1][0] = expt2FamiliarWordValues.getMean();
      participantTypeExperimentAverageFamiliarNovelValues[participantType][1][1] = expt2FamiliarWordValues.getStandardDeviation()/(Math.sqrt(_totalRepeats));
      participantTypeExperimentAverageFamiliarNovelValues[participantType][1][2] = expt2NovelWordValues.getMean();
      participantTypeExperimentAverageFamiliarNovelValues[participantType][1][3] = expt2NovelWordValues.getStandardDeviation()/(Math.sqrt(_totalRepeats));
    }
    
    return participantTypeExperimentAverageFamiliarNovelValues;
  }
  
  /**
   * Calculates the r<sup>2</sup> and RMSE values for each participant type
   * repeat.
   * 
   * @param data A 4D {@link java.util.Arrays Array} with the following 
   * structure (can be obtained from {@link 
   * #calculateMeanSdTAndPValuesForEachExperimentRepeat(double[][][][][])}:
   * 
   * <ul>
   *  <li>First dimension: participant types</li>
   *  <li>Second dimension: repeats</li>
   *  <li>Third dimension: experiments</li>
   *  <li>Fourth dimension:</li> 
   *    <ol type="1">
   *      <li>Familiar word presentation time mean</li>
   *      <li>Familiar word presentation time standard deviation</li>
   *      <li>Novel word presentation time mean</li>
   *      <li>Novel word presentation time standard deviation</li>
   *      <li>t value for familiar/novel word presentation time means</li>
   *      <li>p value for familiar/novel word presentation time means</li>
   *    </ol>
   * </ul>
   * 
   * For example, invoking data[5][3][1][4] would return the t-value associated 
   * with the second experiment of the fourth repeat for participant type 6.  
   * 
   * @return A 3D {@link java.util.Arrays Array}:
   * <ul>
   *  <li>1st dimension: participant types</li>
   *  <li>2nd dimension: repeats</li>
   *  <li>3rd dimension</li>
   *    <ol type="1">
   *      <li>R-square</li>
   *      <li>RMSE</li>
   *    </ol>
   * </ul>
   * 
   * For example, assigning the result to a variable j and invoking j[5][3][1] 
   * would return the RMSE value associated with the fourth repeat of 
   * experiments for participant type 6.
   */
  private static double[][][] calculateRSquareAndRmseForEachRepeat(double[][][][] data){
    
    double[][][] rsquareAndRmseValues = new double[_totalParticipantTypes][_totalRepeats][2];
    
    for(int participantType = 0; participantType < _totalParticipantTypes; participantType++){
      for(int repeat = 0; repeat < _totalRepeats; repeat++){
    
        double[][] humanModelFamilAndNovelRecTimesMeans = new double[4][2];

        humanModelFamilAndNovelRecTimesMeans[0][0] = 7.97;
        humanModelFamilAndNovelRecTimesMeans[0][1] = data[participantType][repeat][0][0];
        humanModelFamilAndNovelRecTimesMeans[1][0] = 8.85;
        humanModelFamilAndNovelRecTimesMeans[1][1] = data[participantType][repeat][0][2];

        humanModelFamilAndNovelRecTimesMeans[2][0] = 6.77;
        humanModelFamilAndNovelRecTimesMeans[2][1] = data[participantType][repeat][1][0];
        humanModelFamilAndNovelRecTimesMeans[3][0] = 7.60;
        humanModelFamilAndNovelRecTimesMeans[3][1] = data[participantType][repeat][0][2];

        /****************************/
        /**** Calculate R-square ****/
        /****************************/
        SimpleRegression repeatRegression = new SimpleRegression();
        repeatRegression.addData(humanModelFamilAndNovelRecTimesMeans);
        rsquareAndRmseValues[participantType][repeat][0] = repeatRegression.getRSquare();
        
        /************************/
        /**** Calculate RMSE ****/
        /************************/
       
        //The Apache Stats library gives incorrect RMSEs if 
        //Math.sqrt(repeatRegression.getMeanSquaredError()) is used.  Therefore, 
        //the RMSE for a repeat is calculated "by hand".
        
        //First, get the difference between the mean famil/novel times in each
        //experiment and square them.
        double expt1FamilWordTimeHumanModelDiffSquared = Math.pow(humanModelFamilAndNovelRecTimesMeans[0][0] - humanModelFamilAndNovelRecTimesMeans[0][1], 2.0);
        double expt1NovelWordTimeHumanModelDiffSquared = Math.pow(humanModelFamilAndNovelRecTimesMeans[1][0] - humanModelFamilAndNovelRecTimesMeans[1][1], 2.0);
        double expt2FamilWordTimeHumanModelDiffSquared = Math.pow(humanModelFamilAndNovelRecTimesMeans[2][0] - humanModelFamilAndNovelRecTimesMeans[2][1], 2.0);
        double expt2NovelWordTimeHumanModelDiffSquared = Math.pow(humanModelFamilAndNovelRecTimesMeans[3][0] - humanModelFamilAndNovelRecTimesMeans[3][1], 2.0);
        
        //Sum the differences calculated above.
        double sumHumanModelDiffSquares = 
          expt1FamilWordTimeHumanModelDiffSquared + 
          expt1NovelWordTimeHumanModelDiffSquared + 
          expt2FamilWordTimeHumanModelDiffSquared + 
          expt2NovelWordTimeHumanModelDiffSquared
        ;
        
        //Divide the sum above by the number of data points.
        double meanSquaredError = sumHumanModelDiffSquares/8;
        
        //Finally, square root the mean squared error.
        double rootMeanSquaredError = Math.sqrt(meanSquaredError);
        rsquareAndRmseValues[participantType][repeat][1] = rootMeanSquaredError;
      }
    }
    
    return rsquareAndRmseValues;
  }
  
  /**
   * Calculates the mean r<sup>2</sup> and RMSE value for each participant type
   * by taking into account the r<sup>2</sup> and RMSE values for each of the 
   * participant type's repeats.
   * 
   * @param participantTypeRsquareAndRmseForEachRepeat A 3D {@link 
   * java.util.Arrays Array} whose structure should be as follows:
   * 
   * <ul>
   *  <li>First dimension: participant types</li>
   *  <li>Second dimension: repeats</li>
   *  <li>
   *    Third dimension: r<sup>2</sup> and RMSE as elements of the {@link 
   *    java.util.Arrays Array} in this order
   * </li>
   * </ul>
   * 
   * For example, participantTypeRsquareAndRmseForEachRepeat[3][6][0] should
   * return the r<sup>2</sup> for the 7th repeat for participant type 4.
   * 
   * @return A 2D {@link java.util.Arrays Array} whose structure should be as 
   * follows:
   * 
   * <ul>
   *  <li>First dimension: participant types</li>
   *  <li>
   *    Second dimension: average r<sup>2</sup> and RMSE as elements of the 
   *    {@link java.util.Arrays Array} in this order
   * </li>
   * </ul>
   * 
   * For example, invoking [3][0] on the returned {@link java.util.Arrays Array} 
   * will return the average r<sup>2</sup> for participant type 4.
   */
  private static double[][] calculateMeanRsquareAndRmseForEachParticipantType(double[][][] participantTypeRsquareAndRmseForEachRepeat){
    
    double[][] meanRsquareAndRmseForEachParticipantType = new double[_totalParticipantTypes][2];
    
    for(int participantType = 0; participantType < _totalParticipantTypes; participantType++){
      DescriptiveStatistics rsquaresForParticipantType = new DescriptiveStatistics();
      DescriptiveStatistics rmsesForParticipantType = new DescriptiveStatistics();
      
      for(int repeat = 0; repeat < _totalRepeats; repeat++){
        rsquaresForParticipantType.addValue(participantTypeRsquareAndRmseForEachRepeat[participantType][repeat][0]);
        rmsesForParticipantType.addValue(participantTypeRsquareAndRmseForEachRepeat[participantType][repeat][1]);
      }

      meanRsquareAndRmseForEachParticipantType[participantType][0] = rsquaresForParticipantType.getMean(); 
      meanRsquareAndRmseForEachParticipantType[participantType][1] = rmsesForParticipantType.getMean();
    }
    
    return meanRsquareAndRmseForEachParticipantType;
  }
  
  /**
   * Convenience convertor function.
   * 
   * @param values
   * @return 
   */
  private static double[] convertDoubleListToDoubleArray(List<Double> values){
    double[] array = new double[values.size()];
    for(int i = 0; i < array.length; i++){
      array[i] = values.get(i);
    }
    return array;
  }
}
