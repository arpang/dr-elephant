/*
 * Copyright 2016 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.linkedin.drelephant.tuning;

import com.linkedin.drelephant.clients.azkaban.AzkabanJobStatusUtil;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import models.JobExecution;
import models.JobExecution.ExecutionState;
import models.JobSuggestedParamSet;
import models.JobSuggestedParamSet.ParamSetStatus;
import models.TuningJobDefinition;
import models.TuningJobExecutionParamSet;
import org.apache.log4j.Logger;


/**
 * Job completion detector for azkaban jobs. This utility uses azkaban rest api to find out if the jobs in a flow are
 * completed or not.
 */
public class AzkabanJobCompleteDetector extends JobCompleteDetector {

  private static final Logger logger = Logger.getLogger(AzkabanJobCompleteDetector.class);
  private AzkabanJobStatusUtil _azkabanJobStatusUtil;

  public enum AzkabanJobStatus {
    FAILED, CANCELLED, KILLED, SUCCEEDED
  }

  private JobExecution getJobExecution(Long sentParamSetId) {
    TuningJobExecutionParamSet tuningJobExecutionParamSet = TuningJobExecutionParamSet.find
        .fetch(TuningJobExecutionParamSet.TABLE.jobExecution, "*")
        .where()
        .eq(TuningJobExecutionParamSet.TABLE.jobSuggestedParamSet + '.' + JobSuggestedParamSet.TABLE.id, sentParamSetId)
        .order()
        .desc(TuningJobExecutionParamSet.TABLE.jobExecution + '.' + JobExecution.TABLE.id)
        .setMaxRows(1)
        .findUnique();
    return tuningJobExecutionParamSet.jobExecution;
  }

  /**
   * Returns the list of completed executions
   * @param sentParamSetList Started Execution list
   * @return List of completed executions
   * @throws MalformedURLException MalformedURLException
   * @throws URISyntaxException URISyntaxException
   */
  protected List<JobExecution> getCompletedExecutions(List<JobSuggestedParamSet> sentParamSetList)
      throws MalformedURLException, URISyntaxException {
    logger.info("Fetching the list of executions completed since last iteration");
    List<JobExecution> completedExecutions = new ArrayList<JobExecution>();
    try {
      for (JobSuggestedParamSet jobSuggestedParamSet : sentParamSetList) {

        JobExecution jobExecution = getJobExecution(jobSuggestedParamSet.id);

        logger.info("Checking current status of started execution: " + jobExecution.jobExecId);

        if (_azkabanJobStatusUtil == null) {
          logger.info("Initializing  AzkabanJobStatusUtil");
          _azkabanJobStatusUtil = new AzkabanJobStatusUtil();
        }

        try {
          Map<String, String> jobStatus = _azkabanJobStatusUtil.getJobsFromFlow(jobExecution.flowExecution.flowExecId);
          if (jobStatus != null) {
            for (Map.Entry<String, String> job : jobStatus.entrySet()) {
              logger.info("Job Found:" + job.getKey() + ". Status: " + job.getValue());
              if (job.getKey().equals(jobExecution.job.jobName)) {
                if (job.getValue().equals(AzkabanJobStatus.FAILED.toString())) {
                  jobSuggestedParamSet.paramSetState = ParamSetStatus.EXECUTED;
                  jobExecution.executionState = ExecutionState.FAILED;
                }
                if (job.getValue().equals(AzkabanJobStatus.CANCELLED.toString()) || job.getValue()
                    .equals(AzkabanJobStatus.KILLED.toString())) {
                  jobSuggestedParamSet.paramSetState = ParamSetStatus.EXECUTED;
                  jobExecution.executionState = ExecutionState.CANCELLED;
                }
                if (job.getValue().equals(AzkabanJobStatus.SUCCEEDED.toString())) {
                  jobSuggestedParamSet.paramSetState = ParamSetStatus.EXECUTED;
                  jobExecution.executionState = ExecutionState.SUCCEEDED;
                }
                if (jobSuggestedParamSet.paramSetState.equals(ParamSetStatus.EXECUTED)) {
                  jobExecution.update();
                  jobSuggestedParamSet.update();
                  completedExecutions.add(jobExecution);
                  logger.info("Execution " + jobExecution.jobExecId + " is completed");
                } else {
                  logger.info("Execution " + jobExecution.jobExecId + " is still in running state");
                }
              }
            }
          } else {
            logger.info("No jobs found for flow execution: " + jobExecution.flowExecution.flowExecId);
          }
        } catch (Exception e) {
          logger.error("Error in checking status of execution: " + jobExecution.jobExecId, e);
        }
      }
    } catch (Exception e) {
      logger.error("Error in fetching list of completed executions", e);
      e.printStackTrace();
    }
    logger.info("Number of executions completed since last iteration: " + completedExecutions.size());
    return completedExecutions;
  }
}
